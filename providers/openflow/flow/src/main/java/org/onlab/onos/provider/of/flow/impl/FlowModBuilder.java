package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.criteria.Criteria.EthCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.EthTypeCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPProtocolCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.LambdaCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.PortCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.TcpPortCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanIdCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanPcpCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

/**
 * Builder for OpenFlow flow mods based on FlowRules.
 */
public abstract class FlowModBuilder {

    private final Logger log = getLogger(getClass());

    private final OFFactory factory;
    private final FlowRule flowRule;
    private final TrafficSelector selector;

    /**
     * Creates a new flow mod builder.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @return the new flow mod builder
     */
    public static FlowModBuilder builder(FlowRule flowRule, OFFactory factory) {
        switch (factory.getVersion()) {
        case OF_10:
            return new FlowModBuilderVer10(flowRule, factory);
        case OF_13:
            return new FlowModBuilderVer13(flowRule, factory);
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
     */
    protected FlowModBuilder(FlowRule flowRule, OFFactory factory) {
        this.factory = factory;
        this.flowRule = flowRule;
        this.selector = flowRule.selector();
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
                if (ip.ip().isMasked()) {
                    Masked<IPv4Address> maskedIp = Masked.of(IPv4Address.of(ip.ip().toInt()),
                            IPv4Address.of(ip.ip().netmask().toInt()));
                    mBuilder.setMasked(MatchField.IPV4_DST, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_DST, IPv4Address.of(ip.ip().toInt()));
                }
                break;
            case IPV4_SRC:
                ip = (IPCriterion) c;
                if (ip.ip().isMasked()) {
                    Masked<IPv4Address> maskedIp = Masked.of(IPv4Address.of(ip.ip().toInt()),
                            IPv4Address.of(ip.ip().netmask().toInt()));
                    mBuilder.setMasked(MatchField.IPV4_SRC, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_SRC, IPv4Address.of(ip.ip().toInt()));
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
            case OCH_SIGID:
                LambdaCriterion lc = (LambdaCriterion) c;
                mBuilder.setExact(MatchField.OCH_SIGID,
                        new CircuitSignalID((byte) 1, (byte) 2, lc.lambda(), (short) 1));
                break;
            case ARP_OP:
            case ARP_SHA:
            case ARP_SPA:
            case ARP_THA:
            case ARP_TPA:
            case ICMPV4_CODE:
            case ICMPV4_TYPE:
            case ICMPV6_CODE:
            case ICMPV6_TYPE:
            case IN_PHY_PORT:
            case IPV6_DST:
            case IPV6_EXTHDR:
            case IPV6_FLABEL:
            case IPV6_ND_SLL:
            case IPV6_ND_TARGET:
            case IPV6_ND_TLL:
            case IPV6_SRC:
            case IP_DSCP:
            case IP_ECN:
            case METADATA:
            case MPLS_BOS:
            case MPLS_LABEL:
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
