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

import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sEndpointsAdminService;
import org.onosproject.k8snetworking.api.K8sIngressAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sServiceAdminService;
import org.onosproject.k8snetworking.util.K8sNetworkingUtil;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Map;

import static org.onosproject.k8snetworking.api.K8sPort.State.INACTIVE;

/**
 * Synchronizes kubernetes states.
 */
@Service
@Command(scope = "onos", name = "k8s-sync-states",
        description = "Synchronizes all kubernetes states")
public class K8sSyncStateCommand extends AbstractShellCommand {

    private static final String POD_FORMAT = "%-50s%-15s%-15s%-30s";
    private static final String SERVICE_FORMAT = "%-50s%-30s%-30s";
    private static final String ENDPOINTS_FORMAT = "%-50s%-50s%-20s";
    private static final String INGRESS_FORMAT = "%-50s%-15s%-30s";

    private static final String PORT_ID = "portId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String NETWORK_ID = "networkId";

    @Override
    protected void doExecute() {
        K8sApiConfigService configService = get(K8sApiConfigService.class);
        K8sPodAdminService podAdminService = get(K8sPodAdminService.class);
        K8sServiceAdminService serviceAdminService =
                get(K8sServiceAdminService.class);
        K8sIngressAdminService ingressAdminService =
                get(K8sIngressAdminService.class);
        K8sEndpointsAdminService endpointsAdminService =
                get(K8sEndpointsAdminService.class);
        K8sNetworkAdminService networkAdminService =
                get(K8sNetworkAdminService.class);

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

    }

    private void printIngresses(Ingress ingress) {

        List<String> lbIps = Lists.newArrayList();

        ingress.getStatus().getLoadBalancer()
                .getIngress().forEach(i -> lbIps.add(i.getIp()));

        print(INGRESS_FORMAT,
                ingress.getMetadata().getName(),
                ingress.getMetadata().getNamespace(),
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
                endpoints.getMetadata().getName(),
                ips.isEmpty() ? "" : ips,
                ports.isEmpty() ? "" : ports);
    }

    private void printService(io.fabric8.kubernetes.api.model.Service service) {

        List<Integer> ports = Lists.newArrayList();

        service.getSpec().getPorts().forEach(p -> ports.add(p.getPort()));

        print(SERVICE_FORMAT,
                service.getMetadata().getName(),
                service.getSpec().getClusterIP(),
                ports.isEmpty() ? "" : ports);
    }

    private void printPod(Pod pod) {

        List<String> containers = Lists.newArrayList();

        pod.getSpec().getContainers().forEach(c -> containers.add(c.getName()));

        print(POD_FORMAT,
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                pod.getStatus().getPodIP(),
                containers.isEmpty() ? "" : containers);
    }

    private void syncPortFromPod(Pod pod, K8sNetworkAdminService adminService) {
        Map<String, String> annotations = pod.getMetadata().getAnnotations();
        if (annotations != null && !annotations.isEmpty() &&
                annotations.get(PORT_ID) != null) {
            String portId = annotations.get(PORT_ID);

            K8sPort oldPort = adminService.port(portId);

            String networkId = annotations.get(NETWORK_ID);
            DeviceId deviceId = DeviceId.deviceId(annotations.get(DEVICE_ID));
            PortNumber portNumber = PortNumber.portNumber(annotations.get(PORT_NUMBER));
            IpAddress ipAddress = IpAddress.valueOf(annotations.get(IP_ADDRESS));
            MacAddress macAddress = MacAddress.valueOf(annotations.get(MAC_ADDRESS));

            K8sPort newPort = DefaultK8sPort.builder()
                    .portId(portId)
                    .networkId(networkId)
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .macAddress(macAddress)
                    .portNumber(portNumber)
                    .state(INACTIVE)
                    .build();

            if (oldPort == null) {
                adminService.createPort(newPort);
            } else {
                adminService.updatePort(newPort);
            }
        }
    }
}
