/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the scroll search API exposed by {@link ElasticSearchService}.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
public class TestScrollSearch {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testScroll() {

        buildAndIndexTree(100);

        // Initial search request, includes the first batch of results
        String query = "select * from Document order by ecm:path";
        EsScrollResult res = ess.scroll(new NxQueryBuilder(session).nxql(query).limit(20), 10000);
        assertNotNull(res);
        assertNotNull(res.getQueryBuilder());
        assertEquals(10000, res.getKeepAlive());
        assertNotNull(res.getScrollId());

        // Next result batches
        int totalDocCount = 0;
        List<String> docPaths = new ArrayList<>();
        DocumentModelList docs = res.getDocuments();
        while (!docs.isEmpty()) {
            int hitCount = docs.size();
            assertEquals(20, hitCount);
            totalDocCount += hitCount;
            docPaths.addAll(docs.stream().map(DocumentModel::getPathAsString).collect(Collectors.toList()));
            res = ess.scroll(res);
            docs = res.getDocuments();
        }
        assertEquals(100, totalDocCount);

        // Check order
        assertEquals(session.query(query).stream().map(DocumentModel::getPathAsString).collect(Collectors.toList()),
                docPaths);
    }

    protected void buildAndIndexTree(int docCount) {
        String root = "/";
        for (int i = 0; i < docCount; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
        }
        txFeature.nextTransaction();
    }
}
