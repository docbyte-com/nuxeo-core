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
package org.nuxeo.runtime.pubsub;

import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.runtime.pubsub")
@Deploy("org.nuxeo.runtime.pubsub.tests")
@Features(ClusterFeature.class)
public class PubSubFeature implements RunnerFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        var harness = runner.getFeature(RuntimeFeature.class).getHarness();
        var bundle = harness.getOSGiAdapter().getBundle("org.nuxeo.runtime.stream");
        if (bundle != null) {
            // if nuxeo-runtime-stream is deployed switch the pub/sub implementation to stream
            harness.deployContrib("org.nuxeo.runtime.pubsub.tests", "test-stream-contrib.xml");
            harness.deployContrib("org.nuxeo.runtime.pubsub.tests", "test-stream-pubsub-contrib.xml");
        }
    }
}
