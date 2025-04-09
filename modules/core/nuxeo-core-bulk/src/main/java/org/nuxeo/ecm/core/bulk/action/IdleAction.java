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

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 2025.1
 */
public class IdleAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "idle";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PARAM_SLEEP_DURATION_MS_OPTION = "sleepMillis";

    public static final String ENABLED_OPTION = "nuxeo.bulk.action.idle.enabled";

    public static final String PARTITIONS_OPTION = "nuxeo.bulk.action.idle.defaultPartitions";

    public static final String CONCURRENCY_OPTION = "nuxeo.bulk.action.idle.defaultConcurrency";

    // default to ~200 docs/s per thread
    public static final int DEFAULT_SLEEP_DURATION = 5;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(IdleComputation::new, Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class IdleComputation extends AbstractBulkComputation {

        protected int sleepMsPerDoc;

        public IdleComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable versioningParam = command.getParam(PARAM_SLEEP_DURATION_MS_OPTION);
            if (versioningParam == null) {
                sleepMsPerDoc = DEFAULT_SLEEP_DURATION;
            } else {
                sleepMsPerDoc = Integer.parseInt(versioningParam.toString());
            }
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            try {
                Thread.sleep(sleepMsPerDoc * ids.size());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
