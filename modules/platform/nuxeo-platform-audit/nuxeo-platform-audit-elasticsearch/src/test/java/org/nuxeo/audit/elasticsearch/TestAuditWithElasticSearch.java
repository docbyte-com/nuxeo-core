/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *
 */
package org.nuxeo.audit.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.uidgen.KeyValueStoreUIDSequencer.DEFAULT_STORE_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-default-sequencer-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.audit")
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:audit-test-contrib.xml")
public class TestAuditWithElasticSearch {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
        ((KeyValueStoreProvider) Framework.getService(KeyValueService.class)
                                          .getKeyValueStore(DEFAULT_STORE_NAME)).clear();
    }

    @Test
    public void shouldSupportNativeQueries() throws Exception {
        LogEntryGen.generate("dummy", "entry", "category", 9);

        AuditReader reader = Framework.getService(AuditReader.class);

        String jsonQuery = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("filtredQuery.json"),
                "UTF-8");
        List<?> res = reader.nativeQuery(jsonQuery, 0, 5);

        assertEquals(2, res.size());

        jsonQuery = IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("filtredQueryWithParams.json"), "UTF-8");

        Map<String, Object> params = new HashMap<>();
        params.put("category", "category1");
        res = reader.nativeQuery(jsonQuery, params, 0, 5);

        assertEquals(1, res.size());
    }
}
