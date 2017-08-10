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
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onosproject.store.Store;

/**
 * EVPN route store.
 */
public interface EvpnRouteStore extends Store<EvpnInternalRouteEvent,
        EvpnRouteStoreDelegate> {

    /**
     * Adds or updates the given route in the store.
     *
     * @param route route to add or update
     */
    void updateRoute(EvpnRoute route);

    /**
     * Removes the given route from the store.
     *
     * @param route route to remove
     */
    void removeRoute(EvpnRoute route);

    /**
     * Returns the IDs for all route tables in the store.
     *
     * @return route table IDs
     */
    Set<EvpnRouteTableId> getRouteTables();

    /**
     * Returns the routes in the given route table, grouped by prefix.
     *
     * @param table route table ID
     * @return routes
     */
    Collection<EvpnRouteSet> getRoutes(EvpnRouteTableId table);

    /**
     * Returns the routes that point to the given next hop IP address.
     *
     * @param ip IP address of the next hop
     * @return routes for the given next hop
     */
    // TODO think about including route table info
    Collection<EvpnRoute> getRoutesForNextHop(IpAddress ip);

}
