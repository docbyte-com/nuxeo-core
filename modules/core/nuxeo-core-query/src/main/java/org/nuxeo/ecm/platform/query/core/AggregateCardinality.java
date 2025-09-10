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
package org.nuxeo.ecm.platform.query.core;

import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;

/**
 * @since 2025.0
 */
public class AggregateCardinality extends AggregateBase<BucketDouble> {

    public AggregateCardinality(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
        if (!AGG_CARDINALITY.equals(definition.getType())) {
            throw new IllegalArgumentException("Invalid definition type:" + definition);
        }
    }
}
