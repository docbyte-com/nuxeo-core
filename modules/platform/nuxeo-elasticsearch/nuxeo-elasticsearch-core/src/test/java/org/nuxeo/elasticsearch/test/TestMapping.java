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
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.tag")
// tags relies on it and not available when Mongodb
@Deploy("org.nuxeo.ecm.core.storage.sql:OSGI-INF/querymaker-service.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-mapping-contrib.xml")
public class TestMapping {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testIlikeSearch() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File"); // NOSONAR
        doc.setPropertyValue("dc:title", "upper case"); // NOSONAR
        doc.setPropertyValue("dc:description", "UPPER CASE DESC"); // NOSONAR
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File"); // NOSONAR
        doc.setPropertyValue("dc:title", "Mixed Case");
        doc.setPropertyValue("dc:description", "MiXeD cAsE dEsC");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc3", "File");
        doc.setPropertyValue("dc:title", "lower case");
        doc.setPropertyValue("dc:description", "lower case desc");
        session.createDocument(doc);

        txFeature.nextTransaction();

        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description ILIKE '%Case%'"));
        assertEquals(3, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description ILIKE 'Upper%'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dc:description ILIKE 'mixED case desc'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title LIKE 'case%'"));
        assertEquals(3, ret.totalSize());
        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title LIKE 'Case%'"));
        assertEquals(3, ret.totalSize());

        // case sensitive for other operation
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE '%Case%'"));
        assertEquals(0, ret.totalSize());
        try {
            ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE 'Upper%'"));
            fail("phrase prefix on keyword should raise error on elastic 7.x");
        } catch (RuntimeServiceException e) {
            // expected
        }
        try {
            ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE 'UPPER%'"));
            fail("phrase prefix on keyword should raise error on elastic 7.x");
        } catch (RuntimeServiceException e) {
            // expected
        }
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE '%Case%'"));
        assertEquals(3, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE 'Upper%'"));
        assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE 'UPPER%'"));
        assertEquals(1, ret.totalSize());

        assertEquals(1, ret.totalSize());

    }

    @Test
    public void testFulltextAnalyzer() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "new-york.jpg");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "York.jpg");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc3", "File");
        doc.setPropertyValue("dc:title", "foo_jpg");
        session.createDocument(doc);

        txFeature.nextTransaction();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new-york.jpg'"));
        assertEquals(1, ret.totalSize());

        // The standard tokenizer first split new-york.jpg in "new" "york.jpg"
        // then the word delimiter gives: "new" york" "jpg" "york.jpg"
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new york jpg'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new-york'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'york new'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'york -new-york'"));
        assertEquals(1, ret.totalSize());
        assertEquals("testDoc2", ret.get(0).getName());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'NewYork'"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'jpg'"));
        assertEquals(3, ret.totalSize());

    }

    @Test
    public void testNgramSearch() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "FooBar12 test");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "foobar42");
        session.createDocument(doc);

        txFeature.nextTransaction();

        // Common left/right truncature with a ILIKE, translated into wilcard search *oba* with poor performance
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:title ILIKE '%Oba%'"));
        assertEquals(2, ret.totalSize());

        // Use an ngram index
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'ObA'"));
        assertEquals(2, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'fOObar42'"));
        assertEquals(1, ret.totalSize());

        // No tokenizer mind the space
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = '2 t'"));
        assertEquals(1, ret.totalSize());

        // need to provide min_ngram (3) or more characters
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = '42'"));
        assertEquals(0, ret.totalSize());

        // If we don't set the proper analyzer the searched term is also ngramized matching too much
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) OPERATOR(match) */ dc:title = 'ZOObar'"));
        assertEquals(2, ret.totalSize());

        // Using the lowercase analyzer for the search term and a ngram max_gram greater than the search term make it
        // work
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'ZOObar'"));
        assertEquals(0, ret.totalSize());

    }
}
