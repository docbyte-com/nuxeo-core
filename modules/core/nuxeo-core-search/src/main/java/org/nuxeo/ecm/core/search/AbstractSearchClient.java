/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
public abstract class AbstractSearchClient implements SearchClient {

    protected final String name;

    public AbstractSearchClient(Descriptor descriptor) {
        this.name = descriptor.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Capability> getCapabilities() {
        return Arrays.stream(Capability.values()).filter(this::hasCapability).collect(Collectors.toSet());
    }
}
