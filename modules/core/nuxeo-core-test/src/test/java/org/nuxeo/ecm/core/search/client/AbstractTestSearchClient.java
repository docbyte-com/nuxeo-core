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
package org.nuxeo.ecm.core.search.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.SearchClient.Capability.INDEXING;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.search.BulkIndexingRequest;
import org.nuxeo.ecm.core.search.IndexingRequest;
import org.nuxeo.ecm.core.search.SearchClient;
import org.nuxeo.ecm.core.search.SearchIndex;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public abstract class AbstractTestSearchClient {

    @Inject
    protected SearchService service;

    @Inject
    protected SearchIndexingService indexingService;

    protected boolean populated = false;

    @Before
    public void populateIndex() {
        // check we can index before populating the index
        assumeTrue("Search Client: '" + getClient().getName() + "' has no indexing capability",
                getClient().hasCapability(INDEXING));
        // populate only once as there's no cleanup
        if (populated) {
            return;
        }
        String source = getSourceToIndex();
        var requestBuilder = BulkIndexingRequest.buildRequest(true);
        try {
            var lines = Files.readAllLines(Path.of(getClass().getClassLoader().getResource(source).toURI()));
            for (var i = 0; i < lines.size(); i++) {
                requestBuilder.add(IndexingRequest.upsertWithSource(String.format("%03d", i), lines.get(i)));
            }
        } catch (IOException | NullPointerException | URISyntaxException e) {
            throw new AssertionError("Unable to populate index, an error occurred while reading file: " + source, e);
        }
        getClient().indexDocuments(requestBuilder.build(getIndex()));
        populated = true;
    }

    /**
     * @return the {@code ndjson} file to index for the test
     */
    protected abstract String getSourceToIndex();

    public SearchIndex getIndex() {
        return service.getDefaultSearchIndex();
    }

    public SearchClient getClient() {
        return indexingService.getClient(getIndex().client());
    }

    public SearchResponse search(String nxql) {
        return getClient().search(SearchQuery.builder(getIndex(), nxql).limit(1_000).build());
    }

    public SearchResponse searchAndAssert(String nxql, BiConsumer<String, SearchResponse> assertion) {
        var query = SearchQuery.builder(getIndex(), nxql).limit(1_000).build();
        SearchResponse response = search(nxql);
        assertion.accept(dump(nxql, query, response), response);
        return response;
    }

    public SearchResponse searchAndAssertHits(String nxql) {
        return searchAndAssert(nxql, (message, response) -> assertTrue(message, response.getTotal() > 0));
    }

    public SearchResponse searchAndAssertHits(String nxql, int expectedHit) {
        return searchAndAssert(nxql, (message, response) -> assertEquals(message, expectedHit, response.getTotal()));
    }

    public SearchResponse searchAndAssertNoHits(String nxql) {
        return searchAndAssert(nxql, (message, response) -> assertEquals(message, 0, response.getTotal()));
    }

    protected String dump(String nxql, SearchQuery query, SearchResponse response) {
        return String.format("\nnxql: %s\nquery: %s\nresponse: %s", nxql, query, response);
    }
}
