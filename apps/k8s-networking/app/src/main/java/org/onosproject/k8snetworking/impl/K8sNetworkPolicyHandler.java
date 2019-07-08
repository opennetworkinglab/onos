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

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPort;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyListener;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyService;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_CIDR_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_PREFIX;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.shiftIpDomain;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the ACL by referring to the network policy defined through kubernetes.
 */
@Component(immediate = true)
public class K8sNetworkPolicyHandler {

    private final Logger log = getLogger(getClass());

    private static final String DIRECTION_INGRESS = "ingress";
    private static final String DIRECTION_EGRESS = "egress";

    private static final String PROTOCOL_TCP = "tcp";
    private static final String PROTOCOL_UDP = "udp";

    private static final int HOST_PREFIX = 32;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodService k8sPodService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkPolicyService k8sNetworkPolicyService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPodListener internalPodListener =
            new InternalPodListener();
    private final InternalNetworkPolicyListener internalNetworkPolicyListener =
            new InternalNetworkPolicyListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sPodService.addListener(internalPodListener);
        k8sNetworkPolicyService.addListener(internalNetworkPolicyListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        k8sPodService.removeListener(internalPodListener);
        k8sNetworkPolicyService.removeListener(internalNetworkPolicyListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setBlockRulesByPolicy(NetworkPolicy policy, boolean install) {
        Map<String, String> labels =
                policy.getSpec().getPodSelector().getMatchLabels();
        Map<String, List<String>> filter = Maps.newConcurrentMap();

        k8sPodService.pods().forEach(p -> {
            p.getMetadata().getLabels().forEach((k, v) -> {
                if (labels.get(k) != null && labels.get(k).equals(v)) {
                    filter.put(p.getStatus().getPodIP(), policy.getSpec().getPolicyTypes());
                }
            });
        });

        setBlockRules(filter, install);
    }

    private void setBlockRulesByPod(Pod pod, boolean install) {
        Map<String, List<String>> filter = Maps.newConcurrentMap();

        k8sNetworkPolicyService.networkPolicies().forEach(p -> {
            Map<String, String> labels = p.getSpec().getPodSelector().getMatchLabels();
            pod.getMetadata().getLabels().forEach((k, v) -> {
                if (labels.get(k) != null && labels.get(k).equals(v)) {
                    filter.put(pod.getStatus().getPodIP(), p.getSpec().getPolicyTypes());
                }
            });
        });

        setBlockRules(filter, install);
    }

    private void setBlockRules(Map<String, List<String>> filter, boolean install) {
        filter.forEach((k, v) -> {
            v.forEach(d -> {
                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4);
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
                if (d.equalsIgnoreCase(DIRECTION_INGRESS)) {
                    sBuilder.matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ACL_INGRESS_TABLE);
                } else if (d.equalsIgnoreCase(DIRECTION_EGRESS)) {
                    sBuilder.matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ACL_EGRESS_TABLE);
                }

                k8sNodeService.completeNodes().forEach(n -> {
                    k8sFlowRuleService.setRule(
                            appId,
                            n.intgBridge(),
                            sBuilder.build(),
                            tBuilder.build(),
                            PRIORITY_CIDR_RULE,
                            ACL_TABLE,
                            install
                    );
                });
            });
        });
    }

    private void setAllowRulesByPolicy(NetworkPolicy policy, boolean install) {
        Map<String, Map<String, List<NetworkPolicyPort>>> white = Maps.newConcurrentMap();

        policy.getSpec().getIngress().forEach(i -> {
            Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
            direction.put(DIRECTION_INGRESS, i.getPorts());
            i.getFrom().stream()
                    .filter(p -> p.getIpBlock() != null)
                    .forEach(p -> {
                white.compute(p.getIpBlock().getCidr(), (k, v) -> direction);

                // TODO: need to handle namespace label later

                Map<String, String> podLabels = p.getPodSelector().getMatchLabels();
                k8sPodService.pods().forEach(pod -> {
                    pod.getMetadata().getLabels().forEach((k, v) -> {
                        if (podLabels.get(k) != null && podLabels.get(k).equals(v)) {
                            white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                                    SHIFTED_IP_PREFIX), (m, n) -> direction);
                        }
                    });
                });
            });
        });

        policy.getSpec().getEgress().forEach(e -> {
            Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
            direction.put(DIRECTION_EGRESS, e.getPorts());
            e.getTo().stream()
                    .filter(p -> p.getIpBlock() != null)
                    .forEach(p -> {
                white.compute(p.getIpBlock().getCidr(), (k, v) -> {
                    if (v != null) {
                        v.put(DIRECTION_EGRESS, e.getPorts());
                        return v;
                    } else {
                        return direction;
                    }
                });

                // TODO: need to handle namespace label later

                Map<String, String> podLabels = p.getPodSelector().getMatchLabels();
                k8sPodService.pods().forEach(pod -> {
                    pod.getMetadata().getLabels().forEach((k, v) -> {
                        if (podLabels.get(k) != null && podLabels.get(k).equals(v)) {
                            white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                                    SHIFTED_IP_PREFIX), (m, n) -> {
                                if (n != null) {
                                    n.put(DIRECTION_EGRESS, e.getPorts());
                                    return n;
                                } else {
                                    return direction;
                                }
                            });
                        }
                    });
                });
            });
        });

        setAllowRules(white, install);
    }

    private void setAllowRulesByPod(Pod pod, boolean install) {
        Map<String, Map<String, List<NetworkPolicyPort>>> white = Maps.newConcurrentMap();

        k8sNetworkPolicyService.networkPolicies().forEach(policy -> {
            policy.getSpec().getIngress().forEach(i -> {
                Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
                direction.put(DIRECTION_INGRESS, i.getPorts());
                i.getFrom().forEach(peer -> {

                    // TODO: need to handle namespace label later

                    Map<String, String> podLabels = peer.getPodSelector().getMatchLabels();
                    pod.getMetadata().getLabels().forEach((k, v) -> {
                        if (podLabels.get(k) != null && podLabels.get(k).equals(v)) {
                            white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                                    SHIFTED_IP_PREFIX), (m, n) -> direction);
                        }
                    });
                });
            });
        });

        k8sNetworkPolicyService.networkPolicies().forEach(policy -> {
            policy.getSpec().getEgress().forEach(e -> {
                Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
                direction.put(DIRECTION_EGRESS, e.getPorts());
                e.getTo().forEach(p -> {

                    // TODO: need to handle namespace label later

                    Map<String, String> podLabels = p.getPodSelector().getMatchLabels();
                    pod.getMetadata().getLabels().forEach((k, v) -> {
                        if (podLabels.get(k) != null && podLabels.get(k).equals(v)) {
                            white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                                    SHIFTED_IP_PREFIX), (m, n) -> {
                                if (n != null) {
                                    n.put(DIRECTION_EGRESS, e.getPorts());
                                    return n;
                                } else {
                                    return direction;
                                }
                            });
                        }
                    });
                });
            });
        });

        setAllowRules(white, install);
    }

    private void setAllowRules(Map<String, Map<String, List<NetworkPolicyPort>>> white, boolean install) {
        white.forEach((k, v) -> {
            v.forEach((pk, pv) -> {
                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4);
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
                if (pk.equalsIgnoreCase(DIRECTION_INGRESS)) {
                    sBuilder.matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ROUTING_TABLE);

                    if (pv.size() == 0) {
                        k8sNodeService.completeNodes().forEach(n -> {
                            k8sFlowRuleService.setRule(
                                    appId,
                                    n.intgBridge(),
                                    sBuilder.build(),
                                    tBuilder.build(),
                                    PRIORITY_CIDR_RULE,
                                    ACL_INGRESS_TABLE,
                                    install
                            );
                        });
                    } else {
                        pv.forEach(p -> {
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_TCP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
                                sBuilder.matchTcpSrc(TpPort.tpPort(p.getPort().getIntVal()));
                            }
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_UDP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);
                                sBuilder.matchUdpSrc(TpPort.tpPort(p.getPort().getIntVal()));
                            }

                            k8sNodeService.completeNodes().forEach(n -> {
                                k8sFlowRuleService.setRule(
                                        appId,
                                        n.intgBridge(),
                                        sBuilder.build(),
                                        tBuilder.build(),
                                        PRIORITY_CIDR_RULE,
                                        ACL_INGRESS_TABLE,
                                        install
                                );
                            });
                        });
                    }
                } else if (pk.equalsIgnoreCase(DIRECTION_EGRESS)) {
                    sBuilder.matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ROUTING_TABLE);

                    if (pv.size() == 0) {
                        k8sNodeService.completeNodes().forEach(n -> {
                            k8sFlowRuleService.setRule(
                                    appId,
                                    n.intgBridge(),
                                    sBuilder.build(),
                                    tBuilder.build(),
                                    PRIORITY_CIDR_RULE,
                                    ACL_EGRESS_TABLE,
                                    install
                            );
                        });
                    } else {
                        pv.forEach(p -> {
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_TCP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
                                sBuilder.matchTcpDst(TpPort.tpPort(p.getPort().getIntVal()));
                            }
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_UDP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);
                                sBuilder.matchUdpDst(TpPort.tpPort(p.getPort().getIntVal()));
                            }

                            k8sNodeService.completeNodes().forEach(n -> {
                                k8sFlowRuleService.setRule(
                                        appId,
                                        n.intgBridge(),
                                        sBuilder.build(),
                                        tBuilder.build(),
                                        PRIORITY_CIDR_RULE,
                                        ACL_EGRESS_TABLE,
                                        install
                                );
                            });
                        });
                    }

                } else {
                    log.error("In correct direction has been specified at network policy.");
                }
            });
        });
    }

    private class InternalPodListener implements K8sPodListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sPodEvent event) {
            Pod pod = event.subject();
            switch (event.type()) {
                case K8S_POD_CREATED:
                case K8S_POD_UPDATED:
                    eventExecutor.execute(() -> processPodCreation(pod));
                    break;
                case K8S_POD_REMOVED:
                    eventExecutor.execute(() -> processPodRemoval(pod));
                    break;
                default:
                    break;
            }
        }

        private void processPodCreation(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setBlockRulesByPod(pod, true);
            setAllowRulesByPod(pod, true);
        }

        private void processPodRemoval(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setBlockRulesByPod(pod, false);
            setAllowRulesByPod(pod, false);
        }
    }

    private class InternalNetworkPolicyListener implements K8sNetworkPolicyListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNetworkPolicyEvent event) {
            NetworkPolicy policy = event.subject();
            switch (event.type()) {
                case K8S_NETWORK_POLICY_CREATED:
                case K8S_NETWORK_POLICY_UPDATED:
                    eventExecutor.execute(() -> processNetworkPolicyCreation(policy));
                    break;
                case K8S_NETWORK_POLICY_REMOVED:
                    eventExecutor.execute(() -> processNetworkPolicyRemoval(policy));
                    break;
                default:
                    break;
            }
        }

        private void processNetworkPolicyCreation(NetworkPolicy policy) {
            if (!isRelevantHelper()) {
                return;
            }

            setBlockRulesByPolicy(policy, true);
            setAllowRulesByPolicy(policy, true);
        }

        private void processNetworkPolicyRemoval(NetworkPolicy policy) {
            if (!isRelevantHelper()) {
                return;
            }

            setBlockRulesByPolicy(policy, false);
            setAllowRulesByPolicy(policy, false);
        }
    }
}
