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

package org.onosproject.incubator.store.routing.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTableId;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalRouteStore extends AbstractStore<RouteEvent, RouteStoreDelegate>
        implements RouteStore {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<RouteTableId, RouteTable> routeTables;
    private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    private static final RouteTableId IPV6 = new RouteTableId("ipv6");

    private Map<IpAddress, NextHopData> nextHops = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        routeTables = new ConcurrentHashMap<>();

        routeTables.put(IPV4, new RouteTable());
        routeTables.put(IPV6, new RouteTable());
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void removeRoute(Route route) {
        RouteTable table = getDefaultRouteTable(route);
        table.remove(route);
        Collection<Route> routes = table.getRoutesForNextHop(route.nextHop());

        if (routes.isEmpty()) {
            nextHops.remove(route.nextHop());
        }
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<Route> getRoutes(RouteTableId table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    @Override
    public Route longestPrefixMatch(IpAddress ip) {
        return getDefaultRouteTable(ip).longestPrefixMatch(ip);
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return getDefaultRouteTable(ip).getRoutesForNextHop(ip);
    }

    @Override
    public void updateNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        Collection<Route> routes = getDefaultRouteTable(ip).getRoutesForNextHop(ip);

        if (!routes.isEmpty() && !nextHopData.equals(nextHops.get(ip))) {
            NextHopData oldNextHopData = nextHops.put(ip, nextHopData);

            for (Route route : routes) {
                if (oldNextHopData == null) {
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                            new ResolvedRoute(route, nextHopData.mac(), nextHopData.location())));
                } else {
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                            new ResolvedRoute(route, nextHopData.mac(), nextHopData.location()),
                            new ResolvedRoute(route, oldNextHopData.mac(), oldNextHopData.location())));
                }
            }
        }
    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        if (nextHops.remove(ip, nextHopData)) {
            Collection<Route> routes = getDefaultRouteTable(ip).getRoutesForNextHop(ip);
            for (Route route : routes) {
                notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                        new ResolvedRoute(route, nextHopData.mac(), nextHopData.location())));
            }
        }
    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        return nextHops.get(ip);
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        return ImmutableMap.copyOf(nextHops);
    }

    private RouteTable getDefaultRouteTable(Route route) {
        return getDefaultRouteTable(route.prefix().address());
    }

    private RouteTable getDefaultRouteTable(IpAddress ip) {
        RouteTableId routeTableId = (ip.isIp4()) ? IPV4 : IPV6;
        return routeTables.get(routeTableId);
    }

    private static String createBinaryString(IpPrefix ipPrefix) {
        byte[] octets = ipPrefix.address().toOctets();
        StringBuilder result = new StringBuilder(ipPrefix.prefixLength());
        result.append("0");
        for (int i = 0; i < ipPrefix.prefixLength(); i++) {
            int byteOffset = i / Byte.SIZE;
            int bitOffset = i % Byte.SIZE;
            int mask = 1 << (Byte.SIZE - 1 - bitOffset);
            byte value = octets[byteOffset];
            boolean isSet = ((value & mask) != 0);
            result.append(isSet ? "1" : "0");
        }

        return result.toString();
    }

    /**
     * Route table into which routes can be placed.
     */
    private class RouteTable {
        private final InvertedRadixTree<Route> routeTable;

        private final Map<IpPrefix, Route> routes = new ConcurrentHashMap<>();
        private final Multimap<IpAddress, Route> reverseIndex =
                Multimaps.synchronizedMultimap(HashMultimap.create());

        /**
         * Creates a new route table.
         */
        public RouteTable() {
            routeTable = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());
        }

        /**
         * Adds or updates the route in the route table.
         *
         * @param route route to update
         */
        public void update(Route route) {
            synchronized (this) {
                Route oldRoute = routes.put(route.prefix(), route);

                // No need to proceed if the new route is the same
                if (route.equals(oldRoute)) {
                    return;
                }

                NextHopData oldNextHopData = null;
                ResolvedRoute oldResolvedRoute = null;
                if (oldRoute != null) {
                    oldNextHopData = nextHops.get(oldRoute.nextHop());
                    if (oldNextHopData != null) {
                        oldResolvedRoute = new ResolvedRoute(oldRoute,
                                oldNextHopData.mac(), oldNextHopData.location());
                    }
                }

                routeTable.put(createBinaryString(route.prefix()), route);

                // TODO manage routes from multiple providers

                reverseIndex.put(route.nextHop(), route);

                if (oldRoute != null) {
                    reverseIndex.remove(oldRoute.nextHop(), oldRoute);

                    if (reverseIndex.get(oldRoute.nextHop()).isEmpty()) {
                        nextHops.remove(oldRoute.nextHop());
                    }
                }

                NextHopData nextHopData = nextHops.get(route.nextHop());

                if (oldRoute != null && !oldRoute.nextHop().equals(route.nextHop())) {
                    // We don't know the new MAC address yet so delete the route
                    // Don't send ROUTE_REMOVED if the route was unresolved
                    if (nextHopData == null && oldNextHopData != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                oldResolvedRoute));
                    // We know the new MAC address so update the route
                    } else if (nextHopData != null && oldNextHopData != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                new ResolvedRoute(route, nextHopData.mac(), nextHopData.location()),
                                oldResolvedRoute));
                    }
                    return;
                }

                if (nextHopData != null) {
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                            new ResolvedRoute(route, nextHopData.mac(), nextHopData.location())));
                }
            }
        }

        /**
         * Removes the route from the route table.
         *
         * @param route route to remove
         */
        public void remove(Route route) {
            synchronized (this) {
                Route removed = routes.remove(route.prefix());
                routeTable.remove(createBinaryString(route.prefix()));

                if (removed != null) {
                    reverseIndex.remove(removed.nextHop(), removed);
                    NextHopData oldNextHopData = getNextHop(removed.nextHop());
                    // Don't send ROUTE_REMOVED if the route was unresolved
                    if (oldNextHopData != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                new ResolvedRoute(route, oldNextHopData.mac(),
                                        oldNextHopData.location())));
                    }
                }
            }
        }

        /**
         * Returns the routes pointing to a particular next hop.
         *
         * @param ip next hop IP address
         * @return routes for the next hop
         */
        public Collection<Route> getRoutesForNextHop(IpAddress ip) {
            return reverseIndex.get(ip);
        }

        /**
         * Returns all routes in the route table.
         *
         * @return all routes
         */
        public Collection<Route> getRoutes() {
            Iterator<KeyValuePair<Route>> it =
                    routeTable.getKeyValuePairsForKeysStartingWith("").iterator();

            List<Route> routes = new LinkedList<>();

            while (it.hasNext()) {
                KeyValuePair<Route> entry = it.next();
                routes.add(entry.getValue());
            }

            return routes;
        }

        /**
         * Performs a longest prefix match with the given IP in the route table.
         *
         * @param ip IP address to look up
         * @return most specific prefix containing the given
         */
        public Route longestPrefixMatch(IpAddress ip) {
            Iterable<Route> prefixes =
                    routeTable.getValuesForKeysPrefixing(createBinaryString(ip.toIpPrefix()));

            Iterator<Route> it = prefixes.iterator();

            Route route = null;
            while (it.hasNext()) {
                route = it.next();
            }

            return route;
        }
    }

}
