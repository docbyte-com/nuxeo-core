/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.provider;

import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_AVG;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_COUNT;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MAX;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MISSING;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SUM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_TERMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.search.client.opensearch1.ESQueryTransformer;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateAvg;
import org.nuxeo.ecm.platform.query.core.AggregateCardinality;
import org.nuxeo.ecm.platform.query.core.AggregateCount;
import org.nuxeo.ecm.platform.query.core.AggregateDateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateDateRange;
import org.nuxeo.ecm.platform.query.core.AggregateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateMax;
import org.nuxeo.ecm.platform.query.core.AggregateMin;
import org.nuxeo.ecm.platform.query.core.AggregateMissing;
import org.nuxeo.ecm.platform.query.core.AggregateRange;
import org.nuxeo.ecm.platform.query.core.AggregateSum;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.opensearch.index.query.QueryBuilder;

/**
 * Elasticsearch Page provider that converts the NXQL query build by CoreQueryDocumentPageProvider.
 *
 * @since 5.9.3
 */
public class ElasticSearchNxqlPageProvider extends CoreQueryDocumentPageProvider {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String SEARCH_ON_ALL_REPOSITORIES_PROPERTY = "searchAllRepositories";

    // @since 9.2
    public static final String ES_MAX_RESULT_WINDOW_PROPERTY = "org.nuxeo.elasticsearch.provider.maxResultWindow";

    // This is the default ES index.max_result_window
    public static final long DEFAULT_ES_MAX_RESULT_WINDOW_VALUE = 10000;

    protected static final Logger log = LogManager.getLogger(ElasticSearchNxqlPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected HashMap<String, Aggregate<? extends Bucket>> currentAggregates;

    protected Long maxResultWindow;

    @Override
    public List<DocumentModel> getCurrentPage() {

        long t0 = System.currentTimeMillis();

        // use a cache
        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }
        error = null;
        errorMessage = null;
        log.debug("Perform query for provider '{}': with pageSize={}, offset={}", this::getName,
                this::getMinMaxPageSize, this::getCurrentPageOffset);
        currentPageDocuments = new ArrayList<>();
        CoreSession coreSession = getCoreSession();
        NuxeoPrincipal principal = coreSession.getPrincipal();
        if (useUnrestrictedSession() && !principal.isAdministrator()) {
            coreSession = CoreInstance.getCoreSessionSystem(coreSession.getRepositoryName(), principal.getName());
        }
        if (query == null) {
            buildQuery(coreSession);
        }
        if (query == null) {
            throw new NuxeoException(String.format("Cannot perform null query: check provider '%s'", getName()));
        }
        // Build and execute the ES query
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        try {
            NxQueryBuilder nxQuery = getQueryBuilder(coreSession).nxql(query)
                                                                 .offset((int) getCurrentPageOffset())
                                                                 .limit(getLimit())
                                                                 .addAggregates(buildAggregates());
            if (searchOnAllRepositories()) {
                nxQuery.searchOnAllRepositories();
            }

            List<String> highlightFields = getHighlights();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                nxQuery.highlight(highlightFields);
            }

            EsResult ret = ess.queryAndAggregate(nxQuery);
            DocumentModelList dmList = ret.getDocuments();
            currentAggregates = new HashMap<>(ret.getAggregates().size());
            for (Aggregate<Bucket> agg : ret.getAggregates()) {
                currentAggregates.put(agg.getId(), agg);
            }
            setResultsCount(dmList.totalSize());
            currentPageDocuments = dmList;
        } catch (QueryParseException e) {
            error = e;
            errorMessage = e.getMessage();
            log.warn(e.getMessage(), e);
        }

        // send event for statistics !
        fireSearchEvent(getCoreSession().getPrincipal(), query, currentPageDocuments, System.currentTimeMillis() - t0);

