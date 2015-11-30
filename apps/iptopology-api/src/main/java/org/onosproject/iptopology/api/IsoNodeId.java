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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents ISO system id of the device.
 */
public class IsoNodeId implements RouteIdentifier {
    private final byte[] isoNodeId;
    private final ProtocolType type;

    /**
     * Constructor to initialize the values.
     *
     * @param isoNodeId ISO system-ID
     * @param type Protocol type
     */
    public IsoNodeId(byte[] isoNodeId, ProtocolType type) {
        this.isoNodeId = isoNodeId;
        this.type = type;
    }

    /**
     * Obtains ISO system id of the device.
     *
     * @return ISO system id
     */
    public byte[] isoNodeId() {
        return isoNodeId;
    }

    @Override
    public ProtocolType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoNodeId, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IsoNodeId) {
            IsoNodeId other = (IsoNodeId) obj;
            return Objects.equals(isoNodeId, other.isoNodeId) && Objects.equals(type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("isoNodeId", isoNodeId)
                .add("type", type)
                .toString();
    }
}