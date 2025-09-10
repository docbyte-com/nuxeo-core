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
package org.nuxeo.audit.opensearch1;

import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
@XObject("backend")
public class OpenSearchAuditBackendDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@clientId")
    protected String clientId;

    @XNode("@indexName")
    protected String indexName;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean iEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    /**
     * @return the configured {@code clientId} or "audit/{@code name}" if null
     */
    public String getClientId() {
        return Objects.requireNonNullElseGet(clientId, () -> "audit/" + name);
    }

    /**
     * @return the configured {@link #indexName} or "audit" if null
     */
    public String getIndexName() {
        return Objects.requireNonNullElse(indexName, "audit");
    }

}
