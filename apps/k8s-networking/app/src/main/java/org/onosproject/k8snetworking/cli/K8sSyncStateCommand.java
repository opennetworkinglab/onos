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
package org.onosproject.k8snetworking.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sEndpointsAdminService;
import org.onosproject.k8snetworking.api.K8sIngressAdminService;
import org.onosproject.k8snetworking.api.K8sNamespaceAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyAdminService;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sServiceAdminService;
import org.onosproject.k8snetworking.util.K8sNetworkingUtil;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;

import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.CLI_CONTAINERS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_IP_ADDRESSES_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_LABELS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAMESPACE_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_PHASE_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_PORTS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_TYPES_LENGTH;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.genFormatString;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.syncPortFromPod;

/**
 * Synchronizes kubernetes states.
 */
@Service
@Command(scope = "onos", name = "k8s-sync-states",
        description = "Synchronizes all kubernetes states")
public class K8sSyncStateCommand extends AbstractShellCommand {

    private static final String POD_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_NAMESPACE_LENGTH, CLI_IP_ADDRESS_LENGTH,
            CLI_CONTAINERS_LENGTH));
    private static final String SERVICE_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_IP_ADDRESS_LENGTH, CLI_PORTS_LENGTH));
    private static final String ENDPOINTS_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_IP_ADDRESSES_LENGTH, CLI_PORTS_LENGTH));
    private static final String INGRESS_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_NAMESPACE_LENGTH, CLI_IP_ADDRESS_LENGTH));
    private static final String NETWORK_POLICY_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_NAMESPACE_LENGTH, CLI_TYPES_LENGTH));
    private static final String NAMESPACE_FORMAT = genFormatString(ImmutableList.of(
            CLI_NAME_LENGTH, CLI_PHASE_LENGTH, CLI_LABELS_LENGTH));

    @Override
    protected void doExecute() {
        K8sApiConfigService configService = get(K8sApiConfigService.class);
        K8sPodAdminService podAdminService = get(K8sPodAdminService.class);
        K8sNamespaceAdminService namespaceAdminService =
                get(K8sNamespaceAdminService.class);
        K8sServiceAdminService serviceAdminService =
                get(K8sServiceAdminService.class);
        K8sIngressAdminService ingressAdminService =
                get(K8sIngressAdminService.class);
        K8sEndpointsAdminService endpointsAdminService =
                get(K8sEndpointsAdminService.class);
        K8sNetworkAdminService networkAdminService =
                get(K8sNetworkAdminService.class);
        K8sNetworkPolicyAdminService networkPolicyAdminService =
                get(K8sNetworkPolicyAdminService.class);

        K8sApiConfig config =
                configService.apiConfigs().stream().findAny().orElse(null);
        if (config == null) {
            log.error("Failed to find valid kubernetes API configuration.");
            return;
        }

        KubernetesClient client = K8sNetworkingUtil.k8sClient(config);

        if (client == null) {
            log.error("Failed to connect to kubernetes API server.");
            return;
        }

        print("\nSynchronizing kubernetes namespaces");
        print(NAMESPACE_FORMAT, "Name", "Phase", "Labels");
        client.namespaces().list().getItems().forEach(ns -> {
            if (namespaceAdminService.namespace(ns.getMetadata().getUid()) != null) {
                namespaceAdminService.updateNamespace(ns);
            } else {
                namespaceAdminService.createNamespace(ns);
            }
            printNamespace(ns);
        });

        print("Synchronizing kubernetes services");
        print(SERVICE_FORMAT, "Name", "Cluster IP", "Ports");
        client.services().inAnyNamespace().list().getItems().forEach(svc -> {
            if (serviceAdminService.service(svc.getMetadata().getUid()) != null) {
                serviceAdminService.updateService(svc);
            } else {
                serviceAdminService.createService(svc);
            }
            printService(svc);
        });

        print("\nSynchronizing kubernetes endpoints");
        print(ENDPOINTS_FORMAT, "Name", "IP Addresses", "Ports");
        client.endpoints().inAnyNamespace().list().getItems().forEach(ep -> {
            if (endpointsAdminService.endpoints(ep.getMetadata().getUid()) != null) {
                endpointsAdminService.updateEndpoints(ep);
            } else {
                endpointsAdminService.createEndpoints(ep);
            }
            printEndpoints(ep);
        });

        print("\nSynchronizing kubernetes pods");
        print(POD_FORMAT, "Name", "Namespace", "IP", "Containers");
        client.pods().inAnyNamespace().list().getItems().forEach(pod -> {
            if (podAdminService.pod(pod.getMetadata().getUid()) != null) {
                podAdminService.updatePod(pod);
            } else {
                podAdminService.createPod(pod);
            }

            syncPortFromPod(pod, networkAdminService);

            printPod(pod);
        });

        print("\nSynchronizing kubernetes ingresses");
        print(INGRESS_FORMAT, "Name", "Namespace", "LB Addresses");
        client.extensions().ingresses().inAnyNamespace().list().getItems().forEach(ingress -> {
            if (ingressAdminService.ingress(ingress.getMetadata().getUid()) != null) {
                ingressAdminService.updateIngress(ingress);
            } else {
                ingressAdminService.createIngress(ingress);
            }
            printIngresses(ingress);
        });

        print("\nSynchronizing kubernetes network policies");
        print(NETWORK_POLICY_FORMAT, "Name", "Namespace", "Types");
        client.network().networkPolicies().inAnyNamespace().list().getItems().forEach(policy -> {
            if (networkPolicyAdminService.networkPolicy(policy.getMetadata().getUid()) != null) {
                networkPolicyAdminService.updateNetworkPolicy(policy);
            } else {
                networkPolicyAdminService.createNetworkPolicy(policy);
            }
            printNetworkPolicy(policy);
        });
    }

    private void printIngresses(Ingress ingress) {

        List<String> lbIps = Lists.newArrayList();

        ingress.getStatus().getLoadBalancer()
                .getIngress().forEach(i -> lbIps.add(i.getIp()));

        print(INGRESS_FORMAT,
                StringUtils.substring(ingress.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                StringUtils.substring(ingress.getMetadata().getNamespace(),
                        0, CLI_NAMESPACE_LENGTH - CLI_MARGIN_LENGTH),
                lbIps.isEmpty() ? "" : lbIps);
    }

    private void printEndpoints(Endpoints endpoints) {
        List<String> ips = Lists.newArrayList();
        List<Integer> ports = Lists.newArrayList();

        endpoints.getSubsets().forEach(e -> {
            e.getAddresses().forEach(a -> ips.add(a.getIp()));
            e.getPorts().forEach(p -> ports.add(p.getPort()));
        });

        print(ENDPOINTS_FORMAT,
                StringUtils.substring(endpoints.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                ips.isEmpty() ? "" : StringUtils.substring(ips.toString(),
                        0, CLI_IP_ADDRESSES_LENGTH - CLI_MARGIN_LENGTH),
                ports.isEmpty() ? "" : ports);
    }

    private void printNamespace(Namespace namespace) {
        print(NAMESPACE_FORMAT,
                StringUtils.substring(namespace.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                namespace.getStatus().getPhase(),
                namespace.getMetadata() != null &&
                        namespace.getMetadata().getLabels() != null &&
                        !namespace.getMetadata().getLabels().isEmpty() ?
                        StringUtils.substring(namespace.getMetadata()
                                        .getLabels().toString(), 0,
                                CLI_LABELS_LENGTH - CLI_MARGIN_LENGTH) : "");
    }

    private void printService(io.fabric8.kubernetes.api.model.Service service) {

        List<Integer> ports = Lists.newArrayList();

        service.getSpec().getPorts().forEach(p -> ports.add(p.getPort()));

        print(SERVICE_FORMAT,
                StringUtils.substring(service.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                StringUtils.substring(service.getSpec().getClusterIP(),
                        0, CLI_IP_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                ports.isEmpty() ? "" : ports);
    }

    private void printPod(Pod pod) {

        List<String> containers = Lists.newArrayList();

        pod.getSpec().getContainers().forEach(c -> containers.add(c.getName()));

        print(POD_FORMAT,
                StringUtils.substring(pod.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                StringUtils.substring(pod.getMetadata().getNamespace(),
                        0, CLI_NAMESPACE_LENGTH - CLI_MARGIN_LENGTH),
                StringUtils.substring(pod.getStatus().getPodIP(),
                        0, CLI_IP_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                containers.isEmpty() ? "" : containers);
    }

    private void printNetworkPolicy(NetworkPolicy policy) {
        print(NETWORK_POLICY_FORMAT,
                StringUtils.substring(policy.getMetadata().getName(),
                        0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                StringUtils.substring(policy.getMetadata().getNamespace(),
                        0, CLI_NAMESPACE_LENGTH - CLI_MARGIN_LENGTH),
                policy.getSpec().getPolicyTypes().isEmpty() ?
                        "" : policy.getSpec().getPolicyTypes());
    }
}
