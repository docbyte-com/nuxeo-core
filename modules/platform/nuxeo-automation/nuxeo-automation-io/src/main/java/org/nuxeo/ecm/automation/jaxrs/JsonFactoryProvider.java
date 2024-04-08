/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.jaxrs;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * @since 5.7.3
 */
@Provider
public class JsonFactoryProvider implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(() -> Framework.getService(JsonFactoryManager.class).getJsonFactory()).to(JsonFactory.class)
                                                                                                  .in(Singleton.class);
            }
        });
        return true;
    }
}
