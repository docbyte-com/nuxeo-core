/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ftest.web.ui;

import static org.junit.Assert.fail;
import static org.nuxeo.http.test.HttpClientTestRule.NUXEO_URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.functionaltests.LogTestWatchman;
import org.nuxeo.functionaltests.RestTestRule;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * @since 9.10
 */
public class ITJSFToWebUITest {

    public static final String WORKSPACES_PATH = "/default-domain/workspaces";

    public static final String WORKSPACE_TYPE = "Workspace";

    public static final String WORKSPACE_NAME = "Workspace";

    public static final String WORKSPACE_PATH = WORKSPACES_PATH + "/" + WORKSPACE_NAME; // NOSONAR

    public static final String JSF_ID_URL = "/nxdoc/default/%s/view_documents";

    public static final String JSF_PATH_URL = "/nxpath/default%s@view_documents";

    public static final String WEB_UI_ID_URL = "%s/ui/#!/doc/default/%s";

    public static final String WEB_UI_PATH_URL = "%s/ui/#!/browse%s";

    @Rule
    public MethodRule watchman = new LogTestWatchman();

    @Rule
    public final RestTestRule restHelper = new RestTestRule();

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
                                                                   .adminCredentials()
                                                                   .redirectsEnabled(false)
                                                                   .build();

    protected String docId;

    @Before
    public void before() {
        docId = restHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_NAME);
    }

    @Test
    public void testWebUIRedirect() {
        String location = getLocation(String.format(JSF_ID_URL, docId));
        checkURL(URIUtils.getURIPath(location));

        location = getLocation(String.format(JSF_PATH_URL, WORKSPACE_PATH));
        checkURL(URIUtils.getURIPath(location));
    }

    protected String getLocation(String path) {
        return httpClient.buildGetRequest(path).executeAndThen(response -> response.getLocation().toString());
    }

    protected void checkURL(String url) {
        String expectedIdURL = String.format(WEB_UI_ID_URL, NUXEO_URL, docId);
        String expectedPathURL = String.format(WEB_UI_PATH_URL, NUXEO_URL, WORKSPACE_PATH);
        if (!(expectedIdURL.equals(url) || expectedPathURL.equals(url))) {
            fail(String.format("URL '%s' does not equals '%s' nor '%s'", url, expectedIdURL, expectedPathURL));
        }
    }
}
