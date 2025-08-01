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

import static org.nuxeo.runtime.datasource.DataSourceFeature.PASSWORD;
import static org.nuxeo.runtime.datasource.DataSourceFeature.URL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.USER;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 2025.0
 */
abstract class AbstractDataSourceFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(AbstractDataSourceFeature.class);

    protected final String url = System.getProperty(URL.key());

    protected final String user = System.getProperty(USER.key());

    protected final String password = System.getProperty(PASSWORD.key());

    @Override
    public final void start(FeaturesRunner runner) throws Exception {
        initializeDB();
    }

    @Override
    public final void stop(FeaturesRunner runner) throws Exception {
        tearDownDB();
    }

    protected void initializeDB() throws Exception {
    }

    protected void tearDownDB() throws Exception {
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Executes one statement on all the tables in a database.
     */
    protected void doOnAllTables(Connection connection, String schemaPattern, String statement) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> tableNames = new LinkedList<>();
        Set<String> truncateFirst = new HashSet<>();
        try (ResultSet rs = metadata.getTables(null, schemaPattern, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName.indexOf('$') != -1) {
                    // skip Oracle 10g flashback/fulltext-index tables
                    continue;
                }
                if (tableName.toLowerCase().startsWith("trace_xe_")) {
                    // Skip mssql 2012 system table
                    continue;
                }
                if ("sys_config".equals(tableName)) {
                    // Skip MySQL system table
                    continue;
                }
                if ("ACLR_USER_USERS".equals(tableName)) {
                    // skip nested table that is dropped by the main table
                    continue;
                }
                if ("ANCESTORS_ANCESTORS".equals(tableName)) {
                    // skip nested table that is dropped by the main table
                    continue;
                }
                if ("ACLR_MODIFIED".equals(tableName) && this instanceof DataSourceOracleFeature) {
                    // global temporary table on Oracle, must TRUNCATE before DROP
                    truncateFirst.add(tableName);
                }
                tableNames.add(tableName);
            }
        }
        // not all databases can cascade on drop
        // remove hierarchy last because of foreign keys
        if (tableNames.remove("HIERARCHY")) {
            tableNames.add("HIERARCHY");
        }
        // needed for Azure
        if (tableNames.remove("NXP_LOGS")) {
            tableNames.add("NXP_LOGS");
        }
        if (tableNames.remove("NXP_LOGS_EXTINFO")) {
            tableNames.add("NXP_LOGS_EXTINFO");
        }
        // PostgreSQL is lowercase
        if (tableNames.remove("hierarchy")) {
            tableNames.add("hierarchy");
        }
        try (Statement st = connection.createStatement()) {
            for (String tableName : tableNames) {
                if (truncateFirst.contains(tableName)) {
                    String sql = String.format("TRUNCATE TABLE \"%s\"", tableName);
                    executeSql(st, sql);
                }
                String sql = String.format(statement, tableName);
                executeSql(st, sql);
            }
        }
    }

    protected void executeSql(String sql) throws SQLException {
        try (Connection connection = getConnection()) {
            executeSql(connection, sql);
        }
    }

    protected void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            executeSql(st, sql);
        }
    }

    protected void executeSql(Statement st, String sql) throws SQLException {
        log.trace("SQL: {}", sql);
        st.execute(sql);
    }
}
