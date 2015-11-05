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

import java.util.UUID;
import java.util.Objects;

/**
 * Representation of a Port Pair ID.
 */
public final class PortPairId {

    private final UUID portPairId;

    /**
     * Private constructor for port pair id.
     *
     * @param id UUID id of port pair
     */
    private PortPairId(UUID id) {
        checkNotNull(id, "Port chain id can not be null");
        this.portPairId = id;
    }

    /**
     * Returns newly created port pair id object.
     *
     * @param id UUID of port pair id
     * @return object of port pair id
     */
    public static PortPairId of(UUID id) {
        return new PortPairId(id);
    }

    /**
     * Returns newly created port pair id object.
     *
     * @param id port pair id in string
     * @return object of port pair id
     */
    public static PortPairId of(String id) {
        return new PortPairId(UUID.fromString(id));
    }

    /**
     * Returns teh value of port pair id.
     *
     * @return port pair id
     */
    public UUID value() {
        return portPairId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PortPairId) {
            final PortPairId other = (PortPairId) obj;
            return Objects.equals(this.portPairId, other.portPairId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.portPairId);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("portPairId", portPairId)
                .toString();
    }
}
