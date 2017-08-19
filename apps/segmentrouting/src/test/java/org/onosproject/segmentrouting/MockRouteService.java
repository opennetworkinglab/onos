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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteServiceAdapter;
import org.onosproject.routeservice.RouteTableId;

import java.util.Collection;
import java.util.Set;

/**
 * Mock Route Service.
 * We assume there is only one routing table named "default".
 */
public class MockRouteService extends RouteServiceAdapter {
    private Set<RouteInfo> routes;

    MockRouteService(Set<RouteInfo> routes) {
        this.routes = ImmutableSet.copyOf(routes);
    }

    @Override
    public Collection<RouteInfo> getRoutes(RouteTableId id) {
        return routes;
    }

    @Override
    public Collection<RouteTableId> getRouteTables() {
        return Sets.newHashSet(new RouteTableId("default"));
    }
}