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
package org.nuxeo.ecm.core.search.client.repository;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.search.SearchClientFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 2025.0
 */
public class RepositorySearchClientFactory extends DefaultComponent
        implements SearchClientFactory<RepositorySearchClient> {

    protected static final String XP_SEARCH_CLIENT = "searchClient";

    protected Map<String, RepositorySearchClient> searchClients;

    @Override
    public void start(ComponentContext context) {
        searchClients = this.<RepositorySearchClientDescriptor> getDescriptors(XP_SEARCH_CLIENT)
                            .stream()
                            .filter(RepositorySearchClientDescriptor::isEnabled)
                            .map(RepositorySearchClient::new)
                            .collect(Collectors.toMap(RepositorySearchClient::getName, Function.identity()));
    }

    @Override
    public void stop(ComponentContext context) {
        searchClients = null;
    }

    @Override
    public RepositorySearchClient getSearchClient(String name) {
        var client = searchClients.get(name);
        if (client == null) {
            throw new IllegalStateException("The search client with name: " + name + " does not exist");
        }
        return client;
    }
}
