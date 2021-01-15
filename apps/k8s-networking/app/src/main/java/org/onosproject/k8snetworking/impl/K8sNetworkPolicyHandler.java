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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPort;
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
import org.onosproject.k8snetworking.api.K8sNamespaceEvent;
import org.onosproject.k8snetworking.api.K8sNamespaceListener;
import org.onosproject.k8snetworking.api.K8sNamespaceService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyListener;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyService;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.k8snetworking.api.K8sServiceEvent;
import org.onosproject.k8snetworking.api.K8sServiceListener;
import org.onosproject.k8snetworking.api.K8sServiceService;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ACL_EGRESS_BLACK_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_EGRESS_WHITE_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_INGRESS_BLACK_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_INGRESS_WHITE_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ACL_TABLE;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_METADATA_MASK;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_NAMESPACE_HASH;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_SEGMENT_ID;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_SERVICE_IP_NONE;
import static org.onosproject.k8snetworking.api.Constants.GROUPING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.NAMESPACE_TABLE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_CIDR_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_NAMESPACE_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_PREFIX;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.SERVICE_IP_CIDR_DEFAULT;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.namespaceHashByNamespace;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.namespaceHashByPodIp;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.namespaceHashByServiceIp;
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

    private static final String KUBE_SYSTEM = "kube-system";

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
    protected K8sServiceService k8sServiceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkPolicyService k8sNetworkPolicyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNamespaceService k8sNamespaceService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPodListener internalPodListener =
            new InternalPodListener();
    private final InternalServiceListener internalServiceListener =
            new InternalServiceListener();
    private final InternalNetworkPolicyListener internalNetworkPolicyListener =
            new InternalNetworkPolicyListener();
    private final InternalNamespaceListener internalNamespaceListener =
            new InternalNamespaceListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sPodService.addListener(internalPodListener);
        k8sServiceService.addListener(internalServiceListener);
        k8sNetworkPolicyService.addListener(internalNetworkPolicyListener);
        k8sNamespaceService.addListener(internalNamespaceListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        k8sPodService.removeListener(internalPodListener);
        k8sServiceService.removeListener(internalServiceListener);
        k8sNetworkPolicyService.removeListener(internalNetworkPolicyListener);
        k8sNamespaceService.removeListener(internalNamespaceListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setBlockRulesByPolicy(NetworkPolicy policy, boolean install) {
        final Map<String, List<String>> filter = Maps.newConcurrentMap();

        k8sPodService.pods().forEach(pod ->
            filter.putAll(getBlockRuleFilter(pod, policy)));

        setBlockRules(filter, install);
    }

    private void setBlockRulesByPod(Pod pod, boolean install) {
        final Map<String, List<String>> filter = Maps.newConcurrentMap();

        k8sNetworkPolicyService.networkPolicies().forEach(policy ->
            filter.putAll(getBlockRuleFilter(pod, policy)));

        setBlockRules(filter, install);
    }

    private Map<String, List<String>> getBlockRuleFilter(Pod pod, NetworkPolicy policy) {

        // if the POD is not included in the namespace of the given policy,
        // we do not block the POD
        if (!pod.getMetadata().getNamespace().equals(policy.getMetadata().getNamespace())) {
            return Maps.newConcurrentMap();
        }

        Map<String, String> labels = policy.getSpec().getPodSelector().getMatchLabels();
        Map<String, List<String>> filter = Maps.newConcurrentMap();
        String podIp = pod.getStatus().getPodIP();
        List<String> policyTypes = policy.getSpec().getPolicyTypes();

        if (podIp != null && policyTypes != null) {
            if (labels == null) {
                filter.put(podIp, policyTypes);
            } else {
                pod.getMetadata().getLabels().forEach((k, v) -> {
                    if (labels.get(k) != null && labels.get(k).equals(v)) {
                        filter.put(podIp, policyTypes);
                    }
                });
            }
        }

        return filter;
    }

    private void setBlockRules(Map<String, List<String>> filter, boolean install) {
        filter.forEach((k, v) -> {
            v.forEach(d -> {
                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                        .matchEthType(TYPE_IPV4);
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

                Integer nsHash = namespaceHashByPodIp(k8sPodService, k8sNamespaceService, k);
                if (nsHash != null) {
                    tBuilder.setTunnelId(nsHash);
                }

                if (d.equalsIgnoreCase(DIRECTION_INGRESS)) {
                    sBuilder.matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ACL_INGRESS_WHITE_TABLE);
                    setPolicyRulesBase(sBuilder, tBuilder, ACL_TABLE, install);
                } else if (d.equalsIgnoreCase(DIRECTION_EGRESS)) {
                    // original IP
                    sBuilder.matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(k), HOST_PREFIX));
                    tBuilder.transition(ACL_EGRESS_WHITE_TABLE);
                    setPolicyRulesBase(sBuilder, tBuilder, ACL_TABLE, install);


                    // shifted IP
                    sBuilder.matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(
                            shiftIpDomain(k, SHIFTED_IP_PREFIX)), HOST_PREFIX));
                    setPolicyRulesBase(sBuilder, tBuilder, ACL_TABLE, install);
                }
            });
        });
    }

    private void setDefaultAllowNamespaceRules(Namespace namespace, boolean install) {

        String ns = namespace.getMetadata().getName();
        if (KUBE_SYSTEM.equalsIgnoreCase(ns)) {
            setAllowNamespaceRulesBase(0, namespace.hashCode(),
                    DIRECTION_INGRESS, install);
        }
    }

    private void setDefaultAllowServiceRules(boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(SERVICE_IP_CIDR_DEFAULT));
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(DEFAULT_SEGMENT_ID)
                .transition(ROUTING_TABLE);

        setPolicyRulesBase(sBuilder, tBuilder, ACL_INGRESS_WHITE_TABLE, install);
    }

    private void setAllowNamespaceRulesBase(int tunnelId, int metadata,
                                            String direction, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        if (tunnelId != 0) {
            sBuilder.matchTunnelId(tunnelId);
        }

        sBuilder.matchMetadata(metadata);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(DEFAULT_SEGMENT_ID)
                .transition(ROUTING_TABLE);

        if (DIRECTION_INGRESS.equals(direction)) {
            setPolicyRulesBase(sBuilder, tBuilder, ACL_INGRESS_WHITE_TABLE, install);
        }
    }

    private void setAllowRulesByPolicy(NetworkPolicy policy, boolean install) {
        Map<String, Map<String, List<NetworkPolicyPort>>>
                white = Maps.newConcurrentMap();

        int nsHash = namespaceHashByNamespace(k8sNamespaceService,
                policy.getMetadata().getNamespace());

        List<NetworkPolicyIngressRule> ingress = policy.getSpec().getIngress();
        if (ingress != null && ingress.size() == 1) {
            NetworkPolicyIngressRule rule = ingress.get(0);
            if (rule.getFrom().size() == 0 && rule.getPorts().size() == 0) {
                setAllowAllRule(nsHash, DIRECTION_INGRESS, install);
            }
        }

        policy.getSpec().getIngress().forEach(i -> {
            Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
            direction.put(DIRECTION_INGRESS, i.getPorts());
            i.getFrom().forEach(peer -> {

                // IP block
                if (peer.getIpBlock() != null) {
                    if (peer.getIpBlock().getExcept() != null &&
                            peer.getIpBlock().getExcept().size() > 0) {
                        Map<String, List<NetworkPolicyPort>>
                                blkDirection = Maps.newConcurrentMap();

                        blkDirection.put(DIRECTION_INGRESS, i.getPorts());
                        white.compute(peer.getIpBlock().getCidr(), (k, v) -> blkDirection);

                        setBlackRules(peer.getIpBlock().getCidr(), DIRECTION_INGRESS,
                                peer.getIpBlock().getExcept(), install);
                    } else {
                        white.compute(peer.getIpBlock().getCidr(), (k, v) -> direction);
                    }
                }

                // POD selector
                Set<Pod> pods = podsFromPolicyPeer(peer, policy.getMetadata().getNamespace());

                pods.stream()
                        .filter(pod -> pod.getStatus().getPodIP() != null)
                        .forEach(pod -> {
                    white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                            SHIFTED_IP_PREFIX) + "/" + HOST_PREFIX, (m, n) -> direction);
                    white.compute(pod.getStatus().getPodIP() + "/" + HOST_PREFIX, (m, n) -> direction);
                });

                // Namespace selector
                setAllowNamespaceRules(nsHash,
                        namespacesByPolicyPeer(peer), DIRECTION_INGRESS, install);
            });
        });

        List<NetworkPolicyEgressRule> egress = policy.getSpec().getEgress();
        if (egress != null && egress.size() == 1) {
            NetworkPolicyEgressRule rule = egress.get(0);
            if (rule.getTo().size() == 0 && rule.getPorts().size() == 0) {
                setAllowAllRule(nsHash, DIRECTION_EGRESS, install);
            }
        }

        policy.getSpec().getEgress().forEach(e -> {
            Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
            direction.put(DIRECTION_EGRESS, e.getPorts());
            e.getTo().forEach(peer -> {

                // IP block
                if (peer.getIpBlock() != null) {
                    if (peer.getIpBlock().getExcept() != null &&
                            peer.getIpBlock().getExcept().size() > 0) {

                        Map<String, List<NetworkPolicyPort>>
                                blkDirection = Maps.newConcurrentMap();
                        blkDirection.put(DIRECTION_EGRESS, e.getPorts());
                        white.compute(peer.getIpBlock().getCidr(), (k, v) -> {
                            if (v != null) {
                                v.put(DIRECTION_EGRESS, e.getPorts());
                                return v;
                            } else {
                                return blkDirection;
                            }
                        });

                        setBlackRules(peer.getIpBlock().getCidr(), DIRECTION_EGRESS,
                                peer.getIpBlock().getExcept(), install);
                    } else {
                        white.compute(peer.getIpBlock().getCidr(), (k, v) -> {
                            if (v != null) {
                                v.put(DIRECTION_EGRESS, e.getPorts());
                                return v;
                            } else {
                                return direction;
                            }
                        });
                    }
                }

                // POD selector
                Set<Pod> pods = podsFromPolicyPeer(peer, policy.getMetadata().getNamespace());

                pods.stream()
                        .filter(pod -> pod.getStatus().getPodIP() != null)
                        .forEach(pod -> {
                    white.compute(shiftIpDomain(pod.getStatus().getPodIP(),
                            SHIFTED_IP_PREFIX) + "/" + HOST_PREFIX, (m, n) -> {
                        if (n != null) {
                            n.put(DIRECTION_EGRESS, e.getPorts());
                            return n;
                        } else {
                            return direction;
                        }
                    });

                    white.compute(pod.getStatus().getPodIP() + "/" + HOST_PREFIX,
                            (m, n) -> {
                        if (n != null) {
                            n.put(DIRECTION_EGRESS, e.getPorts());
                            return n;
                        } else {
                            return direction;
                        }
                    });
                });

                // Namespace selector
                setAllowNamespaceRules(nsHash,
                        namespacesByPolicyPeer(peer), DIRECTION_EGRESS, install);
            });
        });

        setAllowRules(namespaceHashByNamespace(k8sNamespaceService,
                policy.getMetadata().getNamespace()), white, install);
        setBlackToRouteRules(true);
    }

    private Set<Pod> podsFromPolicyPeer(NetworkPolicyPeer peer, String namespace) {
        Set<Pod> pods = Sets.newConcurrentHashSet();
        if (peer.getPodSelector() != null) {
            Map<String, String> podLabels = peer.getPodSelector().getMatchLabels();
            List<LabelSelectorRequirement> matchExps = peer.getPodSelector().getMatchExpressions();

            if (podLabels == null && matchExps.size() == 0) {
                k8sPodService.pods().stream()
                        .filter(pod -> pod.getMetadata().getNamespace().equals(
                                namespace))
                        .forEach(pods::add);
            } else {
                k8sPodService.pods().stream()
                        .filter(pod -> pod.getMetadata().getNamespace().equals(
                                namespace))
                        .forEach(pod -> {
                            pod.getMetadata().getLabels().forEach((k, v) -> {
                                if (podLabels != null && podLabels.get(k) != null &&
                                        podLabels.get(k).equals(v)) {
                                    pods.add(pod);
                                }
                            });
                        });
            }
        }
        return pods;
    }

    private void setAllowRulesByPod(Pod pod, boolean install) {
        Map<String, Map<String, List<NetworkPolicyPort>>>
                white = Maps.newConcurrentMap();
        AtomicReference<NetworkPolicy> selectedPolicy = new AtomicReference<>();
        k8sNetworkPolicyService.networkPolicies().stream()
                .filter(policy -> policy.getMetadata().getNamespace().equals(
                        pod.getMetadata().getNamespace()))
                .forEach(policy -> {
            String podIp = pod.getStatus().getPodIP();

            policy.getSpec().getIngress().forEach(i -> {
                Map<String, List<NetworkPolicyPort>>
                        direction = Maps.newConcurrentMap();
                direction.put(DIRECTION_INGRESS, i.getPorts());
                i.getFrom().forEach(peer -> {
                    if (peer.getPodSelector() != null) {
                        Map<String, String> podLabels = peer.getPodSelector().getMatchLabels();
                        List<LabelSelectorRequirement> matchExps = peer.getPodSelector().getMatchExpressions();

                        if (podLabels == null && matchExps.size() == 0 && podIp != null) {
                            white.compute(shiftIpDomain(podIp, SHIFTED_IP_PREFIX) +
                                    "/" + HOST_PREFIX, (m, n) -> direction);
                            white.compute(podIp + "/" +
                                    HOST_PREFIX, (m, n) -> direction);

                            selectedPolicy.set(policy);
                        } else {
                            pod.getMetadata().getLabels().forEach((k, v) -> {
                                if (podLabels != null && podLabels.get(k) != null &&
                                        podLabels.get(k).equals(v) && podIp != null) {
                                    white.compute(shiftIpDomain(podIp, SHIFTED_IP_PREFIX) +
                                            "/" + HOST_PREFIX, (m, n) -> direction);
                                    white.compute(podIp + "/" +
                                            HOST_PREFIX, (m, n) -> direction);

                                    selectedPolicy.set(policy);
                                }
                            });
                        }
                    }
                });
            });
        });

        k8sNetworkPolicyService.networkPolicies().stream()
                .filter(policy -> policy.getMetadata().getNamespace().equals(
                        pod.getMetadata().getNamespace()))
                .forEach(policy -> {
            String podIp = pod.getStatus().getPodIP();

            policy.getSpec().getEgress().forEach(e -> {
                Map<String, List<NetworkPolicyPort>> direction = Maps.newConcurrentMap();
                direction.put(DIRECTION_EGRESS, e.getPorts());
                e.getTo().forEach(peer -> {
                    if (peer.getPodSelector() != null) {
                        Map<String, String> podLabels = peer.getPodSelector().getMatchLabels();
                        List<LabelSelectorRequirement> matchExps = peer.getPodSelector().getMatchExpressions();

                        if (podLabels == null && matchExps.size() == 0 && podIp != null) {
                            white.compute(shiftIpDomain(podIp, SHIFTED_IP_PREFIX) +
                                    "/" + HOST_PREFIX, (m, n) -> {
                                if (n != null) {
                                    n.put(DIRECTION_EGRESS, e.getPorts());
                                    return n;
                                } else {
                                    return direction;
                                }
                            });

                            white.compute(podIp + "/" +
                                    HOST_PREFIX, (m, n) -> {
                                if (n != null) {
                                    n.put(DIRECTION_EGRESS, e.getPorts());
                                    return n;
                                } else {
                                    return direction;
                                }
                            });

                            selectedPolicy.set(policy);
                        } else {
                            pod.getMetadata().getLabels().forEach((k, v) -> {
                                if (podLabels != null && podLabels.get(k) != null &&
                                        podLabels.get(k).equals(v) && podIp != null) {
                                    white.compute(shiftIpDomain(podIp, SHIFTED_IP_PREFIX) +
                                            "/" + HOST_PREFIX, (m, n) -> {
                                        if (n != null) {
                                            n.put(DIRECTION_EGRESS, e.getPorts());
                                            return n;
                                        } else {
                                            return direction;
                                        }
                                    });

                                    white.compute(podIp + "/" +
                                            HOST_PREFIX, (m, n) -> {
                                        if (n != null) {
                                            n.put(DIRECTION_EGRESS, e.getPorts());
                                            return n;
                                        } else {
                                            return direction;
                                        }
                                    });

                                    selectedPolicy.set(policy);
                                }
                            });
                        }
                    }
                });
            });
        });

        int nsHash = selectedPolicy.get() != null ? namespaceHashByNamespace(k8sNamespaceService,
                selectedPolicy.get().getMetadata().getNamespace()) : DEFAULT_NAMESPACE_HASH;

        setAllowRules(nsHash, white, install);
        setBlackToRouteRules(true);
    }

    private Set<Namespace> namespacesByLabels(Map<String, String> labels) {
        Set<Namespace> nsSet = Sets.newConcurrentHashSet();
        k8sNamespaceService.namespaces().forEach(ns -> {
            if (ns != null && ns.getMetadata() != null &&
                    ns.getMetadata().getLabels() != null && labels != null) {
                ns.getMetadata().getLabels().forEach((k, v) -> {
                    if (labels.get(k) != null && labels.get(k).equals(v)) {
                        nsSet.add(ns);
                    }
                });
            }
        });

        return nsSet;
    }

    private Set<Namespace> namespacesByPolicyPeer(NetworkPolicyPeer peer) {
        if (peer.getNamespaceSelector() != null) {
            Map<String, String> labels = peer.getNamespaceSelector().getMatchLabels();
            if (labels == null || labels.size() == 0) {
                // if none of match labels are specified, it means the
                // target PODs are from any namespaces
                return k8sNamespaceService.namespaces();
            } else {
                return namespacesByLabels(labels);
            }
        }

        return Sets.newConcurrentHashSet();
    }

    private void setAllowNamespaceRules(int tunnelId, Set<Namespace> nsSet,
                                        String direction, boolean install) {

        nsSet.forEach(ns -> {
            setAllowNamespaceRulesBase(tunnelId, ns.hashCode(), direction, install);
        });
    }

    private void setAllowAllRule(int nsHash, String direction, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchTunnelId(nsHash);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(DEFAULT_SEGMENT_ID)
                .transition(ROUTING_TABLE);

        int table = 0;

        if (DIRECTION_INGRESS.equalsIgnoreCase(direction)) {
            table = ACL_INGRESS_WHITE_TABLE;
        }

        setPolicyRulesBase(sBuilder, tBuilder, table, install);
    }

    private void setAllowRules(int namespaceHash,
                               Map<String, Map<String, List<NetworkPolicyPort>>> white,
                               boolean install) {
        white.forEach((k, v) -> {
            v.forEach((pk, pv) -> {
                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                        .matchTunnelId(namespaceHash)
                        .matchEthType(TYPE_IPV4);
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
                tBuilder.setTunnelId(DEFAULT_SEGMENT_ID);

                if (pk.equalsIgnoreCase(DIRECTION_INGRESS)) {
                    sBuilder.matchIPSrc(IpPrefix.valueOf(k));
                    tBuilder.writeMetadata(k.hashCode(), DEFAULT_METADATA_MASK)
                            .transition(ACL_INGRESS_BLACK_TABLE);

                    if (pv.size() == 0) {
                        setPolicyRulesBase(sBuilder, tBuilder, ACL_INGRESS_WHITE_TABLE, install);
                    } else {
                        pv.forEach(p -> {
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_TCP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);

                                if (p.getPort() != null &&
                                        p.getPort().getIntVal() != null) {
                                    sBuilder.matchTcpDst(TpPort.tpPort(p.getPort().getIntVal()));
                                }
                            }
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_UDP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);

                                if (p.getPort() != null &&
                                        p.getPort().getIntVal() != null) {
                                    sBuilder.matchUdpDst(TpPort.tpPort(p.getPort().getIntVal()));
                                }
                            }

                            setPolicyRulesBase(sBuilder, tBuilder, ACL_INGRESS_WHITE_TABLE, install);
                        });
                    }
                } else if (pk.equalsIgnoreCase(DIRECTION_EGRESS)) {
                    sBuilder.matchIPDst(IpPrefix.valueOf(k));
                    tBuilder.writeMetadata(k.hashCode(), DEFAULT_METADATA_MASK)
                            .transition(ACL_EGRESS_BLACK_TABLE);

                    if (pv.size() == 0) {
                        setPolicyRulesBase(sBuilder, tBuilder, ACL_EGRESS_WHITE_TABLE, install);
                    } else {
                        pv.forEach(p -> {
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_TCP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);

                                if (p.getPort() != null &&
                                        p.getPort().getIntVal() != null) {
                                    sBuilder.matchTcpSrc(TpPort.tpPort(p.getPort().getIntVal()));
                                }
                            }
                            if (p.getProtocol().equalsIgnoreCase(PROTOCOL_UDP)) {
                                sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);

                                if (p.getPort() != null &&
                                        p.getPort().getIntVal() != null) {
                                    sBuilder.matchUdpSrc(TpPort.tpPort(p.getPort().getIntVal()));
                                }
                            }
                            setPolicyRulesBase(sBuilder, tBuilder, ACL_EGRESS_WHITE_TABLE, install);
                        });
                    }

                } else {
                    log.error("In correct direction has been specified at network policy.");
                }
            });
        });
    }

    private void setPolicyRulesBase(TrafficSelector.Builder sBuilder,
                                    TrafficTreatment.Builder tBuilder,
                                    int table,
                                    boolean install) {
        k8sNodeService.completeNodes().forEach(n -> {
            k8sFlowRuleService.setRule(
                    appId,
                    n.intgBridge(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_CIDR_RULE,
                    table,
                    install
            );
        });
    }

    private void setBlackRules(String whiteIpCidr, String direction,
                               List<String> except, boolean install) {
        k8sNodeService.completeNodes().forEach(n -> {
            except.forEach(blkIp -> {
                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                        .matchEthType(TYPE_IPV4)
                        .matchMetadata(whiteIpCidr.hashCode());
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                        .drop();
                int table = 0;
                if (direction.equalsIgnoreCase(DIRECTION_INGRESS)) {
                    sBuilder.matchIPSrc(IpPrefix.valueOf(blkIp));
                    table = ACL_INGRESS_BLACK_TABLE;
                }
                if (direction.equalsIgnoreCase(DIRECTION_EGRESS)) {
                    sBuilder.matchIPDst(IpPrefix.valueOf(blkIp));
                    table = ACL_EGRESS_BLACK_TABLE;
                }

                setPolicyRulesBase(sBuilder, tBuilder, table, install);
            });
        });
    }

    private void setBlackToRouteRules(boolean install) {

        k8sNodeService.completeNodes().forEach(n -> {
            ImmutableSet.of(ACL_INGRESS_BLACK_TABLE, ACL_EGRESS_BLACK_TABLE).forEach(t -> {
                k8sFlowRuleService.setRule(
                        appId,
                        n.intgBridge(),
                        DefaultTrafficSelector.builder().build(),
                        DefaultTrafficTreatment.builder()
                                .transition(ROUTING_TABLE).build(),
                        0,
                        t,
                        install
                );
            });
        });
    }

    private void setNamespaceRulesByPod(Pod pod, boolean install) {
        String podIp = pod.getStatus().getPodIP();

        if (podIp == null) {
            return;
        }

        Integer nsHash = namespaceHashByPodIp(k8sPodService, k8sNamespaceService, podIp);

        // in uninstall case, we will have null nsHash value
        if (install && nsHash == null) {
            return;
        }

        setNamespaceRulesBase(podIp, nsHash, install);
    }

    private void setNamespaceRulesByService(Service service, boolean install) {
        String clusterIp = service.getSpec().getClusterIP();

        if (clusterIp == null) {
            return;
        }

        // one the creating of new service, the cluster IP will be configured as None
        // in this case, we will NOT install any namespace related rules
        if (DEFAULT_SERVICE_IP_NONE.equalsIgnoreCase(clusterIp)) {
            return;
        }

        setNamespaceRulesBase(clusterIp, namespaceHashByServiceIp(k8sServiceService,
                k8sNamespaceService, clusterIp), install);
    }

    private void setNamespaceRulesBase(String ip, Integer nsHash, boolean install) {

        k8sNodeService.completeNodes().forEach(n -> {
            TrafficSelector.Builder origBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4)
                    .matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(ip), HOST_PREFIX));
            TrafficSelector.Builder convBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4)
                    .matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(
                            shiftIpDomain(ip, SHIFTED_IP_PREFIX)), HOST_PREFIX));
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

            if (install) {
                tBuilder.writeMetadata(nsHash, DEFAULT_METADATA_MASK)
                        .transition(GROUPING_TABLE);
            }

            k8sFlowRuleService.setRule(
                    appId,
                    n.intgBridge(),
                    origBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_NAMESPACE_RULE,
                    NAMESPACE_TABLE,
                    install
            );

            k8sFlowRuleService.setRule(
                    appId,
                    n.intgBridge(),
                    convBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_NAMESPACE_RULE,
                    NAMESPACE_TABLE,
                    install
            );
        });
    }

    private class InternalServiceListener implements K8sServiceListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sServiceEvent event) {
            Service service = event.subject();
            switch (event.type()) {
                case K8S_SERVICE_CREATED:
                case K8S_SERVICE_UPDATED:
                    eventExecutor.execute(() -> processServiceCreation(service));
                    break;
                case K8S_SERVICE_REMOVED:
                    eventExecutor.execute(() -> processServiceRemoval(service));
                    break;
                default:
                    break;
            }
        }

        private void processServiceCreation(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            setNamespaceRulesByService(service, true);
        }

        private void processServiceRemoval(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            setNamespaceRulesByService(service, false);
        }
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
            setNamespaceRulesByPod(pod, true);
        }

        private void processPodRemoval(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setBlockRulesByPod(pod, false);
            setAllowRulesByPod(pod, false);
            setNamespaceRulesByPod(pod, false);
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

    private class InternalNamespaceListener implements K8sNamespaceListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNamespaceEvent event) {
            Namespace ns = event.subject();
            switch (event.type()) {
                case K8S_NAMESPACE_CREATED:
                    eventExecutor.execute(() -> processNamespaceCreation(ns));
                    break;
                case K8S_NAMESPACE_REMOVED:
                    eventExecutor.execute(() -> processNamespaceRemoval(ns));
                    break;
                default:
                    break;
            }
        }

        private void processNamespaceCreation(Namespace namespace) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultAllowNamespaceRules(namespace, true);
            setDefaultAllowServiceRules(true);
        }

        private void processNamespaceRemoval(Namespace namespace) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultAllowNamespaceRules(namespace, false);
        }
    }
}
