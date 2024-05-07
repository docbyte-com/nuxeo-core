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

import static org.nuxeo.launcher.NuxeoLauncher.EXIT_CODE_NOT_RUNNING;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.nuxeo.launcher.NuxeoLauncherException;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.MessageInfo;
import org.nuxeo.launcher.info.PackageInfo;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.json.impl.writer.JsonXmlStreamWriter;
import com.sun.xml.bind.marshaller.MinimumEscapeHandler;

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
            var jaxbContext = JAXBContext.newInstance( //
                    CommandInfo.class, //
                    CommandSetInfo.class, //
                    ConfigurationInfo.class, //
                    DistributionInfo.class, //
                    InstanceInfo.class, //
                    KeyValueInfo.class, //
                    PackageInfo.class, //
                    MessageInfo.class //
            );
            printXMLOutput(jaxbContext, object, out);
        } catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
            throw new NuxeoLauncherException("Output serialization failed: " + e.getMessage(), EXIT_CODE_NOT_RUNNING,
                    e);
        }
    }

    /**
     * @since 8.3
     */
    protected void printXMLOutput(JAXBContext context, Object object, OutputStream out)
            throws XMLStreamException, FactoryConfigurationError, JAXBException {
        XMLStreamWriter writer = format == Format.JSON ? jsonWriter(context, out) : xmlWriter(context, out);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        if (format == Format.JSON) {
            // replace the character escape handler as the one for XML doesn't correctly print newline in JSON
            marshaller.setProperty("com.sun.xml.bind.characterEscapeHandler", MinimumEscapeHandler.theInstance);
        }
        marshaller.marshal(object, writer);
    }

    protected XMLStreamWriter jsonWriter(JAXBContext context, OutputStream out) {
        JSONConfiguration config = JSONConfiguration.mapped()
                                                    .rootUnwrapping(true)
                                                    .attributeAsElement("key", "value")
                                                    .build();
        config = JSONConfiguration.createJSONConfigurationWithFormatted(config, true);
        return JsonXmlStreamWriter.createWriter(new OutputStreamWriter(out), config, "");
    }

    protected XMLStreamWriter xmlWriter(JAXBContext context, OutputStream out)
            throws XMLStreamException, FactoryConfigurationError {
        return XMLOutputFactory.newInstance().createXMLStreamWriter(out);
    }

    public enum Format {
        JSON, XML
    }
}
