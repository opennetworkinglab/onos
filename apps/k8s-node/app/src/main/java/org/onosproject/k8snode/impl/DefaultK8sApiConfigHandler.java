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

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sHost;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.ExternalNetworkService;
import org.onosproject.k8snode.api.HostNodesInfo;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigAdminService;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigListener;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostAdminService;
import org.onosproject.k8snode.api.K8sHostState;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.k8snode.api.K8sTunnelBridge;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.Constants.DEFAULT_CLUSTER_NAME;
import static org.onosproject.k8snode.api.Constants.DEFAULT_EXTERNAL_GATEWAY_MAC;
import static org.onosproject.k8snode.api.Constants.EXTERNAL_TO_ROUTER;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.onosproject.k8snode.api.K8sNode.Type.MASTER;
import static org.onosproject.k8snode.api.K8sNode.Type.MINION;
import static org.onosproject.k8snode.api.K8sNodeService.APP_ID;
import static org.onosproject.k8snode.api.K8sNodeState.ON_BOARDED;
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

    private static final String DEFAULT_GATEWAY_IP = "127.0.0.1";
    private static final String DEFAULT_BRIDGE_IP = "127.0.0.1";

    private static final long SLEEP_MS = 10000; // we wait 10s

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostAdminService k8sHostAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ExternalNetworkService extNetworkService;

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

        for (Node node : k8sClient.nodes().list().getItems()) {
            K8sNode k8sNode = buildK8sNode(node, config);
            k8sNodeAdminService.createNode(k8sNode);

            while (k8sNodeAdminService.node(k8sNode.hostname()).state() != ON_BOARDED) {
                try {
                    sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    log.error("Exception caused during on-boarding state checking...");
                }

                if (k8sNodeAdminService.node(k8sNode.hostname()).state() == ON_BOARDED) {
                    break;
                }
            }
        }
    }

    private void bootstrapK8sHosts(K8sApiConfig config) {
        KubernetesClient k8sClient = k8sClient(config);

        if (k8sClient == null) {
            log.warn("Failed to connect to kubernetes API server");
            return;
        }

        config.infos().forEach(h -> {
            k8sHostAdminService.createHost(buildK8sHost(h, config));
        });

    }

    private K8sHost buildK8sHost(HostNodesInfo hostNodesInfo, K8sApiConfig config) {
        int segmentId = config.segmentId();
        K8sTunnelBridge tBridge = new K8sTunnelBridge(segmentId);
        K8sRouterBridge rBridge = new K8sRouterBridge(segmentId);

        return DefaultK8sHost.builder()
                .hostIp(hostNodesInfo.hostIp())
                .state(K8sHostState.INIT)
                .tunBridges(ImmutableSet.of(tBridge))
                .routerBridges(ImmutableSet.of(rBridge))
                .nodeNames(hostNodesInfo.nodes())
                .build();
    }

    private K8sNode buildK8sNode(Node node, K8sApiConfig config) {
        String hostname = node.getMetadata().getName();
        IpAddress managementIp = null;
        IpAddress dataIp = null;
        IpAddress nodeIp = null;

        // pass-through mode: we use host IP as the management and data IP
        // normal mode: we use K8S node's internal IP as the management and data IP
        if (config.mode() == PASSTHROUGH) {
            HostNodesInfo info = config.infos().stream().filter(h -> h.nodes()
                    .contains(hostname)).findAny().orElse(null);
            if (info == null) {
                log.error("None of the nodes were found in the host nodes info mapping list");
            } else {
                managementIp = info.hostIp();
                dataIp = info.hostIp();
            }
            for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
                if (nodeAddress.getType().equals(INTERNAL_IP)) {
                    nodeIp = IpAddress.valueOf(nodeAddress.getAddress());
                }
            }
        } else {
            for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
                if (nodeAddress.getType().equals(INTERNAL_IP)) {
                    managementIp = IpAddress.valueOf(nodeAddress.getAddress());
                    dataIp = IpAddress.valueOf(nodeAddress.getAddress());
                    nodeIp = IpAddress.valueOf(nodeAddress.getAddress());
                }
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

        String extIntf = "";
        String extGatewayIpStr = DEFAULT_GATEWAY_IP;
        String extBridgeIpStr = DEFAULT_BRIDGE_IP;

        if (config.mode() == PASSTHROUGH) {
            extNetworkService.registerNetwork(config.extNetworkCidr());
            extIntf = EXTERNAL_TO_ROUTER + "-" + config.clusterShortName();
            IpAddress gatewayIp = extNetworkService.getGatewayIp(config.extNetworkCidr());
            IpAddress bridgeIp = extNetworkService.allocateIp(config.extNetworkCidr());
            if (gatewayIp != null) {
                extGatewayIpStr = gatewayIp.toString();
            }
            if (bridgeIp != null) {
                extBridgeIpStr = bridgeIp.toString();
            }
        } else {
            extIntf = annots.get(EXT_INTF_NAME);
            extGatewayIpStr = annots.get(EXT_GATEWAY_IP);
            extBridgeIpStr = annots.get(EXT_BRIDGE_IP);
        }

        K8sNode.Builder builder = DefaultK8sNode.builder()
                .clusterName(DEFAULT_CLUSTER_NAME)
                .hostname(hostname)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(new K8sNodeInfo(nodeIp, null))
                .extIntf(extIntf)
                .type(nodeType)
                .segmentId(config.segmentId())
                .state(PRE_ON_BOARD)
                .mode(config.mode())
                .extBridgeIp(IpAddress.valueOf(extBridgeIpStr))
                .extGatewayIp(IpAddress.valueOf(extGatewayIpStr))
                .podCidr(node.getSpec().getPodCIDR());

        if (config.dvr()) {
            builder.extGatewayMac(MacAddress.valueOf(DEFAULT_EXTERNAL_GATEWAY_MAC));
        }

        return builder.build();
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

                if (config.infos().size() > 0) {
                    bootstrapK8sHosts(config);
                }
            }
        }
    }
}
