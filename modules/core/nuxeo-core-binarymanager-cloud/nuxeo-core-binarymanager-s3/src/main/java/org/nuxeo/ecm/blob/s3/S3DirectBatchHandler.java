/*
 * (C) Copyright 2011-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Luís Duarte
 *     Florent Guillaume
 *     Mickaël Schoentgen
 */
package org.nuxeo.ecm.blob.s3;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.ACCELERATE_MODE_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_ID_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SECRET_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SESSION_TOKEN_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_REGION_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.ENDPOINT_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.PATHSTYLEACCESS_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.SERVERSIDE_ENCRYPTION_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.io.upload.batch.Batch;
import org.nuxeo.ecm.core.io.upload.batch.BatchFileInfo;
import org.nuxeo.ecm.core.io.upload.batch.impl.AbstractBatchHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;

/**
 * Batch Handler allowing direct S3 upload.
 *
 * @since 10.1
 */
public class S3DirectBatchHandler extends AbstractBatchHandler {

    private static final Logger log = LogManager.getLogger(S3DirectBatchHandler.class);

    // properties passed at initialization time from extension point

    public static final String POLICY_TEMPLATE_PROPERTY = "policyTemplate";

    /**
     * @since 10.10
     */
    public static final String ROLE_ARN_PROPERTY = "roleArn";

    /**
     * @since 11.1
     */
    public static final String BLOB_PROVIDER_ID_PROPERTY = "blobProvider";

    // keys in the batch properties, returned to the client

    public static final String INFO_AWS_SECRET_KEY_ID = "awsSecretKeyId";

    public static final String INFO_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey";

    public static final String INFO_AWS_SESSION_TOKEN = "awsSessionToken";

    public static final String INFO_BUCKET = "bucket";

    public static final String INFO_BASE_KEY = "baseKey";

    public static final String INFO_EXPIRATION = "expiration";

    /** @since 11.1 */
    public static final String INFO_AWS_ENDPOINT = "endpoint";

    /** @since 11.1 */
    public static final String INFO_AWS_PATH_STYLE_ACCESS = "usePathStyleAccess";

    public static final String INFO_AWS_REGION = "region";

    public static final String INFO_USE_S3_ACCELERATE = "useS3Accelerate";

    protected StsClient stsClient;

    protected S3Client amazonS3;

    protected URI endpointOverride;

    protected boolean pathStyleAccessEnabled;

    protected Region region;

    protected String bucket;

    protected String bucketPrefix;

    protected boolean accelerateModeEnabled;

    protected int expiration;

    protected String policy;

    protected String roleArn;

    protected boolean useServerSideEncryption;

    protected String serverSideKMSKeyID;

    // public for tests
    public String blobProviderId;

    /**
     * @since 11.5
     */
    public static String getMimeType(String contentType) {
        Objects.requireNonNull(contentType);
        HeaderElement headerElement = BasicHeaderValueParser.parseHeaderElement(contentType, null);
        return headerElement.getName();
    }

    /**
     * @since 11.5
     */
    public static String getCharset(String contentType) {
        Objects.requireNonNull(contentType);
        HeaderElement headerElement = BasicHeaderValueParser.parseHeaderElement(contentType, null);
        String encoding = null;
        for (NameValuePair param : headerElement.getParameters()) {
            if (param.getName().equalsIgnoreCase("charset")) {
                String s = param.getValue();
                if (!isBlank(s)) {
                    encoding = s;
                }
                break;
            }
        }
        return encoding;
    }

