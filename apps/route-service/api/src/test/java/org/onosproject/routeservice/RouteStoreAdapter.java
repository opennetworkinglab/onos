/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.routeservice;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Collection;
import java.util.Set;

/**
 * Adapter class for the route store.
 */
public class RouteStoreAdapter implements RouteStore {
    @Override
    public void updateRoute(Route route) {

    }

    @Override
    public void updateRoutes(Collection<Route> routes) {

    }

    @Override
    public void removeRoute(Route route) {

    }

    @Override
    public void removeRoutes(Collection<Route> routes) {

    }

    @Override
    public void replaceRoute(Route route) {

    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return null;
    }

    @Override
    public Collection<RouteSet> getRoutes(RouteTableId table) {
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return null;
    }

    @Override
    public Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> nextHops) {
        return null;
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        return null;
    }

    @Override
    public void setDelegate(RouteStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(RouteStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
    }
}
