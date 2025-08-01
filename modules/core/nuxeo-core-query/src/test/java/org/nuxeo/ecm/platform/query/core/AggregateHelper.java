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
package org.nuxeo.ecm.platform.query.core;

import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_AVG;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_COUNT;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MAX;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MISSING;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SUM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_TERMS;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 2025.0
 */
public class AggregateHelper {

    @SuppressWarnings("unchecked")
    public static <A extends Aggregate<? extends Bucket>> A newAggregate(String type, String id, String field) {
        AggregateDefinition def = new AggregateDescriptor();
        def.setType(type);
        def.setId(id);
        def.setDocumentField(field);
        return (A) switch (type) {
            case AGG_TYPE_TERMS -> new AggregateTerm(def, null);
            case AGG_TYPE_RANGE -> new AggregateRange(def, null);
            case AGG_TYPE_DATE_RANGE -> new AggregateDateRange(def, null);
            case AGG_TYPE_HISTOGRAM -> new AggregateHistogram(def, null);
            case AGG_TYPE_DATE_HISTOGRAM -> new AggregateDateHistogram(def, null);
            case AGG_CARDINALITY -> new AggregateCardinality(def, null);
            case AGG_SUM -> new AggregateSum(def, null);
            case AGG_MIN -> new AggregateMin(def, null);
            case AGG_MAX -> new AggregateMax(def, null);
            case AGG_AVG -> new AggregateAvg(def, null);
            case AGG_COUNT -> new AggregateCount(def, null);
            case AGG_MISSING -> new AggregateMissing(def, null);
            default -> throw new IllegalArgumentException("Invalid aggregate type: " + def.getType() + ", " + def);
        };
    }
}
