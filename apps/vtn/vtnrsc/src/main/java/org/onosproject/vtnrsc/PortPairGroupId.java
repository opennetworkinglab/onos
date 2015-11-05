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
 * Representation of a Port Pair Group ID.
 */
public final class PortPairGroupId {

    private final UUID portPairGroupId;

    /**
     * Private constructor for port pair group id.
     *
     * @param id UUID id of port pair group
     */
    private PortPairGroupId(UUID id) {
        checkNotNull(id, "Port pair group id can not be null");
        this.portPairGroupId = id;
    }

    /**
     * Returns newly created port pair group id object.
     *
     * @param id port pair group id in UUID
     * @return object of port pair group id
     */
    public static PortPairGroupId of(UUID id) {
        return new PortPairGroupId(id);
    }

    /**
     * Returns newly created port pair group id object.
     *
     * @param id port pair group id in string
     * @return object of port pair group id
     */
    public static PortPairGroupId of(String id) {
        return new PortPairGroupId(UUID.fromString(id));
    }

    /**
     * Returns the value of port pair group id.
     *
     * @return port pair group id
     */
    public UUID value() {
        return portPairGroupId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PortPairGroupId) {
            final PortPairGroupId other = (PortPairGroupId) obj;
            return Objects.equals(this.portPairGroupId, other.portPairGroupId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.portPairGroupId);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("portPairGroupId", portPairGroupId)
                .toString();
    }
}
