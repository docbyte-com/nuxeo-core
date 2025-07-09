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
package org.nuxeo.runtime.ts;

/**
 * Service allowing to get access to {@link TransientDataStore}.
 *
 * @since 2025.8
 */
public interface TransientDataService {

    /**
     * Returns the Transient Data store with the given name, or the default store if not found.
     *
     * @param name the store name
     * @return the sore with the given name, or the default store
     */
    TransientDataStore getStore(String name);
}