    @Override
    protected void initialize(Map<String, String> properties) {
        super.initialize(properties);
        String endpoint = properties.get(ENDPOINT_PROPERTY);
        if (isNotBlank(endpoint)) {
            try {
                endpointOverride = new URI(endpoint);
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        }
        pathStyleAccessEnabled = Boolean.parseBoolean(properties.get(PATHSTYLEACCESS_PROPERTY));
        String bucketRegionProperty = properties.get(BUCKET_REGION_PROPERTY);
        if (isNotBlank(bucketRegionProperty)) {
            region = Region.of(bucketRegionProperty);
        } else {
            region = NuxeoAWSRegionProvider.getInstance().getRegion();
        }
        bucket = properties.get(BUCKET_NAME_PROPERTY);
        if (isBlank(bucket)) {
            throw new NuxeoException("Missing configuration property: " + BUCKET_NAME_PROPERTY);
        }
        roleArn = properties.get(ROLE_ARN_PROPERTY);
        if (isBlank(roleArn)) {
            throw new NuxeoException("Missing configuration property: " + ROLE_ARN_PROPERTY);
        }
        bucketPrefix = defaultString(properties.get(BUCKET_PREFIX_PROPERTY));
        accelerateModeEnabled = Boolean.parseBoolean(properties.get(ACCELERATE_MODE_PROPERTY));
        String awsSecretKeyId = properties.get(AWS_ID_PROPERTY);
        String awsSecretAccessKey = properties.get(AWS_SECRET_PROPERTY);
        String awsSessionToken = properties.get(AWS_SESSION_TOKEN_PROPERTY);
        expiration = Integer.parseInt(defaultIfEmpty(properties.get(INFO_EXPIRATION), "0"));
        policy = properties.get(POLICY_TEMPLATE_PROPERTY);

        useServerSideEncryption = Boolean.parseBoolean(properties.get(SERVERSIDE_ENCRYPTION_PROPERTY));
        serverSideKMSKeyID = properties.get(SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY);

        AwsCredentialsProvider credentials = S3Utils.getAwsCredentialsProvider(awsSecretKeyId, awsSecretAccessKey,
                awsSessionToken);
        stsClient = initializeSTSClient(credentials);
        amazonS3 = initializeS3Client(credentials);

        if (!isBlank(bucketPrefix) && !bucketPrefix.endsWith("/")) {
            log.debug("{} {} S3 bucket prefix should end with '/': added automatically.", BUCKET_PREFIX_PROPERTY,
                    bucketPrefix);
            bucketPrefix += "/";
        }

        blobProviderId = Objects.toString(properties.get(BLOB_PROVIDER_ID_PROPERTY), transientStoreName);
    }

    protected StsClient initializeSTSClient(AwsCredentialsProvider credentials) {
        StsClientBuilder stsClientBuilder = StsClient.builder().region(region).credentialsProvider(credentials);
        if (endpointOverride != null) {
            stsClientBuilder.endpointOverride(endpointOverride);
        }
        return stsClientBuilder.build();
    }

    protected S3Client initializeS3Client(AwsCredentialsProvider credentials) {
        ApacheHttpClient.Builder apacheHttpClientBuilder = ApacheHttpClient.builder();
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                                  .region(region)
                                                  .credentialsProvider(credentials)
                                                  .httpClientBuilder(apacheHttpClientBuilder);
        if (endpointOverride != null) {
            s3ClientBuilder.endpointOverride(endpointOverride);
        }

        if (pathStyleAccessEnabled) {
            s3ClientBuilder.forcePathStyle(true);
        }
        if (accelerateModeEnabled) {
            s3ClientBuilder.accelerate(true);
        }
        return s3ClientBuilder.build();
    }

    @Override
    public Batch getBatch(String batchId) {
        Map<String, Serializable> parameters = getBatchParameters(batchId);
        if (parameters == null) {
            return null;
        }

        // create the batch
        Batch batch = new Batch(batchId, parameters, getName(), getTransientStore());

        Credentials credentials = getAwsCredentials(batchId);

        Map<String, Object> properties = batch.getProperties();
        properties.put(INFO_AWS_SECRET_KEY_ID, credentials.accessKeyId());
        properties.put(INFO_AWS_SECRET_ACCESS_KEY, credentials.secretAccessKey());
        properties.put(INFO_AWS_SESSION_TOKEN, credentials.sessionToken());
        properties.put(INFO_BUCKET, bucket);
        properties.put(INFO_BASE_KEY, bucketPrefix);
        properties.put(INFO_EXPIRATION, credentials.expiration().toEpochMilli());
        properties.put(INFO_AWS_ENDPOINT, endpointOverride != null ? endpointOverride.toString() : null);
        properties.put(INFO_AWS_PATH_STYLE_ACCESS, pathStyleAccessEnabled);
        properties.put(INFO_AWS_REGION, region);
        properties.put(INFO_USE_S3_ACCELERATE, accelerateModeEnabled);

        return batch;
    }

    protected Credentials assumeRole(AssumeRoleRequest request) {
        return stsClient.assumeRole(request).credentials();
    }

    @Override
    public boolean completeUpload(String batchId, String fileIndex, BatchFileInfo fileInfo) {
        String fileKey = fileInfo.getKey();
        String key = StringUtils.removeStart(fileKey, bucketPrefix);
        HeadObjectResponse response = amazonS3.headObject(
                HeadObjectRequest.builder().bucket(bucket).key(fileKey).build());

        // Deduplicating blob providers have as invariant that if a key looks like a digest then it is one.
        // (see AbstractBlobStore.writeBlobUsingOptimizedCopy in particular)
        // The old S3BinaryManager assumes the same thing.

        if (isValidDigest(key)) {
            // the key looks like a digest, move it to a non-digest key
            key = response.eTag().replace("\"", "");
            if (isValidDigest(key)) {
                key += "-0"; // cannot be confused with a digest
            }
            move(fileKey, bucketPrefix + key);
        }

        // materialize the direct upload blob as a Nuxeo Blob

        BlobInfo blobInfo = new BlobInfo();
        String contentType = response.contentType();
        blobInfo.mimeType = getMimeType(contentType);
        blobInfo.encoding = getCharset(contentType);
        blobInfo.filename = fileInfo.getFilename();
        blobInfo.length = response.contentLength();
        blobInfo.key = key;
        // (no digest needed)

        Blob blob;
        try {
            blob = getBlobProvider().readBlob(blobInfo);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        // put the blob in the batch (will copy to new bucket)

        Batch batch = getBatch(batchId);
        try {
            batch.addFile(fileIndex, blob, blob.getFilename(), blob.getMimeType());
        } catch (NuxeoException e) {
            try {
                amazonS3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(bucketPrefix + key).build());
            } catch (AwsServiceException s3E) {
                e.addSuppressed(s3E);
            }
            throw e;
        }

        return true;
    }

    protected void move(String sourceKey, String destinationKey) {
        CopyObjectRequest.Builder copyObjectRequestBuilder = CopyObjectRequest.builder()
                                                                              .sourceBucket(bucket)
                                                                              .sourceKey(sourceKey)
                                                                              .destinationBucket(bucket)
                                                                              .destinationKey(destinationKey);
        // server-side encryption
        if (useServerSideEncryption) {
            // server-side encryption
            if (isNotBlank(serverSideKMSKeyID)) {
                // SSE-KMS
                copyObjectRequestBuilder.ssekmsKeyId(serverSideKMSKeyID);
            } else {
                // SSE-S3
                copyObjectRequestBuilder.sseCustomerAlgorithm(ServerSideEncryption.AES256.toString());
            }
        }
        CopyRequest copyRequest = CopyRequest.builder().copyObjectRequest(copyObjectRequestBuilder.build()).build();
        Copy copy = getTransferManager().copy(copyRequest);
        try {
            copy.completionFuture().join();
        } catch (CompletionException e) {
            throw new NuxeoException(e);
        } finally {
            try {
                amazonS3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(sourceKey).build());
            } catch (AwsServiceException e) {
                log.debug("Unable to cleanup object, move has already been done", e);
            }
        }
    }

