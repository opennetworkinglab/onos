/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.fnl.impl;

import org.onlab.packet.IpPrefix;
import org.onosproject.fnl.intf.NetworkAnomaly;
import org.onosproject.fnl.intf.NetworkDiagnostic;
import org.onosproject.fnl.base.TsLoopPacket;
import org.onosproject.fnl.base.TsReturn;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onlab.packet.EthType.EtherType.VLAN;
import static org.onosproject.fnl.base.NetworkDiagnosticUtils.isDevice;
import static org.onosproject.fnl.base.NetworkDiagnosticUtils.sortCriteria;
import static org.onosproject.fnl.base.NetworkDiagnosticUtils.sortFlowTable;
import static org.onosproject.fnl.base.TsLoopPacket.matchBuilder;
import static org.onosproject.fnl.intf.NetworkDiagnostic.Type.LOOP;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.net.flow.criteria.Criteria.matchInPort;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_TYPE;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PORT;
import static org.onosproject.net.flow.criteria.Criterion.Type.IP_PROTO;

/**
 * Loop Checking Diagnostic Implementation.
 *
 * Strategy Pattern.
 */
public class DefaultCheckLoop implements NetworkDiagnostic {

    private static final int IP_PROTO_TCP_TS = 6;
    private static final int IP_PROTO_UDP_TS = 17;

    private static final String E_CANNOT_HANDLE = "can not handle {} now.";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DeviceService deviceService;
    private final HostService hostService;
    private final FlowRuleService flowRuleService;
    private final LinkService linkService;


    private Map<DeviceId, Device> deviceInfo;
    private Map<DeviceId, Iterable<FlowEntry>> flowEntryInfo;
    private Set<Device> accessDevices;

    //conventionally used by tsGetEgressLinks()
    private Map<DeviceId, Set<Link>> egressLinkInfo;

    //Two below are hot data in checking process.
    private Set<NetworkAnomaly> loops;
    private Set<DeviceId> excludeDeviceId;

    /**
     * Creates and returns an instance of Loop Checking algorithm module.
     *
     * @param ds reference of DeviceService
     * @param hs reference of HostService
     * @param frs reference of FlowRuleService
     * @param ls reference of LinkService
     */
    public DefaultCheckLoop(DeviceService ds,
                            HostService hs,
                            FlowRuleService frs,
                            LinkService ls) {
        checkNotNull(ds, "DeviceService cannot be null");
        checkNotNull(hs, "HostService cannot be null");
        checkNotNull(frs, "FlowRuleService cannot be null");
        checkNotNull(ls, "LinkService  cannot be null");

        deviceService = ds;
        hostService = hs;
        flowRuleService = frs;
        linkService = ls;
    }

    /**
     * Checks for loops and returns any that were found.
     * An empty set is returned if there are no loops.
     *
     * @return the set of loops; may be empty
     */
    @Override
    public Set<NetworkAnomaly> findAnomalies() {
        return findLoop();
    }

    @Override
    public Type type() {
        return LOOP;
    }

    /**
     * Enter of the loop checking algorithm.
     *
     * @return the set of loop results; empty, if there is no loop
     */
    private Set<NetworkAnomaly> findLoop() {

        getNetworkSnapshot();

        loops = new HashSet<>();
        excludeDeviceId = new HashSet<>();

        for (Device device : accessDevices) {
            if (excludeDeviceId.contains(device.id())) {
                continue;
            }


            List<FlowEntry> availableFlowEntries = new ArrayList<>();

            flowEntryInfo.get(device.id()).forEach(flowEntry -> {
                if (flowEntry.state() == ADDED) {
                    availableFlowEntries.add(flowEntry);
                }
            });

            List<FlowEntry> sortedFlowEntries = sortFlowTable(availableFlowEntries);


            for (FlowEntry flow : sortedFlowEntries) {

                TsLoopPacket pkt =
                        matchBuilder(flow.selector().criteria(), null);

                pkt.pushPathFlow(flow);

                List<Instruction> inst = flow.treatment().immediate();

                for (Instruction instOne : inst) {
                    // Attention !!!
                    // if you would like to modify the code here,
                    // please MAKE VERY SURE that you are clear with
                    // the relationship of any invoked methods, and
                    // the relationship of params which are passed in and out.
                    processOneInstruction(instOne, device.id(),
                            null, pkt, true, flow);
                }
            }
        }

        // TODO - avoid two-hop LOOP

        // TODO - another clean operations

        return loops;
    }

