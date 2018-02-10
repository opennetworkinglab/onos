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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class holds the Metrics registry for ONOS.
 * All metrics (Counter, Histogram, Timer, Meter, Gauge) use a hierarchical
 * string-based naming scheme: COMPONENT.FEATURE.NAME.
 * Example: "Topology.Counters.TopologyUpdates".
 * The COMPONENT and FEATURE names have to be registered in advance before
 * a metric can be created. Example:
 * <pre>
 *   <code>
 *     private final MetricsManager.MetricsComponent COMPONENT =
 *         MetricsManager.registerComponent("Topology");
 *     private final MetricsManager.MetricsFeature FEATURE =
 *         COMPONENT.registerFeature("Counters");
 *     private final Counter counterTopologyUpdates =
 *         MetricsManager.createCounter(COMPONENT, FEATURE, "TopologyUpdates");
 *   </code>
 * </pre>
 * Gauges are slightly different because they are not created directly in
 * this class, but are allocated by the caller and passed in for registration:
 * <pre>
 *   <code>
 *     private final Gauge&lt;Long&gt; gauge =
 *         new {@literal Gauge&lt;Long&gt}() {
 *             {@literal @}Override
 *             public Long getValue() {
 *                 return gaugeValue;
 *             }
 *         };
 *     MetricsManager.registerMetric(COMPONENT, FEATURE, GAUGE_NAME, gauge);
 *   </code>
 * </pre>
 */
public class MetricsManager implements MetricsService {

    /**
     * Registry to hold the Components defined in the system.
     */
    private ConcurrentMap<String, MetricsComponent> componentsRegistry =
        new ConcurrentHashMap<>();

    /**
     * Registry for the Metrics objects created in the system.
     */
    private MetricRegistry metricsRegistry = new MetricRegistry();

    /**
     * Reporter for exposing metrics objects to third party persistent system.
     */
    private Set<MetricsReporter> reporters = Sets.newConcurrentHashSet();

    /**
     * Clears the internal state.
     */
    protected void clear() {
        this.componentsRegistry = new ConcurrentHashMap<>();
        this.metricsRegistry = new MetricRegistry();
    }

    /**
     * Registers a component.
     *
     * @param name name of the Component to register
     * @return MetricsComponent object that can be used to create Metrics.
     */
    @Override
    public MetricsComponent registerComponent(final String name) {
        MetricsComponent component = componentsRegistry.get(name);
        if (component == null) {
            final MetricsComponent createdComponent =
                new MetricsComponent(name);
            component = componentsRegistry.putIfAbsent(name, createdComponent);
            if (component == null) {
                component = createdComponent;
            }
        }
        return component;
    }

    /**
     * Fetches existing metric registry.
     *
     * @return metric registry
     */
    @Override
    public MetricRegistry getMetricRegistry() {
        return metricsRegistry;
    }

    /**
     * Generates a name for a Metric from its component and feature.
     *
     * @param component component the metric is defined in
     * @param feature feature the metric is defined in
     * @param metricName local name of the metric
     *
     * @return full name of the metric
     */
    private String generateName(final MetricsComponent component,
                                final MetricsFeature feature,
                                final String metricName) {
        return MetricRegistry.name(component.getName(),
                                   feature.getName(),
                                   metricName);
    }

    /**
     * Creates a Counter metric.
     *
     * @param component component the Counter is defined in
     * @param feature feature the Counter is defined in
     * @param metricName local name of the metric
     * @return the created Counter Meteric
     */
    @Override
    public Counter createCounter(final MetricsComponent component,
                                 final MetricsFeature feature,
                                 final String metricName) {
        final String name = generateName(component, feature, metricName);
        return metricsRegistry.counter(name);
    }

    /**
     * Creates a Histogram metric.
     *
     * @param component component the Histogram is defined in
     * @param feature feature the Histogram is defined in
     * @param metricName local name of the metric
     * @return the created Histogram Metric
     */
    @Override
    public Histogram createHistogram(final MetricsComponent component,
                                     final MetricsFeature feature,
                                     final String metricName) {
        final String name = generateName(component, feature, metricName);
        return metricsRegistry.histogram(name);
    }

