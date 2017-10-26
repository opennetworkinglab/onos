/*
 * Copyright 2014-present Open Networking Foundation
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
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.*;

/**
 * Representation of a UDP packet.
 */
public class UDP extends BasePacket {
    public static final Map<Integer, Deserializer<? extends IPacket>> PORT_DESERIALIZER_MAP =
            ImmutableMap.<Integer, Deserializer<? extends IPacket>>builder()
                    .put(UDP.DHCP_SERVER_PORT, DHCP.deserializer())
                    .put(UDP.DHCP_CLIENT_PORT, DHCP.deserializer())
                    .put(UDP.DHCP_V6_SERVER_PORT, DHCP6.deserializer())
                    .put(UDP.DHCP_V6_CLIENT_PORT, DHCP6.deserializer())
                    .put(UDP.VXLAN_UDP_PORT, VXLAN.deserializer())
                    .put(UDP.RIP_PORT, RIP.deserializer())
                    .put(UDP.RIPNG_PORT, RIPng.deserializer())
                    .build();

    public static final int DHCP_SERVER_PORT = 67;
    public static final int DHCP_CLIENT_PORT = 68;
    public static final int DHCP_V6_SERVER_PORT = 547;
    public static final int DHCP_V6_CLIENT_PORT = 546;
    public static final int VXLAN_UDP_PORT = 4789;
    public static final int RIP_PORT = 520;
    public static final int RIPNG_PORT = 521;

    private static final short UDP_HEADER_LENGTH = 8;

    protected int sourcePort;
    protected int destinationPort;
    protected short length;
    protected short checksum;

    /**
     * @return the sourcePort
     */
    public int getSourcePort() {
        return this.sourcePort;
    }

    /**
     * @param sourcePort
     *            the sourcePort to set (16 bits unsigned integer)
     * @return this
     */
    public UDP setSourcePort(final int sourcePort) {
        this.sourcePort = sourcePort;
        return this;
    }

    /**
     * @return the destinationPort
     */
    public int getDestinationPort() {
        return this.destinationPort;
    }

    /**
     * @param destinationPort
     *            the destinationPort to set (16 bits unsigned integer)
     * @return this
     */
    public UDP setDestinationPort(final int destinationPort) {
        this.destinationPort = destinationPort;
        return this;
    }

    /**
     * @return the length
     */
    public short getLength() {
        return this.length;
    }

    /**
     * @return the checksum
     */
    public short getChecksum() {
        return this.checksum;
    }

    /**
     * @param checksum
     *            the checksum to set
     * @return this
     */
    public UDP setChecksum(final short checksum) {
        this.checksum = checksum;
        return this;
    }

    @Override
    public void resetChecksum() {
        this.checksum = 0;
        super.resetChecksum();
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     */
    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        this.length = (short) (8 + (payloadData == null ? 0
                : payloadData.length));

        final byte[] data = new byte[this.length];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putShort((short) (this.sourcePort & 0xffff));
        bb.putShort((short) (this.destinationPort & 0xffff));
        bb.putShort(this.length);
        bb.putShort(this.checksum);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IPv4) {
            ((IPv4) this.parent).setProtocol(IPv4.PROTOCOL_UDP);
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;

            // compute pseudo header mac
            if (this.parent != null) {
                if (this.parent instanceof IPv4) {
                    final IPv4 ipv4 = (IPv4) this.parent;
                    accumulation += (ipv4.getSourceAddress() >> 16 & 0xffff)
                            + (ipv4.getSourceAddress() & 0xffff);
                    accumulation += (ipv4.getDestinationAddress() >> 16 & 0xffff)
                            + (ipv4.getDestinationAddress() & 0xffff);
                    accumulation += ipv4.getProtocol() & 0xff;
                    accumulation += length & 0xffff;
                } else if (this.parent instanceof IPv6) {
                    final IPv6 ipv6 = (IPv6) this.parent;
                    final int bbLength =
                            Ip6Address.BYTE_LENGTH * 2 // IPv6 src, dst
                                    + 2  // nextHeader (with padding)
                                    + 4; // length
                    final ByteBuffer bbChecksum = ByteBuffer.allocate(bbLength);
                    bbChecksum.put(ipv6.getSourceAddress());
                    bbChecksum.put(ipv6.getDestinationAddress());
                    bbChecksum.put((byte) 0); // padding
                    bbChecksum.put(ipv6.getNextHeader());
                    bbChecksum.putInt(length);
                    bbChecksum.rewind();
                    for (int i = 0; i < bbLength / 2; ++i) {
                        accumulation += 0xffff & bbChecksum.getShort();
                    }
                }
            }

            for (int i = 0; i < this.length / 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            // pad to an even number of shorts
            if (this.length % 2 > 0) {
                accumulation += (bb.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(6, this.checksum);
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
        result = prime * result + this.checksum;
        result = prime * result + this.destinationPort;
        result = prime * result + this.length;
        result = prime * result + this.sourcePort;
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
        if (!(obj instanceof UDP)) {
            return false;
        }
        final UDP other = (UDP) obj;
        if (this.checksum != other.checksum) {
            return false;
        }
        if (this.destinationPort != other.destinationPort) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        if (this.sourcePort != other.sourcePort) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for UDP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<UDP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, UDP_HEADER_LENGTH);

            UDP udp = new UDP();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            udp.sourcePort = (bb.getShort() & 0xffff);
            udp.destinationPort = (bb.getShort() & 0xffff);
            udp.length = bb.getShort();
            udp.checksum = bb.getShort();

            Deserializer<? extends IPacket> deserializer;
            if (UDP.PORT_DESERIALIZER_MAP.containsKey(udp.destinationPort)) {
                deserializer = UDP.PORT_DESERIALIZER_MAP.get(udp.destinationPort);
            } else if (UDP.PORT_DESERIALIZER_MAP.containsKey(udp.sourcePort)) {
                deserializer = UDP.PORT_DESERIALIZER_MAP.get(udp.sourcePort);
            } else {
                deserializer = Data.deserializer();
            }

            udp.payload = deserializer.deserialize(data, bb.position(),
                                                   bb.limit() - bb.position());
            udp.payload.setParent(udp);
            return udp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("sourcePort", Integer.toString(sourcePort))
                .add("destinationPort", Integer.toString(destinationPort))
                .add("length", Short.toString(length))
                .add("checksum", Short.toString(checksum))
                .toString();
    }
}
