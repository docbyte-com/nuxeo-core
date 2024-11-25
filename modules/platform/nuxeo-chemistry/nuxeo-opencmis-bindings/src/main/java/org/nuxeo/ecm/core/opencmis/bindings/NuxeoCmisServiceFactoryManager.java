/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.util.Map;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the definition
 */
public class NuxeoCmisServiceFactoryManager extends DefaultComponent {

    private static final String XP_FACTORY = "factory";

    /**
     * Gets the {@link NuxeoCmisServiceFactory} based on contributed {@link NuxeoCmisServiceFactoryDescriptor}s.
     */
    public NuxeoCmisServiceFactory getNuxeoCmisServiceFactory() {
        NuxeoCmisServiceFactoryDescriptor descriptor = getDescriptor(XP_FACTORY, UNIQUE_DESCRIPTOR_ID);

        Class<? extends NuxeoCmisServiceFactory> factoryClass = descriptor.getFactoryClass();
        Map<String, String> factoryParameters = descriptor.getFactoryParameters();
        try {
            var nuxeoCmisServiceFactory = factoryClass.getDeclaredConstructor().newInstance();
            nuxeoCmisServiceFactory.init(factoryParameters);
            return nuxeoCmisServiceFactory;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate nuxeoCmisServiceFactory: " + factoryClass.getName(), e);
        }
    }

}
