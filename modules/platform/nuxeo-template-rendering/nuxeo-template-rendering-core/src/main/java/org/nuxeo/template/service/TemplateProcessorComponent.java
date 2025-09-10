/*
 * (C) Copyright 2012-2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.template.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.pubsub.AbstractPubSubBroker;
import org.nuxeo.runtime.pubsub.SerializableMessage;
import org.nuxeo.template.adapters.doc.TemplateBasedDocumentAdapterImpl;
import org.nuxeo.template.adapters.doc.TemplateBinding;
import org.nuxeo.template.adapters.doc.TemplateBindings;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;
import org.nuxeo.template.context.AbstractContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.processors.IdentityProcessor;

/**
 * Runtime Component used to handle Extension Points and expose the {@link TemplateProcessorService} interface
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TemplateProcessorComponent extends DefaultComponent implements TemplateProcessorService {

    protected static final Logger log = LogManager.getLogger(TemplateProcessorComponent.class);

    public static final String PROCESSOR_XP = "processor";

    public static final String CONTEXT_EXTENSION_XP = "contextExtension";

    public static final String OUTPUT_FORMAT_EXTENSION_XP = "outputFormat";

    protected static final String TEMPLATE_PROCESSOR_INVAL_PUBSUB_TOPIC = "templateProcessorInval";

    private static final String FILTER_VERSIONS_PROPERTY = "nuxeo.templating.filterVersions";

    protected Map<String, TemplateProcessorDescriptor> processors;

    protected Map<String, ContextExtensionFactoryDescriptor> contextFactories;

    protected Map<String, OutputFormatDescriptor> outputFormats;

    protected volatile Map<String, List<String>> type2Template;

    protected TemplateProcessorInvalidator invalidator;

    @Override
    public void start(ComponentContext context) {
        processors = this.<TemplateProcessorDescriptor> getDescriptors(PROCESSOR_XP)
                         .stream()
                         .filter(TemplateProcessorDescriptor::isEnabled)
                         .collect(Collectors.toMap(TemplateProcessorDescriptor::getName, Function.identity()));
        contextFactories = this.<ContextExtensionFactoryDescriptor> getDescriptors(CONTEXT_EXTENSION_XP)
                               .stream()
                               .filter(ContextExtensionFactoryDescriptor::isEnabled)
                               .collect(Collectors.toMap(ContextExtensionFactoryDescriptor::getName,
                                       Function.identity()));
        outputFormats = this.<OutputFormatDescriptor> getDescriptors(OUTPUT_FORMAT_EXTENSION_XP)
                            .stream()
                            .filter(OutputFormatDescriptor::isEnabled)
                            .collect(Collectors.toMap(OutputFormatDescriptor::getId, Function.identity()));
        // force recompute of reserved keywords
        FreeMarkerVariableExtractor.resetReservedContextKeywords();
        registerInvalidator();
    }

    @Override
    public void stop(ComponentContext context) {
        processors = null;
        contextFactories = null;
        outputFormats = null;
        unregisterInvalidator();
    }

    protected void registerInvalidator() {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService != null && clusterService.isEnabled()) {
            // register TemplateProcessor invalidator
            String nodeId = clusterService.getNodeId();
            invalidator = new TemplateProcessorInvalidator();
            invalidator.initialize(TEMPLATE_PROCESSOR_INVAL_PUBSUB_TOPIC, nodeId);
            log.info("Registered Template Processor invalidator for node: {}", nodeId);
        } else {
            log.info("Not registering a Template Processor invalidator because clustering is not enabled");
        }
    }

    protected void unregisterInvalidator() {
        if (invalidator != null) {
            invalidator.close();
        }
    }

    @Override
    public TemplateProcessor findProcessor(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            return null;
        }
    }

    @Override
    public String findProcessorName(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getName();
        } else {
            return null;
        }
    }

    public TemplateProcessorDescriptor findProcessorDescriptor(Blob templateBlob) {
        TemplateProcessorDescriptor processor = null;
        String mt = templateBlob.getMimeType();
        if (mt != null) {
            processor = findProcessorByMimeType(mt);
        }
        if (processor == null) {
            String fileName = templateBlob.getFilename();
            if (fileName != null) {
                String ext = FileUtils.getFileExtension(fileName);
                processor = findProcessorByExtension(ext);
            }
        }
        return processor;
    }

    @Override
    public void addContextExtensions(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx) {
        for (var entry : contextFactories.entrySet()) {
            var name = entry.getKey();
            var descriptor = entry.getValue();
            ContextExtensionFactory factory = descriptor.getExtensionFactory();
            if (factory != null) {
                Object ob = factory.getExtension(currentDocument, wrapper, ctx);
                if (ob != null) {
                    ctx.put(name, ob);
                    // also manage aliases
                    for (String alias : descriptor.getAliases()) {
                        ctx.put(alias, ob);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getReservedContextKeywords() {
        List<String> keywords = new ArrayList<>();
        for (var entry : contextFactories.entrySet()) {
            keywords.add(entry.getKey());
            keywords.addAll(entry.getValue().getAliases());
        }
        keywords.addAll(List.of(AbstractContextBuilder.RESERVED_VAR_NAMES));
        return keywords;
    }

    @Override
    public Map<String, ContextExtensionFactoryDescriptor> getRegistredContextExtensions() {
        return contextFactories;
    }

    protected TemplateProcessorDescriptor findProcessorByMimeType(String mt) {
        List<TemplateProcessorDescriptor> candidates = new ArrayList<>();
        for (TemplateProcessorDescriptor desc : processors.values()) {
            if (desc.getSupportedMimeTypes().contains(mt)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.getFirst();
        }
        return null;
    }

    protected TemplateProcessorDescriptor findProcessorByExtension(String extension) {
        List<TemplateProcessorDescriptor> candidates = new ArrayList<>();
        for (TemplateProcessorDescriptor desc : processors.values()) {
            if (desc.getSupportedExtensions().contains(extension)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.getFirst();
        }
        return null;
    }

    public TemplateProcessorDescriptor getDescriptor(String name) {
        return processors.get(name);
    }

    @Override
    public TemplateProcessor getProcessor(String name) {
        if (name == null) {
            log.info("No defined processor with name: {}, using Identity as default", name);
            name = IdentityProcessor.NAME;
        }
        TemplateProcessorDescriptor desc = processors.get(name);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            log.warn("Can not get a TemplateProcessor with name: {}", name);
            return null;
        }
    }

    protected String buildTemplateSearchQuery(String targetType) {
        String query = "select * from Document where ecm:mixinType = 'Template' AND ecm:isTrashed = 0";
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            query += " AND ecm:isVersion = 0";
        }
        if (targetType != null) {
            query += " AND tmpl:applicableTypes IN ( 'all', '" + targetType + "')";
        }
        return query;
    }

    protected String buildTemplateSearchByNameQuery(String name) {
        String query = "select * from Document where ecm:mixinType = 'Template' AND tmpl:templateName = "
                + NXQL.escapeString(name);
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            query += " AND ecm:isVersion = 0";
        }
        return query;
    }

    @Override
    public List<DocumentModel> getAvailableTemplateDocs(CoreSession session, String targetType) {
        return session.query(buildTemplateSearchQuery(targetType));
    }

    @Override
    public DocumentModel getTemplateDoc(CoreSession session, String name) {
        String query = buildTemplateSearchByNameQuery(name);
        List<DocumentModel> docs = session.query(query);
        return docs.isEmpty() ? null : docs.getFirst();
    }

    protected <T> List<T> wrap(List<DocumentModel> docs, Class<T> adapter) {
        List<T> result = new ArrayList<>();
        for (DocumentModel doc : docs) {
            T adapted = doc.getAdapter(adapter);
            if (adapted != null) {
                result.add(adapted);
            }
        }
        return result;
    }

    @Override
    public List<TemplateSourceDocument> getAvailableOfficeTemplates(CoreSession session, String targetType) {
        String query = buildTemplateSearchQuery(targetType) + " AND tmpl:useAsMainContent=1";
        List<DocumentModel> docs = session.query(query);
        return wrap(docs, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateSourceDocument> getAvailableTemplates(CoreSession session, String targetType) {
        List<DocumentModel> filtredResult = getAvailableTemplateDocs(session, targetType);
        return wrap(filtredResult, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(DocumentModel source) {
        StringBuilder sb = new StringBuilder().append(
                "select * from Document where ecm:isVersion = 0 AND ecm:isProxy = 0 AND ")
                                              .append(TemplateBindings.BINDING_PROP_NAME)
                                              .append("/*/")
                                              .append(TemplateBinding.TEMPLATE_ID_KEY)
                                              .append(" = '")
                                              .append(source.getId())
                                              .append("'");
        DocumentModelList docs = source.getCoreSession().query(sb.toString());

        List<TemplateBasedDocument> result = new ArrayList<>();
        for (DocumentModel doc : docs) {
            TemplateBasedDocument templateBasedDocument = doc.getAdapter(TemplateBasedDocument.class);
            if (templateBasedDocument != null) {
                result.add(templateBasedDocument);
            }
        }
        return result;
    }

    @Override
    public Collection<TemplateProcessorDescriptor> getRegisteredTemplateProcessors() {
        return processors.values();
    }

    @Override
    public Map<String, List<String>> getTypeMapping() {
        if (type2Template == null) {
            synchronized (this) {
                if (type2Template == null) {
                    TemplateMappingFetcher fetcher = new TemplateMappingFetcher();
                    fetcher.runUnrestricted();
                    type2Template = new ConcurrentHashMap<>(fetcher.getMapping());
                }
            }
        }
        return type2Template;
    }

    @Override
    public synchronized void registerTypeMapping(DocumentModel doc) {
        TemplateSourceDocument tmpl = doc.getAdapter(TemplateSourceDocument.class);
        if (tmpl != null) {
            Map<String, List<String>> mapping = getTypeMapping();
            // check existing mapping for this docId
            List<String> boundTypes = new ArrayList<>();
            boolean mappingChanged = false;
            for (String type : mapping.keySet()) {
                if (mapping.get(type) != null) {
                    if (mapping.get(type).contains(doc.getId())) {
                        boundTypes.add(type);
                    }
                }
            }
            // unbind previous mapping for this docId
            for (String type : boundTypes) {
                List<String> templates = mapping.get(type);
                if (templates.remove(doc.getId())) {
                    mappingChanged = true;
                }
                if (templates.isEmpty()) {
                    mapping.remove(type);
                }
            }
            // rebind types (with override)
            for (String type : tmpl.getForcedTypes()) {
                List<String> templates = mapping.get(type);
                if (templates == null) {
                    templates = new ArrayList<>();
                    mapping.put(type, templates);
                    mappingChanged = true;
                }
                if (!templates.contains(doc.getId())) {
                    templates.add(doc.getId());
                    mappingChanged = true;
                }
            }
            if (mappingChanged && invalidator != null) {
                invalidator.sendMessage(new TemplateProcessorInvalidation());
            }
        }
    }

    @Override
    public DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc, DocumentModel sourceTemplateDoc,
            boolean save) {
        targetDoc.addFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
        TemplateBasedDocument tmplBased = targetDoc.getAdapter(TemplateBasedDocument.class);
        // bind the template
        return tmplBased.setTemplate(sourceTemplateDoc, save);
    }

    @Override
    public DocumentModel detachTemplateBasedDocument(DocumentModel targetDoc, String templateName, boolean save) {
        DocumentModel docAfterDetach = null;
        TemplateBasedDocument tbd = targetDoc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            if (!tbd.getTemplateNames().contains(templateName)) {
                return targetDoc;
            }
            if (tbd.getTemplateNames().size() == 1) {
                // remove the whole facet since there is no more binding
                targetDoc.removeFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
                if (log.isDebugEnabled()) {
                    log.debug("detach after removeFacet, ck=" + targetDoc.getCacheKey());
                }
                if (save) {
                    docAfterDetach = targetDoc.getCoreSession().saveDocument(targetDoc);
                }
            } else {
                // only remove the binding
                docAfterDetach = tbd.removeTemplateBinding(templateName, true);
            }
        }
        if (docAfterDetach != null) {
            return docAfterDetach;
        }
        return targetDoc;
    }

    protected synchronized void invalidateTypeMapping() {
        type2Template = null;
    }

    @Override
    public Collection<OutputFormatDescriptor> getOutputFormats() {
        return outputFormats.values();
    }

    @Override
    public OutputFormatDescriptor getOutputFormatDescriptor(String outputFormatId) {
        return outputFormats.get(outputFormatId);
    }

    public static class TemplateProcessorInvalidation implements SerializableMessage {

        private static final long serialVersionUID = 1L;

        @Override
        public void serialize(OutputStream out) {
            // nothing to write, sending the message itself is enough
        }
    }

    public class TemplateProcessorInvalidator extends AbstractPubSubBroker<TemplateProcessorInvalidation> {

        @Override
        public TemplateProcessorInvalidation deserialize(InputStream in) {
            return new TemplateProcessorInvalidation();
        }

        @Override
        public void receivedMessage(TemplateProcessorInvalidation message) {
            // nothing to read from the message, receiving the message itself is enough
            invalidateTypeMapping();
        }
    }
}
