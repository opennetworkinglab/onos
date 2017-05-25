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

package org.onosproject.incubator.net.routing.impl;

import com.google.common.collect.Sets;
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
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.store.routing.impl.LocalRouteStore;
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
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WorkQueue;

import java.util.Collections;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * Unit tests for the route manager.
 */
public class RouteManagerTest {

    private static final ConnectPoint CP1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final IpPrefix V4_PREFIX1 = Ip4Prefix.valueOf("1.1.1.0/24");
    private static final IpPrefix V4_PREFIX2 = Ip4Prefix.valueOf("2.2.2.0/24");
    private static final IpPrefix V6_PREFIX1 = Ip6Prefix.valueOf("4000::/64");

    private static final IpAddress V4_NEXT_HOP1 = Ip4Address.valueOf("192.168.10.1");
    private static final IpAddress V4_NEXT_HOP2 = Ip4Address.valueOf("192.168.20.1");
    private static final IpAddress V6_NEXT_HOP1 = Ip6Address.valueOf("1000::1");
    private static final IpAddress V6_NEXT_HOP2 = Ip6Address.valueOf("2000::1");

    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:00:00:00:04");

    private HostService hostService;

    private RouteListener routeListener;
    private HostListener hostListener;

    private RouteManager routeManager;

    @Before
    public void setUp() throws Exception {
        setUpHostService();

        routeListener = createMock(RouteListener.class);

        routeManager = new TestRouteManager();
        routeManager.hostService = hostService;

        routeManager.clusterService = createNiceMock(ClusterService.class);
        replay(routeManager.clusterService);
        routeManager.storageService = createNiceMock(StorageService.class);
        expect(routeManager.storageService.getWorkQueue(anyString(), anyObject()))
                .andReturn(createNiceMock(WorkQueue.class));
        replay(routeManager.storageService);

        LocalRouteStore routeStore = new LocalRouteStore();
        routeStore.activate();
        routeManager.routeStore = routeStore;
        routeManager.activate();

        routeManager.addListener(routeListener);
    }

    /**
     * Sets up the host service with details of some hosts.
     */
    private void setUpHostService() {
        hostService = createMock(HostService.class);

        hostService.addListener(anyObject(HostListener.class));
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        Host host1 = createHost(MAC1, V4_NEXT_HOP1);
        expectHost(host1);

        Host host2 = createHost(MAC2, V4_NEXT_HOP2);
        expectHost(host2);

        Host host3 = createHost(MAC3, V6_NEXT_HOP1);
        expectHost(host3);

        Host host4 = createHost(MAC4, V6_NEXT_HOP2);
        expectHost(host4);

        replay(hostService);
    }

    /**
     * Sets expectations on the host service for a given host.
     *
     * @param host host
     */
    private void expectHost(Host host) {
        // Assume the host only has one IP address
        IpAddress ip = host.ipAddresses().iterator().next();

        expect(hostService.getHostsByIp(ip))
                .andReturn(Sets.newHashSet(host)).anyTimes();
        hostService.startMonitoringIp(ip);
        expectLastCall().anyTimes();
    }

    /**
     * Creates a host with the given parameters.
     *
     * @param macAddress MAC address
     * @param ipAddress IP address
     * @return new host
     */
    private Host createHost(MacAddress macAddress, IpAddress ipAddress) {
        return new DefaultHost(ProviderId.NONE, HostId.NONE, macAddress,
                VlanId.NONE, new HostLocation(CP1, 1),
                Sets.newHashSet(ipAddress));
    }

    /**
     * Adds a route to the route service without expecting any specific events
     * to be sent to the route listeners.
     *
     * @param route route to add
     */
    private void addRoute(Route route) {
        reset(routeListener);

        routeListener.event(anyObject(RouteEvent.class));
        expectLastCall().once();
        replay(routeListener);

        routeManager.update(Collections.singleton(route));

        reset(routeListener);
    }

    /**
     * Tests adding routes to the route manager.
     */
    @Test
    public void testRouteAdd() {
        Route route = new Route(Route.Source.STATIC, V4_PREFIX1, V4_NEXT_HOP1);
        ResolvedRoute resolvedRoute = new ResolvedRoute(route, MAC1, CP1);

        verifyRouteAdd(route, resolvedRoute);

        route = new Route(Route.Source.STATIC, V6_PREFIX1, V6_NEXT_HOP1);
        resolvedRoute = new ResolvedRoute(route, MAC3, CP1);

        verifyRouteAdd(route, resolvedRoute);
    }

    /**
     * Tests adding a new route and verifies that the correct event was sent
     * to the route listener.
     *
     * @param route route to add
     * @param resolvedRoute resolved route that should be sent to the route
     *                      listener
     */
    private void verifyRouteAdd(Route route, ResolvedRoute resolvedRoute) {
        reset(routeListener);

        routeListener.event(event(RouteEvent.Type.ROUTE_ADDED, resolvedRoute));

        replay(routeListener);

        routeManager.update(Collections.singleton(route));

        verify(routeListener);
    }

