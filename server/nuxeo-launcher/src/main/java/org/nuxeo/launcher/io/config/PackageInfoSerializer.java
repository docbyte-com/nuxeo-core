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

import org.nuxeo.launcher.info.PackageInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @since 2025.0
 */
public class PackageInfoSerializer extends StdSerializer<PackageInfo> {

    public PackageInfoSerializer() {
        super(PackageInfo.class);
    }

    @Override
    public void serialize(PackageInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeIfNonNull(gen, "id", value.id);
        writeIfNonNull(gen, "state", value.state);
        writeIfNonNull(gen, "version", value.version);
        writeIfNonNull(gen, "name", value.name);
        writeIfNonNull(gen, "type", value.type);
        writeIfNonNull(gen, "targetPlatforms", value.targetPlatforms);
        writeIfNonNull(gen, "vendor", value.vendor);
        gen.writeStringField("supportsHotReload", String.valueOf(value.supportsHotReload));
        writeIfNonNull(gen, "provides", value.provides);
        writeIfNonNull(gen, "dependencies", value.dependencies);
        writeIfNonNull(gen, "conflicts", value.conflicts);
        writeIfNonNull(gen, "title", value.title);
        writeIfNonNull(gen, "description", value.description);
        writeIfNonNull(gen, "licenseType", value.licenseType);
        writeIfNonNull(gen, "licenseUrl", value.licenseUrl);
        writeIfNonNull(gen, "templates", value.templates);
        gen.writeEndObject();
    }
}
