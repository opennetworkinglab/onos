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
package org.onosproject.net.flow.criteria;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

/**
 * Factory class to create various traffic selection criteria.
 */
public final class Criteria {

    //TODO: incomplete type implementation. Need to implement complete list from Criterion

    // Ban construction
    private Criteria() {
    }

    /**
     * Creates a match on IN_PORT field using the specified value.
     *
     * @param port inport value
     * @return match criterion
     */
    public static Criterion matchInPort(PortNumber port) {
        return new PortCriterion(port, Type.IN_PORT);
    }

    /**
     * Creates a match on IN_PHY_PORT field using the specified value.
     *
     * @param port inport value
     * @return match criterion
     */
    public static Criterion matchInPhyPort(PortNumber port) {
        return new PortCriterion(port, Type.IN_PHY_PORT);
    }

    /**
     * Creates a match on METADATA field using the specified value.
     *
     * @param metadata metadata value (64 bits data)
     * @return match criterion
     */
    public static Criterion matchMetadata(long metadata) {
        return new MetadataCriterion(metadata);
    }

    /**
     * Creates a match on ETH_DST field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param mac MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthDst(MacAddress mac) {
        return new EthCriterion(mac, Type.ETH_DST);
    }

    /**
     * Creates a match on ETH_SRC field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param mac MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthSrc(MacAddress mac) {
        return new EthCriterion(mac, Type.ETH_SRC);
    }

    /**
     * Creates a match on ETH_TYPE field using the specified value.
     *
     * @param ethType eth type value (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchEthType(int ethType) {
        return new EthTypeCriterion(ethType);
    }

    /**
     * Creates a match on VLAN ID field using the specified value.
     *
     * @param vlanId vlan id value
     * @return match criterion
     */
    public static Criterion matchVlanId(VlanId vlanId) {
        return new VlanIdCriterion(vlanId);
    }

    /**
     * Creates a match on VLAN PCP field using the specified value.
     *
     * @param vlanPcp vlan pcp value (3 bits)
     * @return match criterion
     */
    public static Criterion matchVlanPcp(byte vlanPcp) {
        return new VlanPcpCriterion(vlanPcp);
    }

    /**
     * Creates a match on IP DSCP field using the specified value.
     *
     * @param ipDscp ip dscp value (6 bits)
     * @return match criterion
     */
    public static Criterion matchIPDscp(byte ipDscp) {
        return new IPDscpCriterion(ipDscp);
    }

    /**
     * Creates a match on IP ECN field using the specified value.
     *
     * @param ipEcn ip ecn value (3 bits)
     * @return match criterion
     */
    public static Criterion matchIPEcn(byte ipEcn) {
        return new IPEcnCriterion(ipEcn);
    }

    /**
     * Creates a match on IP proto field using the specified value.
     *
     * @param proto ip protocol value (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchIPProtocol(short proto) {
        return new IPProtocolCriterion(proto);
    }

    /**
     * Creates a match on IPv4 source field using the specified value.
     *
     * @param ip ipv4 source value
     * @return match criterion
     */
    public static Criterion matchIPSrc(IpPrefix ip) {
        return new IPCriterion(ip, Type.IPV4_SRC);
    }

    /**
     * Creates a match on IPv4 destination field using the specified value.
     *
     * @param ip ipv4 source value
     * @return match criterion
     */
    public static Criterion matchIPDst(IpPrefix ip) {
        return new IPCriterion(ip, Type.IPV4_DST);
    }

