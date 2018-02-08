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
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.InterfaceConfig;
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
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
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
import static org.onosproject.net.flow.TrafficSelector.Builder;
import static org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsHeaderInstruction;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
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

    static final String PACKET_TO_CONTROLLER = "Packet goes to the controller";

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Override
    public StaticPacketTrace trace(HostId sourceHost, HostId destinationHost, EtherType etherType) {
        Host source = hostService.getHost(sourceHost);
        Host destination = hostService.getHost(destinationHost);

        //Temporary trace to fail in case we don't have neough information or what is provided is incoherent
        StaticPacketTrace failTrace = new StaticPacketTrace(null, null);

        if (source == null) {
            failTrace.addResultMessage("Source Host " + sourceHost + " does not exist");
            return failTrace;
        }

        if (destination == null) {
            failTrace.addResultMessage("Destination Host " + destinationHost + " does not exist");
            return failTrace;
        }

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                .matchInPort(source.location().port())
                .matchEthType(etherType.ethType().toShort())
                .matchEthDst(source.mac())
                .matchVlanId(source.vlan());



        try {
            //if the location deviceId is the same, the two hosts are under same subnet and vlan on the interface
            // we are under same leaf so it's L2 Unicast.
            if (areBridged(source, destination)) {
                selectorBuilder.matchEthDst(destination.mac());
                return trace(selectorBuilder.build(), source.location());
            }

            //handle the IPs for src and dst in case of L3
            if (etherType.equals(EtherType.IPV4) || etherType.equals(EtherType.IPV6)) {

                //Match on the source IP
                if (!matchIP(source, failTrace, selectorBuilder, etherType, true)) {
                    return failTrace;
                }

                //Match on destination IP
                if (!matchIP(destination, failTrace, selectorBuilder, etherType, false)) {
                    return failTrace;
                }

            } else {
                failTrace.addResultMessage("Host based trace supports only IPv4 or IPv6 as EtherType, " +
                        "please use packet based");
                return failTrace;
            }

            //l3 unicast, we get the dst mac of the leaf the source is connected to from netcfg
            SegmentRoutingDeviceConfig segmentRoutingConfig = networkConfigService.getConfig(source.location()
                    .deviceId(), SegmentRoutingDeviceConfig.class);
            if (segmentRoutingConfig != null) {
                selectorBuilder.matchEthDst(segmentRoutingConfig.routerMac());
            } else {
                failTrace.addResultMessage("Can't get " + source.location().deviceId() +
                        " router MAC from segment routing config can't perform L3 tracing.");
            }

            return trace(selectorBuilder.build(), source.location());

        } catch (ConfigException e) {
            failTrace.addResultMessage("Can't get config " + e.getMessage());
            return failTrace;
        }
    }

    /**
     * Matches src and dst IPs based on host information.
     *
     * @param host            the host
     * @param failTrace       the trace to use in case of failure
     * @param selectorBuilder the packet we are building to trace
     * @param etherType       the traffic type
     * @param src             is this src host or dst host
     * @return true if properly matched
     */
    private boolean matchIP(Host host, StaticPacketTrace failTrace, Builder selectorBuilder,
                            EtherType etherType, boolean src) {
        List<IpAddress> ips = host.ipAddresses().stream().filter(ipAddress -> {
            if (etherType.equals(EtherType.IPV4)) {
                return ipAddress.isIp4() && !ipAddress.isLinkLocal();
            } else if (etherType.equals(EtherType.IPV6)) {
                return ipAddress.isIp6() && !ipAddress.isLinkLocal();
            }
            return false;
        }).collect(Collectors.toList());

        if (ips.size() > 0) {
            if (src) {
                selectorBuilder.matchIPSrc(ips.get(0).toIpPrefix());
            } else {
                selectorBuilder.matchIPDst(ips.get(0).toIpPrefix());
            }
        } else {
            failTrace.addResultMessage("Host " + host + " has no " + etherType + " address");
            return false;
        }
        return true;
    }

    /**
     * Checks that two hosts are bridged (L2Unicast).
     *
     * @param source      the source host
     * @param destination the destination host
     * @return true if bridged.
     * @throws ConfigException if config can't be properly retrieved
     */
    private boolean areBridged(Host source, Host destination) throws ConfigException {

        //If the location is not the same we don't even check vlan or subnets
        if (!source.location().deviceId().equals(destination.location().deviceId())) {
            return false;
        }

        InterfaceConfig interfaceCfgH1 = networkConfigService.getConfig(source.location(), InterfaceConfig.class);
        InterfaceConfig interfaceCfgH2 = networkConfigService.getConfig(destination.location(), InterfaceConfig.class);
        if (interfaceCfgH1 != null && interfaceCfgH2 != null) {

            //following can be optimized but for clarity is left as is
            Interface intfH1 = interfaceCfgH1.getInterfaces().stream().findFirst().get();
            Interface intfH2 = interfaceCfgH2.getInterfaces().stream().findFirst().get();

            if (!intfH1.vlanNative().equals(intfH2.vlanNative())) {
                return false;
            }

            if (!intfH1.vlanTagged().equals(intfH2.vlanTagged())) {
                return false;
            }

            if (!intfH1.vlanUntagged().equals(intfH2.vlanUntagged())) {
                return false;
            }

            List<InterfaceIpAddress> intersection = new ArrayList<>(intfH1.ipAddressesList());
            intersection.retainAll(intfH2.ipAddressesList());
            if (intersection.size() == 0) {
                return false;
            }
        }
        return true;
    }

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
                trace.addResultMessage(PACKET_TO_CONTROLLER + " " + master.id());
                computePath(completePath, trace, outputPath.getOutput());
                handleVlanToController(outputPath, trace);

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
                EthTypeCriterion ethTypeCriterion = (EthTypeCriterion) trace.getInitialPacket()
                        .getCriterion(Criterion.Type.ETH_TYPE);
                //We treat as correct output only if it's not LLDP or BDDP
                if (!(ethTypeCriterion.ethType().equals(EtherType.LLDP.ethType())
                        || !ethTypeCriterion.ethType().equals(EtherType.BDDP.ethType()))) {
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
                }

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
     * If the initial packet comes tagged with a Vlan we output it with that to ONOS.
     * If ONOS applied a vlan we remove it.
     *
     * @param outputPath the output
     * @param trace      the trace we are building
     */
    private void handleVlanToController(GroupsInDevice outputPath, StaticPacketTrace trace) {

        VlanIdCriterion initialVid = (VlanIdCriterion) trace.getInitialPacket().getCriterion(Criterion.Type.VLAN_VID);
        VlanIdCriterion finalVid = (VlanIdCriterion) outputPath.getFinalPacket().getCriterion(Criterion.Type.VLAN_VID);

        if (initialVid != null && !initialVid.equals(finalVid) && initialVid.vlanId().equals(VlanId.NONE)) {

            Set<Criterion> finalCriteria = new HashSet<>(outputPath.getFinalPacket().criteria());
            //removing the final vlanId
            finalCriteria.remove(finalVid);
            Builder packetUpdated = DefaultTrafficSelector.builder();
            finalCriteria.forEach(packetUpdated::add);
            //Initial was none so we set it to that
            packetUpdated.add(Criteria.matchVlanId(VlanId.NONE));
            //Update final packet
            outputPath.setFinalPacket(packetUpdated.build());
        }
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

        //handle when the input is the controller
        //NOTE, we are using the input port as a convenience to carry the CONTROLLER port number even if
        // a packet in from the controller will not actually traverse the pipeline and have no such notion
        // as the input port.
        if (in.port().equals(PortNumber.CONTROLLER)) {
            StaticPacketTrace outputTrace = inputFromController(trace, in);
            if (outputTrace != null) {
                return trace;
            }
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

                //On table 10 OFDPA needs two rules to apply the vlan if none and then to transition to the next table.
                if (needsSecondTable10Flow(flowEntry, isOfdpaHardware)) {

                    //Let's get the packet vlanId instruction
                    VlanIdCriterion packetVlanIdCriterion =
                            (VlanIdCriterion) packet.getCriterion(Criterion.Type.VLAN_VID);

                    //Let's get the flow entry vlan mod instructions
                    ModVlanIdInstruction entryModVlanIdInstruction = (ModVlanIdInstruction) flowEntry.treatment()
                            .immediate().stream()
                            .filter(instruction -> instruction instanceof ModVlanIdInstruction)
                            .findFirst().orElse(null);

                    //If the entry modVlan is not null we need to make sure that the packet has been updated and there
                    // is a flow rule that matches on same criteria and with updated vlanId
                    if (entryModVlanIdInstruction != null) {

                        FlowEntry secondVlanFlow = getSecondFlowEntryOnTable10(packet, in,
                                packetVlanIdCriterion, entryModVlanIdInstruction);

                        //We found the flow that we expected
                        if (secondVlanFlow != null) {
                            flows.add(secondVlanFlow);
                        } else {
                            trace.addResultMessage("Missing forwarding rule for tagged packet on " + in);
                            return trace;
                        }
                    }

                }

            }
        }

        //Creating a modifiable builder for the output packet
        Builder builder = DefaultTrafficSelector.builder();
        packet.criteria().forEach(builder::add);

        //Adding all the flows to the trace
        trace.addFlowsForDevice(in.deviceId(), ImmutableList.copyOf(flows));

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

        log.debug("Output Packet {}", packet);
        return trace;
    }

    /**
     * Handles the specific case where the Input is the controller.
     * Note that the in port is used as a convenience to store the port of the controller even if the packet in
     * from a controller should not have a physical input port. The in port from the Controller is used to make sure
     * the flood to all active physical ports of the device.
     *
     * @param trace the trace
     * @param in    the controller port
     * @return the augmented trace.
     */
    private StaticPacketTrace inputFromController(StaticPacketTrace trace, ConnectPoint in) {
        EthTypeCriterion ethTypeCriterion = (EthTypeCriterion) trace.getInitialPacket()
                .getCriterion(Criterion.Type.ETH_TYPE);
        //If the packet is LLDP or BDDP we flood it on all active ports of the switch.
        if (ethTypeCriterion != null && (ethTypeCriterion.ethType().equals(EtherType.LLDP.ethType())
                || ethTypeCriterion.ethType().equals(EtherType.BDDP.ethType()))) {
            //get the active ports
            List<Port> enabledPorts = deviceService.getPorts(in.deviceId()).stream()
                    .filter(Port::isEnabled)
                    .collect(Collectors.toList());
            //build an output from each one
            enabledPorts.forEach(port -> {
                GroupsInDevice output = new GroupsInDevice(new ConnectPoint(port.element().id(), port.number()),
                        ImmutableList.of(), trace.getInitialPacket());
                trace.addGroupOutputPath(in.deviceId(), output);
            });
            return trace;
        }
        return null;
    }

    private boolean needsSecondTable10Flow(FlowEntry flowEntry, boolean isOfdpaHardware) {
        return isOfdpaHardware && flowEntry.table().equals(IndexTableId.of(10))
                && flowEntry.selector().getCriterion(Criterion.Type.VLAN_VID) != null
                && ((VlanIdCriterion) flowEntry.selector().getCriterion(Criterion.Type.VLAN_VID))
                .vlanId().equals(VlanId.NONE);
    }

    /**
     * Method that finds a flow rule on table 10 that matches the packet and the VLAN of the already
     * found rule on table 10. This is because OFDPA needs two rules on table 10, first to apply the rule,
     * second to transition to following table
     *
     * @param packet                    the incoming packet
     * @param in                        the input connect point
     * @param packetVlanIdCriterion     the vlan criterion from the packet
     * @param entryModVlanIdInstruction the entry vlan instruction
     * @return the second flow entry that matched
     */
    private FlowEntry getSecondFlowEntryOnTable10(TrafficSelector packet, ConnectPoint in,
                                                  VlanIdCriterion packetVlanIdCriterion,
                                                  ModVlanIdInstruction entryModVlanIdInstruction) {
        FlowEntry secondVlanFlow = null;
        //Check the packet has been update from the first rule.
        if (packetVlanIdCriterion.vlanId().equals(entryModVlanIdInstruction.vlanId())) {
            //find a rule on the same table that matches the vlan and
            // also all the other elements of the flow such as input port
            secondVlanFlow = Lists.newArrayList(flowRuleService.getFlowEntries(in.deviceId()).iterator())
                    .stream()
                    .filter(entry -> {
                        return entry.table().equals(IndexTableId.of(10));
                    })
                    .filter(entry -> {
                        VlanIdCriterion criterion = (VlanIdCriterion) entry.selector()
                                .getCriterion(Criterion.Type.VLAN_VID);
                        return criterion != null && match(packet, entry)
                                && criterion.vlanId().equals(entryModVlanIdInstruction.vlanId());
                    }).findFirst().orElse(null);

        }
        return secondVlanFlow;
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

        //if the output is the input same we drop, except if the output is the controller
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
                        ModVlanIdInstruction vlanIdInstruction =
                                (ModVlanIdInstruction) instruction;
                        VlanId id = vlanIdInstruction.vlanId();
                        criterion = Criteria.matchVlanId(id);
                        break;
                    case VLAN_POP:
                        criterion = Criteria.matchVlanId(VlanId.NONE);
                        break;
                    case MPLS_PUSH:
                        ModMplsHeaderInstruction mplsEthInstruction =
                                (ModMplsHeaderInstruction) instruction;
                        criterion = Criteria.matchEthType(mplsEthInstruction.ethernetType().toShort());
                        break;
                    case MPLS_POP:
                        ModMplsHeaderInstruction mplsPopInstruction =
                                (ModMplsHeaderInstruction) instruction;
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
                        ModMplsLabelInstruction mplsLabelInstruction =
                                (ModMplsLabelInstruction) instruction;
                        criterion = Criteria.matchMplsLabel(mplsLabelInstruction.label());
                        newSelector.matchMplsBos(true);
                        break;
                    case ETH_DST:
                        ModEtherInstruction modEtherDstInstruction =
                                (ModEtherInstruction) instruction;
                        criterion = Criteria.matchEthDst(modEtherDstInstruction.mac());
                        break;
                    case ETH_SRC:
                        ModEtherInstruction modEtherSrcInstruction =
                                (ModEtherInstruction) instruction;
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
