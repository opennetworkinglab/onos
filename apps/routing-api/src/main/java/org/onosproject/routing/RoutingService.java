/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.routing;

import java.util.Collection;

/**
 * Provides a way of interacting with the RIB management component.
 */
public interface RoutingService {

    /**
     * Starts the routing service.
     *
     * @param listener listener to send FIB updates to
     */
    public void start(FibListener listener);

    /**
     * Stops the routing service.
     */
    public void stop();

    /**
     * Gets all IPv4 routes known to SDN-IP.
     *
     * @return the SDN-IP IPv4 routes
     */
    public Collection<RouteEntry> getRoutes4();

    /**
     * Gets all IPv6 routes known to SDN-IP.
     *
     * @return the SDN-IP IPv6 routes
     */
    public Collection<RouteEntry> getRoutes6();
}
