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

package org.onosproject.routeservice.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.routeservice.InternalRouteEvent;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteSet;
import org.onosproject.routeservice.RouteStoreDelegate;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMultimap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a route table based on a consistent map.
 */
public class DefaultRouteTable implements RouteTable {

    private final RouteTableId id;
    private final ConsistentMultimap<IpPrefix, Route> routes;
    private final RouteStoreDelegate delegate;
    private final ExecutorService executor;
    private final RouteTableListener listener = new RouteTableListener();

    private final Consumer<DistributedPrimitive.Status> statusChangeListener;

    /**
     * Creates a new route table.
     *
     * @param id route table ID
     * @param delegate route store delegate to notify of events
     * @param storageService storage service
     * @param executor executor service
     */
    public DefaultRouteTable(RouteTableId id, RouteStoreDelegate delegate,
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

        routes.addListener(listener, executor);
    }

    private void notifyExistingRoutes() {
        getRoutes().forEach(routeSet -> delegate.notify(
            new InternalRouteEvent(InternalRouteEvent.Type.ROUTE_ADDED, routeSet)));
    }

    private ConsistentMultimap<IpPrefix, Route> buildRouteMap(StorageService storageService) {
        KryoNamespace routeTableSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(Route.class)
                .register(Route.Source.class)
                .build();
        return storageService.<IpPrefix, Route>consistentMultimapBuilder()
                .withName("onos-routes-" + id.name())
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(routeTableSerializer))
                .build();
    }

    @Override
    public RouteTableId id() {
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
    public void update(Route route) {
        routes.put(route.prefix(), route);
    }

    @Override
    public void remove(Route route) {
        routes.remove(route.prefix(), route);
    }

    @Override
    public Collection<RouteSet> getRoutes() {
        return routes.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.groupingBy(Route::prefix))
            .entrySet()
            .stream()
            .map(entry -> new RouteSet(id, entry.getKey(), ImmutableSet.copyOf(entry.getValue())))
            .collect(Collectors.toList());
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        Versioned<Collection<? extends Route>> routeSet = routes.get(prefix);

        if (routeSet != null) {
            return new RouteSet(id, prefix, ImmutableSet.copyOf(routeSet.value()));
        }
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress nextHop) {
        return routes.stream()
            .map(Map.Entry::getValue)
            .filter(r -> r.nextHop().equals(nextHop))
            .collect(Collectors.toSet());
    }

    private class RouteTableListener
            implements MultimapEventListener<IpPrefix, Route> {

        private InternalRouteEvent createRouteEvent(
                InternalRouteEvent.Type type, MultimapEvent<IpPrefix, Route> event) {
            Collection<? extends Route> currentRoutes = Versioned.valueOrNull(routes.get(event.key()));
            return new InternalRouteEvent(type, new RouteSet(
                id, event.key(), currentRoutes != null ? ImmutableSet.copyOf(currentRoutes) : Collections.emptySet()));
        }

        @Override
        public void event(MultimapEvent<IpPrefix, Route> event) {
            InternalRouteEvent ire = null;
            switch (event.type()) {
            case INSERT:
                ire = createRouteEvent(InternalRouteEvent.Type.ROUTE_ADDED, event);
                break;
            case REMOVE:
                ire = createRouteEvent(InternalRouteEvent.Type.ROUTE_REMOVED, event);
                break;
            default:
                break;
            }
            delegate.notify(ire);
        }
    }

}
