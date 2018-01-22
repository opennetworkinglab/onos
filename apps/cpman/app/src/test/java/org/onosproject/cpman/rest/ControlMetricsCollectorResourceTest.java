/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.rest;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsReporter;
import org.onlab.metrics.MetricsService;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.SystemInfo;
import org.onosproject.cpman.impl.SystemInfoFactory;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test for ControlMetricsCollector.
 */
public class ControlMetricsCollectorResourceTest extends ResourceTest {

    final ControlPlaneMonitorService mockControlPlaneMonitorService =
                                     createMock(ControlPlaneMonitorService.class);
    final MetricsService mockMetricsService = new MockMetricsService();

    private static final String PREFIX = "collector";

    /**
     * Constructs a control metrics collector resource test instance.
     */
    public ControlMetricsCollectorResourceTest() {
        super(ResourceConfig.forApplicationClass(CPManWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(ControlPlaneMonitorService.class, mockControlPlaneMonitorService)
                        .add(MetricsService.class, mockMetricsService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests CPU metrics POST through REST API.
     */
    @Test
    public void testCpuMetricsPost() {
        mockControlPlaneMonitorService.updateMetric(anyObject(), anyInt(),
                (Optional<DeviceId>) anyObject());
        expectLastCall().times(5);
        replay(mockControlPlaneMonitorService);

        basePostTest("cpu-metrics-post.json", PREFIX + "/cpu_metrics");
    }

    /**
     * Tests memory metrics POST through REST API.
     */
    @Test
    public void testMemoryMetricsPost() {
        mockControlPlaneMonitorService.updateMetric(anyObject(), anyInt(),
                (Optional<DeviceId>) anyObject());
        expectLastCall().times(4);
        replay(mockControlPlaneMonitorService);

        basePostTest("memory-metrics-post.json", PREFIX + "/memory_metrics");
    }

    /**
     * Tests disk metrics POST through REST API.
     */
    @Test
    public void testDiskMetrics() {
        mockControlPlaneMonitorService.updateMetric(anyObject(), anyInt(), anyString());
        expectLastCall().times(4);
        replay(mockControlPlaneMonitorService);

        basePostTest("disk-metrics-post.json", PREFIX + "/disk_metrics");
    }

    /**
     * Tests network metrics POST through REST API.
     */
    @Test
    public void testNetworkMetrics() {
        mockControlPlaneMonitorService.updateMetric(anyObject(), anyInt(), anyString());
        expectLastCall().times(8);
        replay(mockControlPlaneMonitorService);

        basePostTest("network-metrics-post.json", PREFIX + "/network_metrics");
    }

    @Test
    public void testSystemInfoPost() {
        basePostTest("system-info-post.json", PREFIX + "/system_info");

        SystemInfo si = SystemInfoFactory.getInstance().getSystemInfo();
        assertThat(si.cpuSpeed(), is(2048));
        assertThat(si.coreCount(), is(6));
        assertThat(si.cpuCount(), is(2));
        assertThat(si.totalMemory(), is(4096));
    }

    private Response baseTest(String jsonFile, String path) {
        final WebTarget wt = target();
        InputStream jsonStream = ControlMetricsCollectorResourceTest.class
                .getResourceAsStream(jsonFile);

        assertThat(jsonStream, notNullValue());

        return wt.path(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
    }

    private void basePostTest(String jsonFile, String path) {
        Response response = baseTest(jsonFile, path);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    private class MockMetricsService implements MetricsService {

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
        public Counter createCounter(MetricsComponent component, MetricsFeature feature,
                                     String metricName) {
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
            return new Meter();
        }

        @Override
        public <T extends Metric> T registerMetric(MetricsComponent component,
                                                   MetricsFeature feature,
                                                   String metricName, T metric) {
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
}
