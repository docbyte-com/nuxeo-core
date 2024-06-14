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
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
public class TestTreeIndexing {

    private static final String IDX_NAME = "nxutest";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected TransactionalFeature txFeature;

    protected void buildTree() {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void buildAndIndexTree() throws Exception {
        buildTree();
        txFeature.nextTransaction();

        // check indexing at ES level
        SearchResponse searchResponse = searchAll();
        assertEquals(10, searchResponse.getHits().getTotalHits().value);
    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected SearchResponse search(QueryBuilder query) {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        request.source(new SearchSourceBuilder().query(query));
        return esa.getClient().search(request);
    }

    @Test
    public void shouldIndexTree() throws Exception {
        buildAndIndexTree();

        // check sub tree search
        SearchResponse searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2"));
        assertEquals(8, searchResponse.getHits().getTotalHits().value);
    }

    @Test
    public void shouldUnIndexSubTree() throws Exception {
        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));

        session.removeDocument(ref);
        txFeature.nextTransaction();

        SearchResponse searchResponse = searchAll();
        assertEquals(2, searchResponse.getHits().getTotalHits().value);
    }

    @Test
    public void shouldIndexMovedSubTree() throws Exception {
        buildAndIndexTree();
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));
        DocumentModel doc = session.getDocument(ref);

        // move in the same folder : rename
        session.move(ref, doc.getParentRef(), "folderA");

        txFeature.nextTransaction();
        SearchResponse searchResponse = searchAll();
        assertEquals(10, searchResponse.getHits().getTotalHits().value);

        // check sub tree search
        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2"));
        assertEquals(0, searchResponse.getHits().getTotalHits().value);

        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folderA"));
        assertEquals(8, searchResponse.getHits().getTotalHits().value);

        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1"));
        assertEquals(9, searchResponse.getHits().getTotalHits().value);

    }

    @Test
    public void shouldFilterTreeOnSecurity() throws Exception {

        buildAndIndexTree();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        txFeature.nextTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        assertEquals(8, docs.totalSize());

        // block rights and check that blocking is taken into account

        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);

        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        acp.addACL(acl);

        session.setACP(ref, acp, true);

        session.save();
        txFeature.nextTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        assertEquals(3, docs.totalSize());
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() throws Exception {
        assumeTrue(session.isNegativeAclAllowed());

        buildAndIndexTree();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        txFeature.nextTransaction();

        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
        assertEquals(8, docs.totalSize());

        // Add an unsupported negative ACL
        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("bob", SecurityConstants.EVERYTHING, false));

        acp.addACL(acl);
        session.setACP(ref, acp, true);
        session.save();
        txFeature.nextTransaction();

        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
        // can view folder2, folder3 and folder4
        assertEquals(3, docs.totalSize());
    }

    @Test
    public void shouldStoreOnlyEffectiveACEs() throws Exception {
        buildAndIndexTree();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        assertEquals(10, docs.totalSize());

        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        assertEquals(0, docs.totalSize());

        DocumentRef ref = new PathRef("/folder0");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(ACE.builder("toto", SecurityConstants.READ).build());
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        txFeature.nextTransaction();

        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
        assertEquals(10, docs.totalSize());

        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        // make the ACE archived
        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(10, ChronoUnit.DAYS).toEpochMilli());
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().minus(2, ChronoUnit.DAYS).toEpochMilli());
        acl.add(ACE.builder("toto", SecurityConstants.READ).begin(begin).end(end).build());
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        txFeature.nextTransaction();

        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
        assertEquals(0, docs.totalSize());
    }

    @Test
    public void shouldReindexSubTreeInTrash() throws Exception {
        buildAndIndexTree();
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));
        Framework.getService(TrashService.class).trashDocument(session.getDocument(ref));

        // let BAF do its work
        txFeature.nextTransaction();
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("select * from Document where ecm:isTrashed = 0"));
        assertEquals(2, docs.totalSize());
    }

    @Test
    public void shouldIndexOnCopy() throws Exception {
        buildAndIndexTree();

        DocumentRef src = new PathRef("/folder0/folder1/folder2");
        DocumentRef dst = new PathRef("/folder0");
        session.copy(src, dst, "folder2-copy");

        txFeature.nextTransaction();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        assertEquals(18, docs.totalSize());
    }

}
