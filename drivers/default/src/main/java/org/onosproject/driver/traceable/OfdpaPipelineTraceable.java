/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.driver.traceable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.pipeline.ofdpa.Ofdpa2Pipeline;
import org.onosproject.driver.pipeline.ofdpa.OvsOfdpaPipeline;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PipelineTraceableHitChain;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DataPlaneEntity;
import org.onosproject.net.PipelineTraceableInput;
import org.onosproject.net.PipelineTraceableOutput;
import org.onosproject.net.PipelineTraceablePacket;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PipelineTraceable;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.L2_FLOOD_TYPE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.L2_INTERFACE_TYPE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.L2_MULTICAST_TYPE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.L3_MULTICAST_TYPE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.ACL_TABLE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.BRIDGING_TABLE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.MPLS_L3_TYPE_TABLE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.MULTICAST_ROUTING_TABLE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.TMAC_TABLE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.VLAN_TABLE;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements a driver behavior that enables a logical probe packet to traverse the device pipeline
 * and to return dataplane entities that matched against the logical probe packet.
 */
public class OfdpaPipelineTraceable extends AbstractHandlerBehaviour implements PipelineTraceable {

    private static final Logger log = getLogger(OfdpaPipelineTraceable.class);
    // Behavior context
    private DeviceId deviceId;
    private String driverName;
    // Utility
    private final Comparator<FlowEntry> comparatorById = Comparator.comparing(
            (FlowEntry f) -> ((IndexTableId) f.table()).id());
    private final Comparator<FlowEntry> comparatorByPriority = Comparator.comparing(
            FlowRule::priority);

    @Override
    public void init() {
        this.deviceId = this.data().deviceId();
        this.driverName = this.data().driver().name();
    }

