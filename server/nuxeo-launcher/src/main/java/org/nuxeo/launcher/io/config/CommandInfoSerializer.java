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
package org.nuxeo.launcher.io.config;

import static org.nuxeo.launcher.io.config.NuxeoLauncherSerializationHelper.writeIfNonNull;

import java.io.IOException;

import org.nuxeo.common.function.ThrowableBiConsumer;
import org.nuxeo.launcher.info.CommandInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * @since 2025.0
 */
public class CommandInfoSerializer extends StdSerializer<CommandInfo> {

    public CommandInfoSerializer() {
        super(CommandInfo.class);
    }

    @Override
    public void serialize(CommandInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // first deduce if we're writing xml or json to handle annotations
        ThrowableBiConsumer<String, String, IOException> attributeStringWriter;
        if (gen instanceof ToXmlGenerator xmlGen) {
            attributeStringWriter = (k, v) -> {
                xmlGen.setNextIsAttribute(true);
                xmlGen.writeStringField(k, v);
                xmlGen.setNextIsAttribute(false);
            };
        } else {
            attributeStringWriter = (k, v) -> gen.writeStringField("@" + k, v);
        }
        gen.writeStartObject();
        writeIfNonNull(attributeStringWriter, "name", value.name);
        writeIfNonNull(attributeStringWriter, "param", value.param);
        attributeStringWriter.accept("exitCode", String.valueOf(value.exitCode));
        writeIfNonNull(attributeStringWriter, "id", value.id);
        attributeStringWriter.accept("pending", String.valueOf(value.pending));
        writeIfNonNull(gen, "messages", "message", value.messages);
        writeIfNonNull(gen, "packages", "package", value.packages);
        gen.writeEndObject();
    }
}
