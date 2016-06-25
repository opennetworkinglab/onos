/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.MetricValue;
import org.onosproject.net.DeviceId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.CPU_METRICS;
import static org.onosproject.cpman.ControlResource.DISK_METRICS;
import static org.onosproject.cpman.ControlResource.MEMORY_METRICS;
import static org.onosproject.cpman.ControlResource.NETWORK_METRICS;
import static org.onosproject.cpman.ControlResource.Type;

/**
 * Unit test of control plane monitoring service.
 */
public class ControlPlaneMonitorTest {

    private ControlPlaneMonitor monitor;
    private static final Integer UPDATE_INTERVAL = 1;
    private ClusterService mockClusterService;
    private ControllerNode mockControllerNode;
    private ClusterCommunicationService mockCommunicationService;
    private NodeId nodeId;

    /**
     * Sets up the services required by control plane monitor.
     */
    @Before
    public void setup() {
        monitor = new ControlPlaneMonitor();

        mockCommunicationService = new ClusterCommunicationServiceAdapter();
        monitor.communicationService = mockCommunicationService;

        nodeId = new NodeId("1");
        mockControllerNode = new MockControllerNode(nodeId);
        mockClusterService = createMock(ClusterService.class);
        monitor.clusterService = mockClusterService;

        expect(mockClusterService.getNode(anyObject()))
                .andReturn(mockControllerNode).anyTimes();
        expect(mockClusterService.getLocalNode())
                .andReturn(mockControllerNode).anyTimes();
        replay(mockClusterService);

        monitor.activate();
    }

    /**
     * Mock class for a controller node.
     */
    private static class MockControllerNode implements ControllerNode {
        final NodeId id;

        public MockControllerNode(NodeId id) {
            this.id = id;
        }

        @Override
        public NodeId id() {
            return this.id;
        }

        @Override
        public IpAddress ip() {
            return null;
        }

        @Override
        public int tcpPort() {
            return 0;
        }
    }

    private void testUpdateMetricWithoutId(ControlMetricType cmt, MetricValue mv) {
        ControlMetric cm = new ControlMetric(cmt, mv);
        monitor.updateMetric(cm, UPDATE_INTERVAL, Optional.empty());
    }

    private void testLoadMetric(NodeId nodeId, ControlMetricType cmt, MetricValue mv) {
        try {
            assertThat(monitor.getLoad(nodeId, cmt, Optional.empty()).get().latest(), is(mv.getLoad()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void testUpdateMetricWithResource(ControlMetricType cmt, MetricValue mv, String resourceName) {
        ControlMetric cm = new ControlMetric(cmt, mv);
        monitor.updateMetric(cm, UPDATE_INTERVAL, resourceName);
    }

    private void testLoadMetricWithResource(NodeId nodeId, ControlMetricType cmt, MetricValue mv, String resourceName) {
        try {
            assertThat(monitor.getLoad(nodeId, cmt, resourceName).get().latest(), is(mv.getLoad()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void testUpdateMetricWithId(ControlMetricType cmt, MetricValue mv, DeviceId did) {
        ControlMetric cm = new ControlMetric(cmt, mv);
        monitor.updateMetric(cm, UPDATE_INTERVAL, Optional.of(did));
    }

    private void testLoadMetricWithId(NodeId nodeId, ControlMetricType cmt, MetricValue mv, DeviceId did) {
        try {
            assertThat(monitor.getLoad(nodeId, cmt, Optional.of(did)).get().latest(), is(mv.getLoad()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests cpu metric update and load function.
     */
    @Test
    public void testCpuMetric() {
        MetricValue mv = new MetricValue.Builder().load(30).add();

        CPU_METRICS.forEach(cmt -> testUpdateMetricWithoutId(cmt, mv));
        CPU_METRICS.forEach(cmt -> testLoadMetric(nodeId, cmt, mv));
    }

    /**
     * Tests memory metric update and load function.
     */
    @Test
    public void testMemoryMetric() {
        MetricValue mv = new MetricValue.Builder().load(40).add();

        MEMORY_METRICS.forEach(cmt -> testUpdateMetricWithoutId(cmt, mv));
        MEMORY_METRICS.forEach(cmt -> testLoadMetric(nodeId, cmt, mv));
    }

    /**
     * Tests disk metric update and load function.
     */
    @Test
    public void testDiskMetric() {
        MetricValue mv = new MetricValue.Builder().load(50).add();

        Set<String> set = ImmutableSet.of("disk1", "disk2");

        set.forEach(disk -> DISK_METRICS.forEach(cmt ->
                testUpdateMetricWithResource(cmt, mv, disk)));

        set.forEach(disk -> DISK_METRICS.forEach(cmt ->
                testLoadMetricWithResource(nodeId, cmt, mv, disk)));
    }

    /**
     * Tests network metric update and load function.
     */
    @Test
    public void testNetworkMetric() {
        MetricValue mv = new MetricValue.Builder().load(10).add();

        Set<String> set = ImmutableSet.of("eth0", "eth1");

        set.forEach(network -> NETWORK_METRICS.forEach(cmt ->
                testUpdateMetricWithResource(cmt, mv, network)));

        set.forEach(network -> NETWORK_METRICS.forEach(cmt ->
                testLoadMetricWithResource(nodeId, cmt, mv, network)));
    }

    /**
     * Tests control message update and load function.
     */
    @Test
    public void testControlMessage() {
        MetricValue mv = new MetricValue.Builder().load(10).add();
        Set<DeviceId> set = ImmutableSet.of(DeviceId.deviceId("of:0000000000000001"),
                                            DeviceId.deviceId("of:0000000000000002"));

        set.forEach(devId -> CONTROL_MESSAGE_METRICS.forEach(cmt ->
                testUpdateMetricWithId(cmt, mv, devId)));

        set.forEach(devId -> CONTROL_MESSAGE_METRICS.forEach(cmt ->
                testLoadMetricWithId(nodeId, cmt, mv, devId)));
    }

    /**
     * Tests available resource update and load function.
     */
    @Test
    public void testAvailableResources() {
        MetricValue mv = new MetricValue.Builder().load(50).add();

        Set<String> diskSet = ImmutableSet.of("disk1", "disk2");

        diskSet.forEach(disk -> DISK_METRICS.forEach(cmt ->
                testUpdateMetricWithResource(cmt, mv, disk)));

        Set<String> networkSet = ImmutableSet.of("eth0", "eth1");

        networkSet.forEach(network -> NETWORK_METRICS.forEach(cmt ->
                testUpdateMetricWithResource(cmt, mv, network)));

        assertThat(monitor.availableResourcesSync(nodeId, Type.DISK), is(diskSet));
        assertThat(monitor.availableResourcesSync(nodeId, Type.NETWORK), is(networkSet));
    }
}
