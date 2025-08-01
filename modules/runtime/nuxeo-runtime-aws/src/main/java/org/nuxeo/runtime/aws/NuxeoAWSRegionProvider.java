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

import static software.amazon.awssdk.regions.Region.US_EAST_1;

import org.nuxeo.runtime.api.Framework;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * AWS Region Provider that uses Nuxeo configuration, or uses the default AWS chain as a fallback.
 *
 * @since 10.3
 */
public class NuxeoAWSRegionProvider implements AwsRegionProvider {

    protected static final AwsRegionProvider INSTANCE = new NuxeoAWSRegionProvider();

    protected static final AwsRegionProvider DEFAULT = new DefaultAwsRegionProviderChain();

    protected static final Region DEFAULT_REGION = US_EAST_1;

    protected final String id;

    /**
     * Gets a Nuxeo AWS Region Provider for the default configuration.
     */
    public static AwsRegionProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Nuxeo AWS Region Provider for the default configuration.
     */
    public NuxeoAWSRegionProvider() {
        this(null);
    }

    /**
     * Creates a new Nuxeo AWS Region Provider for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @since 11.1
     */
    public NuxeoAWSRegionProvider(String id) {
        this.id = id;
    }

    @Override
    public Region getRegion() {
        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            Region region = service.getAwsRegion(id);
            if (region != null) {
                return region;
            }
        }
        Region region = DEFAULT.getRegion();
        if (region != null) {
            return region;
        }
        return DEFAULT_REGION;
    }

}
