/*
 * (C) Copyright 2016-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.client.repository.IgnoreIfRepositorySearchClient;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.query.test:OSGI-INF/test-aggregate-schemas-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfRepositorySearchClient.class, cause = "RepositorySearchClient can not select on ecm:path")
public class TestSearchIterableQueryResult {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testIterableScroll() {
        int nbDocs = 100;
        buildAndIndexTree(nbDocs);

        String nxql = "select ecm:uuid, ecm:path from Document";
        // Request the first batch
        var res = searchService.search(
                SearchQuery.builder(nxql, session).scrollSize(20).scrollKeepAlive(Duration.ofSeconds(10_000)).build());
        assertEquals(nbDocs, res.getTotal());
        assertEquals(20, res.getHitsCount());

        var iterable = (IterableQueryResultImpl) res.getHitsAsIterator();
        assertEquals(nbDocs, iterable.size());
        assertEquals(0, iterable.pos());
        assertTrue(iterable.mustBeClosed());
        assertTrue(iterable.hasNext());

        List<Map<String, Serializable>> rows = new ArrayList<>(nbDocs);
        while (iterable.hasNext()) {
            rows.add(iterable.next());
        }
        assertEquals(nbDocs, rows.size());
        iterable.close();
    }

    @Test
    public void testIterableSkipTo() {
        int nbDocs = 100;
        buildAndIndexTree(nbDocs);

        String nxql = "select ecm:uuid, ecm:path from Document";
        // Request the first batch
        var res = searchService.search(
                SearchQuery.builder(nxql, session).scrollSize(20).scrollKeepAlive(Duration.ofSeconds(10_000)).build());
        var iterable = (IterableQueryResultImpl) res.getHitsAsIterator();

        iterable.skipTo(70);
        List<Map<String, Serializable>> rows = new ArrayList<>(30);
        while (iterable.hasNext()) {
            rows.add(iterable.next());
        }
        assertEquals(30, rows.size());
        iterable.close();

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
