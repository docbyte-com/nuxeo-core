/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob.s3;

import static java.lang.Boolean.FALSE;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DELIMITER;
import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Scroll objects of the s3 blob store of a #{@link S3BlobProvider}, the scroll query is the provider id.
 *
 * @since 2023
 */
public class S3BlobScroll extends AbstractBlobScroll<S3BlobProvider> {

    protected S3Client amazonS3;

    protected S3BlobStoreConfiguration config;

    protected Iterator<ListObjectsV2Response> it;

    protected ListObjectsV2Request request;

    protected ListObjectVersionsRequest.Builder versionsRequestBuilder;

    protected ListObjectVersionsResponse versionsList;

    protected S3BlobStore store;

    protected boolean useVersion;

    @Override
    public void init(S3BlobProvider s3BlobProvider) {
        this.it = null;
        this.store = (S3BlobStore) s3BlobProvider.store.unwrap();
        this.config = this.store.config;
        this.useVersion = this.store.hasVersioning();
        this.amazonS3 = this.store.amazonS3;
        if (useVersion) {
            versionsRequestBuilder = ListObjectVersionsRequest.builder()
                                                              .bucket(this.store.bucketName)
                                                              .prefix(this.store.bucketPrefix)
                                                              .maxKeys(size);
            if (config.getSubDirsDepth() == 0) {
                // use delimiter to avoid useless listing of objects in "subdirectories"
                versionsRequestBuilder.delimiter(DELIMITER);
            }
        } else {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                                                                       .bucket(this.store.bucketName)
                                                                       .prefix(this.store.bucketPrefix)
                                                                       .maxKeys(size);
            if (config.getSubDirsDepth() == 0) {
                // use delimiter to avoid useless listing of objects in "subdirectories"
                builder.delimiter(DELIMITER);
            }
            request = builder.build();
        }
    }

    @Override
    public boolean hasNext() {
        if (useVersion) {
            return hasNextVersions();
        } else {
            return hasNextObject();
        }
    }

    protected boolean hasNextObject() {
        return it == null || it.hasNext();
    }

    protected boolean hasNextVersions() {
        return versionsList == null || versionsList.isTruncated();
    }

    @Override
    public List<String> next() {
        if (useVersion) {
            return nextVersions();
        } else {
            return nextObjects();
        }
    }

    protected List<String> nextObjects() {
        if (it == null) {
            it = amazonS3.listObjectsV2Paginator(request).iterator();
        } else if (!it.hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> result = new ArrayList<>();
        for (S3Object s3Object : it.next().contents()) {
            String path = s3Object.key().substring(store.bucketPrefix.length());
            // if sub dir depth is greater than 0, it means we have a path strategy in place
            String key = config.getSubDirsDepth() == 0 ? path : store.pathStrategy.getKeyForPath(path);
            if (key == null) {
                continue;
            }
            addTo(result, key, s3Object::size);
        }
        return result;
    }

    protected List<String> nextVersions() {
        if (versionsList == null) {
            versionsList = amazonS3.listObjectVersions(versionsRequestBuilder.build());
        } else {
            if (FALSE.equals(versionsList.isTruncated())) {
                throw new NoSuchElementException();
            }
            var keyMarker = versionsList.nextKeyMarker();
            var versionIdMarker = versionsList.nextVersionIdMarker();
            versionsList = amazonS3.listObjectVersions(
                    versionsRequestBuilder.keyMarker(keyMarker).versionIdMarker(versionIdMarker).build());
        }
        List<String> result = new ArrayList<>();
        for (ObjectVersion version : versionsList.versions()) {
            String path = version.key().substring(store.bucketPrefix.length());
            // if sub dir depth is greater than 0, it means we have a path strategy in place
            String key = config.getSubDirsDepth() == 0 ? path : store.pathStrategy.getKeyForPath(path);
            if (key == null) {
                continue;
            }
            key += (VER_SEP + version.versionId());
            addTo(result, key, version::size);
        }
        return result;
    }

}
