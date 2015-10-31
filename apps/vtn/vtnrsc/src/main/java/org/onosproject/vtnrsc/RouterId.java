/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Immutable representation of a router identifier.
 */
public final class RouterId {

    private final String routerId;

    // Public construction is prohibited
    private RouterId(String routerId) {
        checkNotNull(routerId, "routerId cannot be null");
        this.routerId = routerId;
    }

    /**
     * Creates a router identifier.
     *
     * @param routerId the router identifier
     * @return the router identifier
     */
    public static RouterId valueOf(String routerId) {
        return new RouterId(routerId);
    }

    /**
     * Returns the router identifier.
     *
     * @return the router identifier
     */
    public String routerId() {
        return routerId;
    }

    @Override
    public int hashCode() {
        return routerId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouterId) {
            final RouterId that = (RouterId) obj;
            return Objects.equals(this.routerId, that.routerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("routerId", routerId).toString();
    }
}

