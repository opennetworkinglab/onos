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
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Junit tests for VirtualNetworkManager.
 */
public class VirtualNetworkManagerTest extends TestDeviceParams {
    private final String tenantIdValue1 = "TENANT_ID1";
    private final String tenantIdValue2 = "TENANT_ID2";

    private VirtualNetworkManager manager;
    private VirtualNetworkTopologyProvider topologyProvider;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestListener listener = new TestListener();
    private TestableIntentService intentService = new FakeIntentManager();
    private TopologyService topologyService;
    private IdGenerator idGenerator = new MockIdGenerator();

    private ConnectPoint cp6;
    private ConnectPoint cp7;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();
        Intent.bindIdGenerator(idGenerator);

        coreService = new TestCoreService();
        virtualNetworkManagerStore.setCoreService(coreService);
        TestUtils.setField(coreService, "coreService", new TestCoreService());
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.addListener(listener);
        manager.intentService = intentService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.removeListener(listener);
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
        Intent.unbindIdGenerator(idGenerator);
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
        validateEvents(VirtualNetworkEvent.Type.TENANT_UNREGISTERED, VirtualNetworkEvent.Type.TENANT_REGISTERED,
                       VirtualNetworkEvent.Type.TENANT_REGISTERED, VirtualNetworkEvent.Type.TENANT_UNREGISTERED,
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
            // attempt to remove the same virtual network again.
            manager.removeVirtualNetwork(virtualNetwork.id());
            assertEquals("The expected virtual network size does not match",
                         remaining, manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1)).size());
        }
        virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertTrue("The virtual network set should be empty.", virtualNetworks.isEmpty());

        // Create/remove a virtual network.
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.removeVirtualNetwork(virtualNetwork.id());

        virtualNetworks = manager.getVirtualNetworks(TenantId.tenantId(tenantIdValue1));
        assertTrue("The virtual network set should be empty.", virtualNetworks.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents(VirtualNetworkEvent.Type.TENANT_REGISTERED, VirtualNetworkEvent.Type.NETWORK_ADDED,
                       VirtualNetworkEvent.Type.NETWORK_ADDED, VirtualNetworkEvent.Type.NETWORK_REMOVED,
                       VirtualNetworkEvent.Type.NETWORK_REMOVED, VirtualNetworkEvent.Type.NETWORK_ADDED,
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
        VirtualNetwork virtualNetwork = new DefaultVirtualNetwork(NetworkId.NONE, TenantId.tenantId(tenantIdValue1));

        manager.createVirtualDevice(virtualNetwork.id(), DID1);
    }

    /**
     * Tests add and remove of virtual devices.
     */
    @Test
    public void testAddRemoveVirtualDevice() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork2 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        manager.createVirtualDevice(virtualNetwork2.id(), DID2);

        Set<VirtualDevice> virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertNotNull("The virtual device set should not be null", virtualDevices1);
        assertEquals("The virtual device set size did not match.", 1, virtualDevices1.size());

        Set<VirtualDevice> virtualDevices2 = manager.getVirtualDevices(virtualNetwork2.id());
        assertNotNull("The virtual device set should not be null", virtualDevices2);
        assertEquals("The virtual device set size did not match.", 1, virtualDevices2.size());

        for (VirtualDevice virtualDevice : virtualDevices1) {
            manager.removeVirtualDevice(virtualNetwork1.id(), virtualDevice.id());
            // attempt to remove the same virtual device again.
            manager.removeVirtualDevice(virtualNetwork1.id(), virtualDevice.id());
        }
        virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertTrue("The virtual device set should be empty.", virtualDevices1.isEmpty());

        // Add/remove the virtual device again.
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        manager.removeVirtualDevice(virtualDevice.networkId(), virtualDevice.id());
        virtualDevices1 = manager.getVirtualDevices(virtualNetwork1.id());
        assertTrue("The virtual device set should be empty.", virtualDevices1.isEmpty());

        // Validate that the events were all received in the correct order.
        validateEvents(VirtualNetworkEvent.Type.TENANT_REGISTERED, VirtualNetworkEvent.Type.NETWORK_ADDED,
                       VirtualNetworkEvent.Type.NETWORK_ADDED);
    }

    /**
     * Tests adding a null virtual host.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVirtualHost() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        manager.createVirtualHost(virtualNetwork.id(), null, null, null, null, null);
    }

    /**
     * Tests adding a virtual host where no virtual network exists.
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateVirtualHostWithNoNetwork() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = new DefaultVirtualNetwork(NetworkId.NONE, TenantId.tenantId(tenantIdValue1));

        manager.createVirtualHost(virtualNetwork.id(), HID1, null, null, null, null);
    }

    /**
     * Tests add and remove of virtual hosts.
     */
    @Test
    public void testAddRemoveVirtualHost() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork2 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
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
        VirtualHost virtualHost = manager.createVirtualHost(virtualNetwork1.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);
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
        VirtualNetwork virtualNetwork1 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                                  new DefaultPort(srcVirtualDevice, src.port(), true));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork1.id(), dst.deviceId(), dst.port(),
                                  new DefaultPort(dstVirtualDevice, dst.port(), true));

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
        VirtualNetwork virtualNetwork1 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork1.id(), src.deviceId(), src.port(),
                                  new DefaultPort(srcVirtualDevice, src.port(), true));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork1.id(), dst.deviceId(), dst.port(),
                                  new DefaultPort(dstVirtualDevice, dst.port(), true));

        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
        manager.createVirtualLink(virtualNetwork1.id(), src, dst);
    }

    /**
     * Tests add and remove of virtual ports.
     */
    @Test
    public void testAddRemoveVirtualPort() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork1 = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice =
                manager.createVirtualDevice(virtualNetwork1.id(), DID1);
        Port port = new DefaultPort(virtualDevice, PortNumber.portNumber(1), true);

        manager.createVirtualPort(virtualNetwork1.id(), virtualDevice.id(), PortNumber.portNumber(1), port);
        manager.createVirtualPort(virtualNetwork1.id(), virtualDevice.id(), PortNumber.portNumber(2), port);

        Set<VirtualPort> virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertNotNull("The virtual port set should not be null", virtualPorts);
        assertEquals("The virtual port set size did not match.", 2, virtualPorts.size());


        for (VirtualPort virtualPort : virtualPorts) {
            manager.removeVirtualPort(virtualNetwork1.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
            // attempt to remove the same virtual port again.
            manager.removeVirtualPort(virtualNetwork1.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
        }
        virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());

        // Add/remove the virtual port again.
        VirtualPort virtualPort = manager.createVirtualPort(virtualNetwork1.id(), virtualDevice.id(),
                                                            PortNumber.portNumber(1), port);
        manager.removeVirtualPort(virtualNetwork1.id(), (DeviceId) virtualPort.element().id(), virtualPort.number());
        virtualPorts = manager.getVirtualPorts(virtualNetwork1.id(), virtualDevice.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());
    }

    /**
     * Tests the addOrUpdateIntent() method in the store with a null intent.
     */
    @Test(expected = NullPointerException.class)
    public void testAddOrUpdateNullIntent() {
        manager.store.addOrUpdateIntent(null, null);
    }

    /**
     * Tests the removeIntent() method in the store with a null intent key.
     */
    @Test(expected = NullPointerException.class)
    public void testRemoveNullIntentKey() {
        manager.store.removeIntent(null);
    }

    /**
     * Tests the addOrUpdateIntent(), getIntents(), getIntent(), removeIntent() methods with the store.
     */
    @Test
    public void testAddOrUpdateIntent() {
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

        // Add the intent to the store.
        manager.store.addOrUpdateIntent(virtualIntent, IntentState.INSTALL_REQ);
        assertEquals("The intent size should match.", 1, manager.store.getIntents().size());
        assertNotNull("The intent should not be null.", manager.store.getIntent(virtualIntent.key()));

        // remove the intent from the store.
        manager.store.removeIntent(virtualIntent.key());
        assertTrue("The intents should be empty.", manager.store.getIntents().isEmpty());
        assertNull("The intent should be null.", manager.store.getIntent(virtualIntent.key()));
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
        assertEquals("The tunnels size should match.", 1, manager.store.getTunnelIds(virtualIntent).size());

        // Remove the intent to tunnelID mapping from the store.
        manager.store.removeTunnelId(virtualIntent, tunnelId);
        assertTrue("The tunnels should be empty.", manager.store.getTunnelIds(virtualIntent).isEmpty());
    }


    /**
     * Method to create the virtual network for further testing.
     **/
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
        VirtualDevice virtualDevice5 =
                manager.createVirtualDevice(virtualNetwork.id(), DID5);

        ConnectPoint cp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(1),
                                  new DefaultPort(virtualDevice1, PortNumber.portNumber(1), true));

        ConnectPoint cp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(2),
                                  new DefaultPort(virtualDevice1, PortNumber.portNumber(2), true));

        ConnectPoint cp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(3),
                                  new DefaultPort(virtualDevice2, PortNumber.portNumber(3), true));

        ConnectPoint cp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(4),
                                  new DefaultPort(virtualDevice2, PortNumber.portNumber(4), true));

        ConnectPoint cp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(5),
                                  new DefaultPort(virtualDevice3, PortNumber.portNumber(5), true));

        cp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(6),
                                  new DefaultPort(virtualDevice3, PortNumber.portNumber(6), true));

        cp7 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(7));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(7),
                                  new DefaultPort(virtualDevice4, PortNumber.portNumber(7), true));

        ConnectPoint cp8 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(8));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(8),
                                  new DefaultPort(virtualDevice4, PortNumber.portNumber(8), true));

        ConnectPoint cp9 = new ConnectPoint(virtualDevice5.id(), PortNumber.portNumber(9));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice5.id(),
                                  PortNumber.portNumber(9),
                                  new DefaultPort(virtualDevice5, PortNumber.portNumber(9), true));

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
        topologyProvider = new VirtualNetworkTopologyProvider();
        topologyProvider.topologyService = topologyService;

        return virtualNetwork;
    }

    /**
     * Test the topologyChanged() method.
     */
    @Test
    public void testTopologyChanged() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();
        VirtualNetworkProviderService providerService = manager.createProviderService(topologyProvider);

        // Initial setup is two clusters of devices/links.
        assertEquals("The cluster count did not match.", 2, topologyService.currentTopology().clusterCount());

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
            assertTrue("The virtual link should be active.", virtualLink.state().equals(Link.State.ACTIVE));
        });

        virtualNetworkManagerStore.updateLink(link, link.tunnelId(), Link.State.INACTIVE);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.INACTIVE);
        providerService.topologyChanged(topologyProvider.getConnectPoints(topology));

        // Validate that all links are active again.
        manager.getVirtualLinks(virtualNetwork.id()).forEach(virtualLink -> {
            assertTrue("The virtual link should be active.", virtualLink.state().equals(Link.State.ACTIVE));
        });
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
