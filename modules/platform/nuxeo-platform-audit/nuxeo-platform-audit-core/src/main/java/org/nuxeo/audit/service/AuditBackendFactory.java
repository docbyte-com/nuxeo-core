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
package org.nuxeo.audit.service;

import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentStartOrders;

/**
 * Factory allowing to retrieve an {@link AuditBackend}.
 *
 * @since 2025.0
 */
public interface AuditBackendFactory<B extends AuditBackend> extends Component {

    @Override
    default int getApplicationStartedOrder() {
        // start just before the AuditService
        return ComponentStartOrders.AUDIT - 1;
    }

    /**
     * @return the {@link AuditBackend} with the given {@code name}
     */
    B getAuditBackend(String name);
}
