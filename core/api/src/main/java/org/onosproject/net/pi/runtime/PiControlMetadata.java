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
import org.onosproject.net.pi.model.PiControlMetadataId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a control metadata for a protocol-independent pipeline.
 */
@Beta
public final class PiControlMetadata {

    private final PiControlMetadataId id;
    private final ImmutableByteSequence value;

    /**
     * Creates a new control metadata instance for the given identifier and value.
     *
     * @param id    control metadata identifier
     * @param value value for this metadata
     */
    private PiControlMetadata(PiControlMetadataId id, ImmutableByteSequence value) {
        this.id = id;
        this.value = value;
    }

    /**
     * Return the identifier of this control metadata.
     *
     * @return control metadata identifier
     */
    public PiControlMetadataId id() {
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
        PiControlMetadata piPacket = (PiControlMetadata) o;
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
     * Returns a control metadata builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of protocol-independent control metadatas.
     */
    public static final class Builder {

        private PiControlMetadataId id;
        private ImmutableByteSequence value;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the identifier of this control metadata.
         *
         * @param id control metadata identifier
         * @return this
         */
        public Builder withId(PiControlMetadataId id) {
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
         * Returns a new control metadata instance.
         *
         * @return control metadata
         */
        public PiControlMetadata build() {
            checkNotNull(id);
            checkNotNull(value);
            return new PiControlMetadata(id, value);
        }
    }
}
