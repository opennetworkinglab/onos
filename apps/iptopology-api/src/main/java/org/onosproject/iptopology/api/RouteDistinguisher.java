/*
 * Copyright 2015-present Open Networking Laboratory
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

/**
 * Implementation of RouteDistinguisher.
 */
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents Route Distinguisher of device in the network.
 */
public class RouteDistinguisher {
    private final Long routeDistinguisher;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDistinguisher route distinguisher
     */
    public RouteDistinguisher(Long routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    /**
     * Obtain route distinguisher.
     *
     * @return route distinguisher
     */
    public Long routeDistinguisher() {
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
            return Objects.equals(routeDistinguisher, other.routeDistinguisher);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routeDistinguisher", routeDistinguisher)
                .toString();
    }
}