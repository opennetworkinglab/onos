/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.routeservice.ResolvedRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents Network Information Base (NIB) for routes
 * and supports alternative functions to
 * {@link org.onosproject.routeservice.RouteService} for offline data.
 */
public class RouteNib extends AbstractNib {

    // TODO with method optimization, store into subdivided structures at the first load
    // unresolved Route is treated as ResolvedRoute with nextHopMac null
    Set<ResolvedRoute> routes;

    // use the singleton helper to create the instance
    protected RouteNib() {
    }

    /**
     * Sets a set of routes.
     *
     * @param routes route set
     */
    public void setRoutes(Set<ResolvedRoute> routes) {
        this.routes = routes;
    }

    /**
     * Returns the set of routes.
     *
     * @return route set
     */
    public Set<ResolvedRoute> getRoutes() {
        return routes;
    }

    /**
     * Performs a longest prefix lookup on the given IP address.
     *
     * @param ip IP address to look up
     * @return most specific matching route, if one exists
     */
    public Optional<ResolvedRoute> longestPrefixLookup(IpAddress ip) {
        return routes.stream()
                .filter(r -> r.prefix().contains(ip))
                .max(Comparator.comparing(r -> r.prefix().prefixLength()));
    }

    /**
     * Returns all resolved routes stored for the given prefix, including the
     * best selected route.
     *
     * @param prefix IP prefix to look up routes for
     * @return all stored resolved routes for this prefix
     */
    public Collection<ResolvedRoute> getAllResolvedRoutes(IpPrefix prefix) {
        return routes.stream()
                .filter(r -> r.prefix().contains(prefix)
                        && r.nextHopMac() != null
                        && r.nextHopVlan() != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns the singleton instance of multicast routes NIB.
     *
     * @return instance of multicast routes NIB
     */
    public static RouteNib getInstance() {
        return RouteNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final RouteNib INSTANCE = new RouteNib();
    }

}
