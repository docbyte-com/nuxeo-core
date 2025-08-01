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
package org.nuxeo.audit.opensearch1;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.audit.service.AuditBackendFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;

/**
 * @since 2025.0
 */
public class OpenSearchAuditBackendFactory extends DefaultComponent
        implements AuditBackendFactory<OpenSearchAuditBackend> {

    protected static final String XP_BACKEND = "backend";

    protected final Map<String, OpenSearchAuditBackend> backends = new HashMap<>();

    @Override
    public void start(ComponentContext context) {
        this.<OpenSearchAuditBackendDescriptor> getDescriptors(XP_BACKEND)
            .stream()
            .filter(OpenSearchAuditBackendDescriptor::iEnabled)
            .map(this::instantiateBackend)
            .forEach(b -> backends.put(b.name(), b.backend()));
    }

    protected OpenSearchAuditBackendWithName instantiateBackend(OpenSearchAuditBackendDescriptor descriptor) {
        var client = Framework.getService(OpenSearchClientService.class).getClient(descriptor.getClientId());
        return new OpenSearchAuditBackendWithName(descriptor.getName(),
                new OpenSearchAuditBackend(client, descriptor.getIndexName()));
    }

    @Override
    public void stop(ComponentContext context) {
        backends.clear();
    }

    @Override
    public OpenSearchAuditBackend getAuditBackend(String name) {
        var backend = backends.get(name);
        if (backend == null) {
            throw new IllegalStateException("The audit backend with name: " + name + " does not exist");
        }
        return backend;
    }

    protected record OpenSearchAuditBackendWithName(String name, OpenSearchAuditBackend backend) {
    }
}