    protected boolean isValidDigest(String key) {
        BlobProvider blobProvider = getBlobProvider();
        if (blobProvider instanceof BlobStoreBlobProvider bsbb) {
            KeyStrategy keyStrategy = bsbb.store.getKeyStrategy();
            if (keyStrategy instanceof KeyStrategyDigest ksd) {
                return ksd.isValidDigest(key);
            }
        }
        return false;
    }

    protected BlobProvider getBlobProvider() {
        return Framework.getService(BlobManager.class).getBlobProvider(blobProviderId);
    }

    protected S3TransferManager getTransferManager() {
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blobProviderId);
        if (!(blobProvider instanceof S3ManagedTransfer s3ManagedTransfer)) {
            throw new NuxeoException("BlobProvider does not implement S3ManagedTransfer");
        }
        return s3ManagedTransfer.getTransferManager();
    }

    protected Credentials getAwsCredentials(String batchId) {
        AssumeRoleRequest.Builder requestBuilder = AssumeRoleRequest.builder()
                                                                    .roleArn(roleArn)
                                                                    .policy(policy)
                                                                    .roleSessionName(batchId);
        if (expiration > 0) {
            requestBuilder.durationSeconds(expiration);
        }
        return assumeRole(requestBuilder.build());
    }

    /** @since 11.1 */
    @Override
    public Map<String, Object> refreshToken(String batchId) {
        Objects.requireNonNull(batchId, "required batch ID");

        Credentials credentials = getAwsCredentials(batchId);
        Map<String, Object> result = new HashMap<>();
        result.put(INFO_AWS_SECRET_KEY_ID, credentials.accessKeyId());
        result.put(INFO_AWS_SECRET_ACCESS_KEY, credentials.secretAccessKey());
        result.put(INFO_AWS_SESSION_TOKEN, credentials.sessionToken());
        result.put(INFO_EXPIRATION, credentials.expiration().toEpochMilli());
        return result;
    }

}
