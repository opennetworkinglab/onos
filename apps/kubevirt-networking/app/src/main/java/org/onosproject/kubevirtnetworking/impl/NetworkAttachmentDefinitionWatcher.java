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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.parseResourceName;
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
    private static final String DEFAULT_ROUTE = "defaultRoute";
    private static final String CIDR = "cidr";
    private static final String HOST_ROUTES = "hostRoutes";
    private static final String DESTINATION = "destination";
    private static final String NEXTHOP = "nexthop";
    private static final String IP_POOL = "ipPool";
    private static final String START = "start";
    private static final String END = "end";
    private static final String DNSES = "dnses";

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

    private void instantiateWatcher() {
        KubernetesClient client = k8sClient(configService);

        if (client != null) {
            try {
                client.customResource(nadCrdCxt).watch(watcher);
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
            // due to the bugs in fabric8, the watcher might be closed,
            // we will re-instantiate the watcher in this case
            // FIXME: https://github.com/fabric8io/kubernetes-client/issues/2135
            log.warn("Network-attachment-definition watcher OnClose, re-instantiate the watcher...");

            instantiateWatcher();
        }

        private void processAddition(String resource) {
            if (!isMaster()) {
                return;
            }

            String name = parseResourceName(resource);

            log.trace("Process NetworkAttachmentDefinition {} creating event from API server.",
                    name);

            KubevirtNetwork network = parseKubevirtNetwork(resource);
            if (network != null) {
                if (adminService.network(network.networkId()) == null) {
                    adminService.createNetwork(network);
                }
            }
        }

        private void processModification(String resource) {
            if (!isMaster()) {
                return;
            }

            String name = parseResourceName(resource);

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

            String name = parseResourceName(resource);

            log.trace("Process NetworkAttachmentDefinition {} removal event from API server.",
                    name);

            if (adminService.network(name) != null) {
                adminService.removeNetwork(name);
            }
        }

        private boolean isMaster() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        private KubevirtNetwork parseKubevirtNetwork(String resource) {
                JsonObject json = JsonObject.readFrom(resource);
                String name = parseResourceName(resource);
                JsonObject annots = json.get("metadata").asObject().get("annotations").asObject();
                if (annots.get(NETWORK_CONFIG) == null) {
                    // SR-IOV network does not contain network-config field
                    return null;
                }
                String networkConfig = annots.get(NETWORK_CONFIG).asString();
                if (networkConfig != null) {
                    KubevirtNetwork.Builder builder = DefaultKubevirtNetwork.builder();

                    JsonObject configJson = JsonObject.readFrom(networkConfig);
                    String type = configJson.get(TYPE).asString().toUpperCase(Locale.ROOT);
                    Integer mtu = configJson.get(MTU).asInt();
                    String gatewayIp = configJson.getString(GATEWAY_IP, "");
                    boolean defaultRoute = configJson.getBoolean(DEFAULT_ROUTE, false);

                    if (!type.equalsIgnoreCase(FLAT.name())) {
                        builder.segmentId(configJson.getString(SEGMENT_ID, ""));
                    }

                    String cidr = configJson.getString(CIDR, "");

                    JsonObject poolJson = configJson.get(IP_POOL).asObject();
                    if (poolJson != null) {
                        String start = poolJson.getString(START, "");
                        String end = poolJson.getString(END, "");
                        builder.ipPool(new KubevirtIpPool(
                                IpAddress.valueOf(start), IpAddress.valueOf(end)));
                    }

                    if (configJson.get(HOST_ROUTES) != null) {
                        JsonArray routesJson = configJson.get(HOST_ROUTES).asArray();
                        Set<KubevirtHostRoute> hostRoutes = new HashSet<>();
                        if (routesJson != null) {
                            for (int i = 0; i < routesJson.size(); i++) {
                                JsonObject route = routesJson.get(i).asObject();
                                String destinationStr = route.getString(DESTINATION, "");
                                String nexthopStr = route.getString(NEXTHOP, "");

                                if (StringUtils.isNotEmpty(destinationStr) &&
                                        StringUtils.isNotEmpty(nexthopStr)) {
                                    hostRoutes.add(new KubevirtHostRoute(
                                            IpPrefix.valueOf(destinationStr),
                                            IpAddress.valueOf(nexthopStr)));
                                }
                            }
                        }
                        builder.hostRoutes(hostRoutes);
                    }

                    if (configJson.get(DNSES) != null) {
                        JsonArray dnsesJson = configJson.get(DNSES).asArray();
                        Set<IpAddress> dnses = new HashSet<>();
                        if (dnsesJson != null) {
                            for (int i = 0; i < dnsesJson.size(); i++) {
                                String dns = dnsesJson.get(i).asString();
                                if (StringUtils.isNotEmpty(dns)) {
                                    dnses.add(IpAddress.valueOf(dns));
                                }
                            }
                        }
                        builder.dnses(dnses);
                    }

                    builder.networkId(name).name(name).type(Type.valueOf(type))
                            .mtu(mtu).gatewayIp(IpAddress.valueOf(gatewayIp))
                            .defaultRoute(defaultRoute).cidr(cidr);

                    return builder.build();
                }
            return null;
        }
    }
}