    /**
     * Creates a Timer metric.
     *
     * @param component component the Timer is defined in
     * @param feature feature the Timer is defined in
     * @param metricName local name of the metric
     * @return the created Timer Metric
     */
    @Override
    public Timer createTimer(final MetricsComponent component,
                             final MetricsFeature feature,
                             final String metricName) {
        final String name = generateName(component, feature, metricName);
        Timer timer = metricsRegistry.getTimers().get(name);
        if (timer != null) {
            return timer;
        }

        timer = new Timer(new SlidingWindowReservoir(1028));
        try {
            return metricsRegistry.register(name, timer);
        } catch (IllegalArgumentException e) {
            return metricsRegistry.timer(name);
        }
    }

    /**
     * Creates a Meter metric.
     *
     * @param component component the Meter is defined in
     * @param feature feature the Meter is defined in
     * @param metricName local name of the metric
     * @return the created Meter Metric
     */
    @Override
    public Meter createMeter(final MetricsComponent component,
                             final MetricsFeature feature,
                             final String metricName) {
        final String name = generateName(component, feature, metricName);
        return metricsRegistry.meter(name);
    }

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
    @Override
    public <T extends Metric> T registerMetric(
                                        final MetricsComponent component,
                                        final MetricsFeature feature,
                                        final String metricName,
                                        final T metric) {
        final String name = generateName(component, feature, metricName);
        metricsRegistry.register(name, metric);
        return metric;
    }

    /**
     * Registers a reporter to receive any changes on metric registry.
     *
     * @param reporter metric reporter
     */
    @Override
    public void registerReporter(MetricsReporter reporter) {
        reporters.add(reporter);
    }

    /**
     * Unregisters the given metric reporter.
     *
     * @param reporter metric reporter
     */
    @Override
    public void unregisterReporter(MetricsReporter reporter) {
        reporters.remove(reporter);
    }

    /**
     * Notifies the changes on metric registry to all registered reporters.
     */
    @Override
    public void notifyReporters() {
        reporters.forEach(MetricsReporter::notifyMetricsChange);
    }

    /**
     * Removes the metric with the given name.
     *
     * @param component component the Metric is defined in
     * @param feature feature the Metric is defined in
     * @param metricName local name of the metric
     * @return true if the metric existed and was removed, otherwise false
     */
    @Override
    public boolean removeMetric(final MetricsComponent component,
                                final MetricsFeature feature,
                                final String metricName) {
        final String name = generateName(component, feature, metricName);
        return metricsRegistry.remove(name);
    }

    /**
     * Fetches the existing Timers.
     *
     * @param filter filter to use to select Timers
     * @return a map of the Timers that match the filter, with the key as the
     *         name String to the Timer.
     */
    @Override
    public Map<String, Timer> getTimers(final MetricFilter filter) {
        return metricsRegistry.getTimers(filter);
    }

    /**
     * Fetches the existing Gauges.
     *
     * @param filter filter to use to select Gauges
     * @return a map of the Gauges that match the filter, with the key as the
     *         name String to the Gauge.
     */
    @Override
    public Map<String, Gauge> getGauges(final MetricFilter filter) {
        return metricsRegistry.getGauges(filter);
    }

    /**
     * Fetches the existing Counters.
     *
     * @param filter filter to use to select Counters
     * @return a map of the Counters that match the filter, with the key as the
     *         name String to the Counter.
     */
    @Override
    public Map<String, Counter> getCounters(final MetricFilter filter) {
        return metricsRegistry.getCounters(filter);
    }

    /**
     * Fetches the existing Meters.
     *
     * @param filter filter to use to select Meters
     * @return a map of the Meters that match the filter, with the key as the
     *         name String to the Meter.
     */
    @Override
    public Map<String, Meter> getMeters(final MetricFilter filter) {
        return metricsRegistry.getMeters(filter);
    }

    /**
     * Fetches the existing Histograms.
     *
     * @param filter filter to use to select Histograms
     * @return a map of the Histograms that match the filter, with the key as
     *         the name String to the Histogram.
     */
    @Override
    public Map<String, Histogram> getHistograms(final MetricFilter filter) {
        return metricsRegistry.getHistograms(filter);
    }

    /**
     * Removes all Metrics that match a given filter.
     *
     * @param filter filter to use to select the Metrics to remove.
     */
    @Override
    public void removeMatching(final MetricFilter filter) {
        metricsRegistry.removeMatching(filter);
    }

    /**
     * Fetches the existing Meters.
     *
     *
     * @return a map of all metrics with the key as the
     *         name String to the Meter.
     */
    public Map<String, Metric> getMetrics() {
        return metricsRegistry.getMetrics();
    }
}
