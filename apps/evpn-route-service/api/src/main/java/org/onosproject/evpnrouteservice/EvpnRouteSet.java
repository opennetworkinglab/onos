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

package org.onosproject.evpnrouteservice;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A set of routes for a particular prefix in a route table.
 */
public class EvpnRouteSet {
    private final EvpnRouteTableId tableId;

    private final EvpnPrefix prefix;
    private final Set<EvpnRoute> routes;

    /**
     * Creates a new route set.
     *
     * @param tableId route table ID
     * @param prefix  IP prefix
     * @param routes  routes for the given prefix
     */
    public EvpnRouteSet(EvpnRouteTableId tableId, EvpnPrefix prefix, Set<EvpnRoute>
            routes) {
        this.tableId = checkNotNull(tableId);
        this.prefix = checkNotNull(prefix);
        this.routes = ImmutableSet.copyOf(checkNotNull(routes));
    }

    /**
     * Returns the route table ID.
     *
     * @return route table ID
     */
    public EvpnRouteTableId tableId() {
        return tableId;
    }

    /**
     * Returns the IP prefix.
     *
     * @return IP prefix
     */
    public EvpnPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the set of routes.
     *
     * @return routes
     */
    public Set<EvpnRoute> routes() {
        return routes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableId, prefix, routes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnRouteSet)) {
            return false;
        }

        EvpnRouteSet that = (EvpnRouteSet) other;

        return Objects.equals(this.tableId, that.tableId) &&
                Objects.equals(this.prefix, that.prefix) &&
                Objects.equals(this.routes, that.routes);
    }
}
