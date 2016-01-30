/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.openstackswitching.OpenstackNetwork;
import org.onosproject.openstackswitching.OpenstackSubnet;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PORT;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates rules for CORD VTN service.
 */
public class CordVtnRuleInstaller {

    protected final Logger log = getLogger(getClass());

    private static final int TABLE_FIRST = 0;
    private static final int TABLE_IN_PORT = 1;
    private static final int TABLE_ACCESS_TYPE = 2;
    private static final int TABLE_IN_SERVICE = 3;
    private static final int TABLE_DST_IP = 4;
    private static final int TABLE_TUNNEL_IN = 5;

    private static final int MANAGEMENT_PRIORITY = 55000;
    private static final int HIGH_PRIORITY = 50000;
    private static final int DEFAULT_PRIORITY = 5000;
    private static final int LOW_PRIORITY = 4000;
    private static final int LOWEST_PRIORITY = 0;

    private static final int VXLAN_UDP_PORT = 4789;

    private final ApplicationId appId;
    private final FlowRuleService flowRuleService;
    private final DeviceService deviceService;
    private final DriverService driverService;
    private final GroupService groupService;
    private final MastershipService mastershipService;
    private final String tunnelType;

    /**
     * Creates a new rule populator.
     *
     * @param appId application id
     * @param flowRuleService flow rule service
     * @param deviceService device service
     * @param driverService driver service
     * @param groupService group service
     * @param mastershipService mastership service
     * @param tunnelType tunnel type
     */
    public CordVtnRuleInstaller(ApplicationId appId,
                                FlowRuleService flowRuleService,
                                DeviceService deviceService,
                                DriverService driverService,
                                GroupService groupService,
                                MastershipService mastershipService,
                                String tunnelType) {
        this.appId = appId;
        this.flowRuleService = flowRuleService;
        this.deviceService = deviceService;
        this.driverService = driverService;
        this.groupService = groupService;
        this.mastershipService = mastershipService;
        this.tunnelType = checkNotNull(tunnelType);
    }

    /**
     * Installs table miss rule to a give device.
     *
     * @param deviceId device id to install the rules
     * @param phyPortName physical port name
     * @param localIp local data plane ip address
     */
    public void init(DeviceId deviceId, String phyPortName, IpAddress localIp) {
        // default is drop packets which can be accomplished without
        // a table miss entry for all table.
        PortNumber tunnelPort = getTunnelPort(deviceId);
        PortNumber phyPort = getPhyPort(deviceId, phyPortName);

        processFirstTable(deviceId, phyPort, localIp);
        processInPortTable(deviceId, tunnelPort, phyPort);
        processAccessTypeTable(deviceId, phyPort);
    }

    /**
     * Populates basic rules that connect a VM to the other VMs in the system.
     *
     * @param host host
     * @param tunnelIp tunnel ip
     * @param vNet openstack network
     */
    public void populateBasicConnectionRules(Host host, IpAddress tunnelIp, OpenstackNetwork vNet) {
        checkNotNull(host);
        checkNotNull(vNet);

        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }

        PortNumber inPort = host.location().port();
        MacAddress dstMac = host.mac();
        IpAddress hostIp = host.ipAddresses().stream().findFirst().get();
        long tunnelId = Long.parseLong(vNet.segmentId());

        OpenstackSubnet subnet = vNet.subnets().stream()
                .findFirst()
                .orElse(null);

        if (subnet == null) {
            log.error("Failed to get subnet for {}", host.id());
            return;
        }

