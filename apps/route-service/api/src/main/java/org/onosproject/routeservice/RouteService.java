/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.event.ListenerService;

import java.util.Collection;
import java.util.Optional;

/**
 * Unicast IP route service.
 */
public interface RouteService extends ListenerService<RouteEvent, RouteListener> {

    /**
     * Returns information about all routes in the given route table.
     *
     * @param id route table ID
     * @return collection of route information
     */
    Collection<RouteInfo> getRoutes(RouteTableId id);

    /**
     * Returns all resolved routes in the given route table.
     *
     * @param id route table ID
     * @return collection of resolved route
     */
    Collection<ResolvedRoute> getResolvedRoutes(RouteTableId id);

    /**
     * Returns the set of route tables in the system.
     *
     * @return collection of route table IDs.
     */
    Collection<RouteTableId> getRouteTables();

    /**
     * Performs a longest prefix lookup on the given IP address.
     *
     * @param ip IP address to look up
     * @return most specific matching route, if one exists
     */
    Optional<ResolvedRoute> longestPrefixLookup(IpAddress ip);

    /**
     * Returns all resolved routes stored for the given prefix, including the
     * best selected route.
     *
     * @param prefix IP prefix to look up routes for
     * @return all stored resolved routes for this prefix
     */
    Collection<ResolvedRoute> getAllResolvedRoutes(IpPrefix prefix);

    /**
     * Performs a longest prefix match on the given IP address. The call will
     * return the route with the most specific prefix that contains the given
     * IP address.
     *
     * @param ip IP address
     * @return longest prefix matched route
     * @deprecated in Kingfisher release. Use {{@link #longestPrefixLookup(IpAddress)}}
     * instead.
     */
    @Deprecated
    Route longestPrefixMatch(IpAddress ip);
}
