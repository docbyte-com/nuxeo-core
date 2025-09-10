/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ftest.server;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.nuxeo.functionaltests.LogTestWatchman;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;

/**
 * Tests the Nuxeo Server login page.
 *
 * @since 2025.0
 */
public class ITLoginPageTest {

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder().redirectsEnabled(false).build();

    @Rule
    public MethodRule watchman = new LogTestWatchman();

    @Test
    public void testLoginPage() {
        var status = httpClient.buildGetRequest("/login.jsp").execute(new HttpStatusCodeHandler());
        assertEquals(SC_OK, (int) status);
    }

}
