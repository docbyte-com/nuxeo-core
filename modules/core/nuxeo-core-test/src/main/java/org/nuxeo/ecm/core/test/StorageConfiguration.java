/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.common.test.configuration.ThirdPartyUnderTest;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SystemProperty;
import org.nuxeo.ecm.core.RepositoryFeature;
import org.nuxeo.ecm.core.storage.mem.DBSMemRepositoryFeature;
import org.nuxeo.ecm.core.storage.mongodb.DBSMongoDBRepositoryFeature;
import org.nuxeo.ecm.core.storage.sql.VCSRepositoryFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Description of the specific capabilities of a repository for tests, and helper methods.
 *
 * @since 7.3
 */
public class StorageConfiguration {

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#CORE_SERVICE_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String CORE_PROPERTY = ThirdPartyUnderTest.CORE_SERVICE_PROPERTY.key();

    public static final String CORE_VCS = "vcs";

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MEM} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String CORE_MEM = ThirdPartyUnderTest.STORAGE_MEM;

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String CORE_MONGODB = ThirdPartyUnderTest.STORAGE_MONGODB;

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_SQL} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_CORE = CORE_VCS;

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_SERVER_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_MONGODB_SERVER = ThirdPartyUnderTest.STORAGE_MONGODB_SERVER_PROPERTY.defaultValue();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_DBNAME_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_MONGODB_DBNAME = ThirdPartyUnderTest.STORAGE_MONGODB_DBNAME_PROPERTY.defaultValue();

    protected final RepositoryFeature feature;

    public StorageConfiguration(RepositoryFeature feature) {
        this.feature = feature;
    }

    /**
     * @deprecated since 2025.0, use
     *             {@link ThirdPartyUnderTest#computeSystemProperty(SystemProperty, SystemProperty...)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static String defaultSystemProperty(String name, String def) {
        return ThirdPartyUnderTest.computeSystemProperty(new SystemProperty(name, def));
    }

    public boolean isVCS() {
        return feature instanceof VCSRepositoryFeature;
    }

    public boolean isVCSH2() {
        return feature instanceof VCSRepositoryFeature vcsRepositoryFeature && vcsRepositoryFeature.isH2();
    }

    public boolean isVCSPostgreSQL() {
        return feature instanceof VCSRepositoryFeature vcsRepositoryFeature && vcsRepositoryFeature.isPostgreSQL();
    }

    public boolean isVCSMySQL() {
        return feature instanceof VCSRepositoryFeature vcsRepositoryFeature && vcsRepositoryFeature.isMySQL();
    }

    public boolean isVCSOracle() {
        return feature instanceof VCSRepositoryFeature vcsRepositoryFeature && vcsRepositoryFeature.isOracle();
    }

    public boolean isVCSSQLServer() {
        return feature instanceof VCSRepositoryFeature vcsRepositoryFeature && vcsRepositoryFeature.isSQLServer();
    }

    public boolean isDBS() {
        return feature instanceof DBSMemRepositoryFeature || feature instanceof DBSMongoDBRepositoryFeature;
    }

    public boolean isDBSMem() {
        return feature instanceof DBSMemRepositoryFeature;
    }

    public boolean isDBSMongoDB() {
        return feature instanceof DBSMongoDBRepositoryFeature;
    }

    public String getRepositoryName() {
        return "test";
    }

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     *
     * @deprecated since 2025.0, use {@link TransactionalFeature#nextTransaction()} instead, it now does nothing
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public void sleepForFulltext() {
    }

    /**
     * Sleep a bit to get to the next millisecond, to have different timestamps.
     */
    public void maybeSleepToNextSecond() {
        try {
            Thread.sleep(1); // 1 millisecond
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated since 2025.0, use {@link TransactionalFeature#nextTransaction()} instead, it now does nothing
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public void waitForAsyncCompletion() {
    }

    /**
     * @deprecated since 2025.0, use {@link TransactionalFeature#nextTransaction()} instead, it now does nothing
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public void waitForFulltextIndexing() {
    }

    /**
     * Checks if the database supports multiple fulltext indexes.
     */
    public boolean supportsMultipleFulltextIndexes() {
        return feature.supportsMultipleFulltextIndexes();
    }

    /**
     * Checks if the database supports fulltext search.
     *
     * @since 11.1
     */
    public boolean supportsFulltextSearch() {
        return feature.supportsFulltextSearch();
    }

    public boolean isChangeTokenEnabled() {
        return feature.isChangeTokenEnabled();
    }

    /**
     * @since 9.2
     */
    public String getCoreType() {
        String coreServiceValue = ThirdPartyUnderTest.CORE_SERVICE_VALUE;
        if (ThirdPartyUnderTest.STORAGE_SQL.equals(coreServiceValue)) {
            return CORE_VCS;
        }
        return coreServiceValue;
    }

}
