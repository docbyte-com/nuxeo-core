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
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_DRIVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_PASSWORD;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_URL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DEPRECATED_USER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.DRIVER;
import static org.nuxeo.runtime.datasource.DataSourceFeature.PASSWORD;
import static org.nuxeo.runtime.datasource.DataSourceFeature.URL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.USER;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
class DataSourceOracleFeature extends AbstractDataSourceFeature {

    static {
        // the Oracle driver can not be in our dependency tree
        computeSystemProperty(DRIVER.withDefault("oracle.jdbc.OracleDriver"), DEPRECATED_DRIVER);
        computeSystemProperty(URL.withDefault("jdbc:oracle:thin:@:localhost:1521:XE"), DEPRECATED_URL);
        computeSystemProperty(USER.withDefault("nuxeo"), DEPRECATED_USER);
        computeSystemProperty(PASSWORD.withDefault("nuxeo"), DEPRECATED_PASSWORD);
    }

    @Override
    protected void initializeDB() throws Exception {
        try (var connection = getConnection()) {
            doOnAllTables(connection, user.toUpperCase(), "DROP TABLE \"%s\" CASCADE CONSTRAINTS PURGE");
            dropSequences(connection);
        }
    }

    protected void dropSequences(Connection connection) throws SQLException {
        List<String> sequenceNames = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT SEQUENCE_NAME FROM USER_SEQUENCES")) {
                while (rs.next()) {
                    String sequenceName = rs.getString(1);
                    if (sequenceName.indexOf('$') != -1) {
                        continue;
                    }
                    sequenceNames.add(sequenceName);
                }
            }
            for (String sequenceName : sequenceNames) {
                executeSql(st, String.format("DROP SEQUENCE \"%s\"", sequenceName));
            }
        }
    }
}
