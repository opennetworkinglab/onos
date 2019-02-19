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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.mastership.MastershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes pod watcher used for feeding pod information.
 */
@Component(immediate = true)
public class K8sPodWatcher {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodAdminService k8sPodAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigService k8sApiConfigService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final Watcher<Pod> internalK8sPodWatcher = new InternalK8sPodWatcher();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        initWatcher();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void initWatcher() {
        K8sApiConfig config =
                k8sApiConfigService.apiConfigs().stream().findAny().orElse(null);
        if (config == null) {
            log.error("Failed to find valid kubernetes API configuration.");
            return;
        }

        KubernetesClient client = k8sClient(config);

        if (client == null) {
            log.error("Failed to connect to kubernetes API server.");
            return;
        }

        client.pods().watch(internalK8sPodWatcher);
    }

    private class InternalK8sPodWatcher implements Watcher<Pod> {

        @Override
        public void eventReceived(Action action, Pod pod) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(pod));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(pod));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(pod));
                    break;
                case ERROR:
                    log.warn("Failures processing pod manipulation.");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onClose(KubernetesClientException e) {
            log.info("Pod watcher OnClose: {}" + e);
        }

        private void processAddition(Pod pod) {
            if (!isMaster()) {
                return;
            }

            log.info("Process pod {} creating event from API server.",
                    pod.getMetadata().getName());

            k8sPodAdminService.createPod(pod);
        }

        private void processModification(Pod pod) {
            if (!isMaster()) {
                return;
            }

            log.info("Process pod {} updating event from API server.",
                    pod.getMetadata().getName());

            k8sPodAdminService.updatePod(pod);
        }

        private void processDeletion(Pod pod) {
            if (!isMaster()) {
                return;
            }

            log.info("Process pod {} removal event from API server.",
                    pod.getMetadata().getName());

            k8sPodAdminService.removePod(pod.getMetadata().getUid());
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }
    }
}
