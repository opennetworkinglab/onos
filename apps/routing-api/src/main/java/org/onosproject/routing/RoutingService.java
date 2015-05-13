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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

/**
 * Provides a way of interacting with the RIB management component.
 */
public interface RoutingService {

    /**
     * Specifies the type of an IP address or an IP prefix location.
     */
    static enum LocationType {
        /**
         * The location of an IP address or an IP prefix is in local SDN network.
         */
        LOCAL,
        /**
         * The location of an IP address or an IP prefix is outside local SDN network.
         */
        INTERNET,
        /**
         * There is no route for this IP address or IP prefix.
         */
        NO_ROUTE
    }

    /**
     * Specifies the type of traffic.
     * <p>
     * We classify traffic by the first packet of each traffic.
     * </p>
     */
    enum TrafficType {
        /**
         * Traffic from a host located in local SDN network wants to
         * communicate with destination host located in Internet (outside
         * local SDN network).
         */
        HOST_TO_INTERNET,
        /**
         * Traffic from Internet wants to communicate with a host located
         * in local SDN network.
         */
        INTERNET_TO_HOST,
        /**
         * Both the source host and destination host of a traffic are in
         * local SDN network.
         */
        HOST_TO_HOST,
        /**
         * Traffic from Internet wants to traverse local SDN network.
         */
        INTERNET_TO_INTERNET,
        /**
         * Any traffic wants to communicate with a destination which has
         * no route, or traffic from Internet wants to access a local private
         * IP address.
         */
        DROP,
        /**
         * Traffic does not belong to the types above.
         */
        UNKNOWN
    }

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
     * Adds intent creation and submission listener.
     *
     * @param intentRequestListener listener to send intent creation and
     *        submission request to
     */
    void addIntentRequestListener(IntentRequestListener
                                         intentRequestListener);

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
     * Evaluates the location of an IP address and returns the location type.
     *
     * @param ipAddress the IP address to evaluate
     * @return the IP address location type
     */
    LocationType getLocationType(IpAddress ipAddress);

    /**
     * Finds out the route entry which has the longest matchable IP prefix.
     *
     * @param ipAddress IP address used to find out longest matchable IP prefix
     * @return a route entry which has the longest matchable IP prefix if
     * found, otherwise null
     */
    RouteEntry getLongestMatchableRouteEntry(IpAddress ipAddress);

    /**
     * Finds out the egress connect point where to emit the first packet
     * based on destination IP address.
     *
     * @param dstIpAddress the destination IP address
     * @return the egress connect point if found, otherwise null
     */
    ConnectPoint getEgressConnectPoint(IpAddress dstIpAddress);

    /**
     * Routes packet reactively.
     *
     * @param dstIpAddress the destination IP address of a packet
     * @param srcIpAddress the source IP address of a packet
     * @param srcConnectPoint the connect point where a packet comes from
     * @param srcMacAddress the source MAC address of a packet
     */
    void packetReactiveProcessor(IpAddress dstIpAddress,
                                        IpAddress srcIpAddress,
                                        ConnectPoint srcConnectPoint,
                                        MacAddress srcMacAddress);
}
