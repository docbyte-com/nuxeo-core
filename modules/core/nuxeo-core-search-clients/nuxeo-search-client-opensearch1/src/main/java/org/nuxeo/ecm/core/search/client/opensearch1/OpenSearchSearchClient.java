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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import org.nuxeo.ecm.core.search.AbstractSearchClient;
import org.nuxeo.ecm.core.search.SearchClientDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;
import org.nuxeo.runtime.opensearch1.OpenSearchComponent;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;

/**
 * @since 2025.0
 */
public class OpenSearchSearchClient extends AbstractSearchClient {

    protected final OpenSearchClient client;

    public OpenSearchSearchClient(SearchClientDescriptor descriptor) {
        super(descriptor);
        client = Framework.getService(OpenSearchClientService.class)
                          .getClient(descriptor.getConnectionOptions().getOrDefault("clientId", "search/" + name));
    }

    @Override
    public boolean isReady() {
        return client.isReady();
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case INDEXING -> true;
        };
    }

    @Override
    public boolean createIndexIfNotExists(String name, String repository, String settings, String mapping) {
        throw new IllegalStateException("Index creation is handled by OpenSearchComponent");
    }

    @Override
    public void dropIndex(String name) {
        client.dropIndex(name);
    }

    @Override
    public void dropAndInitIndex(String indexName) {
        ((OpenSearchComponent) Framework.getService(OpenSearchClientService.class)).dropAndInitIndex(indexName);
    }

    @Override
    public void close() {
        // nothing, the client is handled by its Nuxeo component
    }

    @Override
    public String toString() {
        return "<OpenSearchSearchClient name=\"" + name + "\" clientId=\"" + client.getId() + "\" />";
    }
}
