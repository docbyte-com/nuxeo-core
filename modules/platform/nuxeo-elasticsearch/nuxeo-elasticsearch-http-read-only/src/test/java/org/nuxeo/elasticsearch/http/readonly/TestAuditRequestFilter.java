/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.elasticsearch.http.readonly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.http.readonly.filter.AuditRequestFilter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.http.readonly")
@Deploy("org.nuxeo.elasticsearch.audit.test:elasticsearch-audit-index-test-contrib.xml")
public class TestAuditRequestFilter {

    @Inject
    protected ElasticSearchAdmin esa;

    @Test
    public void testMatchAllAuditAsAdmin() {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        filter.init(TestSearchRequestFilter.getAdminCoreSession(),
                esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE), "pretty", payload);
        assertEquals(payload, filter.getPayload());
    }

    @Test
    public void testMatchAllAuditAsNonAdmin() {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        assertThrows("Non Admin should not be able to access audit", IllegalArgumentException.class,
                () -> filter.init(TestSearchRequestFilter.getNonAdminCoreSession(),
                        esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE), "pretty", payload));
    }

}
