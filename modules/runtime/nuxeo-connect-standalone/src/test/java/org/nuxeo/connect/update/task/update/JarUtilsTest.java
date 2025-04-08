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
package org.nuxeo.connect.update.task.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @since 2025.1
 */
public class JarUtilsTest {

    @Test
    public void testFindJarVersion() {
        var match = JarUtils.findJarVersion("netty-codec-4.1.118.Final.jar");
        assertNotNull(match);
        assertEquals("netty-codec", match.object);
        assertEquals("4.1.118.Final", match.version);
    }

    @Test
    public void testFindJarVersionForSNAPSHOT() {
        var match = JarUtils.findJarVersion("nuxeo-platform-audit-core-2025.0-SNAPSHOT.jar");
        assertNotNull(match);
        assertEquals("nuxeo-platform-audit-core", match.object);
        assertEquals("2025.0-SNAPSHOT", match.version);
    }

    @Test
    public void testFindJarVersionForMilestone() {
        var match = JarUtils.findJarVersion("jersey-client-4.0.0-M1.jar");
        assertNotNull(match);
        assertEquals("jersey-client", match.object);
        assertEquals("4.0.0-M1", match.version);
    }

    // NXP-33139
    @Test
    public void testFindJarVersionWithDash() {
        var match = JarUtils.findJarVersion("zstd-jni-1.5.5-11.jar");
        assertNotNull(match);
        assertEquals("zstd-jni", match.object);
        assertEquals("1.5.5-11", match.version);
    }
}
