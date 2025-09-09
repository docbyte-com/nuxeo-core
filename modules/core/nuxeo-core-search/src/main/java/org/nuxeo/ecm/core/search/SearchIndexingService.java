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

/**
 * Search Indexing Service. This service is for internal usage since indexing is done automatically.
 * 
 * @since 2025.0
 */
public interface SearchIndexingService {

    /**
     * Internal: index documents.
     */
    BulkIndexingResponse indexDocuments(BulkIndexingRequest request);

    /**
     * Internal: reindex the repository
     *
     * @return the bulk command id in charge of reindexing
     */
    String reindexRepository(String repository);

    /**
     * Internal: reindex documents of the given repository according to the given NXQL query.
     *
     * @param repository the repository name
     * @param nxql the NXQL query
     * @return the bulk command id in charge of reindexing
     */
    default String reindexDocuments(String repository, String nxql) {
        return reindexDocuments(repository, nxql, -1);
    }

    /**
     * Internal: reindex documents of the given repository according to the given NXQL query using a limit.
     *
     * @since 2025.8
     * @param repository the repository name
     * @param nxql the NXQL query
     * @param queryLimit limit applied to the number of documents to index, -1 for no limit
     * @return the bulk command id in charge of reindexing
     */
    String reindexDocuments(String repository, String nxql, long queryLimit);

    /**
     * Refreshes an index so newly indexed documents are searchable.
     */
    void refresh(SearchIndex index);

    /**
     * Internal: Gets a given search client.
     */
    SearchClient getClient(String clientName);

    /**
     * Waits for completion of indexing activity for testing purpose only.
     *
     * @param duration the duration to wait
     * @return {@code true} if all indexing processing completed or {@code false} if one or more has not finished after
     *         the timeout
     */
    boolean await(Duration duration) throws InterruptedException;
}
