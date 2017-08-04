/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;

/**
 * Metric measurements for events.
 */
public class EventMetric {
    private static final String GAUGE_TIMESTAMP_NAME = "Timestamp.EpochMs";
    private static final String METER_RATE_NAME = "Rate";

    private final MetricsService metricsService;
    private final String componentName;
    private final String featureName;

    private MetricsComponent metricsComponent;
    private MetricsFeature metricsFeature;

    private volatile long lastEventTimestampEpochMs = 0;
    private Gauge<Long> lastEventTimestampGauge;
    private Meter eventRateMeter;

    /**
     * Constructor.
     *
     * @param metricsService the Metrics Service to use for Metrics
     * registration and deregistration
     * @param componentName the Metrics Component Name to use for Metrics
     * registration and deregistration
     * @param featureName the Metrics Feature Name to use for Metrics
     * registration and deregistration
     */
    public EventMetric(MetricsService metricsService, String componentName,
                       String featureName) {
        this.metricsService = metricsService;
        this.componentName = componentName;
        this.featureName = featureName;
    }

    /**
     * Registers the metrics.
     */
    public void registerMetrics() {
        metricsComponent = metricsService.registerComponent(componentName);
        metricsFeature = metricsComponent.registerFeature(featureName);

        lastEventTimestampEpochMs = 0;
        lastEventTimestampGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeature,
                                          GAUGE_TIMESTAMP_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return lastEventTimestampEpochMs;
                                              }
                                          });

        eventRateMeter = metricsService.createMeter(metricsComponent,
                                                    metricsFeature,
                                                    METER_RATE_NAME);
    }

    /**
     * Removes the metrics.
     */
    public void removeMetrics() {
        lastEventTimestampEpochMs = 0;
        metricsService.removeMetric(metricsComponent,
                                    metricsFeature,
                                    GAUGE_TIMESTAMP_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeature,
                                    METER_RATE_NAME);
    }

    /**
     * Updates the metric measurements for a single event.
     */
    public void eventReceived() {
        lastEventTimestampEpochMs = System.currentTimeMillis();
        eventRateMeter.mark(1);
    }

    /**
     * Gets the last event timestamp Gauge (ms from the Epoch).
     *
     * @return the last event timestamp Gauge (ms from the Epoch)
     */
    public Gauge<Long> lastEventTimestampGauge() {
        return lastEventTimestampGauge;
    }

    /**
     * Gets the event rate meter.
     *
     * @return the event rate meter
     */
    public Meter eventRateMeter() {
        return eventRateMeter;
    }
}
