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
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements IPv6 authentication extension header format. (RFC 4302)
 */
public class Authentication extends BasePacket implements IExtensionHeader {
    public static final byte FIXED_HEADER_LENGTH = 12; // bytes
    public static final byte LENGTH_UNIT = 4; // bytes per unit
    public static final byte MINUS = 2;

    protected byte nextHeader;
    protected byte payloadLength;
    protected int securityParamIndex;
    protected int sequence;
    protected byte[] integrityCheck;

    @Override
    public byte getNextHeader() {
        return this.nextHeader;
    }

    @Override
    public Authentication setNextHeader(final byte nextHeader) {
        this.nextHeader = nextHeader;
        return this;
    }

    /**
     * Gets the payload length of this header.
     *
     * @return the payload length
     */
    public byte getPayloadLength() {
        return this.payloadLength;
    }

    /**
     * Sets the payload length of this header.
     *
     * @param payloadLength the payload length to set
     * @return this
     */
    public Authentication setPayloadLength(final byte payloadLength) {
        this.payloadLength = payloadLength;
        return this;
    }

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
    public Authentication setSecurityParamIndex(final int securityParamIndex) {
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
    public Authentication setSequence(final int sequence) {
        this.sequence = sequence;
        return this;
    }

    /**
     * Gets the integrity check value of this header.
     *
     * @return the integrity check value
     */
    public byte[] getIntegrityCheck() {
        return this.integrityCheck;
    }

    /**
     * Sets the integrity check value of this header.
     *
     * @param integrityCheck the integrity check value to set
     * @return this
     */
    public Authentication setIngegrityCheck(final byte[] integrityCheck) {
        this.integrityCheck =
                Arrays.copyOfRange(integrityCheck, 0, integrityCheck.length);
        return this;
    }

    /**
     * Gets the total length of this header.
     * According to spec, payload length should be the total length of this AH
     * in 4-octet unit, minus 2
     *
     * @return the total length
     */
    public int getTotalLength() {
        return (this.payloadLength + MINUS) * LENGTH_UNIT;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int headerLength = FIXED_HEADER_LENGTH + integrityCheck.length;
        int payloadLength = 0;
        if (payloadData != null) {
            payloadLength = payloadData.length;
        }

        final byte[] data = new byte[headerLength + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.nextHeader);
        bb.put(this.payloadLength);
        bb.putShort((short) 0);
        bb.putInt(this.securityParamIndex);
        bb.putInt(this.sequence);
        bb.put(this.integrityCheck, 0, integrityCheck.length);

        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IExtensionHeader) {
            ((IExtensionHeader) this.parent).setNextHeader(IPv6.PROTOCOL_AH);
        }
        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.nextHeader = bb.get();
        this.payloadLength = bb.get();
        bb.getShort();
        this.securityParamIndex = bb.getInt();
        this.sequence = bb.getInt();
        int icvLength = getTotalLength() - FIXED_HEADER_LENGTH;
        this.integrityCheck = new byte[icvLength];
        bb.get(this.integrityCheck, 0, icvLength);

        Deserializer<? extends IPacket> deserializer;
        if (IPv6.PROTOCOL_DESERIALIZER_MAP.containsKey(this.nextHeader)) {
            deserializer = IPv6.PROTOCOL_DESERIALIZER_MAP.get(this.nextHeader);
        } else {
            deserializer = Data.deserializer();
        }

        try {
            this.payload = deserializer.deserialize(data, bb.position(),
                                                              bb.limit() - bb.position());
            this.payload.setParent(this);
        } catch (DeserializationException e) {
            return this;
        }

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
        result = prime * result + this.nextHeader;
        result = prime * result + this.payloadLength;
        result = prime * result + this.securityParamIndex;
        result = prime * result + this.sequence;
        for (byte b : this.integrityCheck) {
            result = prime * result + b;
        }
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
        if (!(obj instanceof Authentication)) {
            return false;
        }
        final Authentication other = (Authentication) obj;
        if (this.nextHeader != other.nextHeader) {
            return false;
        }
        if (this.payloadLength != other.payloadLength) {
            return false;
        }
        if (this.securityParamIndex != other.securityParamIndex) {
            return false;
        }
        if (this.sequence != other.sequence) {
            return false;
        }
        if (!Arrays.equals(this.integrityCheck, other.integrityCheck)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for authentication headers.
     *
     * @return deserializer function
     */
    public static Deserializer<Authentication> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, FIXED_HEADER_LENGTH);

            Authentication authentication = new Authentication();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            authentication.nextHeader = bb.get();
            authentication.payloadLength = bb.get();
            bb.getShort();
            authentication.securityParamIndex = bb.getInt();
            authentication.sequence = bb.getInt();
            int icvLength = (authentication.payloadLength + MINUS) * LENGTH_UNIT - FIXED_HEADER_LENGTH;
            authentication.integrityCheck = new byte[icvLength];
            bb.get(authentication.integrityCheck, 0, icvLength);

            Deserializer<? extends IPacket> deserializer;
            if (IPv6.PROTOCOL_DESERIALIZER_MAP.containsKey(authentication.nextHeader)) {
                deserializer = IPv6.PROTOCOL_DESERIALIZER_MAP.get(authentication.nextHeader);
            } else {
                deserializer = Data.deserializer();
            }
            authentication.payload = deserializer.deserialize(data, bb.position(),
                                               bb.limit() - bb.position());
            authentication.payload.setParent(authentication);

            return authentication;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("nextHeader", Byte.toString(nextHeader))
                .add("payloadLength", Byte.toString(payloadLength))
                .add("securityParamIndex", Integer.toString(securityParamIndex))
                .add("sequence", Integer.toString(sequence))
                .add("integrityCheck", Arrays.toString(integrityCheck))
                .toString();
    }
}
