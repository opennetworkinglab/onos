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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiControlMetadataId;
import org.onosproject.net.pi.model.PiPacketOperationType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a packet I/O operation, and its control metadatas, for a protocol-independent pipeline.
 */
@Beta
public final class PiPacketOperation {

    private final DeviceId deviceId;
    private final ImmutableByteSequence data;
    private final Set<PiControlMetadata> packetMetadatas;
    private final PiPacketOperationType type;

    /**
     * Creates a new packet I/O operation for the given device ID, data, control metadatas and operation type.
     *
     * @param deviceId        device ID
     * @param data            the packet raw data
     * @param packetMetadatas collection of control metadata
     * @param type            type of this packet operation
     */
    private PiPacketOperation(DeviceId deviceId, ImmutableByteSequence data,
                              Collection<PiControlMetadata> packetMetadatas,
                              PiPacketOperationType type) {
        this.deviceId = deviceId;
        this.data = data;
        this.packetMetadatas = ImmutableSet.copyOf(packetMetadatas);
        this.type = type;
    }

    /**
     * Returns the device ID of this packet operation.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Return the type of this packet.
     *
     * @return packet type
     */
    public PiPacketOperationType type() {
        return type;
    }

    /**
     * Returns the data of this packet.
     *
     * @return packet data
     */
    public ImmutableByteSequence data() {
        return data;
    }

    /**
     * Returns all metadatas of this packet. Returns an empty collection if the packet doesn't have any metadata.
     *
     * @return collection of metadatas
     */
    public Collection<PiControlMetadata> metadatas() {
        return packetMetadatas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiPacketOperation that = (PiPacketOperation) o;
        return Objects.equal(packetMetadatas, that.packetMetadatas) &&
                Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(data, that.data()) &&
                Objects.equal(type, that.type());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, data, packetMetadatas, type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .addValue(type.toString())
                .addValue(packetMetadatas)
                .toString();
    }

    /**
     * Returns a new builder of packet operations.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of packet operations.
     */
    public static final class Builder {

        private DeviceId deviceId;
        private Map<PiControlMetadataId, PiControlMetadata> packetMetadatas = new HashMap<>();
        private PiPacketOperationType type;
        private ImmutableByteSequence data;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the device ID.
         *
         * @param deviceId device ID
         * @return this
         */
        public Builder forDevice(DeviceId deviceId) {
            checkNotNull(deviceId);
            this.deviceId = deviceId;
            return this;
        }

        /**
         * Sets the raw packet data.
         *
         * @param data the packet raw data
         * @return this
         */
        public Builder withData(ImmutableByteSequence data) {
            checkNotNull(data);
            this.data = data;
            return this;
        }

        /**
         * Adds a control metadata. Only one metadata is allowed for a given metadata id. If a metadata with same id
         * already exists it will be replaced by the given one.
         *
         * @param metadata packet metadata
         * @return this
         */
        public Builder withMetadata(PiControlMetadata metadata) {
            checkNotNull(metadata);
            packetMetadatas.put(metadata.id(), metadata);

            return this;
        }

        /**
         * Adds many packet metadatas.
         *
         * @param metadatas collection of metadata
         * @return this
         */
        public Builder withMetadatas(Collection<PiControlMetadata> metadatas) {
            checkNotNull(metadatas);
            metadatas.forEach(this::withMetadata);
            return this;
        }

        /**
         * Sets the type of this packet.
         *
         * @param type type of the packet
         * @return this
         */
        public Builder withType(PiPacketOperationType type) {
            this.type = type;
            return this;
        }

        /**
         * Builds a new instance of a packet operation.
         *
         * @return packet operation
         */
        public PiPacketOperation build() {
            checkNotNull(deviceId);
            checkNotNull(data);
            checkNotNull(packetMetadatas);
            checkNotNull(type);
            return new PiPacketOperation(deviceId, data, packetMetadatas.values(), type);
        }
    }
}
