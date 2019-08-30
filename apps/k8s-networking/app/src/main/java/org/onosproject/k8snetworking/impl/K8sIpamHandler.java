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

import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sIpamAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.mastership.MastershipService;
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
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getSubnetIps;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Initializes and purges the kubernetes IPAM.
 */
@Component(immediate = true)
public class K8sIpamHandler {

    private final Logger log = getLogger(getClass());

    private static final String IP_ADDRESS = "ipAddress";
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
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodService k8sPodService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sIpamAdminService k8sIpamAdminService;

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
        k8sPodService.removeListener(k8sPodListener);
        k8sNetworkService.removeListener(k8sNetworkListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNetworkEvent event) {
            switch (event.type()) {
                case K8S_NETWORK_CREATED:
                    eventExecutor.execute(() -> processNetworkAddition(event));
                    break;
                case K8S_NETWORK_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processNetworkAddition(K8sNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            Set<IpAddress> ips = getSubnetIps(event.subject().cidr());
            String networkId = event.subject().networkId();
            k8sIpamAdminService.initializeIpPool(networkId, ips);

            k8sPodService.pods().stream()
                    .filter(p -> p.getStatus().getPodIP() != null)
                    .filter(p -> p.getMetadata().getAnnotations() != null)
                    .filter(p -> networkId.equals(p.getMetadata()
                                 .getAnnotations().get(NETWORK_ID)))
                    .forEach(p -> {
                        String podIp = p.getStatus().getPodIP();

                        // if the POD with valid IP address has not yet been
                        // added into IPAM IP pool, we will reserve that IP address
                        // for the POD
                        if (!k8sIpamAdminService.allocatedIps(networkId)
                                .contains(IpAddress.valueOf(podIp))) {
                            k8sIpamAdminService.reserveIp(networkId, IpAddress.valueOf(podIp));
                        }
                    });
        }

        private void processNetworkRemoval(K8sNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            k8sIpamAdminService.purgeIpPool(event.subject().networkId());
        }
    }

    private class InternalK8sPodListener implements K8sPodListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sPodEvent event) {
            switch (event.type()) {
                case K8S_POD_CREATED:
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

            Map<String, String> annots = pod.getMetadata().getAnnotations();

            if (annots == null || annots.isEmpty()) {
                return;
            }

            String annotIp = annots.get(IP_ADDRESS);
            String annotNetwork = annots.get(NETWORK_ID);
            String podIp = pod.getStatus().getPodIP();

            if (podIp == null && annotIp == null) {
                return;
            }

            if (annotNetwork == null) {
                return;
            }

            if (!StringUtils.equals(annotIp, podIp)) {
                return;
            }

            k8sIpamAdminService.availableIps(annotNetwork);

            // if the kubernetes network has been initialized, we may have
            // empty available IP pool, in this case, we will postpone IP reserve
            // process until finishing kubernetes network initialization
            if (!containIp(annotIp, annotNetwork)) {
                return;
            }

            k8sIpamAdminService.reserveIp(annotNetwork, IpAddress.valueOf(podIp));
        }

        private boolean containIp(String podIp, String networkId) {
            return k8sIpamAdminService.availableIps(networkId).stream()
                    .anyMatch(i -> i.toString().equals(podIp));
        }
    }
}
