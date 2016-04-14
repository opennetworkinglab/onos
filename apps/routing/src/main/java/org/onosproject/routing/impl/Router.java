/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.routing.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableId;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RoutingService;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Adapts new route service interface to old RoutingService interface.
 */
@Service
@Component(immediate = true, enabled = false)
public class Router implements RoutingService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteService routeService;

    @Override
    public void start() {
    }

    @Override
    public void addFibListener(FibListener fibListener) {
        routeService.addListener(new InternalRouteListener(fibListener));
    }

    @Override
    public void stop() {
    }

    @Override
    public Collection<RouteEntry> getRoutes4() {
        return routeService.getAllRoutes().get(new RouteTableId("ipv4")).stream()
                .map(route -> new RouteEntry(route.prefix(), route.nextHop()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<RouteEntry> getRoutes6() {
        return routeService.getAllRoutes().get(new RouteTableId("ipv6")).stream()
                .map(route -> new RouteEntry(route.prefix(), route.nextHop()))
                .collect(Collectors.toList());
    }

    @Override
    public RouteEntry getLongestMatchableRouteEntry(IpAddress ipAddress) {
        Route route = routeService.longestPrefixMatch(ipAddress);
        if (route != null) {
            return new RouteEntry(route.prefix(), route.nextHop());
        }
        return null;
    }

    /**
     * Internal route listener.
     */
    private class InternalRouteListener implements RouteListener {

        private final FibListener fibListener;

        /**
         * Constructor.
         *
         * @param fibListener FIB listener
         */
        public InternalRouteListener(FibListener fibListener) {
            this.fibListener = fibListener;
        }

        @Override
        public void event(RouteEvent event) {
            ResolvedRoute route = event.subject();
            FibEntry entry = new FibEntry(route.prefix(), route.nextHop(), route.nextHopMac());

            switch (event.type()) {
            case ROUTE_ADDED:
            case ROUTE_UPDATED:
                fibListener.update(Collections.singleton(new FibUpdate(FibUpdate.Type.UPDATE, entry)),
                        Collections.emptyList());
                break;
            case ROUTE_REMOVED:
                fibListener.update(Collections.emptyList(),
                        Collections.singleton(new FibUpdate(FibUpdate.Type.DELETE, entry)));
                break;
            default:
                break;
            }
        }
    }
}
