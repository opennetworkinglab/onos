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

package org.onosproject.evpnrouteservice;

import java.util.Collection;

import org.onlab.packet.IpAddress;

/**
 * Represents a route table that stores routes.
 */
public interface EvpnTable {

    /**
     * Adds a route to the route table.
     *
     * @param route route
     */
    void update(EvpnRoute route);

    /**
     * Removes a route from the route table.
     *
     * @param route route
     */
    void remove(EvpnRoute route);

    /**
     * Returns the route table ID.
     *
     * @return route table ID
     */
    EvpnRouteTableId id();

    /**
     * Returns all routes in the route table.
     *
     * @return collection of routes, grouped by prefix
     */
    Collection<EvpnRouteSet> getRoutes();

    /**
     * Returns the routes in this table pertaining to a given prefix.
     *
     * @param prefix IP prefix
     * @return routes for the prefix
     */
    EvpnRouteSet getRoutes(EvpnPrefix prefix);

    /**
     * Returns all routes that have the given next hop.
     *
     * @param nextHop next hop IP address
     * @return collection of routes
     */
    Collection<EvpnRoute> getRoutesForNextHop(IpAddress nextHop);

    /**
     * Releases route table resources held locally.
     */
    void shutdown();

    /**
     * Releases route table resources across the entire cluster.
     */
    void destroy();

}
