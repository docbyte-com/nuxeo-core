/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.mongodb;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
public class TestMongoDBAuditBackend {

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    @Inject
    protected AuditBackend backend;

    @Test
    public void shouldSupportNativeQueries() {
        auditCoreFeature.generateLogEntries("dummy", "entry", "category", 9);

        String jsonQuery = getClassLoaderResourceAsString("query.json");
        List<?> res = backend.nativeQuery(jsonQuery, 0, 5);

        assertEquals(2, res.size());

        jsonQuery = getClassLoaderResourceAsString("queryWithParams.json");

        Map<String, Object> params = new HashMap<>();
        params.put("category", "category1");
        res = backend.nativeQuery(jsonQuery, params, 0, 5);

        assertEquals(1, res.size());
    }
}
