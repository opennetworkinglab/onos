package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.criteria.Criteria.EthCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.EthTypeCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.IPProtocolCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.PortCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanIdCriterion;
import org.onlab.onos.net.flow.criteria.Criteria.VlanPcpCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onlab.onos.net.flow.instructions.L3ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowSwitch;
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
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private FlowRuleProviderService providerService;

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }
    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            applyRule(flowRules[i]);
        }

    }

    private void applyRule(FlowRule flowRule) {
        OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
        Match match = buildMatch(flowRule.selector().criteria(), sw.factory());
        List<OFAction> actions =
                buildActions(flowRule.treatment().instructions(), sw.factory());

        //TODO: what to do without bufferid? do we assume that there will be a pktout as well?
        OFFlowMod fm = sw.factory().buildFlowModify()
                .setActions(actions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setIdleTimeout(10)
                .setHardTimeout(10)
                .setPriority(flowRule.priority())
                .build();

        sw.sendMsg(fm);

    }

    private List<OFAction> buildActions(List<Instruction> instructions, OFFactory factory) {
        List<OFAction> acts = new LinkedList<>();
        for (Instruction i : instructions) {
            switch (i.type()) {
            case DROP:
                log.warn("Saw drop action; assigning drop action");
                return acts;
            case L2MODIFICATION:
                acts.add(buildL2Modification(i, factory));
            case L3MODIFICATION:
                acts.add(buildL3Modification(i, factory));
            case OUTPUT:
                break;
            case GROUP:
            default:
                log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }

        return acts;
    }

    private OFAction buildL3Modification(Instruction i, OFFactory factory) {
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

    private OFAction buildL2Modification(Instruction i, OFFactory factory) {
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

    private Match buildMatch(List<Criterion> criteria, OFFactory factory) {
        Match.Builder mBuilder = factory.buildMatch();
        EthCriterion eth;
        IPCriterion ip;
        for (Criterion c : criteria) {
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
            case IPV4_DST:
                ip = (IPCriterion) c;
                mBuilder.setExact(MatchField.IPV4_DST, IPv4Address.of(ip.ip().toString()));
                break;
            case IPV4_SRC:
                ip = (IPCriterion) c;
                mBuilder.setExact(MatchField.IPV4_SRC, IPv4Address.of(ip.ip().toString()));
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
                log.warn("Action type {} not yet implemented.", c.type());
            }
        }
        return mBuilder.build();
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }

    @Override
    public Iterable<FlowEntry> getFlowMetrics(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }


    //TODO: InternalFlowRuleProvider listening to stats and error and flowremoved.
    // possibly barriers as well. May not be internal at all...


}
