/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.onlab.packet.EthType;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.OchSignalType;

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
     * Creates a match on ETH_TYPE field using the specified value.
     *
     * @param ethType eth type value
     * @return match criterion
     */
    public static Criterion matchEthType(EthType ethType) {
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
     * @param ipEcn ip ecn value (2 bits)
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
     * Creates a match on Tunnel ID.
     *
     * @param tunnelId Tunnel ID (64 bits)
     * @return match criterion
     */
    public static Criterion matchTunnelId(long tunnelId) {
        return new TunnelIdCriterion(tunnelId);
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
     * @deprecated in Cardinal Release. Use {@link #matchLambda(Lambda)} instead.
     */
    @Deprecated
    public static Criterion matchLambda(int lambda) {
        return new LambdaCriterion(lambda, Type.OCH_SIGID);
    }

    /**
     * Creates a match on lambda using the specified value.
     *
     * @param lambda lambda
     * @return match criterion
     */
    public static Criterion matchLambda(Lambda lambda) {
        if (lambda instanceof IndexedLambda) {
            return new IndexedLambdaCriterion((IndexedLambda) lambda);
        } else if (lambda instanceof OchSignal) {
            return new OchSignalCriterion((OchSignal) lambda);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported type of Lambda: %s", lambda));
        }
    }

    /**
     * Creates a match on optical signal type using the specified value.
     *
     * @param sigType optical signal type (8 bits unsigned integer)
     * @return match criterion
     * @deprecated in Cardinal Release
     */
    @Deprecated
    public static Criterion matchOpticalSignalType(short sigType) {
        return new OpticalSignalTypeCriterion(sigType, Type.OCH_SIGTYPE);
    }

    /**
     * Create a match on OCh (Optical Channel) signal type.
     *
     * @param signalType OCh signal type
     * @return match criterion
     */
    public static Criterion matchOchSignalType(OchSignalType signalType) {
        return new OchSignalTypeCriterion(signalType);
    }

    public static Criterion dummy() {
        return new DummyCriterion();
    }

    /**
     * Dummy Criterion used with @see{FilteringObjective}.
     */
    private static class DummyCriterion implements Criterion {

        @Override
        public Type type() {
            return Type.DUMMY;
        }
    }
}
