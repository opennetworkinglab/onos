package org.projectfloodlight.openflow.protocol.match;

import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.ClassId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpEcn;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.LagId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBitMask128;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.UDF;
import org.projectfloodlight.openflow.types.VRF;
import org.projectfloodlight.openflow.types.VlanPcp;

@SuppressWarnings("unchecked")
public class MatchField<F extends OFValueType<F>> {
    private final String name;
    public final MatchFields id;
    private final Prerequisite<?>[] prerequisites;

    private MatchField(final String name, final MatchFields id, Prerequisite<?>... prerequisites) {
        this.name = name;
        this.id = id;
        this.prerequisites = prerequisites;
    }

    public final static MatchField<OFPort> IN_PORT =
            new MatchField<OFPort>("in_port", MatchFields.IN_PORT);

    public final static MatchField<OFPort> IN_PHY_PORT =
            new MatchField<OFPort>("in_phy_port", MatchFields.IN_PHY_PORT,
                    new Prerequisite<OFPort>(MatchField.IN_PORT));

    public final static MatchField<OFMetadata> METADATA =
            new MatchField<OFMetadata>("metadata", MatchFields.METADATA);

    public final static MatchField<MacAddress> ETH_DST =
            new MatchField<MacAddress>("eth_dst", MatchFields.ETH_DST);

    public final static MatchField<MacAddress> ETH_SRC =
            new MatchField<MacAddress>("eth_src", MatchFields.ETH_SRC);

    public final static MatchField<EthType> ETH_TYPE =
            new MatchField<EthType>("eth_type", MatchFields.ETH_TYPE);

    public final static MatchField<OFVlanVidMatch> VLAN_VID =
            new MatchField<OFVlanVidMatch>("vlan_vid", MatchFields.VLAN_VID);

    public final static MatchField<VlanPcp> VLAN_PCP =
            new MatchField<VlanPcp>("vlan_pcp", MatchFields.VLAN_PCP,
                    new Prerequisite<OFVlanVidMatch>(MatchField.VLAN_VID));

