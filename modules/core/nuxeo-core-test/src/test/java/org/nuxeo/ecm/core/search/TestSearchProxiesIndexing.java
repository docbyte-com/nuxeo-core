/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the indexing of proxies
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public class TestSearchProxiesIndexing {
    @Inject
    protected CoreSession coreSession;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testSearch() {
        DocumentModel note = init();

        var searchQuery = newSearchQuery(coreSession, "SELECT * FROM Note WHERE ecm:isProxy = 1");
        DocumentModelList results = searchService.search(searchQuery).loadDocuments(coreSession);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getPropertyValue("dc:title")).isEqualTo("Titre 1");

        update(note);

        results = searchService.search(searchQuery).loadDocuments(coreSession);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getPropertyValue("dc:title")).isEqualTo("Titre 2");
    }

    @Test
    public void testRepository() {
        DocumentModel note = init();

        DocumentModelList results = coreSession.query("SELECT * FROM Note WHERE ecm:isProxy = 1");
        assertThat(results).hasSize(1);
        DocumentModel proxy = results.getFirst();
        assertThat(proxy.getPropertyValue("dc:title")).isEqualTo("Titre 1");
        update(note);

        proxy = coreSession.getDocument(proxy.getRef());
        assertThat(proxy.getPropertyValue("dc:title")).isEqualTo("Titre 2");

        results = coreSession.query("SELECT * FROM Note WHERE ecm:isProxy = 1");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getPropertyValue("dc:title")).isEqualTo("Titre 2");

    }

    protected DocumentModel init() {
        DocumentModel folder = coreSession.createDocumentModel("/", "folder", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel note = coreSession.createDocumentModel("/", "note", "Note");
        note.setPropertyValue("dc:title", "Titre 1");
        note = coreSession.createDocument(note);

        coreSession.createProxy(note.getRef(), folder.getRef());

        coreSession.save();
        txFeature.nextTransaction();
        return note;
    }

    protected void update(DocumentModel note) {
        note = coreSession.getDocument(note.getRef());
        note.setPropertyValue("dc:title", "Titre 2");
        coreSession.saveDocument(note);

        coreSession.save();
        txFeature.nextTransaction();
    }
}
