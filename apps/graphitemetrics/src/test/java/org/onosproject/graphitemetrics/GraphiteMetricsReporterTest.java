/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.graphitemetrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreServiceAdapter;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for metrics reporter of graphite.
 */
public class GraphiteMetricsReporterTest {

    private GraphiteMetricsReporter gmr;

    private static final String METRIC_NAME1 = "consistentMap.onos-app-ids.putIfAbsent";
    private static final String METRIC_NAME2 = "consistentMap.onos-hosts.entrySet";
    private static final String METRIC_NAME3 = "clusterCommunication.endpoint.*";
    private static final String METRIC_NAME4 = "atomicCounter.onos-app-id-counter.*";

    private static final String PREFIXES1 = "consistentMap";
    private static final String PREFIXES2 = "topology";
    private static final String PREFIXES3 = "consistentMap.onos-app-ids";
    private static final String PREFIXES4 = "consistentMap, clusterCommunication, atomicCounter";

    /**
     * Sets up graphite metrics reporter instance.
     */
    @Before
    public void setUp() {
        gmr = new GraphiteMetricsReporter();
        gmr.coreService = new CoreServiceAdapter();
        gmr.cfgService = new ComponentConfigAdapter();
        gmr.metricsService = new TestMetricsService();
        gmr.activate();
    }

    /**
     * Tears down graphite metrics reporter instance.
     */
    public void tearDown() {
        gmr.deactivate();
    }

    /**
     * Tests whether the containsName method can always return the correct result
     * with the given metric name and a set of prefixes.
     */
    @Test
    public void testContainsName() {
        assertTrue(gmr.containsName(METRIC_NAME1, PREFIXES1));
        assertTrue(gmr.containsName(METRIC_NAME1, PREFIXES3));
        assertTrue(gmr.containsName(METRIC_NAME1, PREFIXES4));
        assertTrue(gmr.containsName(METRIC_NAME2, PREFIXES4));
        assertTrue(gmr.containsName(METRIC_NAME3, PREFIXES4));
        assertTrue(gmr.containsName(METRIC_NAME4, PREFIXES4));
        assertFalse(gmr.containsName(METRIC_NAME1, PREFIXES2));
    }

    /**
     * Tests whether the filter method can always return the correct result.
     */
    @Test
    public void testFilter() {
        MetricRegistry filtered;
        MetricRegistry full = new MetricRegistry();
        full.meter(METRIC_NAME1);
        full.meter(METRIC_NAME2);
        full.meter(METRIC_NAME3);
        full.meter(METRIC_NAME4);

        gmr.monitorAll = true;
        filtered = gmr.filter(full);

        assertTrue(filtered.getNames()
                .containsAll(ImmutableSet.of(METRIC_NAME1, METRIC_NAME2,
                                             METRIC_NAME3, METRIC_NAME4)));

        gmr.monitorAll = false;
        gmr.metricNames = PREFIXES1;
        filtered = gmr.filter(full);

        assertTrue(filtered.getNames()
                .containsAll(ImmutableSet.of(METRIC_NAME1, METRIC_NAME2)));
        assertFalse(filtered.getNames()
                .containsAll(ImmutableSet.of(METRIC_NAME3, METRIC_NAME4)));

        gmr.metricNames = PREFIXES2;
        filtered = gmr.filter(full);

        assertFalse(filtered.getNames().containsAll(ImmutableSet.of(METRIC_NAME1)));

        gmr.metricNames = PREFIXES3;
        filtered = gmr.filter(full);

        assertTrue(filtered.getNames().containsAll(ImmutableSet.of(METRIC_NAME1)));
        assertFalse(filtered.getNames().containsAll(ImmutableSet.of(METRIC_NAME2)));

        gmr.metricNames = PREFIXES4;
        filtered = gmr.filter(full);

        assertTrue(filtered.getNames()
                .containsAll(ImmutableSet.of(METRIC_NAME1, METRIC_NAME2,
                        METRIC_NAME3, METRIC_NAME4)));
    }

    /**
     * Tests whether the methods of MetricsService receives null value parameter.
     */
    private class TestMetricsService implements MetricsService {

        @Override
        public MetricsComponent registerComponent(String name) {
            assertNotNull("Component name is null.", name);
            return null;
        }

        @Override
        public MetricRegistry getMetricRegistry() {
            return null;
        }

        @Override
        public Counter createCounter(MetricsComponent component,
                                     MetricsFeature feature, String metricName) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            return null;
        }

        @Override
        public Histogram createHistogram(MetricsComponent component,
                                         MetricsFeature feature, String metricName) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            return null;
        }

        @Override
        public Timer createTimer(MetricsComponent component,
                                 MetricsFeature feature, String metricName) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            return null;
        }

        @Override
        public Meter createMeter(MetricsComponent component,
                                 MetricsFeature feature, String metricName) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            return null;
        }

        @Override
        public <T extends Metric> T registerMetric(MetricsComponent component,
                                                   MetricsFeature feature, String metricName, T metric) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            assertNotNull("Metric is null.", metric);
            return null;
        }

        @Override
        public boolean removeMetric(MetricsComponent component,
                                    MetricsFeature feature, String metricName) {
            assertNotNull("MetricsComponent is null.", component);
            assertNotNull("MetricsFeature is null.", feature);
            assertNotNull("Metric name is null.", metricName);
            return false;
        }

        @Override
        public Map<String, Timer> getTimers(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
            return null;
        }

        @Override
        public Map<String, Gauge> getGauges(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
            return null;
        }

        @Override
        public Map<String, Counter> getCounters(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
            return null;
        }

        @Override
        public Map<String, Meter> getMeters(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
            return null;
        }

        @Override
        public Map<String, Histogram> getHistograms(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
            return null;
        }

        @Override
        public Map<String, Metric> getMetrics() {
            return null;
        }

        @Override
        public void removeMatching(MetricFilter filter) {
            assertNotNull("MetricFilter is null.", filter);
        }
    }
}
