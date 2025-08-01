/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.enums.NuxeoOAuth2TokenType;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-oauth2provider-config.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-oauth2-directory-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OAuth2ObjectTest {

    public static final String OAUTH2_PROVIDER_TYPE = "nuxeoOAuth2ServiceProvider";

    public static final String OAUTH2_PROVIDERS_TYPE = "nuxeoOAuth2ServiceProviders";

    public static final String OAUTH2_TOKEN_TYPE = "nuxeoOAuth2Token";

    public static final String OAUTH2_TOKENS_TYPE = "nuxeoOAuth2Tokens";

    public static final String OAUTH2_CLIENT_TYPE = "oauth2Client";

    /**
     * @deprecated since 11.1. Use {@link OAuth2ClientService#OAUTH2CLIENT_SCHEMA}
     */
    @Deprecated
    public static final String OAUTH2_CLIENTS_TYPE = OAuth2ClientService.OAUTH2CLIENT_SCHEMA;

    public static final String TEST_OAUTH2_PROVIDER = "test-oauth2-provider";

    public static final String TEST_OAUTH2_PROVIDER_2 = "test-oauth2-provider-2";

    public static final String TOKEN_STORE = "org.nuxeo.server.token.store";

    public static final String TEST_OAUTH2_CLIENTID = "clientId";

    public static final String TEST_OAUTH2_USER = "Administrator";

    public static final String TEST_OAUTH2_SERVICE_USERID = TEST_OAUTH2_USER + "@email.com";

    public static final String TEST_OAUTH2_ACCESS_TOKEN = "y38Hs3_sdas98l";

    protected static final String PROVIDER_PATH = "oauth2/provider";

    protected static final String TOKEN_PATH = "oauth2/token";

    protected static final String CLIENT_PATH = "oauth2/client";

    protected static final String PROVIDER_TOKEN_PATH = "oauth2/token/provider";

    protected static final String CLIENT_TOKEN_PATH = "oauth2/token/client";

    protected static final String TEST_CLIENT = "my-client";

    protected static final String TEST_CLIENT_NAME = "my-client-name";

    protected static final String TEST_CLIENT_2 = "my-client-2";

    protected static final String TEST_CLIENT_NAME_2 = "my-second-client-name";

    /**
     * @since 11.1
     */
    protected static final String TEST_CLIENT_3 = "my-client-3";

    /**
     * @since 11.1
     */
    protected static final String TEST_CLIENT_NAME_3 = "my-third-client-name";

    /**
     * @since 11.1
     */
    protected static final String TOKEN_PATH_NUXEO_AS_PROVIDER = String.format("oauth2/token/%s",
            NuxeoOAuth2TokenType.AS_PROVIDER);

    /**
     * @since 11.1
     */
    protected static final String TOKEN_PATH_NUXEO_AS_CLIENT = String.format("oauth2/token/%s",
            NuxeoOAuth2TokenType.AS_CLIENT);

    /**
     * @since 11.1
     */
    protected static final String SEARCH_TOKENS_PATH = "oauth2/token/search";

    /**
     * @since 11.1
     */
    protected static final String SEARCH_TOKENS_QUERY_PARAM = "q";

    protected static final String AUTHORIZATION_SERVER_URL = "https://test.oauth2.provider/authorization";

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultJsonClient(
            () -> restServerFeature.getRestApiUrl());

    protected static String getScopeUrl(int id) {
        return "https://test.oauth2.provider/scopes/scope" + Integer.toString(id);
    }

    protected static String getProviderPath(String providerId) {
        return PROVIDER_PATH + "/" + providerId;
    }

    protected static String getTokenPath(String providerId) {
        return getProviderPath(providerId) + "/token";
    }

    protected static String getClientPath(String clientId) {
        return CLIENT_PATH + "/" + clientId;
    }

    // test oauth2/provider

    @Test
    public void iCanGetProviders() {
        httpClient.buildGetRequest(PROVIDER_PATH).executeAndConsume(new JsonNodeHandler(), node -> {
            assertEquals(OAUTH2_PROVIDERS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(2, node.get("entries").size());
            verifyProvider(node.get("entries").get(0), TEST_OAUTH2_PROVIDER, true);
            verifyProvider(node.get("entries").get(1), TEST_OAUTH2_PROVIDER_2, false);
        });
    }

    @Test
    public void iCanGetProvider() {
        httpClient.buildGetRequest(getProviderPath(TEST_OAUTH2_PROVIDER))
                  .executeAndConsume(new JsonNodeHandler(), node -> verifyProvider(node, TEST_OAUTH2_PROVIDER, true));
    }

    @Test
    public void iCantGetInvalidProvider() {
        httpClient.buildGetRequest(getProviderPath("fake"))
                  .executeAndConsume(new JsonNodeHandler(SC_NOT_FOUND),
                          node -> assertEquals("Invalid provider: fake", JsonNodeHelper.getErrorMessage(node)));
    }

    @Test
    public void iCanCreateProvider() {
        String serviceName = "myservice";
        String data = """
                {
                  "entity-type": "nuxeoOAuth2ServiceProvider",
                  "authorizationServerURL": "https://test.oauth2.provider/authorization",
                  "clientId": "clientId",
                  "clientSecret": "123secret321",
                  "description": "My Service",
                  "isEnabled": true,
                  "scopes": [
                    "https://test.oauth2.provider/scopes/scope0",
                    "https://test.oauth2.provider/scopes/scope1"
                  ],
                  "serviceName": "myservice",
                  "tokenServerURL": "https://test.oauth2.provider/token"
                }""";
        httpClient.buildPostRequest(PROVIDER_PATH)
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
        httpClient.buildPostRequest(PROVIDER_PATH)
                  .entity(data)
                  .executeAndConsume(new JsonNodeHandler(), node -> verifyProvider(node, serviceName, false));
    }

    @Test
    public void iCantCreateProviderWithBlankServiceName() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2ServiceProvider",
                  "authorizationServerURL": "https://test.oauth2.provider/authorization",
                  "clientId": "clientId",
                  "clientSecret": "123secret321",
                  "description": "My Service",
                  "isEnabled": true,
                  "scopes": [
                    "https://test.oauth2.provider/scopes/scope0",
                    "https://test.oauth2.provider/scopes/scope1"
                  ],
                  "serviceName": " ",
                  "tokenServerURL": "https://test.oauth2.provider/token"
                }""";
        httpClient.buildPostRequest(PROVIDER_PATH)
                  .entity(data)
                  .executeAndConsume(new JsonNodeHandler(SC_BAD_REQUEST),
                          node -> assertEquals(
                                  "java.lang.IllegalArgumentException: The provider's service name cannot be blank!",
                                  node.get("message").textValue()));
    }

    @Test
    public void iCanUpdateProvider() {
        httpClient.buildGetRequest(getProviderPath(TEST_OAUTH2_PROVIDER_2))
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals("clientId", node.get("clientId").textValue());
                      assertTrue(node.get("clientSecret").isNull());
                      assertFalse(node.get("isEnabled").booleanValue());
                  });

        String data = """
                {
                  "entity-type": "nuxeoOAuth2ServiceProvider",
                  "authorizationServerURL": "https://test.oauth2.provider/authorization",
                  "clientId": "myId",
                  "clientSecret": "123secret321",
                  "description": "Test OAuth2 Provider 2",
                  "isEnabled": true,
                  "scopes": [
                    "https://test.oauth2.provider/scopes/scope0",
                    "https://test.oauth2.provider/scopes/scope1"
                  ],
                  "serviceName": "test-oauth2-provider-2",
                  "tokenServerURL": "https://test.oauth2.provider/token"
                }""";
        httpClient.buildPutRequest(getProviderPath(TEST_OAUTH2_PROVIDER_2))
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
        httpClient.buildPutRequest(getProviderPath(TEST_OAUTH2_PROVIDER_2))
                  .entity(data)
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals("myId", node.get("clientId").textValue());
                      assertEquals("123secret321", node.get("clientSecret").textValue());
                      assertTrue(node.get("isEnabled").booleanValue());
                  });
    }

    @Test
    public void iCantUpdateInvalidProvider() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2ServiceProvider",
                  "authorizationServerURL": "https://test.oauth2.provider/authorization",
                  "clientId": "myId",
                  "clientSecret": "123secret321",
                  "description": "Test OAuth2 Provider 2",
                  "isEnabled": true,
                  "scopes": [
                    "https://test.oauth2.provider/scopes/scope0",
                    "https://test.oauth2.provider/scopes/scope1"
                  ],
                  "serviceName": "test-oauth2-provider-2",
                  "tokenServerURL": "https://test.oauth2.provider/token"
                }""";

        httpClient.buildPutRequest(getProviderPath("fake"))
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    @Test
    public void iCanDeleteProvider() {
        httpClient.buildDeleteRequest(getProviderPath(TEST_OAUTH2_PROVIDER_2))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        httpClient.buildDeleteRequest(getProviderPath(TEST_OAUTH2_PROVIDER))
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void iCantDeleteInvalidProvider() {
        httpClient.buildDeleteRequest(getProviderPath("fake"))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    // test oauth2/provider/{provider}/token

    @Test
    public void iCanGetValidProviderToken() {
        httpClient.buildGetRequest(getTokenPath(TEST_OAUTH2_PROVIDER))
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertEquals(TEST_OAUTH2_ACCESS_TOKEN, node.get("token").textValue()));
    }

    @Test
    public void iCantGetTokenInvalidProvider() {
        httpClient.buildGetRequest(getTokenPath("fake"))
                  .executeAndConsume(new JsonNodeHandler(SC_NOT_FOUND),
                          node -> assertEquals("Invalid provider: fake", JsonNodeHelper.getErrorMessage(node)));
    }

    protected void verifyProvider(JsonNode node, String serviceName, Boolean checkToken) {
        if (node.isArray()) {
            JsonNode child;
            for (int i = 0; i < node.size(); i++) {
                child = node.get(i);
                if (child.get("entity-type").textValue().equals(OAUTH2_PROVIDER_TYPE)
                        && child.get("serviceName").textValue().equals(serviceName)
                        && child.get("clientId").textValue().equals(TEST_OAUTH2_CLIENTID)
                        && (!checkToken || child.get("userId").textValue().equals(TEST_OAUTH2_SERVICE_USERID))
                        && child.get("authorizationURL")
                                .textValue()
                                .equals(AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID
                                        + "&redirect_uri=" + restServerFeature.getRestApiUrl() + "/site/oauth2/"
                                        + serviceName + "/callback" + "&response_type=code&scope=" + getScopeUrl(0)
                                        + "%20" + getScopeUrl(1))) {
                    return;
                }
            }
            fail("No provider found.");
        } else {
            assertEquals(OAUTH2_PROVIDER_TYPE, node.get("entity-type").textValue());
            assertEquals(serviceName, node.get("serviceName").textValue());
            assertEquals(TEST_OAUTH2_CLIENTID, node.get("clientId").textValue());
            assertEquals(
                    AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID + "&redirect_uri="
                            + servletContainerFeature.getHttpUrl() + "/site/oauth2/" + serviceName + "/callback"
                            + "&response_type=code&scope=" + getScopeUrl(0) + "%20" + getScopeUrl(1),
                    node.get("authorizationURL").textValue());
            if (checkToken) {
                assertEquals(TEST_OAUTH2_SERVICE_USERID, node.get("userId").textValue());
            }
        }
    }

    // test oauth2/token
    @Test
    public void iCanGetAllTokens() {
        httpClient.buildGetRequest(TOKEN_PATH).executeAndConsume(new JsonNodeHandler(), node -> {
            assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(5, node.get("entries").size());
            verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "Administrator", "2017-05-09 11:11:11");
            verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        });

        httpClient.buildGetRequest(TOKEN_PATH)
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanGetTokensProvidedByNuxeo() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-21 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(TOKEN_PATH_NUXEO_AS_PROVIDER)
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanSearchTokensByNuxeoLogin() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": null,
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(SEARCH_TOKENS_PATH)
                  .addQueryParameter(SEARCH_TOKENS_QUERY_PARAM, "er1")
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanSearchTokensByFullNuxeoLogin() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": null,
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(SEARCH_TOKENS_PATH)
                  .addQueryParameter(SEARCH_TOKENS_QUERY_PARAM, "user1")
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanSearchTokensByServiceName() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-21 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(SEARCH_TOKENS_PATH)
                  .addQueryParameter(SEARCH_TOKENS_QUERY_PARAM, "token.store")
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanSearchTokensByFullServiceName() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-21 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(SEARCH_TOKENS_PATH)
                  .addQueryParameter(SEARCH_TOKENS_QUERY_PARAM, "org.nuxeo.server.token.store")
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanSearchTokensByServiceNameOrNuxeoLogin() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "Administrator",
                      "clientId": null,
                      "serviceLogin": "Administrator@email.com",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": null,
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "isShared": false,
                      "clientId": null,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-08 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-21 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "org.nuxeo.server.token.store",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": "my-client",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-20 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(SEARCH_TOKENS_PATH)
                  .addQueryParameter(SEARCH_TOKENS_QUERY_PARAM, "u")
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotGetTokensProvidedByNuxeoByUnauthorizedUsers() {
        httpClient.buildGetRequest(TOKEN_PATH_NUXEO_AS_PROVIDER)
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanGetTokensConsumedByNuxeo() {
        String data = """
                {
                  "entity-type": "nuxeoOAuth2Tokens",
                  "entries": [
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "Administrator",
                      "clientId": null,
                      "serviceLogin": "Administrator@email.com",
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user1",
                      "serviceLogin": "my1@mail ",
                      "clientId": null,
                      "isShared": false,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-09 11:11:11"
                    },
                    {
                      "entity-type": "nuxeoOAuth2Token",
                      "serviceName": "test-oauth2-provider",
                      "nuxeoLogin": "user2",
                      "serviceLogin": "my2@mail ",
                      "isShared": false,
                      "clientId": null,
                      "sharedWith": ["null"],
                      "creationDate": "2017-05-08 11:11:11"
                    }
                  ]
                }""";
        httpClient.buildGetRequest(TOKEN_PATH_NUXEO_AS_CLIENT)
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotGetTokensConsumedByNuxeoByUnauthorizedUsers() {
        httpClient.buildGetRequest(TOKEN_PATH_NUXEO_AS_CLIENT)
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void shouldFailWhenRetrieveTokensWithoutValidType() {
        httpClient.buildGetRequest("/oauth2/token/anyType")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    // test oauth2/token/provider
    @Test
    public void iCanGetUserProviderTokens() {
        httpClient.buildGetRequest(PROVIDER_TOKEN_PATH)
                  .credentials("user1", "user1")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
                      assertNotNull(node.get("entries"));
                      assertEquals(1, node.get("entries").size());
                      verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
                  });
    }

    // test oauth2/token/provider/{provider}/user/{user}
    @Test
    public void iCanGetProviderToken() {
        var jsonNodeHandler = new JsonNodeHandler();
        var statusCodeHandler = new HttpStatusCodeHandler();

        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11"));

        // will get a 404 if not token is found for the provider/user pair
        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user3")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NOT_FOUND, status.intValue()));

        // users must be able to fetch their own tokens
        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .credentials("user1", "user1")
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11"));

        // but not other users'
        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user2")
                  .credentials("user1", "user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void iCanUpdateProviderToken() {
        var jsonNodeHandler = new JsonNodeHandler();

        String data = """
                {
                  "entity-type": "nuxeoOAuth2Token",
                  "clientId": null,
                  "creationDate": "2017-05-10 11:11:11",
                  "isShared": false,
                  "nuxeoLogin": "user1",
                  "serviceLogin": "my1@mail",
                  "serviceName": "test-oauth2-provider",
                  "sharedWith": []
                }""";
        httpClient.buildPutRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .entity(data)
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-10 11:11:11"));

        // users must be able to update their own tokens
        data = """
                {
                  "entity-type": "nuxeoOAuth2Token",
                  "clientId": null,
                  "creationDate": "2017-05-11 11:11:11",
                  "isShared": false,
                  "nuxeoLogin": "user1",
                  "serviceLogin": "my1@mail",
                  "serviceName": "test-oauth2-provider",
                  "sharedWith": []
                }""";
        httpClient.buildPutRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-11 11:11:11"));

        // but not other users'
        httpClient.buildPutRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .credentials("user2", "user2")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void iCanDeleteProviderToken() {
        // a user cannot delete some else's token
        var statusCodeHandler = new HttpStatusCodeHandler();
        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .credentials("user2", "user2")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_FORBIDDEN, status.intValue()));

        // but can delete his/her own
        httpClient.buildDeleteRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .credentials("user1", "user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        // and admins can delete everybody's
        httpClient.buildDeleteRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user2")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        httpClient.buildGetRequest(TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    // test oauth2/token/client
    @Test
    public void iCanGetUserClientTokens() {
        httpClient.buildGetRequest(CLIENT_TOKEN_PATH)
                  .credentials("user1", "user1")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
                      assertNotNull(node.get("entries"));
                      assertEquals(1, node.get("entries").size());
                      verifyToken(node.get("entries"), TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11");
                  });
    }

    // test oauth2/token/client/{provider}/user/{user}
    @Test
    public void iCanGetClientToken() {
        var jsonNodeHandler = new JsonNodeHandler();
        var statusCodeHandler = new HttpStatusCodeHandler();

        httpClient.buildGetRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11"));

        // will get a 404 if not token is found for the provider/user pair
        httpClient.buildGetRequest(TOKEN_PATH + "/client/unknown-client/user/user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NOT_FOUND, status.intValue()));

        // users must be able to fetch their own tokens
        httpClient.buildGetRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .credentials("user1", "user1")
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11"));

        // but not other users'
        httpClient.buildGetRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user2")
                  .credentials("user1", "user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void iCanUpdateClientToken() {
        var jsonNodeHandler = new JsonNodeHandler();

        String data = """
                {
                  "entity-type": "nuxeoOAuth2Token",
                  "clientId": "my-client",
                  "creationDate": "2017-05-21 11:11:11",
                  "isShared": false,
                  "nuxeoLogin": "user1",
                  "serviceLogin": "my1@mail",
                  "serviceName": "org.nuxeo.server.token.store",
                  "sharedWith": []
                }""";
        httpClient.buildPutRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .entity(data)
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-21 11:11:11"));

        // users must be able to update their own tokens
        data = """
                {
                  "entity-type": "nuxeoOAuth2Token",
                  "clientId": "my-client",
                  "creationDate": "2017-05-22 11:11:11",
                  "isShared": false,
                  "nuxeoLogin": "user1",
                  "serviceLogin": "my1@mail",
                  "serviceName": "org.nuxeo.server.token.store",
                  "sharedWith": []
                }""";
        httpClient.buildPutRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(jsonNodeHandler,
                          node -> verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-22 11:11:11"));

        // but not other users'
        httpClient.buildPutRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .credentials("user2", "user2")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void iCanDeleteClientToken() {
        var statusCodeHandler = new HttpStatusCodeHandler();
        // a user cannot delete some else's token
        httpClient.buildDeleteRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .credentials("user2", "user2")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_FORBIDDEN, status.intValue()));

        // but can delete his/her own
        httpClient.buildDeleteRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .credentials("user1", "user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        // and admins can delete everybody's
        httpClient.buildDeleteRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user2")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        httpClient.buildDeleteRequest(TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")
                  .executeAndConsume(statusCodeHandler, status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    protected void verifyToken(JsonNode node, String serviceName, String clientId, String nxuser, String creationDate) {
        if (node.isArray()) {
            JsonNode child;
            for (int i = 0; i < node.size(); i++) {
                child = node.get(i);
                if (child.get("entity-type").textValue().equals(OAUTH2_TOKEN_TYPE)
                        && child.get("serviceName").textValue().equals(serviceName)
                        && child.get("nuxeoLogin").textValue().equals(nxuser)
                        && child.get("creationDate").textValue().equals(creationDate)
                        && (clientId == null || child.get("clientId").textValue().equals(clientId))) {
                    return;
                }
            }
            fail("No token found.");
        } else {
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            assertEquals(serviceName, node.get("serviceName").textValue());
            assertEquals(nxuser, node.get("nuxeoLogin").textValue());
            assertEquals(creationDate, node.get("creationDate").textValue());
            if (clientId != null) {
                assertEquals(clientId, node.get("clientId").textValue());
            }
        }
    }

    // test oauth2/client
    @Test
    public void iCanGetClients() {
        String data = """
                {
                  "entity-type": "oauth2Clients",
                  "entries": [
                    {
                      "entity-type": "oauth2Client",
                      "name": "%s",
                      "redirectURIs": [
                        "nuxeo://authorize"
                       ],
                      "secret": "2113425ygfsd",
                      "id": "%s",
                      "isAutoGrant": true,
                      "isEnabled": true
                    },
                    {
                      "entity-type": "oauth2Client",
                      "name": "%s",
                      "redirectURIs": [
                        "nuxeo://authorize"
                       ],
                      "secret": "s234dsfsdss",
                      "id": "%s",
                      "isAutoGrant": true,
                      "isEnabled": true
                    },
                    {
                      "entity-type": "oauth2Client",
                      "name": "%s",
                      "redirectURIs": [
                        "nuxeo://authorize"
                      ],
                      "secret": "s234dsfsdss",
                      "id": "%s",
                      "isAutoGrant": false,
                      "isEnabled": false
                    }
                  ]
                }
                """.formatted(TEST_CLIENT_NAME, TEST_CLIENT, TEST_CLIENT_NAME_2, TEST_CLIENT_2, TEST_CLIENT_NAME_3,
                TEST_CLIENT_3);

        httpClient.buildGetRequest(CLIENT_PATH)
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    @Test
    public void iCanGetClient() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "name": "%s",
                  "secret": "2113425ygfsd",
                  "id": "%s",
                  "isAutoGrant": true,
                  "isEnabled": true,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""".formatted(TEST_CLIENT_NAME, TEST_CLIENT);

        httpClient.buildGetRequest(getClientPath(TEST_CLIENT))
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    @Test
    public void iCantGetInvalidClient() {
        String clientId = "fake";
        String data = createResponseError(String.format("Invalid client: %s", clientId), SC_NOT_FOUND);
        httpClient.buildGetRequest(getClientPath(clientId))
                  .executeAndConsume(new StringHandler(SC_NOT_FOUND),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.LENIENT));
    }

    /** @since 2021.31 */
    @Test
    public void cannotGetClientsByUnauthorizedUsers() {
        httpClient.buildGetRequest(CLIENT_PATH)
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /** @since 2021.31 */
    @Test
    public void cannotGetClientByUnauthorizedUsers() {
        httpClient.buildGetRequest(CLIENT_PATH + "/" + TEST_CLIENT)
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void cannotCreateClientByUnauthorizedUsers() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-4",
                  "name": "Nuxeo Client 4",
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""";
        httpClient.buildPostRequest(CLIENT_PATH)
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void cannotDeleteClientByUnauthorizedUsers() {
        httpClient.buildDeleteRequest(getClientPath(TEST_CLIENT_3))
                  .credentials("user1", "user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void cannotUpdateClientByUnauthorizedUsers() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "Nuxeo Client 5",
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""";
        httpClient.buildPutRequest(getClientPath(TEST_CLIENT))
                  .credentials("user1", "user1")
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotMakeOperationOnClientWithoutId() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "name": "Nuxeo Client 2",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""";
        String responseError = createResponseError("Client Id is required", SC_BAD_REQUEST);
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                          body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotMakeOperationOnClientWithoutName() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-6",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""";
        String responseError = createResponseError("Client name is required", SC_BAD_REQUEST);
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                          body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotMakeOperationOnClientWithoutRedirectURIs() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "client-id-7",
                  "name": "Nuxeo Client 7",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true
                }""";
        String responseError = createResponseError("Redirect URIs is required", SC_BAD_REQUEST);
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                          body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotMakeOperationOnClientWithoutValidRedirectURIs() {
        List<String> invalidURIs = List.of("http://authorize", "http://localhost.somecompany.com");
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "client-id-7",
                  "name": "Nuxeo Client 7",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true,
                  "redirectURIs":["%s"]
                }""";
        for (String uri : invalidURIs) {
            String responseError = createResponseError(String.format("'%s' is not a valid redirect URI", uri),
                    SC_BAD_REQUEST);
            httpClient.buildPostRequest(CLIENT_PATH)
                      .entity(String.format(data, uri))
                      .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                              body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));
        }
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotReCreateExistingClient() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "Nuxeo Client 8",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""".formatted(TEST_CLIENT);
        String responseError = createResponseError(String.format("Client with id '%s' already exists", TEST_CLIENT),
                SC_BAD_REQUEST);
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                          body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotUpdateUnExistingClient() {
        String clientId = "unExisting-client-id";

        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "Nuxeo Client 9",
                  "isEnabled": false,
                  "secret": "4321",
                  "isAutoGrant": false,
                  "redirectURIs": [
                    "nuxeo://authorization"
                  ]
                }""".formatted(clientId);
        httpClient.buildPutRequest(getClientPath(clientId))
                  .entity(data)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotDeleteUnExistingClient() {
        String clientId = "unExisting-client-id";
        httpClient.buildDeleteRequest(getClientPath(clientId))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanCreateClient() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-10",
                  "name": "Nuxeo Client 10",
                  "isEnabled": true,
                  "secret": "1234",
                  "isAutoGrant": true,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""";
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_CREATED),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanCreateClientWithRequiredFieldsOnly() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-11",
                  "name": "Nuxeo Client 11",
                  "redirectURIs": [
                    "nuxeo://authorize","nuxeo://authorize2", "nuxeo://authorize3"
                  ]
                }""";

        String expected = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-11",
                  "name": "Nuxeo Client 11",
                  "isEnabled": false,
                  "secret": null,
                  "isAutoGrant": false,
                  "redirectURIs": [
                    "nuxeo://authorize2", "nuxeo://authorize","nuxeo://authorize3"
                  ]
                }""";
        httpClient.buildPostRequest(CLIENT_PATH)
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_CREATED),
                          body -> JSONAssert.assertEquals(expected, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanUpdateClientWithRequiredFieldsOnly() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "%s",
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ]
                }""".formatted(TEST_CLIENT_NAME, TEST_CLIENT);

        String expected = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "%s",
                  "isEnabled": false,
                  "isAutoGrant": false,
                  "redirectURIs": [
                    "nuxeo://authorize"
                  ],
                  "secret": null
                }""".formatted(TEST_CLIENT_NAME, TEST_CLIENT);
        httpClient.buildPutRequest(getClientPath(TEST_CLIENT))
                  .entity(data)
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(expected, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCannotUpdateClientWithExistingClientId() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "%s",
                  "name": "%s",
                  "isEnabled": false,
                  "secret": "4321",
                  "isAutoGrant": false,
                  "redirectURIs": [
                    "nuxeo://authorization"
                  ]
                }""".formatted(TEST_CLIENT_2, TEST_CLIENT_NAME_2);
        String responseError = createResponseError(String.format("Client with id '%s' already exists", TEST_CLIENT_2),
                SC_BAD_REQUEST);
        httpClient.buildPutRequest(getClientPath(TEST_CLIENT))
                  .entity(data)
                  .executeAndConsume(new StringHandler(SC_BAD_REQUEST),
                          body -> JSONAssert.assertEquals(responseError, body, JSONCompareMode.LENIENT));

    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanUpdateClient() {
        String data = """
                {
                  "entity-type": "oauth2Client",
                  "id": "nuxeo-client-2",
                  "name": "Nuxeo Client 207",
                  "isEnabled": false,
                  "secret": "4321",
                  "isAutoGrant": false,
                  "redirectURIs": [
                    "nuxeo://authorization"
                  ]
                }""";
        httpClient.buildPutRequest(getClientPath(TEST_CLIENT))
                  .entity(data)
                  .executeAndConsume(new StringHandler(),
                          body -> JSONAssert.assertEquals(data, body, JSONCompareMode.NON_EXTENSIBLE));
    }

    /**
     * @since 11.1
     */
    @Test
    public void iCanDeleteClient() {
        String clientPath = getClientPath(TEST_CLIENT_3);
        httpClient.buildDeleteRequest(clientPath)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        // Try to get the deleted resource
        httpClient.buildGetRequest(clientPath)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    protected String createResponseError(String message, int status) {
        return """
                {
                  "entity-type": "exception",
                  "status": %s,
                  "message": "%s"
                }
                """.formatted(status, message);
    }
}
