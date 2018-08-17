/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnrouteservice.store;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.evpnrouteservice.EvpnInternalRouteEvent;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteStore;
import org.onosproject.evpnrouteservice.EvpnRouteStoreDelegate;
import org.onosproject.evpnrouteservice.EvpnRouteTableId;
import org.onosproject.evpnrouteservice.EvpnTable;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
@Component(service = EvpnRouteStore.class)
public class DistributedEvpnRouteStore extends
        AbstractStore<EvpnInternalRouteEvent,
                EvpnRouteStoreDelegate>
        implements EvpnRouteStore {

    private static final Logger log = LoggerFactory
            .getLogger(DistributedEvpnRouteStore.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public StorageService storageService;

    private static final EvpnRouteTableId EVPN_IPV4 = new EvpnRouteTableId("evpn_ipv4");
    private static final EvpnRouteTableId EVPN_IPV6 = new EvpnRouteTableId("evpn_ipv6");

    private final SetEventListener<EvpnRouteTableId> masterRouteTableListener =
            new MasterRouteTableListener();
    private final EvpnRouteStoreDelegate ourDelegate = new
            InternalEvpnRouteStoreDelegate();

    // Stores the route tables that have been created
    public DistributedSet<EvpnRouteTableId> masterRouteTable;
    // Local memory map to store route table object
    public Map<EvpnRouteTableId, EvpnTable> routeTables;

    private ExecutorService executor;


    /**
     * Sets up distributed route store.
     */
    @Activate
    public void activate() {
        routeTables = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadExecutor(groupedThreads("onos/route", "store", log));

        KryoNamespace masterRouteTableSerializer = KryoNamespace.newBuilder()
                .register(EvpnRouteTableId.class)
                .build();

        masterRouteTable = storageService.<EvpnRouteTableId>setBuilder()
                .withName("onos-master-route-table")
                .withSerializer(Serializer.using(masterRouteTableSerializer))
                .build()
                .asDistributedSet();

        masterRouteTable.forEach(this::createRouteTable);

        masterRouteTable.addListener(masterRouteTableListener);

        // Add default tables (add is idempotent)
        masterRouteTable.add(EVPN_IPV4);
        masterRouteTable.add(EVPN_IPV6);

        log.info("Started");
    }

    /**
     * Cleans up distributed route store.
     */
    @Deactivate
    public void deactivate() {
        masterRouteTable.removeListener(masterRouteTableListener);

        routeTables.values().forEach(EvpnTable::shutdown);

        log.info("Stopped");
    }

    @Override
    public void updateRoute(EvpnRoute route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void removeRoute(EvpnRoute route) {
        getDefaultRouteTable(route).remove(route);
    }

    @Override
    public Set<EvpnRouteTableId> getRouteTables() {
        return ImmutableSet.copyOf(masterRouteTable);
    }

    @Override
    public Collection<EvpnRouteSet> getRoutes(EvpnRouteTableId table) {
        EvpnTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        } else {
            return ImmutableSet.copyOf(routeTable.getRoutes());
        }
    }

    @Override
    public Collection<EvpnRoute> getRoutesForNextHop(IpAddress ip) {
        return getDefaultRouteTable(ip).getRoutesForNextHop(ip);
    }

    private void createRouteTable(EvpnRouteTableId tableId) {
        routeTables.computeIfAbsent(tableId, id -> new EvpnRouteTable(id,
                                                                      ourDelegate, storageService, executor));
    }

    private void destroyRouteTable(EvpnRouteTableId tableId) {
        EvpnTable table = routeTables.remove(tableId);
        if (table != null) {
            table.destroy();
        }
    }

    private EvpnTable getDefaultRouteTable(EvpnRoute route) {
        return getDefaultRouteTable(route.prefixIp().address());
    }

    private EvpnTable getDefaultRouteTable(IpAddress ip) {
        EvpnRouteTableId routeTableId = (ip.isIp4()) ? EVPN_IPV4 : EVPN_IPV6;
        return routeTables.getOrDefault(routeTableId, EmptyEvpnRouteTable
                .instance());
    }

    private class InternalEvpnRouteStoreDelegate implements
            EvpnRouteStoreDelegate {
        @Override
        public void notify(EvpnInternalRouteEvent event) {
            executor.execute(() -> DistributedEvpnRouteStore
                    .this.notifyDelegate(event));
        }
    }

    private class MasterRouteTableListener implements SetEventListener<EvpnRouteTableId> {
        @Override
        public void event(SetEvent<EvpnRouteTableId> event) {
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