    @Override
    public PipelineTraceableOutput apply(PipelineTraceableInput input) {
        PipelineTraceableOutput.Builder outputBuilder = PipelineTraceableOutput.builder();
        log.debug("Current packet {} - applying flow tables", input.ingressPacket());
        List<FlowEntry> outputFlows = new ArrayList<>();
        List<Instruction> deferredInstructions = new ArrayList<>();
        PipelineTraceableHitChain currentHitChain = PipelineTraceableHitChain.emptyHitChain();
        TrafficSelector currentPacket = DefaultTrafficSelector.builder(
                input.ingressPacket().packet()).build();

        // Init step - find out the first table
        int initialTableId = -1;
        FlowEntry nextTableIdEntry = findNextTableIdEntry(initialTableId, input.flows());
        if (nextTableIdEntry == null) {
            currentHitChain.setEgressPacket(new PipelineTraceablePacket(currentPacket));
            currentHitChain.dropped();
            return outputBuilder.appendToLog("No flow rules for device " + deviceId + ". Aborting")
                    .noFlows()
                    .addHitChain(currentHitChain)
                    .build();
        }

        // Iterates over the flow tables until the end of the pipeline
        TableId tableId = nextTableIdEntry.table();
        FlowEntry flowEntry;
        boolean lastTable = false;
        while (!lastTable) {
            log.debug("Searching a Flow Entry on table {} for packet {}", tableId, currentPacket);

            // Gets the rule that matches the incoming packet
            flowEntry = matchHighestPriority(currentPacket, tableId, input.flows());
            log.debug("Found Flow Entry {}", flowEntry);

            // If the flow entry on a table is null and we are on hardware we treat as table miss, with few exceptions
            if (flowEntry == null && isHardwareSwitch()) {
                log.debug("Ofdpa Hw setup, no flow rule means table miss");

                if (((IndexTableId) tableId).id() == MPLS_L3_TYPE_TABLE) {
                    // Apparently a miss but Table 27 on OFDPA is a fixed table
                    currentPacket = handleOfdpa27FixedTable(input.ingressPacket().packet(), currentPacket);
                    // The nextTable should be ACL
                    tableId = IndexTableId.of(ACL_TABLE - 1);
                }

                // Finding next table to go In case of miss
                nextTableIdEntry = findNextTableIdEntry(((IndexTableId) tableId).id(), input.flows());
                log.debug("Next table id entry {}", nextTableIdEntry);
                // FIXME Find better solution that enable granularity greater than 0 or all rules
                // (another possibility is max tableId)
                if (nextTableIdEntry == null && currentHitChain.hitChain().size() == 0) {
                    currentHitChain.setEgressPacket(new PipelineTraceablePacket(currentPacket));
                    currentHitChain.dropped();
                    return outputBuilder.appendToLog("No flow rules for device " + deviceId + ". Aborting")
                            .noFlows()
                            .addHitChain(currentHitChain)
                            .build();

                } else if (nextTableIdEntry == null) {
                    // Means that no more flow rules are present
                    lastTable = true;

                } else if (((IndexTableId) tableId).id() == TMAC_TABLE) {
                    // If the table is 20 OFDPA skips to table 50
                    log.debug("A miss on Table 20 on OFDPA means that we skip directly to table 50");
                    tableId = IndexTableId.of(BRIDGING_TABLE);

                } else if (((IndexTableId) tableId).id() == MULTICAST_ROUTING_TABLE) {
                    // If the table is 40 OFDPA skips to table 60
                    log.debug("A miss on Table 40 on OFDPA means that we skip directly to table 60");
                    tableId = IndexTableId.of(ACL_TABLE);
                } else {
                    tableId = nextTableIdEntry.table();
                }

            } else if (flowEntry == null) {
                currentHitChain.setEgressPacket(new PipelineTraceablePacket(currentPacket));
                currentHitChain.dropped();
                return outputBuilder.appendToLog("Packet has no match on table " + tableId
                        + " in device " + deviceId + ". Dropping")
                        .noFlows()
                        .addHitChain(currentHitChain)
                        .build();
            } else {

                // If the table has a transition
                if (flowEntry.treatment().tableTransition() != null) {
                    // Updates the next table we transitions to
                    tableId = IndexTableId.of(flowEntry.treatment().tableTransition().tableId());
                    log.debug("Flow Entry has transition to table Id {}", tableId);
                    currentHitChain.addDataPlaneEntity(new DataPlaneEntity(flowEntry));
                } else {
                    // Table has no transition so it means that it's an output rule if on the last table
                    log.debug("Flow Entry has no transition to table, treating as last rule {}", flowEntry);
                    currentHitChain.addDataPlaneEntity(new DataPlaneEntity(flowEntry));
                    outputFlows.add(flowEntry);
                    lastTable = true;
                }

                // Updates the packet according to the immediate actions of this flow rule.
                currentPacket = updatePacket(currentPacket, flowEntry.treatment().immediate()).build();

                // Saves the deferred rules for later maintaining the order
                deferredInstructions.addAll(flowEntry.treatment().deferred());

                // If the flow requires to clear deferred actions we do so for all the ones we encountered.
                if (flowEntry.treatment().clearedDeferred()) {
                    deferredInstructions.clear();
                }

                // On table 10 OFDPA needs two rules to apply the vlan if none and then to transition to the next table.
                if (shouldMatchSecondVlanFlow(flowEntry)) {

                    // Let's get the packet vlanId instruction
                    VlanIdCriterion packetVlanIdCriterion =
                            (VlanIdCriterion) currentPacket.getCriterion(Criterion.Type.VLAN_VID);

                    // Let's get the flow entry vlan mod instructions
                    ModVlanIdInstruction entryModVlanIdInstruction = (ModVlanIdInstruction) flowEntry.treatment()
                            .immediate().stream()
                            .filter(instruction -> instruction instanceof ModVlanIdInstruction)
                            .findFirst().orElse(null);

                    // If the entry modVlan is not null we need to make sure that the packet has been updated and there
                    // is a flow rule that matches on same criteria and with updated vlanId
                    if (entryModVlanIdInstruction != null) {

                        FlowEntry secondVlanFlow = getSecondFlowEntryOnTable10(currentPacket,
                                packetVlanIdCriterion, entryModVlanIdInstruction, input.flows());

                        // We found the flow that we expected
                        if (secondVlanFlow != null) {
                            currentHitChain.addDataPlaneEntity(new DataPlaneEntity(secondVlanFlow));
                        } else {
                            currentHitChain.setEgressPacket(new PipelineTraceablePacket(currentPacket));
                            currentHitChain.dropped();
                            return outputBuilder.appendToLog("Missing forwarding rule for tagged"
                                    + " packet on " + deviceId)
                                    .noFlows()
                                    .addHitChain(currentHitChain)
                                    .build();
                        }
                    }
                }
            }
        }

        // Creating a modifiable builder for the egress packet
        TrafficSelector.Builder egressPacket = DefaultTrafficSelector.builder(currentPacket);

        log.debug("Current packet {} - applying output flows", currentPacket);
        // Handling output flows which basically means handling output to controller.
        // OVS and OFDPA have both immediate -> OUTPUT:CONTROLLER. Theoretically there is no
        // need to reflect the updates performed on the packets and on the chain.
        List<PortNumber> outputPorts = new ArrayList<>();
        handleOutputFlows(currentPacket, outputFlows, egressPacket, outputPorts, currentHitChain,
                outputBuilder, input.ingressPacket().packet());

        // Immediate instructions
        log.debug("Current packet {} - applying immediate instructions", currentPacket);
        // Handling immediate instructions which basically means handling output to controller.
        // OVS has immediate -> group -> OUTPUT:CONTROLLER.
        List<DataPlaneEntity> entries = ImmutableList.copyOf(currentHitChain.hitChain());
        // Go to the next step - using a copy of the egress packet and of the hit chain
        PipelineTraceableHitChain newHitChain = PipelineTraceableHitChain.emptyHitChain();
        currentHitChain.hitChain().forEach(newHitChain::addDataPlaneEntity);
        TrafficSelector.Builder newEgressPacket = DefaultTrafficSelector.builder(egressPacket.build());
        for (DataPlaneEntity entry : entries) {
            flowEntry = entry.getFlowEntry();
            if (flowEntry != null) {
                getGroupsFromInstructions(input.groups(), flowEntry.treatment().immediate(), newEgressPacket,
                        outputPorts, newHitChain, outputBuilder, input, false);
            }
        }

        // Deferred instructions
        log.debug("Current packet {} - applying deferred instructions", egressPacket.build());
        // If we have deferred instructions at this point we handle them.
        // Here, we are basically handling the normal forwarding scenarios that
        // always happen through deferred:group. Here we don't care about the
        // egress packet and of the hit chain. This is the last step.
        if (deferredInstructions.size() > 0) {
            handleDeferredActions(egressPacket.build(), input.groups(), deferredInstructions, outputPorts,
                    currentHitChain, outputBuilder, input);
        }

        // If there are no outputs - packet is dropped
        // Let's store the partial hit chain and set a message
        if (outputPorts.isEmpty()) {
            currentHitChain.setEgressPacket(new PipelineTraceablePacket(egressPacket.build()));
            currentHitChain.dropped();
            outputBuilder.appendToLog("Packet has no output in device " + deviceId + ". Dropping")
                    .dropped()
                    .addHitChain(currentHitChain);
        }

        // Done!
        return outputBuilder.build();
    }

