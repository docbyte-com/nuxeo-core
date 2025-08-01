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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoForwardDeprecatedPathFilter.DeprecatedPath;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, RuntimeFeature.class })
@SuppressWarnings("deprecation")
public class TestDeprecatedPath {

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = NuxeoForwardDeprecatedPathFilter.class, logLevel = "INFO")
    @WithFrameworkProperty(name = NuxeoForwardDeprecatedPathFilter.ENABLED_PROPERTY, value = "true")
    public void testRealFilter() throws ServletException, IOException {
        var request = MockHttpServletRequest.builder()
                                            .method("GET")
                                            .servletPath("/api")
                                            .pathInfo("/v1/search/lang/NXQL/execute")
                                            .build()
                                            .mock();

        new NuxeoForwardDeprecatedPathFilter().doFilter(request, mock(HttpServletResponse.class),
                mock(FilterChain.class));

        verify(request).getRequestDispatcher(eq("/api/v1/search/execute"));
        logResult.assertHasEvent();
        assertEquals(
                "The path: /api/v1/search/lang/NXQL/execute is deprecated since 10.3, "
                        + "please update your code to execute the request to: /api/v1/search/execute",
                logResult.getCaughtEventMessages().getFirst());
    }

    @Test
    public void testSearchLangExecute() {
        assertTrue(DeprecatedPath.SEARCH_LANG_EXECUTE.matches("GET", "/api/v1/search/lang/NXQL/execute"));
        assertFalse(DeprecatedPath.SEARCH_LANG_BULK.matches("POST", "/api/v1/search/execute"));
        assertEquals("/api/v1/search/execute",
                DeprecatedPath.SEARCH_LANG_EXECUTE.getReplacement("/api/v1/search/lang/NXQL/execute"));
    }

    @Test
    public void testSearchLangBulk() {
        assertTrue(DeprecatedPath.SEARCH_LANG_BULK.matches("POST", "/api/v1/search/lang/NXQL/bulk/setProperties"));
        assertFalse(DeprecatedPath.SEARCH_LANG_BULK.matches("POST", "/api/v1/search/bulk/setProperties"));
        assertEquals("/api/v1/search/bulk/setProperties",
                DeprecatedPath.SEARCH_LANG_BULK.getReplacement("/api/v1/search/lang/NXQL/bulk/setProperties"));
    }

    @Test
    public void testOAuth2Token() {
        assertTrue(DeprecatedPath.OAUTH2_TOKEN.matches("DELETE", "/api/v1/oauth2/token/myProvider/myUser"));
        assertFalse(
                DeprecatedPath.OAUTH2_TOKEN.matches("DELETE", "/api/v1/oauth2/token/provider/myProvider/user/myUser"));
        assertEquals("/api/v1/oauth2/token/provider/myProvider/user/myUser",
                DeprecatedPath.OAUTH2_TOKEN.getReplacement("/api/v1/oauth2/token/myProvider/myUser"));
    }

    @Test
    public void testElasticsearch() {
        assertTrue(DeprecatedPath.MANAGEMENT_ELASTICSEARCH.matches("POST",
                "/api/v1/management/elasticsearch/DOC_ID/reindex"));
        assertTrue(
                DeprecatedPath.MANAGEMENT_ELASTICSEARCH.matches("GET", "/api/v1/management/elasticsearch/checkSearch"));
        assertTrue(DeprecatedPath.MANAGEMENT_ELASTICSEARCH.matches("POST", "/api/v1/management/elasticsearch/reindex"));
        assertEquals("/api/v1/management/search/reindex",
                DeprecatedPath.MANAGEMENT_ELASTICSEARCH.getReplacement("/api/v1/management/elasticsearch/reindex"));
    }
}
