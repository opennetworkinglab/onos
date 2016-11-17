/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.routing;

import org.onlab.packet.IpAddress;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Unicast route store.
 */
public interface RouteStore extends Store<RouteEvent, RouteStoreDelegate> {

    /**
     * Adds or updates the given route in the store.
     *
     * @param route route to add or update
     */
    void updateRoute(Route route);

    /**
     * Removes the given route from the store.
     *
     * @param route route to remove
     */
    void removeRoute(Route route);

    /**
     * Returns the IDs for all route tables in the store.
     *
     * @return route table IDs
     */
    Set<RouteTableId> getRouteTables();

    /**
     * Returns the routes for a particular route table.
     *
     * @param table route table
     * @return collection of route in the table
     */
    Collection<Route> getRoutes(RouteTableId table);

    /**
     * Performs a longest prefix match with the given IP address.
     *
     * @param ip IP to look up
     * @return longest prefix match route
     */
    Route longestPrefixMatch(IpAddress ip);

    /**
     * Returns the routes that point to the given next hop IP address.
     *
     * @param ip IP address of the next hop
     * @return routes for the given next hop
     */
    Collection<Route> getRoutesForNextHop(IpAddress ip);

    /**
     * Updates a next hop information in the store.
     *
     * @param ip IP address
     * @param nextHopData Information of the next hop
     */
    void updateNextHop(IpAddress ip, NextHopData nextHopData);

    /**
     * Removes a next hop information from the store.
     *
     * @param ip IP address
     * @param nextHopData Information of the next hop
     */
    void removeNextHop(IpAddress ip, NextHopData nextHopData);

    /**
     * Returns the information of the given next hop.
     *
     * @param ip next hop IP
     * @return Information of the next hop
     */
    NextHopData getNextHop(IpAddress ip);

    /**
     * Returns all next hops in the route store.
     *
     * @return next hops
     */
    Map<IpAddress, NextHopData> getNextHops();
}