    // Finds the flow entry with the minimum next table Id.
    private FlowEntry findNextTableIdEntry(int currentId, List<FlowEntry> flows) {
        return flows.stream()
                .filter(f -> ((IndexTableId) f.table()).id() > currentId)
                .min(comparatorById).orElse(null);
    }

    // Finds the rule in the device that matches the input packet and has the highest priority.
    // TODO Candidate for an AbstractBehavior implementation
    private FlowEntry matchHighestPriority(TrafficSelector packet, TableId tableId, List<FlowEntry> flows) {
        //Computing the possible match rules.
        return flows.stream()
                .filter(flowEntry -> flowEntry.table().equals(tableId))
                .filter(flowEntry -> match(packet, flowEntry))
                .max(comparatorByPriority).orElse(null);
    }

    // Matches the packet with the given flow entry
    // TODO Candidate for an AbstractBehavior implementation
    private boolean match(TrafficSelector packet, FlowEntry flowEntry) {
        return flowEntry.selector().criteria().stream().allMatch(criterion -> {
            Criterion.Type type = criterion.type();
            //If the criterion has IP we need to do LPM to establish matching.
            if (type.equals(Criterion.Type.IPV4_SRC) || type.equals(Criterion.Type.IPV4_DST) ||
                    type.equals(Criterion.Type.IPV6_SRC) || type.equals(Criterion.Type.IPV6_DST)) {
                return matchIp(packet, (IPCriterion) criterion);
                //we check that the packet contains the criterion provided by the flow rule.
            } else if (type.equals(Criterion.Type.ETH_SRC_MASKED)) {
                return matchMac(packet, (EthCriterion) criterion, false);
            } else if (type.equals(Criterion.Type.ETH_DST_MASKED)) {
                return matchMac(packet, (EthCriterion) criterion, true);
            } else {
                return packet.criteria().contains(criterion);
            }
        });
    }

