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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.webengine.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features({ ServletContainerTransactionalFeature.class, CoreIOFeature.class, ClusterFeature.class, LogFeature.class })
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.webengine.rest")
@Deploy("org.nuxeo.ecm.webengine.core")
@Deploy("org.nuxeo.ecm.webengine.core.test")
@SuppressWarnings("unchecked")
public class TestWebEngineApplication {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Test
    public void testGetSimpleString() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-string");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("value1", json.get("key1"));
        assertEquals("value2", json.get("key2"));
    }

    @Test
    public void testGetSimpleMap() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-map");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("value1", json.get("key1"));
        assertEquals("value2", json.get("key2"));
    }

    @Test
    public void testGetSimpleResponse() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-response");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("value1", json.get("key1"));
        assertEquals("value2", json.get("key2"));
    }

    @Test
    @LoggerLevel(klass = TransactionHelper.class, level = "FATAL") // mute No transaction associated with current thread
    @LoggerLevel(klass = WebEngineExceptionMapper.class, level = "FATAL") // mute default server logging error
    public void testGetSimpleException() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-exception");

        assertEquals(500, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("exception", json.get("entity-type"));
        assertEquals(500, json.get("status"));
        assertEquals("Internal Server Error", json.get("message"));
    }

    @Test
    public void testPostSimpleString() throws IOException {
        HttpResponse<String> response = executeRequest("/webengine-test/simple-string",
                builder -> builder.POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "WebEngine Tester"
                        }""")));

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("Hello WebEngine Tester, String body was received!", json.get("message"));
    }

    @Test
    public void testPostSimpleMap() throws IOException {
        HttpResponse<String> response = executeRequest("/webengine-test/simple-map",
                builder -> builder.POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "WebEngine Tester"
                        }""")));

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("Hello WebEngine Tester, Map body was received!", json.get("message"));
    }

    @Test
    public void testGetSimpleTemplate() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-template");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("simple-template.ftl", json.get("location"));
    }

    @Test
    public void testGetSimpleView() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/simple-view");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("view/WebEngineTestRoot/simple-view.ftl", json.get("location"));
    }

    @Test
    public void testGetSimpleViewWithMediaType() throws IOException {
        HttpResponse<String> response = executeRequest("/webengine-test/simple-view",
                builder -> builder.GET().setHeader("Content-Type", "application/xml"));

        assertEquals(200, response.statusCode());
        Map<String, Object> xml = new ObjectMapper(new XmlFactory()).readValue(response.body(), Map.class);
        assertEquals("view/WebEngineTestRoot/simple-view-xml.ftl", xml.get("location"));
    }

    @Test
    public void testGetWebObject() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/web-object");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("WebEngineTestObject", json.get("location"));
        assertEquals("WebEngineTestRoot", json.get("origin"));
    }

    // NXP-21290 - test injection of jersey object
    @Test
    public void testGetWebObjectMyHeader() throws IOException {
        HttpResponse<String> response = executeRequest("/webengine-test/web-object/my-header",
                builder -> builder.GET().setHeader("X-My-Header", "What a value"));

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("What a value", json.get("header"));
    }

    @Test
    public void testGetWebObjectMyObject() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/web-object/my-object");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("MyObjectPerRequestProvider", json.get("location"));
        var providerReference = json.get("providerReference");
        var uuid = json.get("uuid");

        response = executeGETRequest("/webengine-test/web-object/my-object");

        assertEquals(200, response.statusCode());
        json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("MyObjectPerRequestProvider", json.get("location"));
        // providerReference should be a singleton
        assertEquals(providerReference, json.get("providerReference"));
        // uuids should be different as the injected object has PerRequest scope
        assertNotEquals(uuid, json.get("uuid"));
    }

    @Test
    public void testGetWebAdapter() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/@web-adapter");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("WebEngineTestAdapter", json.get("location"));
        assertEquals("WebEngineTestRoot", json.get("origin"));
    }

    @Test
    public void testGetWebAdapterGetWebObject() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/@web-adapter/web-object");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("WebEngineTestObject", json.get("location"));
        assertEquals("WebEngineTestRoot/WebEngineTestAdapter", json.get("origin"));
    }

    // NXP-21398
    @Test
    public void testGetWebAdapterGetWebObjectGetWebAdapter() throws IOException {
        HttpResponse<String> response = executeGETRequest("/webengine-test/@web-adapter/web-object/@web-adapter");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = MAPPER.readValue(response.body(), Map.class);
        assertEquals("WebEngineTestAdapter", json.get("location"));
        assertEquals("WebEngineTestRoot/WebEngineTestAdapter/WebEngineTestObject", json.get("origin"));
    }

    // TODO review, should we keep it?
    protected HttpResponse<String> executeGETRequest(String endpoint) {
        return executeRequest(endpoint, HttpRequest.Builder::GET);
    }

    protected HttpResponse<String> executeRequest(String endpoint, UnaryOperator<HttpRequest.Builder> customizer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(
                    new URI("http://localhost:" + servletContainerFeature.getPort() + endpoint))
                                                     .setHeader("Content-Type", "application/json");
            HttpRequest request = customizer.andThen(HttpRequest.Builder::build).apply(builder);
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new AssertionError("Unexpected exception", e);
        }
    }
}
