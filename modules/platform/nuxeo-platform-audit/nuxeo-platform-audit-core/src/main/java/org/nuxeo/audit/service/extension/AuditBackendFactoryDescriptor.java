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
package org.nuxeo.audit.service.extension;

import org.nuxeo.audit.service.AuditBackendFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor to contribute an {@link AuditBackendFactory}.
 *
 * @since 2025.0
 */
@XObject("backend")
public class AuditBackendFactoryDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@factory")
    protected Class<? extends AuditBackendFactory<?>> factory;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Class<? extends AuditBackendFactory<?>> getFactory() {
        return factory;
    }
}