    /**
     * Creates a match on TCP source port field using the specified value.
     *
     * @param tcpPort TCP source port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchTcpSrc(int tcpPort) {
        return new TcpPortCriterion(tcpPort, Type.TCP_SRC);
    }

    /**
     * Creates a match on TCP destination port field using the specified value.
     *
     * @param tcpPort TCP destination port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchTcpDst(int tcpPort) {
        return new TcpPortCriterion(tcpPort, Type.TCP_DST);
    }

    /**
     * Creates a match on UDP source port field using the specified value.
     *
     * @param udpPort UDP source port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchUdpSrc(int udpPort) {
        return new UdpPortCriterion(udpPort, Type.UDP_SRC);
    }

    /**
     * Creates a match on UDP destination port field using the specified value.
     *
     * @param udpPort UDP destination port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchUdpDst(int udpPort) {
        return new UdpPortCriterion(udpPort, Type.UDP_DST);
    }

    /**
     * Creates a match on SCTP source port field using the specified value.
     *
     * @param sctpPort SCTP source port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchSctpSrc(int sctpPort) {
        return new SctpPortCriterion(sctpPort, Type.SCTP_SRC);
    }

    /**
     * Creates a match on SCTP destination port field using the specified
     * value.
     *
     * @param sctpPort SCTP destination port (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchSctpDst(int sctpPort) {
        return new SctpPortCriterion(sctpPort, Type.SCTP_DST);
    }

    /**
     * Creates a match on ICMP type field using the specified value.
     *
     * @param icmpType ICMP type (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchIcmpType(short icmpType) {
        return new IcmpTypeCriterion(icmpType);
    }

    /**
     * Creates a match on ICMP code field using the specified value.
     *
     * @param icmpCode ICMP code (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchIcmpCode(short icmpCode) {
        return new IcmpCodeCriterion(icmpCode);
    }

    /**
     * Creates a match on IPv6 source field using the specified value.
     *
     * @param ip ipv6 source value
     * @return match criterion
     */
    public static Criterion matchIPv6Src(IpPrefix ip) {
        return new IPCriterion(ip, Type.IPV6_SRC);
    }

    /**
     * Creates a match on IPv6 destination field using the specified value.
     *
     * @param ip ipv6 destination value
     * @return match criterion
     */
    public static Criterion matchIPv6Dst(IpPrefix ip) {
        return new IPCriterion(ip, Type.IPV6_DST);
    }

    /**
     * Creates a match on IPv6 flow label field using the specified value.
     *
     * @param flowLabel IPv6 flow label (20 bits)
     * @return match criterion
     */
    public static Criterion matchIPv6FlowLabel(int flowLabel) {
        return new IPv6FlowLabelCriterion(flowLabel);
    }

    /**
     * Creates a match on ICMPv6 type field using the specified value.
     *
     * @param icmpv6Type ICMPv6 type (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchIcmpv6Type(short icmpv6Type) {
        return new Icmpv6TypeCriterion(icmpv6Type);
    }

    /**
     * Creates a match on ICMPv6 code field using the specified value.
     *
     * @param icmpv6Code ICMPv6 code (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchIcmpv6Code(short icmpv6Code) {
        return new Icmpv6CodeCriterion(icmpv6Code);
    }

    /**
     * Creates a match on IPv6 Neighbor Discovery target address using the
     * specified value.
     *
     * @param targetAddress IPv6 Neighbor Discovery target address
     * @return match criterion
     */
    public static Criterion matchIPv6NDTargetAddress(Ip6Address targetAddress) {
        return new IPv6NDTargetAddressCriterion(targetAddress);
    }

    /**
     * Creates a match on IPv6 Neighbor Discovery source link-layer address
     * using the specified value.
     *
     * @param mac IPv6 Neighbor Discovery source link-layer address
     * @return match criterion
     */
    public static Criterion matchIPv6NDSourceLinkLayerAddress(MacAddress mac) {
        return new IPv6NDLinkLayerAddressCriterion(mac, Type.IPV6_ND_SLL);
    }

    /**
     * Creates a match on IPv6 Neighbor Discovery target link-layer address
     * using the specified value.
     *
     * @param mac IPv6 Neighbor Discovery target link-layer address
     * @return match criterion
     */
    public static Criterion matchIPv6NDTargetLinkLayerAddress(MacAddress mac) {
        return new IPv6NDLinkLayerAddressCriterion(mac, Type.IPV6_ND_TLL);
    }

    /**
     * Creates a match on MPLS label.
     *
     * @param mplsLabel MPLS label (20 bits)
     * @return match criterion
     */
    public static Criterion matchMplsLabel(MplsLabel mplsLabel) {
        return new MplsCriterion(mplsLabel);
    }

    /**
     * Creates a match on IPv6 Extension Header pseudo-field fiags.
     * Those are defined in Criterion.IPv6ExthdrFlags.
     *
     * @param exthdrFlags IPv6 Extension Header pseudo-field flags (16 bits)
     * @return match criterion
     */
    public static Criterion matchIPv6ExthdrFlags(int exthdrFlags) {
        return new IPv6ExthdrFlagsCriterion(exthdrFlags);
    }

