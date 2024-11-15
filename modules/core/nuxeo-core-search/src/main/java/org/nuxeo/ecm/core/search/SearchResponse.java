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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 2025.0
 */
public interface SearchResponse {

    /**
     * Gets the search duration in millisecond.
     */
    int getElapsedMillis();

    /**
     * Returns true if the search failed.
     */
    boolean isFailed();

    /**
     * Returns the error code.
     */
    int getErrorCode();

    /**
     * Returns the error message if any.
     */
    String getErrorMessage();

    /**
     * Returns true if the search has failed because of a timed out.
     */
    boolean isTimedOut();

    /**
     * Returns the number of returned search hits, taking in account limits.
     */
    long getHitsCount();

    /**
     * Returns the search hits.
     */
    List<SearchHit> getHits();

    /**
     * Returns search hits as a partial list.
     */
    PartialList<Map<String, Serializable>> getHitsAsMap();

    /**
     * Returns search hits as an iterator, the query must be a scroll query.
     *
     * @throws IllegalArgumentException if the query is not of scroll type.
     */
    IterableQueryResult getHitsAsIterator();

    /**
     * Get the total number of match for the query. Limit is not taken in account.
     *
     * @return -1 if unknown.
     */
    long getTotal();

    /**
     * Returns true if the total is accurate, i.e. not an estimation.
     */
    boolean isTotalAccurate();

    /**
     * Returns a scroll context to use in {@link SearchService#searchScroll(SearchScrollContext)} to fetch the next
     * batch of results.
     */
    @Nullable
    SearchScrollContext getScrollContext();

    /**
     * Returns the aggregates.
     */
    List<Aggregate<? extends Bucket>> getAggregates();

    /**
     * Loads search hits from repository as a DocumentModelList. Can be an expensive operation.
     */
    DocumentModelList loadDocuments(CoreSession session);

    /**
     * @return a {@link SearchResponse} builder for an error response
     */
    static SearchResponseImpl.Builder builder(int errorCode, String errorMessage, int elapsedMillis) {
        return new SearchResponseImpl.Builder(errorCode, errorMessage, elapsedMillis);
    }

    /**
     * @return a {@link SearchResponse} builder for a successful response
     */
    static SearchResponseImpl.Builder builder(List<SearchHit> hits, int elapsedMillis) {
        return new SearchResponseImpl.Builder(hits, elapsedMillis);
    }
}
