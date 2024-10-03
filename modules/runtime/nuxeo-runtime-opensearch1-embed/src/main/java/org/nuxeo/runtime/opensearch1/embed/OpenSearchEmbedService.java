
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
package org.nuxeo.runtime.opensearch1.embed;

/**
 * Service used to get an embedded server url.
 *
 * @since 2025.0
 */
public interface OpenSearchEmbedService {

    /**
     * Gets the opensearch server url for an embedded node id.
     *
     * @param id the embedded server id
     * @return the server url
     * @throws IllegalArgumentException if embedded server with given {@code id} is not found
     * @throws IllegalStateException if embedded server is not started
     */
    String getServerUrl(String id);
}
