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

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public interface SearchQuery {

    /**
     * Gets the list of searchIndex to perform the query. If there are more than one item it's a multi repository
     * search, note that all search indexes share the same search client.
     */
    List<SearchIndex> getSearchIndexes();

    default boolean isMultiRepositories() {
        return getSearchIndexes().size() > 1;
    }

    SQLQuery getQuery();

    NuxeoPrincipal getPrincipal();

    int getOffset();

    int getLimit();

    boolean isScrollSearch();

    Duration getScrollKeepAlive();

    int getScrollSize();

    List<String> getHighlights();

    List<Aggregate<? extends Bucket>> getAggregates();

    /**
     * Returns the fields present in the {@code SELECT} clause with their type if it exists
     */
    Map<String, Type> getSelectFields();

    /**
     * Returns a query builder initialized with an NXQL query, the current principal and the default search index of the
     * default repository.
     *
     * @since 2025.1
     */
    static SearchQueryImpl.Builder builder(String nxql) {
        return builder(nxql, NuxeoPrincipal.getCurrent());
    }

    /**
     * Returns a query builder initialized with an NXQL query, principal and repository taken from the session, the
     * search index is the default one for the session's repository.
     *
     * @since 2025.1
     */
    static SearchQueryImpl.Builder builder(String nxql, CoreSession session) {
        var index = Framework.getService(SearchService.class).getDefaultIndexName(session.getRepositoryName());
        return new SearchQueryImpl.Builder(nxql, session.getPrincipal()).index(index);
    }

    /**
     * Returns a query builder initialized with an NXQL query on a given principal.
     *
     * @since 2025.1
     */
    static SearchQueryImpl.Builder builder(String nxql, NuxeoPrincipal principal) {
        return new SearchQueryImpl.Builder(nxql, principal);
    }

    /**
     * @deprecated since 2025.1, use {@link #builder(String, CoreSession)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    static SearchQueryImpl.Builder builder(CoreSession session, String nxql) {
        return builder(nxql, session);
    }

    /**
     * @deprecated since 2025.1, use {@link #builder(String)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    static SearchQueryImpl.Builder builder(SearchIndex searchIndex, String nxql) {
        return builder(nxql).searchIndex(searchIndex);
    }

    /**
     * @deprecated since 2025.1, use {@link #builder(String, NuxeoPrincipal)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    static SearchQueryImpl.Builder builder(SearchIndex searchIndex, String nxql, NuxeoPrincipal principal) {
        return builder(nxql, principal).searchIndex(searchIndex);
    }

    /**
     * @deprecated since 2025.1, use {@link #builder(String, NuxeoPrincipal)} instead.
     */
    @Deprecated(since = "2025.1", forRemoval = true)
    static SearchQueryImpl.Builder builder(List<SearchIndex> searchIndexes, String nxql, NuxeoPrincipal principal) {
        return builder(nxql, principal).searchIndex(searchIndexes);
    }
}
