/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import jakarta.inject.Inject;

import org.nuxeo.ecm.core.scroll.TestRepositoryScroll;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.0
 */
@Features(CoreSearchFeature.class)
public class TestSearchScroller extends TestRepositoryScroll {

    @Inject
    protected CoreSearchFeature searchFeature;

    @Override
    public String getScrollName() {
        if (searchFeature.hasRepositoryClient()) {
            return "repository_search";
        }
        return "search";
    }
}
