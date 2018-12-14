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
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
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
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.util.RulePopulatorUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ADMIN_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_EXTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_INTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GENEVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GRE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VXLAN;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT_DEFAULT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.tunnelPortNumByNetType;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Handles OpenStack router events.
 */
@Component(
    immediate = true,
    property = {
        USE_STATEFUL_SNAT + ":Boolean=" + USE_STATEFUL_SNAT_DEFAULT
    }
)
public class OpenstackRoutingHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_ENABLED = "Enabled ";
    private static final String MSG_DISABLED = "Disabled ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";

    private static final int VM_PREFIX = 32;

    /** Use Stateful SNAT for source NATing. */
    private boolean useStatefulSnat = USE_STATEFUL_SNAT_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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

        flag = Tools.isPropertyEnabled(properties, USE_STATEFUL_SNAT);
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
            Network network = osNetworkAdminService.network(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                    .getNetworkId());
            Type netType = osNetworkAdminService.networkType(
                    osNetworkAdminService.subnet(iface.getSubnetId())
                    .getNetworkId());
            setRouterAdminRules(network.getProviderSegID(),
                                netType, !osRouter.isAdminStateUp());
        });

        ExternalPeerRouter externalPeerRouter =
                            osNetworkAdminService.externalPeerRouter(exGateway);
        VlanId vlanId = externalPeerRouter == null ? VlanId.NONE : externalPeerRouter.vlanId();

        if (exGateway == null) {
            deleteUnassociatedExternalPeerRouter();
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface ->
                    setSourceNat(iface, false));
        } else {
            osNetworkAdminService.deriveExternalPeerRouterMac(exGateway, osRouter, vlanId);
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface ->
                    setSourceNat(iface, exGateway.isEnableSnat()));
        }
    }

    private void deleteUnassociatedExternalPeerRouter() {
        log.trace("Deleting unassociated external peer router");

        try {
            Set<String> routerIps = Sets.newConcurrentHashSet();

            osRouterService.routers().stream()
                    .filter(router -> getGatewayIpAddress(router) != null)
                    .map(router -> getGatewayIpAddress(router).toString())
                    .forEach(routerIps::add);

            osNetworkAdminService.externalPeerRouters().stream()
                    .filter(externalPeerRouter ->
                            !routerIps.contains(externalPeerRouter.ipAddress().toString()))
                    .forEach(externalPeerRouter -> {
                        osNetworkAdminService
                                .deleteExternalPeerRouter(
                                        externalPeerRouter.ipAddress().toString());
                        log.trace("Deleted unassociated external peer router {}",
                                externalPeerRouter.ipAddress().toString());
                    });
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }
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
        setGatewayIcmp(osSubnet, osRouter, true);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, true);
        }
        log.info("Connected subnet({}) to {}", osSubnet.getCidr(), osRouter.getName());
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
        setGatewayIcmp(osSubnet, osRouter, false);
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, false);
        }
        log.info("Disconnected subnet({}) from {}", osSubnet.getCidr(), osRouter.getName());
    }

    private void setSourceNat(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        osNodeService.completeNodes(COMPUTE).forEach(cNode -> {
            setRulesToGateway(cNode, osNet.getProviderSegID(),
                    IpPrefix.valueOf(osSubnet.getCidr()), netType, install);
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
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        if (netType == FLAT) {
            return;
        }

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
        String netId = osNetworkAdminService.subnet(routerIface.getSubnetId()).getNetworkId();

        osNodeService.completeNodes(OpenstackNode.NodeType.GATEWAY)
                .forEach(gwNode -> {
                        instancePortService.instancePorts(netId)
                                .stream()
                                .filter(port -> port.state() == ACTIVE)
                                .forEach(port -> setRulesForSnatIngressRule(gwNode.intgBridge(),
                                    Long.parseLong(osNet.getProviderSegID()),
                                    IpPrefix.valueOf(port.ipAddress(), VM_PREFIX),
                                    port.deviceId(),
                                    netType,
                                    install));

                        setOvsNatIngressRule(gwNode.intgBridge(),
                                IpPrefix.valueOf(natAddress, VM_PREFIX),
                                Constants.DEFAULT_EXTERNAL_ROUTER_MAC, install);
                        setOvsNatEgressRule(gwNode.intgBridge(),
                                natAddress, Long.parseLong(osNet.getProviderSegID()),
                                gwNode.patchPortNum(), install);
                });
    }

    private void setReactiveSnatRules(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        osNodeService.completeNodes(GATEWAY)
                .forEach(gwNode -> setRulesToController(
                        gwNode.intgBridge(),
                        osNet.getProviderSegID(),
                        IpPrefix.valueOf(osSubnet.getCidr()),
                        netType,
                        install));
    }

    private IpAddress getGatewayIpAddress(Router osRouter) {

        if (osRouter.getExternalGatewayInfo() == null) {
            return null;
        }
        String extNetId = osNetworkAdminService.network(
                        osRouter.getExternalGatewayInfo().getNetworkId()).getId();
        Optional<Subnet> extSubnet = osNetworkAdminService.subnets().stream()
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

    private void setGatewayIcmp(Subnet osSubnet, Router osRouter, boolean install) {
        OpenstackNode srcNatGw = osNodeService.completeNodes(GATEWAY)
                                         .stream().findFirst().orElse(null);

        if (srcNatGw == null) {
            return;
        }

        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            // do nothing if no gateway is set
            return;
        }

        // take ICMP request to a subnet gateway through gateway node group
        Network net = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, osSubnet.getId());

        switch (netType) {
            case VXLAN:
                setGatewayIcmpForVxlan(osSubnet, srcNatGw, net, routableSubnets, install);
                break;
            case GRE:
                setGatewayIcmpForGre(osSubnet, srcNatGw, net, routableSubnets, install);
                break;
            case GENEVE:
                setGatewayIcmpForGeneve(osSubnet, srcNatGw, net, routableSubnets, install);
                break;
            case VLAN:
                setGatewayIcmpForVlan(osSubnet, srcNatGw, net, routableSubnets, install);
                break;
            default:
                final String error = String.format("%s %s", ERR_UNSUPPORTED_NET_TYPE,
                                                            netType.toString());
                throw new IllegalStateException(error);
        }

        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        osNodeService.completeNodes(GATEWAY).forEach(gNode ->
            setGatewayIcmpRule(
                    gatewayIp,
                    gNode.intgBridge(),
                    install));

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.debug(updateStr + "ICMP to {}", osSubnet.getGateway());
    }

    private void setGatewayIcmpForVxlan(Subnet osSubnet,
                                        OpenstackNode srcNatGw,
                                        Network network,
                                        Set<Subnet> routableSubnets,
                                        boolean install) {
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> setRulesToGatewayWithRoutableSubnets(
                        cNode,
                        srcNatGw,
                        network.getProviderSegID(),
                        osSubnet,
                        routableSubnets,
                        VXLAN,
                        install));
    }

    private void setGatewayIcmpForGre(Subnet osSubnet,
                                      OpenstackNode srcNatGw,
                                      Network network,
                                      Set<Subnet> routableSubnets,
                                      boolean install) {
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> setRulesToGatewayWithRoutableSubnets(
                        cNode,
                        srcNatGw,
                        network.getProviderSegID(),
                        osSubnet,
                        routableSubnets,
                        GRE,
                        install));
    }

    private void setGatewayIcmpForGeneve(Subnet osSubnet,
                                         OpenstackNode srcNatGw,
                                         Network network,
                                         Set<Subnet> routableSubnets,
                                         boolean install) {
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> setRulesToGatewayWithRoutableSubnets(
                        cNode,
                        srcNatGw,
                        network.getProviderSegID(),
                        osSubnet,
                        routableSubnets,
                        GENEVE,
                        install));
    }

    private void setGatewayIcmpForVlan(Subnet osSubnet,
                                       OpenstackNode srcNatGw,
                                       Network network,
                                       Set<Subnet> routableSubnets,
                                       boolean install) {
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.vlanPortNum() != null)
                .forEach(cNode -> setRulesToGatewayWithRoutableSubnets(
                        cNode,
                        srcNatGw,
                        network.getProviderSegID(),
                        osSubnet,
                        routableSubnets,
                        VLAN,
                        install));
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

    private void setGatewayIcmpRule(IpAddress gatewayIp, DeviceId deviceId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(gatewayIp.getIp4Address().toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
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

    private void setRulesToGateway(OpenstackNode osNode,
                                   String segmentId,
                                   IpPrefix srcSubnet,
                                   Type networkType,
                                   boolean install) {
        OpenstackNode sourceNatGateway =
                osNodeService.completeNodes(GATEWAY).stream().findFirst().orElse(null);

        if (sourceNatGateway == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

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

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                PortNumber portNum = tunnelPortNumByNetType(networkType, osNode);
                tBuilder.extension(buildExtension(
                                deviceService,
                                osNode.intgBridge(),
                                sourceNatGateway.dataIp().getIp4Address()),
                                osNode.intgBridge())
                        .setOutput(portNum);
                break;

            case VLAN:
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
                PRIORITY_EXTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);
    }

    private void setRulesForSnatIngressRule(DeviceId deviceId,
                                            Long vni,
                                            IpPrefix destVmIp,
                                            DeviceId dstDeviceId,
                                            Type networkType,
                                            boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(destVmIp)
                .build();

        PortNumber portNum = tunnelPortNumByNetType(networkType,
                                                    osNodeService.node(deviceId));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(vni)
                .extension(buildExtension(
                        deviceService,
                        deviceId,
                        osNodeService.node(dstDeviceId).dataIp().getIp4Address()),
                        deviceId)
                .setOutput(portNum)
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

    private void setRulesToGatewayWithRoutableSubnets(OpenstackNode osNode,
                                                      OpenstackNode sourceNatGateway,
                                                      String segmentId,
                                                      Subnet updatedSubnet,
                                                      Set<Subnet> routableSubnets,
                                                      Type networkType,
                                                      boolean install) {
        //At first we install flow rules to gateway with segId and gatewayIp of updated subnet
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

    private void setOvsNatIngressRule(DeviceId deviceId,
                                      IpPrefix cidr,
                                      MacAddress dstMac,
                                      boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(cidr)
                .build();

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, deviceId)
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

    private void setOvsNatEgressRule(DeviceId deviceId,
                                     IpAddress natAddress,
                                     long vni,
                                     PortNumber output,
                                     boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(DEFAULT_GATEWAY_MAC)
                .matchTunnelId(vni)
                .build();

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, deviceId)
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

    private void setRulesToController(DeviceId deviceId,
                                      String segmentId,
                                      IpPrefix srcSubnet,
                                      Type networkType,
                                      boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet)
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

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

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (networkType == VLAN) {
            tBuilder.popVlan();
        }

        tBuilder.punt();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_EXTERNAL_ROUTING_RULE,
                GW_COMMON_TABLE,
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

        // FIXME only one leader in the cluster should process
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
                                                    event.subject().getName(),
                                                    event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterUpdate(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is updated",
                                                    event.subject().getName(),
                                                    event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is removed",
                                                    event.subject().getName(),
                                                    event.subject().getId());

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
                case OPENSTACK_NODE_INCOMPLETE:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        reconfigureRouters(osNode);
                    });
                    break;
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
            log.info("Reconfigure routers for {}", osNode.hostname());
        }
    }

    private class InternalInstancePortListener implements InstancePortListener {

        private boolean isRelevantHelper(InstancePortEvent event) {
            return mastershipService.isLocalMaster(event.subject().deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                    eventExecutor.execute(() ->
                                processInstancePortDetection(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() ->
                                processInstancePortRemoval(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_STARTED:
                    eventExecutor.execute(() ->
                                processInstanceMigrationStart(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    eventExecutor.execute(() ->
                                processInstanceMigrationEnd(event, instPort));
                    break;
                default:
                    break;
            }
        }

        private void processInstancePortDetection(InstancePortEvent event,
                                                  InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Instance port detected MAC:{} IP:{}",
                                                    instPort.macAddress(),
                                                    instPort.ipAddress());

            instPortDetected(event.subject());
        }

        private void processInstancePortRemoval(InstancePortEvent event,
                                                InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Instance port vanished MAC:{} IP:{}",
                                                    instPort.macAddress(),
                                                    instPort.ipAddress());

            instPortRemoved(event.subject());
        }

        private void processInstanceMigrationStart(InstancePortEvent event,
                                                   InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Migration started for MAC:{} IP:{}",
                                                    instPort.macAddress(),
                                                    instPort.ipAddress());

            instPortDetected(instPort);
        }

        private void processInstanceMigrationEnd(InstancePortEvent event,
                                                 InstancePort instPort) {
            log.info("RoutingHandler: Migration finished for MAC:{} IP:{}",
                                                    instPort.macAddress(),
                                                    instPort.ipAddress());
            // TODO: need to reconfigure rules to point to update VM
        }

        private void instPortDetected(InstancePort instPort) {
            Network network = osNetworkAdminService.network(instPort.networkId());
            Type netType = osNetworkAdminService.networkType(instPort.networkId());

            if (netType == FLAT) {
                return;
            }

            if (useStatefulSnat) {
                osNodeService.completeNodes(GATEWAY)
                        .forEach(gwNode -> setRulesForSnatIngressRule(
                                gwNode.intgBridge(),
                                Long.parseLong(network.getProviderSegID()),
                                IpPrefix.valueOf(instPort.ipAddress(), VM_PREFIX),
                                instPort.deviceId(), netType, true));
            }
        }

        private void instPortRemoved(InstancePort instPort) {
            Network network = osNetworkAdminService.network(instPort.networkId());
            Type netType = osNetworkAdminService.networkType(instPort.networkId());

            if (netType == FLAT) {
                return;
            }

            if (useStatefulSnat) {
                osNodeService.completeNodes(GATEWAY)
                        .forEach(gwNode -> setRulesForSnatIngressRule(
                                gwNode.intgBridge(),
                                Long.parseLong(network.getProviderSegID()),
                                IpPrefix.valueOf(instPort.ipAddress(), VM_PREFIX),
                                instPort.deviceId(), netType, false));
            }
        }
    }
}
