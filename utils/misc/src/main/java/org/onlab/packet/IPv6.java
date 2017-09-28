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

import org.onlab.packet.ipv6.Authentication;
import org.onlab.packet.ipv6.DestinationOptions;
import org.onlab.packet.ipv6.EncapSecurityPayload;
import org.onlab.packet.ipv6.Fragment;
import org.onlab.packet.ipv6.HopByHopOptions;
import org.onlab.packet.ipv6.IExtensionHeader;
import org.onlab.packet.ipv6.Routing;

import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements IPv6 packet format. (RFC 2460)
 */
public class IPv6 extends IP implements IExtensionHeader {
    public static final byte FIXED_HEADER_LENGTH = 40; // bytes

    public static final byte PROTOCOL_TCP = 0x6;
    public static final byte PROTOCOL_UDP = 0x11;
    public static final byte PROTOCOL_ICMP6 = 0x3A;
    public static final byte PROTOCOL_HOPOPT = 0x00;
    public static final byte PROTOCOL_ROUTING = 0x2B;
    public static final byte PROTOCOL_FRAG = 0x2C;
    public static final byte PROTOCOL_ESP = 0x32;
    public static final byte PROTOCOL_AH = 0x33;
    public static final byte PROTOCOL_DSTOPT = 0x3C;

    public static final byte LINK_LOCAL_0 = (byte) 0xfe;
    public static final byte LINK_LOCAL_1 = (byte) 0x80;

    public static final Map<Byte, Deserializer<? extends IPacket>> PROTOCOL_DESERIALIZER_MAP =
            ImmutableMap.<Byte, Deserializer<? extends IPacket>>builder()
                .put(IPv6.PROTOCOL_ICMP6, ICMP6.deserializer())
                .put(IPv6.PROTOCOL_TCP, TCP.deserializer())
                .put(IPv6.PROTOCOL_UDP, UDP.deserializer())
                .put(IPv6.PROTOCOL_HOPOPT, HopByHopOptions.deserializer())
                .put(IPv6.PROTOCOL_ROUTING, Routing.deserializer())
                .put(IPv6.PROTOCOL_FRAG, Fragment.deserializer())
                .put(IPv6.PROTOCOL_ESP, EncapSecurityPayload.deserializer())
                .put(IPv6.PROTOCOL_AH, Authentication.deserializer())
                .put(IPv6.PROTOCOL_DSTOPT, DestinationOptions.deserializer())
                .build();

    protected byte version;
    protected byte trafficClass;
    protected int flowLabel;
    protected short payloadLength;
    protected byte nextHeader;
    protected byte hopLimit;
    protected byte[] sourceAddress = new byte[Ip6Address.BYTE_LENGTH];
    protected byte[] destinationAddress = new byte[Ip6Address.BYTE_LENGTH];

    /**
     * Default constructor that sets the version to 6.
     */
    public IPv6() {
        super();
        this.version = 6;
    }

    @Override
    public byte getVersion() {
        return this.version;
    }

    @Override
    public IPv6 setVersion(final byte version) {
        this.version = version;
        return this;
    }

    /**
     * Gets traffic class.
     *
     * @return the traffic class
     */
    public byte getTrafficClass() {
        return this.trafficClass;
    }

    /**
     * Sets traffic class.
     *
     * @param trafficClass the traffic class to set
     * @return this
     */
    public IPv6 setTrafficClass(final byte trafficClass) {
        this.trafficClass = trafficClass;
        return this;
    }

    /**
     * Gets flow label.
     *
     * @return the flow label
     */
    public int getFlowLabel() {
        return this.flowLabel;
    }

    /**
     * Sets flow label.
     *
     * @param flowLabel the flow label to set
     * @return this
     */
    public IPv6 setFlowLabel(final int flowLabel) {
        this.flowLabel = flowLabel;
        return this;
    }

    @Override
    public byte getNextHeader() {
        return this.nextHeader;
    }

    @Override
    public IPv6 setNextHeader(final byte nextHeader) {
        this.nextHeader = nextHeader;
        return this;
    }

    /**
     * Gets hop limit.
     *
     * @return the hop limit
     */
    public byte getHopLimit() {
        return this.hopLimit;
    }

    /**
     * Sets hop limit.
     *
     * @param hopLimit the hop limit to set
     * @return this
     */
    public IPv6 setHopLimit(final byte hopLimit) {
        this.hopLimit = hopLimit;
        return this;
    }

