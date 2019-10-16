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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteSet;
import org.onosproject.routeservice.RouteTableId;

import java.util.Collection;

/**
 * Represents a route table that stores routes.
 */
public interface RouteTable {

    /**
     * Adds a route to the route table.
     *
     * @param route route
     */
    void update(Route route);

    /**
     * Adds the routes to the route table.
     *
     * @param routes routes
     */
    void update(Collection<Route> routes);

    /**
     * Removes a route from the route table.
     *
     * @param route route
     */
    void remove(Route route);

    /**
     * Removes the routes from the route table.
     *
     * @param routes routes
     */
    void remove(Collection<Route> routes);

    /**
     * Replaces a route in the route table.
     *
     * @param route route
     */
    void replace(Route route);

    /**
     * Returns the route table ID.
     *
     * @return route table ID
     */
    RouteTableId id();

    /**
     * Returns all routes in the route table.
     *
     * @return collection of routes, grouped by prefix
     */
    Collection<RouteSet> getRoutes();

    /**
     * Returns the routes in this table pertaining to a given prefix.
     *
     * @param prefix IP prefix
     * @return routes for the prefix
     */
    RouteSet getRoutes(IpPrefix prefix);

    /**
     * Returns all routes that have the given next hop.
     *
     * @param nextHop next hop IP address
     * @return collection of routes
     */
    Collection<Route> getRoutesForNextHop(IpAddress nextHop);

    /**
     * Returns all routes that have the given next hops.
     *
     * @param nextHops next hops IP addresses
     * @return collection of routes sets
     */
    Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> nextHops);

    /**
     * Releases route table resources held locally.
     */
    void shutdown();

    /**
     * Releases route table resources across the entire cluster.
     */
    void destroy();

}
