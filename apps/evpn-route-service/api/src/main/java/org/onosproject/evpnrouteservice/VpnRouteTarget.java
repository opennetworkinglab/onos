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

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents Route target of vpn instance.
 */
public final class VpnRouteTarget {
    private final String routeTarget;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeTarget route target
     */
    private VpnRouteTarget(String routeTarget) {
        this.routeTarget = routeTarget;
    }

    /**
     * Creates the vpn route target.
     *
     * @param routeTarget route target
     * @return route target
     */
    public static VpnRouteTarget routeTarget(String routeTarget) {
        return new VpnRouteTarget(routeTarget);
    }

    /**
     * get the route target.
     *
     * @return route target
     */
    public String getRouteTarget() {
        return routeTarget;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeTarget);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof VpnRouteTarget) {
            VpnRouteTarget other = (VpnRouteTarget) obj;
            return Objects.equals(routeTarget, other.routeTarget);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("routeTarget", routeTarget).toString();
    }
}
