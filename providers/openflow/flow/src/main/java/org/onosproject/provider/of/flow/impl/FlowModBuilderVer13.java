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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModLambdaInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.PushHeaderInstructions;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onlab.packet.Ip4Address;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow mod builder for OpenFlow 1.3+.
 */
public class FlowModBuilderVer13 extends FlowModBuilder {

    private static final Logger log = LoggerFactory.getLogger(FlowModBuilderVer10.class);
    private static final int OFPCML_NO_BUFFER = 0xffff;

    private final TrafficTreatment treatment;

    /**
     * Constructor for a flow mod builder for OpenFlow 1.3.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     */
    protected FlowModBuilderVer13(FlowRule flowRule, OFFactory factory, Optional<Long> xid) {
        super(flowRule, factory, xid);

        this.treatment = flowRule.treatment();
    }

    @Override
    public OFFlowAdd buildFlowAdd() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();

        // FIXME had to revert back to using apply-actions instead of
        // write-actions because LINC-OE apparently doesn't support
        // write-actions. I would prefer to change this back in the future
        // because apply-actions is an optional instruction in OF 1.3.

        //OFInstruction writeActions =
                //factory().instructions().writeActions(actions);

        long cookie = flowRule().id().value();


        OFFlowAdd fm = factory().buildFlowAdd()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                //.setInstructions(Collections.singletonList(writeActions))
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .build();

        return fm;
    }

    @Override
    public OFFlowMod buildFlowMod() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();
        //OFInstruction writeActions =
                //factory().instructions().writeActions(actions);

        long cookie = flowRule().id().value();


        OFFlowMod fm = factory().buildFlowModify()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                //.setInstructions(Collections.singletonList(writeActions))
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .build();

        return fm;
    }

    @Override
    public OFFlowDelete buildFlowDel() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();
        //OFInstruction writeActions =
                //factory().instructions().writeActions(actions);

        long cookie = flowRule().id().value();

        OFFlowDelete fm = factory().buildFlowDelete()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .build();

        return fm;
    }

    private List<OFAction> buildActions() {
        List<OFAction> actions = new LinkedList<>();
        if (treatment == null) {
            return actions;
        }
        for (Instruction i : treatment.instructions()) {
            switch (i.type()) {
            case DROP:
                log.warn("Saw drop action; assigning drop action");
                return new LinkedList<>();
            case L0MODIFICATION:
                actions.add(buildL0Modification(i));
                break;
            case L2MODIFICATION:
                actions.add(buildL2Modification(i));
                break;
            case L3MODIFICATION:
                actions.add(buildL3Modification(i));
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
            default:
                log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }

        return actions;
    }

    private OFAction buildL0Modification(Instruction i) {
        L0ModificationInstruction l0m = (L0ModificationInstruction) i;
        switch (l0m.subtype()) {
        case LAMBDA:
            ModLambdaInstruction ml = (ModLambdaInstruction) i;
            return factory().actions().circuit(factory().oxms().ochSigidBasic(
                    new CircuitSignalID((byte) 1, (byte) 2, ml.lambda(), (short) 1)));
        default:
            log.warn("Unimplemented action type {}.", l0m.subtype());
            break;
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
                PushHeaderInstructions pushHeaderInstructions =
                        (PushHeaderInstructions) l2m;
                return factory().actions().pushMpls(EthType.of(pushHeaderInstructions
                                                               .ethernetType().getEtherType()));
            case MPLS_POP:
                PushHeaderInstructions  popHeaderInstructions =
                        (PushHeaderInstructions) l2m;
                return factory().actions().popMpls(EthType.of(popHeaderInstructions
                                                          .ethernetType().getEtherType()));
            case MPLS_LABEL:
                ModMplsLabelInstruction mplsLabel =
                        (ModMplsLabelInstruction) l2m;
                oxm = factory().oxms().mplsLabel(U32.of(mplsLabel.label()
                                                                  .longValue()));

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
        OFOxm<?> oxm = null;
        switch (l3m.subtype()) {
        case IP_DST:
            ip = (ModIPInstruction) i;
            ip4 = ip.ip().getIp4Address();
            oxm = factory().oxms().ipv4Dst(IPv4Address.of(ip4.toInt()));
            break;
        case IP_SRC:
            ip = (ModIPInstruction) i;
            ip4 = ip.ip().getIp4Address();
            oxm = factory().oxms().ipv4Src(IPv4Address.of(ip4.toInt()));
            break;
        default:
            log.warn("Unimplemented action type {}.", l3m.subtype());
            break;
        }

        if (oxm != null) {
            return factory().actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

}
