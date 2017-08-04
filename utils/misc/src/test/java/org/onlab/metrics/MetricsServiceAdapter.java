/*
 * Copyright 2015-present Open Networking Foundation
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


import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.Collections;
import java.util.Map;

/**
 * Test adapter for metrics service.
 */
public class MetricsServiceAdapter implements MetricsService {

    @Override
    public MetricsComponent registerComponent(String name) {
        MetricsComponent metricsComponent = new MetricsComponent(name);
        return metricsComponent;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return null;
    }

    @Override
    public Counter createCounter(MetricsComponent component,
                                 MetricsFeature feature, String metricName) {
        return null;
    }

    @Override
    public Histogram createHistogram(MetricsComponent component,
                                     MetricsFeature feature, String metricName) {
        return null;
    }

    @Override
    public Timer createTimer(MetricsComponent component,
                             MetricsFeature feature, String metricName) {
        return null;
    }

    @Override
    public Meter createMeter(MetricsComponent component,
                             MetricsFeature feature, String metricName) {
        return null;
    }

    @Override
    public <T extends Metric> T registerMetric(MetricsComponent component,
                                               MetricsFeature feature, String metricName, T metric) {
        return null;
    }

    @Override
    public void registerReporter(MetricsReporter reporter) {
    }

    @Override
    public void unregisterReporter(MetricsReporter reporter) {
    }

    @Override
    public void notifyReporters() {
    }

    @Override
    public boolean removeMetric(MetricsComponent component,
                                MetricsFeature feature, String metricName) {
        return false;
    }

    @Override
    public Map<String, Timer> getTimers(MetricFilter filter) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Gauge> getGauges(MetricFilter filter) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Counter> getCounters(MetricFilter filter) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Meter> getMeters(MetricFilter filter) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Histogram> getHistograms(MetricFilter filter) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.emptyMap();
    }

    @Override
    public void removeMatching(MetricFilter filter) {
    }
}