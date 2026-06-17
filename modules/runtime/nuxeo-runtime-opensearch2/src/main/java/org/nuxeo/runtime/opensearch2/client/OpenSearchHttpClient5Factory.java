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
 */
package org.nuxeo.runtime.opensearch2.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import cloud.docbyte.aws.auth.AwsRequestSigningApacheHttpClient5Interceptor;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner;
import software.amazon.awssdk.regions.Region;

/**
 * Factory for OpenSearch 2.x clients backed by Apache HttpClient 5, with support for
 * both basic auth and AWS IAM (SigV4) authentication.
 */
public class OpenSearchHttpClient5Factory implements OpenSearchClientFactory {

    private static final Logger log = LogManager.getLogger(OpenSearchHttpClient5Factory.class);

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    public static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(20);

    public static final String DEFAULT_AUTHENTICATION_TYPE = "basic";

    public static final String DEFAULT_REGION = "eu-west-1";

    @Override
    public OpenSearchClient create(OpenSearchClientConfig config) {
        return createHttpClient5(config);
    }

    protected OpenSearchClient createHttpClient5(OpenSearchClientConfig config) {
        var servers = config.getEmbedServer()
                            .map(OpenSearchEmbedNodeUrlResolver::getUrl)
                            .map(List::of)
                            .orElseGet(config::getServers);
        if (servers.isEmpty()) {
            throw new IllegalArgumentException(
                    "No server defined for client: %s, can not create OpenSearchClient".formatted(config.getId()));
        }
        log.debug("Creating client: {} targeting servers: {}", config.getId(), servers);
        var httpHosts = servers.stream().map(this::createHttpHost).toArray(HttpHost[]::new);
        var builder = ApacheHttpClient5TransportBuilder.builder(httpHosts);
        addClientCallback(config, builder);
        var transport = builder.setMapper(new JacksonJsonpMapper()).build();
        var rawClient = new org.opensearch.client.opensearch.OpenSearchClient(transport); // NOSONAR (factory)
        checkConnection(config.getId(), rawClient);
        return new OpenSearchHttpClient5(config.getId(), rawClient);
    }

    protected HttpHost createHttpHost(String server) {
        try {
            return HttpHost.create(server);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid server URI: " + server, e);
        }
    }

    private void addClientCallback(OpenSearchClientConfig config, ApacheHttpClient5TransportBuilder builder) {
        if ("iam".equalsIgnoreCase(config.getAuthenticationType().orElse(DEFAULT_AUTHENTICATION_TYPE))) {
            addIamAuthClientCallback(config, builder);
        } else {
            addBasicAuthClientCallback(config, builder);
        }
    }

    // UNSIGNED-PAYLOAD skips SigV4 body hashing. This is safe here because:
    // 1. The connection is over HTTPS (TLS guarantees body integrity in transit).
    // 2. There are no intermediaries between this client and AWS that terminate TLS
    //    (direct app-to-AWS connection). If a terminating proxy were introduced, body
    //    signing should be reconsidered.
    // The unsigned payload approach is necessary because HC5's async pipeline does not
    // buffer the request body at interceptor time, making it impossible to compute the hash.
    @SuppressWarnings("deprecation") // Aws4UnsignedPayloadSigner is deprecated; migration to the new AWS HTTP auth SPI requires rewriting the interceptor
    protected void addIamAuthClientCallback(OpenSearchClientConfig config,
            ApacheHttpClient5TransportBuilder builder) {
        String region = config.getRegion().orElse(DEFAULT_REGION);
        ConnectionConfig connectionConfig = getConnectionConfig(config);
        SSLContext sslContext = getSslContext(config);
        TlsStrategy tlsStrategy = sslContext != null ? new DefaultClientTlsStrategy(sslContext) : null;
        builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                .addRequestInterceptorLast(new AwsRequestSigningApacheHttpClient5Interceptor(
                        "es",
                        Aws4UnsignedPayloadSigner.create(),
                        DefaultCredentialsProvider.create(),
                        Region.of(region)))
                .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .setTlsStrategy(tlsStrategy)
                        .build()));
    }

    private void addBasicAuthClientCallback(OpenSearchClientConfig config,
            ApacheHttpClient5TransportBuilder builder) {
        BasicCredentialsProvider credentialProvider = getCredentialProvider(config);
        SSLContext sslContext = getSslContext(config);
        ConnectionConfig connectionConfig = getConnectionConfig(config);

        TlsStrategy tlsStrategy = null;
        if (sslContext != null || !config.isSslCertificateVerification()) {
            SSLContext ctx = sslContext != null ? sslContext : SSLContexts.createDefault();
            tlsStrategy = !config.isSslCertificateVerification()
                    ? new DefaultClientTlsStrategy(ctx, NoopHostnameVerifier.INSTANCE)
                    : new DefaultClientTlsStrategy(ctx);
        }

        final TlsStrategy finalTlsStrategy = tlsStrategy;
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (credentialProvider != null) {
                httpClientBuilder.setDefaultCredentialsProvider(credentialProvider);
            }
            return httpClientBuilder.setConnectionManager(
                    PoolingAsyncClientConnectionManagerBuilder.create()
                            .setDefaultConnectionConfig(connectionConfig)
                            .setTlsStrategy(finalTlsStrategy)
                            .build());
        });
    }

    protected ConnectionConfig getConnectionConfig(OpenSearchClientConfig config) {
        long connectionTimeout = config.getConnectionTimeout().orElse(DEFAULT_CONNECT_TIMEOUT).toMillis();
        long socketTimeout = config.getSocketTimeout().orElse(DEFAULT_SOCKET_TIMEOUT).toMillis();
        return ConnectionConfig.custom()
                               .setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .setSocketTimeout((int) socketTimeout, TimeUnit.MILLISECONDS)
                               .build();
    }

    protected BasicCredentialsProvider getCredentialProvider(OpenSearchClientConfig config) {
        if (config.getUsername().isEmpty() || config.getPassword().isEmpty()) {
            return null;
        }
        var credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(null, -1),
                new UsernamePasswordCredentials(
                        config.getUsername().get(), config.getPassword().get().toCharArray()));
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

    protected void checkConnection(String clientId, org.opensearch.client.opensearch.OpenSearchClient client) {
        try {
            client.ping();
        } catch (IOException e) {
            throw new IllegalStateException("Fail to ping rest node targeted by clientId: " + clientId, e);
        }
    }
}
