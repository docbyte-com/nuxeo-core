/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.blob.s3.S3BlobProviderFeature.PREFIX_TEST;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_ID_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SECRET_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SESSION_TOKEN_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_REGION_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.SYSTEM_PROPERTY_PREFIX;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_ACCESS_KEY_ID;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_REGION;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SECRET_ACCESS_KEY;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SESSION_TOKEN;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.blob.AbstractCloudBlobProviderFeature;

/**
 * Helper to connect to S3 in tests.
 *
 * @since 11.5
 */
public class S3TestHelper {

    private S3TestHelper() {
        // utility class
    }

    public static Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();

        String envId = defaultIfBlank(System.getenv(AWS_ACCESS_KEY_ID.environmentVariable()),
                System.getenv("AWS_ACCESS_KEY"));
        String envSecret = defaultIfBlank(System.getenv(AWS_SECRET_ACCESS_KEY.environmentVariable()),
                System.getenv("AWS_SECRET_KEY"));
        // fall back on empty string to allow AWS credentials provider to generate credentials without session token
        String envSessionToken = defaultIfBlank(System.getenv(AWS_SESSION_TOKEN.environmentVariable()), "");
        String envRegion = System.getenv(AWS_REGION.environmentVariable());
        String bucketName = System.getProperty(PREFIX_TEST + BUCKET_NAME_PROPERTY);
        assumeTrue("AWS credentials, region and bucket not set in the environment variables",
                isNoneBlank(envId, envSecret, envRegion, bucketName));

        String bucketPrefixProperty = defaultIfBlank(System.getProperty(PREFIX_TEST + BUCKET_PREFIX_PROPERTY),
                "testFolder");
        String bucketPrefix = getUniqueBucketPrefix(bucketPrefixProperty);

        properties.put(AWS_ID_PROPERTY, envId);
        properties.put(AWS_SECRET_PROPERTY, envSecret);
        properties.put(AWS_SESSION_TOKEN_PROPERTY, envSessionToken);
        properties.put(BUCKET_REGION_PROPERTY, envRegion);
        properties.put(BUCKET_NAME_PROPERTY, bucketName);
        properties.put(BUCKET_PREFIX_PROPERTY, bucketPrefix);
        return properties;
    }

    public static void setProperty(String key, String value) {
        System.getProperties().put(SYSTEM_PROPERTY_PREFIX + '.' + key, value);
    }

    public static void removeProperty(String key) {
        System.getProperties().remove(SYSTEM_PROPERTY_PREFIX + '.' + key);
    }

    public static String getUniqueBucketPrefix(String prefix) {
        return AbstractCloudBlobProviderFeature.getUniqueBucketPrefix(prefix);
    }

}
