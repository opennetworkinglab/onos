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
 * Represents the igp administrative tags of the prefix.
 */
public class RouteTag {
    private final int routeTag;

    /**
     * Constructor to initialize its parameter.
     *
     * @param routeTag IGP route tag
     */
    public RouteTag(int routeTag) {
        this.routeTag = routeTag;
    }

    /**
     * Obtains igp administrative tags of the prefix.
     *
     * @return igp administrative tags of the prefix
     */
    public int routeTag() {
        return routeTag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteTag) {
            RouteTag other = (RouteTag) obj;
            return Objects.equals(routeTag, other.routeTag);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routeTag", routeTag)
                .toString();
    }
}