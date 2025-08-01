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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_ACCESS_KEY_ID;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_REGION;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SECRET_ACCESS_KEY;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SESSION_TOKEN;

import jakarta.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.runtime.aws")
public class TestAWSConfigurationService {

    // from the XML test file
    protected static final String MY_CONFIG = "myconfig";

    @Inject
    protected AWSConfigurationService service;

    protected static void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceCredentials() {
        AwsCredentials credentials = service.getAwsCredentials();
        assertCredentials(credentials, "XML_ACCESS_KEY_ID", "XML_SECRET_KEY", "XML_SESSION_TOKEN");
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceCredentialsWithId() {
        AwsCredentials credentials = service.getAwsCredentials(MY_CONFIG);
        assertCredentials(credentials, "MY_XML_ACCESS_KEY_ID", "MY_XML_SECRET_KEY", "MY_XML_SESSION_TOKEN");
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoCredentialsProvider() {
        AwsCredentials credentials = NuxeoAWSCredentialsProvider.getInstance().resolveCredentials();
        assertCredentials(credentials, "XML_ACCESS_KEY_ID", "XML_SECRET_KEY", "XML_SESSION_TOKEN");
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoCredentialsProviderWithId() {
        AwsCredentials credentials = new NuxeoAWSCredentialsProvider(MY_CONFIG).resolveCredentials();
        assertCredentials(credentials, "MY_XML_ACCESS_KEY_ID", "MY_XML_SECRET_KEY", "MY_XML_SESSION_TOKEN");
    }

    @Test
    public void testServiceCredentialsWithoutNuxeoConfiguration() {
        assertNull(service.getAwsCredentials());
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    public void testNuxeoCredentialsProviderWithoutNuxeoConfiguration() {
        try {
            assumeTrue("Cannot run if AWS env vars are already set",
                    EnvironmentVariableCredentialsProvider.create().resolveCredentials() == null);
        } catch (SdkClientException e) {
            // ok, no env vars set
        }

        // use system prop config from default AWS chain
        String oldAccessKeyId = System.getProperty(AWS_ACCESS_KEY_ID.property());
        String oldSecretKey = System.getProperty(AWS_SECRET_ACCESS_KEY.property());
        String oldSessionToken = System.getProperty(AWS_SESSION_TOKEN.property());
        try {
            setSystemProperty(AWS_ACCESS_KEY_ID.property(), "SYSPROP_ACCESS_KEY_ID");
            setSystemProperty(AWS_SECRET_ACCESS_KEY.property(), "SYSPROP_SECRET_KEY");
            setSystemProperty(AWS_SESSION_TOKEN.property(), "SYSPROP_SESSION_TOKEN");
            AwsCredentials credentials = NuxeoAWSCredentialsProvider.getInstance().resolveCredentials();
            assertCredentials(credentials, "SYSPROP_ACCESS_KEY_ID", "SYSPROP_SECRET_KEY", "SYSPROP_SESSION_TOKEN");
        } finally {
            setSystemProperty(AWS_ACCESS_KEY_ID.property(), oldAccessKeyId);
            setSystemProperty(AWS_SECRET_ACCESS_KEY.property(), oldSecretKey);
            setSystemProperty(AWS_SESSION_TOKEN.property(), oldSessionToken);
        }
    }

    protected static void assertCredentials(AwsCredentials credentials, String accessKeyId, String secretKey,
            String sessionToken) {
        assertNotNull(credentials);
        assertEquals(accessKeyId, credentials.accessKeyId());
        assertEquals(secretKey, credentials.secretAccessKey());
        if (sessionToken != null) {
            assertTrue(credentials instanceof AwsSessionCredentials);
            assertEquals(sessionToken, ((AwsSessionCredentials) credentials).sessionToken());
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceRegion() {
        Region region = service.getAwsRegion();
        assertEquals(Region.of("XML_REGION"), region);
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceRegionWithId() {
        Region region = service.getAwsRegion(MY_CONFIG);
        assertEquals(Region.of("MY_XML_REGION"), region);
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoRegionProvider() {
        Region region = NuxeoAWSRegionProvider.getInstance().getRegion();
        assertEquals(Region.of("XML_REGION"), region);
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoRegionProviderWithId() {
        Region region = new NuxeoAWSRegionProvider(MY_CONFIG).getRegion();
        assertEquals(Region.of("MY_XML_REGION"), region);
    }

    @Test
    public void testServiceRegionWithoutNuxeoConfiguration() {
        assertNull(service.getAwsRegion());
    }

    @Test
    @Ignore //Ignore this test since we are testing on AWS enabled environment
    public void testNuxeoRegionProviderWithoutNuxeoConfiguration() {
        Region systemSettingsRegion = null;
        try {
            systemSettingsRegion = new SystemSettingsRegionProvider().getRegion();
        } catch (SdkClientException e) {
            // means there is no system region defined
        }
        assumeTrue("Cannot run if AWS env vars are already set", systemSettingsRegion == null);

        // use system prop config from default AWS chain
        String oldRegion = System.getProperty(AWS_REGION.property());
        try {
            setSystemProperty(AWS_REGION.property(), "SYSPROP_REGION");
            Region region = NuxeoAWSRegionProvider.getInstance().getRegion();
            assertEquals(Region.of("SYSPROP_REGION"), region);
        } finally {
            setSystemProperty(AWS_REGION.property(), oldRegion);
        }
    }

}
