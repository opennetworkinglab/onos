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

import org.onlab.packet.ipv6.IExtensionHeader;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.Redirect;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;

import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements ICMPv6 packet format. (RFC 4443)
 */
public class ICMP6 extends BasePacket {
    public static final byte HEADER_LENGTH = 4; // bytes

    // Type
    /** Destination Unreachable. */
    public static final byte DEST_UNREACH = (byte) 0x01;
    /** Packet Too Big. */
    public static final byte PKT_TOO_BIG = (byte) 0x02;
    /** Time Exceeded. */
    public static final byte TIME_EXCEED = (byte) 0x03;
    /** Parameter Problem. */
    public static final byte PARAM_ERR = (byte) 0x04;
    /** Echo Request. */
    public static final byte ECHO_REQUEST = (byte) 0x80;
    /** Echo Reply. */
    public static final byte ECHO_REPLY = (byte) 0x81;
    /** Multicast Listener Query. */
    public static final byte MCAST_QUERY = (byte) 0x82;
    /** Multicast Listener Report. */
    public static final byte MCAST_REPORT = (byte) 0x83;
    /** Multicast Listener Done. */
    public static final byte MCAST_DONE = (byte) 0x84;
    /** Router Solicitation. */
    public static final byte ROUTER_SOLICITATION = (byte) 0x85;
    /** Router Advertisement. */
    public static final byte ROUTER_ADVERTISEMENT = (byte) 0x86;
    /** Neighbor Solicitation. */
    public static final byte NEIGHBOR_SOLICITATION = (byte) 0x87;
    /** Neighbor Advertisement. */
    public static final byte NEIGHBOR_ADVERTISEMENT = (byte) 0x88;
    /** Redirect Message. */
    public static final byte REDIRECT = (byte) 0x89;

    // Code for DEST_UNREACH
    /** No route to destination. */
    public static final byte NO_ROUTE = (byte) 0x00;
    /** Communication with destination administratively prohibited. */
    public static final byte COMM_PROHIBIT = (byte) 0x01;
    /** Beyond scope of source address. */
    public static final byte BEYOND_SCOPE = (byte) 0x02;
    /** Address unreachable. */
    public static final byte ADDR_UNREACH = (byte) 0x03;
    /** Port unreachable. */
    public static final byte PORT_UNREACH = (byte) 0x04;
    /** Source address failed ingress/egress policy. */
    public static final byte FAIL_POLICY = (byte) 0x05;
    /** Reject route to destination. */
    public static final byte REJECT_ROUTE = (byte) 0x06;
    /** Error in Source Routing Header. */
    public static final byte SRC_ROUTING_HEADER_ERR = (byte) 0x07;

    // Code for TIME_EXCEED
    /** Hop limit exceeded in transit. */
    public static final byte HOP_LIMIT_EXCEED = (byte) 0x00;
    /** Fragment reassembly time exceeded. */
    public static final byte DEFRAG_TIME_EXCEED = (byte) 0x01;

    // Code for PARAM_ERR
    /** Erroneous header field encountered. */
    public static final byte HDR_FIELD_ERR = (byte) 0x00;
    /** Unrecognized Next Header type encountered. */
    public static final byte NEXT_HEADER_ERR = (byte) 0x01;
    /** Unrecognized IPv6 option encountered. */
    public static final byte IPV6_OPT_ERR = (byte) 0x01;

    public static final Map<Byte, Deserializer<? extends IPacket>> TYPE_DESERIALIZER_MAP =
            ImmutableMap.<Byte, Deserializer<? extends IPacket>>builder()
                .put(ICMP6.ROUTER_SOLICITATION, RouterSolicitation.deserializer())
                .put(ICMP6.ROUTER_ADVERTISEMENT, RouterAdvertisement.deserializer())
                .put(ICMP6.NEIGHBOR_SOLICITATION, NeighborSolicitation.deserializer())
                .put(ICMP6.NEIGHBOR_ADVERTISEMENT, NeighborAdvertisement.deserializer())
                .put(ICMP6.REDIRECT, Redirect.deserializer())
                .build();

    protected byte icmpType;
    protected byte icmpCode;
    protected short checksum;

    private static final byte[] ZERO_ADDRESS = new byte[Ip6Address.BYTE_LENGTH];

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
        final ByteBuffer bbData = ByteBuffer.wrap(data);

        // Creating ByteBuffer for checksum calculation
        final byte[] checksumData =
            new byte[IPv6.FIXED_HEADER_LENGTH + HEADER_LENGTH + payloadLength];
        final ByteBuffer bbChecksum = ByteBuffer.wrap(checksumData);

