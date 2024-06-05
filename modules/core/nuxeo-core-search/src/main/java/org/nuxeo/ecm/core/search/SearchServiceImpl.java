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
package org.nuxeo.ecm.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LogManager.getLogger(SearchServiceImpl.class);

    protected final Map<String, SearchClient> searchClients = new HashMap<>();

    protected final List<String> searchClientNames;

    protected final String defaultClientName;

    protected final Map<String, String> indexToClient = new HashMap<>();

    protected final Map<String, SearchIndex> repoToDefaultSearchIndex = new HashMap<>();

    protected final Map<String, List<SearchIndex>> repoToSearchIndices = new HashMap<>();

    protected final String defaultRepository;

    public SearchServiceImpl(List<SearchClientDescriptor> clients) {
        RepositoryManager repoManager = Framework.getService(RepositoryManager.class);
        if (repoManager != null) {
            defaultRepository = repoManager.getDefaultRepositoryName();
        } else if (Framework.isTestModeSet()) {
            defaultRepository = "test";
        } else {
            throw new IllegalStateException("No repository manager available to get the default repository");
        }
        String defaultName = null;
        for (SearchClientDescriptor descriptor : clients) {
            log.debug("Creating SearchClient: '{}', class: {}", descriptor::getId, descriptor::getClientClass);
            try {
                SearchClient searchClient = descriptor.getClientClass()
                                                      .getDeclaredConstructor(SearchClientDescriptor.class)
                                                      .newInstance(descriptor);
                searchClients.put(descriptor.getId(), searchClient);
                if (descriptor.isDefault() || defaultName == null) {
                    defaultName = descriptor.getName();
                }
                if (!searchClient.isReady()) {
                    throw new IllegalStateException("SearchClient: " + searchClient + " is not ready.");
                }
                initIndexes(searchClient, descriptor.getIndexes());
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(
                        "Invalid SearchClient class: " + descriptor.getClientClass() + " for: " + descriptor.getId(),
                        e);
            }
        }
        searchClientNames = List.copyOf(searchClients.keySet());
        defaultClientName = defaultName;
    }

    protected void initIndexes(SearchClient client, List<SearchIndexDescriptor> indexes) {
        String defaultIndex = null;
        for (SearchIndexDescriptor descriptor : indexes) {
            if (!descriptor.isEnabled()) {
                continue;
            }
            if (descriptor.canCreateIndex()) {
                log.debug("Creating index: {} on repository: {} if not exists for client: {}", descriptor.getId(),
                        descriptor.getRepositoryName(), client.getName());
                client.createIndexIfNotExists(descriptor.getId(), descriptor.getRepositoryName(),
                        descriptor.getSettings(), descriptor.getMapping());
            }
            indexToClient.put(descriptor.getId(), client.getName());
            SearchIndex index = SearchIndex.of(descriptor.getRepositoryName(), client.getName(), descriptor.getId());
            String repo = descriptor.getRepositoryName();
            repoToSearchIndices.computeIfAbsent(repo, k -> new ArrayList<>()).add(index);
            if (descriptor.isDefault() || defaultIndex == null) {
                defaultIndex = descriptor.getId();
                repoToDefaultSearchIndex.put(repo, index);
            }
        }
    }

    @Override
    public String getDefaultRepositoryName() {
        return defaultRepository;
    }

    @Override
    public Set<String> getRepositoryNames() {
        return repoToSearchIndices.keySet();
    }

    @Override
    public SearchIndex getDefaultSearchIndexForRepository(String repository) {
        return repoToDefaultSearchIndex.get(repository);
    }

    @Override
    public List<SearchIndex> getSearchIndexForRepository(String repository) {
        return repoToSearchIndices.getOrDefault(repository, Collections.emptyList());
    }

}
