/*
 * (C) Copyright 2023-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.gcp;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.TestAbstractBlobStoreWithOptimizedCopy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RandomBug;

/**
 * @since 2023.5
 */
@Features(GoogleStorageBlobProviderFeature.class)
public class TestGoogleStorageBlobStore extends TestAbstractBlobStoreWithOptimizedCopy {

    @Test
    @Override
    @RandomBug.Repeat(issue = "NXP-32368")
    public void testGC() throws IOException {
        super.testGC();
    }

    @Test
    @Override
    @RandomBug.Repeat(issue = "NXP-32368")
    public void testGCWithConcurrentCreation() throws IOException {
        super.testGCWithConcurrentCreation();
    }
}
