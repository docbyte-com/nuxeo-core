/*
 * (C) Copyright 2017-2024 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.HttpResponse;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;

/**
 * HTML representation of the {@code oauth2Grant.jsp} page.
 *
 * @since 9.2
 */
public class OAuth2GrantPage {

    protected static final String ATTRIBUTE_NAME = "name";

    protected static final String ATTRIBUTE_VALUE = "value";

    protected static final String ELEMENT_FORM = "form";

    protected static final String INPUT_CLIENT_ID = "client_id";

    protected static final String INPUT_CODE_CHALLENGE = "code_challenge";

    protected static final String INPUT_CODE_CHALLENGE_METHOD = "code_challenge_method";

    protected static final String INPUT_DENY_ACCESS = "deny_access";

    protected static final String INPUT_GRANT_ACCESS = "grant_access";

    protected static final String INPUT_REDIRECT_URI = "redirect_uri";

    protected static final String INPUT_RESPONSE_TYPE = "response_type";

    protected static final String INPUT_SCOPE = "scope";

    protected static final String INPUT_STATE = "state";

    protected Element form;

    protected String clientId;

    protected String responseType;

    protected String redirectURI;

    protected String scope;

    protected String state;

    protected String codeChallenge;

    protected String codeChallengeMethod;

    protected HttpClientTestRule httpClient;

    public static OAuth2GrantPage getDefaultGrantPage(HttpClientTestRule client) {
        return getOAuth2GrantPageBuilder(client).build();
    }

    public static OAuth2GrantPage getEmptyGrantPage(HttpClientTestRule client) {
        return getGrantPage(client, "/oauth2Grant.jsp");
    }

    public static OAuth2GrantPage getGrantPage(HttpClientTestRule client, String path) {
        return new OAuth2GrantPage(
                new Source(client.buildGetRequest(path).executeAndThen(HttpResponse::getEntityString)));
    }

    public static OAuth2GrantPageBuilder getOAuth2GrantPageBuilder(HttpClientTestRule client) {
        return new OAuth2GrantPageBuilder(client);
    }

    protected static class OAuth2GrantPageBuilder {

        protected HttpClientTestRule client;

        protected Map<String, String> extraParameters;

        protected OAuth2GrantPageBuilder(HttpClientTestRule client) {
            this.client = client;
        }

        public OAuth2GrantPageBuilder extraParameters(final Map<String, String> extraParameters) {
            this.extraParameters = extraParameters;
            return this;
        }

        public OAuth2GrantPage build() {
            String path = "/oauth2/authorize?client_id=test-client&response_type=code";
            if (extraParameters != null) {
                path = URIUtils.addParametersToURIQuery(path, extraParameters);
            }
            OAuth2GrantPage grantPage = getGrantPage(client, path);
            grantPage.checkClientName("Test Client");
            grantPage.checkResponseType("code");
            grantPage.checkClientId("test-client");
            int fieldCount = 2;
            if (extraParameters != null) {
                extraParameters.forEach(grantPage::checkExtraParameter);
                fieldCount += extraParameters.size();
            }
            grantPage.checkFieldCount(fieldCount);
            grantPage.setHttpClient(client);
            return grantPage;
        }

    }

    public OAuth2GrantPage(Source html) {
        form = html.getFirstElement(ELEMENT_FORM);
        clientId = getInputValue(INPUT_CLIENT_ID);
        responseType = getInputValue(INPUT_RESPONSE_TYPE);
        redirectURI = getInputValue(INPUT_REDIRECT_URI);
        scope = getInputValue(INPUT_SCOPE);
        state = getInputValue(INPUT_STATE);
        codeChallenge = getInputValue(INPUT_CODE_CHALLENGE);
        codeChallengeMethod = getInputValue(INPUT_CODE_CHALLENGE_METHOD);
    }

    public void checkClientName(String expected) {
        assertTrue(form.getTextExtractor().toString().contains(expected));
    }

    public void checkClientId(String expected) {
        assertEquals(expected, clientId);
    }

    public void checkResponseType(String expected) {
        assertEquals(expected, responseType);
    }

    public void checkExtraParameter(String name, String expected) {
        assertEquals(expected, getInputValue(name));
    }

    public void checkFieldCount(int count) {
        assertEquals(count, form.getAllElements("input").size());
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public void setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public void setHttpClient(HttpClientTestRule httpClient) {
        this.httpClient = httpClient;
    }

    public Source deny() {
        return postForm(false);
    }

    public String getDenyLocation() {
        return getPostLocation(false);
    }

    public Source grant() {
        return postForm(true);
    }

    public String getGrantLocation() {
        return getPostLocation(true);
    }

    protected Source postForm(boolean grant) {
        return new Source(httpClient.buildPostRequest("/oauth2/authorize_submit")
                                    .entity(getFormData(grant))
                                    .executeAndThen(HttpResponse::getEntityString));
    }

    protected String getPostLocation(boolean grant) {
        return httpClient.buildPostRequest("/oauth2/authorize_submit")
                         .entity(getFormData(grant))
                         .executeAndThen(response -> response.getLocation().toString());
    }

    protected Map<String, String> getFormData(boolean grant) {
        Map<String, String> formData = new HashMap<>();
        formData.put(INPUT_RESPONSE_TYPE, responseType);
        formData.put(INPUT_CLIENT_ID, clientId);
        if (redirectURI != null) {
            formData.put(INPUT_REDIRECT_URI, redirectURI);
        }
        if (scope != null) {
            formData.put(INPUT_SCOPE, scope);
        }
        if (state != null) {
            formData.put(INPUT_STATE, state);
        }
        if (codeChallenge != null) {
            formData.put(INPUT_CODE_CHALLENGE, codeChallenge);
        }
        if (codeChallengeMethod != null) {
            formData.put(INPUT_CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }
        if (grant) {
            formData.put(INPUT_GRANT_ACCESS, "1");
        } else {
            formData.put(INPUT_DENY_ACCESS, "1");
        }
        return formData;
    }

    protected String getInputValue(String name) {
        var element = form.getFirstElement(ATTRIBUTE_NAME, name, true);
        if (element == null) {
            return null;
        }
        return element.getAttributeValue(ATTRIBUTE_VALUE);
    }

}
