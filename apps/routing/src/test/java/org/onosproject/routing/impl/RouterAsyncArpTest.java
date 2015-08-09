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
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.onosproject.routing.impl.Router.InternalHostListener;
import org.onosproject.routing.BgpService;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RouteListener;
import org.onosproject.routing.RouteUpdate;

import java.util.Collections;

import static org.easymock.EasyMock.*;

/**
* This class tests adding a route and updating a route.
* The HostService module answers the MAC address asynchronously.
*/
public class RouterAsyncArpTest {

    private HostService hostService;
    private FibListener fibListener;
    private RoutingConfigurationService routingConfigurationService;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private Router router;
    private InternalHostListener internalHostListener;

    @Before
    public void setUp() throws Exception {
        hostService = createMock(HostService.class);
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

        internalHostListener = router.new InternalHostListener();
    }

    @After
    public void tearDown() {
        // Called during shutdown
        reset(hostService);
        hostService.removeListener(anyObject(HostListener.class));

        router.stop();
    }

    /**
     * Tests adding a route entry with asynchronous HostService replies.
     */
    @Test
    public void testRouteAdd() {
        // Construct a route entry
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        IpAddress nextHopIp = Ip4Address.valueOf("192.168.10.1");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHopIp);

        // Host service will reply with no hosts when asked
        reset(hostService);
        expect(hostService.getHostsByIp(anyObject(IpAddress.class))).andReturn(
                Collections.emptySet()).anyTimes();
        hostService.startMonitoringIp(IpAddress.valueOf("192.168.10.1"));
        replay(hostService);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(false);
        replay(routingConfigurationService);

        // Initially when we add the route, no FIB update will be sent
        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntry)));

        verify(fibListener);


        // Now when we send the event, we expect the FIB update to be sent
        reset(fibListener);
        FibEntry fibEntry = new FibEntry(prefix, nextHopIp,
                                         MacAddress.valueOf("00:00:00:00:00:01"));

        fibListener.update(Collections.singletonList(new FibUpdate(
                FibUpdate.Type.UPDATE, fibEntry)), Collections.emptyList());
        replay(fibListener);

        Host host = new DefaultHost(ProviderId.NONE, HostId.NONE,
                                    MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                                    new HostLocation(
                                            SW1_ETH1.deviceId(),
                                            SW1_ETH1.port(), 1),
                                    Sets.newHashSet(IpAddress.valueOf("192.168.10.1")));

        // Send in the host event
        internalHostListener.event(
                new HostEvent(HostEvent.Type.HOST_ADDED, host));

        verify(fibListener);
    }

    /**
     * Tests updating a route entry with asynchronous HostService replies.
     */
    @Test
    public void testRouteUpdate() {
        // Add a route
        testRouteAdd();

        // Construct a route entry
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        IpAddress nextHopIp = Ip4Address.valueOf("192.168.20.1");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHopIp);

        // Host service will reply with no hosts when asked
        reset(hostService);
        expect(hostService.getHostsByIp(anyObject(IpAddress.class))).andReturn(
                Collections.emptySet()).anyTimes();
        hostService.startMonitoringIp(IpAddress.valueOf("192.168.20.1"));
        replay(hostService);

        reset(routingConfigurationService);
        expect(routingConfigurationService.isIpPrefixLocal(
                anyObject(IpPrefix.class))).andReturn(false);
        replay(routingConfigurationService);

        // Initially when we add the route, the DELETE FIB update will be sent
        // but the UPDATE FIB update will come later when the MAC is resolved
        reset(fibListener);

        fibListener.update(Collections.emptyList(), Collections.singletonList(new FibUpdate(
                FibUpdate.Type.DELETE, new FibEntry(prefix, null, null))));
        replay(fibListener);

        router.processRouteUpdates(Collections.singletonList(
                new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntry)));

        verify(fibListener);


        // Now when we send the event, we expect the FIB update to be sent
        reset(fibListener);
        FibEntry fibEntry = new FibEntry(prefix, nextHopIp,
                                         MacAddress.valueOf("00:00:00:00:00:02"));

        fibListener.update(Collections.singletonList(new FibUpdate(
                FibUpdate.Type.UPDATE, fibEntry)), Collections.emptyList());
        replay(fibListener);

        Host host = new DefaultHost(ProviderId.NONE, HostId.NONE,
                                    MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                                    new HostLocation(
                                            SW1_ETH1.deviceId(),
                                            SW1_ETH1.port(), 1),
                                    Sets.newHashSet(IpAddress.valueOf("192.168.20.1")));

        // Send in the host event
        internalHostListener.event(
                new HostEvent(HostEvent.Type.HOST_ADDED, host));

        verify(fibListener);
    }
}
