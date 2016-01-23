/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cpman;

import com.codahale.metrics.Meter;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
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
     * Constructs a new MetricsAggregator for aggregating a metric.
     * Instantiates the metrics service
     * Initializes all the general metrics for that object
     *
     * @param metricsService MetricsService reference
     * @param type           Control metric type
     * @param deviceId       DeviceId
     */
    MetricsAggregator(MetricsService metricsService, ControlMetricType type, Optional<DeviceId> deviceId) {
        String primitiveName = type.toString();
        String objName = "all";
        if (deviceId.isPresent()) {
            objName = deviceId.toString();
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

    public ControlMetricType getMetricsType() {
        return metricsType;
    }

    /**
     * Removes both rate and count metrics.
     */
    protected void removeMetrics() {
        metricsService.removeMetric(metricsComponent, metricsFeature, RATE_NAME);
        metricsService.removeMetric(metricsComponent, metricsFeature, COUNT_NAME);
    }

    /**
     * Increments the meter rate by {@code n}, and the meter counter by 1.
     *
     * @param n Increment the meter rate by {@code n}.
     */
    public void increment(long n) {
        rateMeter.mark(n);
        countMeter.mark(1);
    }

    /**
     * Obtains the average load value.
     *
     * @return load value
     */
    public double getLoad() {
        return rateMeter.getOneMinuteRate() / countMeter.getOneMinuteRate();
    }

    /**
     * Obtains the average meter rate within recent 1 minute.
     *
     * @return rate value
     */
    public double getRate() {
        return rateMeter.getOneMinuteRate();
    }

    /**
     * Obtains the average meter count within recent 1 minute.
     *
     * @return count value
     */
    public double getCount() {
        return countMeter.getOneMinuteRate() * EXECUTE_PERIOD_IN_SECOND;
    }
}