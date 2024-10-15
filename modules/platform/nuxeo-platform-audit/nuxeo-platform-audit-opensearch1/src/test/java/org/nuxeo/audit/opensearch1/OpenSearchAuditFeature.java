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
package org.nuxeo.audit.opensearch1;

import static org.nuxeo.audit.AuditCoreFeatureTest.DEFAULT_AUDIT_BACKEND_PROPERTY;
import static org.nuxeo.audit.opensearch1.OpenSearchAuditFeature.AUDIT_BACKEND_FACTORY;

import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;
import org.nuxeo.runtime.opensearch1.OpenSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-default-sequencer-contrib.xml")
@Deploy("org.nuxeo.audit.opensearch1")
@Deploy("org.nuxeo.audit.opensearch1.test")
@Features({ AuditCoreFeature.class, OpenSearchFeature.class })
@WithFrameworkProperty(name = DEFAULT_AUDIT_BACKEND_PROPERTY, value = AUDIT_BACKEND_FACTORY)
public class OpenSearchAuditFeature implements RunnerFeature {

    public static final String AUDIT_BACKEND_FACTORY = "org.nuxeo.audit.opensearch1.OpenSearchAuditBackendFactory";

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(duration -> {
            long begin = System.currentTimeMillis();
            var client = Framework.getService(OpenSearchClientService.class).getClient("audit/default");
            client.flush("audit");
            client.refresh("audit");
            return duration.toMillis() > System.currentTimeMillis() - begin;
        });
    }
}
