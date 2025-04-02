/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.oauth.tests;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.local.DummyLoginFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.oauth2.bulk.GarbageCollectExpiredOAuth2TokensAction;
import org.nuxeo.ecm.platform.oauth2.events.GarbageCollectExpiredOAuth2TokensListener;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features({ DummyLoginFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.oauth")
public class OAuthFeature implements RunnerFeature {

    protected CapturingEventListener capturingListener;

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        this.capturingListener = new CapturingEventListener(GarbageCollectExpiredOAuth2TokensListener.EVENT_NAME);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        this.capturingListener.close();
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(duration -> {
            return this.capturingListener == null || this.capturingListener.getCapturedEvents().isEmpty()
                    || runner.getFeature(CoreBulkFeature.class)
                             .wait(GarbageCollectExpiredOAuth2TokensAction.ACTION_NAME, duration);
        });
    }
}
