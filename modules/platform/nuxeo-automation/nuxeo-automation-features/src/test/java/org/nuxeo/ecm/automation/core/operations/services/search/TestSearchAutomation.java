/*
 * (C) Copyright 2010-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.forceRefresh;
import static org.nuxeo.ecm.core.search.index.IndexingDomainEventProducer.DISABLE_AUTO_INDEXING;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeaturesFeature.class)
@Deploy("org.nuxeo.ecm.automation.features.tests:test-indexAndRefresh-chain-contrib.xml")
public class TestSearchAutomation {

    private static final String INDEX_CHAIN = "indexAndRefresh";

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected CoreSearchFeature coreSearchFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected SearchService searchService;

    protected DocumentRef rootRef;

    protected DocumentRef fileRef;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(coreSession);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Before
    public void initRepo() throws Exception {
        assumeTrue("Only for implementation that can init index", coreSearchFeature.dropAndInitIndex());
        // create 2 docs without indexing them
        DocumentModel doc = coreSession.createDocumentModel("/", "my-folder", "Folder");
        doc.setPropertyValue("dc:title", "A folder");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = coreSession.createDocument(doc);
        rootRef = doc.getRef();

        doc = coreSession.createDocumentModel("/my-folder/", "my-file", "File");
        doc.setPropertyValue("dc:title", "A file");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = coreSession.createDocument(doc);
        fileRef = doc.getRef();
        coreSession.save();
        txFeature.nextTransaction();
        // nothing indexed because of disable indexing flag
        assertEquals(0, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingAll() throws Exception {
        automationService.run(ctx, INDEX_CHAIN);
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingFromRoot() throws Exception {
        ctx.setInput(rootRef);
        automationService.run(ctx, INDEX_CHAIN);
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingFromPath() throws Exception {
        // first index all
        automationService.run(ctx, INDEX_CHAIN);
        // then reindex from path, so we have 2 commands: delete + insert
        ctx.setInput(rootRef);
        automationService.run(ctx, INDEX_CHAIN);
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingFromNxql() throws Exception {
        ctx.setInput("SELECT ecm:uuid FROM Document WHERE ecm:primaryType = 'File'");
        automationService.run(ctx, INDEX_CHAIN);
        assertEquals(1, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingAllBulkService() throws Exception {
        Blob result = (Blob) automationService.run(ctx, SearchIndexOperation.ID);
        assertNotNull(result);
        String commandId = new ObjectMapper().readTree(result.getString()).get("commandId").asText();
        boolean waitResult = bulkService.await(commandId, Duration.ofSeconds(60));
        assertTrue("Bulk action didn't finish", waitResult);
        // indexing is done but refresh is processed just after, do it sync
        forceRefresh();
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", coreSession).build())
                                     .getHitsCount());
    }

    @Test
    public void testIndexingOneDoc() throws Exception {
        ctx.setInput(fileRef);
        Blob result = (Blob) automationService.run(ctx, SearchIndexOperation.ID);
        String commandId = new ObjectMapper().readTree(result.getString()).get("commandId").asText();
        boolean waitResult = bulkService.await(commandId, Duration.ofSeconds(60));
        assertTrue("Bulk action didn't finish", waitResult);
        var status = bulkService.getStatus(commandId);
        assertEquals(1, status.getProcessed());
        assertEquals(1, status.getTotal());
    }

}
