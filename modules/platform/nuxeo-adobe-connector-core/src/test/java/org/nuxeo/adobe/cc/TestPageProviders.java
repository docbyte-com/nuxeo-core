/*
 * (C) Copyright 2019-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.adobe.cc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_TYPE_NAME;
import static org.nuxeo.ecm.restapi.server.QueryObject.ORDERED_PARAMS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.search.client.repository.IgnoreIfRepositorySearchClientAndFulltextSearchDisabled;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.ecm.platform.picture.test.ImagingFeature;
import org.nuxeo.ecm.restapi.test.JsonNodeHelper;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ //
        CollectionFeature.class, //
        CoreSearchFeature.class, //
        ImagingFeature.class, //
        RestServerFeature.class })
@Deploy("org.nuxeo.adobe.cc.nuxeo-adobe-connector-core")
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
public class TestPageProviders {

    protected static final String TEST_WORKSPACE_PATH = "/default-domain/workspaces/test";

    @Inject
    protected CoreSession session;

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultJsonClient(
            () -> restServerFeature.getRestApiUrl());

    protected String testWorkspaceId;

    @Before
    public void before() {
        DocumentModel doc = session.getDocument(new PathRef(TEST_WORKSPACE_PATH));
        testWorkspaceId = doc.getId();
    }

    @Test
    public void testAllImages() {
        DocumentModel doc = session.createDocumentModel(TEST_WORKSPACE_PATH, "foo", PICTURE_TYPE_NAME);
        doc = session.createDocument(doc);

        final DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-all-images", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().get("uid").asText()).isEqualTo(finalDoc.getId());
        });
    }

    @Test
    public void testBrowse() {
        String rootId = session.getDocument(new PathRef("/default-domain/workspaces")).getId();

        testPageProvider("adobe-connector-browse", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().get("path").asText()).isEqualTo(TEST_WORKSPACE_PATH);
        }, rootId);
    }

    @Test
    public void testOthers() {
        DocumentModel doc = session.createDocumentModel(TEST_WORKSPACE_PATH, "foo", PICTURE_TYPE_NAME);
        doc = session.createDocument(doc);

        CollectionManager cm = Framework.getService(CollectionManager.class);
        DocumentModel collection = cm.createCollection(session, "my-collec", "dummy", TEST_WORKSPACE_PATH);
        cm.addToCollection(collection, doc, session);

        AtomicReference<String> collectionId = new AtomicReference<>();
        testPageProvider("adobe-connector-other_primary", (entries) -> {
            assertThat(entries).hasSize(1);
            collectionId.set(entries.getFirst().get("uid").asText());
        });

        DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-other_secondary", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().get("uid").asText()).isEqualTo(finalDoc.getId());
        }, collectionId.get());
    }

    @Test
    @ConditionalIgnore(condition = IgnoreIfRepositorySearchClientAndFulltextSearchDisabled.class, cause = "The PP needs fulltext search")
    public void testSearch() {
        DocumentModel doc = session.createDocumentModel(TEST_WORKSPACE_PATH, "foo", PICTURE_TYPE_NAME);
        doc.setPropertyValue("dc:description", "hello");
        session.createDocument(doc);

        doc = session.createDocumentModel(TEST_WORKSPACE_PATH, "bar", PICTURE_TYPE_NAME);
        doc.setPropertyValue("dc:description", "world");
        doc = session.createDocument(doc);

        Map<String, String> params = new HashMap<>();
        params.put("system_fulltext", "world");
        params.put("system_parentId", testWorkspaceId);

        DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-search", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().get("uid").asText()).isEqualTo(finalDoc.getId());
        }, params);
    }

    protected void testPageProvider(String ppName, Consumer<List<JsonNode>> consumer, Map<String, String> qarams,
            String... parameters) {
        transactionalFeature.nextTransaction();

        httpClient.buildGetRequest("/search/pp/" + ppName + "/execute")
                  .addQueryParameter(ORDERED_PARAMS, parameters)
                  .addQueryParameters(qarams)
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      List<JsonNode> logEntries = JsonNodeHelper.getEntries(node);
                      consumer.accept(logEntries);
                  });
    }

    protected void testPageProvider(String ppName, Consumer<List<JsonNode>> consumer, String... parameters) {
        testPageProvider(ppName, consumer, Map.of(), parameters);
    }
}
