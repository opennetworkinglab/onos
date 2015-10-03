/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.impl;

import com.google.common.collect.Sets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routing.BgpService;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RouteListener;
import org.onosproject.routing.RouteUpdate;
import org.onosproject.routing.config.RoutingConfigurationService;

import java.util.Collections;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests adding a route, updating a route, deleting a route,
 * and adding a route whose next hop is the local BGP speaker.
 * <p/>
 * The HostService answers requests synchronously.
 */
public class RouterTest {

    private HostService hostService;
    private RoutingConfigurationService routingConfigurationService;

    private FibListener fibListener;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW4_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000004"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW5_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000005"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW6_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000006"),
            PortNumber.portNumber(1));
    private Router router;

    @Before
    public void setUp() throws Exception {
        setUpHostService();
        routingConfigurationService =
                createMock(RoutingConfigurationService.class);

        BgpService bgpService = createMock(BgpService.class);
        bgpService.start(anyObject(RouteListener.class));
        bgpService.stop();
        replay(bgpService);

        fibListener = createMock(FibListener.class);

        router = new Router();
        router.coreService = createNiceMock(CoreService.class);
        router.hostService = hostService;
        router.routingConfigurationService = routingConfigurationService;
        router.bgpService = bgpService;
        router.activate();

        router.addFibListener(fibListener);
        router.start();
    }

    @After
    public void tearDown() {
        router.stop();
    }

    /**
     * Sets up the host service with details of some hosts.
     */
    private void setUpHostService() {
        hostService = createMock(HostService.class);

        hostService.addListener(anyObject(HostListener.class));
        expectLastCall().anyTimes();

        IpAddress host1Address = IpAddress.valueOf("192.168.10.1");
        Host host1 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                new HostLocation(SW1_ETH1, 1),
                Sets.newHashSet(host1Address));

        expect(hostService.getHostsByIp(host1Address))
                .andReturn(Sets.newHashSet(host1)).anyTimes();
        hostService.startMonitoringIp(host1Address);
        expectLastCall().anyTimes();

        IpAddress host2Address = IpAddress.valueOf("192.168.20.1");
        Host host2 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                new HostLocation(SW2_ETH1, 1),
                Sets.newHashSet(host2Address));

        expect(hostService.getHostsByIp(host2Address))
                .andReturn(Sets.newHashSet(host2)).anyTimes();
        hostService.startMonitoringIp(host2Address);
        expectLastCall().anyTimes();

        // Next hop on a VLAN
        IpAddress host3Address = IpAddress.valueOf("192.168.40.1");
        Host host3 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:03"), VlanId.vlanId((short) 1),
                new HostLocation(SW3_ETH1, 1),
                Sets.newHashSet(host3Address));

        expect(hostService.getHostsByIp(host3Address))
                .andReturn(Sets.newHashSet(host3)).anyTimes();
        hostService.startMonitoringIp(host3Address);
        expectLastCall().anyTimes();

        IpAddress host4Address = IpAddress.valueOf("1000::1");
        Host host4 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                 MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE,
                 new HostLocation(SW4_ETH1, 1),
                 Sets.newHashSet(host4Address));

        expect(hostService.getHostsByIp(host4Address))
                .andReturn(Sets.newHashSet(host4)).anyTimes();
        hostService.startMonitoringIp(host4Address);
        expectLastCall().anyTimes();

        IpAddress host5Address = IpAddress.valueOf("2000::1");
        Host host5 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                 MacAddress.valueOf("00:00:00:00:00:05"), VlanId.NONE,
                 new HostLocation(SW5_ETH1, 1),
                 Sets.newHashSet(host5Address));

        expect(hostService.getHostsByIp(host5Address))
                .andReturn(Sets.newHashSet(host5)).anyTimes();
        hostService.startMonitoringIp(host5Address);
        expectLastCall().anyTimes();

        // Next hop on a VLAN
        IpAddress host6Address = IpAddress.valueOf("3000::1");
        Host host6 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                 MacAddress.valueOf("00:00:00:00:00:06"), VlanId.vlanId((short) 1),
                 new HostLocation(SW6_ETH1, 1),
                 Sets.newHashSet(host6Address));

        expect(hostService.getHostsByIp(host6Address))
                .andReturn(Sets.newHashSet(host6)).anyTimes();
        hostService.startMonitoringIp(host6Address);
        expectLastCall().anyTimes();


        // Called during shutdown
        hostService.removeListener(anyObject(HostListener.class));

        replay(hostService);
    }

    /**
     * Tests adding a IPv4 route entry.
     */
    @Test
    public void testIpv4RouteAdd() {
        // Construct a route entry
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        IpAddress nextHopIp = Ip4Address.valueOf("192.168.10.1");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHopIp);

        // Expected FIB entry
        FibEntry fibEntry = new FibEntry(prefix, nextHopIp,
                MacAddress.valueOf("00:00:00:00:00:01"));

        fibListener.update(Collections.singletonList(new FibUpdate(
                FibUpdate.Type.UPDATE, fibEntry)), Collections.emptyList());

        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntry)));

        verify(fibListener);
    }


    /**
     * Tests adding a IPv6 route entry.
     */
    @Test
    public void testIpv6RouteAdd() {
        // Construct a route entry
        IpPrefix prefix = Ip6Prefix.valueOf("4000::/64");
        IpAddress nextHopIp = Ip6Address.valueOf("1000::1");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHopIp);

        // Expected FIB entry
        FibEntry fibEntry = new FibEntry(prefix, nextHopIp,
                MacAddress.valueOf("00:00:00:00:00:04"));

        fibListener.update(Collections.singletonList(new FibUpdate(
                FibUpdate.Type.UPDATE, fibEntry)), Collections.emptyList());

        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntry)));

        verify(fibListener);
    }


    /**
     * Tests updating a IPv4 route entry.
     */
    @Test
    public void testRouteUpdate() {
        // Firstly add a route
        testIpv4RouteAdd();

        // Route entry with updated next hop for the original prefix
        RouteEntry routeEntryUpdate = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        // The old FIB entry will be withdrawn
        FibEntry withdrawFibEntry = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"), null, null);

        // A new FIB entry will be added
        FibEntry updateFibEntry = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.20.1"),
                MacAddress.valueOf("00:00:00:00:00:02"));

        reset(fibListener);
        fibListener.update(Collections.singletonList(new FibUpdate(
                                    FibUpdate.Type.UPDATE, updateFibEntry)),
                           Collections.singletonList(new FibUpdate(
                                    FibUpdate.Type.DELETE, withdrawFibEntry)));
        replay(fibListener);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(false);
        replay(routingConfigurationService);

        router.processRouteUpdates(Collections.singletonList(new RouteUpdate(
                RouteUpdate.Type.UPDATE, routeEntryUpdate)));

        verify(fibListener);
    }

    /**
     * Tests updating a IPv6 route entry.
     */
    @Test
    public void testIpv6RouteUpdate() {
        // Firstly add a route
        testIpv6RouteAdd();

        // Route entry with updated next hop for the original prefix
        RouteEntry routeEntryUpdate = new RouteEntry(
                Ip6Prefix.valueOf("4000::/64"),
                Ip6Address.valueOf("2000::1"));

        // The old FIB entry will be withdrawn
        FibEntry withdrawFibEntry = new FibEntry(
                Ip6Prefix.valueOf("4000::/64"), null, null);

        // A new FIB entry will be added
        FibEntry updateFibEntry = new FibEntry(
                Ip6Prefix.valueOf("4000::/64"),
                Ip6Address.valueOf("2000::1"),
                MacAddress.valueOf("00:00:00:00:00:05"));

        reset(fibListener);
        fibListener.update(Collections.singletonList(new FibUpdate(
                                   FibUpdate.Type.UPDATE, updateFibEntry)),
                           Collections.singletonList(new FibUpdate(
                                   FibUpdate.Type.DELETE, withdrawFibEntry)));
        replay(fibListener);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(false);
        replay(routingConfigurationService);

        router.processRouteUpdates(Collections.singletonList(new RouteUpdate(
                RouteUpdate.Type.UPDATE, routeEntryUpdate)));

        verify(fibListener);
    }

    /**
     * Tests deleting a IPv4 route entry.
     */
    @Test
    public void testIpv4RouteDelete() {
        // Firstly add a route
        testIpv4RouteAdd();

        RouteEntry deleteRouteEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        FibEntry deleteFibEntry = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"), null, null);

        reset(fibListener);
        fibListener.update(Collections.emptyList(), Collections.singletonList(
                new FibUpdate(FibUpdate.Type.DELETE, deleteFibEntry)));

        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.DELETE, deleteRouteEntry)));

        verify(fibListener);
    }

    /**
     * Tests deleting a IPv6 route entry.
     */
    @Test
    public void testIpv6RouteDelete() {
        // Firstly add a route
        testIpv6RouteAdd();

        RouteEntry deleteRouteEntry = new RouteEntry(
                Ip6Prefix.valueOf("4000::/64"),
                Ip6Address.valueOf("1000::1"));

        FibEntry deleteFibEntry = new FibEntry(
                Ip6Prefix.valueOf("4000::/64"), null, null);

        reset(fibListener);
        fibListener.update(Collections.emptyList(), Collections.singletonList(
                new FibUpdate(FibUpdate.Type.DELETE, deleteFibEntry)));

        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.DELETE, deleteRouteEntry)));

        verify(fibListener);
    }

    /**
     * Tests adding a IPv4 route whose next hop is the local BGP speaker.
     */
    @Test
    public void testIpv4LocalRouteAdd() {
        // Construct a route entry, the next hop is the local BGP speaker
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("0.0.0.0"));

        // No methods on the FIB listener should be called
        replay(fibListener);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(true);
        replay(routingConfigurationService);

        // Call the processRouteUpdates() method in Router class
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                                  routeEntry);
        router.processRouteUpdates(Collections.singletonList(routeUpdate));

        // Verify
        assertEquals(1, router.getRoutes4().size());
        assertTrue(router.getRoutes4().contains(routeEntry));
        verify(fibListener);
    }

    /**
     * Tests adding a IPv6 route whose next hop is the local BGP speaker.
     */
    @Test
    public void testIpv6LocalRouteAdd() {
        // Construct a route entry, the next hop is the local BGP speaker
        RouteEntry routeEntry = new RouteEntry(
                Ip6Prefix.valueOf("4000::/64"),
                Ip6Address.valueOf("::"));

        // No methods on the FIB listener should be called
        replay(fibListener);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(true);
        replay(routingConfigurationService);

        // Call the processRouteUpdates() method in Router class
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                                  routeEntry);
        router.processRouteUpdates(Collections.singletonList(routeUpdate));

        // Verify
        assertEquals(1, router.getRoutes6().size());
        assertTrue(router.getRoutes6().contains(routeEntry));
        verify(fibListener);
    }
}
