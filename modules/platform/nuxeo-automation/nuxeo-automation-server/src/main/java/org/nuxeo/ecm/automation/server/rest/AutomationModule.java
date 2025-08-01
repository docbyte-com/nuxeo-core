/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.rest;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.io.rest.JsonFactoryProvider;
import org.nuxeo.ecm.automation.io.rest.operations.MultiPartExecutionRequestReader;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.rest.coreiodelegate.CoreIODelegate;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationModule extends WebEngineModule {

    protected static final Logger log = LogManager.getLogger(AutomationModule.class);

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> result = super.getClasses();
        result.add(MultiPartExecutionRequestReader.class);

        // setup readers & writers from contribution
        var automationServer = Framework.getService(AutomationServer.class);
        result.addAll(automationServer.getReaders());
        result.addAll(automationServer.getWriters());
        result.add(AutomationServiceProvider.class);
        result.add(AutomationServerProvider.class);
        result.add(JsonFactoryProvider.class);
        result.add(CoreSessionProvider.class);
        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(CoreIODelegate.class);
        return result;
    }
}
