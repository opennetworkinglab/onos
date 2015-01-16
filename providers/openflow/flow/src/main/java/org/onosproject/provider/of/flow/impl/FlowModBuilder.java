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
package org.onosproject.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criteria.EthCriterion;
import org.onosproject.net.flow.criteria.Criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.Criteria.IPCriterion;
import org.onosproject.net.flow.criteria.Criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.Criteria.LambdaCriterion;
import org.onosproject.net.flow.criteria.Criteria.PortCriterion;
import org.onosproject.net.flow.criteria.Criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.Criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.Criteria.VlanPcpCriterion;
import org.onosproject.net.flow.criteria.Criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.Criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Builder for OpenFlow flow mods based on FlowRules.
 */
public abstract class FlowModBuilder {

    private final Logger log = getLogger(getClass());

    private final OFFactory factory;
    private final FlowRule flowRule;
    private final TrafficSelector selector;
    protected final Long xid;

    /**
     * Creates a new flow mod builder.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     * @return the new flow mod builder
     */
    public static FlowModBuilder builder(FlowRule flowRule,
                                         OFFactory factory, Optional<Long> xid) {
        switch (factory.getVersion()) {
        case OF_10:
            return new FlowModBuilderVer10(flowRule, factory, xid);
        case OF_13:
            return new FlowModBuilderVer13(flowRule, factory, xid);
        default:
            throw new UnsupportedOperationException(
                    "No flow mod builder for protocol version " + factory.getVersion());
        }
    }

    /**
     * Constructs a flow mod builder.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     */
    protected FlowModBuilder(FlowRule flowRule, OFFactory factory, Optional<Long> xid) {
        this.factory = factory;
        this.flowRule = flowRule;
        this.selector = flowRule.selector();
        this.xid = xid.orElse((long) 0);

    }