    public final static MatchField<IpDscp> IP_DSCP =
            new MatchField<IpDscp>("ip_dscp", MatchFields.IP_DSCP,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv4, EthType.IPv6));

    public final static MatchField<IpEcn> IP_ECN =
            new MatchField<IpEcn>("ip_ecn", MatchFields.IP_ECN,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv4, EthType.IPv6));

    public final static MatchField<IpProtocol> IP_PROTO =
            new MatchField<IpProtocol>("ip_proto", MatchFields.IP_PROTO,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv4, EthType.IPv6));

    public final static MatchField<IPv4Address> IPV4_SRC =
            new MatchField<IPv4Address>("ipv4_src", MatchFields.IPV4_SRC,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv4));

    public final static MatchField<IPv4Address> IPV4_DST =
            new MatchField<IPv4Address>("ipv4_dst", MatchFields.IPV4_DST,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv4));

    public final static MatchField<TransportPort> TCP_SRC = new MatchField<TransportPort>(
            "tcp_src", MatchFields.TCP_SRC,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.TCP));

    public final static MatchField<TransportPort> TCP_DST = new MatchField<TransportPort>(
            "tcp_dst", MatchFields.TCP_DST,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.TCP));

    public final static MatchField<TransportPort> UDP_SRC = new MatchField<TransportPort>(
            "udp_src", MatchFields.UDP_SRC,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.UDP));

    public final static MatchField<TransportPort> UDP_DST = new MatchField<TransportPort>(
            "udp_dst", MatchFields.UDP_DST,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.UDP));

    public final static MatchField<TransportPort> SCTP_SRC = new MatchField<TransportPort>(
            "sctp_src", MatchFields.SCTP_SRC,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.SCTP));

    public final static MatchField<TransportPort> SCTP_DST = new MatchField<TransportPort>(
            "sctp_dst", MatchFields.SCTP_DST,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.SCTP));

    public final static MatchField<ICMPv4Type> ICMPV4_TYPE = new MatchField<ICMPv4Type>(
            "icmpv4_type", MatchFields.ICMPV4_TYPE,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.ICMP));

    public final static MatchField<ICMPv4Code> ICMPV4_CODE = new MatchField<ICMPv4Code>(
            "icmpv4_code", MatchFields.ICMPV4_CODE,
            new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.ICMP));

    public final static MatchField<ArpOpcode> ARP_OP = new MatchField<ArpOpcode>(
            "arp_op", MatchFields.ARP_OP,
            new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.ARP));

    public final static MatchField<IPv4Address> ARP_SPA =
            new MatchField<IPv4Address>("arp_spa", MatchFields.ARP_SPA,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.ARP));

    public final static MatchField<IPv4Address> ARP_TPA =
            new MatchField<IPv4Address>("arp_tpa", MatchFields.ARP_TPA,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.ARP));

    public final static MatchField<MacAddress> ARP_SHA =
            new MatchField<MacAddress>("arp_sha", MatchFields.ARP_SHA,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.ARP));

    public final static MatchField<MacAddress> ARP_THA =
            new MatchField<MacAddress>("arp_tha", MatchFields.ARP_THA,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.ARP));

    public final static MatchField<IPv6Address> IPV6_SRC =
            new MatchField<IPv6Address>("ipv6_src", MatchFields.IPV6_SRC,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv6));

    public final static MatchField<IPv6Address> IPV6_DST =
            new MatchField<IPv6Address>("ipv6_dst", MatchFields.IPV6_DST,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv6));

    public final static MatchField<IPv6FlowLabel> IPV6_FLABEL =
            new MatchField<IPv6FlowLabel>("ipv6_flabel", MatchFields.IPV6_FLABEL,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.IPv6));

    public final static MatchField<U8> ICMPV6_TYPE =
            new MatchField<U8>("icmpv6_type", MatchFields.ICMPV6_TYPE,
                    new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.IPv6_ICMP));

    public final static MatchField<U8> ICMPV6_CODE =
            new MatchField<U8>("icmpv6_code", MatchFields.ICMPV6_CODE,
                    new Prerequisite<IpProtocol>(MatchField.IP_PROTO, IpProtocol.IPv6_ICMP));

    public final static MatchField<IPv6Address> IPV6_ND_TARGET =
            new MatchField<IPv6Address>("ipv6_nd_target", MatchFields.IPV6_ND_TARGET,
                    new Prerequisite<U8>(MatchField.ICMPV6_TYPE, U8.of((short)135), U8.of((short)136)));

    public final static MatchField<MacAddress> IPV6_ND_SLL =
            new MatchField<MacAddress>("ipv6_nd_sll", MatchFields.IPV6_ND_SLL,
                    new Prerequisite<U8>(MatchField.ICMPV6_TYPE, U8.of((short)135)));

    public final static MatchField<MacAddress> IPV6_ND_TLL =
            new MatchField<MacAddress>("ipv6_nd_tll", MatchFields.IPV6_ND_TLL,
                    new Prerequisite<U8>(MatchField.ICMPV6_TYPE, U8.of((short)136)));

    public final static MatchField<U32> MPLS_LABEL =
            new MatchField<U32>("mpls_label", MatchFields.MPLS_LABEL,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.MPLS_UNICAST, EthType.MPLS_MULTICAST));

    public final static MatchField<U8> MPLS_TC =
            new MatchField<U8>("mpls_tc", MatchFields.MPLS_TC,
                    new Prerequisite<EthType>(MatchField.ETH_TYPE, EthType.MPLS_UNICAST, EthType.MPLS_MULTICAST));

    public final static MatchField<U64> TUNNEL_ID = 
            new MatchField<U64>("tunnel_id", MatchFields.TUNNEL_ID);

    public final static MatchField<OFBitMask128> BSN_IN_PORTS_128 =
            new MatchField<OFBitMask128>("bsn_in_ports_128", MatchFields.BSN_IN_PORTS_128);

    public final static MatchField<LagId> BSN_LAG_ID =
            new MatchField<LagId>("bsn_lag_id", MatchFields.BSN_LAG_ID);

    public final static MatchField<VRF> BSN_VRF =
            new MatchField<VRF>("bsn_vrf", MatchFields.BSN_VRF);

    public final static MatchField<OFBooleanValue> BSN_GLOBAL_VRF_ALLOWED =
            new MatchField<OFBooleanValue>("bsn_global_vrf_allowed", MatchFields.BSN_GLOBAL_VRF_ALLOWED);

    public final static MatchField<ClassId> BSN_L3_INTERFACE_CLASS_ID =
            new MatchField<ClassId>("bsn_l3_interface_class_id", MatchFields.BSN_L3_INTERFACE_CLASS_ID);

    public final static MatchField<ClassId> BSN_L3_SRC_CLASS_ID =
            new MatchField<ClassId>("bsn_l3_src_class_id", MatchFields.BSN_L3_SRC_CLASS_ID);

    public final static MatchField<ClassId> BSN_L3_DST_CLASS_ID =
            new MatchField<ClassId>("bsn_l3_dst_class_id", MatchFields.BSN_L3_DST_CLASS_ID);

    public final static MatchField<ClassId> BSN_EGR_PORT_GROUP_ID =
            new MatchField<ClassId>("bsn_egr_port_group_id", MatchFields.BSN_EGR_PORT_GROUP_ID);

    public final static MatchField<UDF> BSN_UDF0 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF0);

    public final static MatchField<UDF> BSN_UDF1 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF1);

    public final static MatchField<UDF> BSN_UDF2 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF2);

    public final static MatchField<UDF> BSN_UDF3 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF3);

    public final static MatchField<UDF> BSN_UDF4 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF4);

    public final static MatchField<UDF> BSN_UDF5 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF5);

    public final static MatchField<UDF> BSN_UDF6 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF6);

    public final static MatchField<UDF> BSN_UDF7 =
            new MatchField<UDF>("bsn_udf", MatchFields.BSN_UDF7);

    public final static MatchField<U16> BSN_TCP_FLAGS =
            new MatchField<U16>("bsn_tcp_flags", MatchFields.BSN_TCP_FLAGS);

    public final static MatchField<ClassId> BSN_VLAN_XLATE_PORT_GROUP_ID =
            new MatchField<ClassId>("bsn_vlan_xlate_port_group_id", MatchFields.BSN_VLAN_XLATE_PORT_GROUP_ID);

    public String getName() {
        return name;
    }

    public boolean arePrerequisitesOK(Match match) {
        for (Prerequisite<?> p : this.prerequisites) {
            if (!p.isSatisfied(match)) {
                return false;
            }
        }
        return true;
    }

}
