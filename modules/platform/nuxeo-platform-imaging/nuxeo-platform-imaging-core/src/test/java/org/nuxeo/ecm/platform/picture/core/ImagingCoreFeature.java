/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.picture.core;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.ACTION_NAME;

import org.nuxeo.binary.metadata.test.BinaryMetadataFeature;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.platform.filemanager.FileManagerFeature;
import org.nuxeo.ecm.platform.preview.PreviewCoreFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * since 2025.0
 */
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
// tags relies on it and not available when Mongodb
@Deploy("org.nuxeo.ecm.core.storage.sql:OSGI-INF/querymaker-service.xml")
@Deploy("org.nuxeo.ecm.platform.picture.core.tests:OSGI-INF/empty-picture-configuration-contrib.xml")
@Features({ //
        AutomationFeature.class, //
        BinaryMetadataFeature.class, //
        FileManagerFeature.class, //
        PreviewCoreFeature.class, //
        RenditionFeature.class })
public class ImagingCoreFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        // picture views generation is made of two dependant async processes, the generation is done as below:
        // - sync listener org.nuxeo.ecm.platform.picture.listener.PictureChangedListener which prefill picture views
        // if the main blob has changed
        // - async listener org.nuxeo.ecm.platform.picture.listener.PictureViewsGenerationListener which checks if
        // picture views have to be computed, and if so it triggers the recomputeViews Bulk Action
        // - async Bulk Action org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction
        // so we need to first wait for the work to finish and then wait for the bulk action to finish
        var coreBulkFeature = runner.getFeature(CoreBulkFeature.class);
        coreBulkFeature.addBulkCommandWaiterForListener(runner, ACTION_NAME, DOCUMENT_CREATED);
        coreBulkFeature.addBulkCommandWaiterForListener(runner, ACTION_NAME, DOCUMENT_UPDATED);
    }
}
