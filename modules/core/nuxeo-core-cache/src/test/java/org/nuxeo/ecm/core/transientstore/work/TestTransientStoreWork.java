/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.transientstore.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.transientstore.InMemoryTransientStoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, InMemoryTransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.core.event")
public class TestTransientStoreWork {

    @Inject
    protected EventService eventService;

    @Inject
    protected WorkManager workManager;

    @Test
    public void shouldStoreBlobAndParamsInTransientStore() throws IOException {
        TransientStoreWork work = new DummyTransientStoreWork();
        workManager.schedule(work);

        eventService.waitForAsyncCompletion();

        BlobHolder holder = TransientStoreWork.getBlobHolder(work.getEntryKey());
        assertNotNull(holder);

        Map<String, Serializable> entryParams = holder.getProperties();
        assertEquals(2, entryParams.size());
        Serializable value = entryParams.get("firstparam");
        assertNotNull(value);
        assertEquals("firstvalue", value);
        value = entryParams.get("secondparam");
        assertNotNull(value);
        assertEquals("secondvalue", value);

        List<Blob> blobs = holder.getBlobs();
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("a simple blob", blob.getString());
        assertEquals("text/plain", blob.getMimeType());
    }
}
