/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cordvtn.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cordvtn.api.CordVtnNode;
import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides CORD VTN pipeline.
 */
@Component(immediate = true)
@Service(value = CordVtnPipeline.class)
public final class CordVtnPipeline {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    // tables
    public static final int TABLE_ZERO = 0;
    public static final int TABLE_IN_PORT = 1;
    public static final int TABLE_ACCESS_TYPE = 2;
    public static final int TABLE_IN_SERVICE = 3;
    public static final int TABLE_DST_IP = 4;
    public static final int TABLE_TUNNEL_IN = 5;
    public static final int TABLE_VLAN = 6;

    // priorities
    public static final int PRIORITY_MANAGEMENT = 55000;
    public static final int PRIORITY_HIGH = 50000;
    public static final int PRIORITY_DEFAULT = 5000;
    public static final int PRIORITY_LOW = 4000;
    public static final int PRIORITY_ZERO = 0;

    public static final int VXLAN_UDP_PORT = 4789;
    public static final VlanId VLAN_WAN = VlanId.vlanId((short) 500);
    public static final String DEFAULT_TUNNEL = "vxlan";
    private static final String PORT_NAME = "portName";

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(CordVtnService.CORDVTN_APP_ID);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Flush flows installed by this application.
     */
    public void flushRules() {
        flowRuleService.getFlowRulesById(appId).forEach(flowRule -> processFlowRule(false, flowRule));
    }

    /**
     * Installs table miss rule to a give device.
     *
     * @param node cordvtn node
     * @param dpPort data plane port number
     * @param tunnelPort tunnel port number
     */
    public void initPipeline(CordVtnNode node, PortNumber dpPort, PortNumber tunnelPort) {
        checkNotNull(node);

        processTableZero(node.intBrId(), dpPort, node.dpIp().ip());
        processInPortTable(node.intBrId(), tunnelPort, dpPort);
        processAccessTypeTable(node.intBrId(), dpPort);
        processVlanTable(node.intBrId(), dpPort);
    }

    private void processTableZero(DeviceId deviceId, PortNumber dpPort, IpAddress dpIp) {
        // take vxlan packet out onto the physical port
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(dpPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_HIGH)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take a vxlan encap'd packet through the Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(dpPort)
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
                .withPriority(PRIORITY_HIGH)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take a packet to the data plane ip through Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(dpPort)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(dpIp.toIpPrefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_HIGH)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take an arp packet from physical through Linux stack
        selector = DefaultTrafficSelector.builder()
                .matchInPort(dpPort)
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(dpIp.getIp4Address())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_HIGH)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
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
                .withPriority(PRIORITY_ZERO)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        // take all vlan tagged packet to the VLAN table
        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.ANY)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_VLAN)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_MANAGEMENT)
                .forDevice(deviceId)
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    private void processInPortTable(DeviceId deviceId, PortNumber tunnelPort, PortNumber dpPort) {
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
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(dpPort)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(deviceId)
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    private void processAccessTypeTable(DeviceId deviceId, PortNumber dpPort) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(dpPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_ZERO)
                .forDevice(deviceId)
                .forTable(TABLE_ACCESS_TYPE)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    private void processVlanTable(DeviceId deviceId, PortNumber dpPort) {
        // for traffic going out to WAN, strip vid 500 and take through data plane interface
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_WAN)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .setOutput(dpPort)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(deviceId)
                .forTable(TABLE_VLAN)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_WAN)
                .matchEthType(Ethernet.TYPE_ARP)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_HIGH)
                .forDevice(deviceId)
                .forTable(TABLE_VLAN)
                .makePermanent()
                .build();

        processFlowRule(true, flowRule);
    }

    public void processFlowRule(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder oBuilder = FlowRuleOperations.builder();
        oBuilder = install ? oBuilder.add(rule) : oBuilder.remove(rule);

        flowRuleService.apply(oBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onError(FlowRuleOperations ops) {
                log.error(String.format("Failed %s, %s", ops.toString(), rule.toString()));
            }
        }));
    }

    public ExtensionTreatment tunnelDstTreatment(DeviceId deviceId, Ip4Address remoteIp) {
        try {
            Device device = deviceService.getDevice(deviceId);

            if (device.is(ExtensionTreatmentResolver.class)) {
                ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
                ExtensionTreatment treatment =
                        resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
                treatment.setPropertyValue("tunnelDst", remoteIp);
                return treatment;
            } else {
                log.warn("The extension treatment resolving behaviour is not supported in device {}",
                         device.id().toString());
                return null;
            }
        } catch (ItemNotFoundException | UnsupportedOperationException |
                ExtensionPropertyException e) {
            log.error("Failed to get extension instruction {}", deviceId);
            return null;
        }
    }
}