    /**
     * Iterate one by one at switch hops.
     * Return whether we discover a Loop now or not.
     *
     * When flows form a loop,
     * pkt is also a return value indicating the loop header.
     *
     * @param deviceId the device needed to be checked
     * @param pkt virtual packet forwarded by switches
     * @return true if a loop is discovered
     */
    private boolean matchDeviceFlows(DeviceId deviceId, TsLoopPacket pkt) {
        if (pkt.isPassedDevice(deviceId)) {
            return true; // Attention: pkt should be held outside
        }


        List<FlowEntry> availableFlowEntries = new ArrayList<>();

        flowEntryInfo.get(deviceId).forEach(flowEntry -> {
            if (flowEntry.state() == ADDED) {
                availableFlowEntries.add(flowEntry);
            }
        });

        List<FlowEntry> sortedFlowEntries = sortFlowTable(availableFlowEntries);


        for (FlowEntry flowEntry : sortedFlowEntries) {
            TsReturn<Boolean> isBigger = new TsReturn<>();
            TsLoopPacket newPkt = pkt.copyPacketMatch();

            if (!matchAndAddFlowEntry(flowEntry, newPkt, isBigger)) {
                continue;
            }

            newPkt.pushPathFlow(flowEntry);
            // no need to popPathFlow(),
            // because we will drop this newPkt, and copy pkt again

            for (Instruction instOne : flowEntry.treatment().immediate()) {
                // Attention !!!
                // if you would like to modify the code here,
                // please MAKE VERY SURE that you are clear with
                // the relationship of any invoked methods, and
                // the relationship of params which are passed in and out.
                if (processOneInstruction(instOne, deviceId,
                        pkt, newPkt, false, null)) {
                    return true;
                }
            }

            newPkt.popPathFlow();

            if (!isBigger.getValue()) {
                break;
            }
        }
        return false;
    }

    /**
     * Process one of every instructions.
     *
     * The isFindLoop should be true if it is invoked by findLoop method,
     * and be false if it is invoked by matchDeviceFlows method.
     *
     * @param instOne the instruction to be processed
     * @param currentDeviceId id of the device we are now in
     * @param initPkt the packet before being copied
     * @param matchedPkt the packet which matched the flow entry,
     *                   to which this instruction belongs
     * @param isFindLoop indicate if it is invoked by findLoop method
     * @param firstEntry the flow entry from which the packet is generated
     * @return true, if a loop is discovered;
     *         false, 1. invoked by matchDeviceFlows method, and detected no loop;
     *                2. invoked by findLoop method
     */
    private boolean processOneInstruction(Instruction instOne,
                                          DeviceId currentDeviceId,
                                          TsLoopPacket initPkt,
                                          TsLoopPacket matchedPkt,
                                          boolean isFindLoop,
                                          FlowEntry firstEntry) {
        if (isFindLoop) {
            checkArgument(initPkt == null,
                    "initPkt is not null, while isFindLoop is true.");
        } else {
            checkArgument(firstEntry == null,
                    "firstEntry is not null, while isFindLoop is false.");
        }


        Instruction.Type type = instOne.type();
        switch (type) {
            case L2MODIFICATION:
                //TODO - modify the L2 header of virtual packet
                log.warn(E_CANNOT_HANDLE, type);
                break;
            case L3MODIFICATION:
                //TODO - modify the L3 header of virtual packet
                log.warn(E_CANNOT_HANDLE, type);
                break;
            case L4MODIFICATION:
                //TODO - modify the L4 header of virtual packet
                log.warn(E_CANNOT_HANDLE, type);
                break;
            case OUTPUT:
                if (processOneOutputInstruction(instOne, currentDeviceId,
                        initPkt, matchedPkt, isFindLoop, firstEntry)) {
                    return true;
                }
                break;
            default:
                log.error("Can't process this type of instruction: {} now.",
                        instOne.type());
                break;
        }
        return false;
    }

