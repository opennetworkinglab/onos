/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.flowapi;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Packet length extension implementation.
 */
public class DefaultExtPacketLength implements ExtPacketLength {

    private List<ExtOperatorValue> packetLength;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtPacketLength which contains packet length list..
     *
     * @param packetLength is a packet length rule
     * @param type ExtType type
     */
    DefaultExtPacketLength(List<ExtOperatorValue> packetLength, ExtType type) {
        this.packetLength = packetLength;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> packetLength() {
        return packetLength;
    }

    @Override
    public boolean exactMatch(ExtPacketLength length) {
        return this.equals(packetLength) &&
                Objects.equals(this.packetLength, length.packetLength())
                && Objects.equals(this.type, length.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetLength, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtPacketLength) {
            DefaultExtPacketLength that = (DefaultExtPacketLength) obj;
            return Objects.equals(packetLength, that.packetLength())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("packetLength", packetLength.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension packet length.
     */
    public static class Builder implements ExtPacketLength.Builder {
        private List<ExtOperatorValue> packetLength;
        private ExtType type;

        @Override
        public Builder setPacketLength(List<ExtOperatorValue> packetLength) {
            this.packetLength = packetLength;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtPacketLength build() {
            checkNotNull(packetLength, "packetLength cannot be null");
            return new DefaultExtPacketLength(packetLength, type);
        }
    }
}
