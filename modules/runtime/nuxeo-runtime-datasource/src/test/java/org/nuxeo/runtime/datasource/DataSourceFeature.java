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

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_H2;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_POSTGRESQL;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.computeSystemProperty;

import org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SystemProperty;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.DynamicFeaturesLoader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.runtime.datasource.test:OSGI-INF/test-datasource-contrib.xml")
@Features(RuntimeFeature.class)
public class DataSourceFeature implements RunnerFeature {

    public static final String STORAGE_SQL_DB_DB2 = "DB2";

    public static final String STORAGE_SQL_DB_MYSQL = "MySQL";

    public static final String STORAGE_SQL_DB_ORACLE = "Oracle";

    public static final String STORAGE_SQL_DB_SQL_SERVER = "SQLServer";

    public static final SystemProperty DATABASE = new SystemProperty("nuxeo.test.sql.database", "nuxeojunittests");

    public static final SystemProperty DRIVER = new SystemProperty("nuxeo.test.sql.driver");

    public static final SystemProperty SERVER = new SystemProperty("nuxeo.test.sql.server", "localhost");

    public static final SystemProperty PORT = new SystemProperty("nuxeo.test.sql.port");

    public static final SystemProperty URL = new SystemProperty("nuxeo.test.sql.url");

    public static final SystemProperty USER = new SystemProperty("nuxeo.test.sql.user");

    public static final SystemProperty PASSWORD = new SystemProperty("nuxeo.test.sql.password");

    /** @deprecated since 2025.0, use {@link #DATABASE} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_DATABASE = new SystemProperty("nuxeo.test.vcs.database");

    /** @deprecated since 2025.0, use {@link #DRIVER} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_DRIVER = new SystemProperty("nuxeo.test.vcs.driver");

    /** @deprecated since 2025.0, use {@link #SERVER} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_SERVER = new SystemProperty("nuxeo.test.vcs.server");

    /** @deprecated since 2025.0, use {@link #PORT} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_PORT = new SystemProperty("nuxeo.test.vcs.port");

    /** @deprecated since 2025.0, use {@link #URL} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_URL = new SystemProperty("nuxeo.test.vcs.url");

    /** @deprecated since 2025.0, use {@link #USER} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_USER = new SystemProperty("nuxeo.test.vcs.user");

    /** @deprecated since 2025.0, use {@link #PASSWORD} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2025.0", forRemoval = true)
    protected static final SystemProperty DEPRECATED_PASSWORD = new SystemProperty("nuxeo.test.vcs.password");

    public static final String DATABASE_VALUE = computeSystemProperty(DATABASE, DEPRECATED_DATABASE);

    // class is package-protected and we have a warning due to protected keyword
    @SuppressWarnings("ClassEscapesDefinedScope")
    protected final Class<? extends AbstractDataSourceFeature> implementationFeatureClass;

    // class is package-protected and we have a warning due to protected keyword
    @SuppressWarnings("ClassEscapesDefinedScope")
    protected AbstractDataSourceFeature implementationFeature;

    public DataSourceFeature(DynamicFeaturesLoader loader) {
        implementationFeatureClass = switch (STORAGE_SQL_DB_VALUE) {
            case STORAGE_SQL_DB_DB2 -> DataSourceDB2Feature.class;
            case STORAGE_SQL_DB_H2 -> DataSourceH2Feature.class;
            case STORAGE_SQL_DB_MYSQL -> DataSourceMySQLFeature.class;
            case STORAGE_SQL_DB_ORACLE -> DataSourceOracleFeature.class;
            case STORAGE_SQL_DB_POSTGRESQL -> DataSourcePostgreSQLFeature.class;
            case STORAGE_SQL_DB_SQL_SERVER -> DataSourceSQLServerFeature.class;
            default ->
                throw new UnsupportedOperationException("SQL DB type: " + STORAGE_SQL_DB_VALUE + " is not supported");
        };
        loader.loadFeature(implementationFeatureClass);
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        // check that mandatory properties have been defined by sub feature
        assertSystemPropertyIsDefined(DRIVER);
        assertSystemPropertyIsDefined(URL);
        assertSystemPropertyIsDefined(USER);

        implementationFeature = runner.getFeature(implementationFeatureClass);
    }

    private static void assertSystemPropertyIsDefined(SystemProperty property) {
        assertNotNull(String.format("The system property: %s is not defined", property.key()),
                System.getProperty(property.key()));
    }

    public boolean isDB2() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_DB2);
    }

    public boolean isH2() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_H2);
    }

    public boolean isPostgreSQL() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_POSTGRESQL);
    }

    public boolean isMySQL() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_MYSQL);
    }

    public boolean isOracle() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_ORACLE);
    }

    public boolean isSQLServer() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_SQL_SERVER);
    }

    public void initializeDB() throws Exception {
        implementationFeature.initializeDB();
    }

    public void tearDownDB() throws Exception {
        implementationFeature.tearDownDB();
    }
}