    /**
     * Process one output instruction.
     *
     * Params are passed from processOneInstruction directly,
     * and obey the same rules.
     *
     * @param instOne the instruction to be processed
     * @param currentDeviceId id of the device we are now in
     * @param initPkt the packet before being copied
     * @param matchedPkt the packet which matched the flow entry,
     *                   to which this instruction belongs
     * @param isFindLoop indicate if it is invoked by findLoop method
     * @param firstEntry the flow entry from which the packet is generated
     * @return true, if a loop is discovered;
     *         false, 1. invoked by matchDeviceFlows method, and detected no loop;
     *                2. invoked by findLoop method
     */
    private boolean processOneOutputInstruction(Instruction instOne,
                                                DeviceId currentDeviceId,
                                                TsLoopPacket initPkt,
                                                TsLoopPacket matchedPkt,
                                                boolean isFindLoop,
                                                FlowEntry firstEntry) {
        OutputInstruction instOutput = (OutputInstruction) instOne;
        PortNumber instPort = instOutput.port();

        if (!instPort.isLogical()) {
            // single OUTPUT - NIC or normal port

            Set<Link> dstLink = tsGetEgressLinks(
                    new ConnectPoint(currentDeviceId, instPort));
            if (!dstLink.isEmpty()) {

                // TODO - now, just deal with the first destination.
                // will there be more destinations?

                Link dstThisLink = dstLink.iterator().next();
                ConnectPoint dstPoint = dstThisLink.dst();

                // check output to devices only (output to a host is normal)
                if (isDevice(dstPoint)) {
                    PortCriterion oldInPort =
                            updatePktInportPerHop(matchedPkt, dstPoint);
                    matchedPkt.pushPathLink(dstThisLink);
                    TsLoopPacket newNewPkt = matchedPkt.copyPacketMatch();

                    boolean loopFound =
                            matchDeviceFlows(dstPoint.deviceId(), newNewPkt);
                    if (isFindLoop) {
                        if (loopFound) {
                            loops.add(newNewPkt);
                            updateExcludeDeviceSet(newNewPkt);
                        }
                        matchedPkt.resetLinkFlow(firstEntry);
                    } else {
                        if (loopFound) {
                            initPkt.handInLoopMatch(newNewPkt);
                            return true;
                        }
                        matchedPkt.popPathLink();
                    }
                    restorePktInportPerHop(matchedPkt, oldInPort);
                }
            } else {
                if (!isFindLoop) {
                    //TODO - NEED
                    log.warn("no link connecting at device {}, port {}",
                            currentDeviceId, instPort);
                }
            }
        } else if (instPort.equals(PortNumber.IN_PORT)) {
            //TODO - in the future,
            // we may need to resolve this condition 1
            log.warn("can not handle {} port now.", PortNumber.IN_PORT);
        } else if (instPort.equals(PortNumber.NORMAL) ||
                instPort.equals(PortNumber.FLOOD) ||
                instPort.equals(PortNumber.ALL)) {
            //TODO - in the future,
            // we may need to resolve this condition 2
            log.warn("can not handle {}/{}/{} now.",
                    PortNumber.NORMAL, PortNumber.FLOOD, PortNumber.ALL);
        }
        return false;
    }

    private void updateExcludeDeviceSet(TsLoopPacket loopPkt) {
        Iterator<Link> iter = loopPkt.getPathLink();
        while (iter.hasNext()) {
            excludeDeviceId.add(iter.next().src().deviceId());
        }
    }

    private PortCriterion updatePktInportPerHop(TsLoopPacket oldPkt,
                                                ConnectPoint dstPoint) {

        PortCriterion inPort = oldPkt.getInport();
        PortCriterion oldInPort =
                null != inPort ? (PortCriterion) matchInPort(inPort.port()) : null;
        // TODO - check - if it really copies this object

        oldPkt.setHeader(matchInPort(dstPoint.port()));

        return oldInPort;
    }

    private void restorePktInportPerHop(TsLoopPacket pkt,
                                        PortCriterion oldInPort) {
        if (oldInPort == null) {
            pkt.delHeader(IN_PORT);
        } else {
            pkt.setHeader(oldInPort);
        }
    }

    private void getNetworkSnapshot() {
        deviceInfo = new HashMap<>();
        deviceService.getDevices().forEach(d -> deviceInfo.put(d.id(), d));

        flowEntryInfo = new HashMap<>();
        deviceInfo.keySet().forEach(id ->
                flowEntryInfo.put(id, flowRuleService.getFlowEntries(id)));

        egressLinkInfo = new HashMap<>();
        deviceInfo.keySet().forEach(id ->
                egressLinkInfo.put(id, linkService.getDeviceEgressLinks(id)));

        accessDevices = new HashSet<>();
        hostService.getHosts().forEach(h ->
                accessDevices.add(deviceInfo.get(h.location().deviceId())));
    }

