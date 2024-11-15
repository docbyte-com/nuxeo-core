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

import static org.nuxeo.ecm.core.search.client.opensearch1.OpenSearchQueryTransformer.getKeepAlive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.search.AbstractSearchClient;
import org.nuxeo.ecm.core.search.BulkIndexingRequest;
import org.nuxeo.ecm.core.search.IndexingRequest;
import org.nuxeo.ecm.core.search.SearchClientDescriptor;
import org.nuxeo.ecm.core.search.SearchClientException;
import org.nuxeo.ecm.core.search.SearchClientRetryableException;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.ecm.core.search.SearchScrollContext;
import org.nuxeo.runtime.RetryableException;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;
import org.nuxeo.runtime.opensearch1.OpenSearchComponent;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.VersionType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

/**
 * @since 2025.0
 */
public class OpenSearchSearchClient extends AbstractSearchClient {

    private static final Logger log = LogManager.getLogger(OpenSearchSearchClient.class);

    public static final String ECM_ANCESTOR_FIELDS = "ecm:ancestorId";

    protected static final OpenSearchQueryTransformer QUERY_TRANSFORMER = new OpenSearchQueryTransformer();

    protected static final OpenSearchResponseTransformer RESPONSE_TRANSFORMER = new OpenSearchResponseTransformer();

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
            case HIGHLIGHT -> true;
            case AGGREGATE -> true;
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
    public void indexDocuments(BulkIndexingRequest request) throws SearchClientRetryableException {
        String indexName = request.getSearchIndex().index();
        BulkRequest bulkRequest = new BulkRequest();
        int count = 0;
        for (IndexingRequest item : request.getRequests()) {
            if (item.isDelete()) {
                bulkRequest.add(new DeleteRequest(indexName, item.getDocumentId()).versionType(VersionType.EXTERNAL)
                                                                                  .version(request.getVersion()));
                if (item.isDeleteRecursive()) {
                    // recurse delete are processed first before bulk index/delete command
                    var query = QueryBuilders.constantScoreQuery(
                            QueryBuilders.termQuery(ECM_ANCESTOR_FIELDS, item.getDocumentId()));
                    doRecurseDelete(indexName, query);
                }
                count++;
            } else {
                bulkRequest.add(new IndexRequest(indexName).id(item.getDocumentId())
                                                           .versionType(VersionType.EXTERNAL)
                                                           .version(request.getVersion())
                                                           .source(item.getSource(), XContentType.JSON));
                count++;
            }
        }
        if (request.isRefresh()) {
            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        }
        // TODO check bulk request size and split if necessary
        if (count > 0) {
            // TODO add a BulkResponse with error, missing, indexed docs
            // the goal is to be able to retry only the failed documents one by one.
            try {
                client.bulk(bulkRequest);
            } catch (RetryableException e) {
                throw new SearchClientRetryableException(e.getMessage());
            }
        }
    }

    @Override
    public void refresh(String indexName) {
        client.refresh(indexName);
    }

    protected void doRecurseDelete(String indexName, QueryBuilder queryBuilder) throws SearchClientRetryableException {
        log.debug("delete recurse: {}", queryBuilder);
        try {
            TimeValue keepAlive = TimeValue.timeValueMinutes(1);
            SearchSourceBuilder search = new SearchSourceBuilder().size(100).query(queryBuilder).fetchSource(false);
            SearchRequest request = new SearchRequest(indexName).scroll(keepAlive).source(search);
            request.scroll(TimeValue.timeValueMinutes(1));

            org.opensearch.action.search.SearchResponse response;
            for (response = client.search(request); //
                    response.getHits().getHits().length > 0; //
                    response = client.scroll(new SearchScrollRequest(response.getScrollId()).scroll(keepAlive))) {
                // Build bulk delete request
                BulkRequest bulkRequest = new BulkRequest();
                for (org.opensearch.search.SearchHit hit : response.getHits().getHits()) {
                    bulkRequest.add(new DeleteRequest(hit.getIndex(), hit.getId()));
                }
                log.debug("Bulk delete request on {} elements", bulkRequest.numberOfActions());
                // Run bulk delete request
                client.bulk(bulkRequest);
            }
            // Close the scroll
            ClearScrollRequest closeScrollRequest = new ClearScrollRequest();
            closeScrollRequest.addScrollId(response.getScrollId());
            client.clearScroll(closeScrollRequest);
        } catch (RetryableException e) {
            throw new SearchClientRetryableException(e.getMessage());
        } catch (RuntimeServiceException e) {
            throw new SearchClientException(e);
        }
    }

    @Override
    public String getDocument(String indexName, String documentId) {
        try {
            GetResponse response = client.get(new GetRequest(indexName).id(documentId));
            if (response.isExists()) {
                return response.getSourceAsString();
            }
            return null;
        } catch (RuntimeServiceException e) {
            throw new SearchClientException(e);
        }
    }

    @Override
    public Long getDocumentVersion(String indexName, String documentId) {
        try {
            GetResponse response = client.get(new GetRequest(indexName).id(documentId));
            if (response.isExists()) {
                return response.getVersion();
            }
            return null;
        } catch (RuntimeServiceException e) {
            throw new SearchClientException(e);
        }
    }

    @Override
    public SearchResponse search(SearchQuery query) {
        try {
            var osSearchRequest = QUERY_TRANSFORMER.apply(query);
            var osSearchResponse = client.search(osSearchRequest);
            return RESPONSE_TRANSFORMER.apply(query, osSearchResponse);
        } catch (RuntimeServiceException e) {
            // OpenSearchStatusException is raised when using phrase prefix on keyword type
            throw new SearchClientException(e);
        }
    }

    @Override
    public SearchResponse searchScroll(SearchScrollContext scrollContext) {
        var osRequest = new SearchScrollRequest(scrollContext.scrollId()).scroll(
                getKeepAlive(scrollContext.searchQuery()));
        var osSearchResponse = client.scroll(osRequest);
        return new OpenSearchResponseTransformer().apply(scrollContext.searchQuery(), osSearchResponse);
    }

    @Override
    public boolean clearScroll(SearchScrollContext scrollContext) {
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollContext.scrollId());
        var response = client.clearScroll(request);
        return response.isSucceeded();
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
