/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.host.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.TestStorageService;

import com.google.common.collect.Sets;
import org.onosproject.store.service.Versioned;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * Tests for the ECHostStore.
 */
public class DistributedHostStoreTest {

    private DistributedHostStore ecXHostStore;

    private static final HostId HOSTID = HostId.hostId(MacAddress.valueOf("1a:1a:1a:1a:1a:1a"));
    private static final HostId HOSTID1 = HostId.hostId(MacAddress.valueOf("1a:1a:1a:1a:1a:1b"));

    private static final IpAddress IP1 = IpAddress.valueOf("10.2.0.2");
    private static final IpAddress IP2 = IpAddress.valueOf("10.2.0.3");

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PID2 = new ProviderId("of", "foo2");

    private static final HostDescription HOST_LEARNT =
            createHostDesc(HOSTID, Sets.newHashSet(IP1), false, Collections.emptySet());
    private static final HostDescription HOST_CONFIGURED =
            createHostDesc(HOSTID, Sets.newHashSet(IP1), true, Collections.emptySet());
    // Host with locations
    private static final DeviceId DEV1 = DeviceId.deviceId("of:0000000000000001");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final ConnectPoint CP11 = new ConnectPoint(DEV1, P1);
    private static final HostLocation HOST_LOC11 = new HostLocation(CP11, 0);
    private static final ConnectPoint CP12 = new ConnectPoint(DEV1, P2);
    private static final HostLocation HOST_LOC12 = new HostLocation(CP12, 0);
    private static final Set<HostLocation> HOST_LOCATIONS = ImmutableSet.of(HOST_LOC11, HOST_LOC12);
    private static final Set<HostLocation> HOST_LOCATION = ImmutableSet.of(HOST_LOC11);
    private static final Set<HostLocation> NONE_LOCATION = ImmutableSet.of(HostLocation.NONE);
    private static final Set<IpAddress> HOST_ADDRESS = ImmutableSet.of(IP1);
    private static final Set<IpAddress> HOST_ADDRESSES = ImmutableSet.of(IP1, IP2);
    private static final HostDescription HOST_LEARNT_WITH_LOCATIONS =
            createHostDesc(HOSTID, HOST_ADDRESS, false, HOST_LOCATIONS);
    private static final HostDescription HOST_LEARNT_WITH_ADDRESSES =
            createHostDesc(HOSTID, Sets.newHashSet(IP1, IP2), false, Collections.emptySet());
    private static final DefaultHost OLD_HOST = new DefaultHost(PID, HOSTID,
                                                                HOST_LEARNT_WITH_ADDRESSES.hwAddress(),
                                                                HOST_LEARNT_WITH_ADDRESSES.vlan(),
                                                                HOST_LEARNT_WITH_ADDRESSES.locations(),
                                                                HOST_LEARNT_WITH_ADDRESSES.ipAddress(),
                                                                HOST_LEARNT_WITH_ADDRESSES.configured(),
                                                                HOST_LEARNT_WITH_ADDRESSES.annotations());
    private static final DefaultHost NEW_HOST = new DefaultHost(PID, HOSTID,
                                                                HOST_LEARNT_WITH_ADDRESSES.hwAddress(),
                                                                HOST_LEARNT_WITH_ADDRESSES.vlan(),
                                                                HOST_LEARNT_WITH_ADDRESSES.locations(),
                                                                HOST_ADDRESS,
                                                                HOST_LEARNT_WITH_ADDRESSES.configured(),
                                                                HOST_LEARNT_WITH_ADDRESSES.annotations());
    private static final MapEvent<HostId, DefaultHost> HOST_EVENT =
            new MapEvent<>("foobar", HOSTID, new Versioned<>(NEW_HOST, 0), new Versioned<>(OLD_HOST, 0));

    @Before
    public void setUp() {
        ecXHostStore = new DistributedHostStore();

        ecXHostStore.storageService = new TestStorageService();
        ecXHostStore.activate();
    }

    @After
    public void tearDown() {
        ecXHostStore.deactivate();
    }

    /**
     * Tests the removeIp method call.
     */
    @Test
    public void testRemoveIp() {
        Set<IpAddress> ips = new HashSet<>();
        ips.add(IP1);
        ips.add(IP2);

        HostDescription description = createHostDesc(HOSTID, ips);
        ecXHostStore.createOrUpdateHost(PID, HOSTID, description, false);
        ecXHostStore.removeIp(HOSTID, IP1);
        Host host = ecXHostStore.getHost(HOSTID);

        assertFalse(host.ipAddresses().contains(IP1));
        assertTrue(host.ipAddresses().contains(IP2));
    }

    @Test
    public void testAddHostByIp() {
        Set<IpAddress> ips = new HashSet<>();
        ips.add(IP1);
        ips.add(IP2);

        HostDescription description = createHostDesc(HOSTID, ips);
        ecXHostStore.createOrUpdateHost(PID, HOSTID, description, false);

        Set<Host> hosts = ecXHostStore.getHosts(IP1);

        assertFalse(hosts.size() > 1);
        assertTrue(hosts.size() == 1);

        HostDescription description1 = createHostDesc(HOSTID1, Sets.newHashSet(IP2));
        ecXHostStore.createOrUpdateHost(PID, HOSTID1, description1, false);

        Set<Host> hosts1 = ecXHostStore.getHosts(IP2);

        assertFalse(hosts1.size() < 1);
        assertTrue(hosts1.size() == 2);
    }

