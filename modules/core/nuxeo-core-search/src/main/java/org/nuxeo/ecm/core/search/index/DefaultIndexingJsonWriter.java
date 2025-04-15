/*
 * (C) Copyright 2014-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.search.index;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.automation.core.util.JSONPropertyWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

/**
 * JSon writer that outputs a format ready to eat by elasticsearch.
 *
 * @since 5.9.3
 */
public class DefaultIndexingJsonWriter implements IndexingJsonWriter {
    private static final Logger log = LogManager.getLogger(DefaultIndexingJsonWriter.class);

    public static final String REPOSITORY_PROP = "ecm:repository";

    public static final String TAG_FACET = "NXTag";

    public static final String TAG_PROP = "nxtag:tags";

    public static final String TAG_LABEL_PROP = "label";

    @Override
    public void writeDocument(JsonGenerator jg, DocumentModel doc) throws IOException {
        jg.writeStartObject();
        writeSystemProperties(jg, doc);
        writeSchemas(jg, doc);
        jg.writeEndObject();
        jg.flush();
    }

    /**
     * @since 7.2
     */
    protected void writeSystemProperties(JsonGenerator jg, DocumentModel doc) throws IOException {
        String docId = doc.getId();
        CoreSession session = doc.getCoreSession();
        jg.writeStringField(REPOSITORY_PROP, doc.getRepositoryName());
        jg.writeStringField("ecm:uuid", docId);
        jg.writeStringField("ecm:name", doc.getName());
        jg.writeStringField("ecm:title", doc.getTitle());

        String pathAsString = doc.getPathAsString();
        jg.writeStringField("ecm:path", pathAsString);
        if (StringUtils.isNotBlank(pathAsString)) {
            String[] split = pathAsString.split("/");
            if (split.length > 0) {
                for (int i = 1; i < split.length; i++) {
                    jg.writeStringField("ecm:path@level" + i, split[i]);
                }
            }
            jg.writeNumberField("ecm:path@depth", split.length);
        }

        jg.writeStringField("ecm:primaryType", doc.getType());
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef != null) {
            jg.writeStringField("ecm:parentId", parentRef.toString());
            // @since 2025 ancestors must be materialized
            jg.writeArrayFieldStart("ecm:ancestorId");
            jg.writeString(parentRef.toString());
            Arrays.stream(session.getParentDocumentRefs(parentRef))
                  .map(ref -> ref.reference().toString())
                  .forEach(ThrowableConsumer.asConsumer(jg::writeString));
            jg.writeEndArray();
        }
        jg.writeStringField("ecm:currentLifeCycleState", doc.getCurrentLifeCycleState());
        if (doc.isVersion() || doc.isProxy()) {
            jg.writeStringField("ecm:versionLabel", doc.getVersionLabel());
            jg.writeStringField("ecm:versionVersionableId", doc.getVersionSeriesId());
            jg.writeStringField("ecm:versionDescription", doc.getCheckinComment());
            Calendar cd = doc.getCheckinDate();
            if (cd != null) {
                jg.writeStringField("ecm:versionCreated", cd.toInstant().toString());
            }
        }
        if (doc.isProxy()) {
            jg.writeStringField("ecm:proxyVersionableId", doc.getVersionSeriesId());
            jg.writeStringField("ecm:proxyTargetId", doc.getSourceId());
        }
        jg.writeBooleanField("ecm:isCheckedIn", !doc.isCheckedOut());
        jg.writeBooleanField("ecm:isProxy", doc.isProxy());
        jg.writeBooleanField("ecm:isTrashed", doc.isTrashed());
        jg.writeBooleanField("ecm:isVersion", doc.isVersion());
        jg.writeBooleanField("ecm:isLatestVersion", doc.isLatestVersion());
        jg.writeBooleanField("ecm:isLatestMajorVersion", doc.isLatestMajorVersion());
        jg.writeBooleanField("ecm:isRecord", doc.isRecord());
        Calendar retainUntil = doc.getRetainUntil();
        if (retainUntil != null) {
            jg.writeStringField("ecm:retainUntil", retainUntil.toInstant().toString());
        }
        jg.writeBooleanField("ecm:hasLegalHold", doc.hasLegalHold());
        jg.writeArrayFieldStart("ecm:mixinType");
        boolean hasFacetTag = false;
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
            if (TAG_FACET.equals(facet)) {
                hasFacetTag = true;
            }
        }
        jg.writeEndArray();
        if (hasFacetTag) {
            @SuppressWarnings("unchecked")
            var tags = (List<Map<String, Serializable>>) doc.getPropertyValue(TAG_PROP);
            if (!tags.isEmpty()) {
                jg.writeArrayFieldStart("ecm:tag");
                for (Map<String, Serializable> tag : tags) {
                    jg.writeString(tag.get(TAG_LABEL_PROP).toString());
                }
                jg.writeEndArray();
            }
        }
        jg.writeStringField("ecm:changeToken", doc.getChangeToken());
        Long pos = doc.getPos();
        if (pos != null) {
            jg.writeNumberField("ecm:pos", pos);
        }
        // Add a positive ACL only
        SecurityService securityService = Framework.getService(SecurityService.class);
        List<String> browsePermissions = new ArrayList<>(Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
        ACP acp = null;
        try {
            acp = doc.getACP();
        } catch (IllegalArgumentException e) {
            log.atError()
               .withThrowable(log.isDebugEnabled() ? e : null)
               .log("Skipping corrupted ACP for doc: {}, because of: {}", doc.getRef(), e.getMessage());
        }
        if (acp == null) {
            acp = new ACPImpl();
        }
        jg.writeArrayFieldStart("ecm:acl");
        outerloop: for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted() && ace.isEffective() && browsePermissions.contains(ace.getPermission())) {
                    jg.writeString(ace.getUsername());
                }
                if (ace.isDenied() && ace.isEffective()) {
                    if (!EVERYONE.equals(ace.getUsername())) {
                        jg.writeString(UNSUPPORTED_ACL);
                    }
                    break outerloop;
                }
            }
        }

        jg.writeEndArray();
        Map<String, String> bmap = getBinaryFulltext(doc);
        if (bmap != null && !bmap.isEmpty()) {
            for (Map.Entry<String, String> item : bmap.entrySet()) {
                String value = item.getValue();
                if (value != null) {
                    jg.writeStringField("ecm:" + item.getKey(), value);
                }
            }
        }
    }

    // kept separate for easy override
    protected Map<String, String> getBinaryFulltext(DocumentModel doc) {
        return doc.getBinaryFulltext();
    }

    /**
     * @since 7.2
     */
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc) throws IOException {
        for (String schema : doc.getSchemas()) {
            writeProperties(jg, doc, schema);
        }
    }

    protected static void writeProperties(JsonGenerator jg, DocumentModel doc, String schema) throws IOException {
        Collection<Property> properties = doc.getPropertyObjects(schema);
        if (properties.isEmpty()) {
            return;
        }

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        String prefix = schemaManager.getSchema(schema).getNamespace().prefix;
        if (prefix == null || prefix.isEmpty()) {
            prefix = schema;
        }
        JSONPropertyWriter writer = JSONPropertyWriter.create().writeNull(false).writeEmpty(false).prefix(prefix);

        if ("note".equals(schema) && "text/html".equals(doc.getPropertyValue("note:mime_type"))) {
            Property mimeType = doc.getProperty("note:mime_type");
            writer.writeProperty(jg, mimeType);
            jg.writeFieldName("note:note");
            String html = (String) doc.getPropertyValue("note:note");
            jg.writeString(extractTextFromHtml(html));
            return;
        }
        for (Property p : properties) {
            try {
                writer.writeProperty(jg, p);
            } catch (ClassCastException e) {
                throw new PropertyConversionException(String.format("Corrupted property: %s, on document: %s", p, doc),
                        e);
            }
        }
    }

    protected static String extractTextFromHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        Source source = new Source(html);
        source.fullSequentialParse();
        TextExtractor extractor = source.getTextExtractor();
        extractor.setConvertNonBreakingSpaces(true);
        extractor.setExcludeNonHTMLElements(true);
        extractor.setIncludeAttributes(false);
        return extractor.toString();
    }

}
