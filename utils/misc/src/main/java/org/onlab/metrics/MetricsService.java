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

import java.util.Map;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Metrics Service to collect metrics.
 */
public interface MetricsService {

    /**
     * Registers a component.
     *
     * @param name name of the Component to register
     * @return MetricsComponent object that can be used to create Metrics.
     */
    MetricsComponent registerComponent(String name);

    /**
     * Fetches existing metric registry.
     *
     * @return metric registry
     */
    MetricRegistry getMetricRegistry();

    /**
     * Creates a Counter metric.
     *
     * @param component component the Counter is defined in
     * @param feature feature the Counter is defined in
     * @param metricName local name of the metric
     * @return the created Counter Meteric
     */
    Counter createCounter(MetricsComponent component,
            MetricsFeature feature,
            String metricName);

    /**
     * Creates a Histogram metric.
     *
     * @param component component the Histogram is defined in
     * @param feature feature the Histogram is defined in
     * @param metricName local name of the metric
     * @return the created Histogram Metric
     */
    Histogram createHistogram(MetricsComponent component,
            MetricsFeature feature,
            String metricName);

    /**
     * Creates a Timer metric.
     *
     * @param component component the Timer is defined in
     * @param feature feature the Timer is defined in
     * @param metricName local name of the metric
     * @return the created Timer Metric
     */
    Timer createTimer(MetricsComponent component,
            MetricsFeature feature,
            String metricName);

    /**
     * Creates a Meter metric.
     *
     * @param component component the Meter is defined in
     * @param feature feature the Meter is defined in
     * @param metricName local name of the metric
     * @return the created Meter Metric
     */
    Meter createMeter(MetricsComponent component,
            MetricsFeature feature,
            String metricName);

    /**
     * Registers an already created Metric.  This is used for situation where a
     * caller needs to allocate its own Metric, but still register it with the
     * system.
     *
     * @param <T> Metric type
     * @param component component the Metric is defined in
     * @param feature feature the Metric is defined in
     * @param metricName local name of the metric
     * @param metric Metric to register
     * @return the registered Metric
     */
     <T extends Metric> T registerMetric(
             MetricsComponent component,
             MetricsFeature feature,
             String metricName,
             T metric);

    /**
     * Registers a reporter to receive any changes on metric registry.
     *
     * @param reporter metric reporter
     */
    void registerReporter(MetricsReporter reporter);

    /**
     * Unregisters the given metric reporter.
     *
     * @param reporter metric reporter
     */
    void unregisterReporter(MetricsReporter reporter);

    /**
     * Notifies the changes on metric registry to all registered reporters.
     */
    void notifyReporters();

    /**
     * Removes the metric with the given name.
     *
     * @param component component the Metric is defined in
     * @param feature feature the Metric is defined in
     * @param metricName local name of the metric
     * @return true if the metric existed and was removed, otherwise false
     */
    boolean removeMetric(MetricsComponent component,
                         MetricsFeature feature,
                         String metricName);

    /**
     * Fetches the existing Timers.
     *
     * @param filter filter to use to select Timers
     * @return a map of the Timers that match the filter, with the key as the
     *         name String to the Timer.
     */
     Map<String, Timer> getTimers(MetricFilter filter);

    /**
     * Fetches the existing Gauges.
     *
     * @param filter filter to use to select Gauges
     * @return a map of the Gauges that match the filter, with the key as the
     *         name String to the Gauge.
     */
     Map<String, Gauge> getGauges(MetricFilter filter);

    /**
     * Fetches the existing Counters.
     *
     * @param filter filter to use to select Counters
     * @return a map of the Counters that match the filter, with the key as the
     *         name String to the Counter.
     */
     Map<String, Counter> getCounters(MetricFilter filter);

    /**
     * Fetches the existing Meters.
     *
     * @param filter filter to use to select Meters
     * @return a map of the Meters that match the filter, with the key as the
     *         name String to the Meter.
     */
     Map<String, Meter> getMeters(MetricFilter filter);

    /**
     * Fetches the existing Histograms.
     *
     * @param filter filter to use to select Histograms
     * @return a map of the Histograms that match the filter, with the key as
     *         the name String to the Histogram.
     */
     Map<String, Histogram> getHistograms(MetricFilter filter);

    /**
     * Fetches the existing metrics.
     *
     * @return a map of the Metrics, with the key as
     *         the name String to the Histogram.
     */
    Map<String, Metric> getMetrics();

    /**
     * Removes all Metrics that match a given filter.
     *
     * @param filter filter to use to select the Metrics to remove.
     */
     void removeMatching(MetricFilter filter);
}
