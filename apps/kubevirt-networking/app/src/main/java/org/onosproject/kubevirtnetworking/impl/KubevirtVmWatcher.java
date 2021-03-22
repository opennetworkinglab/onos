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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.apache.commons.lang3.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPorts;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.parseResourceName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes VM watcher used for feeding VM information.
 */
@Component(immediate = true)
public class KubevirtVmWatcher {

    private final Logger log = getLogger(getClass());

    private static final long SLEEP_MS = 3000; // we wait 3s

    private static final String SPEC = "spec";
    private static final String TEMPLATE = "template";
    private static final String METADATA = "metadata";
    private static final String ANNOTATIONS = "annotations";
    private static final String DOMAIN = "domain";
    private static final String DEVICES = "devices";
    private static final String INTERFACES = "interfaces";
    private static final String NETWORK_POLICIES = "networkPolicies";
    private static final String SECURITY_GROUPS = "securityGroups";
    private static final String NAME = "name";
    private static final String NETWORK = "network";
    private static final String MAC = "macAddress";
    private static final String IP = "ipAddress";
    private static final String DEFAULT = "default";
    private static final String NETWORK_SUFFIX = "-net";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkAdminService networkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortAdminService portAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPodService podService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService configService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtVmWatcher watcher = new InternalKubevirtVmWatcher();
    private final InternalKubevirtApiConfigListener
            configListener = new InternalKubevirtApiConfigListener();

    CustomResourceDefinitionContext vmCrdCxt = new CustomResourceDefinitionContext
            .Builder()
            .withGroup("kubevirt.io")
            .withScope("Namespaced")
            .withVersion("v1")
            .withPlural("virtualmachines")
            .build();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        configService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void instantiateWatcher() {
        KubernetesClient client = k8sClient(configService);

        if (client != null) {
            try {
                client.customResource(vmCrdCxt).watch(watcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class InternalKubevirtApiConfigListener implements KubevirtApiConfigListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtApiConfigEvent event) {

            switch (event.type()) {
                case KUBEVIRT_API_CONFIG_UPDATED:
                    eventExecutor.execute(this::processConfigUpdate);
                    break;
                case KUBEVIRT_API_CONFIG_CREATED:
                case KUBEVIRT_API_CONFIG_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processConfigUpdate() {
            if (!isRelevantHelper()) {
                return;
            }

            instantiateWatcher();
        }
    }

    private class InternalKubevirtVmWatcher implements Watcher<String> {

        @Override
        public void eventReceived(Action action, String resource) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(resource));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(resource));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(resource));
                    break;
                case ERROR:
                    log.warn("Failures processing VM manipulation.");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            log.warn("VM watcher OnClose, re-instantiate the VM watcher...");
            instantiateWatcher();
        }

        private void processAddition(String resource) {
            if (!isMaster()) {
                return;
            }

            parseMacAddresses(resource).forEach((mac, net) -> {
                KubevirtPort port = DefaultKubevirtPort.builder()
                        .macAddress(mac)
                        .networkId(net)
                        .build();

                String name = parseResourceName(resource);

                Set<String> sgs = parseSecurityGroups(resource);
                port = port.updateSecurityGroups(sgs);

                Map<String, IpAddress> ips = parseIpAddresses(resource);
                IpAddress ip;
                IpAddress existingIp = ips.get(port.networkId());

                KubevirtNetwork network = networkAdminService.network(port.networkId());
                if (network == null) {
                    try {
                        sleep(SLEEP_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                KubevirtPort existingPort = portAdminService.port(port.macAddress());

                if (existingIp == null) {

                    KubernetesClient client = k8sClient(configService);

                    if (client == null) {
                        return;
                    }

                    ip = networkAdminService.allocateIp(port.networkId());
                    log.info("IP address {} is allocated from network {}", ip, port.networkId());

                    try {
                        // we wait a while to avoid potentially referring to old version resource
                        // FIXME: we may need to find a better solution to avoid this
                        sleep(SLEEP_MS);
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> newResource = client.customResource(vmCrdCxt).get(DEFAULT, name);
                        String newResourceStr = mapper.writeValueAsString(newResource);
                        String updatedResource = updateIpAddress(newResourceStr, port.networkId(), ip);
                        client.customResource(vmCrdCxt).edit(DEFAULT, name, updatedResource);
                    } catch (IOException | InterruptedException e) {
                        log.error("Failed to annotate IP addresses", e);
                    } catch (KubernetesClientException kce) {
                        log.error("Failed to update VM resource", kce);
                    }

                } else {
                    if (existingPort != null) {
                        return;
                    }

                    ip = existingIp;
                    networkAdminService.reserveIp(port.networkId(), ip);
                    log.info("IP address {} is reserved from network {}", ip, port.networkId());
                }

                if (existingPort == null) {
                    KubevirtPort updated = port.updateIpAddress(ip);

                    DeviceId deviceId = getDeviceId(podService.pods(), port);

                    if (deviceId != null) {
                        updated = updated.updateDeviceId(deviceId);
                    }

                    portAdminService.createPort(updated);
                }
            });
        }

        private void processModification(String resource) {
            if (!isMaster()) {
                return;
            }

            parseMacAddresses(resource).forEach((mac, net) -> {
                KubevirtPort port = DefaultKubevirtPort.builder()
                        .macAddress(mac)
                        .networkId(net)
                        .build();

                KubevirtPort existing = portAdminService.port(port.macAddress());

                if (existing == null) {
                    return;
                }

                Set<String> sgs = parseSecurityGroups(resource);
                portAdminService.updatePort(existing.updateSecurityGroups(sgs));
            });
        }

        private void processDeletion(String resource) {
            if (!isMaster()) {
                return;
            }

            parseMacAddresses(resource).forEach((mac, net) -> {
                KubevirtPort port = portAdminService.port(mac);
                if (port != null) {
                    networkAdminService.releaseIp(port.networkId(), port.ipAddress());
                    log.info("IP address {} is released from network {}",
                            port.ipAddress(), port.networkId());

                    portAdminService.removePort(mac);
                }
            });
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        // FIXME: to obtains the device ID, we have to search through
        // existing POD inventory, need to find a better wat to obtain device ID
        private DeviceId getDeviceId(Set<Pod> pods, KubevirtPort port) {
            Set<Pod> defaultPods = pods.stream()
                    .filter(pod -> pod.getMetadata().getNamespace().equals(DEFAULT))
                    .collect(Collectors.toSet());

            Set<KubevirtPort> allPorts = new HashSet<>();
            for (Pod pod : defaultPods) {
                allPorts.addAll(getPorts(nodeService, networkAdminService.networks(), pod));
            }

            return allPorts.stream().filter(p -> p.macAddress().equals(port.macAddress()))
                    .map(KubevirtPort::deviceId).findFirst().orElse(null);
        }

        private Map<String, IpAddress> parseIpAddresses(String resource) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resource);
                JsonNode metadata = json.get(SPEC).get(TEMPLATE).get(METADATA);

                JsonNode annots = metadata.get(ANNOTATIONS);
                if (annots == null) {
                    return new HashMap<>();
                }

                JsonNode interfacesJson = annots.get(INTERFACES);
                if (interfacesJson == null) {
                    return new HashMap<>();
                }

                Map<String, IpAddress> result = new HashMap<>();

                String interfacesString = interfacesJson.asText();
                ArrayNode interfaces = (ArrayNode) mapper.readTree(interfacesString);
                for (JsonNode intf : interfaces) {
                    String network = intf.get(NETWORK).asText();
                    String ip = intf.get(IP).asText();
                    result.put(network, IpAddress.valueOf(ip));
                }

                return result;
            } catch (IOException e) {
                log.error("Failed to parse kubevirt VM IP addresses");
            }

            return new HashMap<>();
        }

        private String updateIpAddress(String resource, String network, IpAddress ip) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode json = (ObjectNode) mapper.readTree(resource);
                ObjectNode spec = (ObjectNode) json.get(SPEC);
                ObjectNode template = (ObjectNode) spec.get(TEMPLATE);
                ObjectNode metadata = (ObjectNode) template.get(METADATA);
                ObjectNode annots = (ObjectNode) metadata.get(ANNOTATIONS);

                if (!annots.has(INTERFACES)) {
                    annots.put(INTERFACES, "[]");
                }

                String intfs = annots.get(INTERFACES).asText();
                ArrayNode intfsJson = (ArrayNode) mapper.readTree(intfs);

                ObjectNode intf = mapper.createObjectNode();
                intf.put(NETWORK, network);
                intf.put(IP, ip.toString());

                intfsJson.add(intf);

                annots.put(INTERFACES, intfsJson.toString());
                metadata.set(ANNOTATIONS, annots);
                template.set(METADATA, metadata);
                spec.set(TEMPLATE, template);
                json.set(SPEC, spec);

                return json.toString();

            } catch (IOException e) {
                log.error("Failed to update kubevirt VM IP addresses");
            }
            return null;
        }

