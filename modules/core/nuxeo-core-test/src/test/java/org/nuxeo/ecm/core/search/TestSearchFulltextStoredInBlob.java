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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.FulltextStoredInBlobFeature;
import org.nuxeo.runtime.test.runner.Features;

@Features(FulltextStoredInBlobFeature.class)
public class TestSearchFulltextStoredInBlob extends TestSearchFulltextEnabled {

    @Override
    @Test
    public void testFulltext() {
        createFileWithBlob();
        // binary fulltext extraction is done
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext = 'search'";
        DocumentModelList esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        assertEquals(1, esRet.totalSize());

        // fulltext search with core is not allowed
        assertThrows(QueryParseException.class, () -> session.query(nxql));
    }

    @Override
    @Test
    public void testFulltextOnProxy() {
        DocumentModel doc = createFileWithBlob();
        createSectionAndPublishFile(doc);
        // binary fulltext extraction is done
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext = 'search' AND ecm:isProxy = 1";
        DocumentModelList esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        assertEquals(1, esRet.totalSize());

        // fulltext search with core is not allowed
        assertThrows(QueryParseException.class, () -> session.query(nxql));
    }

}
