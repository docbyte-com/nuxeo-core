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
 *     Kevin Leturc
 */
package org.nuxeo.elasticsearch.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsIterableQueryResultImpl;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:schemas-test-contrib.xml")
public class TestEsIterableQueryResultImpl {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testIterableScroll() {

        buildAndIndexTree(100);

        String nxql = "select ecm:uuid, ecm:path from Document";
        // Request the first batch
        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(nxql).limit(20).onlyElasticsearchResponse();
        EsScrollResult res = ess.scroll(queryBuilder, 10000);

        // Init wrapper
        ElasticSearchService spiedEss = spy(ess);
        EsIterableQueryResultImpl iterable = new EsIterableQueryResultImpl(spiedEss, res);
        assertEquals(100, iterable.size());
        assertEquals(0, iterable.pos());
        assertTrue(iterable.mustBeClosed());
        assertTrue(iterable.hasNext());

        List<Map<String, Serializable>> rows = new ArrayList<>(100);
        while (iterable.hasNext()) {
            rows.add(iterable.next());
        }
        assertEquals(100, rows.size());
        // Each 20 items, a request is made to ES, check that
        // So a request is made at 20, 40, 60, 80
        verify(spiedEss, times(4)).scroll(any());

        iterable.close();
        verify(spiedEss, times(1)).clearScroll(any());

    }

    @Test
    public void testIterableSkipTo() {

        buildAndIndexTree(100);

        String nxql = "select ecm:uuid, ecm:path from Document";
        // Request the first batch
        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(nxql).limit(20).onlyElasticsearchResponse();
        EsScrollResult res = ess.scroll(queryBuilder, 10000);

        // Init wrapper
        ElasticSearchService spiedEss = spy(ess);
        EsIterableQueryResultImpl iterable = new EsIterableQueryResultImpl(spiedEss, res);
        iterable.skipTo(70);
        // Each 20 items, a request is made to ES, check that
        // So a request is made at 20, 40, 60
        verify(spiedEss, times(3)).scroll(any());

        List<Map<String, Serializable>> rows = new ArrayList<>(30);
        while (iterable.hasNext()) {
            rows.add(iterable.next());
        }
        assertEquals(30, rows.size());

        // A request is made at 80
        verify(spiedEss, times(4)).scroll(any());

        iterable.close();
        verify(spiedEss, times(1)).clearScroll(any());

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
