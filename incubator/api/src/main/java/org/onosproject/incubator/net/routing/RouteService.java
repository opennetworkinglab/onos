/*
 * Copyright 2016 Open Networking Laboratory
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

/**
 * IP unicast routing service.
 */
public interface RouteService extends ListenerService<RouteEvent, RouteListener> {

    /**
     * Gets all routes.
     *
     * @return collection of all routes
     */
    Collection<Route> getRoutes();

    /**
     * Gets routes learnt from a particular source.
     *
     * @param source route source
     * @return collection of routes
     */
    Collection<Route> getRoutesFromSource(Route.Source source);

    /**
     * Gets the longest prefix route that matches the given IP address.
     *
     * @param ip IP addres
     * @return longest prefix matched route
     */
    Route longestPrefixMatch(IpAddress ip);

    //TODO should mutation methods be pushed to a different interface?
    /**
     * Updates the given routes in the route service.
     *
     * @param routes collection of routes to update
     */
    void update(Collection<Route> routes);

    /**
     * Withdraws the given routes from the route service.
     *
     * @param routes collection of routes to withdraw
     */
    void withdraw(Collection<Route> routes);
}
