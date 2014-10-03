package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRule.FlowRuleState;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowRemovedReason;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanPcp;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanVid;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

public class FlowRuleBuilder {
    private final Logger log = getLogger(getClass());

    private final OFFlowStatsEntry stat;
    private final OFFlowRemoved removed;

    private final Match match;
    private final List<OFAction> actions;

    private final Dpid dpid;

    private final boolean addedRule;


    public FlowRuleBuilder(Dpid dpid, OFFlowStatsEntry entry) {
        this.stat = entry;
        this.match = entry.getMatch();
        this.actions = getActions(entry);
        this.dpid = dpid;
        this.removed = null;
        this.addedRule = true;
    }

    public FlowRuleBuilder(Dpid dpid, OFFlowRemoved removed) {
        this.match = removed.getMatch();
        this.removed = removed;

        this.dpid = dpid;
        this.actions = null;
        this.stat = null;
        this.addedRule = false;

    }

    public FlowRule build() {
        if (addedRule) {
            return new DefaultFlowRule(DeviceId.deviceId(Dpid.uri(dpid)),
                    buildSelector(), buildTreatment(), stat.getPriority(),
                    FlowRuleState.ADDED, stat.getDurationNsec() / 1000000,
                    stat.getPacketCount().getValue(), stat.getByteCount().getValue(),
                    stat.getCookie().getValue(), false, stat.getIdleTimeout());
        } else {
            // TODO: revisit potentially.
            return new DefaultFlowRule(DeviceId.deviceId(Dpid.uri(dpid)),
                    buildSelector(), null, removed.getPriority(),
                    FlowRuleState.REMOVED, removed.getDurationNsec() / 1000000,
                    removed.getPacketCount().getValue(), removed.getByteCount().getValue(),
                    removed.getCookie().getValue(),
                    removed.getReason() == OFFlowRemovedReason.IDLE_TIMEOUT.ordinal(),
                    stat.getIdleTimeout());
        }
    }

    private List<OFAction> getActions(OFFlowStatsEntry entry) {
        switch (entry.getVersion()) {
            case OF_10:
                return entry.getActions();
            case OF_11:
            case OF_12:
            case OF_13:
                List<OFInstruction> ins = entry.getInstructions();
                for (OFInstruction in : ins) {
                    if (in.getType().equals(OFInstructionType.APPLY_ACTIONS)) {
                        OFInstructionApplyActions apply = (OFInstructionApplyActions) in;
                        return apply.getActions();
                    }
                }
                return Lists.newLinkedList();
            default:
                log.warn("Unknown OF version {}", entry.getVersion());
        }
        return Lists.newLinkedList();
    }

    private TrafficTreatment buildTreatment() {
        TrafficTreatment.Builder builder = new DefaultTrafficTreatment.Builder();
        // If this is a drop rule
        if (actions.size() == 0) {
            builder.drop();
            return builder.build();
        }
        for (OFAction act : actions) {
            switch (act.getType()) {
            case OUTPUT:
                OFActionOutput out = (OFActionOutput) act;
                builder.setOutput(
                        PortNumber.portNumber(out.getPort().getPortNumber()));
                break;
            case SET_VLAN_VID:
                OFActionSetVlanVid vlan = (OFActionSetVlanVid) act;
                builder.setVlanId(VlanId.vlanId(vlan.getVlanVid().getVlan()));
                break;
            case SET_VLAN_PCP:
                OFActionSetVlanPcp pcp = (OFActionSetVlanPcp) act;
                builder.setVlanId(VlanId.vlanId(pcp.getVlanPcp().getValue()));
                break;
            case SET_DL_DST:
                OFActionSetDlDst dldst = (OFActionSetDlDst) act;
                builder.setEthDst(
                        MacAddress.valueOf(dldst.getDlAddr().getLong()));
                break;
            case SET_DL_SRC:
                OFActionSetDlSrc dlsrc = (OFActionSetDlSrc) act;
                builder.setEthSrc(
                        MacAddress.valueOf(dlsrc.getDlAddr().getLong()));

                break;
            case SET_NW_DST:
                OFActionSetNwDst nwdst = (OFActionSetNwDst) act;
                IPv4Address di = nwdst.getNwAddr();
                if (di.isCidrMask()) {
                    builder.setIpDst(IpPrefix.valueOf(di.getInt(),
                            di.asCidrMaskLength()));
                } else {
                    builder.setIpDst(IpPrefix.valueOf(di.getInt()));
                }
                break;
            case SET_NW_SRC:
                OFActionSetNwSrc nwsrc = (OFActionSetNwSrc) act;
                IPv4Address si = nwsrc.getNwAddr();
                if (si.isCidrMask()) {
                    builder.setIpSrc(IpPrefix.valueOf(si.getInt(),
                            si.asCidrMaskLength()));
                } else {
                    builder.setIpSrc(IpPrefix.valueOf(si.getInt()));
                }
                break;
            case SET_TP_DST:
            case SET_TP_SRC:
            case POP_MPLS:
            case POP_PBB:
            case POP_VLAN:
            case PUSH_MPLS:
            case PUSH_PBB:
            case PUSH_VLAN:
            case SET_FIELD:
            case SET_MPLS_LABEL:
            case SET_MPLS_TC:
            case SET_MPLS_TTL:
            case SET_NW_ECN:
            case SET_NW_TOS:
            case SET_NW_TTL:
            case SET_QUEUE:
            case STRIP_VLAN:
            case COPY_TTL_IN:
            case COPY_TTL_OUT:
            case DEC_MPLS_TTL:
            case DEC_NW_TTL:
            case ENQUEUE:
            case EXPERIMENTER:
            case GROUP:
            default:
                log.warn("Action type {} not yet implemented.", act.getType());
            }
        }

        return builder.build();
    }

