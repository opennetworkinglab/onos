/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.store.routing.impl;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteSet;
import org.onosproject.incubator.net.routing.RouteTableId;

import java.util.Collection;
import java.util.Collections;

/**
 * Route table that contains no routes.
 */
public final class EmptyRouteTable implements RouteTable {

    private final RouteTableId id = new RouteTableId("empty");

    private static final EmptyRouteTable INSTANCE = new EmptyRouteTable();

    /**
     * Returns the instance of the empty route table.
     *
     * @return empty route table
     */
    public static EmptyRouteTable instance() {
        return INSTANCE;
    }

    private EmptyRouteTable() {
    }

    @Override
    public void update(Route route) {

    }

    @Override
    public void remove(Route route) {

    }

    @Override
    public RouteTableId id() {
        return id;
    }

    @Override
    public Collection<RouteSet> getRoutes() {
        return Collections.emptyList();
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress nextHop) {
        return Collections.emptyList();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void destroy() {

    }
}
