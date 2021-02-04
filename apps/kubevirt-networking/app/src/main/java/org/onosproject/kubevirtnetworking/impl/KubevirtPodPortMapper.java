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
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPodListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
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
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Associates the kubevirt container port and pod.
 */
@Component(immediate = true)
public class KubevirtPodPortMapper {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_STATUS_KEY = "k8s.v1.cni.cncf.io/network-status";
    private static final String NAME = "name";
    private static final String IPS = "ips";
    private static final String NETWORK_PREFIX = "default/";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortAdminService kubevirtPortAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkAdminService kubevirtNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPodAdminService kubevirtPodAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService kubevirtApiConfigService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtPodListener kubevirtPodListener =
            new InternalKubevirtPodListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtPodAdminService.addListener(kubevirtPodListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtPodAdminService.removeListener(kubevirtPodListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
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
                    eventExecutor.execute(() -> processPodDeletion(event.subject()));
                    break;
                case KUBEVIRT_POD_CREATED:
                    eventExecutor.execute(() -> processPodCreation(event.subject()));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processPodCreation(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            Map<String, String> annots = pod.getMetadata().getAnnotations();
            if (annots == null) {
                return;
            }

            if (!annots.containsKey(NETWORK_STATUS_KEY)) {
                return;
            }

            try {
                String networkStatusStr = pod.getMetadata().getAnnotations().get(NETWORK_STATUS_KEY);
                JSONArray networkStatus = new JSONArray(networkStatusStr);
                for (int i = 0; i < networkStatus.length(); i++) {
                    JSONObject object = networkStatus.getJSONObject(i);
                    String name = object.getString(NAME);
                    KubevirtNetwork jsonNetwork = kubevirtNetworkAdminService.networks().stream()
                            .filter(n -> (NETWORK_PREFIX + n.name()).equals(name))
                            .findAny().orElse(null);
                    if (jsonNetwork != null) {
                        JSONArray ips = object.getJSONArray(IPS);
                        if (ips != null && ips.length() > 0) {
                            IpAddress ip = IpAddress.valueOf(ips.getString(0));
                            kubevirtNetworkAdminService.reserveIp(jsonNetwork.networkId(), ip);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to reserve IP address", e);
            }

            KubevirtPort port = getPort(kubevirtNetworkAdminService.networks(), pod);
            if (port == null) {
                return;
            }

            if (kubevirtPortAdminService.port(port.macAddress()) == null) {
                kubevirtPortAdminService.createPort(port);
            }
        }

        private void processPodUpdate(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtPort port = getPort(kubevirtNetworkAdminService.networks(), pod);
            if (port == null) {
                return;
            }

            if (kubevirtPortAdminService.port(port.macAddress()) != null) {
                return;
            }

            if (port.ipAddress() == null) {
                try {
                    IpAddress ip = kubevirtNetworkAdminService.allocateIp(port.networkId());
                    port = port.updateIpAddress(ip);

                    // update the POD annotation to inject the allocated IP address
                    String networkStatusStr = pod.getMetadata().getAnnotations().get(NETWORK_STATUS_KEY);
                    JSONArray networkStatus = new JSONArray(networkStatusStr);
                    for (int i = 0; i < networkStatus.length(); i++) {
                        JSONObject object = networkStatus.getJSONObject(i);
                        String name = object.getString(NAME);
                        KubevirtNetwork jsonNetwork = kubevirtNetworkAdminService.networks().stream()
                                .filter(n -> (NETWORK_PREFIX + n.name()).equals(name))
                                .findAny().orElse(null);
                        if (jsonNetwork != null) {
                            JSONArray ipsJson = new JSONArray();
                            ipsJson.put(ip.toString());
                            object.put(IPS, ipsJson);
                        }
                    }
                    Map<String, String> annots = pod.getMetadata().getAnnotations();
                    annots.put(NETWORK_STATUS_KEY, networkStatus.toString(4));

                    KubernetesClient client = k8sClient(kubevirtApiConfigService);

                    if (client == null) {
                        return;
                    }

                    client.pods().inNamespace(pod.getMetadata().getNamespace())
                            .withName(pod.getMetadata().getName())
                            .edit(r -> new PodBuilder(r)
                                    .editMetadata()
                                    .addToAnnotations(annots)
                                    .endMetadata().build()
                            );
                } catch (Exception e) {
                    log.error("Failed to allocate IP address", e);
                }
            }
            kubevirtPortAdminService.createPort(port);
        }

        private void processPodDeletion(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtPort port = getPort(kubevirtNetworkAdminService.networks(), pod);
            if (port == null) {
                return;
            }

            if (port.ipAddress() != null) {
                kubevirtNetworkAdminService.releaseIp(port.networkId(), port.ipAddress());
            }

            kubevirtPortAdminService.removePort(port.macAddress());
        }
    }
}
