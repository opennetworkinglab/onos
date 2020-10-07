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
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CNI_PT_IP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_FLAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.shiftIpDomain;
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

    public static final String SHIFTED_IP_PREFIX = "172.10";

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

    private void setNodeToPodIpRules(IpAddress k8sNodeIp,
                                     IpPrefix podCidr, IpPrefix serviceCidr,
                                     IpAddress gatewayIp, String osK8sIntPortName,
                                     MacAddress k8sIntOsPortMac, boolean install) {

        InstancePort instPort = instancePortService.instancePorts().stream().filter(p -> {
            OpenstackNetwork.Type netType = osNetworkService.networkType(p.networkId());
            return netType == FLAT && p.ipAddress().equals(k8sNodeIp);
        }).findAny().orElse(null);

        if (instPort == null) {
            return;
        }

        DeviceId deviceId = instPort.deviceId();
        OpenstackNode osNode = osNodeService.node(deviceId);

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
        InstancePort instPort = instancePortService.instancePorts().stream().filter(p -> {
            OpenstackNetwork.Type netType = osNetworkService.networkType(p.networkId());
            return netType == FLAT && p.ipAddress().equals(k8sNodeIp);
        }).findAny().orElse(null);

        if (instPort == null) {
            return;
        }

        DeviceId deviceId = instPort.deviceId();
        OpenstackNode osNode = osNodeService.node(deviceId);

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
                .transition(STAT_FLAT_OUTBOUND_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_CNI_PT_IP_RULE,
                DHCP_TABLE,
                install
        );
    }
}