    // Checks if the packet has an dst or src IP and if that IP matches the subnet of the ip criterion
    // TODO Candidate for an AbstractBehavior implementation
    private boolean matchIp(TrafficSelector packet, IPCriterion criterion) {
        IPCriterion matchCriterion = (IPCriterion) packet.getCriterion(criterion.type());
        // if the packet does not have an IPv4 or IPv6 criterion we return true
        if (matchCriterion == null) {
            return false;
        }
        log.debug("Checking if {} is under {}", matchCriterion.ip(), criterion.ip());
        IpPrefix subnet = criterion.ip();
        return subnet.contains(matchCriterion.ip().address());
    }

    // Checks if the packet has a dst or src MAC and if that Mac matches the mask of the mac criterion
    // TODO Candidate for an AbstractBehavior implementation
    private boolean matchMac(TrafficSelector packet, EthCriterion hitCriterion, boolean dst) {
        //Packet can have only one EthCriterion
        EthCriterion matchCriterion;
        if (dst) {
            matchCriterion = (EthCriterion) packet.criteria().stream().filter(criterion1 ->
                    criterion1.type().equals(Criterion.Type.ETH_DST_MASKED) ||
                            criterion1.type().equals(Criterion.Type.ETH_DST))
                    .findFirst().orElse(null);
        } else {
            matchCriterion = (EthCriterion) packet.criteria().stream().filter(criterion1 ->
                    criterion1.type().equals(Criterion.Type.ETH_SRC_MASKED) ||
                            criterion1.type().equals(Criterion.Type.ETH_SRC))
                    .findFirst().orElse(null);
        }
        //if the packet does not have an ETH criterion we return true
        if (matchCriterion == null) {
            return true;
        }
        log.debug("Checking if {} is under {}/{}", matchCriterion.mac(), hitCriterion.mac(), hitCriterion.mask());
        return matchCriterion.mac().inRange(hitCriterion.mac(), hitCriterion.mask());
    }

    // Handles table 27 in Ofpda which is a fixed table not visible to any controller that handles Mpls Labels.
    private TrafficSelector handleOfdpa27FixedTable(TrafficSelector initialPacket, TrafficSelector packet) {
        log.debug("Handling table 27 on OFDPA, removing mpls ETH Type and change mpls label");

        Criterion mplsCriterion = packet.getCriterion(Criterion.Type.ETH_TYPE);
        // T3 was using the initial packet of the trace - using the metadata in the packet to carry this info
        Criterion metadataCriterion = initialPacket.getCriterion(Criterion.Type.METADATA);
        ImmutableList.Builder<Instruction> builder = ImmutableList.builder();

        // If the packet comes in with the expected elements we update it as per OFDPA spec.
        if (mplsCriterion != null && ((EthTypeCriterion) mplsCriterion).ethType()
                .equals(EthType.EtherType.MPLS_UNICAST.ethType()) && metadataCriterion != null) {

            // Get the metadata to restore the original ethertype
            long ethType = ((MetadataCriterion) metadataCriterion).metadata();
            //TODO update with parsing with eth MPLS pop Instruction for treating label an bos
            Instruction ethInstruction = Instructions.popMpls(EthType.EtherType.lookup((short) ethType).ethType());
            //FIXME what do we use as L3_Unicast mpls Label ?
            //translateInstruction(builder, ethInstruction);
            builder.add(ethInstruction);

            // Filtering out metadata
            TrafficSelector.Builder currentPacketBuilder = DefaultTrafficSelector.builder();
            packet.criteria().stream()
                    .filter(criterion -> criterion.type() != Criterion.Type.METADATA)
                    .forEach(currentPacketBuilder::add);
            packet = currentPacketBuilder.build();
        }
        packet = updatePacket(packet, builder.build()).build();
        return packet;
    }

    // Applies all give instructions to the input packet
    private TrafficSelector.Builder updatePacket(TrafficSelector packet, List<Instruction> instructions) {
        TrafficSelector.Builder newSelector = DefaultTrafficSelector.builder(packet);

        //FIXME optimize
        for (Instruction instruction : instructions) {
            newSelector = translateInstruction(newSelector, instruction);
        }
        return newSelector;
    }

