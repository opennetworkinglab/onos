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
package org.onosproject.kubevirtnode.impl;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
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
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.MASTER;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.kubevirtnode.api.KubevirtNodeService.APP_ID;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INIT;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.buildKubevirtNode;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes node watcher used for feeding node information.
 */
@Component(immediate = true)
public class KubevirtNodeWatcher {

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
    protected KubevirtNodeAdminService kubevirtNodeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService kubevirtApiConfigService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final Watcher<Node> internalKubevirtNodeWatcher = new InternalKubevirtNodeWatcher();
    private final InternalKubevirtApiConfigListener
            internalKubevirtApiConfigListener = new InternalKubevirtApiConfigListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtApiConfigService.addListener(internalKubevirtApiConfigListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtApiConfigService.removeListener(internalKubevirtApiConfigListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void instantiateNodeWatcher() {
        KubevirtApiConfig config = kubevirtApiConfigService.apiConfig();
        if (config == null) {
            return;
        }
        KubernetesClient client = k8sClient(config);

        if (client != null) {
            client.nodes().watch(internalKubevirtNodeWatcher);
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
                    eventExecutor.execute(this::processConfigUpdating);
                    break;
                case KUBEVIRT_API_CONFIG_CREATED:
                case KUBEVIRT_API_CONFIG_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processConfigUpdating() {
            if (!isRelevantHelper()) {
                return;
            }

            instantiateNodeWatcher();
        }
    }

    private class InternalKubevirtNodeWatcher implements Watcher<Node> {

        @Override
        public void eventReceived(Action action, Node node) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(node));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(node));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(node));
                    break;
                case ERROR:
                    log.warn("Failures processing node manipulation.");
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            // due to the bugs in fabric8, node watcher might be closed,
            // we will re-instantiate the node watcher in this case
            // FIXME: https://github.com/fabric8io/kubernetes-client/issues/2135
            log.warn("Node watcher OnClose, re-instantiate the node watcher...");
            instantiateNodeWatcher();
        }

        private void processAddition(Node node) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process node {} creating event from API server.",
                    node.getMetadata().getName());

            KubevirtNode kubevirtNode = buildKubevirtNode(node);
            if (kubevirtNode.type() == WORKER || kubevirtNode.type() == GATEWAY) {
                if (!kubevirtNodeAdminService.hasNode(kubevirtNode.hostname())) {
                    kubevirtNodeAdminService.createNode(kubevirtNode);
                }
            }
        }

        private void processModification(Node node) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process node {} updating event from API server.",
                    node.getMetadata().getName());

            KubevirtNode original = buildKubevirtNode(node);
            KubevirtNode existing = kubevirtNodeAdminService.node(node.getMetadata().getName());

            // if a master node is annotated as a gateway node, we simply add
            // the node into the cluster
            if (original.type() == GATEWAY && existing == null) {
                kubevirtNodeAdminService.createNode(original);
            }

            // if a gateway annotation removed from the master node, we simply remove
            // the node from the cluster
            if (original.type() == MASTER && existing != null && existing.type() == GATEWAY) {
                kubevirtNodeAdminService.removeNode(original.hostname());
            }

            if (existing != null) {
                // we update the kubevirt node and re-run bootstrapping,
                // if the updated node has different phyInts and data IP
                // this means we assume that the node's hostname, type and mgmt IP
                // are immutable
                if (!original.phyIntfs().equals(existing.phyIntfs()) ||
                        !original.dataIp().equals(existing.dataIp())) {
                    kubevirtNodeAdminService.updateNode(original.updateState(INIT));
                }
            }
        }

        private void processDeletion(Node node) {
            if (!isMaster()) {
                return;
            }

            log.trace("Process node {} removal event from API server.",
                    node.getMetadata().getName());

            KubevirtNode existing = kubevirtNodeAdminService.node(node.getMetadata().getName());

            if (existing != null) {
                kubevirtNodeAdminService.removeNode(node.getMetadata().getName());
            }
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }
    }
}
