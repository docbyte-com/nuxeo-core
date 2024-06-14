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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.search.SearchClient.Capability.INDEXING;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.search.BulkIndexingRequest;
import org.nuxeo.ecm.core.search.IndexingRequest;
import org.nuxeo.ecm.core.search.SearchClient;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(OpenSearchCoreSearchFeature.class)
public class TestOpenSearchSearchClient {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    public SearchService service;

    @Inject
    public SearchIndexingService indexingService;

    @Test
    public void testOpenSearchClient() {
        var searchIndex = service.getDefaultSearchIndex();
        SearchClient client = indexingService.getClient(searchIndex.client());
        assertNotNull(client);
        assertEquals("opensearch", client.getName());
        assertTrue(client.hasCapability(INDEXING));
        assertTrue(client.isReady());
    }

    @Test
    public void testIndexing() {
        var searchIndex = service.getDefaultSearchIndex();
        SearchClient client = indexingService.getClient(searchIndex.client());
        assertEquals(client.getName(), searchIndex.client());

        var requestBuilder = BulkIndexingRequest.buildRequest(true);
        requestBuilder.add(createRequest("foo", "A foo doc"));
        requestBuilder.add(createRequest("bar", "A bar doc"));
        client.indexDocuments(requestBuilder.build(searchIndex));

        assertNotNull(client.getDocument(searchIndex.index(), "foo"));
        assertTrue(client.getDocument(searchIndex.index(), "foo").contains("A foo doc"));
        assertNotNull(client.getDocument(searchIndex.index(), "bar"));
        assertNull(client.getDocument(searchIndex.index(), "foobar"));

        requestBuilder = BulkIndexingRequest.buildRequest(true);
        requestBuilder.add(createRequest("foo", "An updated foo doc"));
        client.indexDocuments(requestBuilder.build(searchIndex));
        assertTrue(client.getDocument(searchIndex.index(), "foo").contains("An updated foo doc"));

        requestBuilder = BulkIndexingRequest.buildRequest(true);
        requestBuilder.add(IndexingRequest.delete("bar"));
        client.indexDocuments(requestBuilder.build(searchIndex));
        assertNull(client.getDocument(searchIndex.index(), "bar"));
    }

    protected IndexingRequest createRequest(String id, String title) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", id);
        node.put("title", title);
        return IndexingRequest.upsertWithSource(id, node.toString());
    }
}
