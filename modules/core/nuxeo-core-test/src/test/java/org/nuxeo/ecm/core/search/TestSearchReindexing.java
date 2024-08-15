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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.forceRefresh;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test "on the fly" indexing via the listener system
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreSearchFeature.class, LogCaptureFeature.class })
public class TestSearchReindexing {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected SearchIndexingService searchIndexingService;

    @Inject
    protected TrashService trashService;

    @Inject
    protected CoreSearchFeature coreSearchFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void shouldReindexDocument() {
        buildDocs();

        String nxql = "SELECT * FROM Document ORDER BY ecm:uuid";
        DocumentModelList coreDocs = session.query(nxql);
        DocumentModelList docs = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);

        assertEquals(coreDocs.totalSize(), docs.totalSize());
        assertEquals(getDigest(coreDocs), getDigest(docs));

        assumeTrue("Only for implementation that can init index", coreSearchFeature.dropAndInitIndex());
        DocumentModelList docs2 = searchService.search(newSearchQuery(session, "SELECT * FROM Document"))
                                               .loadDocuments(session);
        assertEquals(0, docs2.totalSize());

        searchIndexingService.reindexRepository(session.getRepositoryName());
        txFeature.nextTransaction();
        forceRefresh();

        docs2 = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        assertEquals(getDigest(coreDocs), getDigest(docs2));
    }

    private void buildDocs() {
        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        folder = session.createDocument(folder);
        session.saveDocument(folder);
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            BlobHolder holder = doc.getAdapter(BlobHolder.class);
            holder.setBlob(new StringBlob("You know for search" + i));
            doc = session.createDocument(doc);
        }
        session.save();

        txFeature.nextTransaction();

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
            session.publishDocument(doc, folder);
            if (i % 2 == 0) {
                trashService.trashDocuments(List.of(doc));
            }
        }
        txFeature.nextTransaction();
    }

    protected String getDigest(DocumentModelList docs) {
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : docs) {
            String nameOrTitle = doc.getName();
            if (nameOrTitle == null || nameOrTitle.isEmpty()) {
                nameOrTitle = doc.getTitle();
            }
            sb.append(doc.getType()).append(" ").append(doc.isProxy()).append(" ").append(doc.getId()).append(" ");
            sb.append(nameOrTitle);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Test
    // @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = ElasticSearchIndexingImpl.class)
    @ConsoleLogLevelThreshold("ERROR")
    // @WithFrameworkProperty(name = "elasticsearch.index.bulkMaxSize", value = "4096")
    @Ignore("TODO check bulk request size and split if necessary")
    public void shouldReindexDocumentWithSmallBulkSize() {
        shouldReindexDocument();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertFalse("Expecting warn message", events.isEmpty());
        assertTrue(events.get(events.size() - 1).contains("Max bulk size reached"));
    }

}
