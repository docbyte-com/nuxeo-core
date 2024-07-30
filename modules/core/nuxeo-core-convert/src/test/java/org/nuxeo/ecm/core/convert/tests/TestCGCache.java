/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.convert.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.ConvertFeature;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.convert.tests:OSGI-INF/convert-service-frequent-gc-test-contrib.xml")
@Deploy("org.nuxeo.ecm.core.convert.tests:OSGI-INF/converters-test-contrib3.xml")
public class TestCGCache {

    @Inject
    protected ConversionService cs;

    @Test
    public void testCGTask() throws Exception {

        Converter cv = deployConverter();
        assertNotNull(cv);

        BlobHolder bh = getBlobHolder();
        BlobHolder result = cs.convert("identity", bh, null);
        assertNotNull(result);

        // check new cache entry was created
        assertEquals(1, ConversionCacheHolder.getNbCacheEntries());

        // wait for the GCThread to run - 1s configured
        await().atMost(Duration.TWO_SECONDS)
               .pollInterval(Duration.ONE_SECOND)
               .until(() -> ConversionCacheHolder.getNbCacheEntries() == 0);
    }

    private Converter deployConverter() {
        return ConversionServiceImpl.getConverter("identity");
    }

    private static BlobHolder getBlobHolder() throws IOException {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        assertNotNull(file);
        assertTrue(file.length() > 0);
        Blob blob = Blobs.createBlob(file, "application/msword", null, "hello.doc");
        return new SimpleBlobHolder(blob);
    }

}
