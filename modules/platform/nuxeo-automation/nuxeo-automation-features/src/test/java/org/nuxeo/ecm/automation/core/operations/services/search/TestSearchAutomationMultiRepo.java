/*
 * (C) Copyright 2020-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.services.search;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.search.index.IndexingDomainEventProducer.DISABLE_AUTO_INDEXING;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.IgnoreIfSearchClientDoesNotHaveIndexingCapability;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.MultiRepositorySearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeaturesFeature.class, MultiRepositorySearchFeature.class })
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
public class TestSearchAutomationMultiRepo {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession defaultSession;

    @Inject
    @Named("other")
    protected CoreSession otherSession;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected SearchService searchService;

    @Before
    public void init() {
        createDocs(defaultSession);
        createDocs(otherSession);
    }

    protected void createDocs(CoreSession session) {
        // create 2 docs without indexing them
        DocumentModel doc = session.createDocumentModel("/", "my-folder", "Folder");
        doc.setPropertyValue("dc:title", "A folder");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/my-folder/", "my-file", "File");
        doc.setPropertyValue("dc:title", "A file");
        doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        session.createDocument(doc);

        txFeature.nextTransaction();

        // nothing indexed because of disabled indexing flag
        assertEquals(0,
                searchService.search(SearchQuery.builder(session, "SELECT * from Document").build()).getHitsCount());
    }

    @Test
    public void testIndexingAllOnAllRepositoriesBulkService() throws Exception {
        try (var defaultCtx = new OperationContext(defaultSession);
                var secondCtx = new OperationContext(otherSession)) {
            automationService.run(defaultCtx, SearchIndexOperation.ID);
            automationService.run(secondCtx, SearchIndexOperation.ID);

            // will wait for bulk, wait for search indexing
            txFeature.nextTransaction();

            assertEquals(2, searchService.search(SearchQuery.builder(defaultSession, "SELECT * from Document").build())
                                         .getHitsCount());
            assertEquals(2, searchService.search(SearchQuery.builder(otherSession, "SELECT * from Document").build())
                                         .getHitsCount());
        }
    }

}
