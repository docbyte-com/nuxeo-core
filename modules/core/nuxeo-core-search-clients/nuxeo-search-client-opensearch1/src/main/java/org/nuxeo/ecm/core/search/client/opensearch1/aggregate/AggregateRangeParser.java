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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.BucketRange;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.range.Range;
import org.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;

/**
 * @since 6.0
 */
public final class AggregateRangeParser {

    private AggregateRangeParser() {
    }

    public static RangeAggregationBuilder getAggregate(Aggregate<? extends Bucket> agg) {
        RangeAggregationBuilder ret = AggregationBuilders.range(agg.getId()).field(getFieldName(agg.getField()));
        for (AggregateRangeDefinition range : agg.getRanges()) {
            if (range.getFrom() != null) {
                if (range.getTo() != null) {
                    ret.addRange(range.getKey(), range.getFrom(), range.getTo());
                } else {
                    ret.addUnboundedFrom(range.getKey(), range.getFrom());
                }
            } else if (range.getTo() != null) {
                ret.addUnboundedTo(range.getKey(), range.getTo());
            }
        }
        return ret;
    }

    public static QueryBuilder getFilter(Aggregate<? extends Bucket> agg) {
        if (agg.getSelection().isEmpty()) {
            return null;
        }
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (AggregateRangeDefinition range : agg.getRanges()) {
            if (agg.getSelection().contains(range.getKey())) {
                RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(getFieldName(agg.getField()));
                if (range.getFrom() != null) {
                    rangeFilter.gte(range.getFrom());
                }
                if (range.getTo() != null) {
                    rangeFilter.lt(range.getTo());
                }
                ret.should(rangeFilter);
            }
        }
        return ret;
    }

    public static List<BucketRange> parseBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return List.of();
        }
        List<BucketRange> ret = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            Range.Bucket rangeBucket = (Range.Bucket) bucket;
            double from = (double) rangeBucket.getFrom();
            double to = (double) rangeBucket.getTo();
            ret.add(new BucketRange(bucket.getKeyAsString(), from, to, rangeBucket.getDocCount()));
        }
        return ret;
    }

}
