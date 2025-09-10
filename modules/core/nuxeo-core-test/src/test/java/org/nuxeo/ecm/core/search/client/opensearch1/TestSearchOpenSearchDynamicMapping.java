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
 *     mcedica@nuxeo.com
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

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

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/search/test-opensearch1-dynamic-mapping-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfNotOpenSearchSearchClient.class)
public class TestSearchOpenSearchDynamicMapping {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testShouldIndexDocUsingCustomWriter() {
        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        // put some raw json in the node and checked is indexed dynamically
        doc.setPropertyValue("note:note", String.format(
                "{\"type1\":[{\"type1:id_int\":10},{\"type1:name_string\":\"test\"},{\"type1:creation_date\":\"%s\"}]}",
                "2015-01-01T12:30:00"));
        doc = session.createDocument(doc);

        txFeature.nextTransaction();

        // check that the custom mapping applied

        // Since ES 2.x we need to express the full path of property: type1:id_int becomes dynamic/type1/type1:id_int
        var ret = searchService.search(
                newSearchQuery(session, "SELECT * FROM Document WHERE dynamic/type1/type1:id_int = 11"));
        assertEquals(0, ret.getTotal());

        ret = searchService.search(newSearchQuery(session,
                "SELECT * FROM Document WHERE dynamic/type1/type1:id_int = 10 AND ecm:isVersion = 0"));
        assertEquals(1, ret.getTotal());

        ret = searchService.search(newSearchQuery(session,
                "SELECT * FROM Document WHERE dynamic/type1/type1:name_string LIKE 'test' AND ecm:isVersion = 0"));
        assertEquals(1, ret.getTotal());

        ret = searchService.search(newSearchQuery(session,
                "SELECT * FROM Document WHERE dynamic/type1/type1:creation_date BETWEEN DATE '2015-01-01' AND DATE '2015-01-02' AND ecm:isVersion = 0"));
        assertEquals(1, ret.getTotal());
    }
}
