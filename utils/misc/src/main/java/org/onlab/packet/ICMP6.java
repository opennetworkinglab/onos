/*
 * Copyright 2014 Open Networking Laboratory
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

import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.Redirect;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements ICMPv6 packet format. (RFC 4443)
 */
public class ICMP6 extends BasePacket {
    public static final byte HEADER_LENGTH = 4; // bytes

    public static final byte ROUTER_SOLICITATION = (byte) 0x85;
    public static final byte ROUTER_ADVERTISEMENT = (byte) 0x86;
    public static final byte NEIGHBOR_SOLICITATION = (byte) 0x87;
    public static final byte NEIGHBOR_ADVERTISEMENT = (byte) 0x88;
    public static final byte REDIRECT = (byte) 0x89;
    public static final Map<Byte, Class<? extends IPacket>> PROTOCOL_CLASS_MAP =
            new HashMap<>();

    static {
        ICMP6.PROTOCOL_CLASS_MAP.put(ICMP6.ROUTER_SOLICITATION, RouterSolicitation.class);
        ICMP6.PROTOCOL_CLASS_MAP.put(ICMP6.ROUTER_ADVERTISEMENT, RouterAdvertisement.class);
        ICMP6.PROTOCOL_CLASS_MAP.put(ICMP6.NEIGHBOR_SOLICITATION, NeighborSolicitation.class);
        ICMP6.PROTOCOL_CLASS_MAP.put(ICMP6.NEIGHBOR_ADVERTISEMENT, NeighborAdvertisement.class);
        ICMP6.PROTOCOL_CLASS_MAP.put(ICMP6.REDIRECT, Redirect.class);
    }

    protected byte icmpType;
    protected byte icmpCode;
    protected short checksum;

    /**
     * Gets ICMP6 type.
     *
     * @return the ICMP6 type
     */
    public byte getIcmpType() {
        return this.icmpType;
    }

    /**
     * Sets ICMP6 type.
     *
     * @param icmpType the ICMP type to set
     * @return this
     */
    public ICMP6 setIcmpType(final byte icmpType) {
        this.icmpType = icmpType;
        return this;
    }

    /**
     * Gets ICMP6 code.
     *
     * @return the ICMP6 code
     */
    public byte getIcmpCode() {
        return this.icmpCode;
    }

    /**
     * Sets ICMP6 code.
     *
     * @param icmpCode the ICMP6 code to set
     * @return this
     */
    public ICMP6 setIcmpCode(final byte icmpCode) {
        this.icmpCode = icmpCode;
        return this;
    }

    /**
     * Gets checksum.
     *
     * @return the checksum
     */
    public short getChecksum() {
        return this.checksum;
    }

    /**
     * Sets checksum.
     *
     * @param checksum the checksum to set
     * @return this
     */
    public ICMP6 setChecksum(final short checksum) {
        this.checksum = checksum;
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

        bb.put(this.icmpType);
        bb.put(this.icmpCode);
        bb.putShort(this.checksum);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IPv6) {
            ((IPv6) this.parent).setNextHeader(IPv6.PROTOCOL_ICMP6);
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;

            for (int i = 0; i < data.length / 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            // pad to an even number of shorts
            if (data.length % 2 > 0) {
                accumulation += (bb.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(2, this.checksum);
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
        if (!(obj instanceof ICMP6)) {
            return false;
        }
        final ICMP6 other = (ICMP6) obj;
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

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.icmpType = bb.get();
        this.icmpCode = bb.get();
        this.checksum = bb.getShort();

        IPacket payload;
        if (ICMP6.PROTOCOL_CLASS_MAP.containsKey(this.icmpType)) {
            final Class<? extends IPacket> clazz = ICMP6.PROTOCOL_CLASS_MAP
                    .get(this.icmpType);
            try {
                payload = clazz.newInstance();
            } catch (final Exception e) {
                throw new RuntimeException(
                        "Error parsing payload for ICMP6 packet", e);
            }
        } else {
            payload = new Data();
        }
        this.payload = payload.deserialize(data, bb.position(),
                bb.limit() - bb.position());
        this.payload.setParent(this);

        return this;
    }
}