    /**
     * Builds an ADD flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowAdd buildFlowAdd();

    /**
     * Builds a MODIFY flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowMod buildFlowMod();

    /**
     * Builds a DELETE flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowDelete buildFlowDel();

    /**
     * Builds the match for the flow mod.
     *
     * @return the match
     */
    protected Match buildMatch() {
        Match.Builder mBuilder = factory.buildMatch();
        EthCriterion eth;
        IPCriterion ip;
        Ip4Prefix ip4Prefix;
        Ip6Prefix ip6Prefix;
        TcpPortCriterion tp;
        for (Criterion c : selector.criteria()) {
            switch (c.type()) {
            case IN_PORT:
                PortCriterion inport = (PortCriterion) c;
                mBuilder.setExact(MatchField.IN_PORT, OFPort.of((int) inport.port().toLong()));
                break;
            case ETH_SRC:
                eth = (EthCriterion) c;
                mBuilder.setExact(MatchField.ETH_SRC, MacAddress.of(eth.mac().toLong()));
                break;
            case ETH_DST:
                eth = (EthCriterion) c;
                mBuilder.setExact(MatchField.ETH_DST, MacAddress.of(eth.mac().toLong()));
                break;
            case ETH_TYPE:
                EthTypeCriterion ethType = (EthTypeCriterion) c;
                mBuilder.setExact(MatchField.ETH_TYPE, EthType.of(ethType.ethType()));
                break;
            case IPV4_DST:
                ip = (IPCriterion) c;
                ip4Prefix = ip.ip().getIp4Prefix();
                if (ip4Prefix.prefixLength() != Ip4Prefix.MAX_MASK_LENGTH) {
                    Ip4Address maskAddr =
                        Ip4Address.makeMaskPrefix(ip4Prefix.prefixLength());
                    Masked<IPv4Address> maskedIp =
                        Masked.of(IPv4Address.of(ip4Prefix.address().toInt()),
                                  IPv4Address.of(maskAddr.toInt()));
                    mBuilder.setMasked(MatchField.IPV4_DST, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_DST,
                                IPv4Address.of(ip4Prefix.address().toInt()));
                }
                break;
            case IPV4_SRC:
                ip = (IPCriterion) c;
                ip4Prefix = ip.ip().getIp4Prefix();
                if (ip4Prefix.prefixLength() != Ip4Prefix.MAX_MASK_LENGTH) {
                    Ip4Address maskAddr =
                        Ip4Address.makeMaskPrefix(ip4Prefix.prefixLength());
                    Masked<IPv4Address> maskedIp =
                        Masked.of(IPv4Address.of(ip4Prefix.address().toInt()),
                                  IPv4Address.of(maskAddr.toInt()));
                    mBuilder.setMasked(MatchField.IPV4_SRC, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_SRC,
                                IPv4Address.of(ip4Prefix.address().toInt()));
                }
                break;
            case IP_PROTO:
                IPProtocolCriterion p = (IPProtocolCriterion) c;
                mBuilder.setExact(MatchField.IP_PROTO, IpProtocol.of(p.protocol()));
                break;
            case VLAN_PCP:
                VlanPcpCriterion vpcp = (VlanPcpCriterion) c;
                mBuilder.setExact(MatchField.VLAN_PCP, VlanPcp.of(vpcp.priority()));
                break;
            case VLAN_VID:
                VlanIdCriterion vid = (VlanIdCriterion) c;
                mBuilder.setExact(MatchField.VLAN_VID,
                        OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vid.vlanId().toShort())));
                break;
            case TCP_DST:
                tp = (TcpPortCriterion) c;
                mBuilder.setExact(MatchField.TCP_DST, TransportPort.of(tp.tcpPort()));
                break;
            case TCP_SRC:
                tp = (TcpPortCriterion) c;
                mBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(tp.tcpPort()));
                break;
            case MPLS_LABEL:
                Criteria.MplsCriterion mp = (Criteria.MplsCriterion) c;
                mBuilder.setExact(MatchField.MPLS_LABEL,
                                  U32.of(mp.label().intValue()));
                break;
            case OCH_SIGID:
                LambdaCriterion lc = (LambdaCriterion) c;
                mBuilder.setExact(MatchField.OCH_SIGID,
                        new CircuitSignalID((byte) 1, (byte) 2, lc.lambda(), (short) 1));
                break;
            case OCH_SIGTYPE:
                Criteria.OpticalSignalTypeCriterion sc =
                        (Criteria.OpticalSignalTypeCriterion) c;
                mBuilder.setExact(MatchField.OCH_SIGTYPE,
                                  U8.of(sc.signalType()));
                break;
            case IPV6_DST:
                ip = (IPCriterion) c;
                ip6Prefix = ip.ip().getIp6Prefix();
                if (ip6Prefix.prefixLength() != Ip6Prefix.MAX_MASK_LENGTH) {
                    Ip6Address maskAddr =
                            Ip6Address.makeMaskPrefix(ip6Prefix.prefixLength());
                    Masked<IPv6Address> maskedIp =
                            Masked.of(IPv6Address.of(ip6Prefix.address().toString()),
                                    IPv6Address.of(maskAddr.toString()));
                    mBuilder.setMasked(MatchField.IPV6_DST, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV6_DST,
                            IPv6Address.of(ip6Prefix.address().toString()));
                }
                break;
            case IPV6_SRC:
                ip = (IPCriterion) c;
                ip6Prefix = ip.ip().getIp6Prefix();
                if (ip6Prefix.prefixLength() != Ip6Prefix.MAX_MASK_LENGTH) {
                    Ip6Address maskAddr =
                            Ip6Address.makeMaskPrefix(ip6Prefix.prefixLength());
                    Masked<IPv6Address> maskedIp =
                            Masked.of(IPv6Address.of(ip6Prefix.address().toString()),
                                    IPv6Address.of(maskAddr.toString()));
                    mBuilder.setMasked(MatchField.IPV6_SRC, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV6_SRC,
                            IPv6Address.of(ip6Prefix.address().toString()));
                }
                break;
            case ICMPV6_TYPE:
                Icmpv6TypeCriterion icmpv6type = (Icmpv6TypeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV6_TYPE,
                        U8.of(icmpv6type.icmpv6Type().byteValue()));
                break;
            case ICMPV6_CODE:
                Icmpv6CodeCriterion icmpv6code = (Icmpv6CodeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV6_CODE,
                        U8.of(icmpv6code.icmpv6Code().byteValue()));
                break;
            case ARP_OP:
            case ARP_SHA:
            case ARP_SPA:
            case ARP_THA:
            case ARP_TPA:
            case ICMPV4_CODE:
            case ICMPV4_TYPE:
            case IN_PHY_PORT:
            case IPV6_EXTHDR:
            case IPV6_FLABEL:
            case IPV6_ND_SLL:
            case IPV6_ND_TARGET:
            case IPV6_ND_TLL:
            case IP_DSCP:
            case IP_ECN:
            case METADATA:
            case MPLS_BOS:
            case MPLS_TC:
            case PBB_ISID:
            case SCTP_DST:
            case SCTP_SRC:
            case TUNNEL_ID:
            case UDP_DST:
            case UDP_SRC:
            default:
                log.warn("Match type {} not yet implemented.", c.type());
            }
        }
        return mBuilder.build();
    }

    /**
     * Returns the flow rule for this builder.
     *
     * @return the flow rule
     */
    protected FlowRule flowRule() {
        return flowRule;
    }

    /**
     * Returns the factory used for building OpenFlow constructs.
     *
     * @return the factory
     */
    protected OFFactory factory() {
        return factory;
    }

}
