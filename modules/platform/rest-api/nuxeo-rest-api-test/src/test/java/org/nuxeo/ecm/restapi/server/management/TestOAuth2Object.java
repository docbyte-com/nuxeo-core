/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.restapi.server.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SKIP_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_STATE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.1
 */
@Features(ManagementFeature.class)
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-oauth2-directory-contrib.xml")
public class TestOAuth2Object extends ManagementBaseTest {

    @Test
    public void testGCExpiredTokens() {
        var commandId = httpClient.buildDeleteRequest("/management/oauth2/token/expired")
                                  .executeAndThen(new JsonNodeHandler(), node -> {
                                      assertBulkStatusScheduled(node);
                                      return getBulkCommandId(node);
                                  });

        txFeature.nextTransaction();

        httpClient.buildGetRequest("/management/bulk/" + commandId).executeAndConsume(new JsonNodeHandler(), node -> {
            assertEquals(BulkStatus.State.COMPLETED.name(), node.get(STATUS_STATE).asText());
            assertFalse(node.get(STATUS_HAS_ERROR).asBoolean());
            assertEquals(4, node.get(STATUS_SKIP_COUNT).asInt());
            assertEquals(5, node.get(STATUS_PROCESSED).asInt());
            assertEquals(0, node.get(STATUS_ERROR_COUNT).asInt());
            assertEquals(5, node.get(STATUS_TOTAL).asInt());
        });
    }
}
