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
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_PAGE_PROVIDER;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_TYPE;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_TITLE_PROPERTY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.core.query.test:OSGI-INF/test-aggregate-schemas-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-hints-test-contrib.xml")
@Ignore("NXP-32984 impl hints")
public class TestESHintPageProvider {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testNxqlPredicateWithHint() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider("NXQL_WITH_HINT",
                ppdef, model, null, pageSize, 0L, props);
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        // String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        String esquery = null;
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "dc:title.fulltext" : {
                                "value" : "you know",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "fuzzy" : {
                          "my_field" : {
                            "value" : "for search",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "terms" : {
                              "my_subject" : [
                                "foo",
                                "bar"
                              ],
                              "boost" : 1.0
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", esquery);
    }

    @Test
    public void testNxqlPredicateWithHintInParameter() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT_IN_PARAMETER");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider("NXQL_WITH_HINT",
                ppdef, model, null, pageSize, 0L, props);
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        // String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        String esquery = null;
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "dc:title.fulltext" : {
                                "value" : "you know",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "fuzzy" : {
                          "my_field" : {
                            "value" : "for search",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "terms" : {
                              "my_subject" : [
                                "foo",
                                "bar"
                              ],
                              "boost" : 1.0
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", esquery);
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

    @Test
    @Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-pageprovider-contrib.xml")
    @Deploy("org.nuxeo.elasticsearch.core.test:pageprovider-replacers-test-contrib.xml")
    public void iCanUseFulltextOperatorWithElasticsearchPageProvider() {
        DocumentModel coll1 = session.createDocumentModel("/", "testCollection" + 1, COLLECTION_TYPE);
        coll1.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, coll1.getName());
        DocumentModel coll2 = session.createDocumentModel("/", "testCollection" + 2, COLLECTION_TYPE);
        coll2.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, coll2.getName());
        DocumentModel fufu = session.createDocumentModel("/", "furtiveCollection", COLLECTION_TYPE);
        fufu.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, fufu.getName());

        session.createDocument(coll1);
        session.createDocument(coll2);
        session.createDocument(fufu);

        txFeature.nextTransaction();

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(COLLECTION_PAGE_PROVIDER);

        Map<String, Serializable> props = Map.of(SearchServicePageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                ppdef.getName(), ppdef, null, null, null, 0L, props, "testCo");

        List<DocumentModel> page = pp.getCurrentPage();
        assertEquals(2, page.size());
        assertEquals(coll1.getName(), page.get(0).getName());
        assertEquals(coll2.getName(), page.get(1).getName());
    }
}
