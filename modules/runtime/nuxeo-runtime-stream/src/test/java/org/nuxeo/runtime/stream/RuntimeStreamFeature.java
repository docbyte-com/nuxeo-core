/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.stream;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STREAM_KAFKA_SERVERS_PROPERTY;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STREAM_SERVICE_VALUE;
import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * The runtime stream feature provides an In-Memory or Kafka stream implementation depending on test configuration.
 * <p>
 * To run your unit tests on a Memory or Kafka you need to declare {@code nuxeo.test.stream} to either
 * {@link ThirdPartyUnderTest#STREAM_MEM mem} or {@link ThirdPartyUnderTest#STREAM_KAFKA kafka} in your system
 * properties.
 * <p>
 *
 * @since 10.3
 */
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.runtime.stream.test")
@Features(RuntimeFeature.class)
public class RuntimeStreamFeature implements RunnerFeature {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RuntimeStreamFeature.class);

    public static final String BUNDLE_TEST_NAME = "org.nuxeo.runtime.stream.test";

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STREAM_SERVICE_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String STREAM_PROPERTY = ThirdPartyUnderTest.STREAM_SERVICE_PROPERTY.key();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STREAM_MEM} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String STREAM_MEM = ThirdPartyUnderTest.STREAM_MEM;

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STREAM_KAFKA} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String STREAM_KAFKA = ThirdPartyUnderTest.STREAM_KAFKA;

    // kafka properties part

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STREAM_KAFKA_SERVERS_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String KAFKA_SERVERS_PROPERTY = STREAM_KAFKA_SERVERS_PROPERTY.key();

    /**
     * @deprecated since 2025.0, use {@link ThirdPartyUnderTest#STREAM_KAFKA_SERVERS_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String KAFKA_SERVERS_DEFAULT = STREAM_KAFKA_SERVERS_PROPERTY.defaultValue();

    @Override
    public void start(FeaturesRunner runner) {
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        try {
            log.info(MARKER_CONSOLE_OVERRIDE, "Deploying Nuxeo Stream using {}",
                    () -> StringUtils.capitalize(STREAM_SERVICE_VALUE.toLowerCase()));
            switch (STREAM_SERVICE_VALUE) {
                case ThirdPartyUnderTest.STREAM_MEM -> initMem(harness);
                case ThirdPartyUnderTest.STREAM_KAFKA -> initKafka(harness);
                default ->
                    throw new UnsupportedOperationException(STREAM_SERVICE_VALUE + " stream type is not supported");
            }
        } catch (Exception e) {
            throw new RuntimeServiceException("Unable to configure the stream implementation", e);
        }
    }

    protected void initMem(RuntimeHarness harness) throws Exception {
        log.debug("Deploy Mem config");
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-stream-mem-contrib.xml");
    }

    protected void initKafka(RuntimeHarness harness) throws Exception {
        // no need to re-init kafka as we use a random prefix
        log.debug("Deploy Kafka config");
        ThirdPartyUnderTest.computeSystemProperty(STREAM_KAFKA_SERVERS_PROPERTY);
        // deploy component
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-stream-kafka-contrib.xml");
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        log.debug("Cleaning Streams");
        StreamService service = Framework.getService(StreamService.class);
        service.stopProcessors();
        LogManager manager = service.getLogManager();
        if (ThirdPartyUnderTest.STREAM_KAFKA.equals(STREAM_SERVICE_VALUE)) {
            // deleting records is much lighter for Kafka
            manager.listAllNames().forEach(manager::deleteRecords);
            try {
                manager.deleteConsumers();
            } catch (RuntimeException e) {
                // ignore failure if group is seen as not empty
                log.warn("Fail to delete consumers: {}", e::getMessage);
            }
        } else {
            manager.listAllNames().forEach(manager::delete);
        }
    }
}
