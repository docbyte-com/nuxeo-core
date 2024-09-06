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

import static org.junit.Assert.assertFalse;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedNotContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertNotIndexed;
import static org.nuxeo.ecm.core.search.index.IndexingDomainEventProducer.DISABLE_AUTO_INDEXING;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.LocalBlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test service declaration as well as basic indexing API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/search/disable-search-domain-event-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
public class TestSearchManualIndexing {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchIndexingService searchIndexingService;

    @Inject
    protected BlobManager manager;

    protected SearchIndex searchIndex;

    @Before
    public void retrieveSearchIndex() {
        searchIndex = Framework.getService(SearchService.class).getDefaultSearchIndex();
    }

    @Test
    public void checkIndexing() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "Testme");
        // disable automatic indexing to control manually the indexing command
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // sync non recursive
        searchIndexingService.indexDocuments(
                BulkIndexingRequest.buildRequest(true).add(IndexingRequest.upsert(doc.getId())).build(searchIndex));

        assertIndexedContains(doc.getId(), "Testme");
    }

    @Test
    public void shouldHandleMissingBlob() {
        // Create the document with a string blob
        DocumentModel doc = session.createDocumentModel("/", "myBlobFile", "File");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc.setPropertyValue("dc:title", "myBlob");
        Blob fb = Blobs.createBlob("Not worth it", "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        doc = session.createDocument(doc);
        session.save();

        // Remove the binary
        ManagedBlob blobProperty = (ManagedBlob) doc.getPropertyValue("file:content");
        String blobKey = blobProperty.getKey();
        LocalBlobProvider provider = (LocalBlobProvider) manager.getBlobProvider(blobProperty.getProviderId());
        provider.store.deleteBlob(blobKey);
        assertFalse(provider.store.exists(blobKey));

        searchIndexingService.indexDocuments(
                BulkIndexingRequest.buildRequest(true).add(IndexingRequest.upsert(doc.getId())).build(searchIndex));

        assertIndexedContains(doc.getId(), "myBlobFile");
        assertIndexedNotContains(doc.getId(), "Not worth it");
    }

    @Test
    public void checkManualAsyncIndexing() {
        DocumentModel doc0 = session.createDocumentModel("/", "testNote", "Note");
        doc0.setPropertyValue("dc:title", "TestNote");
        doc0.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc0 = session.createDocument(doc0);
        session.save();

        DocumentModel doc0v = session.getLastDocumentVersion(doc0.getRef());

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // init index
        searchIndexingService.indexDocuments(
                BulkIndexingRequest.buildRequest(true).add(IndexingRequest.upsert(doc0.getId())).build(searchIndex));

        // only one doc should be indexed for now
        assertIndexedContains(doc0.getId(), "testNote");
        assertNotIndexed(doc0v.getId());
        assertNotIndexed(doc.getId());

        // now commit and wait for post commit indexing
        txFeature.nextTransaction();

        searchIndexingService.indexDocuments(
                BulkIndexingRequest.buildRequest(true).add(IndexingRequest.upsert(doc.getId())).build(searchIndex));

        // both docs are here
        assertIndexedContains(doc0.getId(), "testNote");
        assertNotIndexed(doc0v.getId());
        assertIndexedContains(doc.getId(), "TestMe");
    }

}
