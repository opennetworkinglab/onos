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

package org.onosproject.routeservice;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.NotImplementedException;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.Set;

/**
 * Unicast route store.
 */
public interface RouteStore extends Store<InternalRouteEvent, RouteStoreDelegate> {

    /**
     * Adds or updates the given route in the store.
     *
     * @param route route to add or update
     */
    void updateRoute(Route route);

    /**
     * Adds or updates the given routes in the store.
     *
     * @param routes routes to add or update
     */
    void updateRoutes(Collection<Route> routes);

    /**
     * Removes the given route from the store.
     *
     * @param route route to remove
     */
    void removeRoute(Route route);

    /**
     * Removes the given routes from the store.
     *
     * @param routes routes to remove
     */
    void removeRoutes(Collection<Route> routes);

    /**
     * Replaces the all the routes for a prefix
     * with the given route.
     *
     * @param route route
     */
    default void replaceRoute(Route route) {
        throw new NotImplementedException("replaceRoute is not implemented");
    }

    /**
     * Returns the IDs for all route tables in the store.
     *
     * @return route table IDs
     */
    Set<RouteTableId> getRouteTables();

    /**
     * Returns the routes in the given route table, grouped by prefix.
     *
     * @param table route table ID
     * @return routes
     */
    Collection<RouteSet> getRoutes(RouteTableId table);

    /**
     * Returns the routes that point to the given next hop IP address.
     *
     * @param ip IP address of the next hop
     * @return routes for the given next hop
     */
    // TODO think about including route table info
    Collection<Route> getRoutesForNextHop(IpAddress ip);

    /**
     * Returns all routes that point to any of the given next hops IP addresses.
     *
     * @param nextHops next hops IP addresses
     * @return collection of routes sets
     */
    Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> nextHops);

    /**
     * Returns the set of routes in the default route table for the given prefix.
     *
     * @param prefix IP prefix
     * @return route set
     */
    // TODO needs to be generalizable across route tables
    @Beta
    RouteSet getRoutes(IpPrefix prefix);

    /**
     * Returns the name of this route store.
     *
     * @return the name of this route store
     */
    default String name() {
        return getClass().getName();
    }
}