        return currentPageDocuments;
    }

    protected int getLimit() {
        int ret = (int) getMinMaxPageSize();
        if (ret == 0) {
            ret = (int) Long.min(getMaxResultWindow(), Integer.MAX_VALUE);
        }
        return ret;
    }

    public QueryBuilder getCurrentQueryAsEsBuilder() {
        String nxql = getCurrentQuery();
        return ESQueryTransformer.toESQueryBuilder(nxql, getCoreSession());
    }

    @Override
    protected void pageChanged() {
        currentPageDocuments = null;
        currentAggregates = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        currentPageDocuments = null;
        currentAggregates = null;
        super.refresh();
    }

    @Override
    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

    protected List<Aggregate<? extends Bucket>> buildAggregates() {
        ArrayList<Aggregate<? extends Bucket>> ret = new ArrayList<>(getAggregateDefinitions().size());
        boolean skip = isSkipAggregates();
        for (AggregateDefinition def : getAggregateDefinitions()) {
            Aggregate<? extends Bucket> agg = switch (def.getType()) {
                case AGG_TYPE_TERMS -> new AggregateTerm(def, getSearchDocumentModel());
                case AGG_TYPE_RANGE -> new AggregateRange(def, getSearchDocumentModel());
                case AGG_TYPE_DATE_RANGE -> new AggregateDateRange(def, getSearchDocumentModel());
                case AGG_TYPE_HISTOGRAM -> new AggregateHistogram(def, getSearchDocumentModel());
                case AGG_TYPE_DATE_HISTOGRAM -> new AggregateDateHistogram(def, getSearchDocumentModel());
                case AGG_CARDINALITY -> new AggregateCardinality(def, getSearchDocumentModel());
                case AGG_SUM -> new AggregateSum(def, getSearchDocumentModel());
                case AGG_MIN -> new AggregateMin(def, getSearchDocumentModel());
                case AGG_MAX -> new AggregateMax(def, getSearchDocumentModel());
                case AGG_AVG -> new AggregateAvg(def, getSearchDocumentModel());
                case AGG_COUNT -> new AggregateCount(def, getSearchDocumentModel());
                case AGG_MISSING -> new AggregateMissing(def, getSearchDocumentModel());
                default -> throw new IllegalArgumentException("Invalid aggregate type: " + def.getType() + ", " + def);
            };
            if (!skip || !agg.getSelection().isEmpty()) {
                // if we want to skip aggregates but one is selected, it has to be computed to filter the result set
                ret.add(agg);
            }
        }
        return ret;
    }

    protected boolean searchOnAllRepositories() {
        String value = (String) getProperties().get(SEARCH_ON_ALL_REPOSITORIES_PROPERTY);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean hasAggregateSupport() {
        return true;
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        getCurrentPage();
        return currentAggregates;
    }

    /**
     * Extends the default implementation to add results of aggregates
     *
     * @since 7.4
     */
    @Override
    protected void incorporateAggregates(Map<String, Serializable> eventProps) {

        super.incorporateAggregates(eventProps);
        if (currentAggregates != null) {
            HashMap<String, Serializable> aggregateMatches = new HashMap<>();
            for (String key : currentAggregates.keySet()) {
                Aggregate<? extends Bucket> ag = currentAggregates.get(key);
                ArrayList<HashMap<String, Serializable>> buckets = new ArrayList<>();
                for (Bucket bucket : ag.getBuckets()) {
                    HashMap<String, Serializable> b = new HashMap<>();
                    b.put("key", bucket.getKey());
                    b.put("count", bucket.getDocCount());
                    buckets.add(b);
                }
                aggregateMatches.put(key, buckets);
            }
            eventProps.put("aggregatesMatches", aggregateMatches);
        }
    }

    @Override
    public boolean isLastPageAvailable() {
        if ((getResultsCount() + getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public boolean isNextPageAvailable() {
        if ((getCurrentPageOffset() + 2 * getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public long getPageLimit() {
        return getMaxResultWindow() / getPageSize();
    }

    /**
     * Returns the max result window where the PP can navigate without raising Elasticsearch
     * QueryPhaseExecutionException. {@code from + size} must be less than or equal to this value.
     *
     * @since 9.2
     */
    public long getMaxResultWindow() {
        if (maxResultWindow == null) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            maxResultWindow = cs.getLong(ES_MAX_RESULT_WINDOW_PROPERTY, DEFAULT_ES_MAX_RESULT_WINDOW_VALUE);
        }
        return maxResultWindow;
    }

    @Override
    public long getResultsCountLimit() {
        return getMaxResultWindow();
    }

    /**
     * Set the max result window where the PP can navigate, for testing purpose.
     *
     * @since 9.2
     */
    public void setMaxResultWindow(long maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }

    /**
     * @since 2021.11
     */
    protected NxQueryBuilder getQueryBuilder(CoreSession session) {
        return new NxQueryBuilder(session);
    }

}
