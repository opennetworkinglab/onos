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
package org.onosproject.openstacknetworking.impl;


import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackGroupRuleService;
import org.onosproject.openstacknetworking.api.OpenstackK8sIntegrationService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CNI_PT_IP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CNI_PT_NODE_PORT_IP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.shiftIpDomain;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.structurePortName;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildPortRangeMatches;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of openstack kubernetes integration service.
 */

@Component(
    immediate = true,
    service = { OpenstackK8sIntegrationService.class }
)
public class OpenstackK8sIntegrationManager implements OpenstackK8sIntegrationService {

    protected final Logger log = getLogger(getClass());

    private static final String SHIFTED_IP_PREFIX = "172.10";
    private static final int NODE_PORT_MIN = 30000;
    private static final int NODE_PORT_MAX = 32767;
    public static final String NODE_FAKE_IP_STR = "172.172.172.172";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackGroupRuleService osGroupRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void installCniPtNodeRules(IpAddress k8sNodeIp,
                                      IpPrefix podCidr, IpPrefix serviceCidr,
                                      IpAddress podGatewayIp, String osK8sIntPortName,
                                      MacAddress k8sIntOsPortMac) {
        setNodeToPodIpRules(k8sNodeIp, podCidr, serviceCidr,
                podGatewayIp, osK8sIntPortName, k8sIntOsPortMac, true);
        setPodToNodeIpRules(k8sNodeIp, podGatewayIp, osK8sIntPortName, true);
    }

    @Override
    public void uninstallCniPtNodeRules(IpAddress k8sNodeIp,
                                        IpPrefix podCidr, IpPrefix serviceCidr,
                                        IpAddress podGatewayIp, String osK8sIntPortName,
                                        MacAddress k8sIntOsPortMac) {
        setNodeToPodIpRules(k8sNodeIp, podCidr, serviceCidr,
                podGatewayIp, osK8sIntPortName, k8sIntOsPortMac, false);
        setPodToNodeIpRules(k8sNodeIp, podGatewayIp, osK8sIntPortName, false);
    }

    @Override
    public void installCniPtNodePortRules(IpAddress k8sNodeIp, String osK8sExtPortName) {
        setNodePortIngressRules(k8sNodeIp, osK8sExtPortName, true);
        setNodePortEgressRules(k8sNodeIp, osK8sExtPortName, true);
    }

    @Override
    public void uninstallCniPtNodePortRules(IpAddress k8sNodeIp, String osK8sExtPortName) {
        setNodePortIngressRules(k8sNodeIp, osK8sExtPortName, false);
        setNodePortEgressRules(k8sNodeIp, osK8sExtPortName, false);
    }

    private void setNodeToPodIpRules(IpAddress k8sNodeIp,
                                     IpPrefix podCidr, IpPrefix serviceCidr,
                                     IpAddress gatewayIp, String osK8sIntPortName,
                                     MacAddress k8sIntOsPortMac, boolean install) {

        OpenstackNode osNode = osNodeByNodeIp(k8sNodeIp);

        if (osNode == null) {
            return;
        }

        PortNumber osK8sIntPortNum = osNode.portNumByName(osK8sIntPortName);

        if (osK8sIntPortNum == null) {
            return;
        }

        TrafficSelector originalPodSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(k8sNodeIp, 32))
                .matchIPDst(podCidr)
                .build();