    // Applies an instruction to the packet in the form of a selector
    private TrafficSelector.Builder translateInstruction(TrafficSelector.Builder newSelector, Instruction instruction) {
        log.debug("Translating instruction {}", instruction);
        log.debug("New Selector {}", newSelector.build());
        //TODO add as required
        Criterion criterion = null;
        if (instruction.type() == Instruction.Type.L2MODIFICATION) {
            L2ModificationInstruction l2Instruction = (L2ModificationInstruction) instruction;
            switch (l2Instruction.subtype()) {
                case VLAN_ID:
                    ModVlanIdInstruction vlanIdInstruction =
                            (ModVlanIdInstruction) instruction;
                    VlanId id = vlanIdInstruction.vlanId();
                    criterion = Criteria.matchVlanId(id);
                    break;
                case VLAN_POP:
                    criterion = Criteria.matchVlanId(VlanId.NONE);
                    break;
                case MPLS_PUSH:
                    L2ModificationInstruction.ModMplsHeaderInstruction mplsEthInstruction =
                            (L2ModificationInstruction.ModMplsHeaderInstruction) instruction;
                    criterion = Criteria.matchEthType(mplsEthInstruction.ethernetType().toShort());

                    // When pushing MPLS adding metadata to remember the original ethtype
                    if (isHardwareSwitch()) {
                        TrafficSelector temporaryPacket = newSelector.build();
                        Criterion ethCriterion = temporaryPacket.getCriterion(Criterion.Type.ETH_TYPE);
                        if (ethCriterion != null) {
                            TrafficSelector.Builder tempSelector = DefaultTrafficSelector.builder(temporaryPacket);
                            // Store the old ether type for the
                            tempSelector.matchMetadata(((EthTypeCriterion) ethCriterion).ethType().toShort());
                            newSelector = tempSelector;
                        }
                    }

                    break;
                case MPLS_POP:
                    L2ModificationInstruction.ModMplsHeaderInstruction mplsPopInstruction =
                            (L2ModificationInstruction.ModMplsHeaderInstruction) instruction;
                    criterion = Criteria.matchEthType(mplsPopInstruction.ethernetType().toShort());

                    //When popping MPLS we remove label and BOS
                    TrafficSelector temporaryPacket = newSelector.build();
                    if (temporaryPacket.getCriterion(Criterion.Type.MPLS_LABEL) != null) {
                        TrafficSelector.Builder noMplsSelector = DefaultTrafficSelector.builder();
                        temporaryPacket.criteria().stream().filter(c ->
                                !c.type().equals(Criterion.Type.MPLS_LABEL) &&
                                        !c.type().equals(Criterion.Type.MPLS_BOS))
                                .forEach(noMplsSelector::add);
                        newSelector = noMplsSelector;
                    }

                    break;
                case MPLS_LABEL:
                    L2ModificationInstruction.ModMplsLabelInstruction mplsLabelInstruction =
                            (L2ModificationInstruction.ModMplsLabelInstruction) instruction;
                    criterion = Criteria.matchMplsLabel(mplsLabelInstruction.label());
                    newSelector.matchMplsBos(true);
                    break;
                case ETH_DST:
                    L2ModificationInstruction.ModEtherInstruction modEtherDstInstruction =
                            (L2ModificationInstruction.ModEtherInstruction) instruction;
                    criterion = Criteria.matchEthDst(modEtherDstInstruction.mac());
                    break;
                case ETH_SRC:
                    L2ModificationInstruction.ModEtherInstruction modEtherSrcInstruction =
                            (L2ModificationInstruction.ModEtherInstruction) instruction;
                    criterion = Criteria.matchEthSrc(modEtherSrcInstruction.mac());
                    break;
                default:
                    log.debug("Unsupported L2 Instruction");
                    break;
            }
        } else {
            log.debug("Unsupported Instruction");
        }
        if (criterion != null) {
            log.debug("Adding criterion {}", criterion);
            newSelector.add(criterion);
        }
        return newSelector;
    }

    // Method that finds a flow rule on table 10 that matches the packet and the VLAN of the already
    // found rule on table 10. This is because OFDPA needs two rules on table 10, first to apply the rule,
    // second to transition to following table
    private FlowEntry getSecondFlowEntryOnTable10(TrafficSelector packet, VlanIdCriterion packetVlanIdCriterion,
                                                  ModVlanIdInstruction entryModVlanIdInstruction,
                                                  List<FlowEntry> flows) {
        FlowEntry secondVlanFlow = null;
        // Check the packet has been update from the first rule.
        if (packetVlanIdCriterion.vlanId().equals(entryModVlanIdInstruction.vlanId())) {
            // find a rule on the same table that matches the vlan and
            // also all the other elements of the flow such as input port
            secondVlanFlow = flows.stream()
                    .filter(entry -> entry.table().equals(IndexTableId.of(VLAN_TABLE)))
                    .filter(entry -> {
                        VlanIdCriterion criterion = (VlanIdCriterion) entry.selector()
                                .getCriterion(Criterion.Type.VLAN_VID);
                        return criterion != null && match(packet, entry)
                                && criterion.vlanId().equals(entryModVlanIdInstruction.vlanId());
                    }).findFirst().orElse(null);

        }
        return secondVlanFlow;
    }

