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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.mastership.MastershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPorts;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.waitFor;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes VMI watcher used for feeding VMI information.
 */
@Component(immediate = true)
public class KubevirtVmiWatcher {

    private final Logger log = getLogger(getClass());

    private static final String STATUS = "status";
    private static final String NODE_NAME = "nodeName";
    private static final String METADATA = "metadata";
    private static final String NAME = "name";

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
    protected KubevirtApiConfigService configService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtVmiWatcher watcher = new InternalKubevirtVmiWatcher();
    private final InternalKubevirtApiConfigListener
            configListener = new InternalKubevirtApiConfigListener();

    CustomResourceDefinitionContext vmiCrdCxt = new CustomResourceDefinitionContext
            .Builder()
            .withGroup("kubevirt.io")
            .withScope("Namespaced")
            .withVersion("v1")
            .withPlural("virtualmachineinstances")
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
                client.customResource(vmiCrdCxt).watch(watcher);
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

    private class InternalKubevirtVmiWatcher implements Watcher<String> {

        @Override
        public void eventReceived(Action action, String s) {
            switch (action) {
                case ADDED:
                case MODIFIED:
                    eventExecutor.execute(() -> processAddition(s));
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

            String nodeName = parseNodeName(resource);
            String vmiName = parseVmiName(resource);

            if (nodeName == null) {
                return;
            }

            KubevirtNode node = nodeService.node(nodeName);

            if (node == null) {
                log.warn("VMI {} scheduled on node {} is not ready, " +
                                "we wait for a while...", vmiName, nodeName);
                waitFor(2);
            }

            Set<KubevirtPort> ports = getPorts(nodeService,
                                        networkAdminService.networks(), resource);

            if (ports.size() == 0) {
                return;
            }

            ports.forEach(port -> {
                KubevirtPort existing = portAdminService.port(port.macAddress());

                if (existing != null) {
                    if (port.deviceId() != null && existing.deviceId() == null) {
                        KubevirtPort updated = existing.updateDeviceId(port.deviceId());
                        // internal we update device ID of kubevirt port
                        portAdminService.updatePort(updated);
                    }
                }
            });
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        private String parseVmiName(String resource) {
            String vmiName = null;

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resource);
                JsonNode metadataJson = json.get(METADATA);
                JsonNode vmiNameJson = metadataJson.get(NAME);
                vmiName = vmiNameJson != null ? vmiNameJson.asText() : null;
            } catch (IOException e) {
                log.error("Failed to parse kubevirt VMI name");
            }

            return vmiName;
        }

        private String parseNodeName(String resource) {
            String nodeName = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resource);
                JsonNode statusJson = json.get(STATUS);
                JsonNode nodeNameJson = statusJson.get(NODE_NAME);
                nodeName = nodeNameJson != null ? nodeNameJson.asText() : null;
            } catch (IOException e) {
                log.error("Failed to parse kubevirt VMI nodename");
            }

            return nodeName;
        }
    }
}
