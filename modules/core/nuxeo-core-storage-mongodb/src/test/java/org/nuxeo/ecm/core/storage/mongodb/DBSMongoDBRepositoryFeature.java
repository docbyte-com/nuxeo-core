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
package org.nuxeo.ecm.core.storage.mongodb;

import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.BaseCoreFeature;
import org.nuxeo.ecm.core.RepositoryFeature;
import org.nuxeo.runtime.migration.MigrationFeature;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.dbs")
@Deploy("org.nuxeo.ecm.core.storage.mongodb")
@Deploy("org.nuxeo.ecm.core.storage.mongodb.test:OSGI-INF/test-dbs-repository-mongodb-contrib.xml")
@Features({ MigrationFeature.class, MongoDBFeature.class, BaseCoreFeature.class })
public class DBSMongoDBRepositoryFeature implements RepositoryFeature {

    private static final Logger log = LogManager.getLogger(DBSMongoDBRepositoryFeature.class);

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        log.info(MARKER_CONSOLE_OVERRIDE, "Deploying a DBS repository using MongoDB");
    }

    @Override
    public boolean supportsMultipleFulltextIndexes() {
        return false;
    }

    @Override
    public boolean supportsFulltextSearch() {
        return true;
    }
}
