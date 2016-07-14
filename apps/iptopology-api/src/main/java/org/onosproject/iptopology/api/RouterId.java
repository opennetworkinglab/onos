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
 * Represents Router ID of the device.
 */
public class RouterId implements RouteIdentifier {
    private final int routerId;
    private final ProtocolType type;

    /**
     * Constructor to initialize its parameters.
     *
     * @param routerId  Router ID of designated router
     * @param type      protocol type
     */
    public RouterId(int routerId, ProtocolType type) {
        this.routerId = routerId;
        this.type = type;
    }

    /**
     * Obtains Router Id of the device.
     *
     * @return Router Id of the device
     */
    public int routerId() {
        return routerId;
    }

    @Override
    public ProtocolType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routerId, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouterId) {
            RouterId other = (RouterId) obj;
            return Objects.equals(routerId, other.routerId) && Objects.equals(type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routerId", routerId)
                .add("type", type)
                .toString();
    }
}