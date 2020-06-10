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

import java.util.Set;

/**
 * A service interface for maintaining Multicast information.
 */
@Beta
public interface MulticastRouteService
        extends ListenerService<McastEvent, McastListener> {

    /**
     * Adds an empty route to the information base for the given group IP.
     *
     * @param route a Multicast route
     */
    void add(McastRoute route);

    /**
     * Removes a route from the information base.
     *
     * @param route a Multicast route
     */
    void remove(McastRoute route);

    /**
     * Gets all Multicast routes in the system.
     *
     * @return set of Multicast routes
     */
    Set<McastRoute> getRoutes();

    /**
     * Gets a Multicast route in the system.
     *
     * @param groupIp  Multicast group IP address
     * @param sourceIp Multicasto source Ip address
     * @return set of Multicast routes
     */
    Set<McastRoute> getRoute(IpAddress groupIp, IpAddress sourceIp);

    /**
     * Adds a host as a source to the route from where the
     * data stream is originating.
     *
     * @param route  the Multicast route
     * @param source a source host
     */
    void addSource(McastRoute route, HostId source);


    /**
     * Adds a set of source connect points for a given host source to the route to
     * which a data stream should be sent to.
     *
     * @param route         a Multicast route
     * @param hostId        a source host
     * @param connectPoints the source for the specific host
     */
    void addSources(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Adds a set of sources to the route from which a data stream should be
     * sent to. If this method is used the connect points will all be
     * used as different sources for that Mcast Tree. For dual-homed sources
     * please use {@link #addSource(McastRoute route, HostId hostId) addSource}.
     *
     * @param route   a Multicast route
     * @param sources a set of source connect points
     */
    void addSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes all the sources connect points from the route.
     *
     * @param route the Multicast route
     */
    void removeSources(McastRoute route);

    /**
     * Removes a source host from the route.
     *
     * @param route  the Multicast route
     * @param source a host source
     */
    void removeSource(McastRoute route, HostId source);

    /**
     * Removes a set of sources from the route.
     * If this method is used the connect points will all be
     * used as different sources for that Mcast Tree. For dual-homed sources
     * please use {@link #removeSource(McastRoute, HostId)}.
     *
     * @param route   the multicast route
     * @param sources set of sources
     */
    void removeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes a set of source connect points for a given host source from the route.
     *
     * @param route         a multicast route
     * @param hostId        a source host
     * @param connectPoints the source for the specific connect points
     */
    void removeSources(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Adds a sink to the route to which a data stream should be
     * sent to.
     *
     * @param route  a Multicast route
     * @param hostId a sink host
     */
    void addSink(McastRoute route, HostId hostId);

    /**
     * Adds a set of sink connect points for a given host sink to the route to
     * which a data stream should be sent to.
     *
     * @param route         a Multicast route
     * @param hostId        a sink host
     * @param connectPoints the sink for the specific host
     */
    void addSinks(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Adds a set of sink to the route to which a data stream should be
     * sent to. If this method is used the connect points will all be
     * used as different sinks for that Mcast Tree. For dual-homed sinks
     * please use {@link #addSink(McastRoute route, HostId hostId) addSink}.
     *
     * @param route a Multicast route
     * @param sinks a set of sink connect point
     */
    void addSinks(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Removes all the sinks from the route.
     *
     * @param route the Multicast route
     */
    void removeSinks(McastRoute route);

    /**
     * Removes a sink from the route.
     *
     * @param route  the Multicast route
     * @param hostId a sink host
     */
    void removeSink(McastRoute route, HostId hostId);

    /**
     * Removes a set of sinks to the route to which a data stream should be
     * sent to. If this method is used the mcast tree does not work
     * for any other sink until it's added. For dual-homed sinks please use
     * {@link #removeSink(McastRoute route, HostId hostId) removeSink}.
     *
     * @param route a Multicast route
     * @param sink  a sink connect point
     */
    void removeSinks(McastRoute route, Set<ConnectPoint> sink);

    /**
     * Return the Multicast data for this route.
     *
     * @param route route
     * @return the mcast route data
     */
    McastRouteData routeData(McastRoute route);

    /**
     * Find the data source association for this Multicast route.
     *
     * @param route a Multicast route
     * @return a connect point
     */
    Set<ConnectPoint> sources(McastRoute route);

    /**
     * Find the set of connect points for a given source for this route.
     *
     * @param route  a Multicast route
     * @param hostId the host
     * @return a list of connect points
     */
    Set<ConnectPoint> sources(McastRoute route, HostId hostId);

    /**
     * Find the list of sinks for this route.
     *
     * @param route a Multicast route
     * @return a list of connect points
     */
    Set<ConnectPoint> sinks(McastRoute route);

    /**
     * Find the set of connect points for a given sink for this route.
     *
     * @param route  a Multicast route
     * @param hostId the host
     * @return a list of connect points
     */
    Set<ConnectPoint> sinks(McastRoute route, HostId hostId);

    /**
     * Obtains all the non host specific sinks for a Multicast route.
     *
     * @param route a Multicast route
     * @return a set of sinks
     */
    Set<ConnectPoint> nonHostSinks(McastRoute route);
}
