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

package org.onosproject.t3.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.t3.api.GroupsInDevice;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.packet.EthType.EtherType;
import static org.onosproject.net.flow.TrafficSelector.*;
import static org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manager to troubleshoot packets inside the network.
 * Given a representation of a packet follows it's path in the network according to the existing flows and groups in
 * the devices.
 */
@Service
@Component(immediate = true)
public class TroubleshootManager implements TroubleshootService {

    private static final Logger log = getLogger(TroubleshootManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Override
    public StaticPacketTrace trace(TrafficSelector packet, ConnectPoint in) {
        log.info("Tracing packet {} coming in through {}", packet, in);
        //device must exist in ONOS
        Preconditions.checkNotNull(deviceService.getDevice(in.deviceId()),
                "Device " + in.deviceId() + " must exist in ONOS");

        StaticPacketTrace trace = new StaticPacketTrace(packet, in);
        //FIXME this can be done recursively
        trace = traceInDevice(trace, packet, in);
        //Building output connect Points
        List<ConnectPoint> path = new ArrayList<>();
        trace = getTrace(path, in, trace);
        return trace;
    }

    /**
     * Computes a trace for a give packet that start in the network at the given connect point.
     *
     * @param completePath the path traversed by the packet
     * @param in           the input connect point
     * @param trace        the trace to build
     * @return the build trace for that packet.
     */
    private StaticPacketTrace getTrace(List<ConnectPoint> completePath, ConnectPoint in, StaticPacketTrace trace) {

        log.debug("------------------------------------------------------------");

        //if the trace already contains the input connect point there is a loop
        if (pathContainsDevice(completePath, in.deviceId())) {
            trace.addResultMessage("Loop encountered in device " + in.deviceId());
            return trace;
        }

        //let's add the input connect point
        completePath.add(in);

        //If the trace has no outputs for the given input we stop here
        if (trace.getGroupOuputs(in.deviceId()) == null) {
            computePath(completePath, trace, null);
            trace.addResultMessage("No output out of device " + in.deviceId() + ". Packet is dropped");
            return trace;
        }

        //If the trace has ouputs we analyze them all
        for (GroupsInDevice outputPath : trace.getGroupOuputs(in.deviceId())) {

            ConnectPoint cp = outputPath.getOutput();
            log.debug("Connect point in {}", in);
            log.debug("Output path {}", cp);

            //Hosts for the the given output
            Set<Host> hostsList = hostService.getConnectedHosts(cp);
            //Hosts queried from the original ip or mac
            Set<Host> hosts = getHosts(trace);

            //If the two host collections contain the same item it means we reached the proper output
            if (!Collections.disjoint(hostsList, hosts)) {
                log.debug("Stopping here because host is expected destination {}, reached through", completePath);
                trace.addResultMessage("Reached required destination Host " + cp);
                computePath(completePath, trace, outputPath.getOutput());
                break;
            } else if (cp.port().equals(PortNumber.CONTROLLER)) {

                //Getting the master when the packet gets sent as packet in
                NodeId master = mastershipService.getMasterFor(cp.deviceId());
                trace.addResultMessage("Packet goes to the controller " + master.id());
                computePath(completePath, trace, outputPath.getOutput());

            } else if (linkService.getEgressLinks(cp).size() > 0) {

                //TODO this can be optimized if we use a Tree structure for paths.
                //if we already have outputs let's check if the one we are considering starts from one of the devices
                // in any of the ones we have.
                if (trace.getCompletePaths().size() > 0) {
                    ConnectPoint inputForOutput = null;
                    List<ConnectPoint> previousPath = new ArrayList<>();
                    for (List<ConnectPoint> path : trace.getCompletePaths()) {
                        for (ConnectPoint connect : path) {
                            //if the path already contains the input for the output we've found we use it
                            if (connect.equals(in)) {
                                inputForOutput = connect;
                                previousPath = path;
                                break;
                            }
                        }
                    }
                    //we use the pre-existing path up to the point we fork to a new output
                    if (inputForOutput != null && completePath.contains(inputForOutput)) {
                        List<ConnectPoint> temp = new ArrayList<>(previousPath);
                        completePath = temp.subList(0, previousPath.indexOf(inputForOutput) + 1);
                    }
                }

                //let's add the ouput for the input
                completePath.add(cp);
                //let's compute the links for the given output
                Set<Link> links = linkService.getEgressLinks(cp);
                log.debug("Egress Links {}", links);
                //For each link we trace the corresponding device
                for (Link link : links) {
                    ConnectPoint dst = link.dst();
                    //change in-port to the dst link in port
                    Builder updatedPacket = DefaultTrafficSelector.builder();
                    outputPath.getFinalPacket().criteria().forEach(updatedPacket::add);
                    updatedPacket.add(Criteria.matchInPort(dst.port()));
                    log.debug("DST Connect Point {}", dst);
                    //build the elements for that device
                    traceInDevice(trace, updatedPacket.build(), dst);
                    //continue the trace along the path
                    getTrace(completePath, dst, trace);
                }

            } else if (deviceService.getPort(cp).isEnabled()) {
                if (hostsList.isEmpty()) {
                    trace.addResultMessage("Packet is " + ((EthTypeCriterion) outputPath.getFinalPacket()
                            .getCriterion(Criterion.Type.ETH_TYPE)).ethType() + " and reached " +
                            cp + " with no hosts connected ");
                } else {
                    trace.addResultMessage("Packet is " + ((EthTypeCriterion) outputPath.getFinalPacket()
                            .getCriterion(Criterion.Type.ETH_TYPE)).ethType() + " and reached " +
                            cp + " with hosts " + hostsList);
                }
                computePath(completePath, trace, outputPath.getOutput());

            } else {
                //No links means that the packet gets dropped.
                log.warn("No links out of {}", cp);
                computePath(completePath, trace, cp);
                trace.addResultMessage("No links depart from " + cp + ". Packet is dropped");
            }
        }
        return trace;
    }

    /**
     * Checks if the path contains the device.
     *
     * @param completePath the path
     * @param deviceId     the device to check
     * @return true if the path contains the device
     */
    //TODO might prove costly, improvement: a class with both CPs and DeviceIds point.
    private boolean pathContainsDevice(List<ConnectPoint> completePath, DeviceId deviceId) {
        for (ConnectPoint cp : completePath) {
            if (cp.deviceId().equals(deviceId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the hosts for the given initial packet.
     *
     * @param trace the trace we are building
     * @return set of the hosts we are trying to reach
     */
    private Set<Host> getHosts(StaticPacketTrace trace) {
        IPCriterion ipv4Criterion = ((IPCriterion) trace.getInitialPacket()
                .getCriterion(Criterion.Type.IPV4_DST));
        IPCriterion ipv6Criterion = ((IPCriterion) trace.getInitialPacket()
                .getCriterion(Criterion.Type.IPV6_DST));
        Set<Host> hosts = new HashSet<>();
        if (ipv4Criterion != null) {
            hosts.addAll(hostService.getHostsByIp(ipv4Criterion.ip().address()));
        }
        if (ipv6Criterion != null) {
            hosts.addAll(hostService.getHostsByIp(ipv6Criterion.ip().address()));
        }
        EthCriterion ethCriterion = ((EthCriterion) trace.getInitialPacket()
                .getCriterion(Criterion.Type.ETH_DST));
        if (ethCriterion != null) {
            hosts.addAll(hostService.getHostsByMac(ethCriterion.mac()));
        }
        return hosts;
    }

    /**
     * Computes the list of traversed connect points.
     *
     * @param completePath the list of devices
     * @param trace        the trace we are building
     * @param output       the final output connect point
     */
    private void computePath(List<ConnectPoint> completePath, StaticPacketTrace trace, ConnectPoint output) {
        List<ConnectPoint> traverseList = new ArrayList<>();
        if (!completePath.contains(trace.getInitialConnectPoint())) {
            traverseList.add(trace.getInitialConnectPoint());
        }
        traverseList.addAll(completePath);
        if (output != null && !completePath.contains(output)) {
            traverseList.add(output);
        }
        trace.addCompletePath(traverseList);
    }

    /**
     * Traces the packet inside a device starting from an input connect point.
     *
     * @param trace  the trace we are building
     * @param packet the packet we are tracing
     * @param in     the input connect point.
     * @return updated trace
     */
    private StaticPacketTrace traceInDevice(StaticPacketTrace trace, TrafficSelector packet, ConnectPoint in) {

        //we already traversed this device.
        if (trace.getGroupOuputs(in.deviceId()) != null) {
            log.debug("Trace already contains device and given outputs");
            return trace;
        }
        log.debug("Packet {} coming in from {}", packet, in);

        //if device is not available exit here.
        if (!deviceService.isAvailable(in.deviceId())) {
            trace.addResultMessage("Device is offline " + in.deviceId());
            return trace;
        }

        List<FlowEntry> flows = new ArrayList<>();
        List<FlowEntry> outputFlows = new ArrayList<>();

        List<Instruction> deferredInstructions = new ArrayList<>();

        FlowEntry nextTableIdEntry = findNextTableIdEntry(in.deviceId(), -1);
        if (nextTableIdEntry == null) {
            trace.addResultMessage("No flow rules for device " + in.deviceId() + ". Aborting");
            return trace;
        }
        TableId tableId = nextTableIdEntry.table();
        FlowEntry flowEntry;
        boolean output = false;
        while (!output) {
            log.debug("Searching a Flow Entry on table {} for packet {}", tableId, packet);
            //get the rule that matches the incoming packet
            flowEntry = matchHighestPriority(packet, in, tableId);
            log.debug("Found Flow Entry {}", flowEntry);

            boolean isOfdpaHardware = TroubleshootUtils.hardwareOfdpaMap
                    .getOrDefault(driverService.getDriver(in.deviceId()).name(), false);

            //if the flow entry on a table is null and we are on hardware we treat as table miss, with few exceptions
            if (flowEntry == null && isOfdpaHardware) {
                log.debug("Ofdpa Hw setup, no flow rule means table miss");

                //Handling Hardware Specifics
                if (((IndexTableId) tableId).id() == 27) {
                    //Apparently a miss but Table 27 on OFDPA is a fixed table
                    packet = handleOfdpa27FixedTable(trace, packet);
                }

                //Finding next table to go In case of miss
                nextTableIdEntry = findNextTableIdEntry(in.deviceId(), ((IndexTableId) tableId).id());
                log.debug("Next table id entry {}", nextTableIdEntry);

                //FIXME find better solution that enable granularity greater than 0 or all rules
                //(another possibility is max tableId)
                if (nextTableIdEntry == null && flows.size() == 0) {
                    trace.addResultMessage("No matching flow rules for device " + in.deviceId() + ". Aborting");
                    return trace;

                } else if (nextTableIdEntry == null) {
                    //Means that no more flow rules are present
                    output = true;

                } else if (((IndexTableId) tableId).id() == 20) {
                    //if the table is 20 OFDPA skips to table 50
                    log.debug("A miss on Table 20 on OFDPA means that we skip directly to table 50");
                    tableId = IndexTableId.of(50);

                } else {
                    tableId = nextTableIdEntry.table();
                }


            } else if (flowEntry == null) {
                trace.addResultMessage("Packet has no match on table " + tableId + " in device " +
                        in.deviceId() + ". Dropping");
                return trace;
            } else {
                //IF the table has a transition
                if (flowEntry.treatment().tableTransition() != null) {
                    //update the next table we transitions to
                    tableId = IndexTableId.of(flowEntry.treatment().tableTransition().tableId());
                    log.debug("Flow Entry has transition to table Id {}", tableId);
                    flows.add(flowEntry);
                } else {
                    //table has no transition so it means that it's an output rule if on the last table
                    log.debug("Flow Entry has no transition to table, treating as last rule {}", flowEntry);
                    flows.add(flowEntry);
                    outputFlows.add(flowEntry);
                    output = true;
                }
                //update the packet according to the immediate actions of this flow rule.
                packet = updatePacket(packet, flowEntry.treatment().immediate()).build();

                //save the deferred rules for later
                deferredInstructions.addAll(flowEntry.treatment().deferred());

                //If the flow requires to clear deferred actions we do so for all the ones we encountered.
                if (flowEntry.treatment().clearedDeferred()) {
                    deferredInstructions.clear();
                }

            }
        }

        //Creating a modifiable builder for the output packet
        Builder builder = DefaultTrafficSelector.builder();
        packet.criteria().forEach(builder::add);

        //Adding all the flows to the trace
        trace.addFlowsForDevice(in.deviceId(), ImmutableList.copyOf(flows));

        log.debug("Flows traversed by {}", packet);
        flows.forEach(entry -> {
            log.debug("Flow {}", entry);
        });

        log.debug("Output Flows for {}", packet);
        outputFlows.forEach(entry -> {
            log.debug("Output Flow {}", entry);
        });
        List<PortNumber> outputPorts = new ArrayList<>();

        //TODO optimization

        //outputFlows contains also last rule of device, so we need filtering for OUTPUT instructions.
        List<FlowEntry> outputFlowEntries = outputFlows.stream().filter(flow -> flow.treatment()
                .allInstructions().stream().filter(instruction -> instruction.type()
                        .equals(Instruction.Type.OUTPUT)).count() > 0).collect(Collectors.toList());

        if (outputFlowEntries.size() > 1) {
            trace.addResultMessage("More than one flow rule with OUTPUT instruction");
            log.warn("There cannot be more than one flow entry with OUTPUT instruction for {}", packet);
        }

        if (outputFlowEntries.size() == 1) {

            OutputInstruction outputInstruction = (OutputInstruction) outputFlowEntries.get(0).treatment()
                    .allInstructions().stream()
                    .filter(instruction -> {
                        return instruction.type().equals(Instruction.Type.OUTPUT);
                    }).findFirst().get();

            //FIXME using GroupsInDevice for output even if flows.
            buildOutputFromDevice(trace, in, builder, outputPorts, outputInstruction, ImmutableList.of());

        }
        log.debug("Handling Groups");
        //Analyze Groups
        List<Group> groups = new ArrayList<>();

        Collection<FlowEntry> nonOutputFlows = flows;
        nonOutputFlows.removeAll(outputFlowEntries);

        //Handling groups pointed at by immediate instructions
        for (FlowEntry entry : flows) {
            getGroupsFromInstructions(trace, groups, entry.treatment().immediate(),
                    entry.deviceId(), builder, outputPorts, in);
        }

        //If we have deferred instructions at this point we handle them.
        if (deferredInstructions.size() > 0) {
            builder = handleDeferredActions(trace, packet, in, deferredInstructions, outputPorts, groups);

        }
        packet = builder.build();
        log.debug("Groups hit by packet {}", packet);
        groups.forEach(group -> {
            log.debug("Group {}", group);
        });

        log.debug("Output ports for packet {}", packet);
        outputPorts.forEach(port -> {
            log.debug("Port {}", port);
        });
        log.info("Output Packet {}", packet);
        return trace;
    }


    /**
     * Handles table 27 in Ofpda which is a fixed table not visible to any controller that handles Mpls Labels.
     *
     * @param packet the incoming packet
     * @return the updated packet
     */
    private TrafficSelector handleOfdpa27FixedTable(StaticPacketTrace trace, TrafficSelector packet) {
        log.debug("Handling table 27 on OFDPA, removing mpls ETH Type and change mpls label");
        Criterion mplsCriterion = packet.getCriterion(Criterion.Type.ETH_TYPE);
        ImmutableList.Builder<Instruction> builder = ImmutableList.builder();

        //If the pakcet comes in with the expected elements we update it as per OFDPA spec.
        if (mplsCriterion != null && ((EthTypeCriterion) mplsCriterion).ethType()
                .equals(EtherType.MPLS_UNICAST.ethType())) {
            //TODO update with parsing with eth MPLS pop Instruction for treating label an bos
            Instruction ethInstruction = Instructions.popMpls(((EthTypeCriterion) trace.getInitialPacket()
                    .getCriterion(Criterion.Type.ETH_TYPE)).ethType());
            //FIXME what do we use as L3_Unicast mpls Label ?
            //translateInstruction(builder, ethInstruction);
            builder.add(ethInstruction);
        }
        packet = updatePacket(packet, builder.build()).build();
        return packet;
    }

    /**
     * Finds the flow entry with the minimun next table Id.
     *
     * @param deviceId  the device to search
     * @param currentId the current id. the search will use this as minimum
     * @return the flow entry with the minimum table Id after the given one.
     */
    private FlowEntry findNextTableIdEntry(DeviceId deviceId, int currentId) {

        final Comparator<FlowEntry> comparator = Comparator.comparing((FlowEntry f) -> ((IndexTableId) f.table()).id());

        return Lists.newArrayList(flowRuleService.getFlowEntries(deviceId).iterator())
                .stream().filter(f -> ((IndexTableId) f.table()).id() > currentId).min(comparator).orElse(null);
    }

    private Builder handleDeferredActions(StaticPacketTrace trace, TrafficSelector packet,
                                          ConnectPoint in, List<Instruction> deferredInstructions,
                                          List<PortNumber> outputPorts, List<Group> groups) {

        //Update the packet with the deferred instructions
        Builder builder = updatePacket(packet, deferredInstructions);

        //Gather any output instructions from the deferred instruction
        List<Instruction> outputFlowInstruction = deferredInstructions.stream().filter(instruction -> {
            return instruction.type().equals(Instruction.Type.OUTPUT);
        }).collect(Collectors.toList());

        //We are considering deferred instructions from flows, there can only be one output.
        if (outputFlowInstruction.size() > 1) {
            trace.addResultMessage("More than one flow rule with OUTPUT instruction");
            log.warn("There cannot be more than one flow entry with OUTPUT instruction for {}", packet);
        }
        //If there is one output let's go through that
        if (outputFlowInstruction.size() == 1) {
            buildOutputFromDevice(trace, in, builder, outputPorts, (OutputInstruction) outputFlowInstruction.get(0),
                    ImmutableList.of());
        }
        //If there is no output let's see if there any deferred instruction point to groups.
        if (outputFlowInstruction.size() == 0) {
            getGroupsFromInstructions(trace, groups, deferredInstructions,
                    in.deviceId(), builder, outputPorts, in);
        }
        return builder;
    }

    /**
     * Gets group information from instructions.
     *
     * @param trace           the trace we are building
     * @param groupsForDevice the set of groups for this device
     * @param instructions    the set of instructions we are searching for groups.
     * @param deviceId        the device we are considering
     * @param builder         the builder of the input packet
     * @param outputPorts     the output ports for that packet
     */
    private void getGroupsFromInstructions(StaticPacketTrace trace, List<Group> groupsForDevice,
                                           List<Instruction> instructions, DeviceId deviceId,
                                           Builder builder, List<PortNumber> outputPorts,
                                           ConnectPoint in) {
        List<Instruction> groupInstructionlist = new ArrayList<>();
        for (Instruction instruction : instructions) {
            log.debug("Considering Instruction {}", instruction);
            //if the instruction is not group we need to update the packet or add the output
            //to the possible outputs for this packet
            if (!instruction.type().equals(Instruction.Type.GROUP)) {
                //if the instruction is not group we need to update the packet or add the output
                //to the possible outputs for this packet
                if (instruction.type().equals(Instruction.Type.OUTPUT)) {
                    buildOutputFromDevice(trace, in, builder, outputPorts,
                            (OutputInstruction) instruction, groupsForDevice);
                } else {
                    builder = translateInstruction(builder, instruction);
                }
            } else {
                //if the instuction is pointing to a group we need to get the group
                groupInstructionlist.add(instruction);
            }
        }
        //handle all the internal instructions pointing to a group.
        for (Instruction instr : groupInstructionlist) {
            GroupInstruction groupInstruction = (GroupInstruction) instr;
            Group group = Lists.newArrayList(groupService.getGroups(deviceId)).stream().filter(groupInternal -> {
                return groupInternal.id().equals(groupInstruction.groupId());
            }).findAny().orElse(null);
            if (group == null) {
                trace.addResultMessage("Null group for Instruction " + instr);
                break;
            }
            //add the group to the traversed groups
            groupsForDevice.add(group);
            //Cycle in each of the group's buckets and add them to the groups for this Device.
            for (GroupBucket bucket : group.buckets().buckets()) {
                getGroupsFromInstructions(trace, groupsForDevice, bucket.treatment().allInstructions(),
                        deviceId, builder, outputPorts, in);
            }
        }
    }

    /**
     * Check if the output is the input port, if so adds a dop result message, otherwise builds
     * a possible output from this device.
     *
     * @param trace             the trace
     * @param in                the input connect point
     * @param builder           the packet builder
     * @param outputPorts       the list of output ports for this device
     * @param outputInstruction the output instruction
     * @param groupsForDevice   the groups we output from
     */
    private void buildOutputFromDevice(StaticPacketTrace trace, ConnectPoint in, Builder builder,
                                       List<PortNumber> outputPorts, OutputInstruction outputInstruction,
                                       List<Group> groupsForDevice) {
        ConnectPoint output = new ConnectPoint(in.deviceId(), outputInstruction.port());
        if (output.equals(in)) {
            trace.addResultMessage("Connect point out " + output + " is same as initial input " +
                    trace.getInitialConnectPoint());
        } else {
            trace.addGroupOutputPath(in.deviceId(),
                    new GroupsInDevice(output, groupsForDevice, builder.build()));
            outputPorts.add(outputInstruction.port());
        }
    }

    /**
     * Applies all give instructions to the input packet.
     *
     * @param packet       the input packet
     * @param instructions the set of instructions
     * @return the packet with the applied instructions
     */
    private Builder updatePacket(TrafficSelector packet, List<Instruction> instructions) {
        Builder newSelector = DefaultTrafficSelector.builder();
        packet.criteria().forEach(newSelector::add);
        //FIXME optimize
        for (Instruction instruction : instructions) {
            newSelector = translateInstruction(newSelector, instruction);
        }
        return newSelector;
    }

    /**
     * Applies an instruction to the packet in the form of a selector.
     *
     * @param newSelector the packet selector
     * @param instruction the instruction to be translated
     * @return the new selector with the applied instruction
     */
    private Builder translateInstruction(Builder newSelector, Instruction instruction) {
        log.debug("Translating instruction {}", instruction);
        log.debug("New Selector {}", newSelector.build());
        //TODO add as required
        Criterion criterion = null;
        switch (instruction.type()) {
            case L2MODIFICATION:
                L2ModificationInstruction l2Instruction = (L2ModificationInstruction) instruction;
                switch (l2Instruction.subtype()) {
                    case VLAN_ID:
                        L2ModificationInstruction.ModVlanIdInstruction vlanIdInstruction =
                                (L2ModificationInstruction.ModVlanIdInstruction) instruction;
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
                        break;
                    case MPLS_POP:
                        L2ModificationInstruction.ModMplsHeaderInstruction mplsPopInstruction =
                                (L2ModificationInstruction.ModMplsHeaderInstruction) instruction;
                        criterion = Criteria.matchEthType(mplsPopInstruction.ethernetType().toShort());

                        //When popping MPLS we remove label and BOS
                        TrafficSelector temporaryPacket = newSelector.build();
                        if (temporaryPacket.getCriterion(Criterion.Type.MPLS_LABEL) != null) {
                            Builder noMplsSelector = DefaultTrafficSelector.builder();
                            temporaryPacket.criteria().stream().filter(c -> {
                                return !c.type().equals(Criterion.Type.MPLS_LABEL) &&
                                        !c.type().equals(Criterion.Type.MPLS_BOS);
                            }).forEach(noMplsSelector::add);
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
                break;
            default:
                log.debug("Unsupported Instruction");
                break;
        }
        if (criterion != null) {
            log.debug("Adding criterion {}", criterion);
            newSelector.add(criterion);
        }
        return newSelector;
    }

    /**
     * Finds the rule in the device that mathces the input packet and has the highest priority.
     *
     * @param packet  the input packet
     * @param in      the connect point the packet comes in from
     * @param tableId the table to search
     * @return the flow entry
     */
    private FlowEntry matchHighestPriority(TrafficSelector packet, ConnectPoint in, TableId tableId) {
        //Computing the possible match rules.
        final Comparator<FlowEntry> comparator = Comparator.comparing(FlowRule::priority);
        return Lists.newArrayList(flowRuleService.getFlowEntries(in.deviceId()).iterator())
                .stream()
                .filter(flowEntry -> {
                    return flowEntry.table().equals(tableId);
                })
                .filter(flowEntry -> {
                    return match(packet, flowEntry);
                }).max(comparator).orElse(null);
    }

    /**
     * Matches the packet with the given flow entry.
     *
     * @param packet    the packet to match
     * @param flowEntry the flow entry to match the packet against
     * @return true if the packet matches the flow.
     */
    private boolean match(TrafficSelector packet, FlowEntry flowEntry) {
        //TODO handle MAC matching
        return flowEntry.selector().criteria().stream().allMatch(criterion -> {
            Criterion.Type type = criterion.type();
            //If the criterion has IP we need to do LPM to establish matching.
            if (type.equals(Criterion.Type.IPV4_SRC) || type.equals(Criterion.Type.IPV4_DST) ||
                    type.equals(Criterion.Type.IPV6_SRC) || type.equals(Criterion.Type.IPV6_DST)) {
                IPCriterion ipCriterion = (IPCriterion) criterion;
                IPCriterion matchCriterion = (IPCriterion) packet.getCriterion(ipCriterion.type());
                //if the packet does not have an IPv4 or IPv6 criterion we return false
                if (matchCriterion == null) {
                    return false;
                }
                try {
                    Subnet subnet = Subnet.createInstance(ipCriterion.ip().toString());
                    return subnet.isInSubnet(matchCriterion.ip().address().toInetAddress());
                } catch (UnknownHostException e) {
                    return false;
                }
                //we check that the packet contains the criterion provided by the flow rule.
            } else {
                return packet.criteria().contains(criterion);
            }
        });
    }
}
