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
package org.onosproject.net.mcast;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;

import java.util.List;

/**
 * A service interface for maintaining multicast information.
 */
@Beta
public interface MulticastRouteService {

    /**
     * Adds a route to the information base.
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
     * Adds a source connection to the route from where the
     * data stream is originating.
     *
     * @param route the multicast route
     * @param connectPoint a source connect point
     */
    void addSource(McastRoute route, ConnectPoint connectPoint);

    /**
     * Adds a sink to the route to which a data stream should be
     * sent to.
     *
     * @param route a multicast route
     * @param connectPoint a sink connect point
     */
    void addSink(McastRoute route, ConnectPoint connectPoint);

    /**
     * Removes a sink from the route.
     *
     * @param route the multicast route
     * @param connectPoint a sink connect point
     */
    void removeSink(McastRoute route, ConnectPoint connectPoint);

    /**
     * Find the data source association for this multicast route.
     *
     * @param route a multicast route
     * @return a connect point
     */
    ConnectPoint fetchSource(McastRoute route);

    /**
     * Find the list of sinks for this route.
     *
     * @param route a multicast route
     * @return a list of connect points
     */
    List<ConnectPoint> fetchSinks(McastRoute route);
}
