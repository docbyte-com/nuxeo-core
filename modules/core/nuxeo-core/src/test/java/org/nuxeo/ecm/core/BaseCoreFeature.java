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
package org.nuxeo.ecm.core;

import org.nuxeo.ecm.core.api.CoreApiFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.event.CoreEventFeature;
import org.nuxeo.ecm.core.schema.CoreSchemaFeature;
import org.nuxeo.ecm.core.work.WorkManagerFeature;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.platform.el")
@Features({ //
        ClusterFeature.class, //
        CoreApiFeature.class, //
        CoreBulkFeature.class, //
        CoreEventFeature.class, //
        CoreSchemaFeature.class, //
        RuntimeStreamFeature.class, //
        WorkManagerFeature.class //
})
public class BaseCoreFeature implements RunnerFeature {
}
