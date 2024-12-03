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
package org.nuxeo.ecm.platform.web.common.vh;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.common.http.HttpHeaders.X_FORWARDED_PORT;
import static org.nuxeo.common.http.HttpHeaders.X_FORWARDED_PROTO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestVirtualHostHelper {

    @Test
    public void testXForwardedProtoOnly() {
        var request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo")
                                            .whenGetHeaderThenReturn(X_FORWARDED_PROTO, "https");
        // serverUrl should not contain the 80 port
        var serverUrl = VirtualHostHelper.getServerURL(request.mock());
        assertEquals("https://localhost/", serverUrl);
    }

    @Test
    public void testXForwardedProtoIsNotTakenIntoAccountIfXForwardedPortIsPresent() {
        var request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo")
                                            .whenGetHeaderThenReturn(X_FORWARDED_PORT, "8443")
                                            .whenGetHeaderThenReturn(X_FORWARDED_PROTO, "https");
        // serverUrl should contain the 8443 port
        var serverUrl = VirtualHostHelper.getServerURL(request.mock());
        assertEquals("https://localhost:8443/", serverUrl);
    }
}
