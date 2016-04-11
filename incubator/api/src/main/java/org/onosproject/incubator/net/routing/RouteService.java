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
import org.onosproject.event.ListenerService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Unicast IP route service.
 */
public interface RouteService extends ListenerService<RouteEvent, RouteListener> {

    /**
     * Returns all routes for all route tables in the system.
     *
     * @return map of route table name to routes in that table
     */
    Map<RouteTableId, Collection<Route>> getAllRoutes();

    /**
     * Performs a longest prefix match on the given IP address. The call will
     * return the route with the most specific prefix that contains the given
     * IP address.
     *
     * @param ip IP address
     * @return longest prefix matched route
     */
    Route longestPrefixMatch(IpAddress ip);

    /**
     * Returns the routes for the given next hop.
     *
     * @param nextHop next hop IP address
     * @return routes for this next hop
     */
    Collection<Route> getRoutesForNextHop(IpAddress nextHop);

    /**
     * Returns all next hops in the route store.
     *
     * @return set of next hops
     */
    Set<NextHop> getNextHops();

}
