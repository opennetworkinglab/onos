/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.mastership.MastershipService;
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
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_TUNNEL_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelToTenantPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.waitFor;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.MASTER;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates switching flow rules on OVS for the tenant network (overlay).
 */
@Component(immediate = true)
public class KubevirtSwitchingTenantHandler {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService kubevirtNetworkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService kubevirtPortService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPodService kubevirtPodService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtNetworkListener kubevirtNetworkListener =
            new InternalKubevirtNetworkListener();
    private final InternalKubevirtPortListener kubevirtPortListener =
            new InternalKubevirtPortListener();
    private final InternalKubevirtNodeListener kubevirtNodeListener =
            new InternalKubevirtNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtPortService.addListener(kubevirtPortListener);
        kubevirtNetworkService.addListener(kubevirtNetworkListener);
        kubevirtNodeService.addListener(kubevirtNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtNetworkService.removeListener(kubevirtNetworkListener);
        kubevirtPortService.removeListener(kubevirtPortListener);
        kubevirtNodeService.removeListener(kubevirtNodeListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setIngressRules(KubevirtNetwork network, boolean install) {
        if (network == null) {
            return;
        }

        if (network.type() == FLAT || network.type() == VLAN) {
            return;
        }

        if (network.segmentId() == null) {
            return;
        }

        for (KubevirtNode localNode : kubevirtNodeService.completeNodes(WORKER)) {

            while (true) {
                KubevirtNode updatedNode = kubevirtNodeService.node(localNode.hostname());
                if (tunnelToTenantPort(deviceService, updatedNode, network) != null) {
                    break;
                } else {
                    log.info("Waiting for tunnel to tenant patch port creation " +
                             "on ingress rule setup on node {}", updatedNode);
                    waitFor(3);
                }
            }

            PortNumber patchPortNumber = tunnelToTenantPort(deviceService, localNode, network);

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                    .matchTunnelId(Long.parseLong(network.segmentId()));

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setOutput(patchPortNumber);

            flowRuleService.setRule(
                    appId,
                    localNode.tunBridge(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_TUNNEL_RULE,
                    TUNNEL_DEFAULT_TABLE,
                    install);

            log.debug("Install ingress rules for segment ID {}", network.segmentId());
        }
    }

    private void setIngressRules(KubevirtNode node, boolean install) {
        for (KubevirtNetwork network : kubevirtNetworkService.tenantNetworks()) {

            if (node == null || node.type() != WORKER) {
                return;
            }

            while (true) {
                KubevirtNode updatedNode = kubevirtNodeService.node(node.hostname());
                if (tunnelToTenantPort(deviceService, updatedNode, network) != null) {
                    break;
                } else {
                    log.info("Waiting for tunnel to tenant patch port creation " +
                             "on ingress rule setup on node {}", updatedNode);
                    waitFor(3);
                }
            }

            PortNumber patchPortNumber = tunnelToTenantPort(deviceService, node, network);

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                    .matchTunnelId(Long.parseLong(network.segmentId()));

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setOutput(patchPortNumber);

            flowRuleService.setRule(
                    appId,
                    node.tunBridge(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_TUNNEL_RULE,
                    TUNNEL_DEFAULT_TABLE,
                    install);

            log.debug("Install ingress rules for segment ID {}", network.segmentId());
        }
    }

    private void setEgressRules(KubevirtPort port, boolean install) {
        if (port.ipAddress() == null) {
            return;
        }

        KubevirtNetwork network = kubevirtNetworkService.network(port.networkId());

        if (network == null) {
            return;
        }

        if (network.type() == FLAT || network.type() == VLAN) {
            return;
        }

        if (network.segmentId() == null) {
            return;
        }

        KubevirtNode localNode = kubevirtNodeService.node(port.deviceId());

        if (localNode == null || localNode.type() == MASTER) {
            return;
        }

        for (KubevirtNode remoteNode : kubevirtNodeService.completeNodes(WORKER)) {
            if (remoteNode.hostname().equals(localNode.hostname())) {
                continue;
            }

            while (true) {
                KubevirtNode updatedNode = kubevirtNodeService.node(localNode.hostname());
                if (tunnelToTenantPort(deviceService, updatedNode, network) != null) {
                    break;
                } else {
                    log.info("Waiting for tunnel to tenant patch port creation " +
                             "on egress rule setup on node {}", updatedNode);
                    waitFor(3);
                }
            }

            PortNumber patchPortNumber = tunnelToTenantPort(deviceService, remoteNode, network);

            PortNumber tunnelPortNumber = tunnelPort(remoteNode, network);
            if (tunnelPortNumber == null) {
                return;
            }

            TrafficSelector.Builder sIpBuilder = DefaultTrafficSelector.builder()
                    .matchInPort(patchPortNumber)
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(port.ipAddress(), 32));

            TrafficSelector.Builder sArpBuilder = DefaultTrafficSelector.builder()
                    .matchInPort(patchPortNumber)
                    .matchEthType(Ethernet.TYPE_ARP)
                    .matchArpTpa(Ip4Address.valueOf(port.ipAddress().toString()));

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setTunnelId(Long.parseLong(network.segmentId()))
                    .extension(buildExtension(
                            deviceService,
                            remoteNode.tunBridge(),
                            localNode.dataIp().getIp4Address()),
                            remoteNode.tunBridge())
                    .setOutput(tunnelPortNumber);

            flowRuleService.setRule(
                    appId,
                    remoteNode.tunBridge(),
                    sIpBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_TUNNEL_RULE,
                    TUNNEL_DEFAULT_TABLE,
                    install);

            flowRuleService.setRule(
                    appId,
                    remoteNode.tunBridge(),
                    sArpBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_TUNNEL_RULE,
                    TUNNEL_DEFAULT_TABLE,
                    install);
        }

        log.debug("Install egress rules for instance {}, segment ID {}",
                port.ipAddress(), network.segmentId());
    }

    private class InternalKubevirtNodeListener implements KubevirtNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {

            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case KUBEVIRT_NODE_INCOMPLETE:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCompletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            setIngressRules(node, true);
            kubevirtPortService.ports().stream()
                    .filter(port -> node.equals(kubevirtNodeService.node(port.deviceId())))
                    .forEach(port -> {
                        setEgressRules(port, true);
                    });
        }
    }

    private class InternalKubevirtNetworkListener implements KubevirtNetworkListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNetworkEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NETWORK_CREATED:
                    eventExecutor.execute(() -> processNetworkAddition(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event.subject()));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkAddition(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            setIngressRules(network, true);
        }

        private void processNetworkRemoval(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            setIngressRules(network, false);
        }
    }

    private class InternalKubevirtPortListener implements KubevirtPortListener {

        @Override
        public boolean isRelevant(KubevirtPortEvent event) {
            return event.subject().deviceId() != null;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtPortEvent event) {

            switch (event.type()) {
                case KUBEVIRT_PORT_UPDATED:
                    eventExecutor.execute(() -> processPortUpdate(event.subject()));
                    break;
                case KUBEVIRT_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemoval(event.subject()));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processPortUpdate(KubevirtPort port) {
            if (!isRelevantHelper()) {
                return;
            }

            setEgressRules(port, true);
        }

        private void processPortRemoval(KubevirtPort port) {
            if (!isRelevantHelper()) {
                return;
            }

            setEgressRules(port, false);
        }
    }
}
