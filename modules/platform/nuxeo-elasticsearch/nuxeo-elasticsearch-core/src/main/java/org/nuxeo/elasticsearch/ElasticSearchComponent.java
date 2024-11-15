/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

import static org.nuxeo.common.concurrent.ThreadFactories.newThreadFactory;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.ES_ENABLED_PROPERTY;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.config.ESHintQueryBuilderDescriptor;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchAdminImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Component used to configure and manage ElasticSearch integration
 */
public class ElasticSearchComponent extends DefaultComponent implements ElasticSearchAdmin {

    private static final Logger log = LogManager.getLogger(ElasticSearchComponent.class);

    protected static final String EP_INDEX = "elasticSearchIndex";

    /**
     * @since 11.1
     */
    protected static final String EP_HINTS = "elasticSearchHints";

    protected final Map<String, ElasticSearchIndexConfig> indexConfig = new HashMap<>();

    protected ElasticSearchAdminImpl esa;

    protected ListeningExecutorService waiterExecutorService;

    // Nuxeo Component impl ======================================é=============
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
            case EP_INDEX:
                ElasticSearchIndexConfig idx = (ElasticSearchIndexConfig) contribution;
                ElasticSearchIndexConfig previous = indexConfig.get(idx.getName());
                if (idx.isEnabled()) {
                    if (previous != null) {
                        previous.merge(idx);
                        indexConfig.put(idx.getName(), previous);
                    } else {
                        indexConfig.put(idx.getName(), idx);
                    }
                    log.info("Registering index configuration: {}, loaded from {}", idx, contributor.getName());
                } else if (previous != null) {
                    log.info("Disabling index configuration: {}, deactivated by {}", previous, contributor.getName());
                    indexConfig.remove(idx.getName());
                }
                break;
            case EP_HINTS:
                ESHintQueryBuilderDescriptor esHintDescriptor = (ESHintQueryBuilderDescriptor) contribution;
                register(EP_HINTS, esHintDescriptor);
                break;
            default:
                throw new IllegalStateException("Invalid EP: " + extensionPoint);
        }
    }

    @Override
    public void start(ComponentContext context) {
        if (!isElasticsearchEnabled()) {
            log.info("Elasticsearch service is disabled");
            return;
        }
        esa = new ElasticSearchAdminImpl(indexConfig, getDescriptors(EP_HINTS));
        initListenerThreadPool();
    }

    @Override
    public void stop(ComponentContext context) {
        if (esa == null) {
            // Elasticsearch service was disabled
            return;
        }
        try {
            shutdownListenerThreadPool();
        } finally {
            try {
                esa.disconnect();
            } finally {
                esa = null;
            }
        }
    }

    protected boolean isElasticsearchEnabled() {
        return Boolean.parseBoolean(Framework.getProperty(ES_ENABLED_PROPERTY, "true"));
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.ELASTIC;
    }

    // Es Admin ================================================================

    @Override
    public OpenSearchClient getClient() {
        return esa.getClient();
    }

    @Override
    public void initIndexes(boolean dropIfExists) {
        esa.initIndexes(dropIfExists);
    }

    @Override
    public void dropAndInitIndex(String indexName) {
        esa.dropAndInitIndex(indexName);
    }

    @Override
    public void dropAndInitRepositoryIndex(String repositoryName, boolean syncAlias) {
        esa.dropAndInitRepositoryIndex(repositoryName, syncAlias);
    }

    @Override
    public void initRepositoryIndexWithAliases(String repositoryName) {
        esa.initRepositoryIndexWithAliases(repositoryName);
    }

    @Override
    public List<String> getRepositoryNames() {
        return esa.getRepositoryNames();
    }

    @Override
    public String getIndexNameForRepository(String repositoryName) {
        return esa.getIndexNameForRepository(repositoryName);
    }

    @Override
    public String getRepositoryForIndex(String indexName) {
        return esa.getRepositoryForIndex(indexName);
    }

    @Override
    public List<String> getIndexNamesForType(String type) {
        return esa.getIndexNamesForType(type);
    }

    @Override
    public String getIndexNameForType(String type) {
        return esa.getIndexNameForType(type);
    }

    @Override
    public String getWriteIndexName(String searchIndexName) {
        return esa.getWriteIndexName(searchIndexName);
    }

    @Override
    public String getSecondaryWriteIndexName(String searchIndexName) {
        return esa.getSecondaryWriteIndexName(searchIndexName);
    }

    @Override
    public void syncSearchAndWriteAlias(String searchIndexName) {
        esa.syncSearchAndWriteAlias(searchIndexName);
    }

    @Override
    public boolean useExternalVersion() {
        return esa.useExternalVersion();
    }

    @Override
    public ListenableFuture<Boolean> prepareWaitForIndexing() {
        return waiterExecutorService.submit(() -> {
            var searchIndexingService = Framework.getService(SearchIndexingService.class);
            boolean completed;
            do {
                completed = searchIndexingService.await(Duration.ofMinutes(5));
            } while (!completed);
            return true;
        });
    }

    protected void initListenerThreadPool() {
        waiterExecutorService = MoreExecutors.listeningDecorator(
                Executors.newCachedThreadPool(newThreadFactory("waitForEsIndexing")));
    }

    protected void shutdownListenerThreadPool() {
        try {
            waiterExecutorService.shutdown();
        } finally {
            waiterExecutorService = null;
        }
    }

    @Override
    public void refresh() {
        esa.refresh();
    }

    @Override
    public void refreshRepositoryIndex(String repositoryName) {
        esa.refreshRepositoryIndex(repositoryName);
    }

    @Override
    public void flush() {
        esa.flush();
    }

    @Override
    public void flushRepositoryIndex(String repositoryName) {
        esa.flushRepositoryIndex(repositoryName);
    }

    @Override
    public void optimize() {
        esa.optimize();
    }

    @Override
    public void optimizeRepositoryIndex(String repositoryName) {
        esa.optimizeRepositoryIndex(repositoryName);
    }

    @Override
    public void optimizeIndex(String indexName) {
        esa.optimizeIndex(indexName);
    }

    // misc ====================================================================
    public boolean isReady() {
        return (esa != null) && esa.isReady();
    }

    @Override
    public Optional<ESHintQueryBuilder> getHintByOperator(String name) {
        return esa.getHintByOperator(name);
    }
}
