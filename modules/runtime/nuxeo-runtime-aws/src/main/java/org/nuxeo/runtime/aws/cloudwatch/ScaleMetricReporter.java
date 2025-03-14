/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.aws.cloudwatch;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;

/**
 * A Metric reporter that exposes nuxeo.streams.scale.metric as a CloudWatch metric in order to be used by AWS
 * autoscaler.
 * 
 * @since 2023.28
 */
public class ScaleMetricReporter extends AbstractMetricsReporter {
    private static final Logger log = LogManager.getLogger(ScaleMetricReporter.class);

    public static final String AWS_CONFIGURATION_ID_KEY = "awsConfigurationId";

    public static final String AWS_TAG_KEY = "awsTag";

    public static final String METRIC_TARGET_KEY = "targetMetric";

    public static final String METRIC_STEP_KEY = "stepMetric";

    protected ScheduledReporter reporter;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        String awsConfigurationId = defaultIfBlank(getOption(AWS_CONFIGURATION_ID_KEY, null), null);
        log.debug("Reporting CloudWatch metric using '{}' aws configuration", awsConfigurationId);
        String tag = getOption(AWS_TAG_KEY, null);
        if (isEmpty(tag)) {
            log.error("Missing required cloudwatch scale tag: {} option, CloudWatch scale metric will not be reported.",
                    AWS_TAG_KEY);
            return;
        }
        boolean stepMetric = getOptionAsBoolean(METRIC_STEP_KEY, true);
        boolean targetMetric = getOptionAsBoolean(METRIC_TARGET_KEY, true);
        if (!stepMetric && !targetMetric) {
            log.error("At least one metric must be enabled: {} or {}, CloudWatch scale metric will not be reported.",
                    METRIC_STEP_KEY, METRIC_TARGET_KEY);
            return;
        }
        reporter = new ScaleScheduledReporter(awsConfigurationId, tag, stepMetric, targetMetric, registry);
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (reporter != null) {
            reporter.stop();
        }
    }
}
