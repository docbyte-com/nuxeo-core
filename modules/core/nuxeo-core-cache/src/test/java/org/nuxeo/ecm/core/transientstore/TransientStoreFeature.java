/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.core.transientstore;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.event.CoreEventFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

// deploy partially the bundle because we have features for cache and transient store
@Deploy("org.nuxeo.ecm.core.cache:OSGI-INF/transientstore-service.xml")
@Features(CoreEventFeature.class)
public class TransientStoreFeature implements RunnerFeature {

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        ((TransientStorageComponent) Framework.getService(TransientStoreService.class)).cleanUpStores();
    }

}
