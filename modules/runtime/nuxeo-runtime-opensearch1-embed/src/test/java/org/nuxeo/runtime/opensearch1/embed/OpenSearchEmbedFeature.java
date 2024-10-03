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
package org.nuxeo.runtime.opensearch1.embed;

import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.opensearch.cluster.service.ClusterApplierService;
import org.opensearch.gateway.DanglingIndicesState;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.runtime.opensearch1.embed")
@Deploy("org.nuxeo.runtime.opensearch1.embed.test:OSGI-INF/opensearch-embed-test-contrib.xml")
@Features({ LogFeature.class, RuntimeFeature.class })
// remove WARN on elastic embedded because node and cluster identifiers change between test suites
@LoggerLevel(klass = ClusterApplierService.class, level = "ERROR")
// remove WARN on OpenSearch Embed because dangling indices cannot be detected
@LoggerLevel(klass = DanglingIndicesState.class, level = "ERROR")
public class OpenSearchEmbedFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(OpenSearchEmbedFeature.class);

    public static final String SERVER_NAME = "nuxeoTestClusterLocal";

    @Override
    public void start(FeaturesRunner runner) {
        log.info(MARKER_CONSOLE_OVERRIDE, "Deploying an embedded OpenSearch");
    }
}
