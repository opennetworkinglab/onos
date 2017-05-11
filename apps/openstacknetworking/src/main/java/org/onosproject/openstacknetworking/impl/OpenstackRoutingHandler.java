/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.openstacknode.OpenstackNodeService.NetworkMode;

import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_MAC;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.*;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.COMPUTE;

/**
 * Handles OpenStack router events.
 */
@Component(immediate = true)
public class OpenstackRoutingHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_ENABLED = "Enabled ";
    private static final String MSG_DISABLED = "Disabled ";
    private static final String ERR_SET_FLOWS = "Failed to set flows for router %s:";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

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
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway == null) {
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                setSourceNat(iface, false);
            });
        } else {
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                setSourceNat(iface, exGateway.isEnableSnat());
            });
        }
    }

    private void routerIfaceAdded(Router osRouter, RouterInterface osRouterIface) {
        Subnet osSubnet = osNetworkService.subnet(osRouterIface.getSubnetId());
        if (osSubnet == null) {
            final String error = String.format(
                    ERR_SET_FLOWS + "subnet %s does not exist",
                    osRouterIface.getId(),
                    osRouterIface.getSubnetId());
            throw new IllegalStateException(error);
        }
        setInternalRoutes(osRouter, osSubnet, true);
        setGatewayIcmp(osSubnet, true);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, true);
        }

        log.info("Connected subnet({}) to {}", osSubnet.getCidr(), osRouter.getName());
    }

    private void routerIfaceRemoved(Router osRouter, RouterInterface osRouterIface) {
        Subnet osSubnet = osNetworkService.subnet(osRouterIface.getSubnetId());
        if (osSubnet == null) {
            final String error = String.format(
                    ERR_SET_FLOWS + "subnet %s does not exist",
                    osRouterIface.getId(),
                    osRouterIface.getSubnetId());
            throw new IllegalStateException(error);
        }

        setInternalRoutes(osRouter, osSubnet, false);
        setGatewayIcmp(osSubnet, false);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, false);
        }

        log.info("Disconnected subnet({}) from {}", osSubnet.getCidr(), osRouter.getName());
    }

    private void setSourceNat(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkService.network(osSubnet.getNetworkId());

        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
                .forEach(osNode -> {
                        setRulesToGateway(osNode.intBridge(), osNet.getProviderSegID(),
                                IpPrefix.valueOf(osSubnet.getCidr()), osNet.getNetworkType(),
                                install);
                });

        // take the first outgoing packet to controller for source NAT
        osNodeService.gatewayDeviceIds()
                .forEach(gwDeviceId -> setRulesToController(
                        gwDeviceId,
                        osNet.getProviderSegID(),
                        IpPrefix.valueOf(osSubnet.getCidr()),
                        osNet.getNetworkType(),
                        install));

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.info(updateStr + "external access for subnet({})", osSubnet.getCidr());
    }

    private void setGatewayIcmp(Subnet osSubnet, boolean install) {
        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            // do nothing if no gateway is set
            return;
        }

        // take ICMP request to a subnet gateway through gateway node group
        Network network = osNetworkService.network(osSubnet.getNetworkId());
        switch (network.getNetworkType()) {
            case VXLAN:
                osNodeService.completeNodes().stream()
                        .filter(osNode -> osNode.type() == COMPUTE)
                        .filter(osNode -> osNode.dataIp().isPresent())
                        .forEach(osNode -> setRulesToGatewayWithDstIp(
                                osNode.intBridge(),
                                osNodeService.gatewayGroupId(osNode.intBridge(), NetworkMode.VXLAN),
                                network.getProviderSegID(),
                                IpAddress.valueOf(osSubnet.getGateway()),
                                NetworkMode.VXLAN,
                                install));
                break;
            case VLAN:
                osNodeService.completeNodes().stream()
                        .filter(osNode -> osNode.type() == COMPUTE)
                        .filter(osNode -> osNode.vlanPort().isPresent())
                        .forEach(osNode -> setRulesToGatewayWithDstIp(
                                osNode.intBridge(),
                                osNodeService.gatewayGroupId(osNode.intBridge(), NetworkMode.VLAN),
                                network.getProviderSegID(),
                                IpAddress.valueOf(osSubnet.getGateway()),
                                NetworkMode.VLAN,
                                install));
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        network.getNetworkType().toString());
                throw new IllegalStateException(error);
        }

        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        osNodeService.gatewayDeviceIds()
                .forEach(gwDeviceId -> setGatewayIcmpRule(
                        gatewayIp,
                        gwDeviceId,
                        install
                ));

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.debug(updateStr + "ICMP to {}", osSubnet.getGateway());
    }

    private void setInternalRoutes(Router osRouter, Subnet updatedSubnet, boolean install) {
        Network updatedNetwork = osNetworkService.network(updatedSubnet.getNetworkId());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, updatedSubnet.getId());
        String updatedSegmendId = getSegmentId(updatedSubnet);

        // installs rule from/to my subnet intentionally to fix ICMP failure
        // to my subnet gateway if no external gateway added to the router
        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
                .forEach(osNode -> {
                    setInternalRouterRules(
                            osNode.intBridge(),
                            updatedSegmendId,
                            updatedSegmendId,
                            IpPrefix.valueOf(updatedSubnet.getCidr()),
                            IpPrefix.valueOf(updatedSubnet.getCidr()),
                            updatedNetwork.getNetworkType(),
                            install
                    );

                    routableSubnets.forEach(subnet -> {
                        setInternalRouterRules(
                                osNode.intBridge(),
                                updatedSegmendId,
                                getSegmentId(subnet),
                                IpPrefix.valueOf(updatedSubnet.getCidr()),
                                IpPrefix.valueOf(subnet.getCidr()),
                                updatedNetwork.getNetworkType(),
                                install
                        );
                        setInternalRouterRules(
                                osNode.intBridge(),
                                getSegmentId(subnet),
                                updatedSegmendId,
                                IpPrefix.valueOf(subnet.getCidr()),
                                IpPrefix.valueOf(updatedSubnet.getCidr()),
                                updatedNetwork.getNetworkType(),
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
                .map(iface -> osNetworkService.subnet(iface.getSubnetId()))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
    }

    private String getSegmentId(Subnet osSubnet) {
        return osNetworkService.network(osSubnet.getNetworkId()).getProviderSegID();
    }

    private void setGatewayIcmpRule(IpAddress gatewayIp, DeviceId deviceId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(gatewayIp.toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                deviceId,
                selector,
                treatment,
                ForwardingObjective.Flag.VERSATILE,
                PRIORITY_ICMP_RULE,
                install);
    }

    private void setInternalRouterRules(DeviceId deviceId, String srcSegmentId, String dstSegmentId,
                                        IpPrefix srcSubnet, IpPrefix dstSubnet,
                                        NetworkType networkType, boolean install) {
        TrafficSelector selector;
        TrafficTreatment treatment;
        switch (networkType) {
            case VXLAN:
                selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchTunnelId(Long.parseLong(srcSegmentId))
                        .matchIPSrc(srcSubnet)
                        .matchIPDst(dstSubnet)
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setTunnelId(Long.parseLong(dstSegmentId))
                        .build();

                RulePopulatorUtil.setRule(
                        flowObjectiveService,
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        ForwardingObjective.Flag.SPECIFIC,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        install);

                selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchTunnelId(Long.parseLong(dstSegmentId))
                        .matchIPSrc(srcSubnet)
                        .matchIPDst(dstSubnet)
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setTunnelId(Long.parseLong(dstSegmentId))
                        .build();

                RulePopulatorUtil.setRule(
                        flowObjectiveService,
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        ForwardingObjective.Flag.SPECIFIC,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        install);
                break;
            case VLAN:
                selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchVlanId(VlanId.vlanId(srcSegmentId))
                        .matchIPSrc(srcSubnet)
                        .matchIPDst(dstSubnet)
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setVlanId(VlanId.vlanId(dstSegmentId))
                        .build();

                RulePopulatorUtil.setRule(
                        flowObjectiveService,
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        ForwardingObjective.Flag.SPECIFIC,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        install);

                selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchVlanId(VlanId.vlanId(dstSegmentId))
                        .matchIPSrc(srcSubnet)
                        .matchIPDst(dstSubnet)
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setVlanId(VlanId.vlanId(dstSegmentId))
                        .build();

                RulePopulatorUtil.setRule(
                        flowObjectiveService,
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        ForwardingObjective.Flag.SPECIFIC,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        install);
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }

    }

    private void setRulesToGateway(DeviceId deviceId, String segmentId, IpPrefix srcSubnet,
                                   NetworkType networkType, boolean install) {
        TrafficTreatment treatment;
        GroupId groupId;

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet)
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (networkType) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));

                groupId = osNodeService.gatewayGroupId(deviceId, NetworkMode.VXLAN);
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));

                groupId = osNodeService.gatewayGroupId(deviceId, NetworkMode.VLAN);
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }

        treatment = DefaultTrafficTreatment.builder()
                .group(groupId)
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                deviceId,
                sBuilder.build(),
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_EXTERNAL_ROUTING_RULE,
                install);
    }

    private void setRulesToController(DeviceId deviceId, String segmentId, IpPrefix srcSubnet,
                                      NetworkType networkType, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet);

        switch (networkType) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(segmentId))
                        .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId))
                        .matchEthDst(MacAddress.valueOf(vlanPortMac(deviceId)));
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthDst(Constants.DEFAULT_GATEWAY_MAC);

        if (networkType.equals(NetworkType.VLAN)) {
            tBuilder.popVlan();
        }

        tBuilder.setOutput(PortNumber.CONTROLLER);

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                ForwardingObjective.Flag.VERSATILE,
                PRIORITY_EXTERNAL_ROUTING_RULE,
                install);
    }

    private String vlanPortMac(DeviceId deviceId) {
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations()
                        .value(PORT_NAME).equals(osNodeService.gatewayNode(deviceId).vlanPort().get()) && p.isEnabled())
                .findFirst().get().annotations().value(PORT_MAC);
    }

    private void setRulesToGatewayWithDstIp(DeviceId deviceId, GroupId groupId, String segmentId,
                                            IpAddress dstIp, NetworkMode networkMode, boolean install) {
        TrafficSelector selector;
        if (networkMode.equals(NetworkMode.VXLAN)) {
            selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchTunnelId(Long.valueOf(segmentId))
                    .matchIPDst(dstIp.toIpPrefix())
                    .build();
        } else {
            selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchVlanId(VlanId.vlanId(segmentId))
                    .matchIPDst(dstIp.toIpPrefix())
                    .build();
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .group(groupId)
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                deviceId,
                selector,
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_SWITCHING_RULE,
                install);
    }

    private class InternalRouterEventListener implements OpenstackRouterListener {

        @Override
        public boolean isRelevant(OpenstackRouterEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader);
        }

        // FIXME only one leader in the cluster should process
        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_ROUTER_CREATED:
                    log.debug("Router(name:{}, ID:{}) is created",
                            event.subject().getName(),
                            event.subject().getId());
                    eventExecutor.execute(() -> routerUpdated(event.subject()));
                    break;
                case OPENSTACK_ROUTER_UPDATED:
                    log.debug("Router(name:{}, ID:{}) is updated",
                            event.subject().getName(),
                            event.subject().getId());
                    eventExecutor.execute(() -> routerUpdated(event.subject()));
                    break;
                case OPENSTACK_ROUTER_REMOVED:
                    log.debug("Router(name:{}, ID:{}) is removed",
                            event.subject().getName(),
                            event.subject().getId());
                    break;
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                    log.debug("Router interface {} added to router {}",
                            event.routerIface().getPortId(),
                            event.routerIface().getId());
                    eventExecutor.execute(() -> routerIfaceAdded(
                            event.subject(),
                            event.routerIface()));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_UPDATED:
                    log.debug("Router interface {} on {} updated",
                            event.routerIface().getPortId(),
                            event.routerIface().getId());
                    break;
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                    log.debug("Router interface {} removed from router {}",
                            event.routerIface().getPortId(),
                            event.routerIface().getId());
                    eventExecutor.execute(() -> routerIfaceRemoved(
                            event.subject(),
                            event.routerIface()));
                    break;
                case OPENSTACK_ROUTER_GATEWAY_ADDED:
                case OPENSTACK_ROUTER_GATEWAY_REMOVED:
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
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader);
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();

            switch (event.type()) {
                case COMPLETE:
                case INCOMPLETE:
                    eventExecutor.execute(() -> {
                        log.info("Reconfigure routers for {}", osNode.hostname());
                        reconfigureRouters();
                    });
                    break;
                case INIT:
                case DEVICE_CREATED:
                default:
                    break;
            }
        }

        private void reconfigureRouters() {
            osRouterService.routers().forEach(osRouter -> {
                routerUpdated(osRouter);
                osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                    routerIfaceAdded(osRouter, iface);
                });
            });
        }
    }
}
