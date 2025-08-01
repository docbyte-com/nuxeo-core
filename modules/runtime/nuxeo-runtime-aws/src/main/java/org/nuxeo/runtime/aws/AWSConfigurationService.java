/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Remi Cattiau
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * The service providing AWS configuration.
 *
 * @since 10.3
 */
public interface AWSConfigurationService {

    /**
     * Gets the AWS Credentials for the default configuration.
     *
     * @return the AWS credentials, or {@code null} if not defined
     */
    default AwsCredentials getAwsCredentials() {
        return getAwsCredentials(null);
    }

    /**
     * Gets the AWS Credentials for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @return the AWS credentials, or {@code null} if not defined
     * @since 11.1
     */
    AwsCredentials getAwsCredentials(String id);

    /**
     * Enriches the given client builder with an SSL socket factory from the default configuration.
     *
     * @param builder the http client builder
     * @since 2025.0
     */
    default void configureSSL(ApacheHttpClient.Builder builder) {
        configureSSL(null, builder);
    }

    /**
     * Enriches the given client builder with an SSL socket factory from the given configuration.
     *
     * @param id the custom configuration id
     * @param builder the http client builder
     * @since 2025.0
     */
    void configureSSL(String id, ApacheHttpClient.Builder builder);

    /**
     * Enriches the given {@link ApacheHttpClient.Builder} with default proxy configuration.
     *
     * @param builder the http client builder
     * @since 2025.0
     */
    default void configureProxy(ApacheHttpClient.Builder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the Aws Region for the default configuration.
     *
     * @return the Aws Region, or {@code null} if not defined
     * @since 2023.1
     */
    default Region getAwsRegion() {
        return getAwsRegion(null);
    }

    /**
     * Gets the AWS Region for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @return the AWS Region, or {@code null} if not defined
     * @since 2023.1
     */
    Region getAwsRegion(String id);

}
