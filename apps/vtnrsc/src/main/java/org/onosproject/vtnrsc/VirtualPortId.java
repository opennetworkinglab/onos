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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Immutable representation of a virtual port identifier.
 */
public final class VirtualPortId {
    private final String portId;
    // Public construction is prohibited
    private VirtualPortId(String virtualPortId) {
        checkNotNull(virtualPortId, "VirtualPortId cannot be null");
        this.portId = virtualPortId;
    }

    public String portId() {
        return portId;
    }

    /**
     * Creates a virtualPort id using the supplied portId.
     *
     * @param portId virtualport identifier
     * @return VirtualPortId
     */
    public static VirtualPortId portId(String portId) {
        return new VirtualPortId(portId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VirtualPortId) {
            final VirtualPortId that = (VirtualPortId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.portId, that.portId);
        }
        return false;
    }

    @Override
    public String toString() {
        return portId;
    }

}
