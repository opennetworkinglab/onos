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

package org.onosproject.routeservice.impl;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteTableId;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Stores resolved routes and best route decisions.
 */
public interface ResolvedRouteStore {

    /**
     * Adds or updates the best route for the given prefix.
     *
     * @param route new best route for this prefix
     * @param alternatives alternative resolved routes
     * @return event describing the change
     */
    RouteEvent updateRoute(ResolvedRoute route, Set<ResolvedRoute> alternatives);

    /**
     * Removes the best route for the given prefix.
     *
     * @param prefix IP prefix
     * @return event describing the change
     */
    RouteEvent removeRoute(IpPrefix prefix);

    /**
     * Gets the set of route tables.
     *
     * @return set of route table IDs
     */
    Set<RouteTableId> getRouteTables();

    /**
     * Returns the best routes for a give route table.
     *
     * @param table route table ID
     * @return collection of selected routes
     */
    Collection<ResolvedRoute> getRoutes(RouteTableId table);

    /**
     * Returns the best selected route for the given IP prefix.
     *
     * @param prefix IP prefix
     * @return optional best route
     */
    Optional<ResolvedRoute> getRoute(IpPrefix prefix);

    /**
     * Returns all resolved routes stored for the given prefix, including the
     * best selected route.
     *
     * @param prefix IP prefix to look up routes for
     * @return all stored resolved routes for this prefix
     */
    Collection<ResolvedRoute> getAllRoutes(IpPrefix prefix);

    /**
     * Performs a longest prefix match of the best routes on the given IP address.
     *
     * @param ip IP address
     * @return optional longest matching route
     */
    Optional<ResolvedRoute> longestPrefixMatch(IpAddress ip);
}
