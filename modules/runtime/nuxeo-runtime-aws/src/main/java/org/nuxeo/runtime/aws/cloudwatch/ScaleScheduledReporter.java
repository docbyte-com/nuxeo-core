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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.AWSConfigurationService;
import org.nuxeo.runtime.aws.NuxeoAWSCredentialsProvider;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Timer;

/**
 * Reports nuxeo.streams.scale.metric as a CloudWatch metric: Nuxeo/Stream Scale, simulating CPU utilization with a
 * target value of 50%.
 */
public class ScaleScheduledReporter extends ScheduledReporter {

    private static final Logger log = LogManager.getLogger(ScaleScheduledReporter.class);

    public static final String METRIC_NAMESPACE = "Nuxeo/Stream";

    public static final String TARGET_METRIC_NAME = "TargetScale";

    public static final String STEP_METRIC_NAME = "StepScale";

    public static final String SCALE_METRIC = "nuxeo.streams.scale.metric";

    public static final String WORKER_COUNT_METRIC = "nuxeo.cluster.worker.count";

    public static final int TARGET_VALUE = 50;

    protected static final MetricFilter SCALE_METRIC_FILTER = (name, metric) -> SCALE_METRIC.equals(name.getKey())
            || WORKER_COUNT_METRIC.equals(name.getKey());

    protected final AmazonCloudWatch cloudWatchClient;

    protected final List<Dimension> dimensions;

    protected final boolean stepMetric;

    protected final boolean targetMetric;

    public ScaleScheduledReporter(String awsConfigurationId, String tag, boolean stepMetric, boolean targetMetric,
            MetricRegistry registry) {
        super(registry, "cloud-watch-reporter", SCALE_METRIC_FILTER, TimeUnit.SECONDS, TimeUnit.SECONDS);
        var credentialsProvider = new NuxeoAWSCredentialsProvider(awsConfigurationId);
        var regionProvider = new NuxeoAWSRegionProvider(awsConfigurationId);
        var clientConfiguration = new ClientConfiguration();
        var confService = Framework.getService(AWSConfigurationService.class);
        confService.configureSSL(clientConfiguration);
        confService.configureProxy(clientConfiguration);
        cloudWatchClient = AmazonCloudWatchClientBuilder.standard()
                                                        .withCredentials(credentialsProvider)
                                                        .withClientConfiguration(clientConfiguration)
                                                        .withRegion(regionProvider.getRegion())
                                                        .build();
        dimensions = getDimensions(tag);
        this.stepMetric = stepMetric;
        this.targetMetric = targetMetric;
        log.debug("CloudWatchClient created");
    }

    protected List<Dimension> getDimensions(String tag) {
        List<Dimension> ret = new ArrayList<>();
        ret.add(new Dimension().withName("Tag").withValue(tag));
        String env = System.getenv("NUXEO_ENVIRONMENT");
        if (!isBlank(env)) {
            ret.add(new Dimension().withName("Environment").withValue(env));
        }
        return ret;
    }

    @Override
    public void report(SortedMap<MetricName, Gauge> gauges, SortedMap<MetricName, Counter> counters,
            SortedMap<MetricName, Histogram> histograms, SortedMap<MetricName, Meter> meters,
            SortedMap<MetricName, Timer> timers) {
        if (gauges.isEmpty()) {
            // only one Worker node is running StreamIntrospectionComputation that produces the gauges for our metrics
            return;
        }
        Integer scale = gauges.entrySet()
                              .stream()
                              .filter(entry -> SCALE_METRIC.equals(entry.getKey().getKey()))
                              .map(entry -> (Integer) entry.getValue().getValue())
                              .findFirst()
                              .orElse(null);
        Integer nodes = gauges.entrySet()
                              .stream()
                              .filter(entry -> WORKER_COUNT_METRIC.equals(entry.getKey().getKey()))
                              .map(entry -> (Integer) entry.getValue().getValue())
                              .findFirst()
                              .orElse(null);
        if (scale == null || nodes == null) {
            return;
        }
        if (stepMetric) {
            Double dValue = (double) scale;
            MetricDatum datum = new MetricDatum().withMetricName(STEP_METRIC_NAME)
                                                 .withUnit(StandardUnit.Count)
                                                 .withDimensions(dimensions)
                                                 .withValue(dValue);
            PutMetricDataRequest request = new PutMetricDataRequest().withNamespace(METRIC_NAMESPACE)
                                                                     .withMetricData(datum);
            cloudWatchClient.putMetricData(request);
            log.debug("Sent stepScale metric: {}, current nodes: {} as cloudwatch {} {}: {}", scale, nodes,
                    METRIC_NAMESPACE, STEP_METRIC_NAME, dValue);
        }
        if (targetMetric) {
            Double dValue = convertToTargetMetric(nodes, scale);
            MetricDatum datum = new MetricDatum().withMetricName(TARGET_METRIC_NAME)
                                                 .withUnit(StandardUnit.Percent)
                                                 .withDimensions(dimensions)
                                                 .withValue(dValue);
            PutMetricDataRequest request = new PutMetricDataRequest().withNamespace(METRIC_NAMESPACE)
                                                                     .withMetricData(datum);
            cloudWatchClient.putMetricData(request);
            log.debug("Sent targetScale metric: {}, current nodes: {} as cloudwatch {} {}: {}", scale, nodes,
                    METRIC_NAMESPACE, TARGET_METRIC_NAME, dValue);
        }

    }

    protected Double convertToTargetMetric(int currentNodes, int diff) {
        if (currentNodes <= 0) {
            log.warn("Number of nodes is unknown, don't scale (nodes: {}, scale: {})", currentNodes, diff);
            return (double) TARGET_VALUE;
        }
        // simulate a total desired CPU utilization, target being 50% per node
        int desiredCpuUtilization = TARGET_VALUE * (currentNodes + diff);
        // compute the current average for the cluster with the current nodes
        return (double) desiredCpuUtilization / currentNodes;
    }

}
