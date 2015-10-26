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

import com.google.common.base.Objects;

/**
 * Representation of a Port Chain ID.
 */
public final class PortChainId {

    private final UUID portChainId;

    /**
     * Private constructor for port chain id.
     *
     * @param id UUID id of port chain
     */
    private PortChainId(UUID id) {
        checkNotNull(id, "Port chain id can not be null");
        this.portChainId = id;
    }

    /**
     * Constructor to create port chain id from UUID.
     *
     * @param id UUID of port chain
     * @return object of port chain id
     */
    public static PortChainId portChainId(UUID id) {
        return new PortChainId(id);
    }

    /**
     * Constructor to create port chain id from string.
     *
     * @param id port chain id in string
     * @return object of port chain id
     */
    public static PortChainId portChainId(String id) {
        return new PortChainId(UUID.fromString(id));
    }

    /**
     * Returns the value of port chain id.
     *
     * @return port chain id
     */
    public UUID value() {
        return portChainId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj.getClass()  == this.getClass()) {
            PortChainId that = (PortChainId) obj;
            return Objects.equal(this.portChainId, that.portChainId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.portChainId);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("portChainId", portChainId.toString())
                .toString();
    }
}
