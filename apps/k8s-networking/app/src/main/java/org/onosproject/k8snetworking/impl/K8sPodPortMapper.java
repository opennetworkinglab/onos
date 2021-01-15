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

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snode.api.K8sApiConfigService;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Associates the kubernetes container port and pod.
 */
@Component(immediate = true)
public class K8sPodPortMapper {

    private final Logger log = getLogger(getClass());

    private static final String PORT_ID = "portId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String NETWORK_ID = "networkId";

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
    protected K8sNetworkAdminService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodAdminService k8sPodService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigService k8sApiConfigService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InternalK8sNetworkListener k8sNetworkListener =
            new InternalK8sNetworkListener();
    private final InternalK8sPodListener k8sPodListener =
            new InternalK8sPodListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sNetworkService.addListener(k8sNetworkListener);
        k8sPodService.addListener(k8sPodListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNetworkService.removeListener(k8sNetworkListener);
        k8sPodService.removeListener(k8sPodListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalK8sPodListener implements K8sPodListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sPodEvent event) {
            switch (event.type()) {
                case K8S_POD_CREATED:
                case K8S_POD_UPDATED:
                    eventExecutor.execute(() -> processPodCreation(event.subject()));
                    break;
                case K8S_POD_REMOVED:
                default:
                    break;
            }
        }

        private void processPodCreation(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            KubernetesClient client = k8sClient(k8sApiConfigService);

            if (client == null) {
                return;
            }

            // if the annotations were configured, we will not update it
            if (pod.getMetadata().getAnnotations() != null) {
                return;
            }

            Set<K8sPort> ports = k8sNetworkService.ports();

            // TODO: we assume that POD IP is unique, there might be other
            // variable which preserves better uniqueness
            ports.stream().filter(p -> p.ipAddress().toString()
                            .equals(pod.getStatus().getPodIP()))
                .forEach(p -> {
                    Map<String, String> annotations = Maps.newConcurrentMap();
                    annotations.put(PORT_ID, p.portId());
                    annotations.put(NETWORK_ID, p.networkId());
                    annotations.put(DEVICE_ID, p.deviceId().toString());
                    annotations.put(IP_ADDRESS, p.ipAddress().toString());
                    annotations.put(MAC_ADDRESS, p.macAddress().toString());

                    if (p.portNumber() != null) {
                        annotations.put(PORT_NUMBER, p.portNumber().toString());
                    }

                    client.pods().inNamespace(pod.getMetadata().getNamespace())
                            .withName(pod.getMetadata().getName())
                            .edit(r -> new PodBuilder(r)
                                    .editMetadata()
                                    .addToAnnotations(annotations)
                                    .endMetadata().build()
                            );
                });
        }
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        private boolean isRelevantHelper(K8sNetworkEvent event) {
            return mastershipService.isLocalMaster(event.port().deviceId());
        }

        @Override
        public void event(K8sNetworkEvent event) {
            switch (event.type()) {
                case K8S_PORT_UPDATED:
                case K8S_PORT_REMOVED:
                    // no need to process port removal event...
                default:
                    break;
            }
        }
    }
}
