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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import org.nuxeo.runtime.api.Framework;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * AWS Credentials Provider that uses Nuxeo configuration, or uses the default AWS chain as a fallback.
 *
 * @since 10.3
 */
public class NuxeoAWSCredentialsProvider implements AwsCredentialsProvider {

    protected static final AwsCredentialsProvider INSTANCE = new NuxeoAWSCredentialsProvider();

    protected static final AwsCredentialsProvider DEFAULT = DefaultCredentialsProvider.create();

    protected final String id;

    /**
     * Gets a Nuxeo AWS Credentials Provider for the default configuration.
     */
    public static AwsCredentialsProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Nuxeo AWS Credentials Provider for the default configuration.
     */
    public NuxeoAWSCredentialsProvider() {
        this(null);
    }

    /**
     * Creates a new Nuxeo AWS Credentials Provider for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @since 11.1
     */
    public NuxeoAWSCredentialsProvider(String id) {
        this.id = id;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            AwsCredentials credentials = service.getAwsCredentials(id);
            if (credentials != null) {
                return credentials;
            }
        }
        return DefaultCredentialsProvider.create().resolveCredentials();
    }

}
