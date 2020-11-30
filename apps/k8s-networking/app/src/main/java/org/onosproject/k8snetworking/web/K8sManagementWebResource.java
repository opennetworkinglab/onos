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
package org.onosproject.k8snetworking.web;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.k8snetworking.api.K8sEndpointsAdminService;
import org.onosproject.k8snetworking.api.K8sIngressAdminService;
import org.onosproject.k8snetworking.api.K8sNamespaceAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyAdminService;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sServiceAdminService;
import org.onosproject.k8snetworking.util.K8sNetworkingUtil;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostAdminService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.syncPortFromPod;
import static org.onosproject.k8snode.api.K8sNode.Type.MASTER;
import static org.onosproject.k8snode.api.K8sNode.Type.MINION;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;

/**
 * REST interface for synchronizing kubernetes network states and rules.
 */
@Path("management")
public class K8sManagementWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long MID_SLEEP_MS = 3000; // we wait 3s
    private static final long SLEEP_MS = 10000; // we wait 10s
    private static final long TIMEOUT_MS = 30000; // we wait 30s

    private static final String MESSAGE_ALL = "Received all %s request";
    private static final String REMOVE = "REMOVE";

    private final K8sApiConfigService configService = get(K8sApiConfigService.class);
    private final K8sPodAdminService podAdminService = get(K8sPodAdminService.class);
    private final K8sNamespaceAdminService namespaceAdminService =
                                            get(K8sNamespaceAdminService.class);
    private final K8sServiceAdminService serviceAdminService =
                                            get(K8sServiceAdminService.class);
    private final K8sIngressAdminService ingressAdminService =
                                            get(K8sIngressAdminService.class);
    private final K8sEndpointsAdminService endpointsAdminService =
                                            get(K8sEndpointsAdminService.class);
    private final K8sNetworkAdminService networkAdminService =
                                            get(K8sNetworkAdminService.class);
    private final K8sNodeAdminService nodeAdminService =
                                            get(K8sNodeAdminService.class);
    private final K8sHostAdminService hostAdminService =
                                            get(K8sHostAdminService.class);
    private final K8sNetworkPolicyAdminService policyAdminService =
                                            get(K8sNetworkPolicyAdminService.class);

    /**
     * Synchronizes the all states with kubernetes API server.
     *
     * @return 200 OK with sync result, 404 not found
     * @throws InterruptedException exception
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync/states")
    public Response syncStates() {
        K8sApiConfig config =
                configService.apiConfigs().stream().findAny().orElse(null);
        if (config == null) {
            throw new ItemNotFoundException("Failed to find valid kubernetes API configuration.");
        }

        KubernetesClient client = K8sNetworkingUtil.k8sClient(config);

        if (client == null) {
            throw new ItemNotFoundException("Failed to connect to kubernetes API server.");
        }

        client.namespaces().list().getItems().forEach(ns -> {
            if (namespaceAdminService.namespace(ns.getMetadata().getUid()) != null) {
                namespaceAdminService.updateNamespace(ns);
            } else {
                namespaceAdminService.createNamespace(ns);
            }
        });

        client.services().inAnyNamespace().list().getItems().forEach(svc -> {
            if (serviceAdminService.service(svc.getMetadata().getUid()) != null) {
                serviceAdminService.updateService(svc);
            } else {
                serviceAdminService.createService(svc);
            }
        });

        client.endpoints().inAnyNamespace().list().getItems().forEach(ep -> {
            if (endpointsAdminService.endpoints(ep.getMetadata().getUid()) != null) {
                endpointsAdminService.updateEndpoints(ep);
            } else {
                endpointsAdminService.createEndpoints(ep);
            }
        });

        client.pods().inAnyNamespace().list().getItems().forEach(pod -> {
            if (podAdminService.pod(pod.getMetadata().getUid()) != null) {
                podAdminService.updatePod(pod);
            } else {
                podAdminService.createPod(pod);
            }

            syncPortFromPod(pod, networkAdminService);
        });

        client.extensions().ingresses().inAnyNamespace().list().getItems().forEach(ingress -> {
            if (ingressAdminService.ingress(ingress.getMetadata().getUid()) != null) {
                ingressAdminService.updateIngress(ingress);
            } else {
                ingressAdminService.createIngress(ingress);
            }
        });

        client.network().networkPolicies().inAnyNamespace().list().getItems().forEach(policy -> {
            if (policyAdminService.networkPolicy(policy.getMetadata().getUid()) != null) {
                policyAdminService.updateNetworkPolicy(policy);
            } else {
                policyAdminService.createNetworkPolicy(policy);
            }
        });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Synchronizes the flow rules.
     *
     * @return 200 OK with sync result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync/rules")
    public Response syncRules() {

        syncRulesBase();
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Removes all nodes and hosts.
     *
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed, and
     * 304 NOT_MODIFIED without the updated config
     */
    @DELETE
    @Path("purge/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response purgeAll() {
        log.trace(String.format(MESSAGE_ALL, REMOVE));

        Set<String> portIds = networkAdminService.ports().stream().map(K8sPort::portId).collect(Collectors.toSet());
        portIds.forEach(networkAdminService::removePort);

        try {
            sleep(MID_SLEEP_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during node synchronization...");
        }

        Set<String> masters = nodeAdminService.nodes(K8sNode.Type.MASTER).stream()
                .map(K8sNode::hostname).collect(Collectors.toSet());
        Set<String> workers = nodeAdminService.nodes(K8sNode.Type.MINION).stream()
                .map(K8sNode::hostname).collect(Collectors.toSet());

        for (String hostname : workers) {
            nodeAdminService.removeNode(hostname);
            try {
                sleep(MID_SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }
        }

        for (String hostname : masters) {
            nodeAdminService.removeNode(hostname);
            try {
                sleep(MID_SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }
        }

        Set<IpAddress> allHosts = hostAdminService.hosts().stream().map(K8sHost::hostIp).collect(Collectors.toSet());
        for (IpAddress hostIp : allHosts) {
            hostAdminService.removeHost(hostIp);
            try {
                sleep(MID_SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }
        }

        return Response.noContent().build();
    }

    private void syncRulesBase() {
        nodeAdminService.completeNodes(MASTER).forEach(this::syncRulesBaseForNode);
        nodeAdminService.completeNodes(MINION).forEach(this::syncRulesBaseForNode);
    }

    private void syncRulesBaseForNode(K8sNode k8sNode) {
        K8sNode updated = k8sNode.updateState(K8sNodeState.INIT);
        nodeAdminService.updateNode(updated);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        while (nodeAdminService.node(k8sNode.hostname()).state() != COMPLETE) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }

            if (nodeAdminService.node(k8sNode.hostname()).state() == COMPLETE) {
                break;
            } else {
                nodeAdminService.updateNode(updated);
                log.info("Failed to synchronize flow rules, retrying...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            log.info("Successfully synchronize flow rules for node {}!", k8sNode.hostname());
        } else {
            log.warn("Failed to synchronize flow rules for node {}.", k8sNode.hostname());
        }
    }
}