    private Set<Link> tsGetEgressLinks(ConnectPoint point) {
        Set<Link> portEgressLink = new HashSet<>();
        DeviceId deviceId = point.deviceId();

        portEgressLink.addAll(
                //all egress links
                egressLinkInfo.get(deviceId)
                        .stream()
                        .filter(l -> l.src().equals(point))
                        .collect(Collectors.toList()));

        return portEgressLink;
    }

    private boolean matchAndAddFlowEntry(FlowEntry flowEntry,
                                         TsLoopPacket pkt,
                                         TsReturn<Boolean> isBigger) {
        isBigger.setValue(false);

        List<Criterion> criterionArray =
                sortCriteria(flowEntry.selector().criteria());

        for (Criterion criterion : criterionArray) {
            // TODO - advance
            switch (criterion.type()) {
                case IN_PORT:
                case ETH_SRC:
                    // At present, not support Ethernet mask (ONOS?)
                case ETH_DST:
                    // At present, not support Ethernet mask (ONOS?)
                case ETH_TYPE:
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case VLAN_VID:
                    // At present, not support VLAN mask (ONOS?)
                case VLAN_PCP:
                    if (!pkt.headerExists(ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(ETH_TYPE))
                                    .ethType().equals(VLAN.ethType())) {
                        return false;
                    }
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IPV4_SRC:
                case IPV4_DST:
                    if (!pkt.headerExists(ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(ETH_TYPE))
                                    .ethType().equals(IPV4.ethType())) {
                        return false;
                    }
                    if (!matchAddIPV4(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_PROTO:
                    if (!pkt.headerExists(ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(ETH_TYPE))
                                    .ethType().equals(IPV4.ethType())) {
                        return false;
                    }
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_DSCP:
                    // TODO: 10/28/16 support IP_DSCP match field
                    break;
                case IP_ECN:
                    // TODO: 10/28/16 support IP_DSCP match field
                    break;
                case TCP_SRC:
                case TCP_DST:
                    if (!pkt.headerExists(IP_PROTO) ||
                            IP_PROTO_TCP_TS !=
                                    ((IPProtocolCriterion)
                                            pkt.getHeader(IP_PROTO))
                                            .protocol()
                            ) {
                        // has TCP match requirement, but can't afford TCP
                        return false;
                    }
                    // in this "for" loop
                    // avoid IP_PROTO locates after TCP_*
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case UDP_SRC:
                case UDP_DST:
                    if (!pkt.headerExists(IP_PROTO) ||
                            IP_PROTO_UDP_TS !=
                                    ((IPProtocolCriterion)
                                            pkt.getHeader(IP_PROTO))
                                            .protocol()
                            ) {
                        // has UDP match requirement, but can't afford UDP
                        return false;
                    }
                    // in this "for" loop
                    // avoid IP_PROTO locates after UDP_*
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;

                default:
                    log.debug("{} can't be supported by OF1.0",
                            criterion.type());
                    return false;
            }
        }
        return true;
    }

    private boolean matchAddExactly(TsLoopPacket pkt,
                                    Criterion criterion,
                                    TsReturn<Boolean> isBigger) {

        if (pkt.headerExists(criterion.type())) {
            if (!pkt.getHeader(criterion.type()).equals(criterion)) {
                return false;
            }

        } else {
            // TODO - check if it is IN_PORT or IN_PHY_PORT, should be strict
            pkt.setHeader(criterion);
            isBigger.setValue(true);
        }

        return true; // should put it here
    }

    // before invoking this, MUST insure EtherType is IPv4.
    private boolean matchAddIPV4(TsLoopPacket pkt,
                                 Criterion criterion,
                                 TsReturn<Boolean> isBigger) {

        if (pkt.headerExists(criterion.type())) {

            IpPrefix ipFlow = ((IPCriterion) criterion).ip();
            IpPrefix ipPkt =
                    ((IPCriterion) pkt.getHeader(criterion.type())).ip();

            // attention - the order below is important
            if (ipFlow.equals(ipPkt)) {
                // shoot

            } else if (ipFlow.contains(ipPkt)) {
                // shoot, pkt is more exact than flowEntry

            } else if (ipPkt.contains(ipFlow)) {
                // pkt should be changed to be more exact
                pkt.setHeader(criterion);
                isBigger.setValue(true);
            } else {
                // match fail
                return false;
            }

        } else {
            // attention the order of criteria in "for" loop
            pkt.setHeader(criterion);
            isBigger.setValue(true);
        }

        return true;
    }
}
