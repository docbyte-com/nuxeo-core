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

import org.nuxeo.launcher.info.ConfigurationInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @since 2025.0
 */
public class ConfigurationInfoSerializer extends StdSerializer<ConfigurationInfo> {

    public ConfigurationInfoSerializer() {
        super(ConfigurationInfo.class);
    }

    @Override
    public void serialize(ConfigurationInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("dbtemplate", value.dbtemplate);
        writeIfNonNull(gen, "basetemplates", "template", value.basetemplates);
        writeIfNonNull(gen, "pkgtemplates", "template", value.pkgtemplates);
        writeIfNonNull(gen, "usertemplates", "template", value.usertemplates);
        writeIfNonNull(gen, "profiles", "profile", value.profiles);
        writeIfNonNull(gen, "keyvals", "keyval", value.keyvals);
        writeIfNonNull(gen, "allkeyvals", "allkeyval", value.allkeyvals);
        gen.writeEndObject();
    }
}
