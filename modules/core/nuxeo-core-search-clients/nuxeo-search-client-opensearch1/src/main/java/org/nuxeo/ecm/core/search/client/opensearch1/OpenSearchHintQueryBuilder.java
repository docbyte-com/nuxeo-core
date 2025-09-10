/*
 * (C) Copyright 2019-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.opensearch.index.query.QueryBuilder;

/**
 * Converts an NXQL OpenSearch Hint into {@link org.opensearch.index.query.QueryBuilder}.
 *
 * @since 11.1
 */
public interface OpenSearchHintQueryBuilder {

    /**
     * Builds the OpenSearch {@link org.opensearch.index.query.QueryBuilder}.
     *
     * @param hint the OpenSearch hint
     * @param fieldName the OpenSearch field name
     * @param value the value that we are looking for
     * @return the {@link QueryBuilder} corresponding to the {@link EsHint OpenSearch hint}
     */
    QueryBuilder make(EsHint hint, String fieldName, Object value);
}
