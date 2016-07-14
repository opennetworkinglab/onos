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

package org.onlab.packet.ipv6;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements IPv6 Encapsulating Security Payload (ESP) extension header format.
 * (RFC 4303)
 */
public class EncapSecurityPayload extends BasePacket {
    public static final byte HEADER_LENGTH = 8; // bytes

    protected int securityParamIndex;
    protected int sequence;
    //
    // NOTE: The remaining fields including payload data, padding length and
    //       next header are encrypted and all considered as a payload of ESP.
    //

    /**
     * Gets the security parameter index of this header.
     *
     * @return the security parameter index
     */
    public int getSecurityParamIndex() {
        return this.securityParamIndex;
    }

    /**
     * Sets the security parameter index of this header.
     *
     * @param securityParamIndex the security parameter index to set
     * @return this
     */
    public EncapSecurityPayload setSecurityParamIndex(final int securityParamIndex) {
        this.securityParamIndex = securityParamIndex;
        return this;
    }

    /**
     * Gets the sequence number of this header.
     *
     * @return the sequence number
     */
    public int getSequence() {
        return this.sequence;
    }

    /**
     * Sets the sequence number of this header.
     *
     * @param sequence the sequence number to set
     * @return this
     */
    public EncapSecurityPayload setSequence(final int sequence) {
        this.sequence = sequence;
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int payloadLength = 0;
        if (payloadData != null) {
            payloadLength = payloadData.length;
        }

        final byte[] data = new byte[HEADER_LENGTH + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(this.securityParamIndex);
        bb.putInt(this.sequence);

        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IExtensionHeader) {
            ((IExtensionHeader) this.parent).setNextHeader(IPv6.PROTOCOL_ESP);
        }
        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.securityParamIndex = bb.getInt();
        this.sequence = bb.getInt();

        this.payload = new Data();
        this.payload.deserialize(data, bb.position(),
                                 bb.limit() - bb.position());
        this.payload.setParent(this);

        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5807;
        int result = super.hashCode();
        result = prime * result + this.securityParamIndex;
        result = prime * result + this.sequence;
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof EncapSecurityPayload)) {
            return false;
        }
        final EncapSecurityPayload other = (EncapSecurityPayload) obj;
        if (this.securityParamIndex != other.securityParamIndex) {
            return false;
        }
        if (this.sequence != other.sequence) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for encapsulated security payload headers.
     *
     * @return deserializer function
     */
    public static Deserializer<EncapSecurityPayload> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            EncapSecurityPayload encapSecurityPayload = new EncapSecurityPayload();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            encapSecurityPayload.securityParamIndex = bb.getInt();
            encapSecurityPayload.sequence = bb.getInt();

            encapSecurityPayload.payload = Data.deserializer().deserialize(
                    data, bb.position(), bb.limit() - bb.position());
            encapSecurityPayload.payload.setParent(encapSecurityPayload);

            return encapSecurityPayload;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("securityParamIndex", Integer.toString(securityParamIndex))
                .add("sequence", Integer.toString(sequence))
                .toString();
    }
}
