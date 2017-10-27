/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.collect.Sets;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.StatTriggerField;
import org.onosproject.net.flow.StatTriggerFlag;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFOxsList;
import org.projectfloodlight.openflow.protocol.OFStatTriggerFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionMeter;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.oxs.OFOxs;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.projectfloodlight.openflow.protocol.OFStatTriggerFlags.ONLY_FIRST;
import static org.projectfloodlight.openflow.protocol.OFStatTriggerFlags.PERIODIC;

/**
 * Flow mod builder for OpenFlow 1.5+.
 */
public class FlowModBuilderVer15 extends FlowModBuilderVer13 {

    /**
     * Constructor for a flow mod builder for OpenFlow 1.5.
     *
     * @param flowRule      the flow rule to transform into a flow mod
     * @param factory       the OpenFlow factory to use to build the flow mod
     * @param xid           the transaction ID
     * @param driverService the device driver service
     */
    protected FlowModBuilderVer15(FlowRule flowRule, OFFactory factory,
                                  Optional<Long> xid,
                                  Optional<DriverService> driverService) {
        super(flowRule, factory, xid, driverService);
    }

    @Override
    public OFFlowMod buildFlowAdd() {
        Match match = buildMatch();
        List<OFAction> deferredActions = buildActions(treatment.deferred(), false);
        List<OFAction> immediateActions = buildActions(treatment.immediate(), true);
        List<OFInstruction> instructions = Lists.newLinkedList();

        if (treatment.clearedDeferred()) {
            instructions.add(factory().instructions().clearActions());
        }
        if (!immediateActions.isEmpty()) {
            instructions.add(factory().instructions().applyActions(immediateActions));
        }
        if (!deferredActions.isEmpty()) {
            instructions.add(factory().instructions().writeActions(deferredActions));
        }
        if (treatment.tableTransition() != null) {
            instructions.add(buildTableGoto(treatment.tableTransition()));
        }
        if (treatment.writeMetadata() != null) {
            instructions.add(buildMetadata(treatment.writeMetadata()));
        }
        if (treatment.statTrigger() != null) {
            instructions.add(buildStatTrigger(treatment.statTrigger()));
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
        List<OFAction> deferredActions = buildActions(treatment.deferred(), false);
        List<OFAction> immediateActions = buildActions(treatment.immediate(), true);
        List<OFInstruction> instructions = Lists.newLinkedList();


        if (!immediateActions.isEmpty()) {
            instructions.add(factory().instructions().applyActions(immediateActions));
        }
        if (treatment.clearedDeferred()) {
            instructions.add(factory().instructions().clearActions());
        }
        if (!deferredActions.isEmpty()) {
            instructions.add(factory().instructions().writeActions(deferredActions));
        }
        if (treatment.tableTransition() != null) {
            instructions.add(buildTableGoto(treatment.tableTransition()));
        }
        if (treatment.writeMetadata() != null) {
            instructions.add(buildMetadata(treatment.writeMetadata()));
        }
        if (treatment.statTrigger() != null) {
            instructions.add(buildStatTrigger(treatment.statTrigger()));
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

    private List<OFAction> buildActions(List<Instruction> treatments, Boolean immediateActions) {
        if (treatment == null) {
            return Collections.emptyList();
        }

        boolean tableFound = false;
        List<OFAction> actions = new LinkedList<>();

        //Meter action handling
        if (null != treatment.meters() && immediateActions) {
            treatment.meters().forEach(meterInstruction -> {
                OFAction meterAction = buildMeterAction(meterInstruction);
                actions.add(meterAction);
            });
        }

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
                    Instructions.OutputInstruction out = (Instructions.OutputInstruction) i;
                    OFActionOutput.Builder action = factory().actions().buildOutput()
                            .setPort(OFPort.of((int) out.port().toLong()));
                    if (out.port().equals(PortNumber.CONTROLLER)) {
                        action.setMaxLen(OFPCML_NO_BUFFER);
                    }
                    actions.add(action.build());
                    break;
                case GROUP:
                    Instructions.GroupInstruction group = (Instructions.GroupInstruction) i;
                    OFActionGroup.Builder groupBuilder = factory().actions().buildGroup()
                            .setGroup(OFGroup.of(group.groupId().id()));
                    actions.add(groupBuilder.build());
                    break;
                case QUEUE:
                    Instructions.SetQueueInstruction queue = (Instructions.SetQueueInstruction) i;
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

    private OFOxsList getOFOxsList(Map<StatTriggerField, Long> statTriggerMap) {
        OFFactory factory = factory();
        List<OFOxs<?>> ofOxsList = Lists.newArrayList();
        for (Map.Entry<StatTriggerField, Long> entry : statTriggerMap.entrySet()) {
            switch (entry.getKey()) {
                case DURATION:
                    ofOxsList.add(factory.oxss().buildDuration().setValue(U64.of(entry.getValue())).build());
                    break;
                case IDLE_TIME:
                    ofOxsList.add(factory.oxss().buildIdleTime().setValue(U64.of(entry.getValue())).build());
                    break;
                case BYTE_COUNT:
                    ofOxsList.add(factory.oxss().buildByteCount().setValue(U64.of(entry.getValue())).build());
                    break;
                case FLOW_COUNT:
                    ofOxsList.add(factory.oxss().buildFlowCount().setValue(U32.of(entry.getValue())).build());
                    break;
                case PACKET_COUNT:
                    ofOxsList.add(factory.oxss().buildPacketCount().setValue(U64.of(entry.getValue())).build());
                    break;
                default:
                    log.warn("Unsupported Stat Trigger field");
                    break;
            }
        }
        return OFOxsList.ofList(ofOxsList);
    }

    private Set<OFStatTriggerFlags> getStatTriggerFlag(StatTriggerFlag flag) {
        Set<OFStatTriggerFlags> statTriggerFlagsSet = Sets.newHashSet();
        switch (flag) {
            case PERIODIC:
                statTriggerFlagsSet.add(PERIODIC);
                break;
            case ONLY_FIRST:
                statTriggerFlagsSet.add(ONLY_FIRST);
                break;
            default:
                break;
        }
        return statTriggerFlagsSet;
    }

    /**
     * Meter action builder.
     *
     * @param meterInstruction meter instruction
     * @return meter action
     */
    protected OFAction buildMeterAction(Instructions.MeterInstruction meterInstruction) {
        OFActionMeter.Builder meterBuilder = factory().actions().buildMeter()
                .setMeterId(meterInstruction.meterId().id());
        return meterBuilder.build();
    }

    protected OFInstruction buildStatTrigger(Instructions.StatTriggerInstruction s) {
        OFInstruction instruction = factory().instructions().statTrigger(getStatTriggerFlag(s.getStatTriggerFlag()),
                getOFOxsList(s.getStatTriggerFieldMap()));
        return instruction;
    }
}
