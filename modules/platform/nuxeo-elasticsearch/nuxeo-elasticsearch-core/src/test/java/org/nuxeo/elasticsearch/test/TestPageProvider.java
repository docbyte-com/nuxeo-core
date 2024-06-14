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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderType;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.query.PageProviderQueryBuilder;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.opensearch.index.query.QueryBuilder;

@SuppressWarnings("unchecked")
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:schemas-test-contrib.xml")
public class TestPageProvider {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void ICanUseANativePageProvider() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NATIVE_PP_PATTERN");
        assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pageProviderService.getPageProvider("NATIVE_PP_PATTERN", ppdef, null, null, pageSize, 0L,
                props);
        assertNotNull(pp);

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
        assertEquals("TestMe9", doc.getTitle());

        pp.nextPage();
        p = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(pageSize, p.size());
        doc = p.get((int) pageSize - 1);
        assertEquals("TestMe0", doc.getTitle());
    }

    @Test
    public void ICanUseANxqlPageProvider() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN");
        assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider(
                "NXQL_PP_PATTERN", ppdef, null, null, pageSize, 0L, props);
        assertNotNull(pp);

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        List<DocumentModel> p = pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
        assertEquals("TestMe9", doc.getTitle());

        assertTrue(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        pp.nextPage();
        p = pp.getCurrentPage();
        assertEquals(pageSize, p.size());
        doc = p.get((int) pageSize - 1);
        assertEquals("TestMe0", doc.getTitle());

        pageSize = 10000;
        ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN2");
        assertNotNull(ppdef);
        pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider("NXQL_PP_PATTERN2", ppdef, null, null,
                pageSize, 0L, props);
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertEquals(10, p.size());
        doc = p.get(0);
        assertEquals("TestMe9", doc.getTitle());

    }

    @Test
    public void ICanUseANxqlPageProviderWithParameters() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("nxql_search");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pageProviderService.getPageProvider("nxql_search", ppdef, null, null, pageSize, 0L, props);
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        String[] params = { "Select * from File where dc:title LIKE 'Test%'" };
        pp.setParameters(params);
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        String esquery = ((ElasticSearchNxqlPageProvider) pp).getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"match_phrase_prefix\" : {\n" + //
                "          \"dc:title\" : {\n" + //
                "            \"query\" : \"Test\",\n" + //
                "            \"slop\" : 0,\n" + //
                "            \"max_expansions\" : 50,\n" + //
                "            \"zero_terms_query\" : \"NONE\",\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"filter\" : [\n" + //
                "      {\n" + //
                "        \"terms\" : {\n" + //
                "          \"ecm:primaryType\" : [\n" + //
                "            \"File\"\n" + //
                "          ],\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", esquery);

        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        p.get(0);
    }

    @Test
    public void ICanUseANxqlPageProviderWithFixedPart() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_FIXED_PART");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pageProviderService.getPageProvider("NXQL_PP_FIXED_PART", ppdef, model, null, pageSize, 0L,
                props);
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }

        txFeature.nextTransaction();

        String[] params = { session.getRootDocument().getId() };
        pp.setParameters(params);

        // get current page
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        p.get(0);
    }

    @Test
    @ConsoleLogLevelThreshold("ERROR")
    public void ICanUseInvalidPageProvider() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("INVALID_PP");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pageProviderService.getPageProvider("INVALID_PP", ppdef, null, null, 0L, 0L, props);
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
        assertEquals(
                "Query: SELECT * FROM Document WHERE ORDER BY dc:title, Syntax error: Invalid token <ORDER BY> at offset 29",
                pp.getErrorMessage());
    }

    @Test
    public void testNativePredicateIn() {
        QueryBuilder qb;
        WhereClauseDefinition whereClause = pageProviderService.getPageProviderDefinition("TEST_IN").getWhereClause();
        DocumentModel model = session.createDocumentModel("/", "doc", "File");
        model.setPropertyValue("dc:subjects", new String[] { "foo", "bar" });

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"dc:title\" : [\n" + //
                "                \"foo\",\n" + //
                "                \"bar\"\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        model.setPropertyValue("dc:subjects", new String[] { "foo" });
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"dc:title\" : [\n" + //
                "                \"foo\"\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        // criteria with no values are removed
        model.setPropertyValue("dc:subjects", new String[] {});
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"match_all\" : {\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());
    }

    @Test
    public void testNativePredicateInIntegers() {
        QueryBuilder qb;
        WhereClauseDefinition whereClause = pageProviderService.getPageProviderDefinition("TEST_IN_INTEGERS")
                                                               .getWhereClause();
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        @SuppressWarnings("boxing")
        Integer[] array1 = new Integer[] { 1, 2, 3 };
        model.setPropertyValue("search:integerlist", array1);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"size\" : [\n" + //
                "                1,\n" + //
                "                2,\n" + //
                "                3\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        // lists work too
        @SuppressWarnings("boxing")
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        model.setPropertyValue("search:integerlist", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"size\" : [\n" + //
                "                1,\n" + //
                "                2,\n" + //
                "                3\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

    }

    @Test
    public void testNativePredicateInStringList() {
        QueryBuilder qb;
        WhereClauseDefinition whereClause = pageProviderService.getPageProviderDefinition("ADVANCED_SEARCH")
                                                               .getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] arrayString = new String[] { "1", "2", "3" };
        model.setPropertyValue("search:subjects", arrayString);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        String json = qb.toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"query_string\" : {\n" + //
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" + //
                "          \"fields\" : [ ],\n" + //
                "          \"type\" : \"best_fields\",\n" + //
                "          \"default_operator\" : \"or\",\n" + //
                "          \"max_determinized_states\" : 10000,\n" + //
                "          \"enable_position_increments\" : true,\n" + //
                "          \"fuzziness\" : \"AUTO\",\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"phrase_slop\" : 0,\n" + //
                "          \"escape\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"dc:subjects\" : [\n" + //
                "                \"1\",\n" + //
                "                \"2\",\n" + //
                "                \"3\"\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        // lists work too
        @SuppressWarnings("boxing")
        List<String> list = Arrays.asList(arrayString);
        model.setPropertyValue("search:subjects", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows(json, qb.toString());

        // don't take into account empty list
        list = new ArrayList<>();
        model.setPropertyValue("search:subjects", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"match_all\" : {\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());
    }

    @Test
    public void testNativePredicateIsNull() {
        QueryBuilder qb;
        WhereClauseDefinition whereClause = pageProviderService.getPageProviderDefinition("ADVANCED_SEARCH")
                                                               .getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"query_string\" : {\n" + //
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" + //
                "          \"fields\" : [ ],\n" + //
                "          \"type\" : \"best_fields\",\n" + //
                "          \"default_operator\" : \"or\",\n" + //
                "          \"max_determinized_states\" : 10000,\n" + //
                "          \"enable_position_increments\" : true,\n" + //
                "          \"fuzziness\" : \"AUTO\",\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"phrase_slop\" : 0,\n" + //
                "          \"escape\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"wildcard\" : {\n" + //
                "          \"dc:title\" : {\n" + //
                "            \"wildcard\" : \"bar\",\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        model.setPropertyValue("search:isPresent", Boolean.TRUE);

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"query_string\" : {\n" + //
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" + //
                "          \"fields\" : [ ],\n" + //
                "          \"type\" : \"best_fields\",\n" + //
                "          \"default_operator\" : \"or\",\n" + //
                "          \"max_determinized_states\" : 10000,\n" + //
                "          \"enable_position_increments\" : true,\n" + //
                "          \"fuzziness\" : \"AUTO\",\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"phrase_slop\" : 0,\n" + //
                "          \"escape\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"wildcard\" : {\n" + //
                "          \"dc:title\" : {\n" + //
                "            \"wildcard\" : \"bar\",\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"bool\" : {\n" + //
                "              \"must_not\" : [\n" + //
                "                {\n" + //
                "                  \"exists\" : {\n" + //
                "                    \"field\" : \"dc:modified\",\n" + //
                "                    \"boost\" : 1.0\n" + //
                "                  }\n" + //
                "                }\n" + //
                "              ],\n" + //
                "              \"adjust_pure_negative\" : true,\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        // only boolean available in schema without default value
        model.setPropertyValue("search:isPresent", Boolean.FALSE);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"query_string\" : {\n" + //
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" + //
                "          \"fields\" : [ ],\n" + //
                "          \"type\" : \"best_fields\",\n" + //
                "          \"default_operator\" : \"or\",\n" + //
                "          \"max_determinized_states\" : 10000,\n" + //
                "          \"enable_position_increments\" : true,\n" + //
                "          \"fuzziness\" : \"AUTO\",\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"phrase_slop\" : 0,\n" + //
                "          \"escape\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"wildcard\" : {\n" + //
                "          \"dc:title\" : {\n" + //
                "            \"wildcard\" : \"bar\",\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"bool\" : {\n" + //
                "              \"must_not\" : [\n" + //
                "                {\n" + //
                "                  \"exists\" : {\n" + //
                "                    \"field\" : \"dc:modified\",\n" + //
                "                    \"boost\" : 1.0\n" + //
                "                  }\n" + //
                "                }\n" + //
                "              ],\n" + //
                "              \"adjust_pure_negative\" : true,\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

        qb = PageProviderQueryBuilder.makeQuery("SELECT * FROM ? WHERE ? = '?'",
                new Object[] { "Document", "dc:title", null }, false, true, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"query_string\" : {\n" + //
                "    \"query\" : \"SELECT * FROM Document WHERE dc:title = ''\",\n" + //
                "    \"fields\" : [ ],\n" + //
                "    \"type\" : \"best_fields\",\n" + //
                "    \"default_operator\" : \"or\",\n" + //
                "    \"max_determinized_states\" : 10000,\n" + //
                "    \"enable_position_increments\" : true,\n" + //
                "    \"fuzziness\" : \"AUTO\",\n" + //
                "    \"fuzzy_prefix_length\" : 0,\n" + //
                "    \"fuzzy_max_expansions\" : 50,\n" + //
                "    \"phrase_slop\" : 0,\n" + //
                "    \"escape\" : false,\n" + //
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "    \"fuzzy_transpositions\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());

    }

    @Test
    public void testNativeFulltext() {
        QueryBuilder qb;
        WhereClauseDefinition whereClause = pageProviderService.getPageProviderDefinition("ADVANCED_SEARCH")
                                                               .getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setPropertyValue("search:fulltext_all", "you know for search");
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"query_string\" : {\n" + //
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" + //
                "          \"fields\" : [ ],\n" + //
                "          \"type\" : \"best_fields\",\n" + //
                "          \"default_operator\" : \"or\",\n" + //
                "          \"max_determinized_states\" : 10000,\n" + //
                "          \"enable_position_increments\" : true,\n" + //
                "          \"fuzziness\" : \"AUTO\",\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"phrase_slop\" : 0,\n" + //
                "          \"escape\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"simple_query_string\" : {\n" + //
                "          \"query\" : \"you know for search\",\n" + //
                "          \"fields\" : [\n" + //
                "            \"all_field^1.0\"\n" + //
                "          ],\n" + //
                "          \"analyzer\" : \"fulltext\",\n" + //
                "          \"flags\" : -1,\n" + //
                "          \"default_operator\" : \"and\",\n" + //
                "          \"analyze_wildcard\" : false,\n" + //
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" + //
                "          \"fuzzy_prefix_length\" : 0,\n" + //
                "          \"fuzzy_max_expansions\" : 50,\n" + //
                "          \"fuzzy_transpositions\" : true,\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", qb.toString());
    }

    @Test
    public void testNxqlPredicateWithHint() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider(
                "NXQL_WITH_HINT", ppdef, model, null, pageSize, 0L, props);
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"term\" : {\n" + //
                "              \"dc:title.fulltext\" : {\n" + //
                "                \"value\" : \"you know\",\n" + //
                "                \"boost\" : 1.0\n" + //
                "              }\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"fuzzy\" : {\n" + //
                "          \"my_field\" : {\n" + //
                "            \"value\" : \"for search\",\n" + //
                "            \"fuzziness\" : \"AUTO\",\n" + //
                "            \"prefix_length\" : 0,\n" + //
                "            \"max_expansions\" : 50,\n" + //
                "            \"transpositions\" : true,\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"my_subject\" : [\n" + //
                "                \"foo\",\n" + //
                "                \"bar\"\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", esquery);
    }

    @Test
    public void testNxqlPredicateWithHintInParameter() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT_IN_PARAMETER");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider(
                "NXQL_WITH_HINT", ppdef, model, null, pageSize, 0L, props);
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"bool\" : {\n" + //
                "    \"must\" : [\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"term\" : {\n" + //
                "              \"dc:title.fulltext\" : {\n" + //
                "                \"value\" : \"you know\",\n" + //
                "                \"boost\" : 1.0\n" + //
                "              }\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"fuzzy\" : {\n" + //
                "          \"my_field\" : {\n" + //
                "            \"value\" : \"for search\",\n" + //
                "            \"fuzziness\" : \"AUTO\",\n" + //
                "            \"prefix_length\" : 0,\n" + //
                "            \"max_expansions\" : 50,\n" + //
                "            \"transpositions\" : true,\n" + //
                "            \"boost\" : 1.0\n" + //
                "          }\n" + //
                "        }\n" + //
                "      },\n" + //
                "      {\n" + //
                "        \"constant_score\" : {\n" + //
                "          \"filter\" : {\n" + //
                "            \"terms\" : {\n" + //
                "              \"my_subject\" : [\n" + //
                "                \"foo\",\n" + //
                "                \"bar\"\n" + //
                "              ],\n" + //
                "              \"boost\" : 1.0\n" + //
                "            }\n" + //
                "          },\n" + //
                "          \"boost\" : 1.0\n" + //
                "        }\n" + //
                "      }\n" + //
                "    ],\n" + //
                "    \"adjust_pure_negative\" : true,\n" + //
                "    \"boost\" : 1.0\n" + //
                "  }\n" + //
                "}", esquery);
    }

    @Test
    public void testMaxResultWindow() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 2;
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider(
                "NXQL_PP_PATTERN", ppdef, null, null, pageSize, 0L, props);
        pp.setMaxResultWindow(6);
        assertEquals(6, pp.getMaxResultWindow());
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertEquals(5, pp.getNumberOfPages());
        assertTrue(pp.isNextPageAvailable());
        // last page is not accessible
        assertFalse(pp.isLastPageAvailable());
        // only 3 pages are navigable
        assertEquals(3, pp.getPageLimit());
        // page 2
        pp.nextPage();
        assertTrue(pp.isNextPageAvailable());
        // page 3 reach the max result window of 6 docs
        pp.nextPage();
        assertFalse(pp.isNextPageAvailable());
        assertFalse(pp.isLastPageAvailable());
    }

    /**
     * Testing an ES page provider when not specifying limit. This shouldn't crash and be boxed by
     * {@link ElasticSearchNxqlPageProvider#ES_MAX_RESULT_WINDOW_PROPERTY}.
     *
     * @since 10.3
     */
    @Test
    public void iCanPerformUnlimitedQuery() {
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_UNLIMITED");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pageProviderService.getPageProvider("NXQL_PP_UNLIMITED", ppdef, null, null, null, 0L,
                props);
        List<?> page = pp.getCurrentPage();
        // here we test that ES doesn't throw an exception + we're able to retrieve something
        assertFalse(page.isEmpty());
    }

    @Test
    public void ICanUseANxqlPageProviderWithUnrestrictedSession() {

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("UNRESTRICTED_PP");

        HashMap<String, Serializable> props = new HashMap<>();
        CoreSession bobSession = CoreInstance.getCoreSession(session.getRepositoryName(), "bob");
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) bobSession);
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pageProviderService.getPageProvider(
                "UNRESTRICTED_PP", ppdef, null, null, null, 0L, props);

        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            session.createDocument(doc);
        }

        txFeature.nextTransaction();

        List<DocumentModel> docs = pp.getCurrentPage();
        assertEquals(10, docs.size());
    }

    /**
     * @since 2021.8
     */
    @Test
    public void testPageProviderType() {
        PageProvider<?> pageProvider = pageProviderService.getPageProvider("NXQL_PP_PATTERN", null, null, null, null);
        assertEquals(PageProviderType.ELASTIC, pageProviderService.getPageProviderType(pageProvider));
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // make tests pass under Windows
            expected = expected.trim();
            expected = expected.replace("\n", "");
            expected = expected.replace("\r", "");
            actual = actual.trim();
            actual = actual.replace("\n", "");
            actual = actual.replace("\r", "");
        }
        assertEquals(expected, actual);
    }

}