    // Handles output flows
    private List<FlowEntry> handleOutputFlows(TrafficSelector currentPacket, List<FlowEntry> outputFlows,
                                              TrafficSelector.Builder egressPacket, List<PortNumber> outputPorts,
                                              PipelineTraceableHitChain currentHitChain,
                                              PipelineTraceableOutput.Builder outputBuilder,
                                              TrafficSelector initialPacket) {
        // TODO optimization
        // outputFlows contains also last rule of device, so we need filtering for OUTPUT instructions.
        List<FlowEntry> outputFlowEntries = outputFlows.stream().filter(flow -> flow.treatment()
                .allInstructions().stream().filter(instruction -> instruction.type()
                        .equals(Instruction.Type.OUTPUT)).count() > 0).collect(Collectors.toList());

        if (outputFlowEntries.size() > 1) {
            outputBuilder.appendToLog("More than one flow rule with OUTPUT instruction");
            log.warn("There cannot be more than one flow entry with OUTPUT instruction for {}", currentPacket);
        }

        if (outputFlowEntries.size() == 1) {
            OutputInstruction outputInstruction = (OutputInstruction) outputFlowEntries.get(0).treatment()
                    .allInstructions().stream()
                    .filter(instruction -> instruction.type().equals(Instruction.Type.OUTPUT))
                    .findFirst().get();
            buildOutputFromDevice(egressPacket, outputPorts, outputInstruction, currentHitChain,
                    outputBuilder, initialPacket, false);
        }

        return outputFlowEntries;
    }

    // Builds a possible output from this device
    private void buildOutputFromDevice(TrafficSelector.Builder egressPacket,
                                       List<PortNumber> outputPorts,
                                       OutputInstruction outputInstruction,
                                       PipelineTraceableHitChain currentHitChain,
                                       PipelineTraceableOutput.Builder outputBuilder,
                                       TrafficSelector initialPacket,
                                       boolean dropped) {
        // Store the output port for further processing
        outputPorts.add(outputInstruction.port());
        // Create the final hit chain from the current one (deep copy)
        ConnectPoint outputPort = new ConnectPoint(deviceId, outputInstruction.port());
        PipelineTraceableHitChain finalHitChain = new PipelineTraceableHitChain(outputPort,
                Lists.newArrayList(currentHitChain.hitChain()),
                new PipelineTraceablePacket(egressPacket.build()));
        // Dropped early
        if (dropped) {
            log.debug("Packet {} has been dropped", egressPacket.build());
        } else {
            finalHitChain.pass();
        }
        if (outputPort.port().equals(PortNumber.CONTROLLER)) {
            handleVlanToController(finalHitChain, initialPacket);
        }
        // If there is already a chain do not add a copy
        outputBuilder.addHitChain(finalHitChain);
    }

    // If the initial packet comes tagged with a Vlan we output it with that to ONOS.
    // If ONOS applied a vlan we remove it.
    // TODO Candidate for an AbstractBehavior implementation
    private void handleVlanToController(PipelineTraceableHitChain currentHitChain, TrafficSelector initialPacket) {

        VlanIdCriterion initialVid = (VlanIdCriterion) initialPacket
                .getCriterion(Criterion.Type.VLAN_VID);
        VlanIdCriterion finalVid = (VlanIdCriterion) currentHitChain.egressPacket().packet()
                .getCriterion(Criterion.Type.VLAN_VID);

        if (initialVid != null && !initialVid.equals(finalVid) && initialVid.vlanId().equals(VlanId.NONE)) {
            Set<Criterion> finalCriteria = new HashSet<>(currentHitChain.egressPacket()
                    .packet().criteria());
            //removing the final vlanId
            finalCriteria.remove(finalVid);
            TrafficSelector.Builder packetUpdated = DefaultTrafficSelector.builder();
            finalCriteria.forEach(packetUpdated::add);
            //Initial was none so we set it to that
            packetUpdated.add(Criteria.matchVlanId(VlanId.NONE));
            //Update final packet
            currentHitChain.setEgressPacket(new PipelineTraceablePacket(packetUpdated.build()));
        }
    }

