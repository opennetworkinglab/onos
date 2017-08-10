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
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents Route Distinguisher of device in the network.
 */
public final class RouteDistinguisher {
    private final String routeDistinguisher;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDistinguisher route distinguisher
     */
    private RouteDistinguisher(String routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    /**
     * Creates the route distinguisher.
     *
     * @param routeDistinguisher route distinguisher
     * @return RouteDistinguisher
     */
    public static RouteDistinguisher routeDistinguisher(String routeDistinguisher) {
        checkNotNull(routeDistinguisher);
        return new RouteDistinguisher(routeDistinguisher);
    }

    /**
     * get route distinguisher.
     *
     * @return distinguisher
     */
    public String getRouteDistinguisher() {
        return routeDistinguisher;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeDistinguisher);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteDistinguisher) {
            RouteDistinguisher other = (RouteDistinguisher) obj;
            return Objects.equals(this.routeDistinguisher, other.routeDistinguisher);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routeDistinguisher", this.routeDistinguisher).toString();
    }
}
