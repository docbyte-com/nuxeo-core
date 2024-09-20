/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.audit.mongodb;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AbstractAuditStorageTest;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
public class TestAuditWithMongoDB extends AbstractAuditStorageTest {

    @Test
    public void shouldSupportNativeQueries() throws Exception {
        LogEntryGen.generate("dummy", "entry", "category", 9);

        String jsonQuery;
        AuditReader reader = Framework.getService(AuditReader.class);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("query.json")) {
            jsonQuery = IOUtils.toString(is, "UTF-8");
        }
        List<?> res = reader.nativeQuery(jsonQuery, 0, 5);

        assertEquals(2, res.size());

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("queryWithParams.json")) {
            jsonQuery = IOUtils.toString(is, "UTF-8");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("category", "category1");
        res = reader.nativeQuery(jsonQuery, params, 0, 5);

        assertEquals(1, res.size());
    }

    @Override
    protected void flush() throws Exception {
        LogEntryGen.flushAndSync();
    }
}