    /**
     * Creates a match on lambda field using the specified value.
     *
     * @param lambda lambda to match on (16 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchLambda(int lambda) {
        return new LambdaCriterion(lambda, Type.OCH_SIGID);
    }

    /**
     * Creates a match on optical signal type using the specified value.
     *
     * @param sigType optical signal type (8 bits unsigned integer)
     * @return match criterion
     */
    public static Criterion matchOpticalSignalType(short sigType) {
        return new OpticalSignalTypeCriterion(sigType, Type.OCH_SIGTYPE);
    }

    /**
     * Implementation of input port criterion.
     */
    public static final class PortCriterion implements Criterion {
        private final PortNumber port;
        private final Type type;

        /**
         * Constructor.
         *
         * @param port the input port number to match
         * @param type the match type. Should be either Type.IN_PORT or
         * Type.IN_PHY_PORT
         */
        public PortCriterion(PortNumber port, Type type) {
            this.port = port;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the input port number to match.
         *
         * @return the input port number to match
         */
        public PortNumber port() {
            return this.port;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("port", port).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), port);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PortCriterion) {
                PortCriterion that = (PortCriterion) obj;
                return Objects.equals(port, that.port) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of Metadata criterion.
     */
    public static final class MetadataCriterion implements Criterion {
        private final long metadata;

        /**
         * Constructor.
         *
         * @param metadata the metadata to match (64 bits data)
         */
        public MetadataCriterion(long metadata) {
            this.metadata = metadata;
        }

        @Override
        public Type type() {
            return Type.METADATA;
        }

        /**
         * Gets the metadata to match.
         *
         * @return the metadata to match (64 bits data)
         */
        public long metadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("metadata", Long.toHexString(metadata))
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), metadata);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof MetadataCriterion) {
                MetadataCriterion that = (MetadataCriterion) obj;
                return Objects.equals(metadata, that.metadata) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of MAC address criterion.
     */
    public static final class EthCriterion implements Criterion {
        private final MacAddress mac;
        private final Type type;

        /**
         * Constructor.
         *
         * @param mac the source or destination MAC address to match
         * @param type the match type. Should be either Type.ETH_DST or
         * Type.ETH_SRC
         */
        public EthCriterion(MacAddress mac, Type type) {
            this.mac = mac;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the MAC address to match.
         *
         * @return the MAC address to match
         */
        public MacAddress mac() {
            return this.mac;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("mac", mac).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, mac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof EthCriterion) {
                EthCriterion that = (EthCriterion) obj;
                return Objects.equals(mac, that.mac) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of Ethernet type criterion (16 bits unsigned integer).
     */
    public static final class EthTypeCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int ethType;              // Ethernet type value: 16 bits

        /**
         * Constructor.
         *
         * @param ethType the Ethernet frame type to match (16 bits unsigned
         * integer)
         */
        public EthTypeCriterion(int ethType) {
            this.ethType = ethType & MASK;
        }

        @Override
        public Type type() {
            return Type.ETH_TYPE;
        }

        /**
         * Gets the Ethernet frame type to match.
         *
         * @return the Ethernet frame type to match (16 bits unsigned integer)
         */
        public int ethType() {
            return ethType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("ethType", Long.toHexString(ethType))
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), ethType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof EthTypeCriterion) {
                EthTypeCriterion that = (EthTypeCriterion) obj;
                return Objects.equals(ethType, that.ethType) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of VLAN ID criterion.
     */
    public static final class VlanIdCriterion implements Criterion {
        private final VlanId vlanId;

        /**
         * Constructor.
         *
         * @param vlanId the VLAN ID to match
         */
        public VlanIdCriterion(VlanId vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public Type type() {
            return Type.VLAN_VID;
        }

        /**
         * Gets the VLAN ID to match.
         *
         * @return the VLAN ID to match
         */
        public VlanId vlanId() {
            return vlanId;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("vlanId", vlanId).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), vlanId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VlanIdCriterion) {
                VlanIdCriterion that = (VlanIdCriterion) obj;
                return Objects.equals(vlanId, that.vlanId) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of VLAN priority criterion (3 bits).
     */
    public static final class VlanPcpCriterion implements Criterion {
        private static final byte MASK = 0x7;
        private final byte vlanPcp;             // VLAN pcp value: 3 bits

        /**
         * Constructor.
         *
         * @param vlanPcp the VLAN priority to match (3 bits)
         */
        public VlanPcpCriterion(byte vlanPcp) {
            this.vlanPcp = (byte) (vlanPcp & MASK);
        }

        @Override
        public Type type() {
            return Type.VLAN_PCP;
        }

        /**
         * Gets the VLAN priority to match.
         *
         * @return the VLAN priority to match (3 bits)
         */
        public byte priority() {
            return vlanPcp;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("priority", Long.toHexString(vlanPcp)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), vlanPcp);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VlanPcpCriterion) {
                VlanPcpCriterion that = (VlanPcpCriterion) obj;
                return Objects.equals(vlanPcp, that.vlanPcp) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IP DSCP (Differentiated Services Code Point)
     * criterion (6 bits).
     */
    public static final class IPDscpCriterion implements Criterion {
        private static final byte MASK = 0x3f;
        private final byte ipDscp;              // IP DSCP value: 6 bits

        /**
         * Constructor.
         *
         * @param ipDscp the IP DSCP value to match
         */
        public IPDscpCriterion(byte ipDscp) {
            this.ipDscp = (byte) (ipDscp & MASK);
        }

        @Override
        public Type type() {
            return Type.IP_DSCP;
        }

        /**
         * Gets the IP DSCP value to match.
         *
         * @return the IP DSCP value to match
         */
        public byte ipDscp() {
            return ipDscp;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("ipDscp", Long.toHexString(ipDscp)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), ipDscp);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPDscpCriterion) {
                IPDscpCriterion that = (IPDscpCriterion) obj;
                return Objects.equals(ipDscp, that.ipDscp) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IP ECN (Explicit Congestion Notification) criterion
     * (3 bits).
     */
    public static final class IPEcnCriterion implements Criterion {
        private static final byte MASK = 0x7;
        private final byte ipEcn;               // IP ECN value: 3 bits

        /**
         * Constructor.
         *
         * @param ipEcn the IP ECN value to match (3 bits)
         */
        public IPEcnCriterion(byte ipEcn) {
            this.ipEcn = (byte) (ipEcn & MASK);
        }

        @Override
        public Type type() {
            return Type.IP_ECN;
        }

        /**
         * Gets the IP ECN value to match.
         *
         * @return the IP ECN value to match (3 bits)
         */
        public byte ipEcn() {
            return ipEcn;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("ipEcn", Long.toHexString(ipEcn)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), ipEcn);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPEcnCriterion) {
                IPEcnCriterion that = (IPEcnCriterion) obj;
                return Objects.equals(ipEcn, that.ipEcn) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of Internet Protocol Number criterion (8 bits unsigned)
     * integer.
     */
    public static final class IPProtocolCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short proto;      // IP protocol number: 8 bits

        /**
         * Constructor.
         *
         * @param protocol the IP protocol (e.g., TCP=6, UDP=17) to match
         * (8 bits unsigned integer)
         */
        public IPProtocolCriterion(short protocol) {
            this.proto = (short) (protocol & MASK);
        }

        @Override
        public Type type() {
            return Type.IP_PROTO;
        }

        /**
         * Gets the IP protocol to match.
         *
         * @return the IP protocol to match (8 bits unsigned integer)
         */
        public short protocol() {
            return proto;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("protocol", proto).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), proto);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPProtocolCriterion) {
                IPProtocolCriterion that = (IPProtocolCriterion) obj;
                return Objects.equals(proto, that.proto);
            }
            return false;
        }
    }

    /**
     * Implementation of IP address criterion.
     */
    public static final class IPCriterion implements Criterion {
        private final IpPrefix ip;
        private final Type type;

        /**
         * Constructor.
         *
         * @param ip the IP prefix to match. Could be either IPv4 or IPv6
         * @param type the match type. Should be one of the following:
         * Type.IPV4_SRC, Type.IPV4_DST, Type.IPV6_SRC, Type.IPV6_DST
         */
        public IPCriterion(IpPrefix ip, Type type) {
            this.ip = ip;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the IP prefix to match.
         *
         * @return the IP prefix to match
         */
        public IpPrefix ip() {
            return this.ip;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("ip", ip).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, ip);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPCriterion) {
                IPCriterion that = (IPCriterion) obj;
                return Objects.equals(ip, that.ip) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of TCP port criterion (16 bits unsigned integer).
     */
    public static final class TcpPortCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int tcpPort;              // Port value: 16 bits
        private final Type type;

        /**
         * Constructor.
         *
         * @param tcpPort the TCP port to match (16 bits unsigned integer)
         * @param type the match type. Should be either Type.TCP_SRC or
         * Type.TCP_DST
         */
        public TcpPortCriterion(int tcpPort, Type type) {
            this.tcpPort = tcpPort & MASK;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the TCP port to match.
         *
         * @return the TCP port to match (16 bits unsigned integer)
         */
        public int tcpPort() {
            return this.tcpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("tcpPort", tcpPort).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, tcpPort);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TcpPortCriterion) {
                TcpPortCriterion that = (TcpPortCriterion) obj;
                return Objects.equals(tcpPort, that.tcpPort) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of UDP port criterion (16 bits unsigned integer).
     */
    public static final class UdpPortCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int udpPort;              // Port value: 16 bits
        private final Type type;

        /**
         * Constructor.
         *
         * @param udpPort the UDP port to match (16 bits unsigned integer)
         * @param type the match type. Should be either Type.UDP_SRC or
         * Type.UDP_DST
         */
        public UdpPortCriterion(int udpPort, Type type) {
            this.udpPort = udpPort & MASK;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the UDP port to match.
         *
         * @return the UDP port to match (16 bits unsigned integer)
         */
        public int udpPort() {
            return this.udpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("udpPort", udpPort).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, udpPort);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof UdpPortCriterion) {
                UdpPortCriterion that = (UdpPortCriterion) obj;
                return Objects.equals(udpPort, that.udpPort) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of SCTP port criterion (16 bits unsigned integer).
     */
    public static final class SctpPortCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int sctpPort;             // Port value: 16 bits
        private final Type type;

        /**
         * Constructor.
         *
         * @param sctpPort the SCTP port to match (16 bits unsigned integer)
         * @param type the match type. Should be either Type.SCTP_SRC or
         * Type.SCTP_DST
         */
        public SctpPortCriterion(int sctpPort, Type type) {
            this.sctpPort = sctpPort & MASK;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the SCTP port to match.
         *
         * @return the SCTP port to match (16 bits unsigned integer)
         */
        public int sctpPort() {
            return this.sctpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("sctpPort", sctpPort).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, sctpPort);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof SctpPortCriterion) {
                SctpPortCriterion that = (SctpPortCriterion) obj;
                return Objects.equals(sctpPort, that.sctpPort) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of ICMP type criterion (8 bits unsigned integer).
     */
    public static final class IcmpTypeCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short icmpType;           // The ICMP type: 8 bits

        /**
         * Constructor.
         *
         * @param icmpType the ICMP type to match (8 bits unsigned integer)
         */
        public IcmpTypeCriterion(short icmpType) {
            this.icmpType = (short) (icmpType & MASK);
        }

        @Override
        public Type type() {
            return Type.ICMPV4_TYPE;
        }

        /**
         * Gets the ICMP type to match.
         *
         * @return the ICMP type to match (8 bits unsigned integer)
         */
        public short icmpType() {
            return icmpType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("icmpType", icmpType).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), icmpType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IcmpTypeCriterion) {
                IcmpTypeCriterion that = (IcmpTypeCriterion) obj;
                return Objects.equals(icmpType, that.icmpType) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of ICMP code criterion (8 bits unsigned integer).
     */
    public static final class IcmpCodeCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short icmpCode;           // The ICMP code: 8 bits

        /**
         * Constructor.
         *
         * @param icmpCode the ICMP code to match (8 bits unsigned integer)
         */
        public IcmpCodeCriterion(short icmpCode) {
            this.icmpCode = (short) (icmpCode & MASK);
        }

        @Override
        public Type type() {
            return Type.ICMPV4_CODE;
        }

        /**
         * Gets the ICMP code to match.
         *
         * @return the ICMP code to match (8 bits unsigned integer)
         */
        public short icmpCode() {
            return icmpCode;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("icmpCode", icmpCode).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), icmpCode);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IcmpCodeCriterion) {
                IcmpCodeCriterion that = (IcmpCodeCriterion) obj;
                return Objects.equals(icmpCode, that.icmpCode) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IPv6 Flow Label (RFC 6437) criterion (20 bits unsigned
     * integer).
     */
    public static final class IPv6FlowLabelCriterion implements Criterion {
        private static final int MASK = 0xfffff;
        private final int flowLabel;            // IPv6 flow label: 20 bits

        /**
         * Constructor.
         *
         * @param flowLabel the IPv6 flow label to match (20 bits)
         */
        public IPv6FlowLabelCriterion(int flowLabel) {
            this.flowLabel = flowLabel & MASK;
        }

        @Override
        public Type type() {
            return Type.IPV6_FLABEL;
        }

        /**
         * Gets the IPv6 flow label to match.
         *
         * @return the IPv6 flow label to match (20 bits)
         */
        public int flowLabel() {
            return flowLabel;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("flowLabel", Long.toHexString(flowLabel)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), flowLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPv6FlowLabelCriterion) {
                IPv6FlowLabelCriterion that = (IPv6FlowLabelCriterion) obj;
                return Objects.equals(flowLabel, that.flowLabel) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of ICMPv6 type criterion (8 bits unsigned integer).
     */
    public static final class Icmpv6TypeCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short icmpv6Type;         // ICMPv6 type: 8 bits

        /**
         * Constructor.
         *
         * @param icmpv6Type the ICMPv6 type to match (8 bits unsigned integer)
         */
        public Icmpv6TypeCriterion(short icmpv6Type) {
            this.icmpv6Type = (short) (icmpv6Type & MASK);
        }

        @Override
        public Type type() {
            return Type.ICMPV6_TYPE;
        }

        /**
         * Gets the ICMPv6 type to match.
         *
         * @return the ICMPv6 type to match (8 bits unsigned integer)
         */
        public short icmpv6Type() {
            return icmpv6Type;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("icmpv6Type", icmpv6Type).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), icmpv6Type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Icmpv6TypeCriterion) {
                Icmpv6TypeCriterion that = (Icmpv6TypeCriterion) obj;
                return Objects.equals(icmpv6Type, that.icmpv6Type) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of ICMPv6 code criterion (8 bits unsigned integer).
     */
    public static final class Icmpv6CodeCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short icmpv6Code;         // ICMPv6 code: 8 bits

        /**
         * Constructor.
         *
         * @param icmpv6Code the ICMPv6 code to match (8 bits unsigned integer)
         */
        public Icmpv6CodeCriterion(short icmpv6Code) {
            this.icmpv6Code = (short) (icmpv6Code & MASK);
        }

        @Override
        public Type type() {
            return Type.ICMPV6_CODE;
        }

        /**
         * Gets the ICMPv6 code to match.
         *
         * @return the ICMPv6 code to match (8 bits unsigned integer)
         */
        public short icmpv6Code() {
            return icmpv6Code;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("icmpv6Code", icmpv6Code).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), icmpv6Code);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Icmpv6CodeCriterion) {
                Icmpv6CodeCriterion that = (Icmpv6CodeCriterion) obj;
                return Objects.equals(icmpv6Code, that.icmpv6Code) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IPv6 Neighbor Discovery target address criterion.
     */
    public static final class IPv6NDTargetAddressCriterion
                                implements Criterion {
        private final Ip6Address targetAddress;

        /**
         * Constructor.
         *
         * @param targetAddress the IPv6 target address to match
         */
        public IPv6NDTargetAddressCriterion(Ip6Address targetAddress) {
            this.targetAddress = targetAddress;
        }

        @Override
        public Type type() {
            return Type.IPV6_ND_TARGET;
        }

        /**
         * Gets the IPv6 target address to match.
         *
         * @return the IPv6 target address to match
         */
        public Ip6Address targetAddress() {
            return this.targetAddress;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("targetAddress", targetAddress).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), targetAddress);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPv6NDTargetAddressCriterion) {
                IPv6NDTargetAddressCriterion that =
                    (IPv6NDTargetAddressCriterion) obj;
                return Objects.equals(targetAddress, that.targetAddress) &&
                        Objects.equals(type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IPv6 Neighbor Discovery link-layer address criterion.
     */
    public static final class IPv6NDLinkLayerAddressCriterion
                                implements Criterion {
        private final MacAddress mac;
        private final Type type;

        /**
         * Constructor.
         *
         * @param mac the source or destination link-layer address to match
         * @param type the match type. Should be either Type.IPV6_ND_SLL or
         * Type.IPV6_ND_TLL
         */
        public IPv6NDLinkLayerAddressCriterion(MacAddress mac, Type type) {
            this.mac = mac;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the MAC link-layer address to match.
         *
         * @return the MAC link-layer address to match
         */
        public MacAddress mac() {
            return this.mac;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("mac", mac).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, mac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPv6NDLinkLayerAddressCriterion) {
                IPv6NDLinkLayerAddressCriterion that =
                    (IPv6NDLinkLayerAddressCriterion) obj;
                return Objects.equals(mac, that.mac) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of MPLS tag criterion (20 bits).
     */
    public static final class MplsCriterion implements Criterion {
        private static final int MASK = 0xfffff;
        private final MplsLabel mplsLabel;

        public MplsCriterion(MplsLabel mplsLabel) {
            this.mplsLabel = mplsLabel;
        }

        @Override
        public Type type() {
            return Type.MPLS_LABEL;
        }

        public MplsLabel label() {
            return mplsLabel;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("mpls", mplsLabel).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), mplsLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof MplsCriterion) {
                MplsCriterion that = (MplsCriterion) obj;
                return Objects.equals(mplsLabel, that.mplsLabel) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of IPv6 Extension Header pseudo-field criterion
     * (16 bits). Those are defined in Criterion.IPv6ExthdrFlags.
     */
    public static final class IPv6ExthdrFlagsCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int exthdrFlags;          // IPv6 Exthdr flags: 16 bits

        /**
         * Constructor.
         *
         * @param exthdrFlags the IPv6 Extension Header pseudo-field flags
         * to match (16 bits). Those are defined in Criterion.IPv6ExthdrFlags
         */
        public IPv6ExthdrFlagsCriterion(int exthdrFlags) {
            this.exthdrFlags = exthdrFlags & MASK;
        }

        @Override
        public Type type() {
            return Type.IPV6_EXTHDR;
        }

        /**
         * Gets the IPv6 Extension Header pseudo-field flags to match.
         *
         * @return the IPv6 Extension Header pseudo-field flags to match
         * (16 bits). Those are defined in Criterion.IPv6ExthdrFlags
         */
        public int exthdrFlags() {
            return exthdrFlags;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("exthdrFlags", Long.toHexString(exthdrFlags)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), exthdrFlags);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof IPv6ExthdrFlagsCriterion) {
                IPv6ExthdrFlagsCriterion that = (IPv6ExthdrFlagsCriterion) obj;
                return Objects.equals(exthdrFlags, that.exthdrFlags) &&
                        Objects.equals(this.type(), that.type());
            }
            return false;
        }
    }

    /**
     * Implementation of lambda (wavelength) criterion (16 bits unsigned
     * integer).
     */
    public static final class LambdaCriterion implements Criterion {
        private static final int MASK = 0xffff;
        private final int lambda;               // Lambda value: 16 bits
        private final Type type;

        /**
         * Constructor.
         *
         * @param lambda the lambda (wavelength) to match (16 bits unsigned
         * integer)
         * @param type the match type. Should be Type.OCH_SIGID
         */
        public LambdaCriterion(int lambda, Type type) {
            this.lambda = lambda & MASK;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the lambda (wavelength) to match.
         *
         * @return the lambda (wavelength) to match (16 bits unsigned integer)
         */
        public int lambda() {
            return lambda;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("lambda", lambda).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, lambda);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof LambdaCriterion) {
                LambdaCriterion that = (LambdaCriterion) obj;
                return Objects.equals(lambda, that.lambda) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }

    /**
     * Implementation of optical signal type criterion (8 bits unsigned
     * integer).
     */
    public static final class OpticalSignalTypeCriterion implements Criterion {
        private static final short MASK = 0xff;
        private final short signalType;         // Signal type value: 8 bits
        private final Type type;

        /**
         * Constructor.
         *
         * @param signalType the optical signal type to match (8 bits unsigned
         * integer)
         * @param type the match type. Should be Type.OCH_SIGTYPE
         */
        public OpticalSignalTypeCriterion(short signalType, Type type) {
            this.signalType = (short) (signalType & MASK);
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the optical signal type to match.
         *
         * @return the optical signal type to match (8 bits unsigned integer)
         */
        public short signalType() {
            return signalType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                .add("signalType", signalType).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, signalType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof OpticalSignalTypeCriterion) {
                OpticalSignalTypeCriterion that = (OpticalSignalTypeCriterion) obj;
                return Objects.equals(signalType, that.signalType) &&
                        Objects.equals(type, that.type);
            }
            return false;
        }
    }
}
