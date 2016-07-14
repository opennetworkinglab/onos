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

import java.util.Arrays;
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
        return Objects.hash(Arrays.hashCode(isoNodeId), type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IsoNodeId) {
            IsoNodeId other = (IsoNodeId) obj;
            return Arrays.equals(isoNodeId, other.isoNodeId) && Objects.equals(type, other.type);
        }
        return false;
    }

    /*
     * Get iso node ID in specified string format.
     */
    private String isoNodeIdString() {
        if (isoNodeId != null) {
            int p1 = (int) isoNodeId[0] << 8 | (int) isoNodeId[1];
            int p2 = (int) isoNodeId[2] << 8 | (int) isoNodeId[3];
            int p3 = (int) isoNodeId[4] << 8 | (int) isoNodeId[5];

            return String.format("%1$d.%2$d.%3$d", p1, p2, p3);
        }
        return null;
    }

    @Override
    public String toString() {
        return toStringHelper(this).omitNullValues()
                .add("isoNodeId", isoNodeIdString())
                .add("type", type)
                .toString();
    }
}