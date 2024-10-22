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

/**
 * Interface used by the SearchService to access an external Search Cluster.
 *
 * @since 2025.0
 */
public interface SearchClient extends AutoCloseable {

    /**
     * Gets the client name.
     */
    String getName();

    /**
     * Is the client ready and the search engine healthy to serve requests.
     */
    boolean isReady();

    /**
     * Checks whether the client has the capability.
     */
    boolean hasCapability(Capability capability);

    /**
     * Creates a new index if it not exists and the client has indexing capability.
     *
     * @return true if index is created.
     */
    boolean createIndexIfNotExists(String name, String repository, String settings, String mapping);

    /**
     * Drops an index.
     */
    void dropIndex(String name);

    /**
     * Recreate an existing index using the same settings and mapping.
     */
    void dropAndInitIndex(String indexName);

    /**
     * Index documents.
     *
     * @throws SearchClientRetryableException when search cluster is not ready
     */
    void indexDocuments(BulkIndexingRequest request) throws SearchClientRetryableException;

    /**
     * Refreshes an index so newly indexed documents are searchable.
     */
    void refresh(String indexName);

    /**
     * Returns a Json document representation or null if not found.
     */
    String getDocument(String indexName, String documentId);

    /**
     * Returns the version which is the timestamp when document was loaded for indexing
     */
    Long getDocumentVersion(String indexName, String documentId);

    @Override
    void close();

    enum Capability {
        INDEXING
    }
}
