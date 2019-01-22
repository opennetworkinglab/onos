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
import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiPacketMetadataId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a metadata field for a controller packet-in/out for a
 * protocol-independent pipeline. Metadata are used to carry information other
 * than the packet-in/out payload, such as the original ingress port of a
 * packet-in, or the egress port of packet-out.
 */
@Beta
public final class PiPacketMetadata {

    private final PiPacketMetadataId id;
    private final ImmutableByteSequence value;

    /**
     * Creates a new packet metadata instance for the given identifier and
     * value.
     *
     * @param id    packet metadata identifier
     * @param value value for this metadata
     */
    private PiPacketMetadata(PiPacketMetadataId id, ImmutableByteSequence value) {
        this.id = id;
        this.value = value;
    }

    /**
     * Return the identifier of this packet metadata.
     *
     * @return packet metadata identifier
     */
    public PiPacketMetadataId id() {
        return id;
    }

    /**
     * Returns the value for the field in this metadata.
     *
     * @return value
     */
    public ImmutableByteSequence value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiPacketMetadata piPacket = (PiPacketMetadata) o;
        return Objects.equal(id, piPacket.id()) &&
                Objects.equal(value, piPacket.value());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, value);
    }

    @Override
    public String toString() {
        return this.id().toString() + " = " + value.toString();
    }

    /**
     * Returns a packet metadata builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of protocol-independent packet metadatas.
     */
    public static final class Builder {

        private PiPacketMetadataId id;
        private ImmutableByteSequence value;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the identifier of this packet metadata.
         *
         * @param id packet metadata identifier
         * @return this
         */
        public Builder withId(PiPacketMetadataId id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the value of this metadata.
         *
         * @param value value of the metadata
         * @return this
         */
        public Builder withValue(ImmutableByteSequence value) {
            this.value = value;
            return this;
        }

        /**
         * Returns a new packet metadata instance.
         *
         * @return packet metadata
         */
        public PiPacketMetadata build() {
            checkNotNull(id);
            checkNotNull(value);
            return new PiPacketMetadata(id, value);
        }
    }
}
