/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.mongodb.ts;

import static org.nuxeo.runtime.ts.RuntimeTransientDataStoreFeature.DEFAULT_TRANSIENT_DATA_STORE_CLASS_PROPERTY;

import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.ts.AbstractTransientDataStoreTest;

/**
 * @since 2025.8
 */
@Features(MongoDBFeature.class)
@WithFrameworkProperty(name = DEFAULT_TRANSIENT_DATA_STORE_CLASS_PROPERTY, value = "org.nuxeo.ecm.core.mongodb.ts.MongoDBTransientDataStore")
public class MongoDBTransientDataStoreTest extends AbstractTransientDataStoreTest<MongoDBTransientDataStore> {

    @Override
    protected boolean hasSlowTTLExpiration() {
        return true;
    }
}
