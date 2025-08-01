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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.bulk.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.CONCURRENCY_OPTION;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.ENABLED_OPTION;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.PARTITIONS_OPTION;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
@WithFrameworkProperty(name = ENABLED_OPTION, value = "true")
@WithFrameworkProperty(name = PARTITIONS_OPTION, value = "4")
@WithFrameworkProperty(name = CONCURRENCY_OPTION, value = "4")
public class TestIdleAction {

    @Inject
    protected BulkService bulkService;

    @Test
    public void testIdle() throws Exception {
        // submit a 1s processing (500 * 0.002s)
        BulkCommand command = new BulkCommand.Builder(IdleAction.ACTION_NAME, "500", "system").useGenericScroller()
                                                                                              .param("sleepMillis", "2")
                                                                                              .build();
        String commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertTrue(status.toString(), status.getProcessingDurationMillis() >= 1000);
    }
}
