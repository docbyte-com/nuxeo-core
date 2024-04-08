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
package org.nuxeo.launcher.io;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED;
import static com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION;
import static org.nuxeo.launcher.NuxeoLauncher.EXIT_CODE_NOT_RUNNING;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.launcher.NuxeoLauncherException;
import org.nuxeo.launcher.io.config.NuxeoLauncherModule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * @since 2025.0
 */
public class NuxeoLauncherTechPrinter {

    protected final Format format;

    public NuxeoLauncherTechPrinter(Format format) {
        this.format = format;
    }

    public void print(Object object, OutputStream out) {
        try {
            var mapper = switch (format) {
                case JSON -> new ObjectMapper();
                case XML -> XmlMapper.builder().enable(WRITE_XML_DECLARATION).build();
            };
            mapper.registerModule(new NuxeoLauncherModule());
            mapper.enable(WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(out, object);
        } catch (IOException e) {
            throw new NuxeoLauncherException("Output serialization failed: " + e.getMessage(), EXIT_CODE_NOT_RUNNING,
                    e);
        }
    }

    public enum Format {
        JSON, XML
    }
}
