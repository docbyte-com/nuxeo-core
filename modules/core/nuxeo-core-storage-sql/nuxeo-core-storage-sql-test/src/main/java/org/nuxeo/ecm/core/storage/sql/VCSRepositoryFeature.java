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
package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_H2;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_POSTGRESQL;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL_DB_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.computeSystemProperty;
import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;
import static org.nuxeo.runtime.datasource.DataSourceFeature.STORAGE_SQL_DB_DB2;
import static org.nuxeo.runtime.datasource.DataSourceFeature.STORAGE_SQL_DB_MYSQL;
import static org.nuxeo.runtime.datasource.DataSourceFeature.STORAGE_SQL_DB_ORACLE;
import static org.nuxeo.runtime.datasource.DataSourceFeature.STORAGE_SQL_DB_SQL_SERVER;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SystemProperty;
import org.nuxeo.ecm.core.BaseCoreFeature;
import org.nuxeo.ecm.core.RepositoryFeature;
import org.nuxeo.runtime.datasource.DataSourceFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.DynamicFeaturesLoader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.sql")
@Features(BaseCoreFeature.class)
public class VCSRepositoryFeature implements RepositoryFeature {

    private static final Logger log = LogManager.getLogger(VCSRepositoryFeature.class);

    // possible values "varchar", "uuid", "sequence"
    public static final SystemProperty ID_TYPE = new SystemProperty("nuxeo.test.vcs.idtype", "varchar");

    public static final String ID_TYPE_VALUE = computeSystemProperty(ID_TYPE);

    // class is package-protected and we have a warning due to protected keyword
    @SuppressWarnings("ClassEscapesDefinedScope")
    protected final Class<? extends VCSAbstractRepositoryFeature> implementationFeatureClass;

    @Inject
    protected DataSourceFeature dataSourceFeature;

    // class is package-protected and we have a warning due to protected keyword
    @SuppressWarnings("ClassEscapesDefinedScope")
    protected VCSAbstractRepositoryFeature implementationFeature;

    public VCSRepositoryFeature(DynamicFeaturesLoader loader) {
        implementationFeatureClass = switch (STORAGE_SQL_DB_VALUE) {
            case STORAGE_SQL_DB_DB2 -> VCSDB2RepositoryFeature.class;
            case STORAGE_SQL_DB_H2 -> VCSH2RepositoryFeature.class;
            case STORAGE_SQL_DB_MYSQL -> VCSMySQLRepositoryFeature.class;
            case STORAGE_SQL_DB_ORACLE -> VCSOracleRepositoryFeature.class;
            case STORAGE_SQL_DB_POSTGRESQL -> VCSPostgreSQLRepositoryFeature.class;
            case STORAGE_SQL_DB_SQL_SERVER -> VCSSQLServerRepositoryFeature.class;
            default ->
                throw new UnsupportedOperationException("SQL DB type: " + STORAGE_SQL_DB_VALUE + " is not supported");
        };
        loader.loadFeature(implementationFeatureClass);
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        implementationFeature = runner.getFeature(implementationFeatureClass);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        log.info(MARKER_CONSOLE_OVERRIDE, "Deploying a VCS repository using {}",
                () -> StringUtils.capitalize(STORAGE_SQL_DB_VALUE.toLowerCase()));
    }

    @Override
    public boolean supportsMultipleFulltextIndexes() {
        return implementationFeature.supportsMultipleFulltextIndexes();
    }

    @Override
    public boolean supportsFulltextSearch() {
        return implementationFeature.supportsFulltextSearch();
    }

    public boolean isDB2() {
        return VCSDB2RepositoryFeature.class.equals(implementationFeatureClass);
    }

    public boolean isH2() {
        return VCSH2RepositoryFeature.class.equals(implementationFeatureClass);
    }

    public boolean isMySQL() {
        return VCSMySQLRepositoryFeature.class.equals(implementationFeatureClass);
    }

    public boolean isOracle() {
        return VCSOracleRepositoryFeature.class.equals(implementationFeatureClass);
    }

    public boolean isPostgreSQL() {
        return VCSPostgreSQLRepositoryFeature.class.equals(implementationFeatureClass);
    }

    public boolean isSQLServer() {
        return VCSSQLServerRepositoryFeature.class.equals(implementationFeatureClass);
    }

    protected void initializeDB() throws Exception {
        dataSourceFeature.initializeDB();
    }

    protected void tearDownDB() throws Exception {
        dataSourceFeature.tearDownDB();
    }

    protected static boolean isDBSupportsSequenceId() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_ORACLE)
                || STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_POSTGRESQL)
                // supported since 11.x - not supported on Azure
                || STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_SQL_SERVER);
    }

    protected static boolean isDBSupportsSoftDelete() {
        return STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_ORACLE)
                || STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_POSTGRESQL)
                || STORAGE_SQL_DB_VALUE.equals(STORAGE_SQL_DB_SQL_SERVER);
    }
}