    @Test
    public void testRemoveHostByIp() {
        Set<IpAddress> ips = new HashSet<>();
        ips.add(IP1);
        ips.add(IP2);

        HostDescription description = createHostDesc(HOSTID, ips);
        ecXHostStore.createOrUpdateHost(PID, HOSTID, description, false);
        ecXHostStore.removeIp(HOSTID, IP1);
        Set<Host> hosts = ecXHostStore.getHosts(IP1);
        assertTrue(hosts.size() == 0);
    }

    @Test
    public void testHostOverride() {
        Host hostInStore;
        ecXHostStore.createOrUpdateHost(PID, HOSTID, HOST_LEARNT, false);
        hostInStore = ecXHostStore.getHost(HOSTID);
        assertFalse(hostInStore.configured());
        assertEquals(PID, hostInStore.providerId());

        // Expect: configured host should override learnt host
        ecXHostStore.createOrUpdateHost(PID2, HOSTID, HOST_CONFIGURED, true);
        hostInStore = ecXHostStore.getHost(HOSTID);
        assertTrue(hostInStore.configured());
        assertEquals(PID2, hostInStore.providerId());

        // Expect: learnt host should not override configured host
        ecXHostStore.createOrUpdateHost(PID, HOSTID, HOST_LEARNT, false);
        hostInStore = ecXHostStore.getHost(HOSTID);
        assertTrue(hostInStore.configured());
        assertEquals(PID2, hostInStore.providerId());
    }

    @Test
    public void testRemoteUpdateHostsByIp() {
        // Add host in the store
        ecXHostStore.createOrUpdateHost(PID, HOSTID, HOST_LEARNT_WITH_ADDRESSES, false);

        // Expected a learnt host with an IP
        Host hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(NONE_LOCATION, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESSES, hostInHostsByIp.ipAddresses());

        // Remove one ip - simulating the update in other instances
        ecXHostStore.hostLocationTracker.event(HOST_EVENT);

        // Expected null
        hostInHostsByIp = ecXHostStore.getHosts(IP2).stream()
                .findFirst().orElse(null);
        assertNull(hostInHostsByIp);

        // Expected an host with an ip address
        hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(NONE_LOCATION, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESS, hostInHostsByIp.ipAddresses());

    }

    @Test
    public void testLocalUpdateHostsByIp() {
        // Add host in the store
        ecXHostStore.createOrUpdateHost(PID, HOSTID, HOST_LEARNT_WITH_ADDRESSES, false);

        // Expected a learnt host with an IP
        Host hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(NONE_LOCATION, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESSES, hostInHostsByIp.ipAddresses());

        // Remove one ip
        ecXHostStore.removeIp(HOSTID, IP2);

        // Expected null
        hostInHostsByIp = ecXHostStore.getHosts(IP2).stream()
                .findFirst().orElse(null);
        assertNull(hostInHostsByIp);

        // Expected an host with an ip address
        hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(NONE_LOCATION, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESS, hostInHostsByIp.ipAddresses());

    }

    @Test
    public void testUpdateLocationInHostsByIp() {
        // Add host in the store
        ecXHostStore.createOrUpdateHost(PID, HOSTID, HOST_LEARNT_WITH_LOCATIONS, false);
        Host hostInHosts = ecXHostStore.getHost(HOSTID);

        // Expected a learnt host with an IP
        assertFalse(hostInHosts.configured());
        assertEquals(HOSTID, hostInHosts.id());
        assertEquals(PID, hostInHosts.providerId());
        assertEquals(HOST_LOCATIONS, hostInHosts.locations());
        assertEquals(HOST_ADDRESS, hostInHosts.ipAddresses());
        Host hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(HOST_LOCATIONS, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESS, hostInHostsByIp.ipAddresses());

        // Remove one location
        ecXHostStore.removeLocation(HOSTID, HOST_LOC12);

        // Verify hosts is updated
        hostInHosts = ecXHostStore.getHost(HOSTID);
        assertFalse(hostInHosts.configured());
        assertEquals(HOSTID, hostInHosts.id());
        assertEquals(PID, hostInHosts.providerId());
        assertEquals(HOST_LOCATION, hostInHosts.locations());
        assertEquals(HOST_ADDRESS, hostInHosts.ipAddresses());

        // Verify hostsByIp is updated
        hostInHostsByIp = ecXHostStore.getHosts(IP1).stream()
                .findFirst().orElse(null);
        assertNotNull(hostInHostsByIp);
        assertFalse(hostInHostsByIp.configured());
        assertEquals(HOSTID, hostInHostsByIp.id());
        assertEquals(PID, hostInHostsByIp.providerId());
        assertEquals(HOST_LOCATION, hostInHostsByIp.locations());
        assertEquals(HOST_ADDRESS, hostInHostsByIp.ipAddresses());
    }


    private static HostDescription createHostDesc(HostId hostId, Set<IpAddress> ips) {
        return createHostDesc(hostId, ips, false, Collections.emptySet());
    }

    private static HostDescription createHostDesc(HostId hostId, Set<IpAddress> ips,
                                                  boolean configured, Set<HostLocation> locations) {
        return locations.isEmpty() ?
                new DefaultHostDescription(hostId.mac(), hostId.vlanId(), HostLocation.NONE, ips, configured) :
                new DefaultHostDescription(hostId.mac(), hostId.vlanId(), locations, ips, configured);

    }

}
