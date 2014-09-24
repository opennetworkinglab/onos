package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.flow.FlowId;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criteria.EthCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.EthTypeCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPProtocolCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.PortCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanIdCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanPcpCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onlab.onos.net.flow.instructions.L3ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;


public class FlowModBuilder {

    private final Logger log = getLogger(getClass());

    private final OFFactory factory;
    private final TrafficTreatment treatment;
    private final TrafficSelector selector;

    private final int priority;

    private final FlowId cookie;



    public FlowModBuilder(FlowRule flowRule, OFFactory factory) {
        this.factory = factory;
        this.treatment = flowRule.treatment();
        this.selector = flowRule.selector();
        this.priority = flowRule.priority();
        this.cookie = flowRule.id();
    }

    public OFFlowMod buildFlowMod() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();

        //TODO: what to do without bufferid? do we assume that there will be a pktout as well?
        OFFlowMod fm = factory.buildFlowModify()
                .setCookie(U64.of(cookie.value()))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(priority)
                .build();

        return fm;

    }

    public OFFlowMod buildFlowDel() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();

        OFFlowMod fm = factory.buildFlowDelete()
                .setCookie(U64.of(cookie.value()))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(priority)
                .build();

        return fm;
    }

    private List<OFAction> buildActions() {
        List<OFAction> acts = new LinkedList<>();
        for (Instruction i : treatment.instructions()) {
            switch (i.type()) {
            case DROP:
                log.warn("Saw drop action; assigning drop action");
                return new LinkedList<>();
            case L2MODIFICATION:
                acts.add(buildL2Modification(i));
                break;
            case L3MODIFICATION:
                acts.add(buildL3Modification(i));
                break;
            case OUTPUT:
                OutputInstruction out = (OutputInstruction) i;
                acts.add(factory.actions().buildOutput().setPort(
                        OFPort.of((int) out.port().toLong())).build());
                break;
            case GROUP:
            default:
                log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }

        return acts;
    }

    private OFAction buildL3Modification(Instruction i) {
        L3ModificationInstruction l3m = (L3ModificationInstruction) i;
        ModIPInstruction ip;
        switch (l3m.subtype()) {
        case L3_DST:
            ip = (ModIPInstruction) i;
            return factory.actions().setNwDst(IPv4Address.of(ip.ip().toInt()));
        case L3_SRC:
            ip = (ModIPInstruction) i;
            return factory.actions().setNwSrc(IPv4Address.of(ip.ip().toInt()));
        default:
            log.warn("Unimplemented action type {}.", l3m.subtype());
            break;
        }
        return null;
    }

    private OFAction buildL2Modification(Instruction i) {
        L2ModificationInstruction l2m = (L2ModificationInstruction) i;
        ModEtherInstruction eth;
        switch (l2m.subtype()) {
        case L2_DST:
            eth = (ModEtherInstruction) l2m;
            return factory.actions().setDlDst(MacAddress.of(eth.mac().toLong()));
        case L2_SRC:
            eth = (ModEtherInstruction) l2m;
            return factory.actions().setDlSrc(MacAddress.of(eth.mac().toLong()));
        case VLAN_ID:
            ModVlanIdInstruction vlanId = (ModVlanIdInstruction) l2m;
            return factory.actions().setVlanVid(VlanVid.ofVlan(vlanId.vlanId.toShort()));
        case VLAN_PCP:
            ModVlanPcpInstruction vlanPcp = (ModVlanPcpInstruction) l2m;
            return factory.actions().setVlanPcp(VlanPcp.of(vlanPcp.vlanPcp()));
        default:
            log.warn("Unimplemented action type {}.", l2m.subtype());
            break;
        }
        return null;
    }

    private Match buildMatch() {
        Match.Builder mBuilder = factory.buildMatch();
        EthCriterion eth;
        IPCriterion ip;
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
            case TCP_DST:
            case TCP_SRC:
            case TUNNEL_ID:
            case UDP_DST:
            case UDP_SRC:
            default:
                log.warn("Match type {} not yet implemented.", c.type());
            }
        }
        return mBuilder.build();
    }



}
