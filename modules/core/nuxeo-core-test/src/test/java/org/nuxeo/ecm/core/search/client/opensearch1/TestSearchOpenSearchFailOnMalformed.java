/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedNotContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedSince;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ CoreSearchFeature.class, LogCaptureFeature.class })
@ConditionalIgnore(condition = IgnoreIfNotOpenSearchSearchClient.class)
public class TestSearchOpenSearchFailOnMalformed {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR")
    public void shouldFailOnMalformedInput() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        // dynamic mapping will autodetect a date format for dc:coverage field
        doc.setPropertyValue("dc:coverage", "2005/11/01");
        doc.setPropertyValue("dc:title", "initial title");
        doc = session.createDocument(doc);
        final long t1 = System.currentTimeMillis();
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t1);

        // now set a malformed date
        doc.setPropertyValue("dc:coverage", "99999/12/01");
        doc.setPropertyValue("dc:title", "updated title");
        doc = session.saveDocument(doc);
        txFeature.nextTransaction();

        // because index.mapping.ignore_malformed is false we have an indexing failure
        List<String> caughtEvents = logResult.getCaughtEventMessages();
        assertEquals(1, caughtEvents.size());
        assertTrue(caughtEvents.toString(),
                caughtEvents.getFirst().startsWith(String.format("Indexing failure of: %s", doc.getId())));
        // the latest changes are not indexed
        assertIndexedNotContains(doc.getId(), "99999");
        assertIndexedNotContains(doc.getId(), "updated title");
        // the index is desynchronized
        assertIndexedContains(doc.getId(), "2005/11/01");
        assertIndexedContains(doc.getId(), "initial title");
    }

}
