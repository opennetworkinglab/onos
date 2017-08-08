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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Route configuration object for Route Service.
 */
public class RouteConfig extends Config<ApplicationId> {
    private static final String PREFIX = "prefix";
    private static final String NEXTHOP = "nextHop";

    /**
     * Returns all routes in this configuration.
     *
     * @return A set of route.
     */
    public Set<Route> getRoutes() {
        ImmutableSet.Builder<Route> routes = ImmutableSet.builder();
        array.forEach(route -> {
            try {
                IpPrefix prefix = IpPrefix.valueOf(route.path(PREFIX).asText());
                IpAddress nextHop = IpAddress.valueOf(route.path(NEXTHOP).asText());
                routes.add(new Route(Route.Source.STATIC, prefix, nextHop));
            } catch (IllegalArgumentException e) {
                // Ignores routes that cannot be parsed correctly
            }
        });
        return routes.build();
    }

    @Override
    public boolean isValid() {
        array.forEach(routeNode -> {
            if (!routeNode.isObject()) {
                throw new IllegalArgumentException("Not object node");
            }
            ObjectNode route = (ObjectNode) routeNode;
            isIpPrefix(route, PREFIX, FieldPresence.MANDATORY);
            isIpAddress(route, NEXTHOP, FieldPresence.MANDATORY);
        });
        return true;
    }
}