        populateLocalInPortRule(deviceId, inPort, hostIp);
        populateDirectAccessRule(Ip4Prefix.valueOf(subnet.cidr()), Ip4Prefix.valueOf(subnet.cidr()));
        populateDstIpRule(deviceId, inPort, dstMac, hostIp, tunnelId, tunnelIp);
        populateTunnelInRule(deviceId, inPort, dstMac, tunnelId);
    }

    /**
     * Removes basic rules related to a given flow information.
     *
     * @param host host to be removed
     */
    public void removeBasicConnectionRules(Host host) {
        checkNotNull(host);

        DeviceId deviceId = host.location().deviceId();
        MacAddress mac = host.mac();
        PortNumber port = host.location().port();
        IpAddress ip = host.ipAddresses().stream().findFirst().orElse(null);

        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }

        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {
            if (flowRule.deviceId().equals(deviceId)) {
                PortNumber inPort = getInPort(flowRule);
                if (inPort != null && inPort.equals(port)) {
                    processFlowRule(false, flowRule);
                    continue;
                }
            }

            MacAddress dstMac = getDstMacFromTreatment(flowRule);
            if (dstMac != null && dstMac.equals(mac)) {
                processFlowRule(false, flowRule);
                continue;
            }

            dstMac = getDstMacFromSelector(flowRule);
            if (dstMac != null && dstMac.equals(mac)) {
                processFlowRule(false, flowRule);
                continue;
            }

            IpPrefix dstIp = getDstIpFromSelector(flowRule);
            if (dstIp != null && dstIp.equals(ip.toIpPrefix())) {
                processFlowRule(false, flowRule);
            }
        }

        // TODO uninstall same network access rule in access table if no vm exists in the network
    }

    /**
     * Populates service dependency rules.
     *
     * @param tService tenant cord service
     * @param pService provider cord service
     */
    public void populateServiceDependencyRules(CordService tService, CordService pService) {
        checkNotNull(tService);
        checkNotNull(pService);

        Ip4Prefix srcRange = tService.serviceIpRange().getIp4Prefix();
        Ip4Prefix dstRange = pService.serviceIpRange().getIp4Prefix();
        Ip4Address serviceIp = pService.serviceIp().getIp4Address();

        Map<DeviceId, GroupId> outGroups = Maps.newHashMap();
        Map<DeviceId, Set<PortNumber>> inPorts = Maps.newHashMap();

        for (Device device : deviceService.getAvailableDevices(SWITCH)) {
            GroupId groupId = createServiceGroup(device.id(), pService);
            outGroups.put(device.id(), groupId);

            Set<PortNumber> vms = tService.hosts().keySet()
                    .stream()
                    .filter(host -> host.location().deviceId().equals(device.id()))
                    .map(host -> host.location().port())
                    .collect(Collectors.toSet());
            inPorts.put(device.id(), vms);
        }

        populateIndirectAccessRule(srcRange, serviceIp, outGroups);
        populateDirectAccessRule(srcRange, dstRange);
        populateInServiceRule(inPorts, outGroups);
    }

    /**
     * Removes service dependency rules.
     *
     * @param tService tenant cord service
     * @param pService provider cord service
     */
    public void removeServiceDependencyRules(CordService tService, CordService pService) {
        checkNotNull(tService);
        checkNotNull(pService);

        Ip4Prefix srcRange = tService.serviceIpRange().getIp4Prefix();
        Ip4Prefix dstRange = pService.serviceIpRange().getIp4Prefix();
        IpPrefix serviceIp = pService.serviceIp().toIpPrefix();

        Map<DeviceId, GroupId> outGroups = Maps.newHashMap();
        GroupKey groupKey = new DefaultGroupKey(pService.id().id().getBytes());

        deviceService.getAvailableDevices(SWITCH).forEach(device -> {
            Group group = groupService.getGroup(device.id(), groupKey);
            if (group != null) {
                outGroups.put(device.id(), group.id());
            }
        });

        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {
            IpPrefix dstIp = getDstIpFromSelector(flowRule);
            IpPrefix srcIp = getSrcIpFromSelector(flowRule);

            if (dstIp != null && dstIp.equals(serviceIp)) {
                processFlowRule(false, flowRule);
                continue;
            }

            if (dstIp != null && srcIp != null) {
                if (dstIp.equals(dstRange) && srcIp.equals(srcRange)) {
                    processFlowRule(false, flowRule);
                    continue;
                }

                if (dstIp.equals(srcRange) && srcIp.equals(dstRange)) {
                    processFlowRule(false, flowRule);
                    continue;
                }
            }

            GroupId groupId = getGroupIdFromTreatment(flowRule);
            if (groupId != null && groupId.equals(outGroups.get(flowRule.deviceId()))) {
                processFlowRule(false, flowRule);
            }
        }

        // TODO remove the group if it is not in use
    }

    /**
     * Updates group buckets for a given service to all devices.
     *
     * @param service cord service
     */
    public void updateServiceGroup(CordService service) {
        checkNotNull(service);

        GroupKey groupKey = getGroupKey(service.id());

        for (Device device : deviceService.getAvailableDevices(SWITCH)) {
            DeviceId deviceId = device.id();
            if (!mastershipService.isLocalMaster(deviceId)) {
                continue;
            }

            Group group = groupService.getGroup(deviceId, groupKey);
            if (group == null) {
                log.trace("No group exists for service {} in {}, do nothing.", service.id(), deviceId);
                continue;
            }

            List<GroupBucket> oldBuckets = group.buckets().buckets();
            List<GroupBucket> newBuckets = getServiceGroupBuckets(
                    deviceId, service.segmentationId(), service.hosts()).buckets();

            if (oldBuckets.equals(newBuckets)) {
                continue;
            }

            List<GroupBucket> bucketsToRemove = new ArrayList<>(oldBuckets);
            bucketsToRemove.removeAll(newBuckets);
            if (!bucketsToRemove.isEmpty()) {
                groupService.removeBucketsFromGroup(
                        deviceId,
                        groupKey,
                        new GroupBuckets(bucketsToRemove),
                        groupKey, appId);
            }

            List<GroupBucket> bucketsToAdd = new ArrayList<>(newBuckets);
            bucketsToAdd.removeAll(oldBuckets);
            if (!bucketsToAdd.isEmpty()) {
                groupService.addBucketsToGroup(
                        deviceId,
                        groupKey,
                        new GroupBuckets(bucketsToAdd),
                        groupKey, appId);
            }
        }
    }

    /**
     * Populates flow rules for management network access.
     *
     * @param host host which has management network interface
     * @param mService management network service
     */
    public void populateManagementNetworkRules(Host host, CordService mService) {
        checkNotNull(mService);

        DeviceId deviceId = host.location().deviceId();
        IpAddress hostIp = host.ipAddresses().stream().findFirst().get();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(mService.serviceIp().getIp4Address())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(MANAGEMENT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(hostIp.getIp4Address())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(host.location().port())
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(MANAGEMENT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(mService.serviceIpRange())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(MANAGEMENT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(mService.serviceIp().toIpPrefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(MANAGEMENT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_ACCESS_TYPE)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Removes management network access rules.
     *
     * @param host host to be removed
     * @param mService service for management network
     */
    public void removeManagementNetworkRules(Host host, CordService mService) {
        checkNotNull(mService);

        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {
            if (flowRule.deviceId().equals(host.location().deviceId())) {
                PortNumber port = getOutputFromTreatment(flowRule);
                if (port != null && port.equals(host.location().port())) {
                    processFlowRule(false, flowRule);
                }
            }

            // TODO remove the other rules if mgmt network is not in use
        }
    }

    /**
     * Populates default rules on the first table.
     * The rules are for shuttling vxlan-encapped packets and supporting physical
     * network connectivity.
     *
     * @param deviceId device id
     * @param phyPort physical port number
     * @param localIp local data plane ip address
     */
    private void processFirstTable(DeviceId deviceId, PortNumber phyPort, IpAddress localIp) {
        // take vxlan packet out onto the physical port
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(phyPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(HIGH_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take a vxlan encap'd packet through the Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(phyPort)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(VXLAN_UDP_PORT))
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(HIGH_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take a packet to the local ip through Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(phyPort)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(localIp.toIpPrefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(HIGH_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take an arp packet from physical through Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(phyPort)
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(localIp.getIp4Address())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(HIGH_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take all else to the next table
        selector = DefaultTrafficSelector.builder()
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_IN_PORT)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(LOWEST_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_FIRST)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Forward table miss packets in ACCESS_TYPE table to physical port.
     *
     * @param deviceId device id
     * @param phyPort physical port number
     */
    private void processAccessTypeTable(DeviceId deviceId, PortNumber phyPort) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(phyPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(LOWEST_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_ACCESS_TYPE)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Populates default rules for IN_PORT table.
     * All packets from tunnel port are forwarded to TUNNEL_ID table and all packets
     * from physical port to ACCESS_TYPE table.
     *
     * @param deviceId device id to install the rules
     * @param tunnelPort tunnel port number
     * @param phyPort physical port number
     */
    private void processInPortTable(DeviceId deviceId, PortNumber tunnelPort, PortNumber phyPort) {
        checkNotNull(tunnelPort);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(tunnelPort)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_TUNNEL_IN)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(phyPort)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Populates rules for local in port in IN_PORT table.
     * Flows from a given in port, whose source IP is service IP transition
     * to DST_TYPE table. Other flows transition to IN_SERVICE table.
     *
     * @param deviceId device id to install the rules
     * @param inPort in port
     * @param srcIp source ip
     */
    private void populateLocalInPortRule(DeviceId deviceId, PortNumber inPort, IpAddress srcIp) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcIp.toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_ACCESS_TYPE)
                .build();


        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_IN_SERVICE)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(LOW_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Populates direct VM access rules for ACCESS_TYPE table.
     * These rules are installed to all devices.
     *
     * @param srcRange source ip range
     * @param dstRange destination ip range
     */
    private void populateDirectAccessRule(Ip4Prefix srcRange, Ip4Prefix dstRange) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcRange)
                .matchIPDst(dstRange)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        for (Device device : deviceService.getAvailableDevices(SWITCH)) {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(LOW_PRIORITY)
                    .forDevice(device.id())
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRuleDirect);
        }
    }

    /**
     * Populates indirect service access rules for ACCESS_TYPE table.
     * These rules are installed to all devices.
     *
     * @param srcRange source range
     * @param serviceIp service ip
     * @param outGroups list of output group
     */
    private void populateIndirectAccessRule(Ip4Prefix srcRange, Ip4Address serviceIp,
                                            Map<DeviceId, GroupId> outGroups) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcRange)
                .matchIPDst(serviceIp.toIpPrefix())
                .build();

        for (Map.Entry<DeviceId, GroupId> outGroup : outGroups.entrySet()) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .group(outGroup.getValue())
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(DEFAULT_PRIORITY)
                    .forDevice(outGroup.getKey())
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRule);
        }
    }

    /**
     * Populates flow rules for IN_SERVICE table.
     *
     * @param inPorts list of inports related to the service for each device
     * @param outGroups set of output groups
     */
    private void populateInServiceRule(Map<DeviceId, Set<PortNumber>> inPorts, Map<DeviceId, GroupId> outGroups) {
        checkNotNull(inPorts);
        checkNotNull(outGroups);

        for (Map.Entry<DeviceId, Set<PortNumber>> entry : inPorts.entrySet()) {
            Set<PortNumber> ports = entry.getValue();
            DeviceId deviceId = entry.getKey();

            GroupId groupId = outGroups.get(deviceId);
            if (groupId == null) {
                continue;
            }

            ports.stream().forEach(port -> {
                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchInPort(port)
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .group(groupId)
                        .build();

                FlowRule flowRule = DefaultFlowRule.builder()
                        .fromApp(appId)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .withPriority(DEFAULT_PRIORITY)
                        .forDevice(deviceId)
                        .forTable(TABLE_IN_SERVICE)
                        .makePermanent()
                        .build();

                processFlowRule(true, flowRule);
            });
        }
    }

    /**
     * Populates flow rules for DST_IP table.
     *
     * @param deviceId device id
     * @param inPort in port
     * @param dstMac mac address
     * @param dstIp destination ip
     * @param tunnelId tunnel id
     * @param tunnelIp tunnel remote ip
     */
    private void populateDstIpRule(DeviceId deviceId, PortNumber inPort, MacAddress dstMac,
                                   IpAddress dstIp, long tunnelId, IpAddress tunnelIp) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(dstIp.toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(dstMac)
                .setOutput(inPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_DST_IP)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        for (Device device : deviceService.getAvailableDevices(SWITCH)) {
            if (device.id().equals(deviceId)) {
                continue;
            }

            ExtensionTreatment tunnelDst = getTunnelDst(device.id(), tunnelIp.getIp4Address());
            if (tunnelDst == null) {
                continue;
            }

            treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(dstMac)
                    .setTunnelId(tunnelId)
                    .extension(tunnelDst, device.id())
                    .setOutput(getTunnelPort(device.id()))
                    .build();

            flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(DEFAULT_PRIORITY)
                    .forDevice(device.id())
                    .forTable(TABLE_DST_IP)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRule);
        }
    }

    /**
     * Populates flow rules for TUNNEL_ID table.
     *
     * @param deviceId device id
     * @param inPort in port
     * @param mac mac address
     * @param tunnelId tunnel id
     */
    private void populateTunnelInRule(DeviceId deviceId, PortNumber inPort, MacAddress mac, long tunnelId) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(tunnelId)
                .matchEthDst(mac)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(inPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .forDevice(deviceId)
                .forTable(TABLE_TUNNEL_IN)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    /**
     * Installs or uninstall a given rule.
     *
     * @param install true to install, false to uninstall
     * @param rule rule
     */
    private void processFlowRule(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder oBuilder = FlowRuleOperations.builder();
        oBuilder = install ? oBuilder.add(rule) : oBuilder.remove(rule);

        flowRuleService.apply(oBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onError(FlowRuleOperations ops) {
                log.error(String.format("Failed %s, %s", ops.toString(), rule.toString()));
            }
        }));
    }

    /**
     * Returns tunnel port of the device.
     *
     * @param deviceId device id
     * @return tunnel port number, or null if no tunnel port exists on a given device
     */
    private PortNumber getTunnelPort(DeviceId deviceId) {
        Port port = deviceService.getPorts(deviceId).stream()
                    .filter(p -> p.annotations().value("portName").contains(tunnelType))
                    .findFirst().orElse(null);

        return port == null ? null : port.number();
    }

    /**
     * Returns physical port name of a given device.
     *
     * @param deviceId device id
     * @param phyPortName physical port name
     * @return physical port number, or null if no physical port exists
     */
    private PortNumber getPhyPort(DeviceId deviceId, String phyPortName) {
        Port port = deviceService.getPorts(deviceId).stream()
                    .filter(p -> p.annotations().value("portName").contains(phyPortName) &&
                            p.isEnabled())
                    .findFirst().orElse(null);

        return port == null ? null : port.number();
    }

    /**
     * Returns the inport from a given flow rule if the rule contains the match of it.
     *
     * @param flowRule flow rule
     * @return port number, or null if the rule doesn't have inport match
     */
    private PortNumber getInPort(FlowRule flowRule) {
        Criterion criterion = flowRule.selector().getCriterion(IN_PORT);
        if (criterion != null && criterion instanceof PortCriterion) {
            PortCriterion port = (PortCriterion) criterion;
            return port.port();
        } else {
            return null;
        }
    }

    /**
     * Returns the destination mac address from a given flow rule if the rule
     * contains the instruction of it.
     *
     * @param flowRule flow rule
     * @return mac address, or null if the rule doesn't have destination mac instruction
     */
    private MacAddress getDstMacFromTreatment(FlowRule flowRule) {
        Instruction instruction = flowRule.treatment().allInstructions().stream()
                .filter(inst -> inst instanceof ModEtherInstruction &&
                        ((ModEtherInstruction) inst).subtype().equals(ETH_DST))
                .findFirst()
                .orElse(null);

        if (instruction == null) {
            return null;
        }

        return ((ModEtherInstruction) instruction).mac();
    }

    /**
     * Returns the destination mac address from a given flow rule if the rule
     * contains the match of it.
     *
     * @param flowRule flow rule
     * @return mac address, or null if the rule doesn't have destination mac match
     */
    private MacAddress getDstMacFromSelector(FlowRule flowRule) {
        Criterion criterion = flowRule.selector().getCriterion(Criterion.Type.ETH_DST);
        if (criterion != null && criterion instanceof EthCriterion) {
            EthCriterion eth = (EthCriterion) criterion;
            return eth.mac();
        } else {
            return null;
        }
    }

    /**
     * Returns the destination IP from a given flow rule if the rule contains
     * the match of it.
     *
     * @param flowRule flow rule
     * @return ip prefix, or null if the rule doesn't have ip match
     */
    private IpPrefix getDstIpFromSelector(FlowRule flowRule) {
        Criterion criterion = flowRule.selector().getCriterion(IPV4_DST);
        if (criterion != null && criterion instanceof IPCriterion) {
            IPCriterion ip = (IPCriterion) criterion;
            return ip.ip();
        } else {
            return null;
        }
    }

    /**
     * Returns the source IP from a given flow rule if the rule contains
     * the match of it.
     *
     * @param flowRule flow rule
     * @return ip prefix, or null if the rule doesn't have ip match
     */
    private IpPrefix getSrcIpFromSelector(FlowRule flowRule) {
        Criterion criterion = flowRule.selector().getCriterion(IPV4_SRC);
        if (criterion != null && criterion instanceof IPCriterion) {
            IPCriterion ip = (IPCriterion) criterion;
            return ip.ip();
        } else {
            return null;
        }
    }

    /**
     * Returns the group ID from a given flow rule if the rule contains the
     * treatment of it.
     *
     * @param flowRule flow rule
     * @return group id, or null if the rule doesn't have group instruction
     */
    private GroupId getGroupIdFromTreatment(FlowRule flowRule) {
        Instruction instruction = flowRule.treatment().allInstructions().stream()
                .filter(inst -> inst instanceof Instructions.GroupInstruction)
                .findFirst()
                .orElse(null);

        if (instruction == null) {
            return null;
        }

        return ((Instructions.GroupInstruction) instruction).groupId();
    }

    /**
     * Returns the output port number from a given flow rule.
     *
     * @param flowRule flow rule
     * @return port number, or null if the rule does not have output instruction
     */
    private PortNumber getOutputFromTreatment(FlowRule flowRule) {
        Instruction instruction = flowRule.treatment().allInstructions().stream()
                .filter(inst -> inst instanceof  Instructions.OutputInstruction)
                .findFirst()
                .orElse(null);

        if (instruction == null) {
            return null;
        }

        return ((Instructions.OutputInstruction) instruction).port();
    }

    /**
     * Creates a new group for a given service.
     *
     * @param deviceId device id to create a group
     * @param service cord service
     * @return group id, or null if it fails to create
     */
    private GroupId createServiceGroup(DeviceId deviceId, CordService service) {
        checkNotNull(service);

        GroupKey groupKey = getGroupKey(service.id());
        Group group = groupService.getGroup(deviceId, groupKey);
        GroupId groupId = getGroupId(service.id(), deviceId);

        if (group != null) {
            log.debug("Group {} is already exist in {}", service.id(), deviceId);
            return groupId;
        }

        GroupBuckets buckets = getServiceGroupBuckets(deviceId, service.segmentationId(), service.hosts());
        GroupDescription groupDescription = new DefaultGroupDescription(
                deviceId,
                GroupDescription.Type.SELECT,
                buckets,
                groupKey,
                groupId.id(),
                appId);

        groupService.addGroup(groupDescription);

        return groupId;
    }

    /**
     * Returns group buckets for a given device.
     *
     * @param deviceId device id
     * @param tunnelId tunnel id
     * @param hosts list of host
     * @return group buckets
     */
    private GroupBuckets getServiceGroupBuckets(DeviceId deviceId, long tunnelId, Map<Host, IpAddress> hosts) {
        List<GroupBucket> buckets = Lists.newArrayList();

        for (Map.Entry<Host, IpAddress> entry : hosts.entrySet()) {
            Host host = entry.getKey();
            Ip4Address remoteIp = entry.getValue().getIp4Address();
            DeviceId hostDevice = host.location().deviceId();

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                    .builder()
                    .setEthDst(host.mac());

            if (deviceId.equals(hostDevice)) {
                tBuilder.setOutput(host.location().port());
            } else {
                ExtensionTreatment tunnelDst = getTunnelDst(deviceId, remoteIp);
                if (tunnelDst == null) {
                    continue;
                }

                tBuilder.extension(tunnelDst, deviceId)
                        .setTunnelId(tunnelId)
                        .setOutput(getTunnelPort(hostDevice));
            }

            buckets.add(DefaultGroupBucket.createSelectGroupBucket(tBuilder.build()));
        }

        return new GroupBuckets(buckets);
    }

    /**
     * Returns globally unique group ID.
     *
     * @param serviceId service id
     * @param deviceId device id
     * @return group id
     */
    private GroupId getGroupId(CordServiceId serviceId, DeviceId deviceId) {
        return new DefaultGroupId(Objects.hash(serviceId, deviceId));
    }

    /**
     * Returns group key of a service.
     *
     * @param serviceId service id
     * @return group key
     */
    private GroupKey getGroupKey(CordServiceId serviceId) {
        return new DefaultGroupKey(serviceId.id().getBytes());
    }

    /**
     * Returns extension instruction to set tunnel destination.
     *
     * @param deviceId device id
     * @param remoteIp tunnel destination address
     * @return extension treatment or null if it fails to get instruction
     */
    private ExtensionTreatment getTunnelDst(DeviceId deviceId, Ip4Address remoteIp) {
        try {
            Driver driver = driverService.getDriver(deviceId);
            DefaultDriverData driverData = new DefaultDriverData(driver, deviceId);
            DriverHandler handler = new DefaultDriverHandler(driverData);
            ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);

            ExtensionTreatment treatment =
                    resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
            treatment.setPropertyValue("tunnelDst", remoteIp);

            return treatment;
        } catch (ItemNotFoundException | UnsupportedOperationException |
                ExtensionPropertyException e) {
            log.error("Failed to get extension instruction {}", deviceId);
            return null;
        }
    }
}

