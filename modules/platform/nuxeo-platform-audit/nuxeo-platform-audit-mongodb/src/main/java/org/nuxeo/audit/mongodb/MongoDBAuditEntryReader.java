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

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

/**
 * Reader for MongoDB Audit.
 *
 * @since 9.1
 */
public class MongoDBAuditEntryReader {

    private static final Logger log = LogManager.getLogger(MongoDBAuditEntryReader.class);

    public static LogEntry read(Document doc) {
        var eventId = doc.get(LOG_EVENT_ID, String.class);
        var eventDate = doc.get(LOG_EVENT_DATE, Date.class);
        var builder = LogEntry.builder(eventId, eventDate);
        for (String key : doc.keySet()) {
            switch (key) {
                case MongoDBSerializationHelper.MONGODB_ID -> builder.id(doc.getLong(key));
                case LOG_CATEGORY -> builder.category(doc.getString(key));
                case LOG_PRINCIPAL_NAME -> builder.principalName(doc.getString(key));
                case LOG_COMMENT -> builder.comment(doc.getString(key));
                case LOG_DOC_LIFE_CYCLE -> builder.docLifeCycle(doc.getString(key));
                case LOG_DOC_PATH -> builder.docPath(doc.getString(key));
                case LOG_DOC_TYPE -> builder.docType(doc.getString(key));
                case LOG_DOC_UUID -> builder.docUUID(doc.getString(key));
                case LOG_REPOSITORY_ID -> builder.repositoryId(doc.getString(key));
                case LOG_LOG_DATE -> builder.logDate(doc.getDate(key));
                case LOG_EXTENDED -> builder.extended(readExtendedInfo(doc.get(key, Document.class)));
                case LOG_EVENT_ID, LOG_EVENT_DATE -> {
                    // already read
                }
                default -> log.warn("Property with key: {} is not a known LogEntry property, skip it.", key);
            }
        }
        return builder.build();
    }

    public static Map<String, Object> readExtendedInfo(Document extInfos) {
        return extInfos.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
