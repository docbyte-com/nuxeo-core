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
package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import static org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateParserBase.getFieldName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;

/**
 * @since 2025.0
 */
final class AggregateTermParserBase {

    public static final int MAX_AGG_SIZE = 1000;

    public static int getAggSize(String prop) {
        // handle the size = 0 which means all terms in ES 2 and which is not supported in ES 5
        int size = Integer.parseInt(prop);
        return size == 0 ? MAX_AGG_SIZE : size;
    }

    public static QueryBuilder getFilter(Aggregate<? extends Bucket> agg) {
        if (agg.getSelection().isEmpty()) {
            return null;
        }
        return QueryBuilders.termsQuery(getFieldName(agg.getField()), agg.getSelection());
    }

    public static List<BucketTerm> parseBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return List.of();
        }
        List<BucketTerm> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            nxBuckets.add(new BucketTerm(bucket.getKeyAsString(), bucket.getDocCount()));
        }
        return nxBuckets;
    }
}
