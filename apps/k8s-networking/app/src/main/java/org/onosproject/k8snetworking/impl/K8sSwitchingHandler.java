/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.base.Strings;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.k8snetworking.api.Constants.ARP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_DEFAULT_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_TUNNEL_TAG_RULE;
import static org.onosproject.k8snetworking.api.Constants.TUN_ENTRY_TABLE;
import static org.onosproject.k8snetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.k8snetworking.api.K8sNetwork.Type.GENEVE;
import static org.onosproject.k8snetworking.api.K8sNetwork.Type.GRE;
import static org.onosproject.k8snetworking.api.K8sNetwork.Type.VXLAN;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getPropertyValue;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.tunnelPortNumByNetId;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.tunnelPortNumByNetType;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildExtension;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates switching flow rules on OVS for the basic connectivity among the
 * container in the same network.
 */
@Component(immediate = true)
public class K8sSwitchingHandler {

    private final Logger log = getLogger(getClass());

    private static final String ARP_MODE = "arpMode";
    private static final String ERR_SET_FLOWS_VNI = "Failed to set flows for " +
            "%s: Failed to get VNI for %s";
    private static final String STR_NONE = "<none>";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InternalK8sNetworkListener k8sNetworkListener =
            new InternalK8sNetworkListener();
    private final InternalK8sNodeListener k8sNodeListener =
            new InternalK8sNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        k8sNetworkService.addListener(k8sNetworkListener);
        localNodeId = clusterService.getLocalNode().id();
        k8sNodeService.addListener(k8sNodeListener);
        leadershipService.runForLeadership(appId.name());

        setGatewayRulesForTunnel(true);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        k8sNodeService.removeListener(k8sNodeListener);
        k8sNetworkService.removeListener(k8sNetworkListener);
        eventExecutor.shutdown();

        setGatewayRulesForTunnel(false);

