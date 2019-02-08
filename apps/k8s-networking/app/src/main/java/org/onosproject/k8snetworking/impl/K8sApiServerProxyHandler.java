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

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.PacketService;
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
import static org.onosproject.k8snetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_TRANSLATION_RULE;
import static org.onosproject.k8snetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles kubernetes API server requests from pods.
 */
@Component(immediate = true)
public class K8sApiServerProxyHandler {
    protected final Logger log = getLogger(getClass());

    private static final String API_SERVER_CLUSTER_IP = "10.96.0.1";
    private static final int API_SERVER_CLUSTER_PORT = 443;
    private static final String API_SERVER_IP = "10.10.10.1";
    private static final int API_SERVER_PORT = 6443;
    private static final int PREFIX_LENGTH = 32;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final K8sNodeListener k8sNodeListener = new InternalNodeEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        k8sNodeService.addListener(k8sNodeListener);
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(k8sNodeListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalNodeEventListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            K8sNode k8sNode = event.subject();
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(k8sNode));
                    break;
                case K8S_NODE_INCOMPLETE:
                    eventExecutor.execute(() -> processNodeIncompletion(k8sNode));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setRequestTranslationRule(k8sNode, true);
            setResponseTranslationRule(k8sNode, true);
        }

        private void processNodeIncompletion(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setRequestTranslationRule(k8sNode, false);
            setResponseTranslationRule(k8sNode, false);
        }

        /**
         * Installs k8s API server rule for receiving all API request packets.
         *
         * @param k8sNode    kubernetes node
         * @param install    installation flag
         */
        private void setRequestTranslationRule(K8sNode k8sNode, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchIPDst(IpPrefix.valueOf(
                            IpAddress.valueOf(API_SERVER_CLUSTER_IP), PREFIX_LENGTH))
                    .matchTcpDst(TpPort.tpPort(API_SERVER_CLUSTER_PORT))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpDst(IpAddress.valueOf(API_SERVER_IP))
                    .setTcpDst(TpPort.tpPort(API_SERVER_PORT))
                    .setOutput(PortNumber.LOCAL)
                    .build();

            k8sFlowRuleService.setRule(
                    appId,
                    k8sNode.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_TRANSLATION_RULE,
                    STAT_OUTBOUND_TABLE,
                    install
            );
        }

        /**
         * Installs k8s API server rule for receiving all API response packets.
         *
         * @param k8sNode    kubernetes node
         * @param install    installation flag
         */
        private void setResponseTranslationRule(K8sNode k8sNode, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchIPSrc(IpPrefix.valueOf(
                            IpAddress.valueOf(API_SERVER_IP), PREFIX_LENGTH))
                    .matchTcpSrc(TpPort.tpPort(API_SERVER_PORT))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpSrc(IpAddress.valueOf(API_SERVER_CLUSTER_IP))
                    .setTcpSrc(TpPort.tpPort(API_SERVER_CLUSTER_PORT))
                    .transition(FORWARDING_TABLE)
                    .build();

            k8sFlowRuleService.setRule(
                    appId,
                    k8sNode.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_TRANSLATION_RULE,
                    STAT_OUTBOUND_TABLE,
                    install
            );
        }
    }
}
