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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.mastership.MastershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes network-attachment-definition watcher used for feeding kubevirt network information.
 */
@Component(immediate = true)
public class NetworkAttachmentDefinitionWatcher {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_CONFIG = "network-config";
    private static final String TYPE = "type";
    private static final String MTU = "mtu";
    private static final String SEGMENT_ID = "segmentId";
    private static final String GATEWAY_IP = "gatewayIp";
    private static final String CIDR = "cidr";
    private static final String HOST_ROUTES = "hostRoutes";
    private static final String IP_POOL = "ipPool";
    private static final String START = "start";
    private static final String END = "end";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService configService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalNetworkAttachmentDefinitionWatcher
            watcher = new InternalNetworkAttachmentDefinitionWatcher();
    private final InternalKubevirtApiConfigListener
            configListener = new InternalKubevirtApiConfigListener();

    CustomResourceDefinitionContext nadCrdCxt = new CustomResourceDefinitionContext
            .Builder()
            .withGroup("k8s.cni.cncf.io")
            .withScope("Namespaced")
            .withVersion("v1")
            .withPlural("network-attachment-definitions")
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

            KubernetesClient client = k8sClient(configService);

            if (client != null) {
                try {
                    client.customResource(nadCrdCxt).watch(watcher);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class InternalNetworkAttachmentDefinitionWatcher
            implements Watcher<String> {

        @Override
        public void eventReceived(Action action, String resource) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(resource));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(resource));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(resource));
                    break;
                case ERROR:
                    log.warn("Failures processing network-attachment-definition manipulation.");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            log.warn("Network-attachment-definition watcher OnClose", e);
        }

        private void processAddition(String resource) {
            if (!isMaster()) {
                return;
            }

            String name = parseName(resource);

            log.trace("Process NetworkAttachmentDefinition {} creating event from API server.",
                    name);

            KubevirtNetwork network = parseKubevirtNetwork(resource);
            if (network != null) {
                adminService.createNetwork(network);
            }
        }

        private void processModification(String resource) {
            if (!isMaster()) {
                return;
            }

            String name = parseName(resource);

            log.trace("Process NetworkAttachmentDefinition {} updating event from API server.",
                    name);

            KubevirtNetwork network = parseKubevirtNetwork(resource);
            if (network != null) {
                adminService.updateNetwork(network);
            }
        }

        private void processDeletion(String resource) {
            if (!isMaster()) {
                return;
            }

            String name = parseName(resource);

            log.trace("Process NetworkAttachmentDefinition {} removal event from API server.",
                    name);

            adminService.removeNetwork(name);
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        private String parseName(String resource) {
            try {
                JSONObject json = new JSONObject(resource);
                return json.getJSONObject("metadata").getString("name");
            } catch (JSONException e) {
                log.error("");
            }
            return "";
        }

        private KubevirtNetwork parseKubevirtNetwork(String resource) {
            try {
                JSONObject json = new JSONObject(resource);
                String name = parseName(resource);
                JSONObject annots = json.getJSONObject("metadata").getJSONObject("annotations");
                String networkConfig = annots.getString(NETWORK_CONFIG);
                if (networkConfig != null) {
                    KubevirtNetwork.Builder builder = DefaultKubevirtNetwork.builder();

                    JSONObject configJson = new JSONObject(networkConfig);
                    String type = configJson.getString(TYPE);
                    Integer mtu = configJson.getInt(MTU);
                    String gatewayIp = configJson.getString(GATEWAY_IP);

                    if (!type.equalsIgnoreCase(FLAT.name())) {
                        builder.segmentId(configJson.getString(SEGMENT_ID));
                    }

                    String cidr = configJson.getString(CIDR);

                    JSONObject poolJson = configJson.getJSONObject(IP_POOL);
                    if (poolJson != null) {
                        String start = poolJson.getString(START);
                        String end = poolJson.getString(END);
                        builder.ipPool(new KubevirtIpPool(
                                IpAddress.valueOf(start), IpAddress.valueOf(end)));
                    }

                    builder.networkId(name).name(name).type(Type.valueOf(type))
                            .mtu(mtu).gatewayIp(IpAddress.valueOf(gatewayIp)).cidr(cidr);

                    return builder.build();
                }
            } catch (JSONException e) {
                log.error("Failed to parse network attachment definition object");
            }

            return null;
        }
    }
}
