/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.Map;
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
     * Returns the set of routes in the default route table for the given prefix.
     *
     * @param prefix IP prefix
     * @return route set
     */
    // TODO needs to be generalizable across route tables
    @Beta
    RouteSet getRoutes(IpPrefix prefix);

    /**
     * Updates a next hop information in the store.
     *
     * @param ip IP address
     * @param nextHopData Information of the next hop
     */
    @Deprecated
    void updateNextHop(IpAddress ip, NextHopData nextHopData);

    /**
     * Removes a next hop information from the store.
     *
     * @param ip IP address
     * @param nextHopData Information of the next hop
     */
    @Deprecated
    void removeNextHop(IpAddress ip, NextHopData nextHopData);

    /**
     * Returns the information of the given next hop.
     *
     * @param ip next hop IP
     * @return Information of the next hop
     */
    @Deprecated
    NextHopData getNextHop(IpAddress ip);

    /**
     * Returns all next hops in the route store.
     *
     * @return next hops
     */
    @Deprecated
    Map<IpAddress, NextHopData> getNextHops();

    /**
     * Performs a longest prefix match with the given IP address.
     *
     * @param ip IP to look up
     * @return longest prefix match route
     * @deprecated in Kingfisher release. Now handled by the manager instead of
     * the store
     */
    @Deprecated
    Route longestPrefixMatch(IpAddress ip);
}