    // Gets group information from instructions.
    private void getGroupsFromInstructions(Map<GroupId, Group> groups, List<Instruction> instructions,
                                           TrafficSelector.Builder egressPacket, List<PortNumber> outputPorts,
                                           PipelineTraceableHitChain currentHitChain,
                                           PipelineTraceableOutput.Builder outputBuilder,
                                           PipelineTraceableInput input,
                                           boolean dropped) {

        List<Instruction> groupInstructionlist = new ArrayList<>();
        // sort instructions according to priority (larger Instruction.Type ENUM constant first)
        // which enables to treat other actions before the OUTPUT action
        // TODO improve the priority scheme according to the OpenFlow ActionSet spec
        List<Instruction> instructionsSorted = new ArrayList<>();
        instructionsSorted.addAll(instructions);
        instructionsSorted.sort((instr1, instr2) ->
                Integer.compare(instr2.type().ordinal(), instr1.type().ordinal()));

        // Handles first all non-group instructions
        for (Instruction instruction : instructionsSorted) {
            log.debug("Considering Instruction {}", instruction);
            // if the instruction is not group we need to update the packet or add the output
            // to the possible outputs for this packet
            if (!instruction.type().equals(Instruction.Type.GROUP)) {
                // FIXME ?? if the instruction is not group we need to update the packet
                // or add the output to the possible outputs for this packet
                if (instruction.type().equals(Instruction.Type.OUTPUT)) {
                    buildOutputFromDevice(egressPacket, outputPorts, (OutputInstruction) instruction,
                            currentHitChain, outputBuilder, input.ingressPacket().packet(), dropped);
                } else {
                    egressPacket = translateInstruction(egressPacket, instruction);
                }
            } else {
                // Store for later if the instruction is pointing to a group
                groupInstructionlist.add(instruction);
            }
        }

        // handle all the internal instructions pointing to a group.
        for (Instruction instr : groupInstructionlist) {
            Instructions.GroupInstruction groupInstruction = (Instructions.GroupInstruction) instr;
            Group group = groups.get(groupInstruction.groupId());

            // group does not exist in the dataplane
            if (group == null) {
                currentHitChain.setEgressPacket(new PipelineTraceablePacket(egressPacket.build()));
                currentHitChain.dropped();
                outputBuilder.appendToLog("Null group for Instruction " + instr)
                        .noGroups()
                        .addHitChain(currentHitChain);
                break;
            }

            log.debug("Analyzing group {}", group.id());

            // group is there but there are no members/buckets
            if (group.buckets().buckets().size() == 0) {
                // add the group to the traversed groups
                currentHitChain.addDataPlaneEntity(new DataPlaneEntity(group));
                currentHitChain.setEgressPacket(new PipelineTraceablePacket(egressPacket.build()));
                currentHitChain.dropped();
                outputBuilder.appendToLog("Group " + group.id() + " has no buckets")
                        .noMembers()
                        .addHitChain(currentHitChain);
                break;
            }

            PipelineTraceableHitChain newHitChain;
            TrafficSelector.Builder newEgressPacket;
            // Cycle in each of the group's buckets and add them to the groups for this Device.
            for (GroupBucket bucket : group.buckets().buckets()) {

                // add the group to the traversed groups
                currentHitChain.addDataPlaneEntity(new DataPlaneEntity(group));

                // Go to the next step - using a copy of the egress packet and of the hit chain
                newHitChain = PipelineTraceableHitChain.emptyHitChain();
                currentHitChain.hitChain().forEach(newHitChain::addDataPlaneEntity);
                newEgressPacket = DefaultTrafficSelector.builder(egressPacket.build());
                getGroupsFromInstructions(groups, bucket.treatment().allInstructions(), newEgressPacket,
                        outputPorts, newHitChain, outputBuilder, input,
                        dropped | isDropped(group.id(), bucket, input.ingressPort()));
            }
        }
    }