        //
        // Creating IPv6 Pseudo Header for checksum calculation according
        // to RFC 4443 and RFC 2460
        //
        IPv6 ipv6Parent = null;
        for (IPacket p = this.parent; p != null; p = p.getParent()) {
            if (p instanceof IPv6) {
                ipv6Parent = (IPv6) p;
                break;
            }
        }
        if (ipv6Parent != null) {
            bbChecksum.put(ipv6Parent.getSourceAddress());
            bbChecksum.put(ipv6Parent.getDestinationAddress());
        } else {
            // NOTE: IPv6 source and destination addresses unknown. Use zeroes.
            bbChecksum.put(ZERO_ADDRESS);
            bbChecksum.put(ZERO_ADDRESS);
        }
        bbChecksum.putInt(HEADER_LENGTH + payloadLength);
        bbChecksum.put((byte) 0);
        bbChecksum.put((byte) 0);
        bbChecksum.put((byte) 0);
        bbChecksum.put(IPv6.PROTOCOL_ICMP6);
        bbChecksum.put(this.icmpType);
        bbChecksum.put(this.icmpCode);
        bbChecksum.put((byte) 0);
        bbChecksum.put((byte) 0);

        bbData.put(this.icmpType);
        bbData.put(this.icmpCode);
        bbData.putShort(this.checksum);
        if (payloadData != null) {
            bbData.put(payloadData);
            bbChecksum.put(payloadData);
        }

        if (this.parent != null) {
            if (this.parent instanceof IPv6) {
                ((IPv6) this.parent).setNextHeader(IPv6.PROTOCOL_ICMP6);
            } else if (this.parent instanceof IExtensionHeader) {
                ((IExtensionHeader) this.parent).setNextHeader(IPv6.PROTOCOL_ICMP6);
            }
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bbData.rewind();
            bbChecksum.rewind();
            int accumulation = 0;

            for (int i = 0; i < checksumData.length / 2; ++i) {
                accumulation += 0xffff & bbChecksum.getShort();
            }
            // pad to an even number of shorts
            if (checksumData.length % 2 > 0) {
                accumulation += (bbChecksum.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bbData.putShort(2, this.checksum);
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

    /**
     * Deserializer function for ICMPv6 packets.
     *
     * @return deserializer function
     */
    public static Deserializer<ICMP6> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            ICMP6 icmp6 = new ICMP6();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            icmp6.icmpType = bb.get();
            icmp6.icmpCode = bb.get();
            icmp6.checksum = bb.getShort();

            Deserializer<? extends IPacket> deserializer;
            if (ICMP6.TYPE_DESERIALIZER_MAP.containsKey(icmp6.icmpType)) {
                deserializer = TYPE_DESERIALIZER_MAP.get(icmp6.icmpType);
            } else {
                deserializer = Data.deserializer();
            }
            icmp6.payload = deserializer.deserialize(data, bb.position(),
                                                bb.limit() - bb.position());
            icmp6.payload.setParent(icmp6);

            return icmp6;
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

    /**
     * Builds an ICMPv6 reply using the supplied ICMPv6 request.
     *
     * @param ethRequest the Ethernet packet containing the ICMPv6 ECHO request
     * @return the Ethernet packet containing the ICMPv6 ECHO reply
     */
    public static Ethernet buildIcmp6Reply(Ethernet ethRequest) {

        if (ethRequest.getEtherType() != Ethernet.TYPE_IPV6) {
            return null;
        }

        IPv6 ipv6Request = (IPv6) ethRequest.getPayload();

        if (ipv6Request.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return null;
        }

        Ethernet ethReply = new Ethernet();


        IPv6 ipv6Reply = new IPv6();

        byte[] destAddress = ipv6Request.getDestinationAddress();
        ipv6Reply.setDestinationAddress(ipv6Request.getSourceAddress());
        ipv6Reply.setSourceAddress(destAddress);
        ipv6Reply.setHopLimit((byte) 64);
        ipv6Reply.setTrafficClass(ipv6Request.getTrafficClass());
        ipv6Reply.setNextHeader(IPv6.PROTOCOL_ICMP6);

        ICMP6 icmpv6Reply = new ICMP6();
        icmpv6Reply.setPayload(ipv6Request.getPayload().getPayload());
        icmpv6Reply.setIcmpType(ICMP6.ECHO_REPLY);
        icmpv6Reply.setIcmpCode((byte) 0);
        ipv6Reply.setPayload(icmpv6Reply);

        ethReply.setEtherType(Ethernet.TYPE_IPV6);
        ethReply.setQinQVID(ethRequest.getQinQVID());
        ethReply.setQinQTPID(ethRequest.getQinQTPID());
        ethReply.setVlanID(ethRequest.getVlanID());
        ethReply.setDestinationMACAddress(ethRequest.getSourceMACAddress());
        ethReply.setSourceMACAddress(ethRequest.getDestinationMACAddress());
        ethReply.setPayload(ipv6Reply);

        return ethReply;
    }
}
