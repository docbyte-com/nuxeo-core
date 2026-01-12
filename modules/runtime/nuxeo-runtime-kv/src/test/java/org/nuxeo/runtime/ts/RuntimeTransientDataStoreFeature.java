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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.ts;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 2025.8
 */
@Deploy("org.nuxeo.runtime.kv:OSGI-INF/transientdata-service.xml")
@Features(RuntimeFeature.class)
public class RuntimeTransientDataStoreFeature implements RunnerFeature {

    /**
     * Use this property with {@link org.nuxeo.runtime.test.runner.WithFrameworkProperty} to change the
     * {@link TransientDataStore} default implementation.
     * <p>
     * Default is not expressed in this feature because the contribution holds a default value.
     */
    public static final String DEFAULT_TRANSIENT_DATA_STORE_CLASS_PROPERTY = "nuxeo.transientdatastore.default.provider.class";

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        var transientService = (TransientDataServiceImpl) Framework.getService(TransientDataService.class);
        transientService.providers.values().forEach(TransientDataStoreProvider::clear);
    }
}
