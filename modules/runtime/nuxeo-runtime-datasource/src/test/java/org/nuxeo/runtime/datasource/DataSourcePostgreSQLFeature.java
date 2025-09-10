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
package org.nuxeo.runtime.datasource;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.computeSystemProperty;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DATABASE_VALUE;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_DRIVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_PASSWORD;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_PORT;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_SERVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_URL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_USER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DRIVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.PASSWORD;
import static org.nuxeo.runtime.datasource.DataSourceFeature.PORT;
import static org.nuxeo.runtime.datasource.DataSourceFeature.SERVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.URL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.USER;

import org.postgresql.Driver;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
class DataSourcePostgreSQLFeature extends AbstractDataSourceFeature {

    static {
        computeSystemProperty(DRIVER.withDefault(Driver.class.getName()), DEPRECATED_DRIVER);
        String server = computeSystemProperty(SERVER, DEPRECATED_SERVER);
        String port = computeSystemProperty(PORT.withDefault("5432"), DEPRECATED_PORT);
        computeSystemProperty(
                URL.withDefault(String.format("jdbc:postgresql://%s:%s/%s", server, port, DATABASE_VALUE)),
                DEPRECATED_URL);
        computeSystemProperty(USER.withDefault("nuxeo"), DEPRECATED_USER);
        computeSystemProperty(PASSWORD.withDefault("nuxeo"), DEPRECATED_PASSWORD);
    }

    @Override
    protected void initializeDB() throws Exception {
        try (var connection = getConnection()) {
            doOnAllTables(connection, "public", "DROP TABLE \"%s\" CASCADE");
            executeSql(connection, "DROP SEQUENCE IF EXISTS hierarchy_seq");
        }
    }
}