        log.info("Stopped");
    }

    /**
     * Configures the flow rules which are used for L2 packet switching.
     * Note that these rules will be inserted in switching table (table 80).
     *
     * @param port      kubernetes port object
     * @param install   install flag, add the rule if true, remove it otherwise
     */
    private void setForwardingRulesForTunnel(K8sPort port, boolean install) {
        // switching rules for the instPorts in the same node
        TrafficSelector selector = DefaultTrafficSelector.builder()
                // TODO: need to handle IPv6 in near future
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(port.ipAddress().toIpPrefix())
                // .matchTunnelId(getVni(port))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(port.macAddress())
                .setOutput(port.portNumber())
                .build();

        k8sFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                FORWARDING_TABLE,
                install);

        // switching rules for the node in the remote node
        K8sNode localNode = k8sNodeService.node(port.deviceId());
        if (localNode == null) {
            final String error = String.format("Cannot find kubernetes node for %s",
                    port.deviceId());
            throw new IllegalStateException(error);
        }
        k8sNodeService.completeNodes().stream()
                .filter(remoteNode -> !remoteNode.intgBridge().equals(localNode.intgBridge()))
                .forEach(remoteNode -> {
                    TrafficTreatment treatmentToTunnel = DefaultTrafficTreatment.builder()
                            .setOutput(remoteNode.intgToTunPortNum())
                            .build();

                    k8sFlowRuleService.setRule(
                            appId,
                            remoteNode.intgBridge(),
                            selector,
                            treatmentToTunnel,
                            PRIORITY_SWITCHING_RULE,
                            FORWARDING_TABLE,
                            install);

                    PortNumber portNum = tunnelPortNumByNetId(port.networkId(),
                            k8sNetworkService, remoteNode);

                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .extension(buildExtension(
                                    deviceService,
                                    remoteNode.tunBridge(),
                                    localNode.dataIp().getIp4Address()),
                                    remoteNode.tunBridge())
                            .setTunnelId(getVni(port))
                            .setOutput(portNum)
                            .build();

                    k8sFlowRuleService.setRule(
                            appId,
                            remoteNode.tunBridge(),
                            selector,
                            treatmentToRemote,
                            PRIORITY_DEFAULT_RULE,
                            TUN_ENTRY_TABLE,
                            install);
                });
    }

    private void setRulesForTunnelBridge(K8sNode node, boolean install) {
        setRulesForTunnelBridgeByType(node, VXLAN, install);
        setRulesForTunnelBridgeByType(node, GRE, install);
        setRulesForTunnelBridgeByType(node, GENEVE, install);
    }

    private void setRulesForTunnelBridgeByType(K8sNode node, K8sNetwork.Type type, boolean install) {

        PortNumber portNum;

        switch (type) {
            case VXLAN:
                portNum = tunnelPortNumByNetType(VXLAN, node);
                break;
            case GRE:
                portNum = tunnelPortNumByNetType(GRE, node);
                break;
            case GENEVE:
                portNum = tunnelPortNumByNetType(GENEVE, node);
                break;
            default:
                return;
        }

        TrafficSelector inboundSelector = DefaultTrafficSelector.builder()
                .matchInPort(portNum)
                .build();

        TrafficTreatment inboundTreatment = DefaultTrafficTreatment.builder()
                .setOutput(node.tunToIntgPortNum())
                .build();

        k8sFlowRuleService.setRule(
                appId,
                node.tunBridge(),
                inboundSelector,
                inboundTreatment,
                PRIORITY_DEFAULT_RULE,
                TUN_ENTRY_TABLE,
                install);
    }


    private void setTunnelTagArpFlowRules(K8sPort port, boolean install) {
        setTunnelTagFlowRules(port, Ethernet.TYPE_ARP, install);
    }

    private void setTunnelTagIpFlowRules(K8sPort port, boolean install) {
        setTunnelTagFlowRules(port, Ethernet.TYPE_IPV4, install);
    }

    private void setNetworkRulesForTunnel(K8sPort port, boolean install) {
        setTunnelTagIpFlowRules(port, install);
        setForwardingRulesForTunnel(port, install);

        if (ARP_BROADCAST_MODE.equals(getArpMode())) {
            setTunnelTagArpFlowRules(port, install);
        }
    }

    /**
     * Configures the flow rule which is for using VXLAN/GRE/GENEVE to tag the packet
     * based on the in_port number of a virtual instance.
     * Note that this rule will be inserted in vTag table.
     *
     * @param port kubernetes port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setTunnelTagFlowRules(K8sPort port, short ethType, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ethType)
                .matchInPort(port.portNumber())
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(getVni(port));

        if (ethType == Ethernet.TYPE_ARP) {
            tBuilder.transition(ARP_TABLE);
        } else if (ethType == Ethernet.TYPE_IPV4) {
            tBuilder.transition(JUMP_TABLE);
        }

        k8sFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                tBuilder.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setExtToIntgTunnelTagFlowRules(K8sNode k8sNode, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(k8sNode.intgToExtPatchPortNum())
                .build();

        K8sNetwork net = k8sNetworkService.network(k8sNode.hostname());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.valueOf(net.segmentId()))
                .transition(JUMP_TABLE);

        k8sFlowRuleService.setRule(
                appId,
                k8sNode.intgBridge(),
                selector,
                tBuilder.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setLocalTunnelTagFlowRules(K8sNode k8sNode, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(k8sNode.intgEntryPortNum())
                .build();

        K8sNetwork net = k8sNetworkService.network(k8sNode.hostname());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.valueOf(net.segmentId()))
                .transition(JUMP_TABLE);

        k8sFlowRuleService.setRule(
                appId,
                k8sNode.intgBridge(),
                selector,
                tBuilder.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setGatewayRulesForTunnel(boolean install) {
        k8sNetworkService.networks().forEach(n -> {
            // switching rules for the instPorts in the same node
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    // TODO: need to handle IPv6 in near future
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(n.gatewayIp(), 32))
                    .matchTunnelId(Long.valueOf(n.segmentId()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.LOCAL)
                    .build();

            // FIXME: need to find a way to install the gateway rules into
            // right OVS
            k8sNodeService.completeNodes().forEach(node -> {
                k8sFlowRuleService.setRule(
                        appId,
                        node.intgBridge(),
                        selector,
                        treatment,
                        PRIORITY_SWITCHING_RULE,
                        FORWARDING_TABLE,
                        install);
            });
        });
    }

    /**
     * Obtains the VNI from the given kubernetes port.
     *
     * @param port kubernetes port object
     * @return Virtual Network Identifier (VNI)
     */
    private Long getVni(K8sPort port) {
        K8sNetwork k8sNet = k8sNetworkService.network(port.networkId());
        if (k8sNet == null || Strings.isNullOrEmpty(k8sNet.segmentId())) {
            final String error =
                    String.format(ERR_SET_FLOWS_VNI,
                            port, k8sNet == null ? STR_NONE : k8sNet.name());
            throw new IllegalStateException(error);
        }
        return Long.valueOf(k8sNet.segmentId());
    }

    private void setNetworkRules(K8sPort port, boolean install) {
        K8sNetwork k8sNet = k8sNetworkService.network(port.networkId());

        if (k8sNet == null) {
            log.warn("Network {} is not found from port {}.",
                    port.networkId(), port.portId());
            return;
        }

        switch (k8sNet.type()) {
            case VXLAN:
            case GRE:
            case GENEVE:
                setNetworkRulesForTunnel(port, install);
                break;
            default:
                log.warn("The given network type {} is not supported.",
                        k8sNet.type().name());
                break;
        }
    }

    private String getArpMode() {
        Set<ConfigProperty> properties =
                configService.getProperties(K8sSwitchingArpHandler.class.getName());
        return getPropertyValue(properties, ARP_MODE);
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        private boolean isRelevantHelper(K8sNetworkEvent event) {
            DeviceId deviceId = event.port().deviceId();
            if (deviceId == null) {
                return false;
            } else {
                return mastershipService.isLocalMaster(deviceId);
            }
        }

        @Override
        public void event(K8sNetworkEvent event) {
            switch (event.type()) {
                case K8S_PORT_ACTIVATED:
                    eventExecutor.execute(() -> processInstanceDetection(event));
                    break;
                case K8S_PORT_REMOVED:
                    eventExecutor.execute(() -> processInstanceRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processInstanceDetection(K8sNetworkEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            setNetworkRules(event.port(), true);
        }

        private void processInstanceRemoval(K8sNetworkEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            setNetworkRules(event.port(), false);
        }
    }

    private class InternalK8sNodeListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setExtToIntgTunnelTagFlowRules(k8sNode, true);
            setLocalTunnelTagFlowRules(k8sNode, true);
            setRulesForTunnelBridge(k8sNode, true);
        }
    }
}
