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

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkGroupStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkPacketStore;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.impl.provider.DefaultVirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.impl.provider.DefaultVirtualGroupProvider;
import org.onosproject.incubator.net.virtual.impl.provider.DefaultVirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.impl.provider.DefaultVirtualPacketProvider;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualFlowObjectiveStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualFlowRuleStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualGroupStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualPacketStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Junit tests for VirtualNetworkManager.
 */
public class VirtualNetworkManagerTest extends VirtualNetworkTestUtil {
    private final String tenantIdValue1 = "TENANT_ID1";
    private final String tenantIdValue2 = "TENANT_ID2";

    private VirtualNetworkManager manager;
    private DefaultVirtualNetworkProvider topologyProvider;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestListener listener = new TestListener();
    private TopologyService topologyService;

    private ConnectPoint cp6;
    private ConnectPoint cp7;

    private TestServiceDirectory testDirectory;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();
        MockIdGenerator.cleanBind();

        coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService",
                           new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.addListener(listener);
        manager.coreService = coreService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());

        testDirectory = new TestServiceDirectory();
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.removeListener(listener);
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
        MockIdGenerator.cleanBind();
    }

    /**
     * Tests registering a null tenant id.
     */
    @Test(expected = NullPointerException.class)
    public void testRegisterNullTenantId() {
        manager.registerTenantId(null);
    }

    /**
     * Tests registering/unregistering a tenant id.
     */
    @Test
    public void testRegisterUnregisterTenantId() {
        manager.unregisterTenantId(TenantId.tenantId(tenantIdValue1));
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        manager.registerTenantId(TenantId.tenantId(tenantIdValue2));
        Collection<TenantId> tenantIdCollection = manager.getTenantIds();
        assertEquals("The tenantId set size did not match.", 2, tenantIdCollection.size());

        manager.unregisterTenantId(TenantId.tenantId(tenantIdValue1));
        manager.unregisterTenantId(TenantId.tenantId(tenantIdValue2));
        tenantIdCollection = manager.getTenantIds();
        assertTrue("The tenantId set should be empty.", tenantIdCollection.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents(VirtualNetworkEvent.Type.TENANT_UNREGISTERED,
                       VirtualNetworkEvent.Type.TENANT_REGISTERED,
                       VirtualNetworkEvent.Type.TENANT_REGISTERED,
                       VirtualNetworkEvent.Type.TENANT_UNREGISTERED,
                       VirtualNetworkEvent.Type.TENANT_UNREGISTERED);
    }

    /**
     * Tests adding a null virtual network.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVirtualNetwork() {
        manager.createVirtualNetwork(null);
    }

    /**
     * Tests removal of a virtual network twice.
     */
    @Test(expected = IllegalStateException.class)
    public void testRemoveVnetTwice() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.removeVirtualNetwork(virtualNetwork.id());
        manager.removeVirtualNetwork(virtualNetwork.id());
    }

    /**
     * Tests add and remove of virtual networks.
     */
    @Test
    public void testAddRemoveVirtualNetwork() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        Set<VirtualNetwork> virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertNotNull("The virtual network set should not be null", virtualNetworks);
        assertEquals("The virtual network set size did not match.", 2, virtualNetworks.size());

        int remaining = virtualNetworks.size();
        for (VirtualNetwork virtualNetwork : virtualNetworks) {
            manager.removeVirtualNetwork(virtualNetwork.id());
            assertEquals("The expected virtual network size does not match",
                         --remaining, manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1)).size());
        }
        virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertTrue("The virtual network set should be empty.", virtualNetworks.isEmpty());

        // Create/remove a virtual network.
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.removeVirtualNetwork(virtualNetwork.id());

        virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertTrue("The virtual network set should be empty.", virtualNetworks.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents(VirtualNetworkEvent.Type.TENANT_REGISTERED,
                       VirtualNetworkEvent.Type.NETWORK_ADDED,
                       VirtualNetworkEvent.Type.NETWORK_ADDED,
                       VirtualNetworkEvent.Type.NETWORK_REMOVED,
                       VirtualNetworkEvent.Type.NETWORK_REMOVED,
                       VirtualNetworkEvent.Type.NETWORK_ADDED,
                       VirtualNetworkEvent.Type.NETWORK_REMOVED);
    }

    /**
     * Tests adding a null virtual device.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVirtualDevice() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        manager.createVirtualDevice(virtualNetwork.id(), null);
    }

    /**
     * Tests adding a virtual device where no virtual network exists.
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateVirtualDeviceWithNoNetwork() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                new DefaultVirtualNetwork(NetworkId.NONE,
                                          TenantId.tenantId(tenantIdValue1));

        manager.createVirtualDevice(virtualNetwork.id(), DID1);
    }

    /**
     * Tests add and remove of virtual devices.
     */
    @Test
    public void testAddRemoveVirtualDevice() {
        List<VirtualNetworkEvent.Type> expectedEventTypes = new ArrayList<>();

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        expectedEventTypes.add(VirtualNetworkEvent.Type.TENANT_REGISTERED);
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        expectedEventTypes.add(VirtualNetworkEvent.Type.NETWORK_ADDED);
        VirtualNetwork virtualNetwork2 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        expectedEventTypes.add(VirtualNetworkEvent.Type.NETWORK_ADDED);
        manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_ADDED);
        manager.createVirtualDevice(virtualNetwork2.id(), DID2);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_ADDED);

        Set<VirtualDevice> virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertNotNull("The virtual device set should not be null", virtualDevices1);
        assertEquals("The virtual device set size did not match.", 1, virtualDevices1.size());

        Set<VirtualDevice> virtualDevices2 = manager.getVirtualDevices(virtualNetwork2.id());
        assertNotNull("The virtual device set should not be null", virtualDevices2);
        assertEquals("The virtual device set size did not match.", 1, virtualDevices2.size());

        for (VirtualDevice virtualDevice : virtualDevices1) {
            manager.removeVirtualDevice(virtualNetwork1.id(), virtualDevice.id());
            expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_REMOVED);
            // attempt to remove the same virtual device again - no event expected.
            manager.removeVirtualDevice(virtualNetwork1.id(), virtualDevice.id());
        }
        virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertTrue("The virtual device set should be empty.", virtualDevices1.isEmpty());

        // Add/remove the virtual device again.
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_ADDED);
        manager.removeVirtualDevice(virtualDevice.networkId(), virtualDevice.id());
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_REMOVED);
        virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertTrue("The virtual device set should be empty.", virtualDevices1.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents((Enum[]) expectedEventTypes.toArray(
                new VirtualNetworkEvent.Type[expectedEventTypes.size()]));
    }

    /**
     * Tests getting a collection of physical device identifier corresponding to
     * the specified virtual device.
     */
    @Test
    public void testGetPhysicalDevices() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        manager.registerTenantId(TenantId.tenantId(tenantIdValue2));

        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork2 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue2));

        // two virtual device in first virtual network
        VirtualDevice vDevice1InVnet1 =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice vDevice2InVnet1 =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        // Two virtual device in second virtual network
        VirtualDevice vDevice1InVnet2 =
                manager.createVirtualDevice(virtualNetwork2.id(), DID1);
        VirtualDevice vDevice2InVnet2 =
                manager.createVirtualDevice(virtualNetwork2.id(), DID2);

        // Connection Point from each physical device
        // Virtual network 1
        ConnectPoint cp1InVnet1 =
                new ConnectPoint(PHYDID1, PortNumber.portNumber(10));
        ConnectPoint cp2InVnet1 =
                new ConnectPoint(PHYDID2, PortNumber.portNumber(20));
        ConnectPoint cp3InVnet1 =
                new ConnectPoint(PHYDID3, PortNumber.portNumber(30));
        ConnectPoint cp4InVnet1 =
                new ConnectPoint(PHYDID4, PortNumber.portNumber(40));
        // Virtual network 2
        ConnectPoint cp1InVnet2 =
                new ConnectPoint(PHYDID1, PortNumber.portNumber(10));
        ConnectPoint cp2InVnet2 =
                new ConnectPoint(PHYDID2, PortNumber.portNumber(20));
        ConnectPoint cp3InVnet2 =
                new ConnectPoint(PHYDID3, PortNumber.portNumber(30));
        ConnectPoint cp4InVnet2 =
                new ConnectPoint(PHYDID4, PortNumber.portNumber(40));

        // Make simple BigSwitch by mapping two phyDevice to one vDevice
        // First vDevice in first virtual network
        manager.createVirtualPort(virtualNetwork1.id(),
                vDevice1InVnet1.id(), PortNumber.portNumber(1), cp1InVnet1);
        manager.createVirtualPort(virtualNetwork1.id(),
                vDevice1InVnet1.id(), PortNumber.portNumber(2), cp2InVnet1);
        // Second vDevice in first virtual network
        manager.createVirtualPort(virtualNetwork1.id(),
                vDevice2InVnet1.id(), PortNumber.portNumber(1), cp3InVnet1);
        manager.createVirtualPort(virtualNetwork1.id(),
                vDevice2InVnet1.id(), PortNumber.portNumber(2), cp4InVnet1);
        // First vDevice in second virtual network
        manager.createVirtualPort(virtualNetwork2.id(),
                vDevice1InVnet2.id(), PortNumber.portNumber(1), cp1InVnet2);
        manager.createVirtualPort(virtualNetwork2.id(),
                vDevice1InVnet2.id(), PortNumber.portNumber(2), cp2InVnet2);
        // Second vDevice in second virtual network
        manager.createVirtualPort(virtualNetwork2.id(),
                vDevice2InVnet2.id(), PortNumber.portNumber(1), cp3InVnet2);
        manager.createVirtualPort(virtualNetwork2.id(),
                vDevice2InVnet2.id(), PortNumber.portNumber(2), cp4InVnet2);


        Set<DeviceId> physicalDeviceSet;
        Set<DeviceId> testSet = new HashSet<>();
        physicalDeviceSet = manager.getPhysicalDevices(virtualNetwork1.id(), vDevice1InVnet1.id());
        testSet.add(PHYDID1);
        testSet.add(PHYDID2);
        assertEquals("The physical devices 1 did not match", testSet, physicalDeviceSet);
        testSet.clear();

        physicalDeviceSet = manager.getPhysicalDevices(virtualNetwork1.id(), vDevice2InVnet1.id());
        testSet.add(PHYDID3);
        testSet.add(PHYDID4);
        assertEquals("The physical devices 2 did not match", testSet, physicalDeviceSet);
        testSet.clear();

        physicalDeviceSet = manager.getPhysicalDevices(virtualNetwork2.id(), vDevice1InVnet2.id());
        testSet.add(PHYDID1);
        testSet.add(PHYDID2);
        assertEquals("The physical devices 1 did not match", testSet, physicalDeviceSet);
        testSet.clear();

        physicalDeviceSet = manager.getPhysicalDevices(virtualNetwork2.id(), vDevice2InVnet2.id());
        testSet.add(PHYDID3);
        testSet.add(PHYDID4);
        assertEquals("The physical devices 2 did not match", testSet, physicalDeviceSet);
        testSet.clear();
    }

    /**
     * Tests adding a null virtual host.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVirtualHost() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        manager.createVirtualHost(virtualNetwork.id(), null, null, null, null, null);
    }

    /**
     * Tests adding a virtual host where no virtual network exists.
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateVirtualHostWithNoNetwork() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                new DefaultVirtualNetwork(NetworkId.NONE, TenantId.tenantId(tenantIdValue1));

        manager.createVirtualHost(virtualNetwork.id(), HID1, null, null, null, null);
    }

    /**
     * Tests adding a virtual host where no virtual port exists.
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateVirtualHostWithNoVirtualPort() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.createVirtualHost(virtualNetwork1.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);
    }

    /**
     * Tests add and remove of virtual hosts.
     */
    @Test
    public void testAddRemoveVirtualHost() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork2 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork2.id(), DID2);

        ConnectPoint hostCp1 = new ConnectPoint(DID1, P1);
        ConnectPoint hostCp2 = new ConnectPoint(DID2, P2);
        manager.createVirtualPort(virtualNetwork1.id(), hostCp1.deviceId(), hostCp1.port(),
                new ConnectPoint(virtualDevice1.id(), hostCp1.port()));
        manager.createVirtualPort(virtualNetwork2.id(), hostCp2.deviceId(), hostCp2.port(),
                new ConnectPoint(virtualDevice2.id(), hostCp2.port()));

        manager.createVirtualHost(virtualNetwork1.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);
        manager.createVirtualHost(virtualNetwork2.id(), HID2, MAC2, VLAN2, LOC2, IPSET2);

        Set<VirtualHost> virtualHosts1 = manager.getVirtualHosts(virtualNetwork1.id());
        assertNotNull("The virtual host set should not be null", virtualHosts1);
        assertEquals("The virtual host set size did not match.", 1, virtualHosts1.size());

        Set<VirtualHost> virtualHosts2 = manager.getVirtualHosts(virtualNetwork2.id());
        assertNotNull("The virtual host set should not be null", virtualHosts2);
        assertEquals("The virtual host set size did not match.", 1, virtualHosts2.size());

        for (VirtualHost virtualHost : virtualHosts1) {
            manager.removeVirtualHost(virtualNetwork1.id(), virtualHost.id());
            // attempt to remove the same virtual host again.
            manager.removeVirtualHost(virtualNetwork1.id(), virtualHost.id());
        }
        virtualHosts1 = manager.getVirtualHosts(virtualNetwork1.id());
        assertTrue("The virtual host set should be empty.", virtualHosts1.isEmpty());

        // Add/remove the virtual host again.
        VirtualHost virtualHost =
                manager.createVirtualHost(virtualNetwork1.id(),
                                          HID1, MAC1, VLAN1, LOC1, IPSET1);
        manager.removeVirtualHost(virtualHost.networkId(), virtualHost.id());
        virtualHosts1 = manager.getVirtualHosts(virtualNetwork1.id());
        assertTrue("The virtual host set should be empty.", virtualHosts1.isEmpty());
    }

    /**
     * Tests add and remove of virtual links.
     */
    @Test
    public void testAddRemoveVirtualLink() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                                  new ConnectPoint(srcVirtualDevice.id(), src.port()));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork1.id(), dst.deviceId(), dst.port(),
                                  new ConnectPoint(dstVirtualDevice.id(), dst.port()));

        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.createVirtualLink(virtualNetwork1.id(), dst, src);

        Set<VirtualLink> virtualLinks = manager.getVirtualLinks(virtualNetwork1.id());
        assertNotNull("The virtual link set should not be null", virtualLinks);
        assertEquals("The virtual link set size did not match.", 2, virtualLinks.size());

        for (VirtualLink virtualLink : virtualLinks) {
            manager.removeVirtualLink(virtualLink.networkId(), virtualLink.src(), virtualLink.dst());
            // attempt to remove the same virtual link again.
            manager.removeVirtualLink(virtualLink.networkId(), virtualLink.src(), virtualLink.dst());
        }
        virtualLinks = manager.getVirtualLinks(virtualNetwork1.id());
        assertTrue("The virtual link set should be empty.", virtualLinks.isEmpty());

        // Add/remove the virtual link again.
        VirtualLink virtualLink = manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.removeVirtualLink(virtualLink.networkId(), virtualLink.src(), virtualLink.dst());
        virtualLinks = manager.getVirtualLinks(virtualNetwork1.id());
        assertTrue("The virtual link set should be empty.", virtualLinks.isEmpty());
    }

    /**
     * Tests adding the same virtual link twice.
     */
    @Test(expected = IllegalStateException.class)
    public void testAddSameVirtualLink() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                                  new ConnectPoint(srcVirtualDevice.id(), src.port()));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork1.id(), dst.deviceId(), dst.port(),
                                  new ConnectPoint(dstVirtualDevice.id(), dst.port()));

        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
    }

    /**
     * Tests add, bind and remove of virtual ports.
     */
    @Test
    public void testAddRemoveVirtualPort() {
        List<VirtualNetworkEvent.Type> expectedEventTypes = new ArrayList<>();

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        expectedEventTypes.add(VirtualNetworkEvent.Type.TENANT_REGISTERED);
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        expectedEventTypes.add(VirtualNetworkEvent.Type.NETWORK_ADDED);
        VirtualDevice virtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_DEVICE_ADDED);
        ConnectPoint cp = new ConnectPoint(virtualDevice.id(), PortNumber.portNumber(1));

        manager.createVirtualPort(virtualNetwork1.id(),
                                  virtualDevice.id(), PortNumber.portNumber(1), cp);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_ADDED);
        manager.createVirtualPort(virtualNetwork1.id(),
                                  virtualDevice.id(), PortNumber.portNumber(2), cp);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_ADDED);

        Set<VirtualPort> virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertNotNull("The virtual port set should not be null", virtualPorts);
        assertEquals("The virtual port set size did not match.", 2, virtualPorts.size());

        for (VirtualPort virtualPort : virtualPorts) {
            manager.removeVirtualPort(virtualNetwork1.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
            expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_REMOVED);
            // attempt to remove the same virtual port again.
            manager.removeVirtualPort(virtualNetwork1.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
        }
        virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());

        // Add/remove the virtual port again.
        VirtualPort virtualPort =
                manager.createVirtualPort(virtualNetwork1.id(), virtualDevice.id(),
                                                            PortNumber.portNumber(1), cp);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_ADDED);

        ConnectPoint newCp = new ConnectPoint(DID2, PortNumber.portNumber(2));
        manager.bindVirtualPort(virtualNetwork1.id(), virtualDevice.id(),
                                PortNumber.portNumber(1), newCp);
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_UPDATED);

        manager.removeVirtualPort(virtualNetwork1.id(),
                                  (DeviceId) virtualPort.element().id(), virtualPort.number());
        expectedEventTypes.add(VirtualNetworkEvent.Type.VIRTUAL_PORT_REMOVED);
        virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents((Enum[]) expectedEventTypes.toArray(
                new VirtualNetworkEvent.Type[expectedEventTypes.size()]));
    }

    /**
     * Tests when a virtual element is removed, all the other elements depending on it are also removed.
     */
    @Test
    public void testRemoveAllElements() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        ConnectPoint src = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                new ConnectPoint(PHYDID1, PortNumber.portNumber(1)));

        ConnectPoint dst = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork1.id(), dst.deviceId(), dst.port(),
                new ConnectPoint(PHYDID2, PortNumber.portNumber(2)));

        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.createVirtualLink(virtualNetwork1.id(), dst, src);

        ConnectPoint hostCp = new ConnectPoint(DID1, P1);
        manager.createVirtualPort(virtualNetwork1.id(), hostCp.deviceId(), hostCp.port(),
                new ConnectPoint(PHYDID1, P1));
        manager.createVirtualHost(virtualNetwork1.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);

        //When a virtual port is removed, all virtual links connected to it should also be removed.
        manager.removeVirtualPort(virtualNetwork1.id(), DID1, PortNumber.portNumber(1));
        Set<VirtualLink> virtualLinks = manager.getVirtualLinks(virtualNetwork1.id());
        assertTrue("The virtual link set should be empty.", virtualLinks.isEmpty());

        //When a virtual port is removed, all virtual hosts located to it should also be removed.
        manager.removeVirtualPort(virtualNetwork1.id(), DID1, P1);
        Set<VirtualHost> virtualHosts = manager.getVirtualHosts(virtualNetwork1.id());
        assertTrue("The virtual host set should be empty.", virtualHosts.isEmpty());

        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                new ConnectPoint(PHYDID1, PortNumber.portNumber(1)));
        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.createVirtualLink(virtualNetwork1.id(), dst, src);
        manager.createVirtualPort(virtualNetwork1.id(), hostCp.deviceId(), hostCp.port(),
                new ConnectPoint(PHYDID1, P1));
        manager.createVirtualHost(virtualNetwork1.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);

        //When a virtual device is removed, all virtual ports, hosts and links depended on it should also be removed.
        manager.removeVirtualDevice(virtualNetwork1.id(), DID1);
        Set<VirtualPort> virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), DID1);
        assertTrue("The virtual port set of DID1 should be empty", virtualPorts.isEmpty());
        virtualLinks = manager.getVirtualLinks(virtualNetwork1.id());
        assertTrue("The virtual link set should be empty.", virtualLinks.isEmpty());
        virtualHosts = manager.getVirtualHosts(virtualNetwork1.id());
        assertTrue("The virtual host set should be empty.", virtualHosts.isEmpty());

        //When a tenantId is removed, all the virtual networks belonging to it should also be removed.
        manager.unregisterTenantId(TenantId.tenantId(tenantIdValue1));
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        Set<VirtualNetwork> virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertNotNull("The virtual network set should not be null", virtualNetworks);
        assertTrue("The virtual network set should be empty.", virtualNetworks.isEmpty());
    }

    /**
     * Tests the addTunnelId() method in the store with a null intent.
     */
    @Test(expected = NullPointerException.class)
    public void testAddTunnelIdNullIntent() {
        manager.store.addTunnelId(null, null);
    }

    /**
     * Tests the removeTunnelId() method in the store with a null intent.
     */
    @Test(expected = NullPointerException.class)
    public void testRemoveTunnelIdNullIntent() {
        manager.store.removeTunnelId(null, null);
    }

    /**
     * Tests the addTunnelId, getTunnelIds(), removeTunnelId() methods with the store.
     */
    @Test
    public void testAddTunnelId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        ConnectPoint cp1 = new ConnectPoint(DID1, P1);
        ConnectPoint cp2 = new ConnectPoint(DID2, P1);

        VirtualNetworkIntent virtualIntent = VirtualNetworkIntent.builder()
                .networkId(virtualNetwork.id())
                .key(Key.of("Test", APP_ID))
                .appId(APP_ID)
                .ingressPoint(cp1)
                .egressPoint(cp2)
                .build();

        TunnelId tunnelId = TunnelId.valueOf("virtual tunnel");
        // Add the intent to tunnelID mapping to the store.
        manager.store.addTunnelId(virtualIntent, tunnelId);
        assertEquals("The tunnels size should match.", 1,
                     manager.store.getTunnelIds(virtualIntent).size());

        // Remove the intent to tunnelID mapping from the store.
        manager.store.removeTunnelId(virtualIntent, tunnelId);
        assertTrue("The tunnels should be empty.",
                   manager.store.getTunnelIds(virtualIntent).isEmpty());
    }


    /**
     * Method to create the virtual network for further testing.
     **/
    private VirtualNetwork setupVirtualNetworkTopology() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        VirtualDevice virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), DID3);
        VirtualDevice virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), DID4);
        VirtualDevice virtualDevice5 =
                manager.createVirtualDevice(virtualNetwork.id(), DID5);

        ConnectPoint cp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(1), cp1);

        ConnectPoint cp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(2), cp2);

        ConnectPoint cp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(3), cp3);

        ConnectPoint cp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(4), cp4);

        ConnectPoint cp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(5), cp5);

        cp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(6), cp6);

        cp7 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(7));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(7), cp7);

        ConnectPoint cp8 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(8));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(8), cp8);

        ConnectPoint cp9 = new ConnectPoint(virtualDevice5.id(), PortNumber.portNumber(9));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice5.id(),
                                  PortNumber.portNumber(9), cp9);

        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), cp1, cp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp3, cp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), cp4, cp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), cp5, cp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), cp8, cp9);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), cp9, cp8);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        topologyProvider = new DefaultVirtualNetworkProvider();
        try {
            TestUtils.setField(topologyProvider, "topologyService", topologyService);
        } catch (TestUtils.TestUtilsException e) {
            e.printStackTrace();
        }
