/*
 * (C) Copyright 2016-2024 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.functionaltests.LogTestWatchman;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * Tests the Nuxeo Server startup page.
 *
 * @since 8.10
 */
public class ITStartupPageTest {

    @Rule
    public MethodRule watchman = new LogTestWatchman();

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder().redirectsEnabled(false).build();

    @Test
    public void testStartupPage() {
        var currentURL = httpClient.buildGetRequest("/").executeAndThen(response -> response.getLocation().toString());
        assertEquals("http://localhost:8080/nuxeo/" + LoginScreenHelper.DEFAULT_STARTUP_PAGE_PATH, currentURL);
    }

}
