/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test "on the fly" indexing via the listener system
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public class TestSearchFulltextEnabled {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSearchFeature coreSearchFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void checkSupportsFulltextSearchIfRepositoryClient() {
        assumeTrue("fulltext search not supported", !coreSearchFeature.hasRepositoryClient()
                || coreFeature.getStorageConfiguration().supportsFulltextSearch());
    }

    @Test
    public void testFulltext() {
        createFileWithBlob();
        // binary fulltext is extracted and searcheable with ES
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search'";
        DocumentModelList esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        assertEquals(1, esRet.totalSize());

        // binary fulltext is also searcheable with VCS
        if (coreFeature.getStorageConfiguration().supportsFulltextSearch()) {
            txFeature.nextTransaction();
            DocumentModelList coreRet = session.query(nxql);
            assertEquals(1, coreRet.totalSize());
        }
    }

    @Test
    public void testFulltextOnProxy() {
        DocumentModel doc = createFileWithBlob();
        createSectionAndPublishFile(doc);
        // binary fulltext is extracted and searcheable with ES
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search' AND ecm:isProxy = 1";
        DocumentModelList esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        assertEquals(1, esRet.totalSize());

        // binary fulltext is also searcheable with VCS
        if (coreFeature.getStorageConfiguration().supportsFulltextSearch()) {
            txFeature.nextTransaction();
            DocumentModelList coreRet = session.query(nxql);
            assertEquals(1, coreRet.totalSize());
        }
    }

    protected DocumentModel createFileWithBlob() {
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        session.createDocument(doc);
        session.save();

        // we need to wait for the async fulltext indexing
        txFeature.nextTransaction();

        // There is one doc
        DocumentModelList ret = searchService.search(newSearchQuery(session, "SELECT * From Document"))
                                             .loadDocuments(session);
        assertEquals(1, ret.totalSize());

        return ret.getFirst();
    }

    protected void createSectionAndPublishFile(DocumentModel doc) {
        // Create a Section
        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);

        // Publish Document
        session.publishDocument(doc, section);
        session.save();

        // we need to wait for the async fulltext indexing
        txFeature.nextTransaction();

        // There is one doc
        DocumentModelList ret = searchService.search(
                newSearchQuery(session, "SELECT * FROM Document WHERE ecm:isProxy = 1")).loadDocuments(session);
        assertEquals(1, ret.totalSize());
    }

}
