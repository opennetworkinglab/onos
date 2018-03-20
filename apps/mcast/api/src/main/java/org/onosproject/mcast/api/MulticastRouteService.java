/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mcast.api;

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Optional;
import java.util.Set;

/**
 * A service interface for maintaining multicast information.
 */
@Beta
public interface MulticastRouteService
        extends ListenerService<McastEvent, McastListener> {

    /**
     * Adds an empty route to the information base for the given group IP.
     *
     * @param route a multicast route
     */
    void add(McastRoute route);

    /**
     * Removes a route from the information base.
     *
     * @param route a multicast route
     */
    void remove(McastRoute route);

    /**
     * Gets all multicast routes in the system.
     *
     * @return set of multicast routes
     */
    Set<McastRoute> getRoutes();

    /**
     * Gets a multicast route in the system.
     *
     * @param groupIp multicast group IP address
     * @param sourceIp multicasto source Ip address
     * @return set of multicast routes
     */
    Optional<McastRoute> getRoute(IpAddress groupIp, IpAddress sourceIp);

    /**
     * Adds a set of source to the route from where the
     * data stream is originating.
     *
     * @param route   the multicast route
     * @param sources a set of sources
     */
    void addSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes all the sources from the route.
     *
     * @param route the multicast route
     */
    void removeSources(McastRoute route);

    /**
     * Removes a set of sources from the route.
     *
     * @param route   the multicast route
     * @param sources a set of sources
     */
    void removeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Adds a set of sink to the route to which a data stream should be
     * sent to.
     *
     * @param route  a multicast route
     * @param hostId a sink host
     */
    void addSink(McastRoute route, HostId hostId);

    /**
     * Adds a set of sink to the route to which a data stream should be
     * sent to. If this method is used this the connect points will all
     * be used a sink for that Mcast Tree. For dual-homed sinks please use
     * {@link #addSink(McastRoute route, HostId hostId) addSink}.
     *
     * @param route a multicast route
     * @param sinks a set of sink connect point
     */
    void addSink(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Removes all the sinks from the route.
     *
     * @param route the multicast route
     */
    void removeSinks(McastRoute route);

    /**
     * Removes a sink host from the route.
     *
     * @param route  the multicast route
     * @param hostId a sink host
     */
    void removeSink(McastRoute route, HostId hostId);

    /**
     * Removes a set of sink connect points for a given host the route.
     *
     * @param route         the multicast route
     * @param hostId        a sink host
     * @param connectPoints a given set of connect points to remove
     */
    void removeSinks(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Removes a set of sinks to the route to which a data stream should be
     * sent to. If this method is used the mcast tree does not work
     * for any other sink until it's added. For dual-homed sinks please use
     * {@link #removeSink(McastRoute route, HostId hostId) removeSink}.
     *
     * @param route a multicast route
     * @param sink  a sink connect point
     */
    void removeSinks(McastRoute route, Set<ConnectPoint> sink);

    /**
     * Return the Data for this route.
     *
     * @param route route
     * @return the mcast route data
     */
    McastRouteData routeData(McastRoute route);

    /**
     * Find the data source association for this multicast route.
     *
     * @param route a multicast route
     * @return a connect point
     */
    Set<ConnectPoint> sources(McastRoute route);

    /**
     * Find the list of sinks for this route.
     *
     * @param route a multicast route
     * @return a list of connect points
     */
    Set<ConnectPoint> sinks(McastRoute route);

    /**
     * Find the list of sinks for a given host for this route.
     *
     * @param route  a multicast route
     * @param hostId the host
     * @return a list of connect points
     */
    Set<ConnectPoint> sinks(McastRoute route, HostId hostId);

    /**
     * Obtains all the non host specific sinks for a multicast route.
     *
     * @param route a multicast route
     * @return a set of sinks
     */
    Set<ConnectPoint> nonHostSinks(McastRoute route);
}