        private Set<String> parseSecurityGroups(String resource) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resource);
                JsonNode metadata = json.get(SPEC).get(TEMPLATE).get(METADATA);

                JsonNode annots = metadata.get(ANNOTATIONS);
                if (annots == null) {
                    return new HashSet<>();
                }

                JsonNode sgsJson = annots.get(SECURITY_GROUPS);
                if (sgsJson == null) {
                    return new HashSet<>();
                }

                Set<String> result = new HashSet<>();
                ArrayNode sgs = (ArrayNode) mapper.readTree(sgsJson.asText());
                for (JsonNode sg : sgs) {
                    result.add(sg.asText());
                }

                return result;

            } catch (IOException e) {
                log.error("Failed to parse kubevirt security group IDs.");
            }

            return new HashSet<>();
        }

        private Map<MacAddress, String> parseMacAddresses(String resource) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resource);
                JsonNode spec = json.get(SPEC).get(TEMPLATE).get(SPEC);
                ArrayNode interfaces = (ArrayNode) spec.get(DOMAIN).get(DEVICES).get(INTERFACES);

                Map<MacAddress, String> result = new HashMap<>();
                for (JsonNode intf : interfaces) {
                    String network = intf.get(NAME).asText();
                    JsonNode macJson = intf.get(MAC);

                    if (!DEFAULT.equals(network) && macJson != null) {
                        String compact = StringUtils.substringBeforeLast(network, NETWORK_SUFFIX);
                        MacAddress mac = MacAddress.valueOf(macJson.asText());
                        result.put(mac, compact);
                    }
                }

                return result;
            } catch (IOException e) {
                log.error("Failed to parse kubevirt VM MAC addresses");
            }

            return new HashMap<>();
        }
    }
}
