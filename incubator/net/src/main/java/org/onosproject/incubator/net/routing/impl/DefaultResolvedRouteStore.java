/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.GuavaCollectors;
import org.onlab.util.Tools;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteTableId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.incubator.net.routing.RouteTools.createBinaryString;

/**
 * Stores routes that have been resolved.
 */
public class DefaultResolvedRouteStore implements ResolvedRouteStore {

    private Map<RouteTableId, RouteTable> routeTables;
    private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    private static final RouteTableId IPV6 = new RouteTableId("ipv6");

    /**
     * Creates a new resolved route store.
     */
    public DefaultResolvedRouteStore() {
        routeTables = new ConcurrentHashMap<>();

        routeTables.put(IPV4, new RouteTable());
        routeTables.put(IPV6, new RouteTable());
    }

    @Override
    public RouteEvent updateRoute(ResolvedRoute route, Set<ResolvedRoute> alternatives) {
        return getDefaultRouteTable(route).update(route, alternatives);
    }

    @Override
    public RouteEvent removeRoute(IpPrefix prefix) {
        RouteTable table = getDefaultRouteTable(prefix.address());
        return table.remove(prefix);
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<ResolvedRoute> getRoutes(RouteTableId table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    @Override
    public Optional<ResolvedRoute> getRoute(IpPrefix prefix) {
        return getDefaultRouteTable(prefix.address()).getRoute(prefix);
    }

    @Override
    public Collection<ResolvedRoute> getAllRoutes(IpPrefix prefix) {
        return getDefaultRouteTable(prefix.address()).getAllRoutes(prefix);
    }

    @Override
    public Optional<ResolvedRoute> longestPrefixMatch(IpAddress ip) {
        return getDefaultRouteTable(ip).longestPrefixMatch(ip);
    }

    private RouteTable getDefaultRouteTable(ResolvedRoute route) {
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
        private final InvertedRadixTree<ResolvedRoute> routeTable;
        private final Map<IpPrefix, Set<ResolvedRoute>> alternativeRoutes;

        /**
         * Creates a new route table.
         */
        public RouteTable() {
            routeTable = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());

            alternativeRoutes = Maps.newHashMap();
        }

        /**
         * Adds or updates the route in the route table.
         *
         * @param route route to update
         * @param alternatives alternative routes
         */
        public RouteEvent update(ResolvedRoute route, Set<ResolvedRoute> alternatives) {
            Set<ResolvedRoute> immutableAlternatives = checkAlternatives(route, alternatives);

            synchronized (this) {
                ResolvedRoute oldRoute = routeTable.put(createBinaryString(route.prefix()), route);
                Set<ResolvedRoute> oldRoutes = alternativeRoutes.put(route.prefix(), immutableAlternatives);

                if (!route.equals(oldRoute)) {
                    if (oldRoute == null) {
                        return new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route,
                                immutableAlternatives);
                    } else {
                        return new RouteEvent(RouteEvent.Type.ROUTE_UPDATED, route,
                                oldRoute, immutableAlternatives);
                    }
                }

                if (!immutableAlternatives.equals(oldRoutes)) {
                    return new RouteEvent(RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED,
                            route, immutableAlternatives);
                }

                return null;
            }
        }

        /**
         * Checks that the best route is present in the alternatives list and
         * returns an immutable set of alternatives.
         *
         * @param route best route
         * @param alternatives alternatives
         * @return immutable set of alternative routes
         */
        private Set<ResolvedRoute> checkAlternatives(ResolvedRoute route, Set<ResolvedRoute> alternatives) {
            if (!alternatives.contains(route)) {
                return ImmutableSet.<ResolvedRoute>builder()
                        .addAll(alternatives)
                        .add(route)
                        .build();
            } else {
                return ImmutableSet.copyOf(alternatives);
            }
        }

        /**
         * Removes the route from the route table.
         *
         * @param prefix prefix to remove
         */
        public RouteEvent remove(IpPrefix prefix) {
            synchronized (this) {
                String key = createBinaryString(prefix);

                ResolvedRoute route = routeTable.getValueForExactKey(key);
                alternativeRoutes.remove(prefix);

                if (route != null) {
                    routeTable.remove(key);
                    return new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, route);
                }
                return null;
            }
        }

        /**
         * Returns all routes in the route table.
         *
         * @return all routes
         */
        public Collection<ResolvedRoute> getRoutes() {
            return Tools.stream(routeTable.getKeyValuePairsForKeysStartingWith(""))
                    .map(KeyValuePair::getValue)
                    .collect(GuavaCollectors.toImmutableList());
        }

        /**
         * Returns the best route for the given prefix, if one exists.
         *
         * @param prefix IP prefix
         * @return best route
         */
        public Optional<ResolvedRoute> getRoute(IpPrefix prefix) {
            return Optional.ofNullable(routeTable.getValueForExactKey(createBinaryString(prefix)));
        }

        public Collection<ResolvedRoute> getAllRoutes(IpPrefix prefix) {
            return alternativeRoutes.getOrDefault(prefix, Collections.emptySet());
        }

        /**
         * Performs a longest prefix match with the given IP in the route table.
         *
         * @param ip IP address to look up
         * @return most specific prefix containing the given
         */
        public Optional<ResolvedRoute> longestPrefixMatch(IpAddress ip) {
            return Tools.stream(routeTable.getValuesForKeysPrefixing(createBinaryString(ip.toIpPrefix())))
                    .reduce((a, b) -> b); // reduces to the last element in the stream
        }
    }
}
