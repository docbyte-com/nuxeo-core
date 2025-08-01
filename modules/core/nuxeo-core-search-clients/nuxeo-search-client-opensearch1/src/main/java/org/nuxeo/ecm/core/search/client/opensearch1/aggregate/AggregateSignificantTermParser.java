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
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_EXCLUDE_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_INCLUDE_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SIZE_PROP;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;
import org.opensearch.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;

/**
 * @since 6.0
 */
public final class AggregateSignificantTermParser {

    private AggregateSignificantTermParser() {
    }

    public static SignificantTermsAggregationBuilder getAggregate(Aggregate<? extends Bucket> agg) {
        SignificantTermsAggregationBuilder ret = AggregationBuilders.significantTerms(agg.getId())
                                                                    .field(getFieldName(agg.getField()));
        Map<String, String> props = agg.getProperties();
        if (props.containsKey(AGG_SIZE_PROP)) {
            ret.size(AggregateTermParserBase.getAggSize(props.get(AGG_SIZE_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_EXCLUDE_PROP) || props.containsKey(AGG_INCLUDE_PROP)) {
            String include = props.get(AGG_INCLUDE_PROP);
            String exclude = props.get(AGG_EXCLUDE_PROP);
            ret.includeExclude(new IncludeExclude(include, exclude));
        }
        return ret;
    }

    public static QueryBuilder getFilter(Aggregate<? extends Bucket> agg) {
        return AggregateTermParserBase.getFilter(agg);
    }

    public static List<BucketTerm> parseBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        return AggregateTermParserBase.parseBuckets(buckets);
    }
}
