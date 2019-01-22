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
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketOperationType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a packet I/O operation that includes the packet body (frame) and
 * its metadata, for a protocol-independent pipeline.
 */
@Beta
public final class PiPacketOperation {

    private final ImmutableByteSequence frame;
    private final Set<PiPacketMetadata> packetMetadatas;
    private final PiPacketOperationType type;

    /**
     * Creates a new packet I/O operation for the given frame, packet metadata
     * and operation type.
     *
     * @param frame     the packet raw data
     * @param metadatas collection of packet metadata
     * @param type      type of this packet operation
     */
    private PiPacketOperation(ImmutableByteSequence frame,
                              Collection<PiPacketMetadata> metadatas,
                              PiPacketOperationType type) {
        this.frame = frame;
        this.packetMetadatas = ImmutableSet.copyOf(metadatas);
        this.type = type;
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
        return frame;
    }

    /**
     * Returns all metadatas of this packet. Returns an empty collection if the
     * packet doesn't have any metadata.
     *
     * @return collection of metadatas
     */
    public Collection<PiPacketMetadata> metadatas() {
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
                Objects.equal(frame, that.data()) &&
                Objects.equal(type, that.type());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(frame, packetMetadatas, type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("metadata", packetMetadatas)
                .add("frame", frame)
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

        private Map<PiPacketMetadataId, PiPacketMetadata> packetMetadatas = new HashMap<>();
        private PiPacketOperationType type;
        private ImmutableByteSequence data;

        private Builder() {
            // hides constructor.
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
         * Adds a packet metadata. Only one metadata is allowed for a given
         * metadata id. If a metadata with same id already exists it will be
         * replaced by the given one.
         *
         * @param metadata packet metadata
         * @return this
         */
        public Builder withMetadata(PiPacketMetadata metadata) {
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
        public Builder withMetadatas(Collection<PiPacketMetadata> metadatas) {
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
            checkNotNull(data);
            checkNotNull(packetMetadatas);
            checkNotNull(type);
            return new PiPacketOperation(data, packetMetadatas.values(), type);
        }
    }
}
