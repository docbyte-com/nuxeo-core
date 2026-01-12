/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;

import org.nuxeo.ecm.core.blob.TestAbstractBlobStoreWithOptimizedCopy;
import org.nuxeo.runtime.test.runner.Features;

import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.StorageClass;

@Features(S3BlobProviderFeature.class)
public abstract class TestS3BlobStoreAbstract extends TestAbstractBlobStoreWithOptimizedCopy {

    protected void assertStorageClass(String key) {
        S3BlobStoreConfiguration config = ((S3BlobProvider) bp).config;
        var s3Key = new S3BlobKey(config, key);
        HeadObjectResponse response = config.amazonS3.headObject(
                HeadObjectRequest.builder().bucket(config.bucketName).key(s3Key.bucketKey()).build());
        assertEquals(expectedStorageClass(), response.storageClass());
    }

    protected StorageClass expectedStorageClass() {
        return null; // storage class is null for StorageClass.STANDARD;
    }

}
