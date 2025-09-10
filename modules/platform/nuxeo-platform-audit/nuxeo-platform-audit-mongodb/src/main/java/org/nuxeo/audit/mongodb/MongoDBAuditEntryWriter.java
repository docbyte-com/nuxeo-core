/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.audit.mongodb;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_CATEGORY;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_COMMENT;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_TYPE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EXTENDED;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_LOG_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_PRINCIPAL_NAME;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.BooleanUtils;
import org.bson.Document;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.io.LogEntryJsonWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writer for MongoDB Audit.
 *
 * @since 9.1
 */
public class MongoDBAuditEntryWriter {

    /**
     * Framework properties to write extended info that are JSON content as object and not string. Default to true.
     *
     * @since 2025.0
     */
    public static final String EXTENDED_INFO_JSON_AS_OBJECT = "nuxeo.audit.backend.mongodb.extended.json.as.object";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public static Document asDocument(LogEntry logEntry) {
        Document document = new Document(MongoDBSerializationHelper.MONGODB_ID, Long.valueOf(logEntry.getId()));
        document.put(LOG_CATEGORY, logEntry.getCategory());
        document.put(LOG_PRINCIPAL_NAME, logEntry.getPrincipalName());
        document.put(LOG_COMMENT, logEntry.getComment());
        document.put(LOG_DOC_LIFE_CYCLE, logEntry.getDocLifeCycle());
        document.put(LOG_DOC_PATH, logEntry.getDocPath());
        document.put(LOG_DOC_TYPE, logEntry.getDocType());
        document.put(LOG_DOC_UUID, logEntry.getDocUUID());
        document.put(LOG_EVENT_ID, logEntry.getEventId());
        document.put(LOG_REPOSITORY_ID, logEntry.getRepositoryId());
        document.put(LOG_EVENT_DATE, logEntry.getEventDate());
        document.put(LOG_LOG_DATE, logEntry.getLogDate());

        Map<String, Object> extendedInfo = logEntry.getExtended();
        Document extended = new Document();
        for (Entry<String, Object> entry : extendedInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Object[] array) {
                value = Arrays.asList(array);
            } else if (BooleanUtils.toBoolean(Framework.getProperty(EXTENDED_INFO_JSON_AS_OBJECT, "true"))
                    && value instanceof String string && LogEntryJsonWriter.isJsonContent(string)) {
                try {
                    value = MAPPER.readValue(string, Map.class);
                } catch (JsonProcessingException e) {
                    // ignore invalid JSON content, same behavior than LogEntryJsonWriter
                    value = null;
                }
            }
            if (value != null) {
                extended.put(key, value);
            }
        }
        document.put(LOG_EXTENDED, extended);
        return document;
    }

}
