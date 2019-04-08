/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpPrefix;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;

/**
 * Utility class for route simplification.
 */
final class RouteSimplifierUtils {

    /*
     * When route with source type listed in leafExclusionRouteTypes,
     * it will programme only on the leaf pair the nexthop attaches to. Other leaves will be ignored.
     */
    private static final ImmutableList<Route.Source> LEAF_EXCLUSION_ROUTE_TYPES =
            ImmutableList.of(Route.Source.DHCP, Route.Source.RIP, Route.Source.DHCPLQ);

    private SegmentRoutingManager srManager;

    RouteSimplifierUtils(SegmentRoutingManager srManager) {

        this.srManager = srManager;
    }

    /**
     * Checking whether the leafExclusionRouteTypes contains the given source type.
     *
     * @param s source type
     * @return boolean if it containsd the source type.
     *
     */
    public boolean hasLeafExclusionEnabledForType(Route.Source s) {
        return LEAF_EXCLUSION_ROUTE_TYPES.contains(s);
    }

    /**
     * When route with any source of given prefix is  listed in leafExclusionRouteTypes,
     * it will programme only on the leaf pair the nexthop attaches to. Other leaves will be ignored.
     *
     * @param ipPrefix  ip prefix of the route.
     * @return boolean if contains the prefix of the mentioned source type.
     */
    public boolean hasLeafExclusionEnabledForPrefix(IpPrefix ipPrefix) {
        for (ResolvedRoute route : srManager.routeService.getAllResolvedRoutes(ipPrefix)) {
            if (hasLeafExclusionEnabledForType(route.route().source())) {
                return true;
            }
        }
        return false;
    }

}
