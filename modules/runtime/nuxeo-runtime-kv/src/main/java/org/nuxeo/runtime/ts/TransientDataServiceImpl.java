/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.ts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 2025.8
 */
public class TransientDataServiceImpl extends DefaultComponent implements TransientDataService {

    public static final String XP_CONFIG = "configuration";

    public static final String DEFAULT_STORE_ID = "default";

    protected Map<String, TransientDataStoreProvider> providers = new ConcurrentHashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.TRANSIENT_DATA_STORE;
    }

    @Override
    public void stop(ComponentContext context) {
        providers.clear();
    }

    @Override
    public TransientDataStore getStore(String name) {
        return providers.computeIfAbsent(name, this::instantiateStore);
    }

    protected TransientDataStoreProvider instantiateStore(String name) {
        TransientDataStoreDescriptor descriptor = getDescriptor(XP_CONFIG, name);
        if (descriptor == null) {
            descriptor = new TransientDataStoreDescriptor();
        }
        // all stores inherit from default, this allows to set a default provider implementation
        descriptor = getDefaultDescriptor().merge(descriptor);
        // restore descriptor name
        descriptor.name = name;
        try {
            return descriptor.provider.klass.getDeclaredConstructor(TransientDataStoreDescriptor.class)
                                            .newInstance(descriptor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException("Unable to instantiate the TransientDataStore with name: " + name, e);
        }
    }

    protected TransientDataStoreDescriptor getDefaultDescriptor() {
        TransientDataStoreDescriptor descriptor = getDescriptor(XP_CONFIG, DEFAULT_STORE_ID);
        if (descriptor == null) {
            throw new RuntimeException("Missing configuration for default transient data store");
        }
        return descriptor;
    }
}
