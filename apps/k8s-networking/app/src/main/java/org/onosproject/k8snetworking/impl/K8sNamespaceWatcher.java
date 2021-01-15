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

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNamespaceAdminService;
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
 * Kubernetes namespace watcher used for feeding namespace information.
 */
@Component(immediate = true)
public class K8sNamespaceWatcher {

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
    protected K8sNamespaceAdminService k8sNamespaceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigService k8sApiConfigService;

    private ApplicationId appId;
    private NodeId localNodeId;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalK8sNamespaceWatcher
            internalK8sNamespaceWatcher = new InternalK8sNamespaceWatcher();
    private final InternalK8sApiConfigListener
            internalK8sApiConfigListener = new InternalK8sApiConfigListener();

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
                client.namespaces().watch(internalK8sNamespaceWatcher);
            }
        }
    }

    private class InternalK8sNamespaceWatcher implements Watcher<Namespace> {

        @Override
        public void eventReceived(Action action, Namespace namespace) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(namespace));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(namespace));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(namespace));
                    break;
                case ERROR:
                    log.warn("Failures processing namespace manipulation.");
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            log.warn("Namespace watcher OnClose", e);
        }

        private void processAddition(Namespace namespace) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process namespace {} creating event from API server.",
                    namespace.getMetadata().getName());

            if (k8sNamespaceAdminService.namespace(namespace.getMetadata().getUid()) == null) {
                k8sNamespaceAdminService.createNamespace(namespace);
            }
        }

        private void processModification(Namespace namespace) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process namespace {} updating event from API server.",
                    namespace.getMetadata().getName());

            if (k8sNamespaceAdminService.namespace(namespace.getMetadata().getUid()) != null) {
                k8sNamespaceAdminService.updateNamespace(namespace);
            }
        }

        private void processDeletion(Namespace namespace) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process namespace {} removal event from API server.",
                    namespace.getMetadata().getName());

            k8sNamespaceAdminService.removeNamespace(namespace.getMetadata().getUid());
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }
    }
}
