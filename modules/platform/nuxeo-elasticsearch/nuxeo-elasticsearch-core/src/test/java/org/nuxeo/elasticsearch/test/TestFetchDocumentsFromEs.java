/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchType;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
public class TestFetchDocumentsFromEs {

    private static final String IDX_NAME = "nxutest";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    protected void buildTree() {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void waitForAsyncIndexing() throws Exception {
        // wait for indexing
        WorkManager wm = Framework.getService(WorkManager.class);
        assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        assertEquals(0, esa.getPendingWorkerCount());
    }

    protected void buildAndIndexTree() throws Exception {

        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }

        // build the tree
        buildTree();

        int n = esa.getTotalCommandProcessed();

        TransactionHelper.commitOrRollbackTransaction();

        waitForAsyncIndexing();

        assertEquals(10, esa.getTotalCommandProcessed() - n);
        esa.refresh();

        TransactionHelper.startTransaction();

        // check indexing
        SearchResponse searchResponse = searchAll();
        assertEquals(10, searchResponse.getHits().getTotalHits().value);

    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    @Test
    public void shouldLoadDocumentFromEs() throws Exception {
        buildAndIndexTree();
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("select * from Document").limit(20).fetchFromElasticsearch());
        assertEquals(10, docs.totalSize());
        /*
         * for (DocumentModel doc : docs) { System.out.println(doc); }
         */

    }

    @Test
    public void checkNotFetch() throws Exception {
        buildAndIndexTree();
        // onlyElasticsearchResponse is useless on query aPI
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("select * from Document").limit(20).onlyElasticsearchResponse());
        assertNull(docs);
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document")
                                                    .limit(20)
                                                    .fetchFromElasticsearch()
                                                    .onlyElasticsearchResponse());
        assertNull(docs);

        // using queryAndAggregate we can have the original Elasticsearch response
        EsResult result = ess.queryAndAggregate(
                new NxQueryBuilder(session).nxql("select * from Document").limit(20).onlyElasticsearchResponse());
        assertNull(result.getDocuments());
        assertNull(result.getAggregates());
        assertEquals(10, result.getElasticsearchResponse().getHits().getTotalHits().value);
        // System.out.println(result.getElasticsearchResponse());

        result = ess.queryAndAggregate(new NxQueryBuilder(session).nxql("select * from Document")
                                                                  .limit(20)
                                                                  .fetchFromElasticsearch()
                                                                  .onlyElasticsearchResponse());
        assertNull(result.getDocuments());
        assertNull(result.getAggregates());
        assertEquals(10, result.getElasticsearchResponse().getHits().getTotalHits().value);
        // System.out.println(result.getElasticsearchResponse());

    }

    /**
     * @since 8.2
     */
    @Test
    public void checkPathLevel() throws Exception {
        buildAndIndexTree();

        EsResult result = ess.queryAndAggregate(new NxQueryBuilder(session).nxql("select * from Document")
                                                                           .limit(20)
                                                                           .fetchFromElasticsearch()
                                                                           .onlyElasticsearchResponse());

        for (SearchHit sh : result.getElasticsearchResponse().getHits()) {
            String path = (String) sh.getSourceAsMap().get("ecm:path");
            int pathDepth = (int) sh.getSourceAsMap().get("ecm:path@depth");
            String[] split = path.split("/");
            assertEquals(split.length, pathDepth);
            for (int i = 1; i < split.length; i++) {
                assertEquals(split[i], sh.getSourceAsMap().get("ecm:path@level" + i));
            }
        }

    }
}
