/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Collection;
import java.util.Optional;

/**
 * Adapter class for the route service.
 */
public class RouteServiceAdapter implements RouteAdminService {
    @Override
    public void update(Collection<Route> routes) {

    }

    @Override
    public void withdraw(Collection<Route> routes) {

    }

    @Override
    public Collection<RouteInfo> getRoutes(RouteTableId id) {
        return null;
    }

    @Override
    public Collection<ResolvedRoute> getResolvedRoutes(RouteTableId id) {
        return null;
    }

    @Override
    public Collection<RouteTableId> getRouteTables() {
        return null;
    }

    @Override
    public Route longestPrefixMatch(IpAddress ip) {
        return null;
    }

    @Override
    public Optional<ResolvedRoute> longestPrefixLookup(IpAddress ip) {
        return null;
    }

    @Override
    public Collection<ResolvedRoute> getAllResolvedRoutes(IpPrefix prefix) {
        return ImmutableList.of();
    }

    @Override
    public void addListener(RouteListener listener) {

    }

    @Override
    public void removeListener(RouteListener listener) {

    }
}