        TrafficSelector transformedPodSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(k8sNodeIp, 32))
                .matchIPDst(IpPrefix.valueOf(shiftIpDomain(podCidr.toString(), SHIFTED_IP_PREFIX)))
                .build();

        TrafficSelector serviceSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(k8sNodeIp, 32))
                .matchIPDst(serviceCidr)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setIpSrc(gatewayIp)
                .setEthSrc(k8sIntOsPortMac)
                .setOutput(osK8sIntPortNum)
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                originalPodSelector,
                treatment,
                PRIORITY_CNI_PT_IP_RULE,
                FLAT_TABLE,
                install
        );

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                transformedPodSelector,
                treatment,
                PRIORITY_CNI_PT_IP_RULE,
                FLAT_TABLE,
                install
        );

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                serviceSelector,
                treatment,
                PRIORITY_CNI_PT_IP_RULE,
                FLAT_TABLE,
                install
        );
    }

    private void setPodToNodeIpRules(IpAddress k8sNodeIp, IpAddress gatewayIp,
                                     String osK8sIntPortName, boolean install) {
        InstancePort instPort = instPortByNodeIp(k8sNodeIp);

        if (instPort == null) {
            return;
        }

        OpenstackNode osNode = osNodeByNodeIp(k8sNodeIp);

        if (osNode == null) {
            return;
        }

        PortNumber osK8sIntPortNum = osNode.portNumByName(osK8sIntPortName);

        if (osK8sIntPortNum == null) {
            return;
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(gatewayIp, 32))
                .matchInPort(osK8sIntPortNum)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setIpDst(k8sNodeIp)
                .setEthDst(instPort.macAddress())
                .transition(FLAT_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_CNI_PT_IP_RULE,
                PRE_FLAT_TABLE,
                install
        );

        setJumpRules(osK8sIntPortNum, osNode, install);
    }

    private void setNodePortIngressRules(IpAddress k8sNodeIp,
                                         String osK8sExtPortName,
                                         boolean install) {
        OpenstackNode osNode = osNodeByNodeIp(k8sNodeIp);

        if (osNode == null) {
            return;
        }

        PortNumber osK8sExtPortNum = portNumberByNodeIpAndPortName(k8sNodeIp, osK8sExtPortName);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(osK8sExtPortNum)
                .build();

        Map<TpPort, TpPort> portRangeMatchMap =
                buildPortRangeMatches(NODE_PORT_MIN, NODE_PORT_MAX);

        portRangeMatchMap.forEach((key, value) -> {
            TrafficSelector.Builder tcpSelectorBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(k8sNodeIp, 32))
                    .matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchTcpDstMasked(key, value);

            TrafficSelector.Builder udpSelectorBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(k8sNodeIp, 32))
                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDstMasked(key, value);

            osFlowRuleService.setRule(
                    appId,
                    osNode.intgBridge(),
                    tcpSelectorBuilder.build(),
                    treatment,
                    PRIORITY_CNI_PT_NODE_PORT_IP_RULE,
                    PRE_FLAT_TABLE,
                    install
            );

            osFlowRuleService.setRule(
                    appId,
                    osNode.intgBridge(),
                    udpSelectorBuilder.build(),
                    treatment,
                    PRIORITY_CNI_PT_NODE_PORT_IP_RULE,
                    PRE_FLAT_TABLE,
                    install
            );
        });
    }

    private void setNodePortEgressRules(IpAddress k8sNodeIp,
                                        String osK8sExtPortName,
                                        boolean install) {
        InstancePort instPort = instPortByNodeIp(k8sNodeIp);

        if (instPort == null) {
            return;
        }

        OpenstackNode osNode = osNodeByNodeIp(k8sNodeIp);

        if (osNode == null) {
            return;
        }

        PortNumber osK8sExtPortNum = portNumberByNodeIpAndPortName(k8sNodeIp, osK8sExtPortName);

        Port phyPort = phyPortByInstPort(instPort);

        if (phyPort == null) {
            log.warn("No phys interface found for instance port {}", instPort);
            return;
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(osK8sExtPortNum)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(instPort.macAddress())
                .setOutput(phyPort.number())
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_CNI_PT_NODE_PORT_IP_RULE,
                PRE_FLAT_TABLE,
                install
        );

        setJumpRules(osK8sExtPortNum, osNode, install);
    }

    private InstancePort instPortByNodeIp(IpAddress k8sNodeIp) {
        return instancePortService.instancePorts().stream().filter(p -> {
            OpenstackNetwork.Type netType = osNetworkService.networkType(p.networkId());
            return netType == FLAT && p.ipAddress().equals(k8sNodeIp);
        }).findAny().orElse(null);
    }

    private OpenstackNode osNodeByNodeIp(IpAddress k8sNodeIp) {
        InstancePort instPort = instPortByNodeIp(k8sNodeIp);

        if (instPort == null) {
            return null;
        }

        return osNodeService.node(instPort.deviceId());
    }

    private PortNumber portNumberByNodeIpAndPortName(IpAddress k8sNodeIp, String portName) {
        OpenstackNode osNode = osNodeByNodeIp(k8sNodeIp);

        if (osNode == null) {
            return null;
        }

        return osNode.portNumByName(portName);
    }

    private Port phyPortByInstPort(InstancePort instPort) {
        Network network = osNetworkService.network(instPort.networkId());

        if (network == null) {
            log.warn("The network does not exist");
            return null;
        }

        return deviceService.getPorts(instPort.deviceId()).stream()
                .filter(port -> {
                    String annotPortName = port.annotations().value(PORT_NAME);
                    String portName = structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX
                            + network.getProviderPhyNet());
                    return Objects.equals(annotPortName, portName);
                }).findAny().orElse(null);
    }

    private void setJumpRules(PortNumber portNumber, OpenstackNode osNode, boolean install) {
        TrafficSelector jumpSelector = DefaultTrafficSelector.builder()
                .matchInPort(portNumber)
                .build();

        TrafficTreatment jumpTreatment = DefaultTrafficTreatment.builder()
                .transition(PRE_FLAT_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                jumpSelector,
                jumpTreatment,
                PRIORITY_SWITCHING_RULE,
                VTAG_TABLE,
                install
        );
    }
}