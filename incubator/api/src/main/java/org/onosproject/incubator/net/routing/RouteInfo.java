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

package org.onosproject.incubator.net.routing;

import com.google.common.annotations.Beta;
import org.onlab.packet.IpPrefix;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Routing information for a given prefix.
 */
@Beta
public class RouteInfo {

    private final IpPrefix prefix;
    private final ResolvedRoute bestRoute;
    private final Set<ResolvedRoute> allRoutes;

    /**
     * Creates a new route info object.
     *
     * @param prefix IP prefix
     * @param bestRoute best route for this prefix if one exists
     * @param allRoutes all known routes for this prefix
     */
    @Beta
    public RouteInfo(IpPrefix prefix, ResolvedRoute bestRoute, Set<ResolvedRoute> allRoutes) {
        this.prefix = checkNotNull(prefix);
        this.bestRoute = bestRoute;
        this.allRoutes = checkNotNull(allRoutes);
    }

    /**
     * Returns the IP prefix.
     *
     * @return IP prefix
     */
    public IpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the best route for this prefix if one exists.
     *
     * @return optional best route
     */
    public Optional<ResolvedRoute> bestRoute() {
        return Optional.ofNullable(bestRoute);
    }

    /**
     * Returns all routes for this prefix.
     *
     * @return all routes
     */
    public Set<ResolvedRoute> allRoutes() {
        return allRoutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, bestRoute, allRoutes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RouteInfo)) {
            return false;
        }

        RouteInfo that = (RouteInfo) other;

        return Objects.equals(this.prefix, that.prefix) &&
                Objects.equals(this.bestRoute, that.bestRoute) &&
                Objects.equals(this.allRoutes, that.allRoutes);
    }
}
