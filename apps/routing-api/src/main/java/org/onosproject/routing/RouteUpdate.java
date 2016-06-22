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
package org.onosproject.routing;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a change in routing information.
 *
 * @deprecated use RouteService instead
 */
@Deprecated
public class RouteUpdate {
    private final Type type;                    // The route update type
    private final RouteEntry routeEntry;        // The updated route entry

    /**
     * Specifies the type of a route update.
     * <p>
     * Route updates can either provide updated information for a route, or
     * withdraw a previously updated route.
     * </p>
     */
    public enum Type {
        /**
         * The update contains updated route information for a route.
         */
        UPDATE,
        /**
         * The update withdraws the route, meaning any previous information is
         * no longer valid.
         */
        DELETE
    }

    /**
     * Class constructor.
     *
     * @param type the type of the route update
     * @param routeEntry the route entry with the update
     */
    public RouteUpdate(Type type, RouteEntry routeEntry) {
        this.type = type;
        this.routeEntry = checkNotNull(routeEntry);
    }

    /**
     * Returns the type of the route update.
     *
     * @return the type of the update
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the route entry the route update is for.
     *
     * @return the route entry the route update is for
     */
    public RouteEntry routeEntry() {
        return routeEntry;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof RouteUpdate)) {
            return false;
        }

        RouteUpdate otherUpdate = (RouteUpdate) other;

        return Objects.equals(this.type, otherUpdate.type) &&
            Objects.equals(this.routeEntry, otherUpdate.routeEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, routeEntry);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("type", type)
            .add("routeEntry", routeEntry)
            .toString();
    }
}
