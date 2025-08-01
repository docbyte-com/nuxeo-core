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
package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import static org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateParserBase.getFieldName;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_EXTENDED_BOUND_MAX_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_EXTENDED_BOUND_MIN_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_INTERVAL_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_ORDER_KEY_ASC;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_ORDER_KEY_DESC;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_ORDER_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateHistogram;
import org.nuxeo.ecm.platform.query.core.BucketRange;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.BucketOrder;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.histogram.Histogram;
import org.opensearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;

/**
 * @since 6.0
 */
public final class AggregateHistogramParser {

    private AggregateHistogramParser() {
    }

    public static HistogramAggregationBuilder getAggregate(Aggregate<? extends Bucket> agg) {
        HistogramAggregationBuilder ret = AggregationBuilders.histogram(agg.getId())
                                                             .field(getFieldName(agg.getField()));
        Map<String, String> props = agg.getProperties();
        ret.interval(getInterval(agg));
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
                case AGG_ORDER_COUNT_DESC:
                    ret.order(BucketOrder.count(false));
                    break;
                case AGG_ORDER_COUNT_ASC:
                    ret.order(BucketOrder.count(true));
                    break;
                case AGG_ORDER_KEY_DESC:
                    ret.order(BucketOrder.key(false));
                    break;
                case AGG_ORDER_KEY_ASC:
                    ret.order(BucketOrder.key(true));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid order: " + props.get(AGG_ORDER_PROP));
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP) && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(Long.parseLong(props.get(AGG_EXTENDED_BOUND_MIN_PROP)),
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MAX_PROP)));
        }
        return ret;
    }

    public static QueryBuilder getFilter(Aggregate<? extends Bucket> agg) {
        if (agg.getSelection().isEmpty()) {
            return null;
        }
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (String sel : agg.getSelection()) {
            RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(getFieldName(agg.getField()));
            long from = Float.valueOf(sel).longValue();
            long to = from + getInterval(agg);
            rangeFilter.gte(from).lt(to);
            ret.should(rangeFilter);
        }
        return ret;
    }

    public static List<BucketRange> parseBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets,
            AggregateHistogram agg) {
        List<BucketRange> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            Histogram.Bucket histoBucket = (Histogram.Bucket) bucket;
            int from = parseInt(histoBucket.getKeyAsString());
            nxBuckets.add(
                    new BucketRange(bucket.getKeyAsString(), from, from + getInterval(agg), histoBucket.getDocCount()));
        }
        return nxBuckets;
    }

    public static int getInterval(Aggregate<? extends Bucket> agg) {
        Map<String, String> props = agg.getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            return Integer.parseInt(props.get(AGG_INTERVAL_PROP));
        } else {
            throw new IllegalArgumentException("interval property must be defined for " + agg);
        }
    }

    protected static int parseInt(String key) {
        if ("-Infinity".equals(key)) {
            return Integer.MIN_VALUE;
        } else if ("+Infinity".equals(key)) {
            return Integer.MAX_VALUE;
        }
        return Math.round(Float.parseFloat(key));
    }

}