    private boolean isDropped(GroupId groupId, GroupBucket bucket, ConnectPoint ingressPort) {
        log.debug("Verify if the packet has to be dropped by the input port {}",
                ingressPort);
        // if It is not a l2 flood group and l2/l3 mcast skip
        int maskedId = groupId.id() & 0xF0000000;
        if (maskedId != L2_FLOOD_TYPE && maskedId != L2_MULTICAST_TYPE &&
                maskedId != L3_MULTICAST_TYPE) {
            return false;
        }
        // Verify if the bucket points to the ingress port
        Instructions.GroupInstruction groupInstruction;
        for (Instruction instr : bucket.treatment().allInstructions()) {
            if (instr.type().equals(Instruction.Type.GROUP)) {
                groupInstruction = (Instructions.GroupInstruction) instr;
                // FIXME According to the OFDPA spec for L3 MCAST if the VLAN is changed packet is not dropped
                if ((groupInstruction.groupId().id() & 0xF0000000) == L2_INTERFACE_TYPE) {
                    return (groupInstruction.groupId().id() & 0x0000FFFF) == ingressPort.port().toLong();
                }
            }
        }
        return false;
    }

    // Handles deferred instructions taken from the flows
    private void handleDeferredActions(TrafficSelector egressPacket, Map<GroupId, Group> groups,
                                       List<Instruction> deferredInstructions, List<PortNumber> outputPorts,
                                       PipelineTraceableHitChain currentHitChain,
                                       PipelineTraceableOutput.Builder outputBuilder,
                                       PipelineTraceableInput input) {
        // Update the packet with the deferred instructions
        TrafficSelector.Builder newEgressPacket = updatePacket(egressPacket, deferredInstructions);

        //Gather any output instructions from the deferred instruction
        List<Instruction> outputFlowInstruction = deferredInstructions.stream().filter(instruction ->
                instruction.type().equals(Instruction.Type.OUTPUT))
                .collect(Collectors.toList());

        //We are considering deferred instructions from flows, there can only be one output.
        if (outputFlowInstruction.size() > 1) {
            outputBuilder.appendToLog("More than one flow rule with OUTPUT instruction");
            log.warn("There cannot be more than one flow entry with OUTPUT instruction for {}", egressPacket);
        }

        // If there is one output let's go through that. No need to make a copy
        // of the egress packet and of the current hit chain.
        if (outputFlowInstruction.size() == 1) {
            buildOutputFromDevice(newEgressPacket, outputPorts, (OutputInstruction) outputFlowInstruction.get(0),
                    currentHitChain, outputBuilder, input.ingressPacket().packet(), false);
        }

        // If there is no output let's see if there any deferred instruction point to groups.
        // No need to make a copy of the egress packet and of the current chain.
        if (outputFlowInstruction.size() == 0) {
            getGroupsFromInstructions(groups, deferredInstructions, newEgressPacket, outputPorts,
                    currentHitChain, outputBuilder, input, false);
        }
    }

    // Checks whether it is an hw device based on different means.
    // throws an exception if the behavior has been used with wrong drivers
    private boolean isHardwareSwitch() {
        // Check if we are using ofdpa hw device by looking at the pipeliner
        // if we need to support a device that does not have a pipeliner
        // we can add an exclusion rules before this
        if (!this.handler().hasBehaviour(Pipeliner.class)) {
            throw new UnsupportedOperationException("Not supported device");
        }
        Pipeliner pipeliner = this.handler().behaviour(Pipeliner.class);
        if (pipeliner instanceof OvsOfdpaPipeline) {
            return false;
        } else if (pipeliner instanceof Ofdpa2Pipeline) {
            return true;
        }
        throw new UnsupportedOperationException("Not supported device");
    }

    // OF-DPA hardware requires one VLAN filtering rule and one VLAN assignment flow in the VLAN table.
    // This method is used to determine whether there is a need to match a second VLAN flow after
    // matching the given flowEntry.
    private boolean shouldMatchSecondVlanFlow(FlowEntry flowEntry) {
        // if we need to support a device that does not have a pipeliner
        // we can add an exclusion rules before this
        if (!this.handler().hasBehaviour(Pipeliner.class)) {
            throw new UnsupportedOperationException("Not supported device");
        }
        Pipeliner pipeliner = this.handler().behaviour(Pipeliner.class);
        if (!(pipeliner instanceof Ofdpa2Pipeline)) {
            return false;
        }
        return ((Ofdpa2Pipeline) pipeliner).requireSecondVlanTableEntry() &&
                flowEntry.table().equals(IndexTableId.of(VLAN_TABLE)) &&
                flowEntry.selector().getCriterion(Criterion.Type.VLAN_VID) != null &&
                ((VlanIdCriterion) flowEntry.selector().getCriterion(Criterion.Type.VLAN_VID))
                        .vlanId().equals(VlanId.NONE);
    }

}
