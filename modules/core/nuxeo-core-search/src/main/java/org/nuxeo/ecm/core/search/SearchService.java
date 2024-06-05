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

import java.util.List;
import java.util.Set;

/**
 * A Service to search on Nuxeo repositories.
 * 
 * @since 2025.0
 */
public interface SearchService {

    /**
     * Gets the default repository name.
     */
    String getDefaultRepositoryName();

    /**
     * Gets the set of repository names.
     */
    Set<String> getRepositoryNames();

    /**
     * Gets the default {@link SearchIndex} for the default repository.
     */
    default SearchIndex getDefaultSearchIndex() {
        return getDefaultSearchIndexForRepository(getDefaultRepositoryName());
    }

    /**
     * Gets the default {@link SearchIndex} for a given repository.
     */
    SearchIndex getDefaultSearchIndexForRepository(String repository);

    /**
     * Gets the list of {@link SearchIndex} for a given repository.
     */
    List<SearchIndex> getSearchIndexForRepository(String repository);
}
