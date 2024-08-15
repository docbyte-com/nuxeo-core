/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import static org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateParserBase.getFieldName;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_AVG;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_COUNT;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MAX;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MISSING;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SUM;

import java.util.List;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.BucketDouble;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.NumericMetricsAggregation;

/**
 * An aggregate that returns a single value.
 *
 * @since 10.3
 */
public final class AggregateSingleValueMetricParser {

    private AggregateSingleValueMetricParser() {
    }

    /**
     * Creates an AggregationBuilder for the supplied aggregate.
     */
    public static AggregationBuilder getAggregate(Aggregate<? extends Bucket> agg) {
        String id = agg.getId();
        String field = getFieldName(agg.getField());
        return switch (agg.getType()) {
            case AGG_CARDINALITY -> AggregationBuilders.cardinality(id).field(field);
            case AGG_COUNT -> AggregationBuilders.count(id).field(field);
            case AGG_SUM -> AggregationBuilders.sum(id).field(field);
            case AGG_AVG -> AggregationBuilders.avg(id).field(field);
            case AGG_MAX -> AggregationBuilders.max(id).field(field);
            case AGG_MIN -> AggregationBuilders.min(id).field(field);
            case AGG_MISSING -> AggregationBuilders.missing(id).field(field);
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + agg);
        };
    }

    public static QueryBuilder getFilter(Aggregate<? extends Bucket> agg) {
        return AggregateTermParserBase.getFilter(agg);
    }

    public static List<BucketDouble> parseBuckets(NumericMetricsAggregation.SingleValue aggregation) {
        return List.of(new BucketDouble(aggregation.getType(), aggregation.value()));
    }

}
