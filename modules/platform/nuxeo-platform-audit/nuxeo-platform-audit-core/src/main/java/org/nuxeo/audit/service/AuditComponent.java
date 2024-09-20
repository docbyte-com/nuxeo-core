/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.service;

import static java.util.stream.Collectors.toMap;
import static org.nuxeo.audit.impl.StreamAuditWriter.COMPUTATION_NAME;
import static org.nuxeo.audit.listener.StreamAuditEventListener.STREAM_NAME;
import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;

import java.io.Serializable;
import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.el.ELException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryBuilder;
import org.nuxeo.audit.service.extension.AdapterDescriptor;
import org.nuxeo.audit.service.extension.AuditBackendFactoryDescriptor;
import org.nuxeo.audit.service.extension.EventDescriptor;
import org.nuxeo.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 2025.0
 */
public class AuditComponent extends DefaultComponent implements AuditService {

    private static final Logger log = LogManager.getLogger(AuditComponent.class);

    /**
     * The default audit backend {@link AuditBackendFactoryDescriptor#getName() name}.
     */
    public static final String DEFAULT_AUDIT_BACKEND = "default";

    /**
     * If passed as true on the event properties, event not logged
     */
    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    public static final String FORCE_AUDIT_FACET = "ForceAudit";

    protected static final String ADAPTER_EXT_POINT = "adapter";

    protected static final String BACKEND_FACTORY_EXT_POINT = "backendFactory";

    protected static final String EVENT_EXT_POINT = "event";

    protected static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    protected final Map<String, AuditBackendFactory<?>> auditBackendFactories = new HashMap<>();

    protected final Map<String, List<ExtendedInfoMapper>> eventExtendedInfoMappers = new HashMap<>();

    protected final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(new ExpressionFactoryImpl());

