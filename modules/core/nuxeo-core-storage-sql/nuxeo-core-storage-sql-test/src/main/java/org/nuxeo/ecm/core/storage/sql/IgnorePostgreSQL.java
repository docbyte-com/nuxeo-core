/*
 * (C) Copyright 2020-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_POSTGRESQL;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_VALUE;

import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

/**
 * @since 11.1
 * @deprecated since 2025.0, use {@link org.nuxeo.runtime.datasource.IgnoreIfPostgreSQL} instead
 */
@Deprecated(since = "2025.0", forRemoval = true)
public class IgnorePostgreSQL implements ConditionalIgnoreRule.Condition {

    @Override
    public boolean shouldIgnore() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_POSTGRESQL);
    }
}
