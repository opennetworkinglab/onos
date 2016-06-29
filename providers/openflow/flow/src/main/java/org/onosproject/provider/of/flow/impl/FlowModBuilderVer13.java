/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.Lists;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.SetQueueInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModOchSignalInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction.ModOduSignalIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsBosInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsHeaderInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanHeaderInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpEthInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpOpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction.ModTransportPortInstruction;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.onosproject.provider.of.flow.util.NoMappingFoundException;
import org.onosproject.provider.of.flow.util.OpenFlowValueMapper;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.OduSignalID;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Flow mod builder for OpenFlow 1.3+.
 */
public class FlowModBuilderVer13 extends FlowModBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final int OFPCML_NO_BUFFER = 0xffff;

    private final TrafficTreatment treatment;

    /**
     * Constructor for a flow mod builder for OpenFlow 1.3.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     * @param driverService the device driver service
     */
    protected FlowModBuilderVer13(FlowRule flowRule, OFFactory factory, Optional<Long> xid,
                                  Optional<DriverService> driverService) {
        super(flowRule, factory, xid, driverService);

        this.treatment = flowRule.treatment();
    }

    @Override
    public OFFlowMod buildFlowAdd() {
        Match match = buildMatch();
        List<OFAction> deferredActions = buildActions(treatment.deferred());
        List<OFAction> immediateActions = buildActions(treatment.immediate());
        List<OFInstruction> instructions = Lists.newLinkedList();


        if (treatment.clearedDeferred()) {
            instructions.add(factory().instructions().clearActions());
        }
        if (immediateActions.size() > 0) {
            instructions.add(factory().instructions().applyActions(immediateActions));
        }
        if (deferredActions.size() > 0) {
            instructions.add(factory().instructions().writeActions(deferredActions));
        }
        if (treatment.tableTransition() != null) {
            instructions.add(buildTableGoto(treatment.tableTransition()));
        }
        if (treatment.writeMetadata() != null) {
            instructions.add(buildMetadata(treatment.writeMetadata()));
        }
        if (treatment.metered() != null) {
            instructions.add(buildMeter(treatment.metered()));
        }

        long cookie = flowRule().id().value();

        OFFlowAdd fm = factory().buildFlowAdd()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInstructions(instructions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .setTableId(TableId.of(flowRule().tableId()))
                .setHardTimeout(flowRule().hardTimeout())
                .build();

        return fm;
    }

    @Override
    public OFFlowMod buildFlowMod() {
        Match match = buildMatch();
        List<OFAction> deferredActions = buildActions(treatment.deferred());
        List<OFAction> immediateActions = buildActions(treatment.immediate());
        List<OFInstruction> instructions = Lists.newLinkedList();


        if (immediateActions.size() > 0) {
            instructions.add(factory().instructions().applyActions(immediateActions));
        }
        if (treatment.clearedDeferred()) {
            instructions.add(factory().instructions().clearActions());
        }
        if (deferredActions.size() > 0) {
            instructions.add(factory().instructions().writeActions(deferredActions));
        }
        if (treatment.tableTransition() != null) {
            instructions.add(buildTableGoto(treatment.tableTransition()));
        }
        if (treatment.writeMetadata() != null) {
            instructions.add(buildMetadata(treatment.writeMetadata()));
        }
        if (treatment.metered() != null) {
            instructions.add(buildMeter(treatment.metered()));
        }

        long cookie = flowRule().id().value();

        OFFlowMod fm = factory().buildFlowModify()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInstructions(instructions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .setTableId(TableId.of(flowRule().tableId()))
                .setHardTimeout(flowRule().hardTimeout())
                .build();

        return fm;
    }

    @Override
    public OFFlowMod buildFlowDel() {
        Match match = buildMatch();

        long cookie = flowRule().id().value();

        OFFlowDeleteStrict fm = factory().buildFlowDeleteStrict()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .setTableId(TableId.of(flowRule().tableId()))
                .setHardTimeout(flowRule().hardTimeout())
                .build();

        return fm;
    }

    private List<OFAction> buildActions(List<Instruction> treatments) {
        if (treatment == null) {
            return Collections.emptyList();
        }

        boolean tableFound = false;
        List<OFAction> actions = new LinkedList<>();
        for (Instruction i : treatments) {
            switch (i.type()) {
                case NOACTION:
                    return Collections.emptyList();
                case L0MODIFICATION:
                    actions.add(buildL0Modification(i));
                    break;
                case L1MODIFICATION:
                    actions.add(buildL1Modification(i));
                    break;
                case L2MODIFICATION:
                    actions.add(buildL2Modification(i));
                    break;
                case L3MODIFICATION:
                    actions.add(buildL3Modification(i));
                    break;
                case L4MODIFICATION:
                    actions.add(buildL4Modification(i));
                    break;
                case OUTPUT:
                    OutputInstruction out = (OutputInstruction) i;
                    OFActionOutput.Builder action = factory().actions().buildOutput()
                            .setPort(OFPort.of((int) out.port().toLong()));
                    if (out.port().equals(PortNumber.CONTROLLER)) {
                        action.setMaxLen(OFPCML_NO_BUFFER);
                    }
                    actions.add(action.build());
                    break;
                case GROUP:
                    GroupInstruction group = (GroupInstruction) i;
                    OFActionGroup.Builder groupBuilder = factory().actions().buildGroup()
                            .setGroup(OFGroup.of(group.groupId().id()));
                    actions.add(groupBuilder.build());
                    break;
                case QUEUE:
                    SetQueueInstruction queue = (SetQueueInstruction) i;
                    OFActionSetQueue.Builder queueBuilder = factory().actions().buildSetQueue()
                            .setQueueId(queue.queueId());
                    actions.add(queueBuilder.build());
                    break;
                case TABLE:
                    //FIXME: should not occur here.
                    tableFound = true;
                    break;
                case EXTENSION:
                    actions.add(buildExtensionAction(((Instructions.ExtensionInstructionWrapper) i)
                            .extensionInstruction()));
                    break;
                default:
                    log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }
        if (tableFound && actions.isEmpty()) {
            // handles the case where there are no actions, but there is
            // a goto instruction for the next table
            return Collections.emptyList();
        }
        return actions;
    }

    private OFInstruction buildTableGoto(Instructions.TableTypeTransition i) {
        OFInstruction instruction = factory().instructions().gotoTable(
                TableId.of(i.tableId()));
        return instruction;
    }

    private OFInstruction buildMetadata(Instructions.MetadataInstruction m) {
        OFInstruction instruction = factory().instructions().writeMetadata(
                U64.of(m.metadata()), U64.of(m.metadataMask()));
        return instruction;
    }

    private OFInstruction buildMeter(Instructions.MeterInstruction metered) {
        return factory().instructions().meter(metered.meterId().id());
    }


    private OFAction buildL0Modification(Instruction i) {
        L0ModificationInstruction l0m = (L0ModificationInstruction) i;
        OFOxm<?> oxm = null;
        switch (l0m.subtype()) {
            case OCH:
                try {
                    ModOchSignalInstruction modOchSignalInstruction = (ModOchSignalInstruction) l0m;
                    OchSignal signal = modOchSignalInstruction.lambda();
                    byte gridType = OpenFlowValueMapper.lookupGridType(signal.gridType());
                    byte channelSpacing = OpenFlowValueMapper.lookupChannelSpacing(signal.channelSpacing());
                    oxm = factory().oxms().expOchSigId(
                            new CircuitSignalID(gridType, channelSpacing,
                                    (short) signal.spacingMultiplier(), (short) signal.slotGranularity()));
                } catch (NoMappingFoundException e) {
                    log.warn(e.getMessage());
                    break;
                }
                break;
            default:
                log.warn("Unimplemented action type {}.", l0m.subtype());
                break;
        }
        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildModOchSignalInstruction(ModOchSignalInstruction instruction) {
        OchSignal signal = instruction.lambda();
        byte gridType = OpenFlowValueMapper.lookupGridType(signal.gridType());
        byte channelSpacing = OpenFlowValueMapper.lookupChannelSpacing(signal.channelSpacing());

        return factory().actions().circuit(factory().oxms().expOchSigId(
                new CircuitSignalID(gridType, channelSpacing,
                        (short) signal.spacingMultiplier(), (short) signal.slotGranularity())
        ));
    }

    private OFAction buildL1Modification(Instruction i) {
        L1ModificationInstruction l1m = (L1ModificationInstruction) i;
        OFOxm<?> oxm = null;
        switch (l1m.subtype()) {
        case ODU_SIGID:
            ModOduSignalIdInstruction modOduSignalIdInstruction = (ModOduSignalIdInstruction) l1m;
            OduSignalId oduSignalId = modOduSignalIdInstruction.oduSignalId();

            OduSignalID oduSignalID = new OduSignalID((short) oduSignalId.tributaryPortNumber(),
                    (short) oduSignalId.tributarySlotLength(),
                    oduSignalId.tributarySlotBitmap());

            oxm = factory().oxms().expOduSigId(oduSignalID);
            break;
        default:
            log.warn("Unimplemented action type {}.", l1m.subtype());
            break;
        }

        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildL2Modification(Instruction i) {
        L2ModificationInstruction l2m = (L2ModificationInstruction) i;
        ModEtherInstruction eth;
        OFOxm<?> oxm = null;
        switch (l2m.subtype()) {
            case ETH_DST:
                eth = (ModEtherInstruction) l2m;
                oxm = factory().oxms().ethDst(MacAddress.of(eth.mac().toLong()));
                break;
            case ETH_SRC:
                eth = (ModEtherInstruction) l2m;
                oxm = factory().oxms().ethSrc(MacAddress.of(eth.mac().toLong()));
                break;
            case VLAN_ID:
                ModVlanIdInstruction vlanId = (ModVlanIdInstruction) l2m;
                oxm = factory().oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanId.vlanId().toShort()));
                break;
            case VLAN_PCP:
                ModVlanPcpInstruction vlanPcp = (ModVlanPcpInstruction) l2m;
                oxm = factory().oxms().vlanPcp(VlanPcp.of(vlanPcp.vlanPcp()));
                break;
            case MPLS_PUSH:
                ModMplsHeaderInstruction pushHeaderInstructions =
                        (ModMplsHeaderInstruction) l2m;
                return factory().actions().pushMpls(EthType.of(pushHeaderInstructions
                                                               .ethernetType().toShort()));
            case MPLS_POP:
                ModMplsHeaderInstruction popHeaderInstructions =
                        (ModMplsHeaderInstruction) l2m;
                return factory().actions().popMpls(EthType.of(popHeaderInstructions
                                                              .ethernetType().toShort()));
            case MPLS_LABEL:
                ModMplsLabelInstruction mplsLabel =
                        (ModMplsLabelInstruction) l2m;
                oxm = factory().oxms().mplsLabel(U32.of(mplsLabel.label().toInt()));
                break;
            case MPLS_BOS:
                ModMplsBosInstruction mplsBos = (ModMplsBosInstruction) l2m;
                oxm = factory().oxms()
                        .mplsBos(mplsBos.mplsBos() ? OFBooleanValue.TRUE
                                                   : OFBooleanValue.FALSE);
                break;
            case DEC_MPLS_TTL:
                return factory().actions().decMplsTtl();
            case VLAN_POP:
                return factory().actions().popVlan();
            case VLAN_PUSH:
                ModVlanHeaderInstruction pushVlanInstruction = (ModVlanHeaderInstruction) l2m;
                return factory().actions().pushVlan(
                        EthType.of(pushVlanInstruction.ethernetType().toShort()));
            case TUNNEL_ID:
                ModTunnelIdInstruction tunnelId = (ModTunnelIdInstruction) l2m;
                oxm = factory().oxms().tunnelId(U64.of(tunnelId.tunnelId()));
                break;
            default:
                log.warn("Unimplemented action type {}.", l2m.subtype());
                break;
        }

        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildL3Modification(Instruction i) {
        L3ModificationInstruction l3m = (L3ModificationInstruction) i;
        ModIPInstruction ip;
        Ip4Address ip4;
        Ip6Address ip6;
        OFOxm<?> oxm = null;
        switch (l3m.subtype()) {
            case IPV4_SRC:
                ip = (ModIPInstruction) i;
                ip4 = ip.ip().getIp4Address();
                oxm = factory().oxms().ipv4Src(IPv4Address.of(ip4.toInt()));
                break;
            case IPV4_DST:
                ip = (ModIPInstruction) i;
                ip4 = ip.ip().getIp4Address();
                oxm = factory().oxms().ipv4Dst(IPv4Address.of(ip4.toInt()));
                break;
            case IPV6_SRC:
                ip = (ModIPInstruction) i;
                ip6 = ip.ip().getIp6Address();
                oxm = factory().oxms().ipv6Src(IPv6Address.of(ip6.toOctets()));
                break;
            case IPV6_DST:
                ip = (ModIPInstruction) i;
                ip6 = ip.ip().getIp6Address();
                oxm = factory().oxms().ipv6Dst(IPv6Address.of(ip6.toOctets()));
                break;
            case IPV6_FLABEL:
                ModIPv6FlowLabelInstruction flowLabelInstruction =
                        (ModIPv6FlowLabelInstruction) i;
                int flowLabel = flowLabelInstruction.flowLabel();
                oxm = factory().oxms().ipv6Flabel(IPv6FlowLabel.of(flowLabel));
                break;
            case ARP_SPA:
                ModArpIPInstruction aip = (ModArpIPInstruction) i;
                ip4 = aip.ip().getIp4Address();
                oxm = factory().oxms().arpSpa(IPv4Address.of(ip4.toInt()));
                break;
            case ARP_SHA:
                ModArpEthInstruction ei = (ModArpEthInstruction) i;
                oxm = factory().oxms().arpSha(MacAddress.of(ei.mac().toLong()));
                break;
            case ARP_OP:
                ModArpOpInstruction oi = (ModArpOpInstruction) i;
                oxm = factory().oxms().arpOp(ArpOpcode.of((int) oi.op()));
                break;
            case DEC_TTL:
                return factory().actions().decNwTtl();
            case TTL_IN:
                return factory().actions().copyTtlIn();
            case TTL_OUT:
                return factory().actions().copyTtlOut();
            default:
                log.warn("Unimplemented action type {}.", l3m.subtype());
                break;
        }

        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildL4Modification(Instruction i) {
        L4ModificationInstruction l4m = (L4ModificationInstruction) i;
        ModTransportPortInstruction tp;
        OFOxm<?> oxm = null;
        switch (l4m.subtype()) {
            case TCP_SRC:
                tp = (ModTransportPortInstruction) l4m;
                oxm = factory().oxms().tcpSrc(TransportPort.of(tp.port().toInt()));
                break;
            case TCP_DST:
                tp = (ModTransportPortInstruction) l4m;
                oxm = factory().oxms().tcpDst(TransportPort.of(tp.port().toInt()));
                break;
            case UDP_SRC:
                tp = (ModTransportPortInstruction) l4m;
                oxm = factory().oxms().udpSrc(TransportPort.of(tp.port().toInt()));
                break;
            case UDP_DST:
                tp = (ModTransportPortInstruction) l4m;
                oxm = factory().oxms().udpDst(TransportPort.of(tp.port().toInt()));
                break;
            default:
                log.warn("Unimplemented action type {}.", l4m.subtype());
                break;
        }

        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildExtensionAction(ExtensionTreatment i) {
        if (!driverService.isPresent()) {
            log.error("No driver service present");
            return null;
        }
        Driver driver = driverService.get().getDriver(deviceId);
        if (driver.hasBehaviour(ExtensionTreatmentInterpreter.class)) {
            DefaultDriverHandler handler =
                    new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
            ExtensionTreatmentInterpreter interpreter = handler.behaviour(ExtensionTreatmentInterpreter.class);
            return interpreter.mapInstruction(factory(), i);
        }

        return null;
    }

}
