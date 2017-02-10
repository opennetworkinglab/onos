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

import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTableId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.incubator.net.routing.RouteEvent.Type.ROUTE_ADDED;
import static org.onosproject.incubator.net.routing.RouteEvent.Type.ROUTE_REMOVED;
import static org.onosproject.incubator.net.routing.RouteTools.createBinaryString;

/**
 * Route store based on distributed storage.
 */
public class DistributedRouteStore extends AbstractStore<RouteEvent, RouteStoreDelegate>
        implements RouteStore {
    public StorageService storageService;

    private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    private static final RouteTableId IPV6 = new RouteTableId("ipv6");
    private static final Logger log = LoggerFactory.getLogger(DistributedRouteStore.class);
    private final MapEventListener<IpPrefix, Route> routeTableListener = new RouteTableListener();
    private final MapEventListener<IpAddress, NextHopData> nextHopListener = new NextHopListener();

    // TODO: ConsistentMap may not scale with high frequency route update
    private final Map<RouteTableId, ConsistentMap<IpPrefix, Route>> routeTables =
            Maps.newHashMap();
    // NOTE: We cache local route tables with InvertedRadixTree for longest prefix matching
    private final Map<RouteTableId, InvertedRadixTree<Route>> localRouteTables =
            Maps.newHashMap();
    private ConsistentMap<IpAddress, NextHopData> nextHops;

    /**
     * Constructs a distributed route store.
     *
     * @param storageService storage service should be passed from RouteStoreImpl
     */
    public DistributedRouteStore(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Sets up distributed route store.
     */
    public void activate() {
        // Creates and stores maps
        ConsistentMap<IpPrefix, Route> ipv4RouteTable = createRouteTable(IPV4);
        ConsistentMap<IpPrefix, Route> ipv6RouteTable = createRouteTable(IPV6);
        routeTables.put(IPV4, ipv4RouteTable);
        routeTables.put(IPV6, ipv6RouteTable);
        localRouteTables.put(IPV4, createLocalRouteTable());
        localRouteTables.put(IPV6, createLocalRouteTable());
        nextHops = createNextHopTable();

        // Adds map listeners
        routeTables.values().forEach(routeTable ->
                routeTable.addListener(routeTableListener, Executors.newSingleThreadExecutor()));
        nextHops.addListener(nextHopListener, Executors.newSingleThreadExecutor());

        log.info("Started");
    }

    /**
     * Cleans up distributed route store.
     */
    public void deactivate() {
        routeTables.values().forEach(routeTable -> {
            routeTable.removeListener(routeTableListener);
            routeTable.destroy();
        });
        nextHops.removeListener(nextHopListener);
        nextHops.destroy();

        routeTables.clear();
        localRouteTables.clear();
        nextHops.clear();

        log.info("Stopped");
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).put(route.prefix(), route);
    }

    @Override
    public void removeRoute(Route route) {
        getDefaultRouteTable(route).remove(route.prefix());

        if (getRoutesForNextHop(route.nextHop()).isEmpty()) {
            nextHops.remove(route.nextHop());
        }
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<Route> getRoutes(RouteTableId table) {
        ConsistentMap<IpPrefix, Route> routeTable = routeTables.get(table);
        return (routeTable != null) ?
                routeTable.values().stream().map(Versioned::value).collect(Collectors.toSet()) :
                Collections.emptySet();
    }

    @Override
    public Route longestPrefixMatch(IpAddress ip) {
        Iterable<Route> prefixes = getDefaultLocalRouteTable(ip)
                .getValuesForKeysPrefixing(createBinaryString(ip.toIpPrefix()));
        Iterator<Route> it = prefixes.iterator();

        Route route = null;
        while (it.hasNext()) {
            route = it.next();
        }

        return route;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return getDefaultRouteTable(ip).values().stream()
                .filter(route -> route.nextHop().equals(ip))
                .collect(Collectors.toList());
    }

    @Override
    public void updateNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        Collection<Route> routes = getRoutesForNextHop(ip);
        if (!routes.isEmpty() && !nextHopData.equals(getNextHop(ip))) {
            nextHops.put(ip, nextHopData);
        }
    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        nextHops.remove(ip, nextHopData);
    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        return Versioned.valueOrNull(nextHops.get(ip));
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        return nextHops.asJavaMap();
    }

    private ConsistentMap<IpPrefix, Route> createRouteTable(RouteTableId tableId) {
        KryoNamespace routeTableSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(Route.class)
                .register(Route.Source.class)
                .build();
        return storageService.<IpPrefix, Route>consistentMapBuilder()
                .withName("onos-routes-" + tableId.name())
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(routeTableSerializer))
                .build();
    }

    private ConcurrentInvertedRadixTree<Route> createLocalRouteTable() {
        return new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
    }

    private ConsistentMap<IpAddress, NextHopData> createNextHopTable() {
        KryoNamespace.Builder nextHopSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(NextHopData.class);
        return storageService.<IpAddress, NextHopData>consistentMapBuilder()
                .withName("onos-nexthops")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(nextHopSerializer.build()))
                .build();
    }

    private Map<IpPrefix, Route> getDefaultRouteTable(Route route) {
        return getDefaultRouteTable(route.prefix().address());
    }

    private Map<IpPrefix, Route> getDefaultRouteTable(IpAddress ip) {
        RouteTableId routeTableId = (ip.isIp4()) ? IPV4 : IPV6;
        return routeTables.get(routeTableId).asJavaMap();
    }

    private InvertedRadixTree<Route> getDefaultLocalRouteTable(IpAddress ip) {
        RouteTableId routeTableId = (ip.isIp4()) ? IPV4 : IPV6;
        return localRouteTables.get(routeTableId);
    }

    private class RouteTableListener implements MapEventListener<IpPrefix, Route> {
        @Override
        public void event(MapEvent<IpPrefix, Route> event) {
            Route route, prevRoute;
            NextHopData nextHopData, prevNextHopData;
            switch (event.type()) {
                case INSERT:
                    route = checkNotNull(event.newValue().value());
                    nextHopData = getNextHop(route.nextHop());

                    // Update local cache
                    getDefaultLocalRouteTable(route.nextHop())
                            .put(createBinaryString(route.prefix()), route);

                    // Send ROUTE_ADDED only when the next hop is resolved
                    if (nextHopData != null) {
                        notifyDelegate(new RouteEvent(ROUTE_ADDED,
                                new ResolvedRoute(route,
                                        nextHopData.mac(), nextHopData.location())));
                    }
                    break;
                case UPDATE:
                    route = checkNotNull(event.newValue().value());
                    prevRoute = checkNotNull(event.oldValue().value());
                    nextHopData = getNextHop(route.nextHop());
                    prevNextHopData = getNextHop(prevRoute.nextHop());

                    // Update local cache
                    getDefaultLocalRouteTable(route.nextHop())
                            .put(createBinaryString(route.prefix()), route);

                    if (nextHopData == null && prevNextHopData != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                new ResolvedRoute(prevRoute,
                                        prevNextHopData.mac(), prevNextHopData.location())));
                    } else if (nextHopData != null && prevNextHopData != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                new ResolvedRoute(route,
                                        nextHopData.mac(), nextHopData.location()),
                                new ResolvedRoute(prevRoute,
                                        prevNextHopData.mac(), prevNextHopData.location())));
                    }

                    cleanupNextHop(prevRoute.nextHop());
                    break;
                case REMOVE:
                    prevRoute = checkNotNull(event.oldValue().value());
                    prevNextHopData = getNextHop(prevRoute.nextHop());

                    // Update local cache
                    getDefaultLocalRouteTable(prevRoute.nextHop())
                            .remove(createBinaryString(prevRoute.prefix()));

                    // Send ROUTE_REMOVED only when the next hop is resolved
                    if (prevNextHopData != null) {
                       notifyDelegate(new RouteEvent(ROUTE_REMOVED,
                               new ResolvedRoute(prevRoute,
                                       prevNextHopData.mac(), prevNextHopData.location())));
                    }

                    cleanupNextHop(prevRoute.nextHop());
                    break;
                default:
                    log.warn("Unknown MapEvent type: {}", event.type());
            }
        }

        /**
         * Cleanup a nexthop when there is no routes reference to it.
         */
        private void cleanupNextHop(IpAddress ip) {
            if (getDefaultRouteTable(ip).values().stream().noneMatch(route ->
                    route.nextHop().equals(ip))) {
                nextHops.remove(ip);
            }
        }
    }

    private class NextHopListener implements MapEventListener<IpAddress, NextHopData> {
        @Override
        public void event(MapEvent<IpAddress, NextHopData> event) {
            NextHopData nextHopData, oldNextHopData;
            Collection<Route> routes = getRoutesForNextHop(event.key());

            switch (event.type()) {
                case INSERT:
                    nextHopData = checkNotNull(event.newValue().value());
                    routes.forEach(route ->
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                new ResolvedRoute(route,
                                        nextHopData.mac(), nextHopData.location())))
                    );
                    break;
                case UPDATE:
                    nextHopData = checkNotNull(event.newValue().value());
                    oldNextHopData = checkNotNull(event.oldValue().value());
                    routes.forEach(route -> {
                        if (oldNextHopData == null) {
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                    new ResolvedRoute(route,
                                            nextHopData.mac(), nextHopData.location())));
                        } else if (!oldNextHopData.equals(nextHopData)) {
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                    new ResolvedRoute(route,
                                            nextHopData.mac(), nextHopData.location()),
                                    new ResolvedRoute(route,
                                            oldNextHopData.mac(), oldNextHopData.location())));
                        }
                    });
                    break;
                case REMOVE:
                    oldNextHopData = checkNotNull(event.oldValue().value());
                    routes.forEach(route ->
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                new ResolvedRoute(route,
                                        oldNextHopData.mac(), oldNextHopData.location())))
                    );
                    break;
                default:
                    log.warn("Unknown MapEvent type: {}", event.type());
            }
        }
    }
}
