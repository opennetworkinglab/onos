/*
 * Copyright 2015-2016 Open Networking Laboratory
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
 */
package org.onosproject.cpman.impl;

import com.codahale.metrics.Meter;
import org.apache.commons.lang3.StringUtils;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.net.DeviceId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An aggregator that aggregates a specific network or performance metrics via the metrics service.
 */
public class MetricsAggregator {

    private Meter rateMeter;
    private Meter countMeter;
    private MetricsService metricsService;
    private MetricsComponent metricsComponent;
    private MetricsFeature metricsFeature;
    private ControlMetricType metricsType;
    private static final int EXECUTE_PERIOD_IN_SECOND = 60;
    private static final String RATE_NAME = "rate";
    private static final String COUNT_NAME = "count";

    /**
     * Constructs a new metrics aggregator for aggregating a metric.
     * Instantiates the metrics service
     * Initializes all the general metrics for that object
     *
     * @param metricsService metric service reference
     * @param type           control metric type
     * @param deviceId       device identification
     */
    MetricsAggregator(MetricsService metricsService, ControlMetricType type,
                      Optional<DeviceId> deviceId) {
        init(metricsService, type, deviceId, null);
    }

    /**
     * Constructs a new metrics aggregator for aggregating a metric.
     * Instantiates the metrics service
     * Initializes all the general metrics for that object
     *
     * @param metricsService metric service reference
     * @param type           control metric type
     * @param resourceName   resource name (e.g., ethernet interface name)
     */
    MetricsAggregator(MetricsService metricsService, ControlMetricType type,
                      String resourceName) {
        init(metricsService, type, Optional.ofNullable(null), resourceName);

    }

    /**
     * Constructs a new metrics aggregator for aggregating a metric.
     * Instantiates the metrics service
     * Initializes all the general metrics for that object
     *
     * @param metricsService metrics service reference
     * @param type           control metric type
     */
    MetricsAggregator(MetricsService metricsService, ControlMetricType type) {
        init(metricsService, type, Optional.ofNullable(null), null);
    }

    /**
     * Base method of the constructor of this class.
     *
     * @param metricsService metrics service reference
     * @param type           control metric type
     * @param deviceId       device identification
     * @param resourceName   resource name
     */
    private void init(MetricsService metricsService, ControlMetricType type,
                      Optional<DeviceId> deviceId, String resourceName) {
        String primitiveName = type.toString();
        String objName = "all";
        if (deviceId.isPresent()) {
            objName = deviceId.toString();
        }

        if (StringUtils.isNotEmpty(resourceName)) {
            objName = resourceName;
        }

        checkNotNull(primitiveName, "Component name cannot be null");
        checkNotNull(objName, "Feature name cannot be null");

        this.metricsType = type;

        this.metricsService = metricsService;
        this.metricsComponent = metricsService.registerComponent(primitiveName);
        this.metricsFeature = metricsComponent.registerFeature(objName);

        this.rateMeter = metricsService.createMeter(metricsComponent, metricsFeature, RATE_NAME);
        this.countMeter = metricsService.createMeter(metricsComponent, metricsFeature, COUNT_NAME);
    }

    /**
     * Returns control metrics type.
     *
     * @return control metrics type
     */
    public ControlMetricType getMetricsType() {
        return metricsType;
    }

    /**
     * Increments the meter rate by n, and the meter counter by 1.
     *
     * @param n increment rate.
     */
    public void increment(long n) {
        rateMeter.mark(n);
        countMeter.mark(1);
    }

    /**
     * Returns the average load value.
     *
     * @return load value
     */
    public long getLoad() {
        return (long) rateMeter.getOneMinuteRate() / (long) countMeter.getOneMinuteRate();
    }

    /**
     * Returns the average meter rate within recent 1 minute.
     *
     * @return rate value
     */
    public long getRate() {
        return (long) rateMeter.getOneMinuteRate();
    }

    /**
     * Returns the average meter count within recent 1 minute.
     *
     * @return count value
     */
    public long getCount() {
        return (long) countMeter.getOneMinuteRate() * EXECUTE_PERIOD_IN_SECOND;
    }
}