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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNode.NetworkMode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_EXTERNAL_ROUTER_MAC;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_EXTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_INTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ADMIN_RULE;
import static org.onosproject.openstacknetworking.impl.RulePopulatorUtil.buildExtension;
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
    private static final String ERR_SET_FLOWS = "Failed to set flows for router %s:";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";
    private static final boolean USE_STATEFUL_SNAT = false;

    @Property(name = "useStatefulSnat", boolValue = USE_STATEFUL_SNAT,
            label = "Use Stateful SNAT for source NATing")
    private boolean useStatefulSnat = USE_STATEFUL_SNAT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();
    private final OpenstackRouterListener osRouterListener = new InternalRouterEventListener();
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osNodeService.addListener(osNodeListener);
        osRouterService.addListener(osRouterListener);
        instancePortService.addListener(instancePortListener);
        configService.registerProperties(getClass());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osRouterService.removeListener(osRouterListener);
        osNodeService.removeListener(osNodeListener);
        instancePortService.removeListener(instancePortListener);
        leadershipService.withdraw(appId.name());
        configService.unregisterProperties(getClass(), false);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, "useStatefulSnat");
        if (flag == null) {
            log.info("useStatefulSnat is not configured, " +
                    "using current value of {}", useStatefulSnat);
        } else {
            useStatefulSnat = flag;
            log.info("Configured. useStatefulSnat is {}",
                    useStatefulSnat ? "enabled" : "disabled");
        }

        resetSnatRules();
    }

    private void routerUpdated(Router osRouter) {
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();

        osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
            Network network = osNetworkService.network(osNetworkService.subnet(iface.getSubnetId())
                    .getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), network.getNetworkType(), !osRouter.isAdminStateUp());
        });

        if (exGateway == null) {
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                setSourceNat(osRouter, iface, false);
            });
        } else {
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                setSourceNat(osRouter, iface, exGateway.isEnableSnat());
            });
        }
    }

    private void routerRemove(Router osRouter) {
        osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
            Network network = osNetworkService.network(osNetworkService.subnet(iface.getSubnetId())
                    .getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), network.getNetworkType(), false);
        });
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

        if (!osRouter.isAdminStateUp()) {
            Network network = osNetworkService.network(osSubnet.getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), network.getNetworkType(), true);
        }

        setInternalRoutes(osRouter, osSubnet, true);
        setGatewayIcmp(osSubnet, true);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouter, osRouterIface, true);
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

        if (!osRouter.isAdminStateUp()) {
            Network network = osNetworkService.network(osSubnet.getNetworkId());
            setRouterAdminRules(network.getProviderSegID(), network.getNetworkType(), false);
        }

        setInternalRoutes(osRouter, osSubnet, false);
        setGatewayIcmp(osSubnet, false);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouter, osRouterIface, false);
        }
        log.info("Disconnected subnet({}) from {}", osSubnet.getCidr(), osRouter.getName());
    }

    private void setSourceNat(Router osRouter, RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkService.network(osSubnet.getNetworkId());

        osNodeService.completeNodes(COMPUTE).forEach(cNode -> {
            setRulesToGateway(cNode, osNet.getProviderSegID(),
                    IpPrefix.valueOf(osSubnet.getCidr()), osNet.getNetworkType(),
                    install);
        });

        if (useStatefulSnat) {
            setStatefulSnatRules(routerIface, install);
        } else {
            setReactiveSnatRules(routerIface, install);
        }

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.info(updateStr + "external access for subnet({})", osSubnet.getCidr());
    }

    private void setStatefulSnatRules(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkService.network(osSubnet.getNetworkId());

        Optional<Router> osRouter = osRouterService.routers().stream()
                .filter(router -> osRouterService.routerInterfaces(routerIface.getId()) != null)
                .findAny();

        if (!osRouter.isPresent()) {
            log.error("Cannot find a router for router interface {} ", routerIface);
            return;
        }
        IpAddress natAddress = getGatewayIpAddress(osRouter.get());
        if (natAddress == null) {
            return;
        }
        String netId = osNetworkService.subnet(routerIface.getSubnetId()).getNetworkId();

        osNodeService.completeNodes(OpenstackNode.NodeType.GATEWAY)
                .forEach(gwNode -> {
                        instancePortService.instancePorts(netId).stream()
                            .forEach(port -> setRulesForSnatIngressRule(gwNode.intgBridge(),
                                    Long.parseLong(osNet.getProviderSegID()),
                                    IpPrefix.valueOf(port.ipAddress(), 32),
                                    port.deviceId(),
                                    install));

                        setOvsNatIngressRule(gwNode.intgBridge(),
                                IpPrefix.valueOf(natAddress, 32),
                                Constants.DEFAULT_EXTERNAL_ROUTER_MAC, install);
                        setOvsNatEgressRule(gwNode.intgBridge(),
                                natAddress, Long.parseLong(osNet.getProviderSegID()),
                                gwNode.patchPortNum(), install);
                });
    }

    private void setReactiveSnatRules(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkService.network(osSubnet.getNetworkId());

        osNodeService.completeNodes(GATEWAY)
                .forEach(gwNode -> setRulesToController(
                        gwNode.intgBridge(),
                        osNet.getProviderSegID(),
                        IpPrefix.valueOf(osSubnet.getCidr()),
                        osNet.getNetworkType(),
                        install));
    }

    private IpAddress getGatewayIpAddress(Router osRouter) {

        String extNetId = osNetworkService.network(osRouter.getExternalGatewayInfo().getNetworkId()).getId();
        Optional<Subnet> extSubnet = osNetworkService.subnets().stream()
                .filter(subnet -> subnet.getNetworkId().equals(extNetId))
                .findAny();

        if (!extSubnet.isPresent()) {
            log.error("Cannot find externel subnet for the router");
            return null;
        }

        return IpAddress.valueOf(extSubnet.get().getGateway());
    }

    private void resetSnatRules() {
        if (useStatefulSnat) {
            osRouterService.routerInterfaces().forEach(
                    routerIface -> {
                        setReactiveSnatRules(routerIface, false);
                        setStatefulSnatRules(routerIface, true);
                    }
            );
        } else {
            osRouterService.routerInterfaces().forEach(
                    routerIface -> {
                        setStatefulSnatRules(routerIface, false);
                        setReactiveSnatRules(routerIface, true);
                    }
            );
        }
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
                osNodeService.completeNodes(COMPUTE).stream()
                        .filter(cNode -> cNode.dataIp() != null)
                        .forEach(cNode -> setRulesToGatewayWithDstIp(
                                cNode,
                                cNode.gatewayGroupId(NetworkMode.VXLAN),
                                network.getProviderSegID(),
                                IpAddress.valueOf(osSubnet.getGateway()),
                                NetworkMode.VXLAN,
                                install));
                break;
            case VLAN:
                osNodeService.completeNodes(COMPUTE).stream()
                        .filter(cNode -> cNode.vlanPortNum() != null)
                        .forEach(cNode -> setRulesToGatewayWithDstIp(
                                cNode,
                                cNode.gatewayGroupId(NetworkMode.VLAN),
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
        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            setGatewayIcmpRule(
                    gatewayIp,
                    gNode.intgBridge(),
                    install
            );
        });

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.debug(updateStr + "ICMP to {}", osSubnet.getGateway());
    }

    private void setInternalRoutes(Router osRouter, Subnet updatedSubnet, boolean install) {
        Network updatedNetwork = osNetworkService.network(updatedSubnet.getNetworkId());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, updatedSubnet.getId());
        String updatedSegmendId = getSegmentId(updatedSubnet);

        // installs rule from/to my subnet intentionally to fix ICMP failure
        // to my subnet gateway if no external gateway added to the router
        osNodeService.completeNodes(COMPUTE).forEach(cNode -> {
            setInternalRouterRules(
                    cNode.intgBridge(),
                    updatedSegmendId,
                    updatedSegmendId,
                    IpPrefix.valueOf(updatedSubnet.getCidr()),
                    IpPrefix.valueOf(updatedSubnet.getCidr()),
                    updatedNetwork.getNetworkType(),
                    install
            );

            routableSubnets.forEach(subnet -> {
                setInternalRouterRules(
                        cNode.intgBridge(),
                        updatedSegmendId,
                        getSegmentId(subnet),
                        IpPrefix.valueOf(updatedSubnet.getCidr()),
                        IpPrefix.valueOf(subnet.getCidr()),
                        updatedNetwork.getNetworkType(),
                        install
                );
                setInternalRouterRules(
                        cNode.intgBridge(),
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
                .matchIPDst(gatewayIp.getIp4Address().toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_ICMP_RULE,
                Constants.GW_COMMON_TABLE,
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
                        .matchIPSrc(srcSubnet.getIp4Prefix())
                        .matchIPDst(dstSubnet.getIp4Prefix())
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setTunnelId(Long.parseLong(dstSegmentId))
                        .transition(FORWARDING_TABLE)
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
                        .transition(FORWARDING_TABLE)
                        .build();

                osFlowRuleService.setRule(
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        ROUTING_TABLE,
                        install);
                break;
            case VLAN:
                selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchVlanId(VlanId.vlanId(srcSegmentId))
                        .matchIPSrc(srcSubnet.getIp4Prefix())
                        .matchIPDst(dstSubnet.getIp4Prefix())
                        .build();

                treatment = DefaultTrafficTreatment.builder()
                        .setVlanId(VlanId.vlanId(dstSegmentId))
                        .transition(FORWARDING_TABLE)
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
                        .transition(FORWARDING_TABLE)
                        .build();

                osFlowRuleService.setRule(
                        appId,
                        deviceId,
                        selector,
                        treatment,
                        PRIORITY_INTERNAL_ROUTING_RULE,
                        ROUTING_TABLE,
                        install);
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }

    }

    private void setRulesToGateway(OpenstackNode osNode, String segmentId, IpPrefix srcSubnet,
                                   NetworkType networkType, boolean install) {
        TrafficTreatment treatment;
        GroupId groupId;

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (networkType) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                groupId = osNode.gatewayGroupId(NetworkMode.VXLAN);
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                groupId = osNode.gatewayGroupId(NetworkMode.VLAN);
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

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                sBuilder.build(),
                treatment,
                PRIORITY_EXTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setRulesForSnatIngressRule(DeviceId deviceId, Long vni, IpPrefix destVmIp,
                                            DeviceId dstDeviceId, boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(destVmIp)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(vni)
                .extension(buildExtension(
                        deviceService,
                        deviceId,
                        osNodeService.node(dstDeviceId).dataIp().getIp4Address()),
                        deviceId)
                .setOutput(osNodeService.node(deviceId).tunnelPortNum())
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_EXTERNAL_ROUTING_RULE,
                Constants.GW_COMMON_TABLE,
                install);
    }

    private void setRulesToGatewayWithDstIp(OpenstackNode osNode, GroupId groupId,
                                            String segmentId, IpAddress dstIp,
                                            NetworkMode networkMode, boolean install) {
        TrafficSelector selector;
        if (networkMode.equals(NetworkMode.VXLAN)) {
            selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchTunnelId(Long.valueOf(segmentId))
                    .matchIPDst(dstIp.getIp4Address().toIpPrefix())
                    .build();
        } else {
            selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchVlanId(VlanId.vlanId(segmentId))
                    .matchIPDst(dstIp.getIp4Address().toIpPrefix())
                    .build();
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .group(groupId)
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setOvsNatIngressRule(DeviceId deviceId, IpPrefix cidr, MacAddress dstMac, boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(cidr)
                .build();

        ExtensionTreatment natTreatment = RulePopulatorUtil.niciraConnTrackTreatmentBuilder(driverService, deviceId)
                .commit(false)
                .natAction(true)
                .table((short) 0)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(dstMac)
                .extension(natTreatment, deviceId)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_COMMON_TABLE,
                install);
    }

    private void setOvsNatEgressRule(DeviceId deviceId, IpAddress natAddress, long vni, PortNumber output,
                                     boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(DEFAULT_GATEWAY_MAC)
                .matchTunnelId(vni)
                .build();

        ExtensionTreatment natTreatment = RulePopulatorUtil.niciraConnTrackTreatmentBuilder(driverService, deviceId)
                .commit(true)
                .natAction(true)
                .natIp(natAddress)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(natTreatment, deviceId)
                .setEthDst(DEFAULT_EXTERNAL_ROUTER_MAC)
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setOutput(output)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_COMMON_TABLE,
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
                        .matchEthDst(osNodeService.node(deviceId).vlanPortMac());
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

        osFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_EXTERNAL_ROUTING_RULE,
                GW_COMMON_TABLE,
                install);


        // Sends ICMP response to controller for SNATing ingress traffic
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(ICMP.TYPE_ECHO_REPLY)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_INTERNAL_ROUTING_RULE,
                GW_COMMON_TABLE,
                install);
    }

    private void setRouterAdminRules(String segmentId, NetworkType networkType, boolean install) {
        TrafficTreatment treatment;
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);

        switch (networkType) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
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
                    eventExecutor.execute(() -> routerRemove(event.subject()));
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
                case OPENSTACK_NODE_COMPLETE:
                case OPENSTACK_NODE_INCOMPLETE:
                    eventExecutor.execute(() -> {
                        log.info("Reconfigure routers for {}", osNode.hostname());
                        reconfigureRouters();
                    });
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
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

    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    eventExecutor.execute(() -> {
                        log.info("RoutingHandler: Instance port detected MAC:{} IP:{}",
                                instPort.macAddress(),
                                instPort.ipAddress());
                        instPortDetected(event.subject());
                    });
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() -> {
                        log.info("RoutingHandler: Instance port vanished MAC:{} IP:{}",
                                instPort.macAddress(),
                                instPort.ipAddress());
                        instPortRemoved(event.subject());
                    });
                    break;
                default:
                    break;
            }
        }

        private void instPortDetected(InstancePort instPort) {
            osNodeService.completeNodes(GATEWAY)
                    .forEach(gwNode -> setRulesForSnatIngressRule(gwNode.intgBridge(),
                                    Long.parseLong(osNetworkService.network(instPort.networkId()).getProviderSegID()),
                                    IpPrefix.valueOf(instPort.ipAddress(), 32),
                                    instPort.deviceId(),
                                    true));
        }

        private void instPortRemoved(InstancePort instPort) {
            osNodeService.completeNodes(GATEWAY)
                    .forEach(gwNode -> setRulesForSnatIngressRule(gwNode.intgBridge(),
                                    Long.parseLong(osNetworkService.network(instPort.networkId()).getProviderSegID()),
                                    IpPrefix.valueOf(instPort.ipAddress(), 32),
                                    instPort.deviceId(),
                                    false));
        }
    }
}
