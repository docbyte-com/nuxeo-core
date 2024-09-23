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
 *     Tiry
 */
package org.nuxeo.audit.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OpenSearchAuditFeature.class)
public class TestOpenSearchAuditBackend {

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    @Test
    public void shouldSupportNativeQueries() {
        auditCoreFeature.generateLogEntries("dummy", "entry", "category", 9);

        String jsonQuery = getClassLoaderResourceAsString("filtredQuery.json");
        List<?> res = backend.nativeQuery(jsonQuery, 0, 5);
        assertEquals(2, res.size());

        jsonQuery = getClassLoaderResourceAsString("filtredQueryWithParams.json");

        res = backend.nativeQuery(jsonQuery, Map.of("category", "category1"), 0, 5);
        assertEquals(1, res.size());
    }
}