    /**
     * Tests updating routes in the route manager.
     */
    @Test
    public void testRouteUpdate() {
        Route route = new Route(Route.Source.STATIC, V4_PREFIX1, V4_NEXT_HOP1);
        Route updatedRoute = new Route(Route.Source.STATIC, V4_PREFIX1, V4_NEXT_HOP2);
        ResolvedRoute resolvedRoute = new ResolvedRoute(route, MAC1, CP1);
        ResolvedRoute updatedResolvedRoute = new ResolvedRoute(updatedRoute, MAC2, CP1);

        verifyRouteUpdated(route, updatedRoute, resolvedRoute, updatedResolvedRoute);

        // Different prefix pointing to the same next hop.
        // In this case we expect to receive a ROUTE_UPDATED event.
        route = new Route(Route.Source.STATIC, V4_PREFIX2, V4_NEXT_HOP1);
        updatedRoute = new Route(Route.Source.STATIC, V4_PREFIX2, V4_NEXT_HOP2);
        resolvedRoute = new ResolvedRoute(route, MAC1, CP1);
        updatedResolvedRoute = new ResolvedRoute(updatedRoute, MAC2, CP1);

        verifyRouteUpdated(route, updatedRoute, resolvedRoute, updatedResolvedRoute);

        route = new Route(Route.Source.STATIC, V6_PREFIX1, V6_NEXT_HOP1);
        updatedRoute = new Route(Route.Source.STATIC, V6_PREFIX1, V6_NEXT_HOP2);
        resolvedRoute = new ResolvedRoute(route, MAC3, CP1);
        updatedResolvedRoute = new ResolvedRoute(updatedRoute, MAC4, CP1);

        verifyRouteUpdated(route, updatedRoute, resolvedRoute, updatedResolvedRoute);
    }

    /**
     * Tests updating a route and verifies that the route listener receives a
     * route updated event.
     *
     * @param original original route
     * @param updated updated route
     * @param resolvedRoute resolved route before update
     * @param updatedResolvedRoute resolved route that is expected to be sent to
     *                             the route listener
     */
    private void verifyRouteUpdated(Route original, Route updated,
                                    ResolvedRoute resolvedRoute,
                                    ResolvedRoute updatedResolvedRoute) {
        // First add the original route
        addRoute(original);

        routeListener.event(event(RouteEvent.Type.ROUTE_UPDATED,
                updatedResolvedRoute, resolvedRoute));
        expectLastCall().once();

        replay(routeListener);

        routeManager.update(Collections.singleton(updated));

        verify(routeListener);
    }

    /**
     * Tests deleting routes from the route manager.
     */
    @Test
    public void testRouteDelete() {
        Route route = new Route(Route.Source.STATIC, V4_PREFIX1, V4_NEXT_HOP1);
        ResolvedRoute removedResolvedRoute = new ResolvedRoute(route, MAC1, CP1);

        verifyDelete(route, removedResolvedRoute);

        route = new Route(Route.Source.STATIC, V6_PREFIX1, V6_NEXT_HOP1);
        removedResolvedRoute = new ResolvedRoute(route, MAC3, CP1);

        verifyDelete(route, removedResolvedRoute);
    }

    /**
     * Tests deleting a route and verifies that the correct event is sent to
     * the route listener.
     *
     * @param route route to delete
     * @param removedResolvedRoute the resolved route being removed
     */
    private void verifyDelete(Route route, ResolvedRoute removedResolvedRoute) {
        addRoute(route);

        RouteEvent withdrawRouteEvent = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                removedResolvedRoute);

        reset(routeListener);
        routeListener.event(withdrawRouteEvent);

        replay(routeListener);

        routeManager.withdraw(Collections.singleton(route));

        verify(routeListener);
    }

    /**
     * Tests adding a route entry where the HostService does not immediately
     * know the MAC address of the next hop, but this is learnt later.
     */
    @Test
    public void testAsyncRouteAdd() {
        Route route = new Route(Route.Source.STATIC, V4_PREFIX1, V4_NEXT_HOP1);

        // Host service will reply with no hosts when asked
        reset(hostService);
        expect(hostService.getHostsByIp(anyObject(IpAddress.class))).andReturn(
                Collections.emptySet()).anyTimes();
        hostService.startMonitoringIp(V4_NEXT_HOP1);
        expectLastCall().anyTimes();
        replay(hostService);

        // Initially when we add the route, no route event will be sent because
        // the host is not known
        replay(routeListener);

        routeManager.update(Collections.singleton(route));

        verify(routeListener);

        // Now when we send the event, we expect the FIB update to be sent
        reset(routeListener);
        routeListener.event(event(RouteEvent.Type.ROUTE_ADDED,
                new ResolvedRoute(route, MAC1, CP1)));
        replay(routeListener);

        Host host = createHost(MAC1, V4_NEXT_HOP1);

        // Set up the host service with a host
        reset(hostService);
        expect(hostService.getHostsByIp(V4_NEXT_HOP1)).andReturn(
                Collections.singleton(host)).anyTimes();
        hostService.startMonitoringIp(V4_NEXT_HOP1);
        expectLastCall().anyTimes();
        replay(hostService);

        // Send in the host event
        hostListener.event(new HostEvent(HostEvent.Type.HOST_ADDED, host));

        verify(routeListener);
    }

    private static RouteEvent event(RouteEvent.Type type, ResolvedRoute subject) {
        return event(type, subject, null);
    }

    private static RouteEvent event(RouteEvent.Type type, ResolvedRoute subject, ResolvedRoute prevSubject) {
        return new RouteEvent(type, subject, prevSubject, Collections.singleton(subject));
    }

    /**
     * Test host service that stores a reference to the host listener.
     */
    private class TestHostService extends HostServiceAdapter {
        @Override
        public void addListener(HostListener listener) {
            hostListener = listener;
        }
    }

    /**
     * Test route manager that extends the real route manager and injects a test
     * listener queue instead of the real listener queue.
     */
    private static class TestRouteManager extends RouteManager {
        @Override
        ListenerQueue createListenerQueue(RouteListener listener) {
            return new TestListenerQueue(listener);
        }
    }

    /**
     * Test listener queue that simply passes route events straight through to
     * the route listener on the caller's thread.
     */
    private static class TestListenerQueue implements ListenerQueue {

        private final RouteListener listener;

        public TestListenerQueue(RouteListener listener) {
            this.listener = listener;
        }

        @Override
        public void post(RouteEvent event) {
            listener.event(event);
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }

}
