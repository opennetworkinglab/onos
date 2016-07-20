/*
 * Copyright 2014-present Open Networking Laboratory
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
import static org.onlab.packet.PacketUtils.*;

/**
 * Implements ICMP packet format.
 */
public class ICMP extends BasePacket {
    protected byte icmpType;
    protected byte icmpCode;
    protected short checksum;

    public static final byte TYPE_ECHO_REQUEST = 0x08;
    public static final byte TYPE_ECHO_REPLY = 0x00;
    public static final byte SUBTYPE_ECHO_REPLY = 0x00;

    public static final short ICMP_HEADER_LENGTH = 4;

    /**
     * @return the icmpType
     */
    public byte getIcmpType() {
        return this.icmpType;
    }

    /**
     * @param icmpType to set
     * @return this
     */
    public ICMP setIcmpType(final byte icmpType) {
        this.icmpType = icmpType;
        return this;
    }

    /**
     * @return the icmp code
     */
    public byte getIcmpCode() {
        return this.icmpCode;
    }

    /**
     * @param icmpCode code to set
     * @return this
     */
    public ICMP setIcmpCode(final byte icmpCode) {
        this.icmpCode = icmpCode;
        return this;
    }

    /**
     * @return the checksum
     */
    public short getChecksum() {
        return this.checksum;
    }

    /**
     * @param checksum the checksum to set
     * @return this
     */
    public ICMP setChecksum(final short checksum) {
        this.checksum = checksum;
        return this;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     */
    @Override
    public byte[] serialize() {
        int length = 4;
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
            length += payloadData.length;
        }

        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.icmpType);
        bb.put(this.icmpCode);
        bb.putShort(this.checksum);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IPv4) {
            ((IPv4) this.parent).setProtocol(IPv4.PROTOCOL_ICMP);
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;

            for (int i = 0; i < length / 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            // pad to an even number of shorts
            if (length % 2 > 0) {
                accumulation += (bb.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(2, this.checksum);
        }
        return data;
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.icmpType = bb.get();
        this.icmpCode = bb.get();
        this.checksum = bb.getShort();

        this.payload = new Data();
        this.payload = this.payload.deserialize(data, bb.position(), bb.limit()
                - bb.position());
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
        result = prime * result + this.icmpType;
        result = prime * result + this.icmpCode;
        result = prime * result + this.checksum;
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
        if (!(obj instanceof ICMP)) {
            return false;
        }
        final ICMP other = (ICMP) obj;
        if (this.icmpType != other.icmpType) {
            return false;
        }
        if (this.icmpCode != other.icmpCode) {
            return false;
        }
        if (this.checksum != other.checksum) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for ICMP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<ICMP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, ICMP_HEADER_LENGTH);

            ICMP icmp = new ICMP();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            icmp.icmpType = bb.get();
            icmp.icmpCode = bb.get();
            icmp.checksum = bb.getShort();

            icmp.payload = Data.deserializer()
                    .deserialize(data, bb.position(), bb.limit()
                            - bb.position());
            icmp.payload.setParent(icmp);
            return icmp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("icmpType", Byte.toString(icmpType))
                .add("icmpCode", Byte.toString(icmpCode))
                .add("checksum", Short.toString(checksum))
                .toString();
    }
}
