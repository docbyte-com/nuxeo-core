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
package org.nuxeo.ecm.core.storage.mongodb.query;

import org.bson.Document;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * Query builder for a MongoDB query from an {@link QueryBuilder}.
 * <p>
 * After calling {@link #walk()} method you will be able to retrieve the {@link #getFilter() filter} and the
 * {@link #getSort() sort} objects to execute the MongoDB query.
 * 
 * @since 2025.0
 */
public class MongoDBQuerySearchBuilder extends MongoDBAbstractSearchBuilder {

    protected final OrderByList orders;

    protected Document order;

    public MongoDBQuerySearchBuilder(MongoDBSearchConverter converter, QueryBuilder queryBuilder) {
        super(converter, queryBuilder.predicate());
        this.orders = queryBuilder.orders();
    }

    @Override
    public void walk() {
        super.walk();
        order = walkOrderBy(orders);
    }

    public Document getSort() {
        return order;
    }

    protected Document walkOrderBy(OrderByList orders) {
        Document orderBy = new Document();
        for (OrderByExpr order : orders) {
            String field = walkReference(order.reference).queryField;
            if (!orderBy.containsKey(field)) {
                orderBy.put(field, order.isDescending ? MINUS_ONE : ONE);
            }
        }
        return orderBy;
    }

    @Override
    protected FieldInfo walkReference(String name) {
        var queryField = converter.keyToBson(name);
        return new FieldInfo(name, name, queryField, queryField, null);
    }
}
