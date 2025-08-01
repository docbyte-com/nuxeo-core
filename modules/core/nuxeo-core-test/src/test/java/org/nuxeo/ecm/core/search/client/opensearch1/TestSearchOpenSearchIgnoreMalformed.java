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

import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedNotContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedSince;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/search/test-opensearch1-ignore-malformed-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfNotOpenSearchSearchClient.class)
public class TestSearchOpenSearchIgnoreMalformed {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void shouldIndexMalformedInput() {
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
        final long t2 = System.currentTimeMillis();
        txFeature.nextTransaction();

        // because index.mapping.ignore_malformed is true indexing is done except for the malformed field
        assertIndexedSince(doc.getId(), t2);
        assertIndexedContains(doc.getId(), "99999");
        assertIndexedContains(doc.getId(), "updated title");
        assertIndexedNotContains(doc.getId(), "2005/11/01");
    }

}
