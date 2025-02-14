/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.FulltextStoredInBlobFeature;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ FulltextStoredInBlobFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestFulltextStoredInBlob extends TestFulltextEnabled {

    @Inject
    protected TransactionalFeature txFeature;

    @Override
    @Test
    public void testFulltext() throws Exception {
        createFileWithBlob();
        // binary fulltext extraction is done
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext = 'search'";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(1, esRet.totalSize());

        // fulltext search with core is not allowed
        exception.expect(QueryParseException.class);
        session.query(nxql);
    }

    @Override
    @Test
    public void testFulltextOnProxy() throws Exception {
        DocumentModel doc = createFileWithBlob();
        createSectionAndPublishFile(doc);
        // binary fulltext extraction is done
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext = 'search' AND ecm:isProxy = 1";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(1, esRet.totalSize());

        // fulltext search with core is not allowed
        exception.expect(QueryParseException.class);
        session.query(nxql);
    }

    @Test
    public void testFulltextBlobGC() throws Exception {
        assumeTrue("Requires blobKeys capability to use StreamOrphanBlobGC",
                coreFeature.getStorageConfiguration().isDBS());
        String docId = createFileWithBlob().getId();
        String ftBlobKey = getBinaryFulltextFieldValue(docId);
        assertTrue(fulltextBlobExists(ftBlobKey));

        // Update the binary fulltext
        updateMainContent(docId, "foo");
        txFeature.nextTransaction();
        // previous binary fulltext blob is removed
        assertFalse(fulltextBlobExists(ftBlobKey));
        // there's a new binary fulltext blob
        ftBlobKey = getBinaryFulltextFieldValue(docId);
        assertTrue(fulltextBlobExists(ftBlobKey));

        // Remove the main content
        removeMainContent(docId);
        txFeature.nextTransaction();
        // no more binary fulltext blob
        assertFalse(fulltextBlobExists(ftBlobKey));

        // Add a main content again
        updateMainContent(docId, "a new content");
        // there's a new binary fulltext blob
        ftBlobKey = getBinaryFulltextFieldValue(docId);
        assertTrue(fulltextBlobExists(ftBlobKey));

        // Remove the doc
        session.removeDocument(new IdRef(docId));
        txFeature.nextTransaction();
        // no more binary fulltext blobs
        assertFalse(fulltextBlobExists(ftBlobKey));
    }

    protected boolean fulltextBlobExists(String blobKey) {
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider("fulltext");
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = blobKey;
        File file = blobProvider.getFile(new SimpleManagedBlob(blobInfo));
        return file != null && file.exists();
    }

    protected String getBinaryFulltextFieldValue(String docId) {
        return (String) ((AbstractSession) session).getSession()
                                                   .getDocumentByUUID(docId)
                                                   .getPropertyValue("ecm:fulltextBinary");
    }

    protected void updateMainContent(String docId, String blobContent) {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(blobContent != null ? new StringBlob(blobContent) : null);
        session.saveDocument(doc);
    }

    protected void removeMainContent(String docId) {
        updateMainContent(docId, null);
    }
}
