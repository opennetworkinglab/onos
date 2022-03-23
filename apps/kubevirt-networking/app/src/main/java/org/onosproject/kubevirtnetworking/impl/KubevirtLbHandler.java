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

import com.google.common.collect.Lists;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtGroupRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerListener;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnetworking.util.RulePopulatorUtil;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_DROP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_LB_GATEWAY_TUN_BRIDGE_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_LB_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.STT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.buildGarpPacket;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.externalPatchPortNum;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getLoadBalancerSetForRouter;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterForKubevirtNetwork;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles kubevirt loadbalancer.
 */
@Component(immediate = true)
public class KubevirtLbHandler {
    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterService routerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtGroupRuleService groupRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService portService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtLoadBalancerService loadBalancerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowRuleService;

    private final InternalLbEventListener lbEventListener =
            new InternalLbEventListener();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private ApplicationId appId;
    private NodeId localNodeId;

    private static final String PROTOCOL_TCP = "TCP";
    private static final String PROTOCOL_UDP = "UDP";
    private static final String PROTOCOL_ICMP = "ICMP";

    private final InternalRouterEventListener kubevirtRouterlistener = new InternalRouterEventListener();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        loadBalancerService.addListener(lbEventListener);
        routerService.addListener(kubevirtRouterlistener);


        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());

        loadBalancerService.removeListener(lbEventListener);
        routerService.removeListener(kubevirtRouterlistener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalLbEventListener implements KubevirtLoadBalancerListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtLoadBalancerEvent event) {
            switch (event.type()) {
                case KUBEVIRT_LOAD_BALANCER_CREATED:
                    eventExecutor.execute(() -> processLbCreated(event.subject()));
                    break;
                case KUBEVIRT_LOAD_BALANCER_UPDATED:
                    eventExecutor.execute(() -> processLbUpdated(event.subject(), event.oldLb()));
                    break;
                case KUBEVIRT_LOAD_BALANCER_REMOVED:
                    eventExecutor.execute(() -> processLbRemoved(event.subject()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processLbCreated(KubevirtLoadBalancer loadBalancer) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNetwork network = networkService.network(loadBalancer.networkId());

            if (network == null) {
                log.warn("Failed to process processLbCreated because there's no network for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtRouter router =
                    getRouterForKubevirtNetwork(routerService, network);

            if (router == null) {
                log.warn("Failed to process processLbCreated because there's no router for lb {}",
                        loadBalancer.name());
                return;
            }

            if (router.electedGateway() == null) {
                log.warn("Failed to process processLbCreated because there's elected gateway for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtNode gateway = nodeService.node(router.electedGateway());

            setLbGroup(loadBalancer, gateway, true);
            setBucketsToGroup(loadBalancer, gateway, true);
            setLbDownstreamRules(loadBalancer, router, gateway, true);
            setLbUpstreamRules(loadBalancer, router, gateway, true);

            if (network.type() == VXLAN || network.type() == GENEVE || network.type() == GRE || network.type() == STT) {
                setLbDownStreamRulesForTunBridge(loadBalancer, gateway, true);
            }
        }

        private void processLbUpdated(KubevirtLoadBalancer loadBalancer, KubevirtLoadBalancer old) {
            if (!isRelevantHelper()) {
                return;
            }
            // clean up buckets and flow rules related to the old loadbalancer

            KubevirtNetwork oldNetwork = networkService.network(loadBalancer.networkId());

            if (oldNetwork == null) {
                log.warn("Failed to process processLbUpdated because there's no network for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtRouter oldRouter =
                    getRouterForKubevirtNetwork(routerService, oldNetwork);

            if (oldRouter == null) {
                log.warn("Failed to process processLbUpdated because there's no router for lb {}",
                        loadBalancer.name());
                return;
            }

            if (oldRouter.electedGateway() == null) {
                log.warn("Failed to process processLbUpdated because there's elected gateway for lb {}",
                        loadBalancer.name());
                return;
            }
            KubevirtNode oldGateway = nodeService.node(oldRouter.electedGateway());

            setLbDownstreamRules(old, oldRouter, oldGateway, false);
            setLbUpstreamRules(old, oldRouter, oldGateway, false);
            if (oldNetwork.type() == VXLAN || oldNetwork.type() == GENEVE ||
                    oldNetwork.type() == GRE || oldNetwork.type() == STT) {
                setLbDownStreamRulesForTunBridge(loadBalancer, oldGateway, false);
            }
            setBucketsToGroup(old, oldGateway, false);
            setLbGroup(old, oldGateway, false);


            KubevirtNetwork network = networkService.network(loadBalancer.networkId());

            if (network == null) {
                log.warn("Failed to process processLbUpdated because there's no network for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtRouter router =
                    getRouterForKubevirtNetwork(routerService, network);

            if (router == null) {
                log.warn("Failed to process processLbUpdated because there's no router for lb {}",
                        loadBalancer.name());
                return;
            }

            if (router.electedGateway() == null) {
                log.warn("Failed to process processLbUpdated because there's elected gateway for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtNode gateway = nodeService.node(router.electedGateway());

            setLbGroup(loadBalancer, gateway, true);
            setBucketsToGroup(loadBalancer, gateway, true);
            setLbDownstreamRules(loadBalancer, router, gateway, true);
            setLbUpstreamRules(loadBalancer, router, gateway, true);
            if (network.type() == VXLAN || network.type() == GENEVE ||
                    network.type() == GRE || network.type() == STT) {
                setLbDownStreamRulesForTunBridge(loadBalancer, gateway, true);
            }
        }

        private void processLbRemoved(KubevirtLoadBalancer loadBalancer) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNetwork network = networkService.network(loadBalancer.networkId());

            if (network == null) {
                log.warn("Failed to process processLbRemoved because there's no network for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtRouter router =
                    getRouterForKubevirtNetwork(routerService, network);

            if (router == null) {
                log.warn("Failed to process processLbRemoved because there's no router for lb {}",
                        loadBalancer.name());
                return;
            }

            if (router.electedGateway() == null) {
                log.warn("Failed to process processLbRemoved because there's elected gateway for lb {}",
                        loadBalancer.name());
                return;
            }

            KubevirtNode gateway = nodeService.node(router.electedGateway());

            setLbDownstreamRules(loadBalancer, router, gateway, false);
            setLbUpstreamRules(loadBalancer, router, gateway, false);
            setBucketsToGroup(loadBalancer, gateway, false);
            setLbGroup(loadBalancer, gateway, false);

            if (network.type() == VXLAN || network.type() == GENEVE || network.type() == GRE || network.type() == STT) {
                setLbDownStreamRulesForTunBridge(loadBalancer, gateway, false);
            }
        }
    }

    private void setLbGroup(KubevirtLoadBalancer loadBalancer, KubevirtNode gateway, boolean install) {

        int groupId = loadBalancer.hashCode();

        groupRuleService.setRule(appId, gateway.intgBridge(), groupId,
                SELECT, Lists.newArrayList(), install);
    }

    private void setBucketsToGroup(KubevirtLoadBalancer loadBalancer, KubevirtNode gateway, boolean install) {
        int groupId = loadBalancer.hashCode();

        KubevirtNetwork network = networkService.network(loadBalancer.networkId());

        Set<KubevirtPort> ports = portService.ports(loadBalancer.networkId());

        List<GroupBucket> bkts = Lists.newArrayList();
        loadBalancer.members().forEach(ip -> {
            ports.stream().filter(port -> port.ipAddress().equals(ip) && port.macAddress() != null)
                    .findAny().ifPresent(port -> bkts.add(buildGroupBucket(port)));
        });

        groupRuleService.setBuckets(appId, gateway.intgBridge(),
                groupId, bkts, install);
    }

    private void setLbDownstreamRules(KubevirtLoadBalancer loadBalancer,
                                      KubevirtRouter router,
                                      KubevirtNode gateway, boolean install) {

        int groupId = loadBalancer.hashCode();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(router.mac())
                .group(GroupId.valueOf(groupId))
                .build();

        loadBalancer.rules().forEach(rule -> {
            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            switch (rule.protocol().toUpperCase()) {
                case PROTOCOL_TCP:
                    sBuilder.matchIPDst(loadBalancer.vip().toIpPrefix())
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPProtocol(IPv4.PROTOCOL_TCP)
                            .matchTcpDst(TpPort.tpPort(rule.portRangeMin().intValue()));
                    break;
                case PROTOCOL_UDP:
                    sBuilder.matchIPDst(loadBalancer.vip().toIpPrefix())
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPProtocol(IPv4.PROTOCOL_UDP)
                            .matchUdpDst(TpPort.tpPort(rule.portRangeMin().intValue()));
                    break;
                case PROTOCOL_ICMP:
                    sBuilder.matchIPDst(loadBalancer.vip().toIpPrefix())
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPProtocol(IPv4.PROTOCOL_ICMP);
                    break;
                default:
                    break;
            }

            flowRuleService.setRule(
                    appId,
                    gateway.intgBridge(),
                    sBuilder.build(),
                    treatment,
                    PRIORITY_LB_RULE,
                    GW_DROP_TABLE,
                    install
            );
        });
    }

    private void setLbDownstreamRulesForFloatingIp(KubevirtNode gateway,
                                                   KubevirtFloatingIp floatingIp,
                                                   boolean install) {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(floatingIp.floatingIp().toIpPrefix());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setIpDst(floatingIp.fixedIp())
                .transition(GW_DROP_TABLE);

        flowRuleService.setRule(
                appId,
                gateway.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_LB_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private void setLbDownStreamRulesForTunBridge(KubevirtLoadBalancer loadBalancer,
                                                  KubevirtNode gateway, boolean install) {
        Set<KubevirtPort> ports = portService.ports(loadBalancer.networkId());
        KubevirtNetwork network = networkService.network(loadBalancer.networkId());

        PortNumber tunnelPortNumber = tunnelPort(gateway, network);
        if (tunnelPortNumber == null) {
            return;
        }

        loadBalancer.members().forEach(ip -> {
            ports.stream().filter(port -> port.ipAddress().equals(ip) && port.macAddress() != null)
                    .findAny().ifPresent(port -> {

                        KubevirtNode workerNode = nodeService.node(port.deviceId());

                        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchIPDst(IpPrefix.valueOf(port.ipAddress(), 32))
                                .matchEthDst(port.macAddress());

                        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                        .setTunnelId(Long.parseLong(network.segmentId()))
                        .extension(buildExtension(
                                deviceService,
                                gateway.tunBridge(),
                                workerNode.dataIp().getIp4Address()),
                                gateway.tunBridge())
                        .setOutput(tunnelPortNumber);

                        flowRuleService.setRule(
                                appId,
                                gateway.tunBridge(),
                                sBuilder.build(),
                                tBuilder.build(),
                                PRIORITY_LB_GATEWAY_TUN_BRIDGE_RULE,
                                TUNNEL_DEFAULT_TABLE,
                                install);
            });
        });
    }
    private void setLbUpstreamRules(KubevirtLoadBalancer loadBalancer,
                                    KubevirtRouter router,
                                    KubevirtNode gateway, boolean install) {

        Set<KubevirtPort> ports = portService.ports(loadBalancer.networkId());

        loadBalancer.members().forEach(ip -> {
            ports.stream().filter(port -> port.ipAddress().equals(ip) && port.macAddress() != null)
                    .findAny().ifPresent(port -> {
                loadBalancer.rules().forEach(rule -> {
                    TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchEthSrc(port.macAddress())
                            .matchIPSrc(port.ipAddress().toIpPrefix());

                    switch (rule.protocol().toUpperCase()) {
                        case PROTOCOL_TCP:
                            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP)
                                    .matchTcpSrc(TpPort.tpPort(rule.portRangeMin()));
                            break;
                        case PROTOCOL_UDP:
                            sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP)
                                    .matchUdpSrc(TpPort.tpPort(rule.portRangeMin()));
                            break;
                        case PROTOCOL_ICMP:
                            sBuilder.matchIPProtocol(IPv4.PROTOCOL_ICMP);
                            break;
                        default:
                            break;
                    }

                    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                            .setEthSrc(router.mac())
                            .setIpSrc(loadBalancer.vip())
                            .transition(GW_DROP_TABLE);

                    flowRuleService.setRule(
                            appId,
                            gateway.intgBridge(),
                            sBuilder.build(),
                            tBuilder.build(),
                            PRIORITY_LB_RULE,
                            GW_ENTRY_TABLE,
                            install);
                });
            });
        });
    }

    private void setArpResponseRuleForFloatingIp(KubevirtNode gateway,
                                                 KubevirtFloatingIp floatingIp,
                                                 boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(externalPatchPortNum(deviceService, gateway))
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(floatingIp.floatingIp().getIp4Address())
                .build();

        Device device = deviceService.getDevice(gateway.intgBridge());

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(RulePopulatorUtil.buildMoveEthSrcToDstExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpShaToThaExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpOp(ARP.OP_REPLY)
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setArpSha(DEFAULT_GATEWAY_MAC)
                .setArpSpa(floatingIp.floatingIp().getIp4Address())
                .setOutput(PortNumber.IN_PORT)
                .build();

        flowRuleService.setRule(
                appId,
                gateway.intgBridge(),
                selector,
                treatment,
                PRIORITY_ARP_GATEWAY_RULE,
                GW_ENTRY_TABLE,
                install);
    }
    private void setLbUpstreamRulesForFloatingIp(KubevirtRouter router,
                                                 KubevirtNode gateway,
                                                 KubevirtFloatingIp floatingIp,
                                                 boolean install) {
        if (router.peerRouter().macAddress() == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthSrc(router.mac())
                .matchIPSrc(floatingIp.fixedIp().toIpPrefix());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setIpSrc(floatingIp.floatingIp())
                .setEthDst(router.peerRouter().macAddress())
                .setOutput(externalPatchPortNum(deviceService, gateway));

        flowRuleService.setRule(
                appId,
                gateway.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_LB_RULE,
                GW_DROP_TABLE,
                install);
    }

    private GroupBucket buildGroupBucket(KubevirtPort port) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setEthDst(port.macAddress())
                .setIpDst(port.ipAddress())
                .setOutput(PortNumber.NORMAL);

        return DefaultGroupBucket.createSelectGroupBucket(tBuilder.build());
    }

    private class InternalRouterEventListener implements KubevirtRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtRouterEvent event) {
            switch (event.type()) {
                case KUBEVIRT_GATEWAY_NODE_ATTACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeAttached(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_CHANGED:
                    eventExecutor.execute(() -> processRouterGatewayNodeChanged(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_DETACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeDetached(event.subject(),
                            event.gateway()));
                    break;

                case KUBEVIRT_FLOATING_IP_LB_ASSOCIATED:
                    eventExecutor.execute(() -> processFloatingIpAssociated(event.subject(),
                            event.floatingIp()));
                    break;
                case KUBEVIRT_FLOATING_IP_LB_DISASSOCIATED:
                    eventExecutor.execute(() -> processFloatingIpDisAssociated(event.subject(),
                            event.floatingIp()));
                    break;
                case KUBEVIRT_PEER_ROUTER_MAC_RETRIEVED:
                    eventExecutor.execute(() -> processPeerRouterRetrieved(event.subject()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processPeerRouterRetrieved(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }

            if (router.peerRouter().macAddress() == null) {
                return;
            }

            if (router.electedGateway() == null) {
                return;
            }

            processRouterGatewayNodeAttached(router, router.electedGateway());
        }

        private void processFloatingIpAssociated(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper()) {
                return;
            }

            if (router.electedGateway() == null) {
                log.warn("Failed to process processFloatingIpAssociated because there's elected gateway for fip {}",
                        floatingIp.floatingIp());
                return;
            }

            KubevirtNode gateway = nodeService.node(router.electedGateway());

            loadBalancerService.loadBalancers().stream()
                    .filter(lb -> lb.vip().equals(floatingIp.fixedIp()))
                    .findAny()
                    .ifPresent(lb -> {
                        setLbUpstreamRulesForFloatingIp(router, gateway, floatingIp, true);
                        setLbDownstreamRulesForFloatingIp(gateway, floatingIp, true);
                        setArpResponseRuleForFloatingIp(gateway, floatingIp, true);
                        processGarpPacketForFloatingIp(floatingIp, gateway);
                    });
        }

        private void processGarpPacketForFloatingIp(KubevirtFloatingIp floatingIp, KubevirtNode electedGw) {
            if (floatingIp == null) {
                return;
            }

            Ethernet ethernet = buildGarpPacket(DEFAULT_GATEWAY_MAC, floatingIp.floatingIp());
            if (ethernet == null) {
                return;
            }

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(externalPatchPortNum(deviceService, electedGw)).build();

            packetService.emit(new DefaultOutboundPacket(electedGw.intgBridge(), treatment,
                    ByteBuffer.wrap(ethernet.serialize())));
        }

        private void processFloatingIpDisAssociated(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper()) {
                return;
            }

            if (router.electedGateway() == null) {
                log.warn("Failed to process processFloatingIpDisAssociated because there's elected gateway for fip {}",
                        floatingIp.floatingIp());
                return;
            }

            KubevirtNode gateway = nodeService.node(router.electedGateway());


            loadBalancerService.loadBalancers().stream()
                    .filter(lb -> lb.vip().equals(floatingIp.fixedIp()))
                    .findAny()
                    .ifPresent(lb -> {
                        setLbUpstreamRulesForFloatingIp(router, gateway, floatingIp, false);
                        setLbDownstreamRulesForFloatingIp(gateway, floatingIp, false);
                        setArpResponseRuleForFloatingIp(gateway, floatingIp, false);
                    });
        }

        private void processRouterGatewayNodeAttached(KubevirtRouter router,
                                                      String associatedGateway) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode gatewayNode = nodeService.node(associatedGateway);
            if (gatewayNode == null) {
                return;
            }

            getLoadBalancerSetForRouter(router, loadBalancerService).forEach(loadBalancer -> {
                setLbGroup(loadBalancer, gatewayNode, true);
                setBucketsToGroup(loadBalancer, gatewayNode, true);
                setLbDownstreamRules(loadBalancer, router, gatewayNode, true);
                setLbUpstreamRules(loadBalancer, router, gatewayNode, true);

                KubevirtNetwork network = networkService.network(loadBalancer.networkId());
                if (network.type() == VXLAN || network.type() == GENEVE ||
                        network.type() == GRE || network.type() == STT) {
                    setLbDownStreamRulesForTunBridge(loadBalancer, gatewayNode, true);
                }

                routerService.floatingIpsByRouter(router.name())
                        .stream()
                        .filter(fip -> fip.fixedIp() != null && fip.fixedIp().equals(loadBalancer.vip()))
                        .findAny()
                        .ifPresent(fip -> {
                            setLbDownstreamRulesForFloatingIp(gatewayNode, fip, true);
                            setLbUpstreamRulesForFloatingIp(router, gatewayNode, fip, true);
                            setArpResponseRuleForFloatingIp(gatewayNode, fip, true);
                        });
            });
        }

        private void processRouterGatewayNodeDetached(KubevirtRouter router,
                                                      String disAssociatedGateway) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode gatewayNode = nodeService.node(disAssociatedGateway);
            if (gatewayNode == null) {
                return;
            }

            getLoadBalancerSetForRouter(router, loadBalancerService).forEach(loadBalancer -> {
                setLbDownstreamRules(loadBalancer, router, gatewayNode, false);
                setLbUpstreamRules(loadBalancer, router, gatewayNode, false);
                setBucketsToGroup(loadBalancer, gatewayNode, false);
                setLbGroup(loadBalancer, gatewayNode, false);

                KubevirtNetwork network = networkService.network(loadBalancer.networkId());
                if (network.type() == VXLAN || network.type() == GENEVE ||
                        network.type() == GRE || network.type() == STT) {
                    setLbDownStreamRulesForTunBridge(loadBalancer, gatewayNode, false);
                }

                routerService.floatingIpsByRouter(router.name())
                        .stream()
                        .filter(fip -> fip.fixedIp() != null && fip.fixedIp().equals(loadBalancer.vip()))
                        .findAny()
                        .ifPresent(fip -> {
                            setLbDownstreamRulesForFloatingIp(gatewayNode, fip, false);
                            setLbUpstreamRulesForFloatingIp(router, gatewayNode, fip, false);
                            setArpResponseRuleForFloatingIp(gatewayNode, fip, false);
                        });
            });
        }

        private void processRouterGatewayNodeChanged(KubevirtRouter router,
                                                     String disAssociatedGateway) {
            if (!isRelevantHelper()) {
                return;
            }

            processRouterGatewayNodeDetached(router, disAssociatedGateway);

            KubevirtNode newGatewayNode = nodeService.node(router.electedGateway());
            if (newGatewayNode == null) {
                return;
            }
            processRouterGatewayNodeAttached(router, newGatewayNode.hostname());
        }
    }
}
