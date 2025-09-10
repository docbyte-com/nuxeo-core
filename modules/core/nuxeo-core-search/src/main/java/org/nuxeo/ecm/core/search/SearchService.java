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
     * Gets the default index for the default repository.
     *
     * @since 2025.1
     */
    default String getDefaultIndexName() {
        return getDefaultIndexName(getDefaultRepositoryName());
    }

    /**
     * Gets the default index for the given repository.
     *
     * @since 2025.1
     */
    String getDefaultIndexName(String repository);

    /**
     * Gets all available indexes for a repository.
     *
     * @since 2025.1
     */
    List<String> getIndexNames(String repository);

    /**
     * Gets the SearchIndex for the given index name.
     *
     * @since 2025.1
     * @throws NullPointerException if indexName is unknown.
     */
    SearchIndex getSearchIndex(String indexName);

    /**
     * Gets the default {@link SearchIndex} for the default repository.
     *
     * @deprecated since 2025.1, use {@link #getDefaultIndexName()} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    default SearchIndex getDefaultSearchIndex() {
        return getSearchIndex(getDefaultIndexName());
    }

    /**
     * Gets the default {@link SearchIndex} for a given repository.
     *
     * @deprecated since 2025.1, use {@link #getDefaultIndexName(String)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    SearchIndex getDefaultSearchIndexForRepository(String repository);

    /**
     * Gets the list of {@link SearchIndex} for a given repository.
     *
     * @deprecated since 2025.1, use {@link #getIndexNames(String)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    List<SearchIndex> getSearchIndexForRepository(String repository);

    /**
     * Executes a search query.
     * {@snippet :
     * var response = searchService.search(SearchQuery.builder("SELECT * FROM Document", session).build());
     * }
     *
     * @throws org.nuxeo.ecm.core.query.QueryParseException if the NXQL query is invalid
     * @throws org.nuxeo.runtime.RetryableException if search client cannot access the search backend
     * @throws SearchClientException if search client is not able to process the query
     */
    SearchResponse search(SearchQuery query);

    /**
     * Iterate on results for a scroll search. The end of scroll is reached when there is no more hit, i.e.
     * {@link SearchResponse#getHitsCount()} returns {@code 0}.
     *
     * @param scrollContext provided by the previous {@link SearchResponse#getScrollContext()};
     */
    SearchResponse searchScroll(SearchScrollContext scrollContext);

    /**
     * Explicit clear the search scroll context, without waiting for the scroll keep alive to timeout.
     *
     * @return {@code true} if the context is successfully cleared.
     */
    boolean clearSearchScroll(SearchScrollContext scrollContext);
}
