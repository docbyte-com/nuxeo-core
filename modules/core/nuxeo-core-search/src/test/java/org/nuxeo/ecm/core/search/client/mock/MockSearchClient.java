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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search.client.mock;

import org.nuxeo.ecm.core.search.AbstractSearchClient;
import org.nuxeo.ecm.core.search.SearchClientDescriptor;

/**
 * @since 2025.0
 */
public class MockSearchClient extends AbstractSearchClient {

    public MockSearchClient(SearchClientDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case INDEXING -> true;
        };
    }

    @Override
    public boolean createIndexIfNotExists(String name, String repository, String settings, String mapping) {
        return false;
    }

    @Override
    public void dropIndex(String name) {

    }

    @Override
    public void dropAndInitIndex(String indexName) {

    }

    @Override
    public void close() {
        // nothing to do
    }
}
