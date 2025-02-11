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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.action.FixBinaryFulltextStorageAction;
import org.nuxeo.ecm.core.test.FulltextStoredInBlobFeature;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

@RunWith(FeaturesRunner.class)
@Features({ FulltextStoredInBlobFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@WithFrameworkProperty(name = "nuxeo.bulk.action.fixBinaryFulltextStorage.enabled", value = "true")
public class TestFulltextStoredInBlobMigration {
    @Inject
    protected CoreSession coreSession;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testMigration() throws InterruptedException {
        String ft1 = "You know for search foo";
        String ft1md5 = "7d502e2063185c0f18b120bea887e0af";
        String doc1 = createDocWithBlob("doc1", ft1);
        String ft2 = "You know for search bar";
        String ft2md5 = "4d56ce3b24f06e8449b6489e907ddb78";
        String doc2 = createDocWithBlob("doc2", ft2);
        txFeature.nextTransaction();

        // check binary fulltext, we need to trim since extracted fulltext contains trailing spaces
        assertEquals(ft1, getBinaryFulltextValue(doc1).trim());
        assertEquals(ft2, getBinaryFulltextValue(doc2).trim());
        // all stored as blob
        assertEquals(ft1md5, getBinaryFulltextFieldValue(doc1));
        assertEquals(ft2md5, getBinaryFulltextFieldValue(doc2));

        DocumentModelList esRet = ess.query(
                new NxQueryBuilder(coreSession).nxql("SELECT * FROM Document WHERE ecm:fulltext = 'search'"));
        assertEquals(2, esRet.totalSize());

        // override the fulltext for doc2 to simulate a fulltext stored in repository
        setBinaryFulltextFieldValue(doc2, getBinaryFulltextValue(doc2));
        txFeature.nextTransaction();

        // check fulltext
        assertEquals(ft1, getBinaryFulltextValue(doc1).trim());
        assertEquals(ft2, getBinaryFulltextValue(doc2).trim());
        // different storages
        assertEquals(ft1md5, getBinaryFulltextFieldValue(doc1));
        assertEquals(ft2, getBinaryFulltextFieldValue(doc2).trim());

        // reindexing doesn't change anything
        esi.reindexRepository(coreSession.getRepositoryName());
        txFeature.nextTransaction();
        esRet = ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * FROM Document WHERE ecm:fulltext = 'search'"));
        assertEquals(2, esRet.totalSize());
        // still different storages
        assertEquals(ft1md5, getBinaryFulltextFieldValue(doc1));
        assertEquals(ft2, getBinaryFulltextFieldValue(doc2).trim());

        // migrate
        String commandId = bulkService.submit(new BulkCommand.Builder(FixBinaryFulltextStorageAction.ACTION_NAME,
                "SELECT * FROM Document", SYSTEM_USERNAME).build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus bulkStatus = bulkService.getStatus(commandId);
        assertEquals(2, bulkStatus.getTotal());
        assertEquals(2, bulkStatus.getProcessed());
        assertEquals(1, bulkStatus.getSkipCount());
        // reload invalidation
        txFeature.nextTransaction();
        // same storage fixed
        assertEquals(ft1md5, getBinaryFulltextFieldValue(doc1));
        assertEquals(ft2md5, getBinaryFulltextFieldValue(doc2));
        assertEquals(ft1, getBinaryFulltextValue(doc1).trim());
        assertEquals(ft2, getBinaryFulltextValue(doc2).trim());
        esRet = ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * FROM Document WHERE ecm:fulltext = 'search'"));
        assertEquals(2, esRet.totalSize());
    }

    protected String createDocWithBlob(String name, String blobContent) {
        DocumentModel doc = coreSession.createDocumentModel("/", name, "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob(blobContent));
        doc = coreSession.createDocument(doc);
        return doc.getId();
    }

    @SuppressWarnings("rawtypes")
    protected Session getSession() {
        return ((AbstractSession) coreSession).getSession();
    }

    protected String getBinaryFulltextFieldValue(String docId) {
        return (String) getSession().getDocumentByUUID(docId).getPropertyValue("ecm:fulltextBinary");
    }

    protected String getBinaryFulltextValue(String docId) {
        return coreSession.getDocument(new IdRef(docId)).getBinaryFulltext().get("binarytext");
    }

    protected void setBinaryFulltextFieldValue(String docId, String value) {
        getSession().getDocumentByUUID(docId).setPropertyValue("ecm:fulltextBinary", value);
        getSession().save();
    }

}
