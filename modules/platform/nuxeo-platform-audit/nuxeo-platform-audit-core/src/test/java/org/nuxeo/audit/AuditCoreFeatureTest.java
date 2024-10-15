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
package org.nuxeo.audit;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.AuditCoreFeatureTest.DEFAULT_AUDIT_BACKEND_FACTORY;
import static org.nuxeo.audit.AuditCoreFeatureTest.DEFAULT_AUDIT_BACKEND_PROPERTY;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(AuditCoreFeature.class)
// audit core requires to have an audit backend in order to start - at server level this is configured in templates
@WithFrameworkProperty(name = DEFAULT_AUDIT_BACKEND_PROPERTY, value = DEFAULT_AUDIT_BACKEND_FACTORY)
public class AuditCoreFeatureTest {

    public static final String DEFAULT_AUDIT_BACKEND_PROPERTY = "nuxeo.audit.backend.default.factory";

    public static final String DEFAULT_AUDIT_BACKEND_FACTORY = "org.nuxeo.audit.mem.MemAuditBackendFactory";

    @Test
    public void testRuntimeStarted() {
        assertTrue("Nuxeo Runtime should start without error, check Nuxeo Platform Started log.",
                Framework.getRuntime().getStatusMessage(new StringBuilder()));
    }
}
