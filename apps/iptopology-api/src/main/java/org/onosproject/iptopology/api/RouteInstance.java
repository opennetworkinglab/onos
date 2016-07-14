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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents routing universe where the network element belongs.
 */
public class RouteInstance {
    private final long routeInstance;

    /**
     * Constructor to initialize routeInstance.
     *
     * @param routeInstance routing protocol instance
     */
    public RouteInstance(long routeInstance) {
        this.routeInstance = routeInstance;
    }

    /**
     * Obtain route instance.
     *
     * @return route instance
     */
    public long routeInstance() {
        return routeInstance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeInstance);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteInstance) {
            RouteInstance other = (RouteInstance) obj;
            return Objects.equals(routeInstance, other.routeInstance);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routeInstance", routeInstance)
                .toString();
    }
}