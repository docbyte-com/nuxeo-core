/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */
package org.nuxeo.ecm.platform.audit.io;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializer class for extended info to a JSON object
 *
 * @since 9.3
 * @deprecated since 2025, use {@link org.nuxeo.audit.io.LogEntryJsonWriter} with help of nuxeo-core-io instead
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
public class ExtendedInfoSerializer extends JsonSerializer<ExtendedInfo> {

    protected static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().appendInstant(3)
                                                                                            .toFormatter();

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void serialize(ExtendedInfo info, JsonGenerator jg, SerializerProvider provider) throws IOException {
        jg.setCodec(MAPPER);
        switch (info) {
            case ExtendedInfoImpl.DateInfo dateInfo ->
                jg.writeObject(DATE_FORMATTER.format(dateInfo.getDateValue().toInstant()));
            case ExtendedInfoImpl.BlobInfo blobInfo ->
                jg.writeObject(Base64.encodeBase64(SerializationUtils.serialize(blobInfo.getBlobValue())));
            case ExtendedInfoImpl.StringInfo stringInfo -> writeString(jg, stringInfo.getStringValue().trim());
            case null, default -> { // ESExtendedInfo or MongoDBExtendedInfo
                if (info != null && info.getSerializableValue() instanceof String serializableValue) {
                    writeString(jg, serializableValue);
                } else {
                    jg.writeObject(info == null ? null : info.getSerializableValue());
                }
            }
        }
    }

    private void writeString(JsonGenerator jg, String stringValue) throws IOException {
        if ((stringValue.startsWith("{") && stringValue.endsWith("}"))
                || (stringValue.startsWith("[") && stringValue.endsWith("]"))) {
            try {
                MAPPER.readTree(stringValue);
                jg.writeRawValue(stringValue);
            } catch (IOException e) {
                // If the value represents an invalid JSON, send a null value to ES to prevent potential
                // mapping exceptions
                jg.writeObject(null);
            }
        } else {
            jg.writeString(stringValue);
        }
    }

}
