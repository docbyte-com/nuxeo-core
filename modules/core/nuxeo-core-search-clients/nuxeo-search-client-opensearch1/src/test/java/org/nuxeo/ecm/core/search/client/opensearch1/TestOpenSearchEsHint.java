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

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.EsIdentifierList;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.AnyOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MyTestTermOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.NestedFilesOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.RegexOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.TestBoolQueryOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.TestTermOpenSearchHintQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.opensearch.common.geo.ShapeRelation;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.GeoBoundingBoxQueryBuilder;
import org.opensearch.index.query.GeoDistanceQueryBuilder;
import org.opensearch.index.query.GeoShapeQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * The TestOpenSearchHintQueryBuilder allow :
 * <ul>
 * <li>add/override/remove contribution to ES hints extension.</li>
 * <li>The consistency between ES Hint queries and ES queries.</li>
 * </ul>
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(OpenSearchCoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.search.client.opensearch1.test:OSGI-INF/test-opensearch-hint-contrib.xml")
public class TestOpenSearchEsHint {

    public static final String ANY_FIELD_NAME = "anyField";

    public static final String ANY_VALUE_NAME = "anyValue";

    @Inject
    protected OpenSearchSearchClientFactory clientFactory;

    protected Map<String, OpenSearchHintQueryBuilder> hintBuilders;

    @Before
    public void before() {
        hintBuilders = clientFactory.getHints();
    }

    @Test
    public void shouldRetrieveOpenSearchHintQueryBuilderWithoutException() {
        assertTrue(hintBuilders.get("testTermQuery") instanceof TestTermOpenSearchHintQueryBuilder);
        assertTrue(hintBuilders.get("testBoolQuery") instanceof TestBoolQueryOpenSearchHintQueryBuilder);
        assertTrue(hintBuilders.get("anyESHintToRemove") instanceof AnyOpenSearchHintQueryBuilder);
        assertTrue(hintBuilders.get("nestedFilesQuery") instanceof NestedFilesOpenSearchHintQueryBuilder);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.search.client.opensearch1.test:OSGI-INF/test-opensearch-hint-override-contrib.xml")
    public void shouldOverrideOpenSearchHintQueryBuilderContributionWithoutException() {
        assertTrue(hintBuilders.get("testTermQuery") instanceof MyTestTermOpenSearchHintQueryBuilder);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.search.client.opensearch1.test:OSGI-INF/test-opensearch-hint-remove-contrib.xml")
    public void shouldRemoveOpenSearchHintQueryBuilderContributionWithoutException() {
        assertNull(hintBuilders.get("anyESHintToRemove"));

        // Ensure that the others are already exist.
        assertNotNull(hintBuilders.get("testTermQuery"));
        assertNotNull(hintBuilders.get("testBoolQuery"));
    }

    @Test
    public void shouldEnsureEqualityBetweenESQueriesAndESHintQueries() {
        verify(QueryBuilders.commonTermsQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.fuzzyQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchPhraseQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchPhrasePrefixQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.queryStringQuery(ANY_VALUE_NAME).field(ANY_FIELD_NAME));
        verify(QueryBuilders.multiMatchQuery(ANY_VALUE_NAME, ANY_FIELD_NAME));
        verify(QueryBuilders.wildcardQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.simpleQueryStringQuery(ANY_VALUE_NAME).field((ANY_FIELD_NAME)));
    }

    @Test
    public void shouldEnsureEqualityBetweenESGeoQueriesAndESHintGeoQueries() {
        String[] points = new String[] { "41", "71", "34", "50" };

        GeoBoundingBoxQueryBuilder geoEsBuilderQuery = QueryBuilders.geoBoundingBoxQuery(ANY_FIELD_NAME)
                                                                    .setCornersOGC(points[0], points[1]);
        OpenSearchHintQueryBuilder geoBoundingBoxOpenSearchHintQueryBuilder = hintBuilders.get(
                GeoBoundingBoxQueryBuilder.NAME);
        assertEquals(geoEsBuilderQuery,
                geoBoundingBoxOpenSearchHintQueryBuilder.make(null, ANY_FIELD_NAME, Arrays.copyOfRange(points, 0, 2)));

        GeoShapeQueryBuilder geoShapeQueryBuilder = QueryBuilders.geoShapeQuery(ANY_FIELD_NAME, points[0])
                                                                 .relation(ShapeRelation.WITHIN)
                                                                 .indexedShapeIndex(points[2])
                                                                 .indexedShapePath(points[3]);

        OpenSearchHintQueryBuilder geoShapeOpenSearchHintQueryBuilder = hintBuilders.get(GeoShapeQueryBuilder.NAME);
        assertEquals(geoShapeQueryBuilder, geoShapeOpenSearchHintQueryBuilder.make(null, ANY_FIELD_NAME, points));
    }

    @Test
    public void shouldFailWhenMakeCommonGeoESHintQueryWithIllegalArguments() {
        var builder = hintBuilders.get(GeoBoundingBoxQueryBuilder.NAME);
        assertNotNull(builder);
        var ne = assertThrows(NuxeoException.class, () -> builder.make(null, ANY_FIELD_NAME, "notArray"));
        assertEquals("Expected an array, found class java.lang.String", ne.getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    @Test
    public void shouldFailWhenMakeCommonGeoESHintQueryWithInvalidPosition() {
        var builder = hintBuilders.get(GeoBoundingBoxQueryBuilder.NAME);
        assertNotNull(builder);
        var ne = assertThrows(NuxeoException.class,
                () -> builder.make(null, ANY_FIELD_NAME, new String[] { "lPostion", "rPosition" }));
        assertEquals("Invalid value for Geo-point: lPostion", ne.getMessage());
        assertEquals("unsupported symbol [l] in geohash [lPostion]", ne.getCause().getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    @Test
    public void shouldFailWhenMakeGeoBoundingBoxESHintQueryWithIllegalArguments() {
        var builder = hintBuilders.get(GeoBoundingBoxQueryBuilder.NAME);
        assertNotNull(builder);
        var ne = assertThrows(NuxeoException.class, () -> builder.make(null, ANY_FIELD_NAME, new String[5]));
        assertEquals(
                "Hints: GeoBoundingBoxOpenSearchHintQueryBuilder requires 2 parameters: bottomLeft and topRight point",
                ne.getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    @Test
    public void shouldFailWhenMakeGeoDistanceESHintQueryWithIllegalArguments() {
        var builder = hintBuilders.get(GeoDistanceQueryBuilder.NAME);
        assertNotNull(builder);
        var ne = assertThrows(NuxeoException.class, () -> builder.make(null, ANY_FIELD_NAME, new String[10]));
        assertEquals("Hints: GeoDistanceOpenSearchHintQueryBuilder requires 2 parameters: point and distance",
                ne.getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    @Test
    public void shouldFailWhenMakeGeoShapeESHintQueryWithIllegalArguments() {
        var builder = hintBuilders.get(GeoShapeQueryBuilder.NAME);
        assertNotNull(builder);
        var ne = assertThrows(NuxeoException.class, () -> builder.make(null, ANY_FIELD_NAME, new String[10]));
        assertEquals(
                "Hints: GeoShapeOpenSearchHintQueryBuilder requires 4 parameters: shapeId, type (unused), index and path",
                ne.getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    @Test
    public void shouldFailWhenMakeNestedESHintQueryWithIllegalArguments() {
        var builder = hintBuilders.get("nestedFilesQuery");
        assertTrue(builder instanceof NestedFilesOpenSearchHintQueryBuilder);
        var ne = assertThrows(NuxeoException.class,
                () -> builder.make(new EsHint(new EsIdentifierList("files:files.file.name"), null, null),
                        ANY_FIELD_NAME, new String[2]));
        assertEquals("Fields size and values length should be the same", ne.getMessage());
        assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
    }

    /**
     * {@link RegexOpenSearchHintQueryBuilder} has a dedicated Test and cannot be integrated in
     * {@link #shouldEnsureEqualityBetweenESQueriesAndESHintQueries}. {@link RegexOpenSearchHintQueryBuilder} is a
     * special case as the operator NXQL <strong>"regex"</strong> and it's different from
     * {@link org.opensearch.index.query.RegexpQueryBuilder#NAME}. Most of the time the NXQL ESHint and the
     * Elasticsearch operator have the same name. But sometimes they are different.
     */
    @Test
    public void shouldEnsureEqualityBetweenESRegexQueryAndRegexESHintQueries() {
        var builder = hintBuilders.get("regex");
        assertNotNull(builder);
        verify(QueryBuilders.regexpQuery(ANY_FIELD_NAME, ANY_VALUE_NAME), builder);
    }

    protected void verify(AbstractQueryBuilder esQueryBuilder) {
        var builder = hintBuilders.get(esQueryBuilder.getWriteableName());
        assertNotNull(builder);
        verify(esQueryBuilder, builder);
    }

    protected void verify(AbstractQueryBuilder esQueryBuilder, OpenSearchHintQueryBuilder builder) {
        EsHint esHint = new EsHint(new EsIdentifierList(ANY_FIELD_NAME), null, null);
        QueryBuilder esHintQueryBuilder = builder.make(esHint, ANY_FIELD_NAME, ANY_VALUE_NAME);

        assertNotNull(esHintQueryBuilder);
        assertEquals(esQueryBuilder, esHintQueryBuilder);
    }
}
