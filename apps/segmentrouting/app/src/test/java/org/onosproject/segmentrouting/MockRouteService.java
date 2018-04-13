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

import com.google.common.collect.Sets;
import org.onlab.packet.IpPrefix;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteServiceAdapter;
import org.onosproject.routeservice.RouteTableId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mock Route Service.
 * We assume there is only one routing table named "default".
 */
public class MockRouteService extends RouteServiceAdapter {
    private Map<IpPrefix, Set<ResolvedRoute>> routeStore;

    MockRouteService(Map<IpPrefix, Set<ResolvedRoute>> routeStore) {
        this.routeStore = routeStore;
    }

    @Override
    public Collection<RouteInfo> getRoutes(RouteTableId id) {
        return routeStore.entrySet().stream().map(e -> {
            IpPrefix prefix = e.getKey();
            Set<ResolvedRoute> resolvedRoutes = e.getValue();
            ResolvedRoute bestRoute =  resolvedRoutes.stream().findFirst().orElse(null);
            return new RouteInfo(prefix, bestRoute, resolvedRoutes);
        }).collect(Collectors.toSet());
    }

    @Override
    public Collection<RouteTableId> getRouteTables() {
        return Sets.newHashSet(new RouteTableId("default"));
    }
}