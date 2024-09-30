/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.runtime.opensearch1.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.common.test.ModuleUnderTest;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.embed.OpenSearchEmbedFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test the client based on Rest Client protocol
 */
@RunWith(FeaturesRunner.class)
@Features(OpenSearchEmbedFeature.class)
public class TestOpenSearchRestClient {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder(new File(ModuleUnderTest.getOutputDirectory()));

    protected OpenSearchRestClientFactory factory = new OpenSearchRestClientFactory();

    protected OpenSearchClient client;

    @Before
    public void initClient() {
        if (client == null) {
            var config = new OpenSearchClientConfig();
            config.embedServer = OpenSearchEmbedFeature.SERVER_NAME;
            client = factory.create(config);
        }
    }

    @After
    public void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    @SuppressWarnings("resource")
    public void testNoClientConfig() {
        var config = new OpenSearchClientConfig();
        assertThrows(IllegalArgumentException.class, () -> factory.create(config));
        // No config so should throw IllegalArgumentException
    }

    @Test
    public void testValidClientConfigURls() {
        var config = new OpenSearchClientConfig();
        config.setServers(List.of("myhost,localhost:9200,local:80", "http://localhosted",
                "https://mysecure,https://moresecure:445"));
        assertEquals(6, config.getServers().size());
        assertEquals("myhost", config.getServers().get(0));
        assertEquals("localhost:9200", config.getServers().get(1));
        assertEquals("local:80", config.getServers().get(2));
        assertEquals("http://localhosted", config.getServers().get(3));
        assertEquals("https://mysecure", config.getServers().get(4));
        assertEquals("https://moresecure:445", config.getServers().get(5));
    }

    @Test
    public void testCredentialProvider() throws Exception {
        var config = new OpenSearchClientConfig();
        config.servers.add("localhost:9200");
        config.username = "bob";
        config.password = "bob";
        try (var client = factory.create(config)) {
            assertNotNull(client);
            // Its not possible to get a reference to check the configuration, but the absence of an error is itself a
            // test.
        }
    }

    @Test
    public void testDefaultTrustStore() throws Exception {
        var config = new OpenSearchClientConfig();
        String password = "difficultpass";
        File keystoreFile = getKeystoreFile(password, KeyStore.getDefaultType());

        config.servers.add("localhost:9200");

        var trustStore = new OpenSearchClientConfig.Store();
        trustStore.path = keystoreFile.getAbsolutePath();
        trustStore.password = password;
        trustStore.type = null;
        config.trustStore = trustStore;

        var keyStore = new OpenSearchClientConfig.Store();
        keyStore.path = keystoreFile.getAbsolutePath();
        keyStore.password = password;
        keyStore.type = null;
        config.keyStore = keyStore;
        try (var client = factory.create(config)) {
            assertNotNull(client);
            // This would error if it couldn't open the keystore.
        }

        keystoreFile.delete();
    }

    @Test
    public void testSslContext() throws Exception {
        var config = new OpenSearchClientConfig();

        String password = "mypass";
        String keystoreType = "pkcs12";
        File keystoreFile = getKeystoreFile(password, keystoreType);

        config.servers.add("localhost:9200");
        var trustStore = new OpenSearchClientConfig.Store();
        trustStore.path = keystoreFile.getAbsolutePath();
        trustStore.password = password;
        trustStore.type = keystoreType;
        config.trustStore = trustStore;

        var keyStore = new OpenSearchClientConfig.Store();
        keyStore.path = keystoreFile.getAbsolutePath();
        keyStore.password = password;
        keyStore.type = keystoreType;
        config.keyStore = keyStore;
        try (var client = factory.create(config)) {
            assertNotNull(client);
            // Its not possible to get a reference to check the configuration, but the absence of an error is itself a
            // test.
        }

        keystoreFile.delete();
    }

    /**
     * Sets up a temporary keystore
     */
    public File getKeystoreFile(String password, String keystoreType) throws Exception {
        File keystoreFile = Framework.createTempFile("keystore_", ".tmp");
        KeyStore ks = KeyStore.getInstance(keystoreType);
        ks.load(null, password.toCharArray());
        try (OutputStream os = new FileOutputStream(keystoreFile)) {
            ks.store(os, password.toCharArray());
        }
        return keystoreFile;
    }

}