    /**
     * Gets source address.
     *
     * @return the IPv6 source address
     */
    public byte[] getSourceAddress() {
        return this.sourceAddress;
    }

    /**
     * Sets source address.
     *
     * @param sourceAddress the IPv6 source address to set
     * @return this
     */
    public IPv6 setSourceAddress(final byte[] sourceAddress) {
        this.sourceAddress = Arrays.copyOfRange(sourceAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    /**
     * Gets destination address.
     *
     * @return the IPv6 destination address
     */
    public byte[] getDestinationAddress() {
        return this.destinationAddress;
    }

    /**
     * Sets destination address.
     *
     * @param destinationAddress the IPv6 destination address to set
     * @return this
     */
    public IPv6 setDestinationAddress(final byte[] destinationAddress) {
        this.destinationAddress = Arrays.copyOfRange(destinationAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        this.payloadLength = 0;
        if (payloadData != null) {
            this.payloadLength = (short) payloadData.length;
        }

        final byte[] data = new byte[FIXED_HEADER_LENGTH + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt((this.version & 0xf) << 28 | (this.trafficClass & 0xff) << 20 | this.flowLabel & 0xfffff);
        bb.putShort(this.payloadLength);
        bb.put(this.nextHeader);
        bb.put(this.hopLimit);
        bb.put(this.sourceAddress, 0, Ip6Address.BYTE_LENGTH);
        bb.put(this.destinationAddress, 0, Ip6Address.BYTE_LENGTH);

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
        final int prime = 2521;
        int result = super.hashCode();
        ByteBuffer bb;
        bb = ByteBuffer.wrap(this.destinationAddress);
        for (int i = 0; i < 4; i++) {
            result = prime * result + bb.getInt();
        }
        result = prime * result + this.trafficClass;
        result = prime * result + this.flowLabel;
        result = prime * result + this.hopLimit;
        result = prime * result + this.nextHeader;
        result = prime * result + this.payloadLength;
        bb = ByteBuffer.wrap(this.sourceAddress);
        for (int i = 0; i < 4; i++) {
            result = prime * result + bb.getInt();
        }
        result = prime * result + this.version;
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
        if (!(obj instanceof IPv6)) {
            return false;
        }
        final IPv6 other = (IPv6) obj;
        if (!Arrays.equals(this.destinationAddress, other.destinationAddress)) {
            return false;
        }
        if (this.trafficClass != other.trafficClass) {
            return false;
        }
        if (this.flowLabel != other.flowLabel) {
            return false;
        }
        if (this.hopLimit != other.hopLimit) {
            return false;
        }
        if (this.nextHeader != other.nextHeader) {
            return false;
        }
        if (this.payloadLength != other.payloadLength) {
            return false;
        }
        if (!Arrays.equals(this.sourceAddress, other.sourceAddress)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for IPv6 packets.
     *
     * @return deserializer function
     */
    public static Deserializer<IPv6> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, FIXED_HEADER_LENGTH);

            IPv6 ipv6 = new IPv6();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            int iscratch = bb.getInt();

            ipv6.version = (byte) (iscratch >> 28 & 0xf);
            ipv6.trafficClass = (byte) (iscratch >> 20 & 0xff);
            ipv6.flowLabel = iscratch & 0xfffff;
            ipv6.payloadLength = bb.getShort();
            ipv6.nextHeader = bb.get();
            ipv6.hopLimit = bb.get();
            bb.get(ipv6.sourceAddress, 0, Ip6Address.BYTE_LENGTH);
            bb.get(ipv6.destinationAddress, 0, Ip6Address.BYTE_LENGTH);

            Deserializer<? extends IPacket> deserializer;
            if (IPv6.PROTOCOL_DESERIALIZER_MAP.containsKey(ipv6.nextHeader)) {
                deserializer = IPv6.PROTOCOL_DESERIALIZER_MAP.get(ipv6.nextHeader);
            } else {
                deserializer = Data.deserializer();
            }

            int remainingLength = bb.limit() - bb.position();
            int payloadLength = ipv6.payloadLength;
            int bytesToRead = (payloadLength <= remainingLength) ?
                    payloadLength : remainingLength;
            ipv6.payload = deserializer.deserialize(data, bb.position(), bytesToRead);
            ipv6.payload.setParent(ipv6);

            return ipv6;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("version", Byte.toString(version))
                .add("trafficClass", Byte.toString(trafficClass))
                .add("flowLabel", Integer.toString(flowLabel))
                .add("payloadLength", Short.toString(payloadLength))
                .add("nextHeader", Byte.toString(nextHeader))
                .add("hopLimit", Byte.toString(hopLimit))
                .add("sourceAddress", Arrays.toString(sourceAddress))
                .add("destinationAddress", Arrays.toString(destinationAddress))
                .toString();
    }

    /**
     * According to the RFC 4291, the solicitation node addresses are
     * formed by taking the low-order 24 bits of an address (unicast or anycast)
     * and appending those bits to the prefix FF02:0:0:0:0:1:FF00::/104.
     *
     * Solicited-Node Address:  FF02:0:0:0:0:1:FFXX:XXXX
     *
     * @param targetIp the unicast or anycast address
     * @return the computed solicitation node address
     */
    public static byte[] getSolicitNodeAddress(byte[] targetIp) {
        checkArgument(targetIp.length == Ip6Address.BYTE_LENGTH);
        return new byte[] {
                (byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, (byte) 0xff,
                targetIp[targetIp.length - 3],
                targetIp[targetIp.length - 2],
                targetIp[targetIp.length - 1]
        };
    }

    /**
     * According to the RFC 2464, an IPv6 packet with a multicast
     * destination address DST, consisting of the sixteen octets DST[1]
     * through DST[16], is transmitted to the Ethernet multicast address
     * whose first two octets are the value 3333 hexadecimal and whose last
     * four octets are the last four octets of DST.
     *
     *                   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *                   |0 0 1 1 0 0 1 1|0 0 1 1 0 0 1 1|
     *                   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *                   |   DST[13]     |   DST[14]     |
     *                   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *                   |   DST[15]     |   DST[16]     |
     *                   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     * @param targetIp the multicast address.
     * @return the multicast mac address
     */
    public static byte[] getMCastMacAddress(byte[] targetIp) {
        checkArgument(targetIp.length == Ip6Address.BYTE_LENGTH);
        return new byte[] {
                0x33, 0x33,
                targetIp[targetIp.length - 4],
                targetIp[targetIp.length - 3],
                targetIp[targetIp.length - 2],
                targetIp[targetIp.length - 1],
        };
    }

    /**
     * According to the RFC 4291, an IPv6 link local address is an IPv6
     * unicast address that can be automatically configured on any interface
     * using the link-local prefix FE80::/10 (1111 1110 10) and the interface
     * identifier in the modified EUI-64 format.
     *
     *    +----------------------------------------------------------------+
     *    |  10 bits   |         54 bits         |          64 bits        |
     *    +----------- +-------------------------+-------------------------+
     *    | 1111111010 |           0             |       interface ID      |
     *    +----------- +-------------------------+-------------------------+
     *
     * @param targetIp the ip address to verify
     * @return true if the ipv6 address is link local,
     * false otherwise
     */
    public static boolean isLinkLocalAddress(byte[] targetIp) {
        checkArgument(targetIp.length == Ip6Address.BYTE_LENGTH);
        return (targetIp[0] & 0xff) == 0xfe && (targetIp[1] & 0xc0) == 0x80;
    }

    /**
     * Returns the auto-generated link local address using the
     * mac address as parameter.
     *
     * @param macAddress the mac address to use
     * @return the ipv6 link local address
     */
    public static byte[] getLinkLocalAddress(byte[] macAddress) {
        checkArgument(macAddress.length == MacAddress.MAC_ADDRESS_LENGTH);
        return new byte[] {
                LINK_LOCAL_0,
                LINK_LOCAL_1,
                0, 0, 0, 0, 0, 0,
                (byte) (macAddress[0] ^ (1 << 1)),
                macAddress[1],
                macAddress[2],
                (byte) 0xff,
                (byte) 0xfe,
                macAddress[3],
                macAddress[4],
                macAddress[5],
        };
    }

    /**
     * Returns the mac address from the auto-generated
     * link local address.
     *
     * @param linkLocalAddress the ipv6 to use
     * @return the mac address
     */
    public static byte[] getMacAddress(byte[] linkLocalAddress) {
        return !isLinkLocalAddress(linkLocalAddress) ? null : new byte[] {
                (byte) (linkLocalAddress[8] ^ (1 << 1)),
                linkLocalAddress[9],
                linkLocalAddress[10],
                linkLocalAddress[13],
                linkLocalAddress[14],
                linkLocalAddress[15],
        };
    }



}
