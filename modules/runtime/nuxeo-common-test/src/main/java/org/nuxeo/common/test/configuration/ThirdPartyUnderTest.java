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
package org.nuxeo.common.test.configuration;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.stream.Stream;

/**
 * Helper class to retrieve current ongoing test configuration regarding Third Party services.
 * 
 * @since 2025.0
 */
public final class ThirdPartyUnderTest {

    // Third Party service possibilities

    public static final String STORAGE_ELASTICSEARCH_7 = "elasticsearch7";

    public static final String STORAGE_ELASTICSEARCH_8 = "elasticsearch8";

    public static final String STORAGE_MONGODB = "mongodb";

    public static final String STORAGE_OPENSEARCH_1 = "opensearch1";

    public static final String STORAGE_SQL = "sql";

    public static final String STREAM_KAFKA = "kafka";

    public static final String STREAM_MEM = "mem";

    // System properties declaration

    public static final SystemProperty CORE_SERVICE_PROPERTY = new SystemProperty("nuxeo.test.core", STORAGE_SQL);

    public static final SystemProperty DIRECTORY_SERVICE_PROPERTY = new SystemProperty("nuxeo.test.directory",
            STORAGE_SQL);

    public static final SystemProperty STREAM_SERVICE_PROPERTY = new SystemProperty("nuxeo.test.stream", STREAM_MEM);

    // Effective property values

    public static final String CORE_SERVICE_VALUE = computeSystemProperty(CORE_SERVICE_PROPERTY);

    public static final String DIRECTORY_SERVICE_VALUE = computeSystemProperty(DIRECTORY_SERVICE_PROPERTY,
            CORE_SERVICE_PROPERTY);

    public static final String STREAM_SERVICE_VALUE = computeSystemProperty(STREAM_SERVICE_PROPERTY);

    // Third Party specific configuration properties

    public static final SystemProperty STORAGE_MONGODB_SERVER_PROPERTY = new SystemProperty("nuxeo.test.mongodb.server",
            "localhost:27017");

    public static final SystemProperty STORAGE_MONGODB_DBNAME_PROPERTY = new SystemProperty("nuxeo.test.mongodb.dbname",
            "unittests");

    public static final SystemProperty STORAGE_OPENSEARCH_1_SERVERS_PROPERTY = new SystemProperty(
            "nuxeo.test.opensearch1.servers", "http://localhost:9200");

    public static final SystemProperty STREAM_KAFKA_SERVERS_PROPERTY = new SystemProperty("nuxeo.test.kafka.servers",
            "localhost:9092");

    // Third Party specific configuration values

    public static final String STORAGE_MONGODB_SERVER_VALUE = computeSystemProperty(STORAGE_MONGODB_SERVER_PROPERTY);

    public static final String STORAGE_MONGODB_DBNAME_VALUE = computeSystemProperty(STORAGE_MONGODB_DBNAME_PROPERTY);

    public static final String STORAGE_OPENSEARCH_1_SERVERS_VALUE = computeSystemProperty(
            STORAGE_OPENSEARCH_1_SERVERS_PROPERTY,
            // fallback on deprecated property
            new SystemProperty("nuxeo.test.elasticsearch.addressList", "http://localhost:9200"));

    public static String computeSystemProperty(String key, String defaultValue) {
        return computeSystemProperty(new SystemProperty(key, defaultValue));
    }

    public static String computeSystemProperty(SystemProperty property, SystemProperty... dependencies) {
        String key = property.key();
        String value = System.getProperty(key);
        if (isBlank(value) || value.equals("${" + key + "}")) {
            value = Stream.of(dependencies)
                          .filter(SystemProperty::isConfigured)
                          .findFirst()
                          .orElse(property)
                          .defaultValue();
            System.setProperty(key, value);
        }
        return value;
    }

    private ThirdPartyUnderTest() {
    }

    public record SystemProperty(String key, String defaultValue, boolean isConfigured) {
        public SystemProperty(String key, String defaultValue) {
            this(key, defaultValue, System.getProperties().containsKey(key));
        }
    }
}
