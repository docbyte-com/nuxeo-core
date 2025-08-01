/*
 * (C) Copyright 2020-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.video;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.nuxeo.ecm.platform.video.service.VideoConversionWork.CATEGORY_VIDEO_CONVERSION;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.filemanager.FileManagerFeature;
import org.nuxeo.ecm.platform.picture.test.ImagingFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * since 11.4
 */
@Deploy("org.nuxeo.ecm.platform.video")
@Deploy("org.nuxeo.ecm.platform.video:disable-picture-migration.xml")
// contribution to deactivate the unwanted works: fulltextExtractor, fulltextUpdater and videoConversion
@Deploy("org.nuxeo.ecm.platform.video:test-video-workmanager-config.xml")
@Features({ CoreFeature.class, ImagingFeature.class, FileManagerFeature.class })
public class VideoFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        // picture views generation for video is made of two dependant async processes, the generation is done as below:
        // - sync listener org.nuxeo.ecm.platform.video.listener.VideoChangedListener which reset picture views
        // if the main blob has changed
        // - async work org.nuxeo.ecm.platform.video.service.VideoInfoWork which computes video info and storyboard, and
        // then triggers video conversions
        // - async work org.nuxeo.ecm.platform.video.service.VideoConversionWork which generates the picture views
        // so we need to first wait for the VideoInfoWork to finish and then wait for the VideoConversionWorks to finish
        runner.getFeature(TransactionalFeature.class).addWaiter(duration -> {
            var workManager = Framework.getService(WorkManager.class);
            long begin = System.currentTimeMillis();
            if (workManager.awaitCompletion(duration.toMillis(), MILLISECONDS)) {
                var leftDuration = duration.minusMillis(System.currentTimeMillis() - begin);
                return workManager.awaitCompletion(CATEGORY_VIDEO_CONVERSION, leftDuration.toMillis(), MILLISECONDS);
            }
            // work manager consumed all the permitted duration
            return false;
        });
    }
}
