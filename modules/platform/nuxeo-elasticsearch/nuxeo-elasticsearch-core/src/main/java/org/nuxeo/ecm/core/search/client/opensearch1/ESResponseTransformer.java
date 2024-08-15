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
package org.nuxeo.ecm.core.search.client.opensearch1;

import java.util.List;

import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.opensearch.action.search.SearchResponse;

/**
 * @since 2025.0
 */
public class ESResponseTransformer {

    protected static final OpenSearchResponseTransformer TRANSFORMER = new OpenSearchResponseTransformer();

    public static List<Aggregate<Bucket>> getAggregates(NxQueryBuilder queryBuilder, SearchResponse response) {
        var searchQuery = SearchQuery.builder(queryBuilder.getSession(), queryBuilder.getNxql())
                                     .addAggregates(queryBuilder.getAggregates())
                                     .build();
        return (List<Aggregate<Bucket>>) ((List<? extends Object>) TRANSFORMER.makeAggregates(searchQuery,
                response.getAggregations()));
    }
}