    // --------------
    // Component APIs
    // --------------

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AuditBackend.class)) {
            return adapter.cast(getAuditBackend(DEFAULT_AUDIT_BACKEND));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.AUDIT;
    }

    @Override
    public void start(ComponentContext context) {
        // pre-compute global extendedInfo mappers
        var extendedInfoDescriptors = this.<ExtendedInfoDescriptor> getDescriptors(EXTENDED_INFO_EXT_POINT)
                                          .stream()
                                          .filter(ExtendedInfoDescriptor::isEnabled)
                                          .toList();
        // register auditable event names and their specific extendedInfo mappers
        eventExtendedInfoMappers.putAll(
                this.<EventDescriptor> getDescriptors(EVENT_EXT_POINT)
                    .stream()
                    .filter(EventDescriptor::isEnabled)
                    .peek(eventDescriptor -> log.debug("Registered event: {}", eventDescriptor::getName))
                    .collect(toMap(EventDescriptor::getName,
                            // compute the extendedInfo mappers for each registered events
                            eventDescriptor -> Stream.concat(extendedInfoDescriptors.stream(),
                                    eventDescriptor.getExtendedInfoDescriptors().stream())
                                                     .collect(Collectors.toMap(ExtendedInfoDescriptor::getKey,
                                                             Function.identity(), ExtendedInfoDescriptor::merge))
                                                     .values()
                                                     .stream()
                                                     // specific mapper can disable global one
                                                     .filter(ExtendedInfoDescriptor::isEnabled)
                                                     .map(descriptor -> new ExtendedInfoMapper(descriptor.getKey(),
                                                             descriptor.getExpression()))
                                                     .toList())));
        // register auditBackendFactories
        auditBackendFactories.putAll( //
                this.<AuditBackendFactoryDescriptor> getDescriptors(BACKEND_FACTORY_EXT_POINT)
                    .stream()
                    .collect(toMap(AuditBackendFactoryDescriptor::getName,
                            descriptor -> Framework.getService(descriptor.getFactory()))));
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        eventExtendedInfoMappers.clear();
        auditBackendFactories.clear();
    }

    // -----------------
    // AuditService APIs
    // -----------------

    @Override
    public Set<String> getAuditableEventNames() {
        return Collections.unmodifiableSet(eventExtendedInfoMappers.keySet());
    }

    @Override
    public List<ExtendedInfoMapper> getExtendedInfoMappers(String eventName) {
        return eventExtendedInfoMappers.getOrDefault(eventName, List.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends AuditBackend> B getAuditBackend(String name) {
        var factory = auditBackendFactories.get(name);
        if (factory == null) {
            throw new NuxeoException("No AuditBackendFactory configured for name: " + name);
        }
        var backend = (B) factory.getAuditBackend(name);
        if (backend == null) {
            throw new NuxeoException("No AuditBackend configured for name: " + name);
        }
        return backend;
    }

    @Override
    public LogEntry buildEntryFromEvent(Event event) {
        EventContext ctx = event.getContext();
        String eventName = event.getName();

        Date eventDate = new Date(event.getTime());

        var builder = LogEntry.builder(eventName, eventDate);

        if (ctx instanceof DocumentEventContext docCtx) {
            DocumentModel document = docCtx.getSourceDocument();
            if (document.hasFacet(SYSTEM_DOCUMENT) && !document.hasFacet(FORCE_AUDIT_FACET)) {
                // do not log event on System documents
                // unless it has the FORCE_AUDIT_FACET facet
                return null;
            }

            Boolean disabled = (Boolean) docCtx.getProperty(DISABLE_AUDIT_LOGGER);
            if (BooleanUtils.isTrue(disabled)) {
                // don't log events with this flag
                return null;
            }
            NuxeoPrincipal principal = docCtx.getPrincipal();
            Map<String, Serializable> properties = docCtx.getProperties();

            builder.docUUID(document.getId());
            builder.docPath(document.getPathAsString());
            builder.docType(document.getType());
            builder.repositoryId(document.getRepositoryName());
            if (principal != null) {
                builder.principalName(principal);
            } else {
                log.warn("received event {} with null principal", eventName);
            }
            builder.comment((String) properties.get("comment"));
            if (document instanceof DeletedDocumentModel) {
                builder.comment("Document does not exist anymore!");
            } else {
                if (document.isLifeCycleLoaded()) {
                    builder.docLifeCycle(document.getCurrentLifeCycleState());
                }
            }
            if (LifeCycleConstants.TRANSITION_EVENT.equals(eventName)) {
                builder.docLifeCycle((String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TO));
            }
            String category = (String) properties.get("category");
            builder.category(Objects.requireNonNullElse(category, "eventDocumentCategory"));

            doPutExtendedInfos(builder, docCtx, document, principal);

        } else {
            NuxeoPrincipal principal = ctx.getPrincipal();
            Map<String, Serializable> properties = ctx.getProperties();

            if (principal != null) {
                builder.principalName(principal);
            }
            builder.comment((String) properties.get("comment"));

            String category = (String) properties.get("category");
            builder.category(category);

            doPutExtendedInfos(builder, ctx, null, principal);

        }

        return builder.build();
    }

    protected void doPutExtendedInfos(LogEntryBuilder builder, EventContext eventContext, DocumentModel source,
            Principal principal) {
        ExpressionContext context = new ExpressionContext();
        if (eventContext != null) {
            expressionEvaluator.bindValue(context, "message", eventContext);
        }
        if (source != null) {
            expressionEvaluator.bindValue(context, "source", source);
            // inject now the adapters
            for (var ad : this.<AdapterDescriptor> getDescriptors(ADAPTER_EXT_POINT)) {
                if (source instanceof DeletedDocumentModel) {
                    continue; // skip
                }
                Object adapter = source.getAdapter(ad.getKlass());
                if (adapter != null) {
                    expressionEvaluator.bindValue(context, ad.getName(), adapter);
                }
            }
        }
        if (principal != null) {
            expressionEvaluator.bindValue(context, "principal", principal);
        }

        populateExtendedInfo(builder, source, context, getExtendedInfoMappers(builder.eventId()));

        if (eventContext != null) {
            @SuppressWarnings("unchecked")
            var map = (Map<String, Serializable>) eventContext.getProperty("extendedInfos");
            if (map != null) {
                for (Map.Entry<String, Serializable> en : map.entrySet()) {
                    Serializable value = en.getValue();
                    if (value != null) {
                        builder.extended(en.getKey(), value);
                    }
                }
            }
        }
    }

    protected void populateExtendedInfo(LogEntryBuilder builder, DocumentModel source, ExpressionContext context,
            List<ExtendedInfoMapper> mappers) {
        for (var mapper : mappers) {
            String exp = mapper.expression();
            Serializable value;
            try {
                value = expressionEvaluator.evaluateExpression(context, exp, Serializable.class);
            } catch (PropertyException | UnsupportedOperationException e) {
                if (source instanceof DeletedDocumentModel) {
                    log.debug("Can not evaluate the expression: {} on a DeletedDocumentModel, skipping.", exp);
                }
                continue;
            } catch (DocumentNotFoundException e) {
                if (!DocumentEventTypes.DOCUMENT_REMOVED.equals(builder.eventId())) {
                    log.error("Document not found while populating extendedInfos into log entry: {}", builder::build,
                            () -> e);
                }
                continue;
            } catch (ELException e) {
                continue;
            }
            if (value == null) {
                continue;
            }
            builder.extended(mapper.key(), value);
        }
    }

    @Override
    public boolean await(Duration duration) throws InterruptedException {
        StreamService service = Framework.getService(StreamService.class);
        org.nuxeo.lib.stream.log.LogManager logManager = service.getLogManager();
        // when there is no lag between producer and consumer we are done
        long deadline = System.currentTimeMillis() + duration.toMillis();
        while (logManager.getLag(Name.ofUrn(STREAM_NAME), Name.ofUrn(COMPUTATION_NAME)).lag() > 0) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("await timeout on audit/writer");
                return false;
            }
            Thread.sleep(50);
        }
        return true;
    }

    /**
     * INTERNAL METHOD FOR TESTS, DO NOT USE.
     */
    protected void clearEntriesFromBackends() {
        auditBackendFactories.entrySet()
                             .stream()
                             .map(entry -> entry.getValue().getAuditBackend(entry.getKey()))
                             .filter(AbstractAuditBackend.class::isInstance)
                             .map(AbstractAuditBackend.class::cast)
                             .forEach(AbstractAuditBackend::clearEntries);
    }
}
