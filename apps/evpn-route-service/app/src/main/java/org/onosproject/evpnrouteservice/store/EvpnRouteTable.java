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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.evpnrouteservice.EvpnInternalRouteEvent;
import org.onosproject.evpnrouteservice.EvpnPrefix;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteStoreDelegate;
import org.onosproject.evpnrouteservice.EvpnRouteTableId;
import org.onosproject.evpnrouteservice.EvpnTable;
import org.onosproject.evpnrouteservice.Label;
import org.onosproject.evpnrouteservice.RouteDistinguisher;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a route table based on a consistent map.
 */
public class EvpnRouteTable implements EvpnTable {

    private final EvpnRouteTableId id;
    private final ConsistentMap<EvpnPrefix, Set<EvpnRoute>> routes;
    private final EvpnRouteStoreDelegate delegate;
    private final ExecutorService executor;
    private final RouteTableListener listener = new RouteTableListener();

    private final Consumer<DistributedPrimitive.Status> statusChangeListener;

    /**
     * Creates a new route table.
     *
     * @param id             route table ID
     * @param delegate       route store delegate to notify of events
     * @param storageService storage service
     * @param executor       executor service
     */
    public EvpnRouteTable(EvpnRouteTableId id, EvpnRouteStoreDelegate delegate,
                          StorageService storageService, ExecutorService executor) {
        this.delegate = checkNotNull(delegate);
        this.id = checkNotNull(id);
        this.routes = buildRouteMap(checkNotNull(storageService));
        this.executor = checkNotNull(executor);

        statusChangeListener = status -> {
            if (status.equals(DistributedPrimitive.Status.ACTIVE)) {
                executor.execute(this::notifyExistingRoutes);
            }
        };
        routes.addStatusChangeListener(statusChangeListener);

        notifyExistingRoutes();

        routes.addListener(listener);
    }

    private void notifyExistingRoutes() {
        routes.entrySet().stream()
                .map(e -> new EvpnInternalRouteEvent(
                        EvpnInternalRouteEvent.Type.ROUTE_ADDED,
                        new EvpnRouteSet(id, e.getKey(), e.getValue().value())))
                .forEach(delegate::notify);
    }

    private ConsistentMap<EvpnPrefix, Set<EvpnRoute>> buildRouteMap(StorageService
                                                                            storageService) {
        KryoNamespace routeTableSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(KryoNamespaces.MISC)
                .register(EvpnRoute.class)
                .register(EvpnPrefix.class)
                .register(RouteDistinguisher.class)
                .register(MacAddress.class)
                .register(IpPrefix.class)
                .register(EvpnRoute.Source.class)
                .register(IpAddress.class)
                .register(VpnRouteTarget.class)
                .register(Label.class)
                .register(EvpnRouteTableId.class)
                .build();
        return storageService.<EvpnPrefix, Set<EvpnRoute>>consistentMapBuilder()
                .withName("onos-evpn-routes-" + id.name())
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(routeTableSerializer))
                .build();
    }

    @Override
    public EvpnRouteTableId id() {
        return id;
    }

    @Override
    public void shutdown() {
        routes.removeStatusChangeListener(statusChangeListener);
        routes.removeListener(listener);
    }

    @Override
    public void destroy() {
        shutdown();
        routes.destroy();
    }

    @Override
    public void update(EvpnRoute route) {
        routes.compute(route.evpnPrefix(), (prefix, set) -> {
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(route);
            return set;
        });
    }

    @Override
    public void remove(EvpnRoute route) {
        routes.compute(route.evpnPrefix(), (prefix, set) -> {
            if (set != null) {
                set.remove(route);
                if (set.isEmpty()) {
                    return null;
                }
                return set;
            }
            return null;
        });
    }

    @Override
    public Collection<EvpnRouteSet> getRoutes() {
        return routes.entrySet().stream()
                .map(e -> new EvpnRouteSet(id, e.getKey(), e.getValue().value()))
                .collect(Collectors.toSet());
    }

    @Override
    public EvpnRouteSet getRoutes(EvpnPrefix prefix) {
        Versioned<Set<EvpnRoute>> routeSet = routes.get(prefix);

        if (routeSet != null) {
            return new EvpnRouteSet(id, prefix, routeSet.value());
        }
        return null;
    }

    @Override
    public Collection<EvpnRoute> getRoutesForNextHop(IpAddress nextHop) {
        // TODO index
        return routes.values().stream()
                .flatMap(v -> v.value().stream())
                .filter(r -> r.nextHop().equals(nextHop))
                .collect(Collectors.toSet());
    }

    private class RouteTableListener
            implements MapEventListener<EvpnPrefix, Set<EvpnRoute>> {

        private EvpnInternalRouteEvent createRouteEvent(
                EvpnInternalRouteEvent.Type type, MapEvent<EvpnPrefix, Set<EvpnRoute>>
                event) {
            Set<EvpnRoute> currentRoutes =
                    (event.newValue() == null) ? Collections.emptySet() : event.newValue().value();
            return new EvpnInternalRouteEvent(type, new EvpnRouteSet(id, event
                    .key(), currentRoutes));
        }

        @Override
        public void event(MapEvent<EvpnPrefix, Set<EvpnRoute>> event) {
            EvpnInternalRouteEvent ire = null;
            switch (event.type()) {
                case INSERT:
                    ire = createRouteEvent(EvpnInternalRouteEvent.Type.ROUTE_ADDED, event);
                    break;
                case UPDATE:
                    if (event.newValue().value().size() > event.oldValue().value().size()) {
                        ire = createRouteEvent(EvpnInternalRouteEvent.Type.ROUTE_ADDED, event);
                    } else {
                        ire = createRouteEvent(EvpnInternalRouteEvent.Type.ROUTE_REMOVED, event);
                    }
                    break;
                case REMOVE:
                    ire = createRouteEvent(EvpnInternalRouteEvent.Type.ROUTE_REMOVED, event);
                    break;
                default:
                    break;
            }
            if (ire != null) {
                delegate.notify(ire);
            }
        }
    }

}

