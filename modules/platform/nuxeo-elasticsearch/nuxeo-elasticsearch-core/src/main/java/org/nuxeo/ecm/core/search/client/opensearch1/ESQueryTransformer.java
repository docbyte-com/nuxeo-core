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

import static org.nuxeo.ecm.core.search.client.opensearch1.OpenSearchQueryTransformer.SCORE_FIELD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.runtime.api.Framework;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;

/**
 * @since 2025.0
 */
public class ESQueryTransformer {

    protected static final OpenSearchQueryTransformer TRANSFORMER = new OpenSearchQueryTransformer();

    public static QueryBuilder toESQueryBuilder(final String nxql) {
        return toESQueryBuilder(nxql, null);
    }

    public static QueryBuilder toESQueryBuilder(final String nxql, final CoreSession session) {
        var searchQuery = SearchQuery.builder(session, nxql).build();
        return TRANSFORMER.makeQueryBuilder(searchQuery);
    }

    public static OpenSearchQueryTransformer.QueryAndFilter makeQueryFromSimpleExpression(String op, String nxqlName,
            Object value, Object[] values, EsHint hint) {
        return TRANSFORMER.makeQueryFromSimpleExpression(op, nxqlName, value, values, hint);
    }

    public static List<SortInfo> getSortInfo(String nxql) {
        final List<SortInfo> sortInfos = new ArrayList<>();
        var searchIndex = Framework.getService(SearchService.class).getDefaultSearchIndex();
        SQLQuery nxqlQuery = SearchQuery.builder(searchIndex, nxql).build().getQuery();
        nxqlQuery.accept(new DefaultQueryVisitor() {

            @Override
            public void visitOrderByExpr(OrderByExpr node) {
                String name = TRANSFORMER.getFieldName(node.reference.name, null);
                if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                    name = SCORE_FIELD;
                }
                sortInfos.add(new SortInfo(name, !node.isDescending));
            }
        });
        return sortInfos;
    }

    public static String guessFieldType(String field) {
        return TRANSFORMER.guessFieldType(field);
    }

    public static HighlightBuilder makeHighlighter(SearchQuery searchQuery) {
        return TRANSFORMER.makeHighlighter(searchQuery);
    }

    public static List<FilterAggregationBuilder> getEsAggregates(SearchQuery searchQuery) {
        return TRANSFORMER.makeAggregationBuilders(searchQuery);
    }

    public static QueryBuilder getAggregateFilter(SearchQuery searchQuery) {
        return TRANSFORMER.makeAggregatePostFilter(searchQuery);
    }

    public static Map<String, Type> getSelectClauseFields(String nxql) {
        var searchIndex = Framework.getService(SearchService.class).getDefaultSearchIndex();
        return SearchQuery.builder(searchIndex, nxql).build().getSelectFields();
    }
}
