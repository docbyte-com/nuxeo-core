/*
 * (C) Copyright 2017-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.runtime.mongodb;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB_DBNAME_PROPERTY;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB_DBNAME_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB_SERVER_PROPERTY;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB_SERVER_VALUE;

import org.nuxeo.common.test.configuration.ThirdPartyUnderTest;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.mongodb.client.MongoClient;

/**
 * @since 9.1
 */
@Deploy("org.nuxeo.runtime.mongodb")
@Deploy("org.nuxeo.runtime.mongodb.test")
@Features(RuntimeFeature.class)
@ConditionalIgnore(condition = IgnoreNoMongoDB.class, cause = "Needs a MongoDB server!")
public class MongoDBFeature implements RunnerFeature {

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_SERVER_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String MONGODB_SERVER_PROPERTY = STORAGE_MONGODB_SERVER_PROPERTY.key();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_DBNAME_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String MONGODB_DBNAME_PROPERTY = STORAGE_MONGODB_DBNAME_PROPERTY.key();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_SERVER_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_MONGODB_SERVER = STORAGE_MONGODB_SERVER_PROPERTY.defaultValue();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STORAGE_MONGODB_DBNAME_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_MONGODB_DBNAME = STORAGE_MONGODB_DBNAME_PROPERTY.defaultValue();

    @Override
    public void start(FeaturesRunner runner) {
        try (MongoClient client = MongoDBConnectionHelper.newMongoClient(STORAGE_MONGODB_SERVER_VALUE)) {
            MongoDBConnectionHelper.getDatabase(client, STORAGE_MONGODB_DBNAME_VALUE).drop();
        }
    }

}
