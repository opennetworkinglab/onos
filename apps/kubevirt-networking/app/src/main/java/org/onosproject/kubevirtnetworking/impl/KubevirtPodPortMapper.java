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
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPodListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
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

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPorts;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Associates the kubevirt container port and pod.
 */
@Component(immediate = true)
public class KubevirtPodPortMapper {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_STATUS_KEY = "k8s.v1.cni.cncf.io/network-status";
    private static final long SLEEP_MS = 2000; // we wait 2s

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

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
                case KUBEVIRT_POD_CREATED:
                case KUBEVIRT_POD_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processPodUpdate(Pod pod) {
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

            KubevirtNode node = kubevirtNodeService.node(pod.getSpec().getNodeName());

            if (node == null) {
                log.warn("POD scheduled node name {} is not ready, " +
                         "we wait for a while...", pod.getSpec().getNodeName());
                try {
                    // we wait until all k8s nodes are available
                    sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Set<KubevirtPort> ports = getPorts(kubevirtNodeService,
                                      kubevirtNetworkAdminService.networks(), pod);
            if (ports.size() == 0) {
                return;
            }

            ports.forEach(port -> {
                KubevirtPort existing = kubevirtPortAdminService.port(port.macAddress());

                if (existing != null) {
                    if (port.deviceId() != null && existing.deviceId() == null) {
                        KubevirtPort updated = existing.updateDeviceId(port.deviceId());
                        // internal we update device ID of kubevirt port
                        kubevirtPortAdminService.updatePort(updated);
                    }
                }
            });
        }
    }
}
