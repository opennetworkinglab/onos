/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.model;

import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketMetadataModel;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiPacketMetadataModel for P4Runtime.
 */
final class P4PacketMetadataModel implements PiPacketMetadataModel {

    static final int BIT_WIDTH_UNDEFINED = -1;

    private final PiPacketMetadataId id;
    private final int bitWidth;

    P4PacketMetadataModel(PiPacketMetadataId id, int bitWidth) {
        this.id = id;
        this.bitWidth = bitWidth;
    }

    @Override
    public PiPacketMetadataId id() {
        return id;
    }

    @Override
    public int bitWidth() {
        return bitWidth;
    }

    @Override
    public boolean hasBitWidth() {
        return bitWidth != BIT_WIDTH_UNDEFINED;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bitWidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4PacketMetadataModel other = (P4PacketMetadataModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.bitWidth, other.bitWidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("bitWidth", bitWidth)
                .toString();
    }
}
