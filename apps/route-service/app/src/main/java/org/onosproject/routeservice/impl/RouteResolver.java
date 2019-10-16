/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.routeservice.impl;

import org.onlab.util.PredictableExecutor;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Resolves routes in multi-thread fashion.
 */
class RouteResolver {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final int DEFAULT_BUCKETS = 0;
    private RouteManager routeManager;
    private HostService hostService;
    protected PredictableExecutor routeResolvers;

    /**
     * Creates a new route resolver.
     *
     * @param routeManager route service
     * @param hostService host service
     */
    RouteResolver(RouteManager routeManager, HostService hostService) {
        this.routeManager = routeManager;
        this.hostService = hostService;
        routeResolvers = new PredictableExecutor(DEFAULT_BUCKETS, groupedThreads("onos/route-resolver",
                                                                                  "route-resolver-%d", log));
    }

    /**
     * Shuts down the route resolver.
     */
    void shutdown() {
        routeResolvers.shutdown();
    }

    private ResolvedRoute tryResolve(Route route) {
        ResolvedRoute resolvedRoute = resolve(route);
        if (resolvedRoute == null) {
            resolvedRoute = new ResolvedRoute(route, null, null);
        }
        return resolvedRoute;
    }

    // Used by external reads
    Set<ResolvedRoute> resolveRouteSet(RouteSet routeSet) {
        return routeSet.routes().stream()
                .map(this::tryResolve)
                .collect(Collectors.toSet());
    }

    // Used by external reads and by resolvers
    ResolvedRoute resolve(Route route) {
        hostService.startMonitoringIp(route.nextHop());
        Set<Host> hosts = hostService.getHostsByIp(route.nextHop());

        return hosts.stream().findFirst()
                .map(host -> new ResolvedRoute(route, host.mac(), host.vlan()))
                .orElse(null);
    }

    private ResolvedRoute decide(ResolvedRoute route1, ResolvedRoute route2) {
        return Comparator.comparing(ResolvedRoute::nextHop)
                .compare(route1, route2) <= 0 ? route1 : route2;
    }

    private void resolveInternal(RouteSet routes) {
        if (routes.routes() == null) {
            // The routes were removed before we got to them, nothing to do
            return;
        }

        Set<ResolvedRoute> resolvedRoutes = routes.routes().stream()
                .map(this::resolve)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Optional<ResolvedRoute> bestRoute = resolvedRoutes.stream()
                .reduce(this::decide);

        if (bestRoute.isPresent()) {
            routeManager.store(bestRoute.get(), resolvedRoutes);
        } else {
            routeManager.remove(routes.prefix());
        }
    }

    // Offload to the resolvers using prefix hashcode as hint
    // TODO Remove RouteManager reference using PickyCallable
    void resolve(RouteSet routes) {
        routeResolvers.execute(() -> resolveInternal(routes), routes.prefix().hashCode());
    }
}
