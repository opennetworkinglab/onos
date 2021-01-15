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

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sIngressAdminService;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigListener;
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
 * Kubernetes ingress watcher used for feeding ingress information.
 */
@Component(immediate = true)
public class K8sIngressWatcher {

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
    protected K8sIngressAdminService k8sIngressAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigService k8sApiConfigService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final Watcher<Ingress> internalK8sIngressWatcher =
            new InternalK8sIngressWatcher();
    private final InternalK8sApiConfigListener internalK8sApiConfigListener =
            new InternalK8sApiConfigListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sApiConfigService.addListener(internalK8sApiConfigListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sApiConfigService.removeListener(internalK8sApiConfigListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalK8sApiConfigListener implements K8sApiConfigListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sApiConfigEvent event) {

            switch (event.type()) {
                case K8S_API_CONFIG_UPDATED:
                    eventExecutor.execute(this::processConfigUpdating);
                    break;
                case K8S_API_CONFIG_CREATED:
                case K8S_API_CONFIG_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processConfigUpdating() {
            if (!isRelevantHelper()) {
                return;
            }

            KubernetesClient client = k8sClient(k8sApiConfigService);

            if (client != null) {
                client.extensions().ingresses().inAnyNamespace().watch(internalK8sIngressWatcher);
            }
        }
    }

    private class InternalK8sIngressWatcher implements Watcher<Ingress> {

        @Override
        public void eventReceived(Action action, Ingress ingress) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(ingress));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(ingress));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(ingress));
                    break;
                case ERROR:
                    log.warn("Failures processing ingress manipulation.");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            log.warn("Ingress watcher OnClose", e);
        }

        private void processAddition(Ingress ingress) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process ingress {} creating event from API server.",
                    ingress.getMetadata().getName());

            if (k8sIngressAdminService.ingress(ingress.getMetadata().getUid()) == null) {
                k8sIngressAdminService.createIngress(ingress);
            }
        }

        private void processModification(Ingress ingress) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process ingress {} updating event from API server.",
                    ingress.getMetadata().getName());

            if (k8sIngressAdminService.ingress(ingress.getMetadata().getUid()) != null) {
                k8sIngressAdminService.updateIngress(ingress);
            }
        }

        private void processDeletion(Ingress ingress) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process ingress {} removal event from API server.",
                    ingress.getMetadata().getName());

            k8sIngressAdminService.removeIngress(ingress.getMetadata().getUid());
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }
    }
}
