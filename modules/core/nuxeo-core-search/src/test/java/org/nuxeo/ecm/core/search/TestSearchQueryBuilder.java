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
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_BOOLEAN_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_DATE_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_INTEGER_PROP;
import static org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter.REPOSITORY_PROP;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.schema.CoreSchemaFeature;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSchemaFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/SecurityService.xml")
public class TestSearchQueryBuilder {

    protected static final SearchIndex SEARCH_INDEX = SearchIndex.of("test", "default", "idx");

    @Test
    public void testBadNxql() {
        assertThrows(QueryParseException.class, () -> buildSearchQuery("SELTEC * FROM Document"));
        assertThrows(QueryParseException.class, () -> buildSearchQuery("SELECT * FROM Document -- No comment"));
    }

    @Test
    public void testSimpleNxql() {
        var searchQuery = buildSearchQuery("SELECT * FROM Document");
        assertEquals(SEARCH_INDEX, searchQuery.getSearchIndexes().getFirst());
        assertNotNull(searchQuery.getQuery()); // not asserting the SQLQuery it is already covered in lower modules
        assertEquals(0, searchQuery.getOffset());
        assertEquals(10, searchQuery.getLimit());
        assertEquals(Map.of(ECM_UUID, StringType.INSTANCE, REPOSITORY_PROP, StringType.INSTANCE),
                searchQuery.getSelectFields());
    }

    @Test
    public void testSelectFields() {
        var searchQuery = buildSearchQuery("SELECT tcs:boolean, tcs:date, tcs:integer FROM CommonDocument");
        var expected = new LinkedHashMap<>();
        expected.put(COMMON_BOOLEAN_PROP, BooleanType.INSTANCE);
        expected.put(COMMON_DATE_PROP, DateType.INSTANCE);
        expected.put(COMMON_INTEGER_PROP, LongType.INSTANCE);
        expected.put(ECM_UUID, StringType.INSTANCE);
        expected.put(REPOSITORY_PROP, StringType.INSTANCE);
        assertEquals(expected, searchQuery.getSelectFields());
    }

    @Test
    public void testBadSortInfos() {
        assertThrows(QueryParseException.class,
                () -> SearchQuery.builder(SEARCH_INDEX, "SELECT * FROM Document ORDER BY ecm:name ASC")
                                 .addSort(new SortInfo(ECM_UUID, true))
                                 .build());
    }

    protected static SearchQuery buildSearchQuery(String query) {
        return SearchQuery.builder(SEARCH_INDEX, query).build();
    }
}
