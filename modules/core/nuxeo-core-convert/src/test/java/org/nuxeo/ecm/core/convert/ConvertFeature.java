/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.core.convert;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 6.0
 */
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.mimetype")
@Deploy("org.nuxeo.ecm.core.convert.tests:OSGI-INF/convert-service-default-test-config.xml")
@Features(RuntimeFeature.class)
public class ConvertFeature implements RunnerFeature {

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        // delete cache entry and cache directory
        ConversionCacheHolder.deleteCache();
        // ensure cache directory exists
        Files.createDirectories(Path.of(ConversionServiceImpl.getCacheBasePath()));
    }
}
