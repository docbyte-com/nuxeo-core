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
import static org.nuxeo.runtime.datasource.DataSourceFeature.DATABASE_VALUE;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(DataSourceFeature.class)
public class TestDataSourceAvailability {

    @Test
    public void testDataSourceMetadata() throws SQLException {
        try (var connection = ConnectionHelper.getConnection(DATABASE_VALUE)) {
            var metadata = connection.getMetaData();
            assertNotNull(metadata);
        }
    }
}
