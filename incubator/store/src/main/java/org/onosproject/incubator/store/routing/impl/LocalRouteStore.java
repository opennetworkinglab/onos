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

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.incubator.net.routing.InternalRouteEvent;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteSet;
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.incubator.net.routing.RouteTools.createBinaryString;

/**
 * Route store based on in-memory storage.
 */
public class LocalRouteStore extends AbstractStore<InternalRouteEvent, RouteStoreDelegate>
        implements RouteStore {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<RouteTableId, RouteTable> routeTables;
    private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    private static final RouteTableId IPV6 = new RouteTableId("ipv6");

    /**
     * Sets up local route store.
     */
    public void activate() {
        routeTables = new ConcurrentHashMap<>();

        routeTables.put(IPV4, new RouteTable(IPV4));
        routeTables.put(IPV6, new RouteTable(IPV6));

        log.info("Started");
    }

    /**
     * Cleans up local route store.
     */
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void removeRoute(Route route) {
        getDefaultRouteTable(route).remove(route);
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<RouteSet> getRoutes(RouteTableId table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable != null) {
            return routeTable.getRouteSets();
        }
        return null;
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
    public RouteSet getRoutes(IpPrefix prefix) {
        return getDefaultRouteTable(prefix.address()).getRoutes(prefix);
    }

    @Override
    public void updateNextHop(IpAddress ip, NextHopData nextHopData) {
        // No longer needed
    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {
        // No longer needed
    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        // No longer needed
        return null;
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        // No longer needed
        return Collections.emptyMap();
    }

    private RouteTable getDefaultRouteTable(Route route) {
        return getDefaultRouteTable(route.prefix().address());
    }

    private RouteTable getDefaultRouteTable(IpAddress ip) {
        RouteTableId routeTableId = (ip.isIp4()) ? IPV4 : IPV6;
        return routeTables.get(routeTableId);
    }

    /**
     * Route table into which routes can be placed.
     */
    private class RouteTable {
        private final InvertedRadixTree<Route> routeTable;
        private final Map<IpPrefix, Route> routes = new ConcurrentHashMap<>();
        private final RouteTableId id;

        /**
         * Creates a new route table.
         */
        public RouteTable(RouteTableId id) {
            this.id = checkNotNull(id);
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

                routeTable.put(createBinaryString(route.prefix()), route);

                notifyDelegate(new InternalRouteEvent(
                        InternalRouteEvent.Type.ROUTE_ADDED, singletonRouteSet(route)));
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
                    notifyDelegate(new InternalRouteEvent(
                            InternalRouteEvent.Type.ROUTE_REMOVED, emptyRouteSet(route.prefix())));
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
            return routes.values()
                    .stream()
                    .filter(route -> route.nextHop().equals(ip))
                    .collect(Collectors.toSet());
        }

        public RouteSet getRoutes(IpPrefix prefix) {
            Route route = routes.get(prefix);
            if (route != null) {
                return singletonRouteSet(route);
            }
            return null;
        }

        public Collection<RouteSet> getRouteSets() {
            return routes.values().stream()
                    .map(this::singletonRouteSet)
                    .collect(Collectors.toSet());
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

        private RouteSet singletonRouteSet(Route route) {
            return new RouteSet(id, route.prefix(), Collections.singleton(route));
        }

        private RouteSet emptyRouteSet(IpPrefix prefix) {
            return new RouteSet(id, prefix, Collections.emptySet());
        }
    }

}
