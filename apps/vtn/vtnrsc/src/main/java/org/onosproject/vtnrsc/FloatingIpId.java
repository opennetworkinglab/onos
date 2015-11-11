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
import java.util.UUID;

/**
 * Immutable representation of a floating IP identifier.
 */
public final class FloatingIpId {
    private final UUID floatingIpId;

    // Public construction is prohibited
    private FloatingIpId(UUID floatingIpId) {
        this.floatingIpId = checkNotNull(floatingIpId, "floatingIpId cannot be null");
    }

    /**
     * Creates a floating IP identifier.
     *
     * @param floatingIpId the UUID id of floating IP identifier
     * @return object of floating IP identifier
     */
    public static FloatingIpId of(UUID floatingIpId) {
        return new FloatingIpId(floatingIpId);
    }

    /**
     * Creates a floating IP identifier.
     *
     * @param floatingIpId the floating IP identifier in string
     * @return object of floating IP identifier
     */
    public static FloatingIpId of(String floatingIpId) {
        return new FloatingIpId(UUID.fromString(floatingIpId));
    }

    /**
     * Returns the floating IP identifier.
     *
     * @return the floating IP identifier
     */
    public UUID floatingIpId() {
        return floatingIpId;
    }

    @Override
    public int hashCode() {
        return floatingIpId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FloatingIpId) {
            final FloatingIpId that = (FloatingIpId) obj;
            return Objects.equals(this.floatingIpId, that.floatingIpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("floatingIpId", floatingIpId).toString();
    }
}
