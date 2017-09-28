/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMJoinPrune;

import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements PIM control packet format.
 */
public class PIM extends BasePacket {

    public static final IpAddress PIM_ADDRESS = IpAddress.valueOf("224.0.0.13");

    public static final byte TYPE_HELLO = 0x00;
    public static final byte TYPE_REGISTER = 0x01;
    public static final byte TYPE_REGISTER_STOP = 0x02;
    public static final byte TYPE_JOIN_PRUNE_REQUEST = 0x03;
    public static final byte TYPE_BOOTSTRAP = 0x04;
    public static final byte TYPE_ASSERT = 0x05;
    public static final byte TYPE_GRAFT = 0x06;
    public static final byte TYPE_GRAFT_ACK = 0x07;
    public static final byte TYPE_CANDIDATE_RP_ADV = 0x08;

    public static final int PIM_HEADER_LEN = 4;

    public static final byte ADDRESS_FAMILY_IP4 = 0x1;
    public static final byte ADDRESS_FAMILY_IP6 = 0x2;

    public static final Map<Byte, Deserializer<? extends IPacket>> PROTOCOL_DESERIALIZER_MAP =
            ImmutableMap.<Byte, Deserializer<? extends IPacket>>builder()
                .put(PIM.TYPE_HELLO, PIMHello.deserializer())
                .put(PIM.TYPE_JOIN_PRUNE_REQUEST, PIMJoinPrune.deserializer())
                .build();

    /*
     * PIM Header fields
     */
    protected byte version;
    protected byte type;
    protected byte reserved;
    protected short checksum;

    /**
     * Default constructor.
     */
    public PIM() {
        super();
        this.version = 2;
        this.reserved = 0;
    }

    /**
     * Return the PIM message type.
     *
     * @return the pimMsgType
     */
    public byte getPimMsgType() {
        return this.type;
    }

    /**
     * Set the PIM message type. Currently PIMJoinPrune and PIMHello are
     * supported.
     *
     * @param type PIM message type
     * @return PIM Header
     */
     public PIM setPIMType(final byte type) {
            this.type = type;
            return this;
     }

    /**
     * Get the version of PIM.
     *
     * @return the PIM version.   Must be 2.
     */
    public byte getVersion() {
         return version;
    }

    /**
     * Set the PIM version type. Should not change from 2.
     *
     * @param version PIM version
     */
    public void setVersion(byte version) {
         this.version = version;
    }

    /**
     * Get the reserved field.
     *
     * @return the reserved field.  Must be ignored.
     */
    public byte getReserved() {
        return reserved;
    }

    /**
     * Set the reserved field.
     *
     * @param reserved should be 0
     */
    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }

    /**
     * Get the checksum of this packet.
     *
     * @return the checksum
     */
    public short getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum.
     *
     * @param checksum the checksum
     */
    public void setChecksum(short checksum) {
        this.checksum = checksum;
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
        result = prime * result + this.type;
        result = prime * result + this.version;
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
        if (!(obj instanceof PIM)) {
            return false;
        }
        final PIM other = (PIM) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.checksum != other.checksum) {
            return false;
        }
        return true;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     *
     * @return will return the serialized packet
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

        bb.put((byte) ((this.version & 0xf) << 4 | this.type & 0xf));
        bb.put(this.reserved);
        bb.putShort(this.checksum);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof PIM) {
            ((PIM) this.parent).setPIMType(TYPE_JOIN_PRUNE_REQUEST);
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


    /**
     * Deserializer function for IPv4 packets.
     *
     * @return deserializer function
     */
    public static Deserializer<PIM> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, PIM_HEADER_LEN);

            PIM pim = new PIM();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            byte versionByte = bb.get();
            pim.version = (byte) (versionByte >> 4 & 0xf);
            pim.setPIMType((byte) (versionByte & 0xf));
            pim.reserved = bb.get();
            pim.checksum = bb.getShort();

            Deserializer<? extends IPacket> deserializer;
            if (PIM.PROTOCOL_DESERIALIZER_MAP.containsKey(pim.getPimMsgType())) {
                deserializer = PIM.PROTOCOL_DESERIALIZER_MAP.get(pim.getPimMsgType());
            } else {
                deserializer = Data.deserializer();
            }

            pim.payload = deserializer.deserialize(data, bb.position(), bb.limit() - bb.position());
            pim.payload.setParent(pim);

            return pim;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("version", Byte.toString(version))
                .add("type", Byte.toString(type))
                .add("reserved", Byte.toString(reserved))
                .add("checksum", Short.toString(reserved))
                .toString();
    }
}
