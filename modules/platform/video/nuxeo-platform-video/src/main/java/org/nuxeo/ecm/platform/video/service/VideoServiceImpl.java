/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.video.service;

import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.platform.video.service.Configuration.DEFAULT_CONFIGURATION;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConversionStatus;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Default implementation of {@link VideoService}.
 *
 * @since 5.5
 */
public class VideoServiceImpl extends DefaultComponent implements VideoService {

    private static final Logger log = LogManager.getLogger(VideoServiceImpl.class);

    public static final String VIDEO_CONVERSIONS_EP = "videoConversions";

    public static final String DEFAULT_VIDEO_CONVERSIONS_EP = "automaticVideoConversions";

    /**
     * @since 7.4
     */
    public static final String CONFIGURATION_EP = "configuration";

    /**
     * @since 7.4
     */
    protected Configuration configuration = DEFAULT_CONFIGURATION;

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        WorkManager workManager = Framework.getService(WorkManager.class);
        if (workManager != null && workManager.isStarted()) {
            try {
                workManager.shutdownQueue(workManager.getCategoryQueueId(VideoConversionWork.CATEGORY_VIDEO_CONVERSION),
                        10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(CONFIGURATION_EP)) {
            configuration = (Configuration) contribution;
        } else {
            register(extensionPoint, (Descriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(CONFIGURATION_EP)) {
            configuration = DEFAULT_CONFIGURATION;
        } else {
            unregister(extensionPoint, (Descriptor) contribution);
        }
    }

    @Override
    public Collection<VideoConversion> getAvailableVideoConversions() {
        return this.<VideoConversion> getDescriptors(VIDEO_CONVERSIONS_EP)
                   .stream()
                   .filter(VideoConversion::isEnabled)
                   .collect(Collectors.toList());
    }

    @Override
    public List<String> getAvailableVideoConversionsNames() {
        return getAvailableVideoConversions().stream().map(VideoConversion::getName).collect(toList());
    }

    @Override
    public List<String> getAutomaticVideoConversionsNames() {
        return this.<AutomaticVideoConversion> getDescriptors(DEFAULT_VIDEO_CONVERSIONS_EP)
            .stream()
            .filter(AutomaticVideoConversion::isEnabled)
            .map(AutomaticVideoConversion::getName)
            .toList();
    }

    @Override
    public void launchConversion(DocumentModel doc, String conversionName) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        VideoConversionWork work = new VideoConversionWork(doc.getRepositoryName(), doc.getId(), conversionName);
        log.debug("Scheduling work: {} conversion of Video document: {}.", conversionName, doc);
        workManager.schedule(work, true);
    }

    @Override
    public void launchAutomaticConversions(DocumentModel doc, boolean onlyMissing) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        this.<AutomaticVideoConversion> getDescriptors(DEFAULT_VIDEO_CONVERSIONS_EP)
            .stream()
            .filter(AutomaticVideoConversion::isEnabled)
            .sorted()
            .forEach(c -> {
                if (!onlyMissing || videoDocument.getTranscodedVideo(c.getName()) == null) {
                    launchConversion(doc, c.getName());
                }
            });
    }

    @Override
    public TranscodedVideo convert(Video originalVideo, String conversionName) {
        var conversion = getVideoConversion(conversionName);
        if (conversion == null) {
            throw new NuxeoException(String.format("'%s' is not a registered video conversion.", conversionName));
        }
        BlobHolder blobHolder = new SimpleBlobHolder(originalVideo.getBlob());
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("height", conversion.getHeight());
        parameters.put("videoInfo", originalVideo.getVideoInfo());
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = conversionService.convert(conversion.getConverter(), blobHolder, parameters);
        VideoInfo videoInfo = VideoHelper.getVideoInfo(result.getBlob());
        return TranscodedVideo.fromBlobAndInfo(conversionName, result.getBlob(), videoInfo);
    }

    @Override
    public VideoConversionStatus getProgressStatus(String repositoryName, String docId, String conversionName) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        Work work = new VideoConversionWork(repositoryName, docId, conversionName);
        State state = workManager.getWorkState(work.getId());
        if (state == null) { // DONE
            return null;
        } else if (state == State.SCHEDULED) {
            String queueId = workManager.getCategoryQueueId(VideoConversionWork.CATEGORY_VIDEO_CONVERSION);
            long queueSize = workManager.getMetrics(queueId).getScheduled().longValue();
            return new VideoConversionStatus(VideoConversionStatus.STATUS_CONVERSION_QUEUED, 0L, queueSize);
        } else { // RUNNING
            return new VideoConversionStatus(VideoConversionStatus.STATUS_CONVERSION_PENDING, 0L, 0L);
        }
    }

    @Override
    public VideoConversion getVideoConversion(String conversionName) {
        return getAvailableVideoConversions().stream()
                                             .filter(c -> Objects.equals(c.getId(), conversionName))
                                             .findFirst()
                                             .orElse(null);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}
