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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
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
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_EXTERNAL_FLOATING_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLOATING_EXTERNAL;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLOATING_INTERNAL;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_ENDED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.associatedFloatingIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByComputeDevId;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.isAssociatedWithVM;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Handles OpenStack floating IP events.
 */
@Component(immediate = true)
public class OpenstackRoutingFloatingIpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_FLOW = "Failed set flows for floating IP %s: ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type %s";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterAdminService osRouterAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final OpenstackRouterListener floatingIpListener = new InternalFloatingIpListener();
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeListener();
    private final Map<String, DeviceId> migrationPool = Maps.newConcurrentMap();
    private final OpenstackNetworkListener osNetworkListener = new InternalOpenstackNetworkListener();
    private final InstancePortListener instPortListener = new InternalInstancePortListener();

    private Map<String, Port> terminatedOsPorts = Maps.newConcurrentMap();
    private Map<String, InstancePort> terminatedInstPorts = Maps.newConcurrentMap();
    private Map<String, InstancePort> tobeRemovedInstPorts = Maps.newConcurrentMap();
    private Map<String, NetFloatingIP> pendingInstPortIds = Maps.newConcurrentMap();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osRouterAdminService.addListener(floatingIpListener);
        osNodeService.addListener(osNodeListener);
        instancePortService.addListener(instancePortListener);
        osNodeService.addListener(osNodeListener);
        osNetworkService.addListener(osNetworkListener);
        instancePortService.addListener(instPortListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        instancePortService.removeListener(instancePortListener);
        instancePortService.removeListener(instPortListener);
        osNetworkService.removeListener(osNetworkListener);
        osNodeService.removeListener(osNodeListener);
        osRouterAdminService.removeListener(floatingIpListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setFloatingIpRules(NetFloatingIP floatingIp, Port osPort,
                                    OpenstackNode gateway, boolean install) {
        Network osNet = osNetworkService.network(osPort.getNetworkId());
        if (osNet == null) {
            final String errorFormat = ERR_FLOW + "no network(%s) exists";
            final String error = String.format(errorFormat,
                    floatingIp.getFloatingIpAddress(),
                    osPort.getNetworkId());
            throw new IllegalStateException(error);
        }

        MacAddress srcMac = MacAddress.valueOf(osPort.getMacAddress());
        log.trace("Mac address of openstack port: {}", srcMac);
        InstancePort instPort = instancePortService.instancePort(srcMac);

        if (instPort == null) {
            instPort = tobeRemovedInstPorts.get(osPort.getId());
            tobeRemovedInstPorts.remove(osPort.getId());
        }

        if (instPort == null) {
            instPort = terminatedInstPorts.get(osPort.getId());
        }

        if (instPort == null) {
            final String errorFormat = ERR_FLOW + "no host(MAC:%s) found";
            final String error = String.format(errorFormat,
                    floatingIp.getFloatingIpAddress(), srcMac);
            throw new IllegalStateException(error);
        }

        ExternalPeerRouter externalPeerRouter = externalPeerRouter(osNet);
        if (externalPeerRouter == null) {
            final String errorFormat = ERR_FLOW + "no external peer router found";
            throw new IllegalStateException(errorFormat);
        }

        updateComputeNodeRules(instPort, osNet, gateway, install);
        updateGatewayNodeRules(floatingIp, instPort, osNet, externalPeerRouter, gateway, install);

        // FIXME: downstream internal rules are still duplicated in all gateway nodes
        // need to make the internal rules de-duplicated sooner or later
        setDownstreamInternalRules(floatingIp, osNet, instPort, install);

        // TODO: need to refactor setUpstreamRules if possible
        setUpstreamRules(floatingIp, osNet, instPort, externalPeerRouter, install);
        log.trace("Succeeded to set flow rules for floating ip {}:{} and install: {}",
                floatingIp.getFloatingIpAddress(),
                floatingIp.getFixedIpAddress(),
                install);
    }

    private synchronized void updateGatewayNodeRules(NetFloatingIP fip,
                                                     InstancePort instPort,
                                                     Network osNet,
                                                     ExternalPeerRouter router,
                                                     OpenstackNode gateway,
                                                     boolean install) {

        Set<OpenstackNode> completedGws = osNodeService.completeNodes(GATEWAY);
        Set<OpenstackNode> finalGws = Sets.newConcurrentHashSet();
        finalGws.addAll(ImmutableSet.copyOf(completedGws));


        if (gateway == null) {
            // these are floating IP related cases...
            setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                        ImmutableSet.copyOf(finalGws), install);

        } else {
            // these are openstack node related cases...
            if (install) {
                if (completedGws.contains(gateway)) {
                    if (completedGws.size() > 1) {
                        finalGws.remove(gateway);
                        if (fip.getPortId() != null) {
                            setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                    ImmutableSet.copyOf(finalGws), false);
                            finalGws.add(gateway);
                        }
                    }
                    if (fip.getPortId() != null) {
                        setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                ImmutableSet.copyOf(finalGws), true);
                    }
                } else {
                    log.warn("Detected node should be included in completed gateway set");
                }
            } else {
                if (!completedGws.contains(gateway)) {
                    if (completedGws.size() >= 1) {
                        if (fip.getPortId() != null) {
                            setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                    ImmutableSet.copyOf(finalGws), true);
                        }
                    }
                } else {
                    log.warn("Detected node should NOT be included in completed gateway set");
                }
            }
        }
    }

    private synchronized void updateComputeNodeRules(InstancePort instPort,
                                                     Network osNet,
                                                     OpenstackNode gateway,
                                                     boolean install) {

        Set<OpenstackNode> completedGws = osNodeService.completeNodes(GATEWAY);
        Set<OpenstackNode> finalGws = Sets.newConcurrentHashSet();
        finalGws.addAll(ImmutableSet.copyOf(completedGws));

        if (gateway == null) {
            // these are floating IP related cases...
            setComputeNodeToGatewayHelper(instPort, osNet,
                    ImmutableSet.copyOf(finalGws), install);

        } else {
            // these are openstack node related cases...
            if (install) {
                if (completedGws.contains(gateway)) {
                    if (completedGws.size() > 1) {
                        finalGws.remove(gateway);
                        setComputeNodeToGatewayHelper(instPort, osNet,
                                ImmutableSet.copyOf(finalGws), false);
                        finalGws.add(gateway);
                    }

                    setComputeNodeToGatewayHelper(instPort, osNet,
                            ImmutableSet.copyOf(finalGws), true);
                } else {
                    log.warn("Detected node should be included in completed gateway set");
                }
            } else {
                if (!completedGws.contains(gateway)) {
                    finalGws.add(gateway);
                    setComputeNodeToGatewayHelper(instPort, osNet,
                            ImmutableSet.copyOf(finalGws), false);
                    finalGws.remove(gateway);
                    if (completedGws.size() >= 1) {
                        setComputeNodeToGatewayHelper(instPort, osNet,
                                ImmutableSet.copyOf(finalGws), true);
                    }
                } else {
                    log.warn("Detected node should NOT be included in completed gateway set");
                }
            }
        }
    }

    // a helper method
    private void setComputeNodeToGatewayHelper(InstancePort instPort,
                                               Network osNet,
                                               Set<OpenstackNode> gateways,
                                               boolean install) {
        TrafficTreatment treatment;

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(instPort.ipAddress().toIpPrefix())
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (osNet.getNetworkType()) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(osNet.getProviderSegID()));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(osNet.getProviderSegID()));
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType().toString());
                throw new IllegalStateException(error);
        }

        OpenstackNode selectedGatewayNode = getGwByComputeDevId(gateways, instPort.deviceId());

        if (selectedGatewayNode == null) {
            final String errorFormat = ERR_FLOW + "no gateway node selected";
            throw new IllegalStateException(errorFormat);
        }
        treatment = DefaultTrafficTreatment.builder()
                .extension(buildExtension(
                        deviceService,
                        instPort.deviceId(),
                        selectedGatewayNode.dataIp().getIp4Address()),
                        instPort.deviceId())
                .setOutput(osNodeService.node(instPort.deviceId()).tunnelPortNum())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                sBuilder.build(),
                treatment,
                PRIORITY_EXTERNAL_FLOATING_ROUTING_RULE,
                ROUTING_TABLE,
                install);
        log.trace("Succeeded to set flow rules from compute node to gateway on compute node");
    }

    private void setDownstreamInternalRules(NetFloatingIP floatingIp,
                                            Network osNet,
                                            InstancePort instPort,
                                            boolean install) {
        OpenstackNode cNode = osNodeService.node(instPort.deviceId());
        if (cNode == null) {
            final String error = String.format("Cannot find openstack node for device %s",
                    instPort.deviceId());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VXLAN && cNode.dataIp() == null) {
            final String errorFormat = ERR_FLOW + "VXLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VLAN && cNode.vlanIntf() == null) {
            final String errorFormat = ERR_FLOW + "VLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }

        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());

        // TODO: following code snippet will be refactored sooner or later
        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            // access from one VM to the others via floating IP
            TrafficSelector internalSelector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(floating.toIpPrefix())
                    .matchInPort(gNode.tunnelPortNum())
                    .build();

            TrafficTreatment.Builder internalBuilder = DefaultTrafficTreatment.builder()
                    .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                    .setEthDst(instPort.macAddress())
                    .setIpDst(instPort.ipAddress().getIp4Address());

            switch (osNet.getNetworkType()) {
                case VXLAN:
                    internalBuilder.setTunnelId(Long.valueOf(osNet.getProviderSegID()))
                            .extension(buildExtension(
                                    deviceService,
                                    gNode.intgBridge(),
                                    cNode.dataIp().getIp4Address()),
                                    gNode.intgBridge())
                            .setOutput(PortNumber.IN_PORT);
                    break;
                case VLAN:
                    internalBuilder.pushVlan()
                            .setVlanId(VlanId.vlanId(osNet.getProviderSegID()))
                            .setOutput(PortNumber.IN_PORT);
                    break;
                default:
                    final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                            osNet.getNetworkType());
                    throw new IllegalStateException(error);
            }

            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    internalSelector,
                    internalBuilder.build(),
                    PRIORITY_FLOATING_INTERNAL,
                    GW_COMMON_TABLE,
                    install);
        });
        log.trace("Succeeded to set flow rules for downstream on gateway nodes");
    }

    private void setDownstreamExternalRulesHelper(NetFloatingIP floatingIp,
                                                  Network osNet,
                                                  InstancePort instPort,
                                                  ExternalPeerRouter externalPeerRouter,
                                                  Set<OpenstackNode> gateways, boolean install) {
        OpenstackNode cNode = osNodeService.node(instPort.deviceId());
        if (cNode == null) {
            final String error = String.format("Cannot find openstack node for device %s",
                    instPort.deviceId());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VXLAN && cNode.dataIp() == null) {
            final String errorFormat = ERR_FLOW + "VXLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VLAN && cNode.vlanIntf() == null) {
            final String errorFormat = ERR_FLOW + "VLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }

        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());

        OpenstackNode selectedGatewayNode = getGwByComputeDevId(gateways, instPort.deviceId());

        if (selectedGatewayNode == null) {
            final String errorFormat = ERR_FLOW + "no gateway node selected";
            throw new IllegalStateException(errorFormat);
        }

        TrafficSelector.Builder externalSelectorBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(floating.toIpPrefix());

        TrafficTreatment.Builder externalTreatmentBuilder = DefaultTrafficTreatment.builder()
                .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                .setEthDst(instPort.macAddress())
                .setIpDst(instPort.ipAddress().getIp4Address());

        if (!externalPeerRouter.externalPeerRouterVlanId().equals(VlanId.NONE)) {
            externalSelectorBuilder.matchVlanId(externalPeerRouter.externalPeerRouterVlanId()).build();
            externalTreatmentBuilder.popVlan();
        }

        switch (osNet.getNetworkType()) {
            case VXLAN:
                externalTreatmentBuilder.setTunnelId(Long.valueOf(osNet.getProviderSegID()))
                        .extension(buildExtension(
                                deviceService,
                                selectedGatewayNode.intgBridge(),
                                cNode.dataIp().getIp4Address()),
                                selectedGatewayNode.intgBridge())
                        .setOutput(selectedGatewayNode.tunnelPortNum());
                break;
            case VLAN:
                externalTreatmentBuilder.pushVlan()
                        .setVlanId(VlanId.vlanId(osNet.getProviderSegID()))
                        .setOutput(selectedGatewayNode.vlanPortNum());
                break;
            default:
                final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType());
                throw new IllegalStateException(error);
        }

        osFlowRuleService.setRule(
                appId,
                selectedGatewayNode.intgBridge(),
                externalSelectorBuilder.build(),
                externalTreatmentBuilder.build(),
                PRIORITY_FLOATING_EXTERNAL,
                GW_COMMON_TABLE,
                install);
    }

    private void setUpstreamRules(NetFloatingIP floatingIp, Network osNet,
                                  InstancePort instPort, ExternalPeerRouter externalPeerRouter,
                                  boolean install) {
        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(instPort.ipAddress().toIpPrefix());

        switch (osNet.getNetworkType()) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.valueOf(osNet.getProviderSegID()));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(osNet.getProviderSegID()));
                break;
            default:
                final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType());
                throw new IllegalStateException(error);
        }

        TrafficSelector selector = sBuilder.build();

        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setIpSrc(floating.getIp4Address())
                    .setEthSrc(instPort.macAddress())
                    .setEthDst(externalPeerRouter.externalPeerRouterMac());

            if (osNet.getNetworkType().equals(NetworkType.VLAN)) {
                tBuilder.popVlan();
            }

            if (!externalPeerRouter.externalPeerRouterVlanId().equals(VlanId.NONE)) {
                tBuilder.pushVlan().setVlanId(externalPeerRouter.externalPeerRouterVlanId());
            }
            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    selector,
                    tBuilder.setOutput(gNode.uplinkPortNum()).build(),
                    PRIORITY_FLOATING_EXTERNAL,
                    GW_COMMON_TABLE,
                    install);
            });
        log.trace("Succeeded to set flow rules for upstream on gateway nodes");
    }

    private ExternalPeerRouter externalPeerRouter(Network network) {
        if (network == null) {
            return null;
        }

        Subnet subnet = osNetworkService.subnets(network.getId()).stream().findAny().orElse(null);

        if (subnet == null) {
            return null;
        }

        RouterInterface osRouterIface = osRouterAdminService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), subnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            return null;
        }

        Router osRouter = osRouterAdminService.router(osRouterIface.getId());
        if (osRouter == null) {
            return null;
        }
        if (osRouter.getExternalGatewayInfo() == null) {
            return null;
        }

        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();
        return osNetworkService.externalPeerRouter(exGatewayInfo);
    }

    private void associateFloatingIp(NetFloatingIP osFip) {
        Port osPort = osNetworkService.port(osFip.getPortId());
        if (osPort == null) {
            final String errorFormat = ERR_FLOW + "port(%s) not found";
            final String error = String.format(errorFormat,
                    osFip.getFloatingIpAddress(), osFip.getPortId());
            throw new IllegalStateException(error);
        }
        // set floating IP rules only if the port is associated to a VM
        if (!Strings.isNullOrEmpty(osPort.getDeviceId())) {

            if (instancePortService.instancePort(osPort.getId()) == null) {
                log.info("Try to associate the fip {} with a terminated VM",
                        osFip.getFloatingIpAddress());
                pendingInstPortIds.put(osPort.getId(), osFip);
                return;
            }

            setFloatingIpRules(osFip, osPort, null, true);
        }
    }

    private void disassociateFloatingIp(NetFloatingIP osFip, String portId) {
        Port osPort = osNetworkService.port(portId);
        if (osPort == null) {
            osPort = terminatedOsPorts.get(portId);
            terminatedOsPorts.remove(portId);
        }

        if (osPort == null) {
            final String errorFormat = ERR_FLOW + "port(%s) not found";
            final String error = String.format(errorFormat,
                    osFip.getFloatingIpAddress(), osFip.getPortId());
            throw new IllegalStateException(error);
        }

        // set floating IP rules only if the port is associated to a VM
        if (!Strings.isNullOrEmpty(osPort.getDeviceId())) {

            if (!tobeRemovedInstPorts.containsKey(osPort.getId()) &&
                    terminatedInstPorts.containsKey(osPort.getId())) {
                tobeRemovedInstPorts.put(osPort.getId(),
                                    terminatedInstPorts.get(osPort.getId()));
            }

            if (instancePortService.instancePort(osPort.getId()) == null) {

                // in case there is pending instance port, we simply remove that
                // port, otherwise, we directly go with rule removal
                if (pendingInstPortIds.containsKey(osPort.getId())) {
                    log.info("Try to disassociate the fip {} with a terminated VM",
                            osFip.getFloatingIpAddress());
                    pendingInstPortIds.remove(osPort.getId());
                    return;
                }
            }

            setFloatingIpRules(osFip, osPort, null, false);
        }
    }

    private class InternalFloatingIpListener implements OpenstackRouterListener {

        @Override
        public boolean isRelevant(OpenstackRouterEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return event.floatingIp() != null;
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_FLOATING_IP_ASSOCIATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        associateFloatingIp(osFip);
                        log.info("Associated floating IP {}:{}",
                                osFip.getFloatingIpAddress(), osFip.getFixedIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_DISASSOCIATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        disassociateFloatingIp(osFip, event.portId());
                        log.info("Disassociated floating IP {}:{}",
                                osFip.getFloatingIpAddress(), osFip.getFixedIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_CREATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            associateFloatingIp(event.floatingIp());
                        }
                        log.info("Created floating IP {}", osFip.getFloatingIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_REMOVED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            disassociateFloatingIp(osFip, osFip.getPortId());
                        }
                        log.info("Removed floating IP {}", osFip.getFloatingIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_UPDATED:
                case OPENSTACK_ROUTER_CREATED:
                case OPENSTACK_ROUTER_UPDATED:
                case OPENSTACK_ROUTER_REMOVED:
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                case OPENSTACK_ROUTER_INTERFACE_UPDATED:
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                default:
                    // do nothing for the other events
                    break;
            }
        }
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return event.subject().type() == GATEWAY;
        }

        @Override
        public void event(OpenstackNodeEvent event) {

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> {
                        for (NetFloatingIP fip : osRouterAdminService.floatingIps()) {

                            if (Strings.isNullOrEmpty(fip.getPortId())) {
                                continue;
                            }

                            Port osPort = osNetworkService.port(fip.getPortId());
                            if (osPort == null) {
                                log.warn("Failed to set floating IP {}", fip.getId());
                                continue;
                            }

                            // This is for handling a VM, which is associated
                            // with a floating IP, was terminated case
                            // in this case, we cannot obtain instance port
                            // information from a terminated VM, we simply
                            // construct a pending map where key is terminated
                            // instance port ID while value is floating IP
                            // address which supposed to be associated with the
                            // terminated instance port.

                            // Note that, at OPENSTACK_INSTANCE_PORT_DETECTED phase,
                            // we will install floating IP related rules by
                            // referring to the key and value stored in pending map
                            if (!Strings.isNullOrEmpty(osPort.getDeviceId()) &&
                                    instancePortService.instancePort(fip.getPortId()) == null) {
                                pendingInstPortIds.put(fip.getPortId(), fip);
                                continue;
                            }
                            setFloatingIpRules(fip, osPort, event.subject(), true);
                        }
                    });
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                    eventExecutor.execute(() -> {
                        for (NetFloatingIP fip : osRouterAdminService.floatingIps()) {
                            if (Strings.isNullOrEmpty(fip.getPortId())) {
                                continue;
                            }
                            Port osPort = osNetworkService.port(fip.getPortId());
                            if (osPort == null) {
                                log.warn("Failed to set floating IP {}", fip.getId());
                                continue;
                            }
                            Network osNet = osNetworkService.network(osPort.getNetworkId());
                            if (osNet == null) {
                                final String errorFormat = ERR_FLOW + "no network(%s) exists";
                                final String error = String.format(errorFormat,
                                        fip.getFloatingIpAddress(),
                                        osPort.getNetworkId());
                                throw new IllegalStateException(error);
                            }
                            MacAddress srcMac = MacAddress.valueOf(osPort.getMacAddress());
                            log.trace("Mac address of openstack port: {}", srcMac);
                            InstancePort instPort = instancePortService.instancePort(srcMac);

                            if (instPort == null) {
                                final String errorFormat = ERR_FLOW + "no host(MAC:%s) found";
                                final String error = String.format(errorFormat,
                                        fip.getFloatingIpAddress(), srcMac);
                                throw new IllegalStateException(error);
                            }

                            ExternalPeerRouter externalPeerRouter = externalPeerRouter(osNet);
                            if (externalPeerRouter == null) {
                                final String errorFormat = ERR_FLOW + "no external peer router found";
                                throw new IllegalStateException(errorFormat);
                            }

                            updateComputeNodeRules(instPort, osNet, event.subject(), false);
                            updateGatewayNodeRules(fip, instPort, osNet,
                                    externalPeerRouter, event.subject(), false);
                        }
                    });
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {

            if (event.type() == OPENSTACK_INSTANCE_MIGRATION_ENDED ||
                    event.type() == OPENSTACK_INSTANCE_MIGRATION_STARTED) {
                Set<NetFloatingIP> ips = osRouterAdminService.floatingIps();
                NetFloatingIP fip = associatedFloatingIp(event.subject(), ips);

                // we check the possible NPE to avoid duplicated null check
                // for OPENSTACK_INSTANCE_MIGRATION_ENDED and
                // OPENSTACK_INSTANCE_MIGRATION_STARTED cases
                if (fip == null || !isAssociatedWithVM(osNetworkService, fip)) {
                    return false;
                }
            }

            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());

            return Objects.equals(localNodeId, leader);
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            Set<OpenstackNode> gateways = osNodeService.completeNodes(GATEWAY);

            Set<NetFloatingIP> ips = osRouterAdminService.floatingIps();
            NetFloatingIP fip;
            Port osPort;
            Network osNet;
            ExternalPeerRouter externalPeerRouter;

            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    if (instPort != null && instPort.portId() != null) {
                        String portId = instPort.portId();

                        terminatedInstPorts.remove(portId);
                        terminatedOsPorts.remove(portId);

                        Port port = osNetworkService.port(portId);

                        if (pendingInstPortIds.containsKey(portId) && port != null) {
                            setFloatingIpRules(pendingInstPortIds.get(portId),
                                    port, null, true);
                            pendingInstPortIds.remove(portId);
                        }
                    }

                    break;

                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    if (instPort != null && instPort.portId() != null) {
                        String portId = instPort.portId();
                        Port port = osNetworkService.port(portId);

                        if (port != null) {
                            terminatedInstPorts.put(portId, instPort);
                            terminatedOsPorts.put(portId, port);
                        }
                    }

                    break;

                case OPENSTACK_INSTANCE_MIGRATION_STARTED:

                    fip = associatedFloatingIp(event.subject(), ips);

                    if (fip == null) {
                        return;
                    }

                    osPort = osNetworkService.port(fip.getPortId());
                    osNet = osNetworkService.network(osPort.getNetworkId());
                    externalPeerRouter = externalPeerRouter(osNet);

                    if (externalPeerRouter == null) {
                        final String errorFormat = ERR_FLOW + "no external peer router found";
                        throw new IllegalStateException(errorFormat);
                    }

                    eventExecutor.execute(() -> {

                        // since downstream internal rules are located in all gateway
                        // nodes, therefore, we simply update the rules with new compute node info
                        setDownstreamInternalRules(fip, osNet, event.subject(), true);

                        // since DownstreamExternal rules should only be placed in
                        // corresponding gateway node, we need to install new rule to
                        // the corresponding gateway node
                        setDownstreamExternalRulesHelper(fip, osNet,
                                event.subject(), externalPeerRouter, gateways, true);

                        // since ComputeNodeToGateway rules should only be placed in
                        // corresponding compute node, we need to install new rule to
                        // the target compute node, and remove rules from original node
                        setComputeNodeToGatewayHelper(event.subject(), osNet, gateways, true);

                        migrationPool.put(fip.getFloatingIpAddress(), event.subject().deviceId());

                    });
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:

                    fip = associatedFloatingIp(event.subject(), ips);

                    if (fip == null) {
                        return;
                    }

                    osPort = osNetworkService.port(fip.getPortId());
                    osNet = osNetworkService.network(osPort.getNetworkId());
                    externalPeerRouter = externalPeerRouter(osNet);

                    if (externalPeerRouter == null) {
                        final String errorFormat = ERR_FLOW + "no external peer router found";
                        throw new IllegalStateException(errorFormat);
                    }

                    // If we only have one gateway, we simply do not remove any
                    // flow rules from either gateway or compute node
                    if (gateways.size() == 1) {
                        return;
                    }

                    // Checks whether the destination compute node's device id
                    // has identical gateway hash or not
                    // if it is true, we simply do not remove the rules, as
                    // it has been overwritten at port detention event
                    // if it is false, we will remove the rules
                    DeviceId newDeviceId = migrationPool.get(fip.getFloatingIpAddress());
                    DeviceId oldDeviceId = event.subject().deviceId();
                    migrationPool.remove(fip.getFloatingIpAddress());

                    OpenstackNode oldGateway = getGwByComputeDevId(gateways, oldDeviceId);
                    OpenstackNode newGateway = getGwByComputeDevId(gateways, newDeviceId);

                    if (oldGateway != null && oldGateway.equals(newGateway)) {
                        return;
                    }

                    eventExecutor.execute(() -> {

                        // We need to remove the old ComputeNodeToGateway rules from
                        // original compute node
                        setComputeNodeToGatewayHelper(event.subject(), osNet, gateways, false);

                        // Since DownstreamExternal rules should only be placed in
                        // corresponding gateway node, we need to remove old rule from
                        // the corresponding gateway node
                        setDownstreamExternalRulesHelper(fip, osNet,
                                event.subject(), externalPeerRouter, gateways, false);
                    });
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader);
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_PORT_REMOVED:
                    Port osPort = event.port();
                    if (terminatedInstPorts.containsKey(osPort.getId())) {
                        updateFipStore(terminatedInstPorts.get(osPort.getId()));
                        terminatedInstPorts.remove(osPort.getId());
                    }
                    break;
                default:
                    break;
            }
        }

        private void updateFipStore(InstancePort port) {

            if (port == null) {
                return;
            }

            Set<NetFloatingIP> ips = osRouterAdminService.floatingIps();
            for (NetFloatingIP fip : ips) {
                if (Strings.isNullOrEmpty(fip.getFixedIpAddress())) {
                    continue;
                }
                if (Strings.isNullOrEmpty(fip.getFloatingIpAddress())) {
                    continue;
                }
                if (fip.getFixedIpAddress().equals(port.ipAddress().toString())) {
                    NeutronFloatingIP neutronFip = (NeutronFloatingIP) fip;
                    // invalidate bound fixed IP and port
                    neutronFip.setFixedIpAddress(null);
                    neutronFip.setPortId(null);
                    tobeRemovedInstPorts.put(port.portId(), port);

                    // Following update will in turn trigger
                    // OPENSTACK_FLOATING_IP_DISASSOCIATED event
                    osRouterAdminService.updateFloatingIp(neutronFip);
                    log.info("Updated floating IP {}, due to host removal",
                            neutronFip.getFloatingIpAddress());
                }
            }
        }
    }
}
