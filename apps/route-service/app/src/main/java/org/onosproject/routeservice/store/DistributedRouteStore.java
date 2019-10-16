/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.routeservice.store;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.routeservice.InternalRouteEvent;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteSet;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routeservice.RouteStoreDelegate;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Route store based on distributed storage.
 */
public class DistributedRouteStore extends AbstractStore<InternalRouteEvent, RouteStoreDelegate>
        implements RouteStore {

    protected StorageService storageService;

    private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    private static final RouteTableId IPV6 = new RouteTableId("ipv6");
    private static final Logger log = LoggerFactory.getLogger(DistributedRouteStore.class);
    private final SetEventListener<RouteTableId> masterRouteTableListener =
            new MasterRouteTableListener();
    private final RouteStoreDelegate ourDelegate = new InternalRouteStoreDelegate();

    // Stores the route tables that have been created
    private DistributedSet<RouteTableId> masterRouteTable;
    // Local memory map to store route table object
    private Map<RouteTableId, RouteTable> routeTables;

    private ExecutorService executor;

    public DistributedRouteStore(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Sets up distributed route store.
     */
    public void activate() {
        routeTables = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadExecutor(groupedThreads("onos/route", "store", log));

        KryoNamespace masterRouteTableSerializer = KryoNamespace.newBuilder()
                .register(RouteTableId.class)
                .build();

        masterRouteTable = storageService.<RouteTableId>setBuilder()
                .withName("onos-master-route-table")
                .withSerializer(Serializer.using(masterRouteTableSerializer))
                .build()
                .asDistributedSet();

        masterRouteTable.addListener(masterRouteTableListener);

        // Add default tables (add is idempotent)
        masterRouteTable.add(IPV4);
        masterRouteTable.add(IPV6);

        masterRouteTable.forEach(this::createRouteTable);

        log.info("Started");
    }

    /**
     * Cleans up distributed route store.
     */
    public void deactivate() {
        masterRouteTable.removeListener(masterRouteTableListener);

        routeTables.values().forEach(RouteTable::shutdown);

        log.info("Stopped");
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void updateRoutes(Collection<Route> routes) {
        Map<RouteTableId, Set<Route>> computedTables = computeRouteTablesFromRoutes(routes);
        computedTables.forEach(
                ((routeTableId, routesToAdd) -> getDefaultRouteTable(routeTableId).update(routesToAdd))
        );
    }

    @Override
    public void removeRoute(Route route) {
        getDefaultRouteTable(route).remove(route);
    }

    @Override
    public void removeRoutes(Collection<Route> routes) {
        Map<RouteTableId, Set<Route>> computedTables = computeRouteTablesFromRoutes(routes);
        computedTables.forEach(
                ((routeTableId, routesToRemove) -> getDefaultRouteTable(routeTableId).remove(routesToRemove))
        );
    }

    @Override
    public void replaceRoute(Route route) {
        getDefaultRouteTable(route).replace(route);
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return ImmutableSet.copyOf(masterRouteTable);
    }

    @Override
    public Collection<RouteSet> getRoutes(RouteTableId table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        } else {
            return ImmutableSet.copyOf(routeTable.getRoutes());
        }
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return getDefaultRouteTable(ip).getRoutesForNextHop(ip);
    }

    @Override
    public Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> nextHops) {
        Map<RouteTableId, Set<IpAddress>> computedTables = computeRouteTablesFromIps(nextHops);
        return computedTables.entrySet().stream()
                .map(entry -> getDefaultRouteTable(entry.getKey()).getRoutesForNextHops(entry.getValue()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        return getDefaultRouteTable(prefix.address()).getRoutes(prefix);
    }

    private void createRouteTable(RouteTableId tableId) {
        routeTables.computeIfAbsent(tableId, id -> new DefaultRouteTable(id, ourDelegate, storageService, executor));
    }

    private void destroyRouteTable(RouteTableId tableId) {
        RouteTable table = routeTables.remove(tableId);
        if (table != null) {
            table.destroy();
        }
    }

    private RouteTable getDefaultRouteTable(Route route) {
        return getDefaultRouteTable(route.prefix().address());
    }

    private RouteTable getDefaultRouteTable(IpAddress ip) {
        RouteTableId routeTableId = (ip.isIp4()) ? IPV4 : IPV6;
        return routeTables.getOrDefault(routeTableId, EmptyRouteTable.instance());
    }

    private RouteTable getDefaultRouteTable(RouteTableId routeTableId) {
        return routeTables.getOrDefault(routeTableId, EmptyRouteTable.instance());
    }

    private Map<RouteTableId, Set<Route>> computeRouteTablesFromRoutes(Collection<Route> routes) {
        Map<RouteTableId, Set<Route>> computedTables = new HashMap<>();
        routes.forEach(route -> {
            RouteTableId routeTableId = (route.prefix().address().isIp4()) ? IPV4 : IPV6;
            Set<Route> tempRoutes = computedTables.computeIfAbsent(routeTableId, k -> Sets.newHashSet());
            tempRoutes.add(route);
        });
        return computedTables;
    }

    private Map<RouteTableId, Set<IpAddress>> computeRouteTablesFromIps(Collection<IpAddress> ipAddresses) {
        Map<RouteTableId, Set<IpAddress>> computedTables = new HashMap<>();
        ipAddresses.forEach(ipAddress -> {
            RouteTableId routeTableId = (ipAddress.isIp4()) ? IPV4 : IPV6;
            Set<IpAddress> tempIpAddresses = computedTables.computeIfAbsent(routeTableId, k -> Sets.newHashSet());
            tempIpAddresses.add(ipAddress);
        });
        return computedTables;
    }

    private class InternalRouteStoreDelegate implements RouteStoreDelegate {
        @Override
        public void notify(InternalRouteEvent event) {
            executor.execute(() -> DistributedRouteStore.this.notifyDelegate(event));
        }
    }

    private class MasterRouteTableListener implements SetEventListener<RouteTableId> {
        @Override
        public void event(SetEvent<RouteTableId> event) {
            switch (event.type()) {
            case ADD:
                executor.execute(() -> createRouteTable(event.entry()));
                break;
            case REMOVE:
                executor.execute(() -> destroyRouteTable(event.entry()));
                break;
            default:
                break;
            }
        }
    }
}
