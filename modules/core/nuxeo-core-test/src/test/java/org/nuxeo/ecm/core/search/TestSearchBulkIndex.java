/*
 * (C) Copyright 2021-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.index.IndexingBackgroundAction.ACTION_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkCommand.Builder;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public class TestSearchBulkIndex {

    // field bigger than a record
    protected static final int BIG_FIELD_SIZE = 1_200_000;

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected SearchService searchService;

    @Inject
    protected CoreSearchFeature coreSearchFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void initWorkingDocuments() {
        for (int i = 0; i < 20; i++) {
            String name = "file" + i;
            String title = String.format("File%02d", i);
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", title);
            if (i == 0) {
                // create a huge field to make the doc bigger than a record
                doc.setPropertyValue("dc:source", new String(new char[BIG_FIELD_SIZE]).replace('\0', 'X'));
            }
            session.createDocument(doc);
        }
        txFeature.nextTransaction();
    }

    @Test
    public void testIndexAction() throws InterruptedException {
        checkSearchOrder();
        assumeTrue("Only for implementation that can init index", coreSearchFeature.dropAndInitIndex());

        BulkCommand command = new Builder(ACTION_NAME, "SELECT * FROM Document", "Administrator").batch(2)
                                                                                                 .bucket(2)
                                                                                                 .build();
        String commandId = bulkService.submit(command);
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(BulkStatus.State.COMPLETED, status.getState());
        assertNotNull("Processing start time is null, status: " + status, status.getProcessingStartTime());
        assertNotNull("Processing end time is null, status: " + status, status.getProcessingEndTime());
        assertTrue("Processing duration is 0, status: " + status, status.getProcessingDurationMillis() > 0);
    }

    protected void checkSearchOrder() {
        SearchQuery query = SearchQuery.builder("SELECT * FROM File", session)
                                       .addSort(new SortInfo("dc:title", false))
                                       .build();
        var response = searchService.search(query);
        var documents = response.loadDocuments(session);
        assertTrue(CollectionUtils.isNotEmpty(documents));
        List<String> ids = documents.stream().map(DocumentModel::getTitle).toList();
        List<String> ordered = new ArrayList<>(ids);
        ordered.sort(Comparator.reverseOrder());
        assertEquals(ordered, ids);
    }
}
