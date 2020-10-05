/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ADMIN_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_REQUEST_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_INTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.ARP_MODE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.tunnelPortNumByNetType;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Handles OpenStack router events.
 */
@Component(immediate = true)
public class OpenstackRoutingHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_ENABLED = "Enabled ";
    private static final String MSG_DISABLED = "Disabled ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkAdminService osNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();
    private final OpenstackRouterListener osRouterListener = new InternalRouterEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osNodeService.addListener(osNodeListener);
        osRouterService.addListener(osRouterListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osRouterService.removeListener(osRouterListener);
        osNodeService.removeListener(osNodeListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void routerUpdated(Router osRouter) {
        osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
            Network network = osNetworkAdminService.network(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                    .getNetworkId());
            Type netType = osNetworkAdminService.networkType(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                    .getNetworkId());
            setRouterAdminRules(network.getProviderSegID(),
                                netType, !osRouter.isAdminStateUp());
        });
    }

    private void routerRemove(Router osRouter) {
        osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
            Network network = osNetworkAdminService.network(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                    .getNetworkId());
            Type netType = osNetworkAdminService.networkType(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                            .getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), netType, false);
        });
    }

    private void routerIfaceAdded(Router osRouter, RouterInterface osRouterIface) {
        Subnet osSubnet = osNetworkAdminService.subnet(osRouterIface.getSubnetId());
        if (osSubnet == null) {
            final String error = String.format(
                    "Failed to set flows for router %s: subnet %s does not exist",
                    osRouterIface.getId(),
                    osRouterIface.getSubnetId());
            throw new IllegalStateException(error);
        }

        if (!osRouter.isAdminStateUp()) {
            Network network = osNetworkAdminService.network(osSubnet.getNetworkId());
            Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), netType, true);
        }

        setInternalRoutes(osRouter, osSubnet, true);
        setGatewayRules(osSubnet, osRouter, true);
        log.info("Connected subnet({}) to {}", osSubnet.getCidr(), deriveResourceName(osRouter));
    }

    private void routerIfaceRemoved(Router osRouter, RouterInterface osRouterIface) {
        Subnet osSubnet = osNetworkAdminService.subnet(osRouterIface.getSubnetId());
        if (osSubnet == null) {
            final String error = String.format(
                    "Failed to set flows for router %s: subnet %s does not exist",
                    osRouterIface.getId(),
                    osRouterIface.getSubnetId());
            throw new IllegalStateException(error);
        }

        if (!osRouter.isAdminStateUp()) {
            Network network = osNetworkAdminService.network(osSubnet.getNetworkId());
            Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), netType, false);
        }

        setInternalRoutes(osRouter, osSubnet, false);
        setGatewayRules(osSubnet, osRouter, false);
        log.info("Disconnected subnet({}) from {}", osSubnet.getCidr(), deriveResourceName(osRouter));
    }

    private void setGatewayRules(Subnet osSubnet, Router osRouter, boolean install) {
        OpenstackNode srcNatGw = osNodeService.completeNodes(GATEWAY)
                                         .stream().findFirst().orElse(null);

        if (srcNatGw == null) {
            return;
        }

        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            // do nothing if no gateway is set
            return;
        }

        Network net = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, osSubnet.getId());

        // install rules to each compute node for routing IP packets to gateways
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> setRulesToGatewayWithRoutableSubnets(
                        cNode,
                        srcNatGw,
                        net.getProviderSegID(),
                        osSubnet,
                        routableSubnets,
                        netType,
                        install));

        // install rules to punt ICMP packets to controller at gateway node
        // this rule is only valid for stateless ICMP SNAT case
        osNodeService.completeNodes(GATEWAY).forEach(gNode ->
                setReactiveGatewayIcmpRule(
                        IpAddress.valueOf(osSubnet.getGateway()),
                        gNode.intgBridge(), install));

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.debug(updateStr + "IP to {}", osSubnet.getGateway());
    }

    private void setInternalRoutes(Router osRouter, Subnet updatedSubnet, boolean install) {
        Type netType = osNetworkAdminService.networkType(updatedSubnet.getNetworkId());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, updatedSubnet.getId());
        String updatedSegmentId = getSegmentId(updatedSubnet);

        // installs rule from/to my subnet intentionally to fix ICMP failure
        // to my subnet gateway if no external gateway added to the router
        osNodeService.completeNodes(COMPUTE).forEach(cNode -> {
            setInternalRouterRules(
                    cNode.intgBridge(),
                    updatedSegmentId,
                    updatedSegmentId,
                    IpPrefix.valueOf(updatedSubnet.getCidr()),
                    IpPrefix.valueOf(updatedSubnet.getCidr()),
                    netType,
                    install
            );

            routableSubnets.forEach(subnet -> {
                setInternalRouterRules(
                        cNode.intgBridge(),
                        updatedSegmentId,
                        getSegmentId(subnet),
                        IpPrefix.valueOf(updatedSubnet.getCidr()),
                        IpPrefix.valueOf(subnet.getCidr()),
                        netType,
                        install
                );
                setInternalRouterRules(
                        cNode.intgBridge(),
                        getSegmentId(subnet),
                        updatedSegmentId,
                        IpPrefix.valueOf(subnet.getCidr()),
                        IpPrefix.valueOf(updatedSubnet.getCidr()),
                        netType,
                        install
                );
            });
        });


        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        routableSubnets.forEach(subnet -> log.debug(
                updateStr + "route between subnet:{} and subnet:{}",
                subnet.getCidr(),
                updatedSubnet.getCidr()));
    }

    private Set<Subnet> routableSubnets(Router osRouter, String osSubnetId) {
        Set<Subnet> osSubnets = osRouterService.routerInterfaces(osRouter.getId())
                .stream()
                .filter(iface -> !Objects.equals(iface.getSubnetId(), osSubnetId))
                .map(iface -> osNetworkAdminService.subnet(iface.getSubnetId()))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
    }

    private String getSegmentId(Subnet osSubnet) {
        return osNetworkAdminService.network(osSubnet.getNetworkId()).getProviderSegID();
    }

    private boolean getStatefulSnatFlag() {
        Set<ConfigProperty> properties = configService.getProperties(OpenstackRoutingSnatHandler.class.getName());
        return getPropertyValueAsBoolean(properties, USE_STATEFUL_SNAT);
    }

    private String getArpMode() {
        Set<ConfigProperty> properties = configService.getProperties(OpenstackRoutingArpHandler.class.getName());
        return getPropertyValue(properties, ARP_MODE);
    }

    private void setReactiveGatewayIcmpRule(IpAddress gatewayIp, DeviceId deviceId, boolean install) {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        int icmpRulePriority;

        if (getStatefulSnatFlag()) {
            sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                    .matchIcmpType(ICMP.TYPE_ECHO_REQUEST);
            icmpRulePriority = PRIORITY_ICMP_REQUEST_RULE;
        } else {
            sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                    .matchIPDst(gatewayIp.getIp4Address().toIpPrefix());
            icmpRulePriority = PRIORITY_ICMP_RULE;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                treatment,
                icmpRulePriority,
                Constants.GW_COMMON_TABLE,
                install);
    }

    private void setInternalRouterRules(DeviceId deviceId, String srcSegId, String dstSegId,
                                        IpPrefix srcSubnet, IpPrefix dstSubnet,
                                        Type networkType, boolean install) {

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                setInternalRouterRulesForTunnel(deviceId, srcSegId, dstSegId,
                                                srcSubnet, dstSubnet, install);
                break;
            case VLAN:
                setInternalRouterRulesForVlan(deviceId, srcSegId, dstSegId,
                                                srcSubnet, dstSubnet, install);
                break;
            default:
                final String error = String.format("%s %s", ERR_UNSUPPORTED_NET_TYPE,
                                                            networkType.toString());
                throw new IllegalStateException(error);
        }

    }

    private void setInternalRouterRulesForTunnel(DeviceId deviceId,
                                                 String srcSegmentId,
                                                 String dstSegmentId,
                                                 IpPrefix srcSubnet,
                                                 IpPrefix dstSubnet,
                                                 boolean install) {
        TrafficSelector selector;
        TrafficTreatment treatment;
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(srcSegmentId))
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchIPDst(dstSubnet.getIp4Prefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(dstSegmentId))
                .transition(STAT_OUTBOUND_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_INTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);

        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(dstSegmentId))
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchIPDst(dstSubnet.getIp4Prefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(dstSegmentId))
                .transition(STAT_OUTBOUND_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_INTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setInternalRouterRulesForVlan(DeviceId deviceId,
                                               String srcSegmentId,
                                               String dstSegmentId,
                                               IpPrefix srcSubnet,
                                               IpPrefix dstSubnet,
                                               boolean install) {
        TrafficSelector selector;
        TrafficTreatment treatment;
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId(srcSegmentId))
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchIPDst(dstSubnet.getIp4Prefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId(dstSegmentId))
                .transition(STAT_OUTBOUND_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_INTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);

        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId(dstSegmentId))
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchIPDst(dstSubnet.getIp4Prefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId(dstSegmentId))
                .transition(STAT_OUTBOUND_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_INTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setRulesToGatewayWithRoutableSubnets(OpenstackNode osNode,
                                                      OpenstackNode sourceNatGateway,
                                                      String segmentId,
                                                      Subnet updatedSubnet,
                                                      Set<Subnet> routableSubnets,
                                                      Type networkType,
                                                      boolean install) {

        if (getStatefulSnatFlag() && ARP_BROADCAST_MODE.equals(getArpMode())) {
            return;
        }

        // at first we install flow rules to gateway with segId and gatewayIp of updated subnet
        setRulesToGatewayWithDstIp(osNode, sourceNatGateway, segmentId,
                IpAddress.valueOf(updatedSubnet.getGateway()), networkType, install);

        routableSubnets.forEach(subnet -> {
            setRulesToGatewayWithDstIp(osNode, sourceNatGateway,
                    segmentId, IpAddress.valueOf(subnet.getGateway()),
                    networkType, install);

            Network network = osNetworkAdminService.network(subnet.getNetworkId());
            setRulesToGatewayWithDstIp(osNode, sourceNatGateway,
                    network.getProviderSegID(), IpAddress.valueOf(updatedSubnet.getGateway()),
                    networkType, install);
        });
    }

    private void setRulesToGatewayWithDstIp(OpenstackNode osNode,
                                            OpenstackNode sourceNatGateway,
                                            String segmentId,
                                            IpAddress dstIp,
                                            Type networkType,
                                            boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(dstIp.getIp4Address().toIpPrefix());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));

                PortNumber portNum = tunnelPortNumByNetType(networkType, osNode);

                tBuilder.extension(buildExtension(
                        deviceService,
                        osNode.intgBridge(),
                        sourceNatGateway.dataIp().getIp4Address()),
                        osNode.intgBridge())
                        .setOutput(portNum);
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                tBuilder.setOutput(osNode.vlanPortNum());
                break;

            default:
                break;
        }

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_SWITCHING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setRouterAdminRules(String segmentId,
                                     Type networkType,
                                     boolean install) {
        TrafficTreatment treatment;
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE,
                        networkType.toString());
                throw new IllegalStateException(error);
        }

        treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
                .forEach(osNode -> {
                    osFlowRuleService.setRule(
                            appId,
                            osNode.intgBridge(),
                            sBuilder.build(),
                            treatment,
                            PRIORITY_ADMIN_RULE,
                            ROUTING_TABLE,
                            install);
                });
    }

    private class InternalRouterEventListener implements OpenstackRouterListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_ROUTER_CREATED:
                    eventExecutor.execute(() -> processRouterCreation(event));
                    break;
                case OPENSTACK_ROUTER_UPDATED:
                    eventExecutor.execute(() -> processRouterUpdate(event));
                    break;
                case OPENSTACK_ROUTER_REMOVED:
                    eventExecutor.execute(() -> processRouterRemoval(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                    eventExecutor.execute(() -> processRouterIntfCreation(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_UPDATED:
                    eventExecutor.execute(() -> processRouterIntfUpdate(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                    eventExecutor.execute(() -> processRouterIntfRemoval(event));
                    break;
                case OPENSTACK_ROUTER_GATEWAY_ADDED:
                    log.debug("Router external gateway {} added",
                                        event.externalGateway().getNetworkId());
                    break;
                case OPENSTACK_ROUTER_GATEWAY_REMOVED:
                    log.debug("Router external gateway {} removed",
                                        event.externalGateway().getNetworkId());
                    break;
                case OPENSTACK_FLOATING_IP_CREATED:
                case OPENSTACK_FLOATING_IP_UPDATED:
                case OPENSTACK_FLOATING_IP_REMOVED:
                case OPENSTACK_FLOATING_IP_ASSOCIATED:
                case OPENSTACK_FLOATING_IP_DISASSOCIATED:
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void processRouterCreation(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is created",
                    deriveResourceName(event.subject()), event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterUpdate(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is updated",
                    deriveResourceName(event.subject()), event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is removed",
                    deriveResourceName(event.subject()), event.subject().getId());

            routerRemove(event.subject());
        }

        private void processRouterIntfCreation(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} added to router {}",
                                                    event.routerIface().getPortId(),
                                                    event.routerIface().getId());

            routerIfaceAdded(event.subject(), event.routerIface());
        }

        private void processRouterIntfUpdate(OpenstackRouterEvent event) {
            log.debug("Router interface {} on {} updated",
                                                    event.routerIface().getPortId(),
                                                    event.routerIface().getId());
        }

        private void processRouterIntfRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} removed from router {}",
                                                    event.routerIface().getPortId(),
                                                    event.routerIface().getId());

            routerIfaceRemoved(event.subject(), event.routerIface());
        }
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        reconfigureRouters(osNode);
                    });
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                case OPENSTACK_NODE_CREATED:
                default:
                    break;
            }
        }

        private void reconfigureRouters(OpenstackNode osNode) {
            osRouterService.routers().forEach(osRouter -> {
                routerUpdated(osRouter);
                osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                    routerIfaceAdded(osRouter, iface);
                });
            });
            log.debug("Reconfigure routers for {}", osNode.hostname());
        }
    }
}
