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
package org.onosproject.k8snetworking.impl;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.eclipsesource.json.JsonObject;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.SERVICE_IP_CIDR_DEFAULT;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getBclassIpPrefixFromCidr;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides kubernetes and openstack integration feature.
 */
@Component(immediate = true)
public class K8sOpenstackIntegrationHandler {

    private final Logger log = getLogger(getClass());

    private static final String K8S_NODE_IP = "k8sNodeIp";
    private static final String OS_K8S_INT_PORT_NAME = "osK8sIntPortName";
    private static final String OS_K8S_EXT_PORT_NAME = "osK8sExtPortName";
    private static final String POD_CIDR = "podCidr";
    private static final String SERVICE_CIDR = "serviceCidr";
    private static final String POD_GW_IP = "podGwIp";
    private static final String K8S_INT_OS_PORT_MAC = "k8sIntOsPortMac";
    private static final String ONOS_PORT = "8181";
    private static final String OS_K8S_INTEGRATION_EP = "onos/openstacknetworking/integration/";
    private static final String ONOS_USERNAME = "karaf";
    private static final String ONOS_PASSWORD = "karaf";
    private static final String B_CLASS_SUFFIX = ".0.0/16";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    private final InternalK8sNodeListener k8sNodeListener =
            new InternalK8sNodeListener();
    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        k8sNodeService.addListener(k8sNodeListener);


        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(k8sNodeListener);

        log.info("Stopped");
    }

    private void setCniPtNodeRules(K8sNode k8sNode, boolean install) {
        K8sNetwork network = k8sNetworkService.network(k8sNode.hostname());
        String k8sNodeIp = k8sNode.nodeIp().toString();
        String gatewayIp = network.gatewayIp().toString();
        String nodePodCidr = network.cidr();
        String srcPodPrefix = getBclassIpPrefixFromCidr(nodePodCidr);
        String podCidr = srcPodPrefix + B_CLASS_SUFFIX;
        String osK8sIntPortName = k8sNode.osToK8sIntgPatchPortName();
        String k8sIntOsPortMac = k8sNode.portMacByName(k8sNode.intgBridge(),
                k8sNode.k8sIntgToOsPatchPortName()).toString();

        String path = install ? "node/pt-install" : "node/pt-uninstall";

        String jsonString = "";

        jsonString = new JsonObject()
                .set(K8S_NODE_IP, k8sNodeIp)
                .set(POD_GW_IP, gatewayIp)
                .set(POD_CIDR, podCidr)
                .set(SERVICE_CIDR, SERVICE_IP_CIDR_DEFAULT)
                .set(OS_K8S_INT_PORT_NAME, osK8sIntPortName)
                .set(K8S_INT_OS_PORT_MAC, k8sIntOsPortMac)
                .toString();
        log.info("push integration configuration {}", jsonString);

        HttpAuthenticationFeature feature =
                HttpAuthenticationFeature.basic(ONOS_USERNAME, ONOS_PASSWORD);

        final Client client = ClientBuilder.newClient();
        client.register(feature);
        String host = "http://" + k8sNode.managementIp().toString() + ":" + ONOS_PORT + "/";
        String endpoint = host + OS_K8S_INTEGRATION_EP;
        WebTarget wt = client.target(endpoint).path(path);
        Response response = wt.request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonString));
        final int status = response.getStatus();

        if (status != 200) {
            log.error("Failed to install/uninstall openstack k8s CNI PT rules.");
        }
    }

    private void setCniPtNodePortRules(K8sNode k8sNode, boolean install) {
        String k8sNodeIp = k8sNode.nodeIp().toString();
        String osK8sExtPortName = k8sNode.osToK8sExtPatchPortName();

        String path = install ? "nodeport/pt-install" : "nodeport/pt-uninstall";

        String jsonString = "";

        jsonString = new JsonObject()
                .set(K8S_NODE_IP, k8sNodeIp)
                .set(SERVICE_CIDR, SERVICE_IP_CIDR_DEFAULT)
                .set(OS_K8S_EXT_PORT_NAME, osK8sExtPortName)
                .toString();
        log.info("push integration configuration {}", jsonString);

        HttpAuthenticationFeature feature =
                HttpAuthenticationFeature.basic(ONOS_USERNAME, ONOS_PASSWORD);

        final Client client = ClientBuilder.newClient();
        client.register(feature);
        String host = "http://" + k8sNode.managementIp().toString() + ":" + ONOS_PORT + "/";
        String endpoint = host + OS_K8S_INTEGRATION_EP;
        WebTarget wt = client.target(endpoint).path(path);
        Response response = wt.request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonString));
        final int status = response.getStatus();

        if (status != 200) {
            log.error("Failed to install/uninstall openstack k8s CNI PT node port rules.");
        }
    }

    private class InternalK8sNodeListener implements K8sNodeListener {

        @Override
        public boolean isRelevant(K8sNodeEvent event) {
            return event.subject().mode() == PASSTHROUGH;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case K8S_NODE_OFF_BOARDED:
                    eventExecutor.execute(() -> processNodeOffboard(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setCniPtNodeRules(k8sNode, true);
            setCniPtNodePortRules(k8sNode, true);
        }

        private void processNodeOffboard(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setCniPtNodeRules(k8sNode, false);
            setCniPtNodePortRules(k8sNode, false);
        }
    }
}