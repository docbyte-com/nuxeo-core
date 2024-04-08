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
package org.nuxeo.ecm.webengine.app;

import java.util.function.Supplier;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

/**
 * @since 2025.0
 */
@Provider
public class MyObjectPerRequestProvider implements Supplier<MyObject>, Feature {

    @Override
    public MyObject get() {
        return new MyObject(getClass().getSimpleName(), hashCode());
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(MyObjectPerRequestProvider.this).to(MyObject.class).in(RequestScoped.class);
            }
        });
        return true;
    }
}
