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

import java.util.LinkedHashSet;
import java.util.Set;

import org.nuxeo.ecm.automation.io.rest.documents.BusinessAdapterListWriter;
import org.nuxeo.ecm.automation.io.rest.operations.MultiPartFormRequestReader;
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
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new LinkedHashSet<>();

        // writers
        result.add(new BusinessAdapterListWriter());
        result.add(new SchemasWriter());
        result.add(new DocumentTypesWriter());
        result.add(new FacetsWriter());
        result.add(new ConversionScheduledWriter());
        result.add(new ConversionStatusWithResultWriter());

        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new CoreIODelegate());

        return result;
    }
}
