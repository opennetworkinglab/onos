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
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.store.service.TestStorageService;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Junit tests for VirtualNetworkPathService.
 */
public class VirtualNetworkPathServiceTest extends TestDeviceParams {
    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private final TestableIntentService intentService = new FakeIntentManager();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        virtualNetworkManagerStore.setCoreService(coreService);
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
     * Sets up an empty virtual network (no devices, links).
     *
     * @return virtual network
     */
    private VirtualNetwork setupEmptyVnet() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        return manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
    }

    /**
     * Creates a virtual network for further testing.
     *
     * @return virtual network
     */
    private VirtualNetwork setupVnet() {
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

        ConnectPoint cp11 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice1, 1);
        ConnectPoint cp12 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice1, 2);
        ConnectPoint cp23 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice2, 3);
        ConnectPoint cp24 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice2, 4);
        ConnectPoint cp35 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice3, 5);
        ConnectPoint cp36 = createConnectPointAndVirtualPort(virtualNetwork, virtualDevice3, 6);
        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), cp11, cp23);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp23, cp11);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), cp24, cp35);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), cp35, cp24);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), cp12, cp36);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), cp36, cp12);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        return virtualNetwork;
    }

    /**
     * Creates a connect point and related virtual port.
     *
     * @param vnet virtual network
     * @param vDev virtual device
     * @param portNumber port number
     * @return connect point
     */
    private ConnectPoint createConnectPointAndVirtualPort(
            VirtualNetwork vnet, VirtualDevice vDev, long portNumber) {
        ConnectPoint cp = new ConnectPoint(vDev.id(), PortNumber.portNumber(portNumber));
        manager.createVirtualPort(vnet.id(), cp.deviceId(), cp.port(),
                                  new DefaultPort(vDev, cp.port(), true));
        return cp;
    }

    /**
     * Tests getPaths(), getDisjointPaths()
     * on a non-empty virtual network.
     */
    @Test
    public void testGetPathsOnNonEmptyVnet() {
        VirtualNetwork vnet = setupVnet();
        PathService pathService = manager.get(vnet.id(), PathService.class);

        // src and dest are in vnet and are connected by a virtual link
        Set<Path> paths = pathService.getPaths(DID1, DID3);
        validatePaths(paths, 1, 1, DID1, DID3, 1.0);

        LinkWeight linkWeight = edge -> 2.0;
        paths = pathService.getPaths(DID1, DID3, linkWeight);
        validatePaths(paths, 1, 1, DID1, DID3, 2.0);

        Set<DisjointPath> disjointPaths = pathService.getDisjointPaths(DID1, DID3);
        validatePaths(disjointPaths, 1, 1, DID1, DID3, 1.0);

        disjointPaths = pathService.getDisjointPaths(DID1, DID3, linkWeight);
        validatePaths(disjointPaths, 1, 1, DID1, DID3, 2.0);

        // src and dest are in vnet but are not connected
        paths = pathService.getPaths(DID4, DID3);
        assertEquals("incorrect path count", 0, paths.size());

        disjointPaths = pathService.getDisjointPaths(DID4, DID3);
        assertEquals("incorrect path count", 0, disjointPaths.size());

        // src is in vnet, but dest is not in vnet.
        DeviceId nonExistentDeviceId = DeviceId.deviceId("nonExistentDevice");
        paths = pathService.getPaths(DID2, nonExistentDeviceId);
        assertEquals("incorrect path count", 0, paths.size());

        disjointPaths = pathService.getDisjointPaths(DID2, nonExistentDeviceId);
        assertEquals("incorrect path count", 0, disjointPaths.size());
    }

    /**
     * Tests getPaths(), getDisjointPaths()
     * on an empty virtual network.
     */
    @Test
    public void testGetPathsOnEmptyVnet() {
        VirtualNetwork vnet = setupEmptyVnet();
        PathService pathService = manager.get(vnet.id(), PathService.class);

        Set<Path> paths = pathService.getPaths(DID1, DID3);
        assertEquals("incorrect path count", 0, paths.size());

        Set<DisjointPath> disjointPaths = pathService.getDisjointPaths(DID1, DID3);
        assertEquals("incorrect path count", 0, disjointPaths.size());
    }

    /**
     * Tests getPaths() using a null source device on an empty virtual network.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPathsWithNullSrc() {
        VirtualNetwork vnet = setupEmptyVnet();
        PathService pathService = manager.get(vnet.id(), PathService.class);
        pathService.getPaths(null, DID3);
    }

    /**
     * Tests getPaths() using a null destination device on a non-empty virtual network.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPathsWithNullDest() {
        VirtualNetwork vnet = setupVnet();
        PathService pathService = manager.get(vnet.id(), PathService.class);
        pathService.getPaths(DID1, null);
    }


    // Makes sure the set of paths meets basic expectations.
    private void validatePaths(Set<? extends Path> paths, int count, int length,
                               ElementId src, ElementId dst, double cost) {
        assertEquals("incorrect path count", count, paths.size());
        for (Path path : paths) {
            assertEquals("incorrect length", length, path.links().size());
            assertEquals("incorrect source", src, path.src().elementId());
            assertEquals("incorrect destination", dst, path.dst().elementId());
            assertEquals("incorrect cost", cost, path.cost(), 0);
        }
    }
}
