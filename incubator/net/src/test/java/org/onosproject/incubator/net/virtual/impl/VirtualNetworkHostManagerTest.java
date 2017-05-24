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

import com.google.common.collect.Iterators;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.host.HostService;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkHostService.
 */
public class VirtualNetworkHostManagerTest extends TestDeviceParams {
    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private TestServiceDirectory testDirectory;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.coreService = coreService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());

        testDirectory = new TestServiceDirectory();
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Sets up a virtual network with hosts.
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

        ConnectPoint hostCp1 = new ConnectPoint(DID1, P1);
        ConnectPoint hostCp2 = new ConnectPoint(DID2, P2);
        manager.createVirtualPort(virtualNetwork.id(), hostCp1.deviceId(), hostCp1.port(),
                new ConnectPoint(virtualDevice1.id(), hostCp1.port()));
        manager.createVirtualPort(virtualNetwork.id(), hostCp2.deviceId(), hostCp2.port(),
                new ConnectPoint(virtualDevice2.id(), hostCp2.port()));

        manager.createVirtualHost(virtualNetwork.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);
        manager.createVirtualHost(virtualNetwork.id(), HID2, MAC2, VLAN2, LOC2, IPSET2);
        return virtualNetwork;
    }

    /**
     * Sets up a virtual network with no hosts.
     *
     * @return virtual network
     */
    private VirtualNetwork setupEmptyVnet() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);

        ConnectPoint hostCp1 = new ConnectPoint(DID1, P1);
        ConnectPoint hostCp2 = new ConnectPoint(DID2, P2);
        manager.createVirtualPort(virtualNetwork.id(), hostCp1.deviceId(), hostCp1.port(),
                new ConnectPoint(virtualDevice1.id(), hostCp1.port()));
        manager.createVirtualPort(virtualNetwork.id(), hostCp2.deviceId(), hostCp2.port(),
                new ConnectPoint(virtualDevice2.id(), hostCp2.port()));

        return virtualNetwork;
    }

    /**
     * Tests the getHosts(), getHost(), getHostsByXX(), getConnectedHosts() methods
     * on a non-empty virtual network.
     */
    @Test
    public void testGetHostsOnNonEmptyVnet() {
        VirtualNetwork virtualNetwork = setupEmptyVnet();
        VirtualHost vhost1 = manager.createVirtualHost(virtualNetwork.id(), HID1, MAC1, VLAN1, LOC1, IPSET1);
        VirtualHost vhost2 = manager.createVirtualHost(virtualNetwork.id(), HID2, MAC2, VLAN2, LOC2, IPSET2);
        HostService hostService = manager.get(virtualNetwork.id(), HostService.class);

        // test the getHosts() and getHostCount() methods
        Iterator<Host> itHosts = hostService.getHosts().iterator();
        assertEquals("The host set size did not match.", 2, Iterators.size(itHosts));
        assertEquals("The host count did not match.", 2, hostService.getHostCount());

        // test the getHost() method
        Host testHost = hostService.getHost(HID2);
        assertEquals("The expected host did not match.", vhost2, testHost);

        // test the getHostsByVlan(...) method
        Collection<Host> collHost = hostService.getHostsByVlan(VLAN1);
        assertEquals("The host set size did not match.", 1, collHost.size());
        assertTrue("The host did not match.", collHost.contains(vhost1));

        // test the getHostsByMac(...) method
        collHost = hostService.getHostsByMac(MAC2);
        assertEquals("The host set size did not match.", 1, collHost.size());
        assertTrue("The host did not match.", collHost.contains(vhost2));

        // test the getHostsByIp(...) method
        collHost = hostService.getHostsByIp(IP1);
        assertEquals("The host set size did not match.", 2, collHost.size());
        collHost = hostService.getHostsByIp(IP2);
        assertEquals("The host set size did not match.", 1, collHost.size());
        assertTrue("The host did not match.", collHost.contains(vhost1));

        // test the getConnectedHosts(ConnectPoint) method
        collHost = hostService.getConnectedHosts(LOC1);
        assertEquals("The host set size did not match.", 1, collHost.size());
        assertTrue("The host did not match.", collHost.contains(vhost1));

        // test the getConnectedHosts(DeviceId) method
        collHost = hostService.getConnectedHosts(DID2);
        assertEquals("The host set size did not match.", 1, collHost.size());
        assertTrue("The host did not match.", collHost.contains(vhost2));
    }

    /**
     * Tests the getHosts(), getHost(), getHostsByXX(), getConnectedHosts() methods
     * on an empty virtual network.
     */
    @Test
    public void testGetHostsOnEmptyVnet() {
        VirtualNetwork virtualNetwork = setupEmptyVnet();
        HostService hostService = manager.get(virtualNetwork.id(), HostService.class);

        // test the getHosts() and getHostCount() methods
        Iterator<Host> itHosts = hostService.getHosts().iterator();
        assertEquals("The host set size did not match.", 0, Iterators.size(itHosts));
        assertEquals("The host count did not match.", 0, hostService.getHostCount());

        // test the getHost() method
        Host testHost = hostService.getHost(HID2);
        assertNull("The host should be null.", testHost);

        // test the getHostsByVlan(...) method
        Collection<Host> collHost = hostService.getHostsByVlan(VLAN1);
        assertEquals("The host set size did not match.", 0, collHost.size());

        // test the getHostsByMac(...) method
        collHost = hostService.getHostsByMac(MAC2);
        assertEquals("The host set size did not match.", 0, collHost.size());

        // test the getHostsByIp(...) method
        collHost = hostService.getHostsByIp(IP1);
        assertEquals("The host set size did not match.", 0, collHost.size());

        // test the getConnectedHosts(ConnectPoint) method
        collHost = hostService.getConnectedHosts(LOC1);
        assertEquals("The host set size did not match.", 0, collHost.size());

        // test the getConnectedHosts(DeviceId) method
        collHost = hostService.getConnectedHosts(DID2);
        assertEquals("The host set size did not match.", 0, collHost.size());
    }

    /**
     * Tests querying for a host using a null host identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetHostByNullId() {
        VirtualNetwork vnet = setupEmptyVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getHost(null);
    }

    /**
     * Tests querying for hosts with null mac.
     */
    @Test(expected = NullPointerException.class)
    public void testGetHostsByNullMac() {
        VirtualNetwork vnet = setupEmptyVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getHostsByMac(null);
    }

    /**
     * Tests querying for hosts with null vlan.
     */
    @Test(expected = NullPointerException.class)
    public void testGetHostsByNullVlan() {
        VirtualNetwork vnet = setupEmptyVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getHostsByVlan(null);
    }

    /**
     * Tests querying for hosts with null ip.
     */
    @Test(expected = NullPointerException.class)
    public void testGetHostsByNullIp() {
        VirtualNetwork vnet = setupVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getHostsByIp(null);
    }

    /**
     * Tests querying for connected hosts with null host location (connect point).
     */
    @Test(expected = NullPointerException.class)
    public void testGetConnectedHostsByNullLoc() {
        VirtualNetwork vnet = setupEmptyVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getConnectedHosts((ConnectPoint) null);
    }

    /**
     * Tests querying for connected hosts with null device id.
     */
    @Test(expected = NullPointerException.class)
    public void testGetConnectedHostsByNullDeviceId() {
        VirtualNetwork vnet = setupVnet();
        HostService hostService = manager.get(vnet.id(), HostService.class);

        hostService.getConnectedHosts((DeviceId) null);
    }

}
