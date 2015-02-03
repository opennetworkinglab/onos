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
        return new PortCriterion(port);
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
     * @param ethType eth type value
     * @return match criterion
     */
    public static Criterion matchEthType(Short ethType) {
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
     * @param vlanPcp vlan pcp value
     * @return match criterion
     */
    public static Criterion matchVlanPcp(Byte vlanPcp) {
        return new VlanPcpCriterion(vlanPcp);
    }

    /**
     * Creates a match on IP proto field using the specified value.
     *
     * @param proto ip protocol value
     * @return match criterion
     */
    public static Criterion matchIPProtocol(Byte proto) {
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
     * @param tcpPort TCP source port
     * @return match criterion
     */
    public static Criterion matchTcpSrc(Short tcpPort) {
        return new TcpPortCriterion(tcpPort, Type.TCP_SRC);
    }

    /**
     * Creates a match on TCP destination port field using the specified value.
     *
     * @param tcpPort TCP destination port
     * @return match criterion
     */
    public static Criterion matchTcpDst(Short tcpPort) {
        return new TcpPortCriterion(tcpPort, Type.TCP_DST);
    }

    /**
     * Creates a match on UDP source port field using the specified value.
     *
     * @param udpPort UDP source port
     * @return match criterion
     */
    public static Criterion matchUdpSrc(Short udpPort) {
        return new UdpPortCriterion(udpPort, Type.UDP_SRC);
    }

    /**
     * Creates a match on UDP destination port field using the specified value.
     *
     * @param udpPort UDP destination port
     * @return match criterion
     */
    public static Criterion matchUdpDst(Short udpPort) {
        return new UdpPortCriterion(udpPort, Type.UDP_DST);
    }

    /**
     * Creates a match on SCTP source port field using the specified value.
     *
     * @param sctpPort SCTP source port
     * @return match criterion
     */
    public static Criterion matchSctpSrc(Short sctpPort) {
        return new SctpPortCriterion(sctpPort, Type.SCTP_SRC);
    }

    /**
     * Creates a match on SCTP destination port field using the specified
     * value.
     *
     * @param sctpPort SCTP destination port
     * @return match criterion
     */
    public static Criterion matchSctpDst(Short sctpPort) {
        return new SctpPortCriterion(sctpPort, Type.SCTP_DST);
    }

    /**
     * Creates a match on ICMP type field using the specified value.
     *
     * @param icmpType ICMP type
     * @return match criterion
     */
    public static Criterion matchIcmpType(Byte icmpType) {
        return new IcmpTypeCriterion(icmpType);
    }

    /**
     * Creates a match on ICMP code field using the specified value.
     *
     * @param icmpCode ICMP code
     * @return match criterion
     */
    public static Criterion matchIcmpCode(Byte icmpCode) {
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
     * @param flowLabel IPv6 flow label
     * @return match criterion
     */
    public static Criterion matchIPv6FlowLabel(Integer flowLabel) {
        return new IPv6FlowLabelCriterion(flowLabel);
    }

    /**
     * Creates a match on ICMPv6 type field using the specified value.
     *
     * @param icmpv6Type ICMPv6 type
     * @return match criterion
     */
    public static Criterion matchIcmpv6Type(Byte icmpv6Type) {
        return new Icmpv6TypeCriterion(icmpv6Type);
    }

    /**
     * Creates a match on ICMPv6 code field using the specified value.
     *
     * @param icmpv6Code ICMPv6 code
     * @return match criterion
     */
    public static Criterion matchIcmpv6Code(Byte icmpv6Code) {
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
     * @param mplsLabel MPLS label
     * @return match criterion
     */
    public static Criterion matchMplsLabel(Integer mplsLabel) {
        return new MplsCriterion(mplsLabel);
    }

    /**
     * Creates a match on lambda field using the specified value.
     *
     * @param lambda lambda to match on
     * @return match criterion
     */
    public static Criterion matchLambda(Short lambda) {
        return new LambdaCriterion(lambda, Type.OCH_SIGID);
    }

    /**
     * Creates a match on optical signal type using the specified value.
     *
     * @param sigType optical signal type
     * @return match criterion
     */
    public static Criterion matchOpticalSignalType(Short sigType) {
        return new OpticalSignalTypeCriterion(sigType, Type.OCH_SIGTYPE);
    }

    /**
     * Implementation of input port criterion.
     */
    public static final class PortCriterion implements Criterion {
        private final PortNumber port;

        /**
         * Constructor.
         *
         * @param port the input port number to match
         */
        public PortCriterion(PortNumber port) {
            this.port = port;
        }

        @Override
        public Type type() {
            return Type.IN_PORT;
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
     * Implementation of Ethernet type criterion.
     */
    public static final class EthTypeCriterion implements Criterion {
        private final Short ethType;

        /**
         * Constructor.
         *
         * @param ethType the Ethernet frame type to match
         */
        public EthTypeCriterion(Short ethType) {
            this.ethType = ethType;
        }

        @Override
        public Type type() {
            return Type.ETH_TYPE;
        }

        /**
         * Gets the Ethernet frame type to match.
         *
         * @return the Ethernet frame type to match
         */
        public Short ethType() {
            return ethType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("ethType", Long.toHexString(ethType & 0xffff))
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
     * Implementation of VLAN priority criterion.
     */
    public static final class VlanPcpCriterion implements Criterion {
        private final Byte vlanPcp;

        /**
         * Constructor.
         *
         * @param vlanPcp the VLAN priority to match
         */
        public VlanPcpCriterion(Byte vlanPcp) {
            this.vlanPcp = vlanPcp;
        }

        @Override
        public Type type() {
            return Type.VLAN_PCP;
        }

        /**
         * Gets the VLAN priority to match.
         *
         * @return the VLAN priority to match
         */
        public Byte priority() {
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
     * Implementation of Internet Protocol Number criterion.
     */
    public static final class IPProtocolCriterion implements Criterion {
        private final Byte proto;

        /**
         * Constructor.
         *
         * @param protocol the IP protocol to match (e.g., TCP=6, UDP=17).
         */
        public IPProtocolCriterion(Byte protocol) {
            this.proto = protocol;
        }

        @Override
        public Type type() {
            return Type.IP_PROTO;
        }

        /**
         * Gets the IP protocol to match.
         *
         * @return the IP protocol to match
         */
        public Byte protocol() {
            return proto;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("protocol", Long.toHexString(proto & 0xff))
                    .toString();
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
     * Implementation of TCP port criterion.
     */
    public static final class TcpPortCriterion implements Criterion {
        private final Short tcpPort;
        private final Type type;

        /**
         * Constructor.
         *
         * @param tcpPort the TCP port to match
         * @param type the match type. Should be either Type.TCP_SRC or
         * Type.TCP_DST
         */
        public TcpPortCriterion(Short tcpPort, Type type) {
            this.tcpPort = tcpPort;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the TCP port to match.
         *
         * @return the TCP port to match
         */
        public Short tcpPort() {
            return this.tcpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("tcpPort", tcpPort & 0xffff).toString();
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
     * Implementation of UDP port criterion.
     */
    public static final class UdpPortCriterion implements Criterion {
        private final Short udpPort;
        private final Type type;

        /**
         * Constructor.
         *
         * @param udpPort the UDP port to match
         * @param type the match type. Should be either Type.UDP_SRC or
         * Type.UDP_DST
         */
        public UdpPortCriterion(Short udpPort, Type type) {
            this.udpPort = udpPort;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the UDP port to match.
         *
         * @return the UDP port to match
         */
        public Short udpPort() {
            return this.udpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("udpPort", udpPort & 0xffff).toString();
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
     * Implementation of SCTP port criterion.
     */
    public static final class SctpPortCriterion implements Criterion {
        private final Short sctpPort;
        private final Type type;

        /**
         * Constructor.
         *
         * @param sctpPort the SCTP port to match
         * @param type the match type. Should be either Type.SCTP_SRC or
         * Type.SCTP_DST
         */
        public SctpPortCriterion(Short sctpPort, Type type) {
            this.sctpPort = sctpPort;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the SCTP port to match.
         *
         * @return the SCTP port to match
         */
        public Short sctpPort() {
            return this.sctpPort;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("sctpPort", sctpPort & 0xffff).toString();
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
     * Implementation of ICMP type criterion.
     */
    public static final class IcmpTypeCriterion implements Criterion {
        private final Byte icmpType;

        /**
         * Constructor.
         *
         * @param icmpType the ICMP type to match
         */
        public IcmpTypeCriterion(Byte icmpType) {
            this.icmpType = icmpType;
        }

        @Override
        public Type type() {
            return Type.ICMPV4_TYPE;
        }

        /**
         * Gets the ICMP type to match.
         *
         * @return the ICMP type to match
         */
        public Byte icmpType() {
            return icmpType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("icmpType", icmpType & 0xff).toString();
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
     * Implementation of ICMP code criterion.
     */
    public static final class IcmpCodeCriterion implements Criterion {
        private final Byte icmpCode;

        /**
         * Constructor.
         *
         * @param icmpCode the ICMP code to match
         */
        public IcmpCodeCriterion(Byte icmpCode) {
            this.icmpCode = icmpCode;
        }

        @Override
        public Type type() {
            return Type.ICMPV4_CODE;
        }

        /**
         * Gets the ICMP code to match.
         *
         * @return the ICMP code to match
         */
        public Byte icmpCode() {
            return icmpCode;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("icmpCode", icmpCode & 0xff).toString();
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
     * Implementation of IPv6 Flow Label criterion (RFC 6437).
     */
    public static final class IPv6FlowLabelCriterion implements Criterion {
        private static final int FLOW_LABEL_MASK = 0xfffff;
        private final Integer flowLabel;        // IPv6 flow label: 20 bits

        /**
         * Constructor.
         *
         * @param flowLabel the IPv6 flow label to match
         */
        public IPv6FlowLabelCriterion(Integer flowLabel) {
            this.flowLabel = flowLabel & FLOW_LABEL_MASK;
        }

        @Override
        public Type type() {
            return Type.IPV6_FLABEL;
        }

        /**
         * Gets the IPv6 flow label to match.
         *
         * @return the IPv6 flow label to match
         */
        public Integer flowLabel() {
            return flowLabel;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("flowLabel", flowLabel).toString();
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
     * Implementation of ICMPv6 type criterion.
     */
    public static final class Icmpv6TypeCriterion implements Criterion {
        private final Byte icmpv6Type;

        /**
         * Constructor.
         *
         * @param icmpv6Type the ICMPv6 type to match
         */
        public Icmpv6TypeCriterion(Byte icmpv6Type) {
            this.icmpv6Type = icmpv6Type;
        }

        @Override
        public Type type() {
            return Type.ICMPV6_TYPE;
        }

        /**
         * Gets the ICMPv6 type to match.
         *
         * @return the ICMPv6 type to match
         */
        public Byte icmpv6Type() {
            return icmpv6Type;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("icmpv6Type", icmpv6Type & 0xff).toString();
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
     * Implementation of ICMPv6 code criterion.
     */
    public static final class Icmpv6CodeCriterion implements Criterion {
        private final Byte icmpv6Code;

        /**
         * Constructor.
         *
         * @param icmpv6Code the ICMPv6 code to match
         */
        public Icmpv6CodeCriterion(Byte icmpv6Code) {
            this.icmpv6Code = icmpv6Code;
        }

        @Override
        public Type type() {
            return Type.ICMPV6_CODE;
        }

        /**
         * Gets the ICMPv6 code to match.
         *
         * @return the ICMPv6 code to match
         */
        public Byte icmpv6Code() {
            return icmpv6Code;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("icmpv6Code", icmpv6Code & 0xff).toString();
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
     * Implementation of MPLS tag criterion.
     */
    public static final class MplsCriterion implements Criterion {
        private final Integer mplsLabel;

        /**
         * Constructor.
         *
         * @param mplsLabel the MPLS label to match
         */
        public MplsCriterion(Integer mplsLabel) {
            this.mplsLabel = mplsLabel;
        }

        @Override
        public Type type() {
            return Type.MPLS_LABEL;
        }

        /**
         * Gets the MPLS label to match.
         *
         * @return the MPLS label to match
         */
        public Integer label() {
            return mplsLabel;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("label", mplsLabel & 0xffffffffL).toString();
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
     * Implementation of lambda (wavelength) criterion.
     */
    public static final class LambdaCriterion implements Criterion {
        private final short lambda;
        private final Type type;

        /**
         * Constructor.
         *
         * @param lambda the lambda (wavelength) to match
         * @param type the match type. Should be Type.OCH_SIGID
         */
        public LambdaCriterion(short lambda, Type type) {
            this.lambda = lambda;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the lambda (wavelength) to match.
         *
         * @return the lambda (wavelength) to match.
         */
        public Short lambda() {
            return this.lambda;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("lambda", lambda & 0xffff).toString();
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
     * Implementation of optical signal type criterion.
     */
    public static final class OpticalSignalTypeCriterion implements Criterion {
        private final Short signalType;
        private final Type type;

        /**
         * Constructor.
         *
         * @param signalType the optical signal type to match
         * @param type the match type. Should be Type.OCH_SIGTYPE
         */
        public OpticalSignalTypeCriterion(Short signalType, Type type) {
            this.signalType = signalType;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        /**
         * Gets the optical signal type to match.
         *
         * @return the optical signal type to match
         */
        public Short signalType() {
            return this.signalType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("signalType", signalType & 0xffff).toString();
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
