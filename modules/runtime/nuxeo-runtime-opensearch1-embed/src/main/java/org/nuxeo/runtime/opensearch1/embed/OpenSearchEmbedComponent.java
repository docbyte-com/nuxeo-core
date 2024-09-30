/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.opensearch1.embed;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 2025.0
 */
public class OpenSearchEmbedComponent extends DefaultComponent implements OpenSearchEmbedService {

    private static final Logger log = LogManager.getLogger(OpenSearchEmbedComponent.class);

    protected static final String XP_SERVER = "server";

    protected final Map<String, OpenSearchEmbedNode> nodes = new HashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.OPENSEARCH - 1;
    }

    @Override
    public void start(ComponentContext context) {
        this.<OpenSearchEmbedServerConfig> getDescriptors(XP_SERVER).stream().peek(config -> {
            if (!Framework.isTestModeSet()) {
                String message = "OpenSearch embed for server \"%s\" is ONLY for testing purpose. Use a dedicated cluster for production.".formatted(
                        config.getName());
                log.warn(message);
                addRuntimeMessage(Level.WARNING, message);
            }
        }).map(OpenSearchEmbedNode::new).forEach(node -> nodes.put(node.getId(), node));
        nodes.values().forEach(OpenSearchEmbedNode::start);
    }

    @Override
    public void stop(ComponentContext context) {
        nodes.values().forEach(OpenSearchEmbedNode::stop);
        nodes.clear();
    }

    @Override
    public String getServerUrl(String id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id).getServerUrl();
        }
        throw new IllegalArgumentException("There is no node with id: " + id);
    }
}
