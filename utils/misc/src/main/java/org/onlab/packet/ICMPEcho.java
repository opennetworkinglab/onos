/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onlab.packet;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * ICMP packet class for echo purpose.
 */
public class ICMPEcho extends BasePacket {
    private short identifier;
    private short sequenceNum;

    public static final short ICMP_ECHO_HEADER_LENGTH = 4;

    /**
     * Sets the identifier.
     *
     * @param identifier identifier
     * @return this
     */
    public ICMPEcho setIdentifier(final short identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Gets the identifier.
     *
     * @return identifier
     */
    public short getIdentifier() {
        return this.identifier;
    }

    /**
     * Sets the sequencer number.
     *
     * @param sequenceNum sequence number
     * @return this
     */
    public ICMPEcho setSequenceNum(final short sequenceNum) {
        this.sequenceNum = sequenceNum;
        return this;
    }

    /**
     * Gets the sequence number.
     *
     * @return sequence number
     */
    public short getSequenceNum() {
        return this.sequenceNum;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     */
    @Override
    public byte[] serialize() {

        int length = ICMP_ECHO_HEADER_LENGTH;
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
            length += payloadData.length;
        }

        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putShort(this.identifier);
        bb.putShort(this.sequenceNum);
        if (payloadData != null) {
            bb.put(payloadData);
        }
        return data;
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
        result = prime * result + this.identifier;
        result = prime * result + this.sequenceNum;
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
        if (!(obj instanceof ICMPEcho)) {
            return false;
        }
        final ICMPEcho other = (ICMPEcho) obj;

        if (this.identifier != other.identifier) {
            return false;
        }
        if (this.sequenceNum != other.sequenceNum) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for ICMPEcho packets.
     *
     * @return deserializer function
     */
    public static Deserializer<ICMPEcho> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, ICMP_ECHO_HEADER_LENGTH);

            ICMPEcho icmp = new ICMPEcho();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            icmp.identifier = bb.getShort();
            icmp.sequenceNum = bb.getShort();

            return icmp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("ICMP echo identifier", Short.toString(identifier))
                .add("ICMP echo sequenceNumber", Short.toString(sequenceNum))
                .toString();
    }
}
