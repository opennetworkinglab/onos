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

package org.onosproject.incubator.net.virtual.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.TestStorageService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkTopologyService.
 */
public class VirtualNetworkTopologyServiceTest extends TestDeviceParams {

    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestableIntentService intentService = new FakeIntentManager();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkTopologyServiceTest.TestCoreService();
        virtualNetworkManagerStore.setCoreService(coreService);
        TestUtils.setField(coreService, "coreService", new VirtualNetworkTopologyServiceTest.TestCoreService());
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.intentService = intentService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Method to create the virtual network for further testing.
     *
     * @return virtual network
     */
    private VirtualNetwork setupVirtualNetworkTopology() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        VirtualDevice virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), DID3);
        VirtualDevice virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), DID4);

        ConnectPoint cp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), cp1.deviceId(), cp1.port(),
                                  new DefaultPort(virtualDevice1, cp1.port(), true));

        ConnectPoint cp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), cp2.deviceId(), cp2.port(),
                                  new DefaultPort(virtualDevice1, cp2.port(), true));

        ConnectPoint cp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        manager.createVirtualPort(virtualNetwork.id(), cp3.deviceId(), cp3.port(),
                                  new DefaultPort(virtualDevice2, cp3.port(), true));

        ConnectPoint cp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        manager.createVirtualPort(virtualNetwork.id(), cp4.deviceId(), cp4.port(),
                                  new DefaultPort(virtualDevice2, cp4.port(), true));

        ConnectPoint cp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        manager.createVirtualPort(virtualNetwork.id(), cp5.deviceId(), cp5.port(),
                                  new DefaultPort(virtualDevice3, cp5.port(), true));

        ConnectPoint cp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        manager.createVirtualPort(virtualNetwork.id(), cp6.deviceId(), cp6.port(),
                                  new DefaultPort(virtualDevice3, cp6.port(), true));

        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), cp1, cp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp3, cp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), cp4, cp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), cp5, cp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), cp2, cp6);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), cp6, cp2);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        return virtualNetwork;
    }

    /**
     * Tests the currentTopology() method.
     */
    @Test
    public void testCurrentTopology() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();
        assertNotNull("The topology should not be null.", topology);
    }

    /**
     * Test isLatest() method using a null topology.
     */
    @Test(expected = NullPointerException.class)
    public void testIsLatestByNullTopology() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);

        // test the isLatest() method with a null topology.
        topologyService.isLatest(null);
    }

    /**
     * Test isLatest() method.
     */
    @Test
    public void testIsLatest() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        // test the isLatest() method.
        assertTrue("This should be latest topology", topologyService.isLatest(topology));

        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);

        // test the isLatest() method where a new device has been added to the current topology.
        assertFalse("This should not be latest topology", topologyService.isLatest(topology));

        topology = topologyService.currentTopology();
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), src.deviceId(), src.port(),
                                  new DefaultPort(srcVirtualDevice, src.port(), true));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), dst.deviceId(), dst.port(),
                                  new DefaultPort(dstVirtualDevice, dst.port(), true));
        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), src, dst);

        // test the isLatest() method where a new link has been added to the current topology.
        assertFalse("This should not be latest topology", topologyService.isLatest(topology));
    }

    /**
     * Test getGraph() method.
     */
    @Test
    public void testGetGraph() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        // test the getGraph() method.
        assertNotNull("The graph should not be null.", topologyService.getGraph(topology));
    }

    /**
     * Test getClusters() method.
     */
    @Test
    public void testGetClusters() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);

        Topology topology = topologyService.currentTopology();

        // test the getClusters() method.
        assertNotNull("The clusters should not be null.", topologyService.getClusters(topology));
        assertEquals("The clusters size did not match.", 2, topologyService.getClusters(topology).size());
    }

    /**
     * Test getCluster() method using a null cluster identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetClusterUsingNullClusterId() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        Set<TopologyCluster> clusters = topologyService.getClusters(topology);
        TopologyCluster cluster = clusters.stream().findFirst().get();

        // test the getCluster() method with a null cluster identifier
        TopologyCluster cluster1 = topologyService.getCluster(topology, null);
    }

    /**
     * Test getCluster() method.
     */
    @Test
    public void testGetCluster() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        Set<TopologyCluster> clusters = topologyService.getClusters(topology);
        assertNotNull("The clusters should not be null.", clusters);
        assertEquals("The clusters size did not match.", 2, clusters.size());

        // test the getCluster() method.
        TopologyCluster cluster = clusters.stream().findFirst().get();
        assertNotNull("The cluster should not be null.", cluster);
        TopologyCluster cluster1 = topologyService.getCluster(topology, cluster.id());
        assertNotNull("The cluster should not be null.", cluster1);
        assertEquals("The cluster ID did not match.", cluster.id(), cluster1.id());
    }

    /**
     * Test getClusterDevices() methods with a null cluster.
     */
    @Test(expected = NullPointerException.class)
    public void testGetClusterDevicesUsingNullCluster() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();
        Set<TopologyCluster> clusters = topologyService.getClusters(topology);

        // test the getClusterDevices() method using a null cluster.
        Object[] objects = clusters.stream().toArray();
        assertNotNull("The cluster should not be null.", objects);
        Set<DeviceId> clusterDevices = topologyService.getClusterDevices(topology, null);
    }

    /**
     * Test getClusterLinks() methods with a null cluster.
     */
    @Test(expected = NullPointerException.class)
    public void testGetClusterLinksUsingNullCluster() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();
        Set<TopologyCluster> clusters = topologyService.getClusters(topology);

        // test the getClusterLinks() method using a null cluster.
        Object[] objects = clusters.stream().toArray();
        assertNotNull("The cluster should not be null.", objects);
        Set<Link> clusterLinks = topologyService.getClusterLinks(topology, null);
    }

    /**
     * Test getClusterDevices() and getClusterLinks() methods.
     */
    @Test
    public void testGetClusterDevicesLinks() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        Set<TopologyCluster> clusters = topologyService.getClusters(topology);
        assertNotNull("The clusters should not be null.", clusters);
        assertEquals("The clusters size did not match.", 2, clusters.size());

        // test the getClusterDevices() method.
        Object[] objects = clusters.stream().toArray();
        assertNotNull("The cluster should not be null.", objects);
        Set<DeviceId> clusterDevices = topologyService.getClusterDevices(topology, (TopologyCluster) objects[0]);
        assertNotNull("The devices should not be null.", clusterDevices);
        assertEquals("The devices size did not match.", 3, clusterDevices.size());
        Set<DeviceId> clusterDevices1 = topologyService.getClusterDevices(topology, (TopologyCluster) objects[1]);
        assertNotNull("The devices should not be null.", clusterDevices1);
        assertEquals("The devices size did not match.", 1, clusterDevices1.size());

        // test the getClusterLinks() method.
        Set<Link> clusterLinks = topologyService.getClusterLinks(topology, (TopologyCluster) objects[0]);
        assertNotNull("The links should not be null.", clusterLinks);
        assertEquals("The links size did not match.", 6, clusterLinks.size());
        Set<Link> clusterLinks1 = topologyService.getClusterLinks(topology, (TopologyCluster) objects[1]);
        assertNotNull("The links should not be null.", clusterLinks1);
        assertEquals("The links size did not match.", 0, clusterLinks1.size());
    }

    /**
     * Test getPaths() method using a null src device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPathsUsingNullSrcDeviceId() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getPaths() method using a null src device identifier.
        Set<Path> paths = topologyService.getPaths(topology, null, dstVirtualDevice.id());
    }

    /**
     * Test getPaths() method using a null dst device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPathsUsingNullDstDeviceId() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getPaths() method using a null dst device identifier.
        Set<Path> paths = topologyService.getPaths(topology, srcVirtualDevice.id(), null);
    }

    /**
     * Test getPaths() method using a null weight.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPathsUsingNullWeight() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getPaths() method using a null weight.
        Set<Path> paths = topologyService.getPaths(topology, srcVirtualDevice.id(), dstVirtualDevice.id(), null);
    }

    /**
     * Test getPaths() and getPaths() by weight methods.
     */
    @Test
    public void testGetPaths() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getPaths() method.
        Set<Path> paths = topologyService.getPaths(topology, srcVirtualDevice.id(), dstVirtualDevice.id());
        assertNotNull("The paths should not be null.", paths);
        assertEquals("The paths size did not match.", 1, paths.size());

        // test the getPaths() by weight method.
        LinkWeight weight = edge -> 1.0;
        Set<Path> paths1 = topologyService.getPaths(topology, srcVirtualDevice.id(), dstVirtualDevice.id(), weight);
        assertNotNull("The paths should not be null.", paths1);
        assertEquals("The paths size did not match.", 1, paths1.size());
        Path path = paths1.iterator().next();
        assertEquals("wrong path length", 1, path.links().size());
        assertEquals("wrong path cost", 1.0, path.cost(), 0.01);
    }

    /**
     * Test getDisjointPaths() methods using a null src device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDisjointPathsUsingNullSrcDeviceId() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getDisjointPaths() method using a null src device identifier.
        Set<DisjointPath> paths = topologyService.getDisjointPaths(topology, null, dstVirtualDevice.id());
    }

    /**
     * Test getDisjointPaths() methods using a null dst device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDisjointPathsUsingNullDstDeviceId() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getDisjointPaths() method using a null dst device identifier.
        Set<DisjointPath> paths = topologyService.getDisjointPaths(topology, srcVirtualDevice.id(), null);
    }

    /**
     * Test getDisjointPaths() methods using a null weight.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDisjointPathsUsingNullWeight() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getDisjointPaths() method using a null weight.
        Set<DisjointPath> paths = topologyService.getDisjointPaths(topology, srcVirtualDevice.id(),
                                                                   dstVirtualDevice.id(), (LinkWeight) null);
    }

    /**
     * Test getDisjointPaths() methods using a null risk profile.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDisjointPathsUsingNullRiskProfile() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getDisjointPaths() method using a null risk profile.
        Set<DisjointPath> paths = topologyService.getDisjointPaths(topology, srcVirtualDevice.id(),
                                                                   dstVirtualDevice.id(), (Map<Link, Object>) null);
    }

    /**
     * Test getDisjointPaths() methods.
     */
    @Test
    public void testGetDisjointPaths() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID2);

        // test the getDisjointPaths() method.
        Set<DisjointPath> paths = topologyService.getDisjointPaths(topology, srcVirtualDevice.id(),
                                                                   dstVirtualDevice.id());
        assertNotNull("The paths should not be null.", paths);
        assertEquals("The paths size did not match.", 1, paths.size());

        // test the getDisjointPaths() method using a weight.
        LinkWeight weight = edge -> 1.0;
        Set<DisjointPath> paths1 = topologyService.getDisjointPaths(topology, srcVirtualDevice.id(),
                                                                    dstVirtualDevice.id(), weight);
        assertNotNull("The paths should not be null.", paths1);
        assertEquals("The paths size did not match.", 1, paths1.size());
    }

    /**
     * Test isInfrastructure() method using a null connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testIsInfrastructureUsingNullConnectPoint() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        // test the isInfrastructure() method using a null connect point.
        Boolean isInfrastructure = topologyService.isInfrastructure(topology, null);
    }

    /**
     * Test isInfrastructure() method.
     */
    @Test
    public void testIsInfrastructure() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID4);
        ConnectPoint cp1 = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        ConnectPoint cp2 = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));

        // test the isInfrastructure() method.
        Boolean isInfrastructure = topologyService.isInfrastructure(topology, cp1);
        assertTrue("The connect point should be infrastructure.", isInfrastructure);

        isInfrastructure = topologyService.isInfrastructure(topology, cp2);
        assertFalse("The connect point should not be infrastructure.", isInfrastructure);
    }

    /**
     * Test isBroadcastPoint() method using a null connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testIsBroadcastUsingNullConnectPoint() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        // test the isInfrastructure() method using a null connect point.
        Boolean isInfrastructure = topologyService.isBroadcastPoint(topology, null);
    }

    /**
     * Test isBroadcastPoint() method.
     */
    @Test
    public void testIsBroadcastPoint() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        TopologyService topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        Topology topology = topologyService.currentTopology();

        VirtualDevice srcVirtualDevice = getVirtualDevice(virtualNetwork.id(), DID1);
        ConnectPoint cp = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));

        // test the isBroadcastPoint() method.
        Boolean isBroadcastPoint = topologyService.isBroadcastPoint(topology, cp);
        assertTrue("The connect point should be a broadcast point.", isBroadcastPoint);
    }

    /**
     * Return the virtual device matching the device identifier.
     *
     * @param networkId virtual network identifier
     * @param deviceId  device identifier
     * @return virtual device
     */
    private VirtualDevice getVirtualDevice(NetworkId networkId, DeviceId deviceId) {
        Optional<VirtualDevice> foundDevice = manager.getVirtualDevices(networkId)
                .stream()
                .filter(device -> deviceId.equals(device.id()))
                .findFirst();
        if (foundDevice.isPresent()) {
            return foundDevice.get();
        }
        return null;
    }

    /**
     * Core service test class.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }
}
