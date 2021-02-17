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

import io.fabric8.kubernetes.api.model.Pod;
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
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPodListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
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
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelToTenantPort;
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

    private final InternalKubevirtPodListener kubevirtPodListener =
            new InternalKubevirtPodListener();
    private final InternalKubevirtNodeListener kubevirtNodeListener =
            new InternalKubevirtNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtPodService.addListener(kubevirtPodListener);
        kubevirtNodeService.addListener(kubevirtNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtPodService.removeListener(kubevirtPodListener);
        kubevirtNodeService.removeListener(kubevirtNodeListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private KubevirtPort getPortByPod(Pod pod) {
        return getPort(kubevirtNetworkService.networks(), pod);
    }

    private void setIngressRules(Pod pod, boolean install) {
        KubevirtPort port = getPortByPod(pod);

        if (port == null) {
            return;
        }

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

        KubevirtNode localNode = kubevirtNodeService.node(pod.getSpec().getNodeName());
        if (localNode == null || localNode.type() == MASTER) {
            return;
        }

        PortNumber patchPortNumber = tunnelToTenantPort(localNode, network);
        if (patchPortNumber == null) {
            return;
        }

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

        log.debug("Install ingress rules for instance {}, segment ID {}",
                port.ipAddress(), network.segmentId());
    }

    private void setEgressRules(Pod pod, boolean install) {
        KubevirtNode localNode = kubevirtNodeService.node(pod.getSpec().getNodeName());

        if (localNode == null) {
            return;
        }

        if (localNode.type() == MASTER) {
            return;
        }

        KubevirtPort port = getPortByPod(pod);

        if (port == null) {
            return;
        }

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

        for (KubevirtNode remoteNode : kubevirtNodeService.completeNodes(WORKER)) {
            if (remoteNode.hostname().equals(localNode.hostname())) {
                continue;
            }

            PortNumber patchPortNumber = tunnelToTenantPort(remoteNode, network);
            if (patchPortNumber == null) {
                return;
            }

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

            kubevirtPodService.pods().stream()
                    .filter(pod -> node.hostname().equals(pod.getSpec().getNodeName()))
                    .forEach(pod -> {
                        setIngressRules(pod, true);
                        setEgressRules(pod, true);
                    });
        }
    }

    private class InternalKubevirtPodListener implements KubevirtPodListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtPodEvent event) {

            switch (event.type()) {
                case KUBEVIRT_POD_UPDATED:
                    eventExecutor.execute(() -> processPodUpdate(event.subject()));
                    break;
                case KUBEVIRT_POD_REMOVED:
                    eventExecutor.execute(() -> processPodRemoval(event.subject()));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processPodUpdate(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setIngressRules(pod, true);
            setEgressRules(pod, true);
        }

        private void processPodRemoval(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setIngressRules(pod, false);
            setEgressRules(pod, false);
        }
    }
}
