/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.opensearch1.embed;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.opensearch.analysis.common.CommonAnalysisPlugin;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.http.HttpServerTransport;
import org.opensearch.node.Node;
import org.opensearch.node.NodeValidationException;
import org.opensearch.plugins.Plugin;
import org.opensearch.transport.Netty4Plugin;

/**
 * @since 9.3
 */
public class OpenSearchEmbedNode implements Closeable {

    private static final Logger log = LogManager.getLogger(OpenSearchEmbedNode.class);

    protected final OpenSearchEmbedServerConfig config;

    protected Node node;

    public OpenSearchEmbedNode(OpenSearchEmbedServerConfig config) {
        this.config = config;
    }

    public String getId() {
        return config.getId();
    }

    public String getServerUrl() {
        if (node == null) {
            throw new IllegalStateException("The embedded node is not started");
        }
        var http = node.injector().getInstance(HttpServerTransport.class);
        var addresses = http.boundAddress().boundAddresses();
        if (ArrayUtils.isEmpty(addresses)) {
            throw new IllegalStateException("The embedded node did not bind any address");
        }
        return String.format("%s", addresses[0].toString());
    }

    public void start() {
        log.info("Starting embedded (in JVM) OpenSearch: {}", config::getName);
        BiFunction<File, String, String> childPath = (parent, child) -> new File(parent, child).getPath();
        Settings settings = //
                Settings.builder()
                        .put("network.host", config.getNetworkHost())
                        .put("path.home",
                                config.getHomePath()
                                      .orElseGet(() -> childPath.apply(Framework.getRuntime().getHome(),
                                              "tmp/opensearch")))
                        .put("path.data",
                                config.getDataPath()
                                      .orElseGet(() -> childPath.apply(Framework.getRuntime().getHome(),
                                              "data/opensearch")))
                        .put("cluster.name", config.getName())
                        .put("node.name", config.getNodeName())
                        .put("discovery.type", "single-node")
                        .put("http.netty.worker_count", 4)
                        .put("http.cors.enabled", true)
                        .put("http.cors.allow-origin", "*")
                        .put("http.cors.allow-credentials", true)
                        .put("http.cors.allow-headers", "Authorization, X-Requested-With, Content-Type, Content-Length")
                        .put("cluster.routing.allocation.disk.threshold_enabled", false)
                        .put("http.port", config.getHttpPort())
                        .put("index.store.type", "mmapfs")
                        .build();
        log.debug("Using settings: {}", () -> settings.toDelimitedString(','));

        Collection<Class<? extends Plugin>> plugins = new HashSet<>();
        plugins.add(Netty4Plugin.class);
        plugins.add(CommonAnalysisPlugin.class);
        try {
            node = new PluginConfigurableNode(settings, plugins);
            node.start();
        } catch (NodeValidationException e) {
            throw new RuntimeServiceException("Cannot start embedded OpenSearch: " + config.getName(), e);
        }
        log.info("OpenSearch node: {} started", config::getName);
        var clientCluster = node.client().admin().cluster();
        clientCluster.health(clientCluster.prepareHealth()
                                          .setWaitForGreenStatus()
                                          .setTimeout(TimeValue.timeValueSeconds(5))
                                          .request())
                     .actionGet();
        log.debug("OpenSearch node: {} ready", config::getName);
    }

    public void stop() {
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (node != null) {
            log.info("Closing embedded (in JVM) OpenSearch: {}", config::getName);
            node.close();
            log.info("OpenSearch node: {} closed: {}", config::getName, node::isClosed);
        }
        node = null;
    }
}
