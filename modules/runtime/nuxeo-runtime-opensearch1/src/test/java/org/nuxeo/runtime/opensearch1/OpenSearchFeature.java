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
package org.nuxeo.runtime.opensearch1;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_OPENSEARCH_1_SERVERS_PROPERTY;
import static org.nuxeo.runtime.opensearch1.OpenSearchComponent.DEFAULT_CLIENT_ID;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;
import org.nuxeo.runtime.opensearch1.embed.OpenSearchEmbedFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.DynamicFeaturesLoader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.opensearch.client.RestClient;

import com.google.inject.Binder;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.runtime.opensearch1")
@Deploy("org.nuxeo.runtime.opensearch1.test")
@Features({ LogFeature.class, RuntimeFeature.class })
// remove WARN on deprecated on [ignore_throttled] parameter
@LoggerLevel(klass = RestClient.class, level = "ERROR")
public class OpenSearchFeature implements RunnerFeature {

    public OpenSearchFeature(DynamicFeaturesLoader loader) {
        if (!STORAGE_OPENSEARCH_1_SERVERS_PROPERTY.isConfigured()) {
            loader.loadFeature(OpenSearchEmbedFeature.class);
        }
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        var harness = runner.getFeature(RuntimeFeature.class).getHarness();
        if (STORAGE_OPENSEARCH_1_SERVERS_PROPERTY.isConfigured()) {
            harness.deployContrib("org.nuxeo.runtime.opensearch1.test", "OSGI-INF/opensearch-client-test-contrib.xml");
        } else {
            harness.deployContrib("org.nuxeo.runtime.opensearch1.test",
                    "OSGI-INF/opensearch-client-embed-test-contrib.xml");
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(OpenSearchClient.class)
              .toProvider(() -> Framework.getService(OpenSearchClientService.class).getClient(DEFAULT_CLIENT_ID));
    }
}
