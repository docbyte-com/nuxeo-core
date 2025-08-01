/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/search/test-search-security-policy-contrib.xml")
public class TestSearchSecurityPolicy {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    protected void buildDocs() {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "folder");
        session.createDocument(doc);
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            doc = session.createDocumentModel("/folder", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            session.createDocument(doc);
        }
    }

    protected void buildAndIndexDocs() {
        buildDocs();
        txFeature.nextTransaction();
    }

    @Test
    public void shouldWorkWithSecurityPolicy() {
        buildAndIndexDocs();
        grantBrowsePermToUser("/folder", "toto");

        // As administrator I can see all docs
        var searchResponse = searchService.search(newSearchQuery(session, "select * from Document"));
        // don't use searchResponse.getTotal that could be wrong, see NXP-29782
        assertEquals(6, searchResponse.getHitsCount());

        // As user File documents are denied
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        searchResponse = searchService.search(newSearchQuery(restrictedSession, "select * from Document"));
        assertEquals(1, searchResponse.getHitsCount());
        assertEquals(1, searchResponse.getTotal());
    }

    protected void grantBrowsePermToUser(String path, String username) {
        DocumentRef ref = new PathRef(path);
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE(username, SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();
    }

}
