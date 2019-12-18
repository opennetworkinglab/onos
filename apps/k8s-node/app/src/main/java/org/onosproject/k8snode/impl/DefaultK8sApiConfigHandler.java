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
package org.onosproject.k8snode.impl;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigAdminService;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigListener;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.K8sNode.Type.MASTER;
import static org.onosproject.k8snode.api.K8sNode.Type.MINION;
import static org.onosproject.k8snode.api.K8sNodeService.APP_ID;
import static org.onosproject.k8snode.api.K8sNodeState.PRE_ON_BOARD;
import static org.onosproject.k8snode.util.K8sNodeUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the state of kubernetes API server configuration.
 */
@Component(immediate = true)
public class DefaultK8sApiConfigHandler {

    private final Logger log = getLogger(getClass());

    private static final String INTERNAL_IP = "InternalIP";
    private static final String K8S_ROLE = "node-role.kubernetes.io";
    private static final String EXT_BRIDGE_IP = "external.bridge.ip";
    private static final String EXT_GATEWAY_IP = "external.gateway.ip";
    private static final String EXT_INTF_NAME = "external.interface.name";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigAdminService k8sApiConfigAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeAdminService k8sNodeAdminService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final K8sApiConfigListener k8sApiConfigListener = new InternalK8sApiConfigListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sApiConfigAdminService.addListener(k8sApiConfigListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sApiConfigAdminService.removeListener(k8sApiConfigListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    /**
     * Checks the validity of the given kubernetes API server configuration.
     *
     * @param config kubernetes API server configuration
     * @return validity result
     */
    private boolean checkApiServerConfig(K8sApiConfig config) {
        KubernetesClient k8sClient = k8sClient(config);
        return k8sClient != null && k8sClient.getApiVersion() != null;
    }

    private void bootstrapK8sNodes(K8sApiConfig config) {
        KubernetesClient k8sClient = k8sClient(config);

        if (k8sClient == null) {
            log.warn("Failed to connect to kubernetes API server");
            return;
        }

        k8sClient.nodes().list().getItems().forEach(n ->
            k8sNodeAdminService.createNode(buildK8sNode(n))
        );
    }

    private K8sNode buildK8sNode(Node node) {
        String hostname = node.getMetadata().getName();
        IpAddress managementIp = null;
        IpAddress dataIp = null;

        for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
            // we need to consider assigning managementIp and dataIp differently
            // FIXME: ExternalIp is not considered currently
            if (nodeAddress.getType().equals(INTERNAL_IP)) {
                managementIp = IpAddress.valueOf(nodeAddress.getAddress());
                dataIp = IpAddress.valueOf(nodeAddress.getAddress());
            }
        }

        String roleStr = node.getMetadata().getLabels().keySet().stream()
                .filter(l -> l.contains(K8S_ROLE))
                .findFirst().orElse(null);

        K8sNode.Type nodeType = MINION;

        if (roleStr != null) {
            String role = roleStr.split("/")[1];
            if (MASTER.name().equalsIgnoreCase(role)) {
                nodeType = MASTER;
            } else {
                nodeType = MINION;
            }
        }

        Map<String, String> annots = node.getMetadata().getAnnotations();

        String extIntf = annots.get(EXT_INTF_NAME);
        String extGatewayIpStr = annots.get(EXT_GATEWAY_IP);
        String extBridgeIpStr = annots.get(EXT_BRIDGE_IP);

        return DefaultK8sNode.builder()
                .hostname(hostname)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .extIntf(extIntf)
                .type(nodeType)
                .state(PRE_ON_BOARD)
                .extBridgeIp(IpAddress.valueOf(extBridgeIpStr))
                .extGatewayIp(IpAddress.valueOf(extGatewayIpStr))
                .podCidr(node.getSpec().getPodCIDR())
                .build();
    }

    /**
     * An internal kubernetes API server config listener.
     * The notification is triggered by K8sApiConfigStore.
     */
    private class InternalK8sApiConfigListener implements K8sApiConfigListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sApiConfigEvent event) {

            switch (event.type()) {
                case K8S_API_CONFIG_CREATED:
                    eventExecutor.execute(() -> processConfigCreation(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processConfigCreation(K8sApiConfig config) {
            if (!isRelevantHelper()) {
                return;
            }

            if (checkApiServerConfig(config)) {
                K8sApiConfig newConfig = config.updateState(K8sApiConfig.State.CONNECTED);
                k8sApiConfigAdminService.updateApiConfig(newConfig);
                bootstrapK8sNodes(config);
            }
        }
    }
}
