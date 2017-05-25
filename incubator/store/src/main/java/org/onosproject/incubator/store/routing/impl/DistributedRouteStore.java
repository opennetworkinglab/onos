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

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.routing.InternalRouteEvent;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteSet;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTableId;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        masterRouteTable.forEach(this::createRouteTable);

        masterRouteTable.addListener(masterRouteTableListener);

        // Add default tables (add is idempotent)
        masterRouteTable.add(IPV4);
        masterRouteTable.add(IPV6);

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
    public void removeRoute(Route route) {
        getDefaultRouteTable(route).remove(route);
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
    public Route longestPrefixMatch(IpAddress ip) {
        // Not supported
        return null;
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
        // Not supported
    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {
        // Not supported
    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        // Not supported
        return null;
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        // Not supported
        return Collections.emptyMap();
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
