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

import com.google.common.collect.ImmutableList;
import org.onosproject.net.pi.model.PiPacketMetadataModel;
import org.onosproject.net.pi.model.PiPacketOperationModel;
import org.onosproject.net.pi.model.PiPacketOperationType;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiPacketOperationModel for P4Runtime.
 */
final class P4PacketOperationModel implements PiPacketOperationModel {

    private final PiPacketOperationType type;
    private final ImmutableList<PiPacketMetadataModel> metadatas;

    P4PacketOperationModel(PiPacketOperationType type,
                           ImmutableList<PiPacketMetadataModel> metadatas) {
        this.type = type;
        this.metadatas = metadatas;
    }

    @Override
    public PiPacketOperationType type() {
        return type;
    }

    @Override
    public List<PiPacketMetadataModel> metadatas() {
        return metadatas;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, metadatas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4PacketOperationModel other = (P4PacketOperationModel) obj;
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.metadatas, other.metadatas);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("metadatas", metadatas)
                .toString();
    }
}