//        topologyProvider.topologyService = topologyService;

        return virtualNetwork;
    }

    /**
     * Test the topologyChanged() method.
     */
    @Test
    public void testTopologyChanged() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();
        VirtualNetworkProviderService providerService =
                manager.createProviderService(topologyProvider);

        // Initial setup is two clusters of devices/links.
        assertEquals("The cluster count did not match.", 2,
                     topologyService.currentTopology().clusterCount());

        // Adding this link will join the two clusters together.
        List<Event> reasons = new ArrayList<>();
        VirtualLink link = manager.createVirtualLink(virtualNetwork.id(), cp6, cp7);
        virtualNetworkManagerStore.updateLink(link, link.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp7, cp6);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);

        Topology topology = topologyService.currentTopology();
        providerService.topologyChanged(topologyProvider.getConnectPoints(topology));

        // Validate that all links are still active.
        manager.getVirtualLinks(virtualNetwork.id()).forEach(virtualLink -> {
            assertTrue("The virtual link should be active.",
                       virtualLink.state().equals(Link.State.ACTIVE));
        });

        virtualNetworkManagerStore.updateLink(link, link.tunnelId(), Link.State.INACTIVE);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.INACTIVE);
        providerService.topologyChanged(topologyProvider.getConnectPoints(topology));

        // Validate that all links are active again.
        manager.getVirtualLinks(virtualNetwork.id()).forEach(virtualLink -> {
            assertTrue("The virtual link should be active.",
                       virtualLink.state().equals(Link.State.ACTIVE));
        });
    }

    /**
     * Tests that the get() method returns saved service instances.
     */
    @Test
    public void testServiceGetReturnsSavedInstance() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork =
                manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), DeviceService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), LinkService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), TopologyService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), IntentService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), HostService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), PathService.class);

        // extra setup needed for FlowRuleService, PacketService, GroupService
        VirtualProviderManager virtualProviderManager = new VirtualProviderManager();
        virtualProviderManager.registerProvider(new DefaultVirtualFlowRuleProvider());
        virtualProviderManager.registerProvider(new DefaultVirtualPacketProvider());
        virtualProviderManager.registerProvider(new DefaultVirtualGroupProvider());
        testDirectory.add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, virtualProviderManager)
                .add(EventDeliveryService.class, new TestEventDispatcher())
                .add(ClusterService.class, new ClusterServiceAdapter())
                .add(VirtualNetworkFlowRuleStore.class, new SimpleVirtualFlowRuleStore())
                .add(VirtualNetworkPacketStore.class, new SimpleVirtualPacketStore())
                .add(VirtualNetworkGroupStore.class, new SimpleVirtualGroupStore())
                .add(VirtualNetworkFlowObjectiveStore.class, new SimpleVirtualFlowObjectiveStore());

        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), FlowRuleService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), FlowObjectiveService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), PacketService.class);
        validateServiceGetReturnsSavedInstance(virtualNetwork.id(), GroupService.class);
    }

    /**
     * Validates that the get() method returns saved service instances.
     */
    private <T> void validateServiceGetReturnsSavedInstance(NetworkId networkId,
                                                            Class<T> serviceClass) {
        T serviceInstanceFirst = manager.get(networkId, serviceClass);
        T serviceInstanceSubsequent = manager.get(networkId, serviceClass);
        assertSame(serviceClass.getSimpleName() +
                     ": Subsequent get should be same as the first one",
                     serviceInstanceFirst, serviceInstanceSubsequent);
    }

    /**
     * Method to validate that the actual versus expected virtual network events were
     * received correctly.
     *
     * @param types expected virtual network events.
     */
    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, listener.events.size());
            for (Event event : listener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            listener.events.clear();
        });
    }

    /**
     * Test listener class to receive virtual network events.
     */
    private static class TestListener implements VirtualNetworkListener {

        private List<VirtualNetworkEvent> events = Lists.newArrayList();

        @Override
        public void event(VirtualNetworkEvent event) {
            events.add(event);
        }

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
