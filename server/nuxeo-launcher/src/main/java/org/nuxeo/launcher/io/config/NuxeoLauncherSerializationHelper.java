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

import java.io.IOException;
import java.util.Collection;

import org.nuxeo.common.function.ThrowableBiConsumer;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.0
 */
public final class NuxeoLauncherSerializationHelper {

    private NuxeoLauncherSerializationHelper() {
        // factory class
    }

    public static <V> void writeIfNonNull(ThrowableBiConsumer<String, V, IOException> consumer, String key, V value)
            throws IOException {
        if (value != null) {
            consumer.accept(key, value);
        }
    }

    public static void writeIfNonNull(JsonGenerator gen, String key, Object value) throws IOException {
        if (value != null) {
            gen.writeObjectField(key, value);
        }
    }

    public static void writeIfNonNull(JsonGenerator gen, String key, Object[] array) throws IOException {
        if (array != null) {
            if (array.length == 1) {
                gen.writeObjectField(key, array[0]);
            } else {
                gen.writeArrayFieldStart(key);
                for (var value : array) {
                    gen.writeObject(value);
                }
                gen.writeEndArray();
            }
        }
    }

    public static void writeIfNonNull(JsonGenerator gen, String key, Collection<?> list) throws IOException {
        if (list != null) {
            writeIfNonNull(gen, key, list.toArray(Object[]::new));
        }
    }

    public static void writeIfNonNull(JsonGenerator gen, String wrapperKey, String key, Collection<?> list)
            throws IOException {
        if (list != null) {
            if (list.isEmpty()) {
                gen.writeNullField(wrapperKey);
            } else if (list.size() == 1) {
                gen.writeObjectFieldStart(wrapperKey);
                gen.writeObjectField(key, list.iterator().next());
                gen.writeEndObject();
            } else {
                gen.writeObjectFieldStart(wrapperKey);
                gen.writeArrayFieldStart(key);
                for (var value : list) {
                    gen.writeObject(value);
                }
                gen.writeEndArray();
                gen.writeEndObject();
            }
        }
    }
}
