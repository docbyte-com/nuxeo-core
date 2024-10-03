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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.test;

import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.client.ESRestClientFactory;
import org.nuxeo.elasticsearch.client.ESTransportClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.embed.OpenSearchEmbedFeature;
import org.nuxeo.runtime.opensearch1.embed.OpenSearchEmbedService;

/**
 * This ES client factory uses environment properties to choose the type of client during tests.
 *
 * @since 9.3
 */
public class ESTestClientFactory implements ESClientFactory {

    private static final Logger log = LogManager.getLogger(ESTestClientFactory.class);

    public static final String TRANSPORT_CLIENT = "TransportClient";

    public static final String REST_CLIENT = "RestClient";

    public static final String DEFAULT_CLIENT = REST_CLIENT;

    public static final String CLIENT_PROPERTY = "nuxeo.test.elasticsearch.client";

    public static final String CLUSTER_NAME_PROPERTY = "nuxeo.test.elasticsearch.clusterName";

    @Override
    public ESClient create(ElasticSearchClientConfig config) {
        // we don't use the provided config for the client
        String clientType = System.getProperty(CLIENT_PROPERTY);
        return switch (clientType != null ? clientType : DEFAULT_CLIENT) {
            case TRANSPORT_CLIENT -> createTransportClient();
            case REST_CLIENT -> createRestClient();
            default -> throw new IllegalArgumentException("Unknown Elasticsearch client type: " + clientType);
        };
    }

    protected ESClient createTransportClient() {
        ESTransportClientFactory factory = new ESTransportClientFactory();
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        addOptions(config);
        log.info(MARKER_CONSOLE_OVERRIDE, "ElasticSearchClient: TransportClient on {}",
                config.getOption("addressList"));
        return factory.create(config);
    }

    protected ESClient createRestClient() {
        ESRestClientFactory factory = new ESRestClientFactory();
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        addOptions(config);
        log.info(MARKER_CONSOLE_OVERRIDE, "ElasticSearchClient: RestClient on {}", config.getOption("addressList"));
        return factory.create(config);
    }

    protected void addOptions(ElasticSearchClientConfig config) {
        String addressList;
        if (!ThirdPartyUnderTest.STORAGE_OPENSEARCH_1_SERVERS_PROPERTY.isConfigured()) {
            // 1 embedded
            addressList = Framework.getService(OpenSearchEmbedService.class)
                                   .getServerUrl(OpenSearchEmbedFeature.SERVER_NAME);
        } else {
            // 2 explicit + legacy
            addressList = ThirdPartyUnderTest.STORAGE_OPENSEARCH_1_SERVERS_VALUE;
        }
        config.options.put("addressList", addressList);
        String clusterName = System.getProperty(CLUSTER_NAME_PROPERTY);
        if (clusterName != null) {
            config.options.put("clusterName", clusterName);
            if (addressList == null) {
                config.options.put("addressList", "localhost:9300");
            }
        }
    }
}
