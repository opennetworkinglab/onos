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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.NodeId;
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


import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a route table based on a consistent map.
 */
public class DefaultRouteTable implements RouteTable {

    private final RouteTableId id;

    // The route map stores RawRoute instead of Route to translate the polymorphic IpPrefix and IpAddress types
    // into monomorphic types (specifically String). Using strings in the stored RawRoute is necessary to ensure
    // the serialized bytes are consistent whether e.g. IpAddress or Ip4Address is used when storing a route.
    private final ConsistentMultimap<String, RawRoute> routes;

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

    private ConsistentMultimap<String, RawRoute> buildRouteMap(StorageService storageService) {
        KryoNamespace routeTableSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(Route.class)
                .register(Route.Source.class)
                .register(RawRoute.class)
                .build();
        return storageService.<String, RawRoute>consistentMultimapBuilder()
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
        routes.put(route.prefix().toString(), new RawRoute(route));
    }

    @Override
    public void update(Collection<Route> routesAdded) {
        Map<String, Collection<? extends RawRoute>> computedRoutes = new HashMap<>();
        computeRoutesToAdd(routesAdded).forEach((prefix, routes) -> computedRoutes.computeIfAbsent(
                prefix, k -> Sets.newHashSet(routes)));
        routes.putAll(computedRoutes);
    }

    @Override
    public void remove(Route route) {
        getRoutes(route.prefix())
            .routes()
            .stream()
            .filter(r -> r.equals(route))
            .findAny()
            .ifPresent(matchRoute -> {
                routes.remove(matchRoute.prefix().toString(), new RawRoute(matchRoute));
            });
    }

    @Override
    public void remove(Collection<Route> routesRemoved) {
        Map<String, Collection<? extends RawRoute>> computedRoutes = new HashMap<>();
        computeRoutesToRemove(routesRemoved).forEach((prefix, routes) -> computedRoutes.computeIfAbsent(
                prefix, k -> Sets.newHashSet(routes)));
        routes.removeAll(computedRoutes);
    }

    @Override
    public void replace(Route route) {
        routes.replaceValues(route.prefix().toString(), Sets.newHashSet(new RawRoute(route)));
    }

    @Override
    public Collection<RouteSet> getRoutes() {
        return routes.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.groupingBy(RawRoute::prefix))
            .entrySet()
            .stream()
            .map(entry -> new RouteSet(id,
                IpPrefix.valueOf(entry.getKey()),
                entry.getValue().stream().map(RawRoute::route).collect(Collectors.toSet())))
            .collect(Collectors.toList());
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        Versioned<Collection<? extends RawRoute>> routeSet = routes.get(prefix.toString());
        if (routeSet != null) {
            return new RouteSet(id, prefix, routeSet.value().stream().map(RawRoute::route).collect(Collectors.toSet()));
        }
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress nextHop) {
        return routes.stream()
            .map(Map.Entry::getValue)
            .filter(r -> IpAddress.valueOf(r.nextHop()).equals(nextHop))
            .map(RawRoute::route)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> nextHops) {
        // First create a reduced snapshot of the store iterating one time the map
        Map<String, Collection<? extends RawRoute>> filteredRouteStore = new HashMap<>();
        routes.stream()
                .map(Map.Entry::getValue)
                .filter(r -> nextHops.contains(IpAddress.valueOf(r.nextHop())))
                .forEach(r -> filteredRouteStore.computeIfAbsent(r.prefix, k -> {
                    // We need to get all the routes because the resolve logic
                    // will use the alternatives as well
                    Versioned<Collection<? extends RawRoute>> routeSet = routes.get(k);
                    if (routeSet != null) {
                        return routeSet.value();
                    }
                    return null;
                }));
        // Return the collection of the routeSet we have to resolve
        return filteredRouteStore.entrySet().stream()
                .map(entry -> new RouteSet(id, IpPrefix.valueOf(entry.getKey()),
                                           entry.getValue().stream().map(RawRoute::route).collect(Collectors.toSet())))
                .collect(Collectors.toSet());
    }

    private Map<String, Collection<RawRoute>> computeRoutesToAdd(Collection<Route> routesAdded) {
        Map<String, Collection<RawRoute>> computedRoutes = new HashMap<>();
        routesAdded.forEach(route -> {
            Collection<RawRoute> tempRoutes = computedRoutes.computeIfAbsent(
                    route.prefix().toString(), k -> Sets.newHashSet());
            tempRoutes.add(new RawRoute(route));
        });
        return computedRoutes;
    }

    private Map<String, Collection<RawRoute>> computeRoutesToRemove(Collection<Route> routesRemoved) {
        Map<String, Collection<RawRoute>> computedRoutes = new HashMap<>();
        routesRemoved.forEach(route -> getRoutes(route.prefix())
                .routes()
                .stream()
                .filter(r -> r.equals(route))
                .findAny()
                .ifPresent(matchRoute -> {
                    Collection<RawRoute> tempRoutes = computedRoutes.computeIfAbsent(
                            matchRoute.prefix().toString(), k -> Sets.newHashSet());
                    tempRoutes.add(new RawRoute(matchRoute));
                }));
        return computedRoutes;
    }

    private class RouteTableListener
            implements MultimapEventListener<String, RawRoute> {

        private InternalRouteEvent createRouteEvent(
                InternalRouteEvent.Type type, MultimapEvent<String, RawRoute> event) {
            Collection<? extends RawRoute> currentRoutes = Versioned.valueOrNull(routes.get(event.key()));
            return new InternalRouteEvent(type, new RouteSet(
                id, IpPrefix.valueOf(event.key()), currentRoutes != null ?
                currentRoutes.stream().map(RawRoute::route).collect(Collectors.toSet())
                : Collections.emptySet()));
        }

        @Override
        public void event(MultimapEvent<String, RawRoute> event) {
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

    /**
     * Represents a route object stored in the underlying ConsistentMultimap.
     */
    private static class RawRoute {
        private Route.Source source;
        private String prefix;
        private String nextHop;
        private NodeId sourceNode;

        RawRoute(Route route) {
            this.source = route.source();
            this.prefix = route.prefix().toString();
            this.nextHop = route.nextHop().toString();
            this.sourceNode = route.sourceNode();
        }

        String prefix() {
            return prefix;
        }

        String nextHop() {
            return nextHop;
        }

        Route route() {
            return new Route(source, IpPrefix.valueOf(prefix), IpAddress.valueOf(nextHop), sourceNode);
        }

        public int hashCode() {
            return Objects.hash(prefix, nextHop);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof RawRoute)) {
                return false;
            }

            RawRoute that = (RawRoute) other;

            return Objects.equals(this.prefix, that.prefix) &&
                    Objects.equals(this.nextHop, that.nextHop);
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("prefix", prefix)
                    .add("nextHop", nextHop)
                    .toString();
        }

    }
}
