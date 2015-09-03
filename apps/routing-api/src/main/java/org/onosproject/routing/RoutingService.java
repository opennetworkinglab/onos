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

import org.onlab.packet.IpAddress;
import org.onosproject.routing.config.BgpConfig;

import java.util.Collection;

/**
 * Provides a way of interacting with the RIB management component.
 */
public interface RoutingService {

    String ROUTER_APP_ID = "org.onosproject.router";

    Class<BgpConfig> CONFIG_CLASS = BgpConfig.class;

    /**
     * Starts the routing service.
     */
    void start();

    /**
     * Adds FIB listener.
     *
     * @param fibListener listener to send FIB updates to
     */
    void addFibListener(FibListener fibListener);

    /**
     * Stops the routing service.
     */
    void stop();

    /**
     * Gets all IPv4 routes known to SDN-IP.
     *
     * @return the SDN-IP IPv4 routes
     */
    Collection<RouteEntry> getRoutes4();

    /**
     * Gets all IPv6 routes known to SDN-IP.
     *
     * @return the SDN-IP IPv6 routes
     */
    Collection<RouteEntry> getRoutes6();

    /**
     * Finds out the route entry which has the longest matchable IP prefix.
     *
     * @param ipAddress IP address used to find out longest matchable IP prefix
     * @return a route entry which has the longest matchable IP prefix if
     * found, otherwise null
     */
    RouteEntry getLongestMatchableRouteEntry(IpAddress ipAddress);

}
