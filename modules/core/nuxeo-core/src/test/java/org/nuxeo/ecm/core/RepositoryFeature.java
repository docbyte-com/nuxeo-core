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
package org.nuxeo.ecm.core;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.computeSystemProperty;

import org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SystemProperty;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 2025.0
 */
public interface RepositoryFeature extends RunnerFeature {

    SystemProperty CHANGE_TOKEN_ENABLED = new SystemProperty("nuxeo.test.changetoken.enabled", "true");

    @SuppressWarnings("unused") // we need to declare it in order to compute it from System properties
    String CHANGE_TOKEN_VALUE = computeSystemProperty(CHANGE_TOKEN_ENABLED);

    boolean supportsMultipleFulltextIndexes();

    boolean supportsFulltextSearch();

    default boolean isChangeTokenEnabled() {
        // allows to change it with WithFrameworkProperty
        return Framework.isBooleanPropertyTrue(CHANGE_TOKEN_ENABLED.key());
    }
}
