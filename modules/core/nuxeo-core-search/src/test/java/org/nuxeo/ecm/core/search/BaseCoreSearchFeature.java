/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.event.CoreEventFeature;
import org.nuxeo.ecm.core.search.index.commands.ThreadLocalIndexingCommandsStacker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.core.search")
@Deploy("org.nuxeo.ecm.core.search.test")
@Features({ CoreBulkFeature.class, CoreEventFeature.class })
public class BaseCoreSearchFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(BaseCoreSearchFeature.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(this::awaitIndexing);
        // make sure we are refreshing sync
        ThreadLocalIndexingCommandsStacker.useSyncIndexing.set(true);
    }

    protected boolean awaitIndexing(Duration duration) throws InterruptedException {
        return Framework.getService(SearchIndexingService.class).await(duration);
    }

    // --------------
    // Helper methods
    // --------------

    public static void assertNotIndexed(String documentId) {
        assertNotIndexed(Framework.getService(SearchService.class).getDefaultSearchIndex(), documentId);
    }

    public static void assertNotIndexed(SearchIndex searchIndex, String documentId) {
        SearchClient client = Framework.getService(SearchIndexingService.class).getClient(searchIndex.client());
        String doc = client.getDocument(searchIndex.index(), documentId);
        if (doc != null) {
            log.warn("Failure doc is not null, try to wait a bit before checking again");
            await("need to wait more").atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                var d = client.getDocument(searchIndex.index(), documentId);
                assertNull("Doc: " + documentId + " found in index: " + searchIndex + ", version in index:\n"
                        + formatJson(d), d);
            });
        }
    }

    public static void assertIndexedBefore(String documentId, long timestamp) {
        assertIndexedBefore(Framework.getService(SearchService.class).getDefaultSearchIndex(), documentId, timestamp);
    }

    public static void assertIndexedBefore(SearchIndex searchIndex, String documentId, long timestamp) {
        SearchClient client = Framework.getService(SearchIndexingService.class).getClient(searchIndex.client());
        Long version = client.getDocumentVersion(searchIndex.index(), documentId);
        assertNotNull("Doc: " + documentId + " not found in index: " + searchIndex, version);
        if (timestamp < version) {
            fail("Doc: " + documentId + " in indexed after " + (timestamp - version) + "ms: " + searchIndex
                    + ", version in index:\n" + formatJson(client.getDocument(searchIndex.index(), documentId)));
        }
    }

    public static void assertIndexedSince(String documentId, long timestamp) {
        assertIndexedSince(Framework.getService(SearchService.class).getDefaultSearchIndex(), documentId, timestamp);
    }

    public static void assertIndexedSince(SearchIndex searchIndex, String documentId, long timestamp) {
        SearchClient client = Framework.getService(SearchIndexingService.class).getClient(searchIndex.client());
        Long version = client.getDocumentVersion(searchIndex.index(), documentId);
        assertNotNull("Doc: " + documentId + " not found in index: " + searchIndex, version);
        if (timestamp > version) {
            fail("Doc: " + documentId + " in index is " + (timestamp - version) + "ms too old on: " + searchIndex
                    + ", version in index:\n" + formatJson(client.getDocument(searchIndex.index(), documentId)));
        }
    }

    public static void assertIndexedContains(String documentId, String match) {
        assertIndexedContains(Framework.getService(SearchService.class).getDefaultSearchIndex(), documentId, match);
    }

    public static void assertIndexedContains(SearchIndex searchIndex, String documentId, String match) {
        SearchClient client = Framework.getService(SearchIndexingService.class).getClient(searchIndex.client());
        String doc = client.getDocument(searchIndex.index(), documentId);
        assertNotNull("Doc: " + documentId + " not found in index: " + searchIndex, doc);
        assertTrue("Match for: \"" + match + "\" in doc:\n" + formatJson(doc), doc.contains(match));
    }

    public static void assertIndexedNotContains(String documentId, String match) {
        assertIndexedNotContains(Framework.getService(SearchService.class).getDefaultSearchIndex(), documentId, match);
    }

    public static void assertIndexedNotContains(SearchIndex searchIndex, String documentId, String match) {
        SearchClient client = Framework.getService(SearchIndexingService.class).getClient(searchIndex.client());
        String doc = client.getDocument(searchIndex.index(), documentId);
        assertNotNull("Doc: " + documentId + " not found in index: " + searchIndex, doc);
        assertFalse("Match for: \"" + match + "\" in doc:\n" + formatJson(doc), doc.contains(match));
    }

    protected static String formatJson(String doc) {
        try {
            return MAPPER.readTree(doc).toPrettyString();
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return doc;
        }
    }
}
