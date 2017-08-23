/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.mcast;

import org.onosproject.net.ConnectPoint;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Entity responsible for storing multicast state information.
 *
 * @deprecated in 1.11 ("Loon") release. To be moved into an app.
 */

@Deprecated
public interface McastStore extends Store<McastEvent, McastStoreDelegate> {

    enum Type {
        /**
         * Adding a route to the mcast rib.
         */
        ADD,

        /**
         * Removing a route from the mcast rib.
         */
        REMOVE
    }

    /**
     * Updates the store with the route information.
     *
     * @param route a multicast route
     * @param operation an operation
     */
    void storeRoute(McastRoute route, Type operation);

    /**
     * Updates the store with source information for the given route. Only one
     * source is permitted. Submitting another source will replace the previous
     * value.
     *
     * @param route a multicast route
     * @param source a source
     */
    void storeSource(McastRoute route, ConnectPoint source);

    /**
     * Updates the store with sink information for a given route. There may be
     * multiple sinks.
     *
     * @param route a multicast route
     * @param sink a sink
     * @param operation an operation
     */
    void storeSink(McastRoute route, ConnectPoint sink, Type operation);

    /**
     * Obtains the source for a multicast route.
     *
     * @param route a multicast route
     * @return a connect point
     */
    ConnectPoint sourceFor(McastRoute route);

    /**
     * Obtains the sinks for a multicast route.
     *
     * @param route a multicast route
     * @return a set of sinks
     */
    Set<ConnectPoint> sinksFor(McastRoute route);

    /**
     * Gets the set of all known multicast routes.
     *
     * @return set of multicast routes
     */
    Set<McastRoute> getRoutes();
}
