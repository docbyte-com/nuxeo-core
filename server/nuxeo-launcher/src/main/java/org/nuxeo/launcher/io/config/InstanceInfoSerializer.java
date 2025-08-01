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

import org.nuxeo.launcher.info.InstanceInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @since 2025.0
 */
public class InstanceInfoSerializer extends StdSerializer<InstanceInfo> {

    public InstanceInfoSerializer() {
        super(InstanceInfo.class);
    }

    @Override
    public void serialize(InstanceInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeIfNonNull(gen, "NUXEO_CONF", value.NUXEO_CONF);
        writeIfNonNull(gen, "NUXEO_HOME", value.NUXEO_HOME);
        writeIfNonNull(gen, "clid", value.clid);
        writeIfNonNull(gen, "distribution", value.distribution);
        writeIfNonNull(gen, "packages", "package", value.packages);
        writeIfNonNull(gen, "configuration", value.config);
        gen.writeEndObject();
    }
}
