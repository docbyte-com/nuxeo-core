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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.CORE_SERVICE_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL;

import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * @since 2025.0
 */
public class IgnoreIfNotVCSRepository implements ConditionalIgnore.Condition {

    @Override
    public boolean shouldIgnore() {
        // TODO constant for vcs ?
        return !(CORE_SERVICE_VALUE.equals(STORAGE_SQL) || CORE_SERVICE_VALUE.equals("vcs"));
    }

    @Override
    public boolean needsRuntime() {
        return false;
    }
}
