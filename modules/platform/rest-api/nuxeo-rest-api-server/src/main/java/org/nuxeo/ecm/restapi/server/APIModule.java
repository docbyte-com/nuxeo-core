/*
 * (C) Copyright 2013-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server;

import java.util.Set;

import org.nuxeo.ecm.automation.io.rest.documents.BusinessAdapterListWriter;
import org.nuxeo.ecm.automation.io.rest.operations.MultiPartExecutionRequestReader;
import org.nuxeo.ecm.restapi.io.conversion.ConversionScheduledWriter;
import org.nuxeo.ecm.restapi.io.conversion.ConversionStatusWithResultWriter;
import org.nuxeo.ecm.restapi.io.types.DocumentTypesWriter;
import org.nuxeo.ecm.restapi.io.types.FacetsWriter;
import org.nuxeo.ecm.restapi.io.types.SchemasWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.rest.coreiodelegate.CoreIODelegate;

/**
 * @since 5.8
 */
public class APIModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        result.add(MultiPartExecutionRequestReader.class);
        // writers
        result.add(BusinessAdapterListWriter.class);
        result.add(SchemasWriter.class);
        result.add(DocumentTypesWriter.class);
        result.add(FacetsWriter.class);
        result.add(ConversionScheduledWriter.class);
        result.add(ConversionStatusWithResultWriter.class);
        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(CoreIODelegate.class);
        return result;
    }
}
