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
package org.onosproject.cpman.rest;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.SystemInfo;
import org.onosproject.cpman.impl.SystemInfoFactory;
import org.onosproject.net.DeviceId;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
public class ControlMetricsCollectorResourceTest extends JerseyTest {

    final ControlPlaneMonitorService mockControlPlaneMonitorService =
                                     createMock(ControlPlaneMonitorService.class);

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
                        .add(ControlPlaneMonitorService.class, mockControlPlaneMonitorService);
        BaseResource.setServiceDirectory(testDirectory);
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
}
