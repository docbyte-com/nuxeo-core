/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.opensearch1.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.SSLContext;

import cloud.docbyte.aws.auth.AwsRequestSigningApacheInterceptor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * @since 9.3
 */
public class OpenSearchRestClientFactory implements OpenSearchClientFactory {

    private static final Logger log = LogManager.getLogger(OpenSearchRestClientFactory.class);

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    public static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(20);

    public static final String DEFAULT_AUTHENTICATION_TYPE = "basic";

    public static final String DEFAULT_REGION = "eu-west-1";
    @Override
    public OpenSearchClient create(OpenSearchClientConfig config) {
        return createRestClient(config);
    }

    @SuppressWarnings("resource") // factory for OpenSearchClient / RestHighLevelClient
    protected OpenSearchClient createRestClient(OpenSearchClientConfig config) {
        var servers = config.getEmbedServer()
                            .map(OpenSearchEmbedNodeUrlResolver::getUrl)
                            .map(List::of)
                            .orElseGet(config::getServers);
        if (servers.isEmpty()) {
            throw new IllegalArgumentException(
                    "No server defined for client: %s, can not create OpenSearchClient".formatted(config.getId()));
        }
        log.debug("Creating client: {} targeting servers: {}", config.getId(), servers);
        var httpHosts = servers.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        long connectionTimeout = config.getConnectionTimeout().orElse(DEFAULT_CONNECT_TIMEOUT).toMillis();
        long socketTimeout = config.getSocketTimeout().orElse(DEFAULT_SOCKET_TIMEOUT).toMillis();
        var builder = RestClient.builder(httpHosts)
                                .setRequestConfigCallback(
                                        callback -> callback.setConnectTimeout((int) connectionTimeout)
                                                            .setSocketTimeout((int) socketTimeout));
        addClientCallback(config, builder);
        var client = new RestHighLevelClient(builder); // NOSONAR (factory)
        checkConnection(config.getId(), client);
        return new OpenSearchRestClient(config.getId(), client);
    }

    protected void addClientCallback(OpenSearchClientConfig config, RestClientBuilder builder) {
        String authenticationType = config.getAuthenticationType().orElse(DEFAULT_AUTHENTICATION_TYPE);
        if ("iam".equalsIgnoreCase(authenticationType)) {
            addIamAuthClientCallback(config, builder);
        } else {
            addBasicClientCallback(config, builder);
        }
    }

    private void addBasicClientCallback(OpenSearchClientConfig config, RestClientBuilder builder) {
        BasicCredentialsProvider credentialProvider = getCredentialProvider(config);
        SSLContext sslContext = getSslContext(config);
        if (sslContext == null && credentialProvider == null) {
            return;
        }
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder.setDefaultCredentialsProvider(credentialProvider);
            if (!config.isSslCertificateVerification()) {
                httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            return httpClientBuilder;
        });
    }

    protected void addIamAuthClientCallback(OpenSearchClientConfig config, RestClientBuilder builder) {
        builder.setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(
                new AwsRequestSigningApacheInterceptor("es", Aws4Signer.create(),
                        DefaultCredentialsProvider.builder().build(), Region.of(config.getRegion().orElse(DEFAULT_REGION))))).build();
    }

    protected BasicCredentialsProvider getCredentialProvider(OpenSearchClientConfig config) {
        if (config.getUsername().isEmpty() || config.getPassword().isEmpty()) {
            return null;
        }
        String user = config.getUsername().get();
        String password = config.getPassword().get();
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        return credentialsProvider;
    }

    protected SSLContext getSslContext(OpenSearchClientConfig config) {
        try {
            if (config.getTrustStore().isEmpty() && config.getKeyStore().isEmpty()) {
                return null;
            }
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (config.getTrustStore().isPresent()) {
                var store = loadKeyStore(config.getTrustStore().get());
                sslContextBuilder.loadTrustMaterial(store, null);
            }
            if (config.getKeyStore().isPresent()) {
                var storeConfig = config.getKeyStore().get();
                var store = loadKeyStore(storeConfig);
                char[] passwordChars = storeConfig.getPassword().map(String::toCharArray).orElse(null);
                sslContextBuilder.loadKeyMaterial(store, passwordChars);
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeServiceException("Cannot setup SSL for OpenSearchClientConfig: " + config, e);
        }
    }

    protected KeyStore loadKeyStore(OpenSearchClientConfig.Store storeConfig)
            throws GeneralSecurityException, IOException {
        String keyStoreType = storeConfig.getType().orElseGet(KeyStore::getDefaultType);
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] passwordChars = storeConfig.getPassword().map(String::toCharArray).orElse(null);
        try (InputStream is = Files.newInputStream(Paths.get(storeConfig.getPath()))) {
            keyStore.load(is, passwordChars);
        }
        return keyStore;
    }

    protected void checkConnection(String clientId, RestHighLevelClient client) {
        try {
            if (!client.ping(RequestOptions.DEFAULT)) {
                throw new IOException("Ping response is false");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Fail to ping rest node targeted by clientId: " + clientId);
        }
    }
}