    private TrafficSelector buildSelector() {
        TrafficSelector.Builder builder = new DefaultTrafficSelector.Builder();
        for (MatchField<?> field : match.getMatchFields()) {
            switch (field.id) {
            case IN_PORT:
                builder.matchInport(PortNumber
                        .portNumber(match.get(MatchField.IN_PORT).getPortNumber()));
                break;
            case ETH_SRC:
                MacAddress sMac = MacAddress.valueOf(match.get(MatchField.ETH_SRC).getLong());
                builder.matchEthSrc(sMac);
                break;
            case ETH_DST:
                MacAddress dMac = MacAddress.valueOf(match.get(MatchField.ETH_DST).getLong());
                builder.matchEthDst(dMac);
                break;
            case ETH_TYPE:
                int ethType = match.get(MatchField.ETH_TYPE).getValue();
                builder.matchEthType((short) ethType);
                break;
            case IPV4_DST:
                IPv4Address di = match.get(MatchField.IPV4_DST);
                IpPrefix dip;
                if (di.isCidrMask()) {
                    dip = IpPrefix.valueOf(di.getInt(), di.asCidrMaskLength());
                } else {
                    dip = IpPrefix.valueOf(di.getInt());
                }
                builder.matchIPDst(dip);
                break;
            case IPV4_SRC:
                IPv4Address si = match.get(MatchField.IPV4_SRC);
                IpPrefix sip;
                if (si.isCidrMask()) {
                    sip = IpPrefix.valueOf(si.getInt(), si.asCidrMaskLength());
                } else {
                    sip = IpPrefix.valueOf(si.getInt());
                }
                builder.matchIPSrc(sip);
                break;
            case IP_PROTO:
                short proto = match.get(MatchField.IP_PROTO).getIpProtocolNumber();
                builder.matchIPProtocol((byte) proto);
                break;
            case VLAN_PCP:
                byte vlanPcp = match.get(MatchField.VLAN_PCP).getValue();
                builder.matchVlanPcp(vlanPcp);
                break;
            case VLAN_VID:
                VlanId vlanId = VlanId.vlanId(match.get(MatchField.VLAN_VID).getVlan());
                builder.matchVlanId(vlanId);
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
            case IPV6_FLABEL:
            case IPV6_ND_SLL:
            case IPV6_ND_TARGET:
            case IPV6_ND_TLL:
            case IPV6_SRC:
            case IP_DSCP:
            case IP_ECN:
            case METADATA:
            case MPLS_LABEL:
            case MPLS_TC:
            case SCTP_DST:
            case SCTP_SRC:
            case TCP_DST:
            case TCP_SRC:
            case TUNNEL_ID:
            case UDP_DST:
            case UDP_SRC:
            default:
                log.warn("Match type {} not yet implemented.", field.id);


            }
        }
        return builder.build();
    }

}
