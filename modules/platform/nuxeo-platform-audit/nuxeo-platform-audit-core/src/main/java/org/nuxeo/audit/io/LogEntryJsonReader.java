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
package org.nuxeo.audit.io;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_CATEGORY;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_COMMENT;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_TYPE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EXTENDED;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_LOG_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_PRINCIPAL_NAME;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;
import static org.nuxeo.audit.io.LogEntryJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.common.utils.DateUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2025.0
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LogEntryJsonReader extends EntityJsonReader<LogEntry> {

    public LogEntryJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected LogEntry readEntity(JsonNode jn) throws IOException {
        if (!(jn.has(LOG_EVENT_ID) && jn.has(LOG_EVENT_DATE))) {
            throw new NuxeoException("Unable to unmarshall LogEntry without eventId or eventDate", SC_BAD_REQUEST);
        }
        var logEntryBuilder = LogEntry.builder(jn.get(LOG_EVENT_ID).asText(),
                DateUtils.toDate(DateUtils.parseISODateTime(jn.get(LOG_EVENT_DATE).asText())));
        setIfNonNull(jn, LOG_ID, id -> logEntryBuilder.id(Long.parseLong(id)));
        setIfNonNull(jn, LOG_PRINCIPAL_NAME, logEntryBuilder::principalName);
        setIfNonNull(jn, LOG_LOG_DATE,
                value -> logEntryBuilder.logDate(DateUtils.toDate(DateUtils.parseISODateTime(value))));
        setIfNonNull(jn, LOG_DOC_UUID, logEntryBuilder::docUUID);
        setIfNonNull(jn, LOG_DOC_PATH, logEntryBuilder::docPath);
        setIfNonNull(jn, LOG_DOC_TYPE, logEntryBuilder::docType);
        setIfNonNull(jn, LOG_CATEGORY, logEntryBuilder::category);
        setIfNonNull(jn, LOG_COMMENT, logEntryBuilder::comment);
        setIfNonNull(jn, LOG_DOC_LIFE_CYCLE, logEntryBuilder::docLifeCycle);
        setIfNonNull(jn, LOG_REPOSITORY_ID, logEntryBuilder::repositoryId);
        if (jn.has(LOG_EXTENDED) && jn.get(LOG_EXTENDED).isObject()) {
            var extendedNode = jn.get(LOG_EXTENDED);
            for (var entry : extendedNode.properties()) {
                var entryNode = entry.getValue();
                var extendedValue = readExtendedEntity(entryNode);
                if (extendedValue != null) {
                    logEntryBuilder.extended(entry.getKey(), extendedValue);
                }
            }
        }
        return logEntryBuilder.build();
    }

    protected Serializable readExtendedEntity(JsonNode entryNode) throws IOException {
        return switch (entryNode.getNodeType()) {
            case NULL -> null;
            case STRING -> {
                var stringValue = entryNode.textValue();
                try {
                    yield Date.from(Instant.parse(stringValue));
                } catch (DateTimeParseException e) {
                    yield stringValue;
                }
            }
            case BOOLEAN -> entryNode.booleanValue();
            case NUMBER -> {
                Number numberValue = entryNode.numberValue();
                if (numberValue instanceof Integer integerValue) {
                    // convert it to long, it is the original type and json can't differentiate int and long
                    yield Long.valueOf(integerValue);
                }
                yield numberValue;
            }
            case BINARY -> SerializationUtils.deserialize(Base64.getDecoder().decode(entryNode.binaryValue()));
            case ARRAY -> StreamSupport.stream(entryNode.spliterator(), false)
                                       .map(ThrowableFunction.asFunction(this::readExtendedEntity))
                                       .filter(Objects::nonNull)
                                       .collect(Collectors.toCollection(ArrayList::new));
            case OBJECT -> entryNode.properties()
                                    .stream()
                                    .map(ThrowableFunction.asFunction(
                                            e -> new ExtendedWithKey(e.getKey(), readExtendedEntity(e.getValue()))))
                                    .filter(e -> e.value() != null)
                                    .collect(Collectors.toMap(ExtendedWithKey::key, ExtendedWithKey::value,
                                            (e1, e2) -> e1, HashMap::new));
            default ->
                throw new UnsupportedOperationException("Error when deserializing type: " + entryNode.getNodeType());
        };
    }

    protected void setIfNonNull(JsonNode jn, String key, Consumer<String> setter) {
        if (jn.hasNonNull(key)) {
            setter.accept(jn.get(key).asText());
        }
    }

    record ExtendedWithKey(String key, Serializable value) {
    }
}
