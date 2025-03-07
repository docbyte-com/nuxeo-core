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
 *     bdelbosc
 */
package org.nuxeo.ecm.platform.web.requestcontroller.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nuxeo.ecm.core.management.jtajca.internal.TracingWebFilter.TRACE_PARENT_HEADER;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.management.jtajca.internal.TracingWebFilter;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.ecm.platform.web.common.MockHttpServletResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import io.opencensus.contrib.http.servlet.OcHttpServletFilter;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
public class TestTracingWebFilter {

    public static final String TRACE_PARENT_VALID = "00-c0000000000000000000000000000000-0000000000000001-01";

    // too short
    public static final String TRACE_PARENT_INVALID = "00-a000000000000000000000000000-00000000000000-01";

    @Inject
    LogCaptureFeature.Result logCaptureResults;

    @Test
    public void testValidTraceParent() throws ServletException, IOException {
        var filter = new OcHttpServletFilter();
        FilterChain chain = mock(FilterChain.class);
        // valid header
        var request = MockHttpServletRequest.init(HttpGet.METHOD_NAME)
                                            .whenGetHeaderThenReturn(TRACE_PARENT_HEADER, TRACE_PARENT_VALID)
                                            .mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());
        filter.destroy();
    }

    @Test
    public void testInvalidTraceParent() throws ServletException, IOException {
        var filter = new OcHttpServletFilter();
        FilterChain chain = mock(FilterChain.class);
        // trace parent header is too short
        var request = MockHttpServletRequest.init(HttpGet.METHOD_NAME)
                                            .whenGetHeaderThenReturn(TRACE_PARENT_HEADER, TRACE_PARENT_INVALID)
                                            .mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        assertThrows(StringIndexOutOfBoundsException.class, () -> filter.doFilter(request, response, chain));
        filter.destroy();
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = TracingWebFilter.class)
    public void testInvalidTraceParentWithTracingWebFilter() throws ServletException, IOException {
        // TracingWebFilter is removing the invalid traceparent header and is executed before OcHttpServletFilter
        var filter = new TracingWebFilter();
        FilterChain chain = mock(FilterChain.class);
        // trace parent header is too short
        var request = MockHttpServletRequest.init(HttpGet.METHOD_NAME)
                                            .whenGetHeaderThenReturn(TRACE_PARENT_HEADER, TRACE_PARENT_INVALID)
                                            .mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());
        filter.destroy();
        List<String> caughtEvents = logCaptureResults.getCaughtEventMessages();
        assertEquals(1, caughtEvents.size());
        assertTrue(caughtEvents.toString(), caughtEvents.get(0).startsWith("Removing invalid traceparent header"));
    }

}
