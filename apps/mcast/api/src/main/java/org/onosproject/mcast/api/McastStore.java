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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Entity responsible for storing Multicast state information.
 */
@Beta
public interface McastStore extends Store<McastEvent, McastStoreDelegate> {

    /**
     * Updates the store with the route information.
     *
     * @param route a Multicast route
     */
    void storeRoute(McastRoute route);

    /**
     * Removes from the store the route information.
     *
     * @param route a Multicast route
     */
    void removeRoute(McastRoute route);

    /**
     * Updates the store with a host based source information for a given route. There may be
     * multiple source connect points for the given host.
     *
     * @param route         a Multicast route
     * @param hostId        the host source
     * @param connectPoints the sources connect point
     */
    void storeSource(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Updates the store with source information for a given route.
     * The source stored with this method are not tied with any host.
     * Traffic will be sent from all of them.
     *
     * @param route   a Multicast route
     * @param sources set of specific connect points
     */
    void storeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes from the store all the sources information for a given route.
     *
     * @param route a Multicast route
     */
    void removeSources(McastRoute route);

    /**
     * Removes from the store the source information for the given route.
     *
     * @param route  a Multicast route
     * @param source a source
     */
    void removeSource(McastRoute route, HostId source);

    /**
     * Removes a set of source connect points for a given route.
     * This method is not tied with any host.
     *
     * @param route   a Multicast route
     * @param sources set of specific connect points
     */
    void removeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes a set of source connect points for a given host the route.
     *
     * @param route         the multicast route
     * @param hostId        a source host
     * @param connectPoints a given set of connect points to remove
     */
    void removeSources(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints);

    /**
     * Updates the store with a host based sink information for a given route. There may be
     * multiple sink connect points for the given host.
     *
     * @param route  a Multicast route
     * @param hostId the host sink
     * @param sinks  the sinks
     */
    void addSink(McastRoute route, HostId hostId, Set<ConnectPoint> sinks);

    /**
     * Updates the store with sinks information for a given route.
     * The sinks stored with this method are not tied with any host.
     * Traffic will be sent to all of them.
     *
     * @param route a Multicast route
     * @param sinks set of specific connect points
     */
    void addSinks(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Removes from the store all the sink information for a given route.
     *
     * @param route a Multicast route
     */
    void removeSinks(McastRoute route);

    /**
     * Removes from the store the complete set of sink information for a given host for a given route.
     *
     * @param route  a Multicast route
     * @param hostId a specific host
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
     * Removes from the store the set of non host bind sink information for a given route.
     *
     * @param route a Multicast route
     * @param sinks a set of Multicast sinks
     */
    void removeSinks(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Obtains the sources for a Multicast route.
     *
     * @param route a Multicast route
     * @return a connect point
     */
    Set<ConnectPoint> sourcesFor(McastRoute route);

    /**
     * Obtains the sources for a given host for a given Multicast route.
     *
     * @param route  a Multicast route
     * @param hostId the host
     * @return a set of sources
     */
    Set<ConnectPoint> sourcesFor(McastRoute route, HostId hostId);

    /**
     * Obtains the sinks for a Multicast route.
     *
     * @param route a Multicast route
     * @return a set of sinks
     */
    Set<ConnectPoint> sinksFor(McastRoute route);

    /**
     * Obtains the sinks for a given host for a given Multicast route.
     *
     * @param route  a Multicast route
     * @param hostId the host
     * @return a set of sinks
     */
    Set<ConnectPoint> sinksFor(McastRoute route, HostId hostId);

    /**
     * Gets the set of all known Multicast routes.
     *
     * @return set of Multicast routes.
     */
    Set<McastRoute> getRoutes();

    /**
     * Gets the Multicast data for a given route.
     *
     * @param route the route
     * @return set of Multicast routes.
     */
    McastRouteData getRouteData(McastRoute route);
}
