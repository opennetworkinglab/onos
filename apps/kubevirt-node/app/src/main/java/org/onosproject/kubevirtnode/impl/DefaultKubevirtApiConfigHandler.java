/*
 * Copyright 2020-present Open Networking Foundation
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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
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
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.State.CONNECTED;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigService.APP_ID;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the state of KubeVirt API server configuration.
 */
@Component(immediate = true)
public class DefaultKubevirtApiConfigHandler {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigAdminService configAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeAdminService nodeAdminService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final KubevirtApiConfigListener configListener = new InternalKubevirtApiConfigListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        configAdminService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configAdminService.removeListener(configListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    /**
     * Checks the validity of the given kubernetes API server configuration.
     *
     * @param config kubernetes API server configuration
     * @return validity result
     */
    private boolean checkApiServerConfig(KubevirtApiConfig config) {
        KubernetesClient k8sClient = k8sClient(config);
        return k8sClient != null && k8sClient.getApiVersion() != null;
    }

    private class InternalKubevirtApiConfigListener implements KubevirtApiConfigListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtApiConfigEvent event) {
            switch (event.type()) {
                case KUBEVIRT_API_CONFIG_CREATED:
                    eventExecutor.execute(() -> processConfigCreation(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processConfigCreation(KubevirtApiConfig config) {
            if (!isRelevantHelper()) {
                return;
            }

            if (checkApiServerConfig(config)) {
                KubevirtApiConfig newConfig = config.updateState(CONNECTED);
                configAdminService.updateApiConfig(newConfig);
            }
        }
    }
}
