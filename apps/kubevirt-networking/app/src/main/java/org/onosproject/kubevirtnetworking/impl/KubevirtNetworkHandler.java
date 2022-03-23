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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultPatchDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.ICMP.CODE_ECHO_REQEUST;
import static org.onlab.packet.ICMP.TYPE_ECHO_REPLY;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_DEFAULT_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_DHCP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FORWARDING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_INTERNAL_ROUTING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_IP_EGRESS_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_IP_INGRESS_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_TUNNEL_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_EGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_INGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ARP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ICMP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_TO_TUNNEL_PREFIX;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_TO_TENANT_PREFIX;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.gatewayNodeForSpecifiedRouter;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterForKubevirtNetwork;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterForKubevirtPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterMacAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.portNumber;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.resolveHostname;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.segmentIdHex;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelToTenantPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.waitFor;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.NXM_NX_IP_TTL;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.NXM_OF_ICMP_TYPE;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveArpShaToThaExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveArpSpaToTpaExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveEthSrcToDstExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveIpSrcToDstExtension;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_TO_INTEGRATION;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles kubevirt network events.
 */
@Component(immediate = true)
public class KubevirtNetworkHandler {
    protected final Logger log = getLogger(getClass());
    private static final String DEFAULT_OF_PROTO = "tcp";
    private static final int DEFAULT_OFPORT = 6653;
    private static final int DPID_BEGIN = 3;
    private static final int DEFAULT_TTL = 0xff;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService apiConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkAdminService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterAdminService kubevirtRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService kubevirtPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService kubevirtNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

    private final KubevirtNetworkListener networkListener = new InternalNetworkEventListener();
    private final KubevirtNodeListener nodeListener = new InternalNodeEventListener();
    private final KubevirtPortListener portListener = new InternalKubevirtPortListener();

    private final InternalRouterEventListener kubevirtRouterlistener =
            new InternalRouterEventListener();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        networkService.addListener(networkListener);
        nodeService.addListener(nodeListener);
        kubevirtPortService.addListener(portListener);
        kubevirtRouterService.addListener(kubevirtRouterlistener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkService.removeListener(networkListener);
        nodeService.removeListener(nodeListener);
        kubevirtPortService.removeListener(portListener);
        kubevirtRouterService.removeListener(kubevirtRouterlistener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void createBridge(KubevirtNode node, KubevirtNetwork network) {

        Device tenantBridge = deviceService.getDevice(network.tenantDeviceId(node.hostname()));
        if (tenantBridge != null && deviceService.isAvailable(tenantBridge.id())) {
            log.warn("The tenant bridge {} already exists at node {}",
                    network.tenantBridgeName(), node.hostname());
            return;
        }

        Device device = deviceService.getDevice(node.ovsdb());

        IpAddress controllerIp = apiConfigService.apiConfig().controllerIp();
        String serviceFqdn = apiConfigService.apiConfig().serviceFqdn();
        IpAddress serviceIp = null;

        if (controllerIp == null) {
            if (serviceFqdn != null) {
                serviceIp = resolveHostname(serviceFqdn);
            }

            if (serviceIp != null) {
                controllerIp = serviceIp;
            } else {
                controllerIp = apiConfigService.apiConfig().ipAddress();
            }
        }

        ControllerInfo controlInfo =
                new ControllerInfo(controllerIp, DEFAULT_OFPORT, DEFAULT_OF_PROTO);
        List<ControllerInfo> controllers = Lists.newArrayList(controlInfo);

        String dpid = network.tenantDeviceId(
                node.hostname()).toString().substring(DPID_BEGIN);

        // if the bridge is already available, we skip creating a new bridge
        if (!deviceService.isAvailable(DeviceId.deviceId(dpid))) {
            BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                    .name(network.tenantBridgeName())
                    .failMode(BridgeDescription.FailMode.SECURE)
                    .datapathId(dpid)
                    .disableInBand()
                    .controllers(controllers);

            BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
            bridgeConfig.addBridge(builder.build());

            log.info("Created a new tunnel bridge for network {} at node {}",
                                        network.networkId(), node.hostname());

            waitFor(3);
        }
    }

    private void removeBridge(KubevirtNode node, KubevirtNetwork network) {
        Device device = deviceService.getDevice(node.ovsdb());

        BridgeName bridgeName = BridgeName.bridgeName(network.tenantBridgeName());

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.deleteBridge(bridgeName);
        deviceService.removeDevice(network.tenantDeviceId(node.hostname()));
    }

    private void createPatchTenantInterface(KubevirtNode node, KubevirtNetwork network) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", node.ovsdb());
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        String tenantToTunIntf =
                TENANT_TO_TUNNEL_PREFIX + segmentIdHex(network.segmentId());
        String tunToTenantIntf =
                TUNNEL_TO_TENANT_PREFIX + segmentIdHex(network.segmentId());

        if (!hasPort(network.tenantDeviceId(node.hostname()), tenantToTunIntf)) {
            // patch ports for tenant bridge -> tunnel bridge
            PatchDescription brTenantTunPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(network.tenantBridgeName())
                            .ifaceName(tenantToTunIntf)
                            .peer(tunToTenantIntf)
                            .build();

            ifaceConfig.addPatchMode(tenantToTunIntf, brTenantTunPatchDesc);

            waitFor(1);
        }

        if (!hasPort(node.tunBridge(), tunToTenantIntf)) {
            // tunnel bridge -> tenant bridge
            PatchDescription brTunTenantPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(TUNNEL_BRIDGE)
                            .ifaceName(tunToTenantIntf)
                            .peer(tenantToTunIntf)
                            .build();
            ifaceConfig.addPatchMode(tunToTenantIntf, brTunTenantPatchDesc);

            waitFor(1);
        }
    }

    private void removeAllFlows(KubevirtNode node, KubevirtNetwork network) {
        DeviceId deviceId = network.tenantDeviceId(node.hostname());
        flowService.purgeRules(deviceId);
    }

    private void removePatchInterface(KubevirtNode node, KubevirtNetwork network) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", node.ovsdb());
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        String tunToIntIntf = TUNNEL_TO_TENANT_PREFIX + segmentIdHex(network.segmentId());

        ifaceConfig.removePatchMode(tunToIntIntf);
    }

    private void setGatewayArpRulesForTenantNetwork(KubevirtNode node,
                                                    KubevirtNetwork network) {

        KubevirtRouter router = getRouterForKubevirtNetwork(kubevirtRouterService, network);
        if (router == null) {
            return;
        }

        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
        if (electedGw == null) {
            return;
        }

        setGatewayArpRuleForTenantInternalNetwork(router, network, TENANT_ARP_TABLE,
                electedGw.intgBridge(), network.tenantDeviceId(node.hostname()), true);
    }

    private void setGatewayIcmpRulesForTenantNetwork(KubevirtNode node,
                                                     KubevirtNetwork network) {
        KubevirtRouter router = getRouterForKubevirtNetwork(kubevirtRouterService, network);
        if (router == null) {
            return;
        }

        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
        if (electedGw == null) {
            return;
        }

        setGatewayIcmpRuleForTenantInternalNetwork(router, network, TENANT_ICMP_TABLE,
                electedGw.intgBridge(), network.tenantDeviceId(node.hostname()), true);
    }

    private void setGatewayRuleToWorkerNodeWhenNodeCreated(KubevirtNode node,
                                                           KubevirtNetwork network) {
        KubevirtRouter router = getRouterForKubevirtNetwork(kubevirtRouterService, network);
        if (router == null) {
            return;
        }

        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
        if (electedGw == null) {
            return;
        }

        setDefaultGatewayRuleToWorkerNodeTunBridge(router, network,
                electedGw.intgBridge(), node, true);
    }

    private void setDefaultRulesForTenantNetwork(KubevirtNode node,
                                                 KubevirtNetwork network) {
        DeviceId deviceId = network.tenantDeviceId(node.hostname());

        while (!deviceService.isAvailable(deviceId)) {
            log.warn("Device {} is not ready for installing rules", deviceId);
            waitFor(3);
        }

        flowService.connectTables(deviceId, TENANT_INBOUND_TABLE, TENANT_DHCP_TABLE);
        flowService.connectTables(deviceId, TENANT_DHCP_TABLE, TENANT_ARP_TABLE);
        flowService.connectTables(deviceId, TENANT_ARP_TABLE, TENANT_ICMP_TABLE);
        flowService.connectTables(deviceId, TENANT_ICMP_TABLE, TENANT_FORWARDING_TABLE);

        setArpRuleForTenantNetwork(deviceId, true);
        setDhcpRuleForTenantNetwork(deviceId, true);
        setForwardingRule(deviceId, true);

        // security group related rules
        setTenantIngressTransitionRule(network, network.tenantDeviceId(node.hostname()), true);
        setTenantEgressTransitionRule(network.tenantDeviceId(node.hostname()), true);

        log.info("Install default flow rules for tenant bridge {}", network.tenantBridgeName());
    }

    private void setDhcpRuleForTenantNetwork(DeviceId deviceId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        flowService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_DHCP_RULE,
                TENANT_DHCP_TABLE,
                install);
    }

    private void setForwardingRule(DeviceId deviceId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.NORMAL)
                .build();

        flowService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_FORWARDING_RULE,
                TENANT_FORWARDING_TABLE,
                install);
    }

    private void initGatewayNodeForInternalNetwork(KubevirtNetwork network,
                                                   KubevirtRouter router,
                                                   KubevirtNode electedGateway,
                                                   boolean install) {
        switch (network.type()) {
            case VXLAN:
            case GRE:
            case GENEVE:
            case STT:
                setDefaultEgressRuleToGatewayNode(router, network,
                        electedGateway.intgBridge(), install);
                kubevirtNodeService.completeNodes(WORKER).forEach(node -> {
                    setGatewayArpRuleForTenantInternalNetwork(router, network,
                            TENANT_ARP_TABLE, electedGateway.intgBridge(),
                            network.tenantDeviceId(node.hostname()), install);
                    setGatewayIcmpRuleForTenantInternalNetwork(router, network,
                            TENANT_ICMP_TABLE, electedGateway.intgBridge(),
                            network.tenantDeviceId(node.hostname()), install);
                    setDefaultGatewayRuleToWorkerNodeTunBridge(router, network,
                            electedGateway.intgBridge(), node, install);
                });
                setGatewayProviderInterNetworkRoutingWithinSameRouter(network, router, electedGateway, install);
                break;
            case FLAT:
            case VLAN:
                setGatewayArpRuleForProviderInternalNetwork(router, network,
                        GW_ENTRY_TABLE, electedGateway.intgBridge(), install);
                setGatewayIcmpRuleForProviderInternalNetwork(router, network,
                        GW_ENTRY_TABLE, electedGateway.intgBridge(), install);
                setGatewayProviderInterNetworkRoutingWithinSameRouter(network,
                        router, electedGateway, install);
                break;
            default:
                // do nothing
                break;
        }
    }

    private void setDefaultGatewayRuleToWorkerNodeTunBridge(KubevirtRouter router,
                                                            KubevirtNetwork network,
                                                            DeviceId gwDeviceId,
                                                            KubevirtNode workerNode,
                                                            boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Setting gateway default egress rule to gateway for tenant " +
                "internal network because there's no br-int port for device {}", gwDeviceId);
            return;
        }

        KubevirtNode gwNode = kubevirtNodeService.node(gwDeviceId);

        if (gwNode == null) {
            log.warn("Setting gateway default egress rule to gateway for tenant " +
                "internal network because there's no gateway node for device {}", gwDeviceId);
            return;
        }

        PortNumber patchPortNumber = tunnelToTenantPort(deviceService, workerNode, network);
        if (patchPortNumber == null) {
            return;
        }

        PortNumber tunnelPortNumber = tunnelPort(workerNode, network);
        if (tunnelPortNumber == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchInPort(patchPortNumber)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst((routerMacAddress));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(network.segmentId()))
                .extension(buildExtension(
                        deviceService,
                        workerNode.tunBridge(),
                        gwNode.dataIp().getIp4Address()),
                        workerNode.tunBridge())
                .setOutput(tunnelPortNumber);

        flowService.setRule(
                appId,
                workerNode.tunBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_FORWARDING_RULE,
                TUNNEL_DEFAULT_TABLE,
                install);
    }

    private void setTenantIngressTransitionRule(KubevirtNetwork network,
                                                DeviceId deviceId, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                .matchInPort(network.tenantToTunnelPort(deviceId));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.transition(TENANT_ACL_INGRESS_TABLE);

        flowService.setRule(appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_IP_INGRESS_RULE,
                TENANT_ICMP_TABLE,
                install
        );
    }

    private void setTenantEgressTransitionRule(DeviceId deviceId, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(EthType.EtherType.IPV4.ethType().toShort());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.transition(TENANT_ACL_EGRESS_TABLE);

        flowService.setRule(appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_IP_EGRESS_RULE,
                TENANT_ICMP_TABLE,
                install
        );
    }

    private void setDefaultEgressRuleToGatewayNode(KubevirtRouter router,
                                                   KubevirtNetwork network,
                                                   DeviceId gwDeviceId,
                                                   boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Setting gateway default eggress rule to gateway for tenant internal network because " +
                    "there's no br-int port for device {}", gwDeviceId);
            return;
        }

        KubevirtNode gwNode = kubevirtNodeService.node(gwDeviceId);

        if (gwNode == null) {
            log.warn("Setting gateway default eggress rule to gateway for tenant internal network because " +
                    "there's no gateway node for device {}", gwDeviceId);
            return;
        }

        PortNumber tunToIntPortNum = portNumber(deviceService, gwNode.tunBridge(), TUNNEL_TO_INTEGRATION);

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.parseLong(network.segmentId()));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setOutput(tunToIntPortNum);

        flowService.setRule(
                appId,
                gwNode.tunBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_TUNNEL_RULE,
                TUNNEL_DEFAULT_TABLE,
                install);
    }


    private void setGatewayIcmpRuleForTenantInternalNetwork(KubevirtRouter router,
                                                            KubevirtNetwork network,
                                                            int tableNum,
                                                            DeviceId gwDeviceId,
                                                            DeviceId tenantDeviceId,
                                                            boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Setting gateway ICMP rule for internal network because " +
                    "there's no br-int port for device {}", gwDeviceId);
            return;
        }

        Device device = deviceService.getDevice(tenantDeviceId);

        if (device == null) {
            log.warn("Setting gateway icmp rule for internal network because " +
                            "there's no tenant device for {} to install gateway arp rule",
                    tenantDeviceId);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(TYPE_ECHO_REQUEST)
                .matchIcmpCode(CODE_ECHO_REQEUST)
                .matchIPDst(IpPrefix.valueOf(network.gatewayIp(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveIpSrcToDstExtension(device), device.id())
                .extension(buildLoadExtension(device,
                        NXM_NX_IP_TTL, DEFAULT_TTL), device.id())
                .extension(buildLoadExtension(device,
                        NXM_OF_ICMP_TYPE, TYPE_ECHO_REPLY), device.id())
                .setIpSrc(network.gatewayIp())
                .setEthSrc(routerMacAddress)
                .setOutput(PortNumber.IN_PORT);

        flowService.setRule(
                appId,
                tenantDeviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ICMP_RULE,
                tableNum,
                install);
    }

    private void setArpRuleForTenantNetwork(DeviceId tenantDeviceId,
                                            boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .transition(TENANT_FORWARDING_TABLE);

        flowService.setRule(
                appId,
                tenantDeviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ARP_DEFAULT_RULE,
                TENANT_ARP_TABLE,
                install
        );
    }

    private void setGatewayArpRuleForTenantInternalNetwork(KubevirtRouter router,
                                                           KubevirtNetwork network,
                                                           int tableNum,
                                                           DeviceId gwDeviceId,
                                                           DeviceId tenantDeviceId,
                                                           boolean install) {

        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Setting gateway arp rule for internal network because " +
                    "there's no br-int port for device {}", gwDeviceId);
            return;
        }

        Device device = deviceService.getDevice(tenantDeviceId);

        if (device == null) {
            log.warn("Setting gateway arp rule for internal network because " +
                    "there's no tenant device for {} to install gateway arp rule",
                    tenantDeviceId);
            return;
        }


        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(Ip4Address.valueOf(network.gatewayIp().toString()));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveArpShaToThaExtension(device), device.id())
                .extension(buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpOp(ARP.OP_REPLY)
                .setArpSha(routerMacAddress)
                .setArpSpa(Ip4Address.valueOf(network.gatewayIp().toString()))
                .setEthSrc(routerMacAddress)
                .setOutput(PortNumber.IN_PORT);

        flowService.setRule(
                appId,
                device.id(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ARP_GATEWAY_RULE,
                tableNum,
                install
        );
    }

    private void setGatewayProviderInterNetworkRoutingWithinSameRouter(
            KubevirtNetwork network, KubevirtRouter router, KubevirtNode gatewayNode, boolean install) {
        router.internal().forEach(srcNetwork -> {
            if (srcNetwork.equals(network.networkId())
                    || kubevirtNetworkService.network(srcNetwork) == null) {
                return;
            }

            kubevirtPortService.ports(network.networkId()).forEach(port -> {
                setGatewayInterNetworkRoutingFromNetworkToPort(router, kubevirtNetworkService.network(srcNetwork),
                        port, gatewayNode, install);
            });
        });
    }

    private void setGatewayInterNetworkRoutingFromNetworkToPort(KubevirtRouter router,
                                                                KubevirtNetwork srcNetwork,
                                                                KubevirtPort dstPort,
                                                                KubevirtNode gatewayNode,
                                                                boolean install) {
        Device gwDevice = deviceService.getDevice(gatewayNode.intgBridge());

        if (gwDevice == null) {
            log.warn("Failed to set internal network routing rule because " +
                    "there's no device Id for device {}", gatewayNode.intgBridge());
            return;
        }

        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Failed to set internal network routing rule because " +
                    "there's no br-int port for device {}", gatewayNode.intgBridge());
            return;
        }

        TrafficSelector.Builder sBuilder;
        TrafficTreatment treatment;

        if (srcNetwork.type() == FLAT || srcNetwork.type() == VLAN) {
            sBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchEthDst(routerMacAddress)
                    .matchIPSrc(IpPrefix.valueOf(srcNetwork.cidr()))
                    .matchIPDst(IpPrefix.valueOf(dstPort.ipAddress(), 32));

            treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(routerMacAddress)
                    .setEthDst(dstPort.macAddress())
                    .transition(FORWARDING_TABLE)
                    .build();

            flowService.setRule(
                    appId,
                    gwDevice.id(),
                    sBuilder.build(),
                    treatment,
                    PRIORITY_INTERNAL_ROUTING_RULE,
                    GW_ENTRY_TABLE,
                    install);
        } else {
            KubevirtNetwork dstNetwork = kubevirtNetworkService.network(dstPort.networkId());
            if (dstNetwork == null) {
                return;
            }

            KubevirtNode dstPortWorkerNode = kubevirtNodeService.node(dstPort.deviceId());
            if (dstPortWorkerNode == null) {
                return;
            }

            sBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchEthDst(routerMacAddress)
                    .matchTunnelId(Long.parseLong(srcNetwork.segmentId()))
                    .matchIPSrc(IpPrefix.valueOf(srcNetwork.cidr()))
                    .matchIPDst(IpPrefix.valueOf(dstPort.ipAddress(), 32));

            treatment = DefaultTrafficTreatment.builder()
                    .setTunnelId(Long.parseLong(dstNetwork.segmentId()))
                    .setEthSrc(routerMacAddress)
                    .setEthDst(dstPort.macAddress())
                    .extension(buildExtension(
                            deviceService,
                            gatewayNode.tunBridge(),
                            dstPortWorkerNode.dataIp().getIp4Address()),
                            gatewayNode.tunBridge())
                    .setOutput(PortNumber.IN_PORT)
                    .build();

            flowService.setRule(
                    appId,
                    gatewayNode.tunBridge(),
                    sBuilder.build(),
                    treatment,
                    PRIORITY_INTERNAL_ROUTING_RULE,
                    TUNNEL_DEFAULT_TABLE,
                    install);
        }
    }

    private void setGatewayArpRuleForProviderInternalNetwork(KubevirtRouter router, KubevirtNetwork network,
                                                             int tableNum, DeviceId gwDeviceId, boolean install) {


        Device device = deviceService.getDevice(gwDeviceId);
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Setting gateway arp rule for internal network because " +
                    "there's no br-int port for device {}", gwDeviceId);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(Ip4Address.valueOf(network.gatewayIp().toString()));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveArpShaToThaExtension(device), device.id())
                .extension(buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpOp(ARP.OP_REPLY)
                .setArpSha(routerMacAddress)
                .setArpSpa(Ip4Address.valueOf(network.gatewayIp().toString()))
                .setEthSrc(routerMacAddress)
                .setOutput(PortNumber.IN_PORT);

        flowService.setRule(
                appId,
                device.id(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ARP_GATEWAY_RULE,
                tableNum,
                install
        );
    }

    /**
     * Sends ICMP echo reply for the ICMP echo request from the kubevirt VM.
     *
     * @param router kubevirt router
     * @param network kubevirt network
     * @param tableNum flow table number
     * @param deviceId device id of the selected gateway for the network
     * @param install install if true, remove otherwise
     */
    private void setGatewayIcmpRuleForProviderInternalNetwork(KubevirtRouter router, KubevirtNetwork network,
                                                              int tableNum, DeviceId deviceId, boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.error("Setting gateway ICMP rule for internal network because " +
                    "there's no br-int port for device {}", deviceId);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(TYPE_ECHO_REQUEST)
                .matchIcmpCode(CODE_ECHO_REQEUST)
                .matchIPDst(IpPrefix.valueOf(network.gatewayIp(), 32));

        Device device = deviceService.getDevice(deviceId);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveIpSrcToDstExtension(device), device.id())
                .extension(buildLoadExtension(device,
                        NXM_NX_IP_TTL, DEFAULT_TTL), device.id())
                .extension(buildLoadExtension(device,
                        NXM_OF_ICMP_TYPE, TYPE_ECHO_REPLY), device.id())
                .setIpSrc(network.gatewayIp())
                .setEthSrc(routerMacAddress)
                .setOutput(PortNumber.IN_PORT);

        flowService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ICMP_RULE,
                tableNum,
                install);
    }

    private boolean hasPort(DeviceId deviceId, String portName) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
        log.info("The port {} already existed on device {}", portName, deviceId);

        return port != null;
    }

    private class InternalRouterEventListener implements KubevirtRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtRouterEvent event) {
            switch (event.type()) {
                case KUBEVIRT_ROUTER_CREATED:
                    eventExecutor.execute(() -> processRouterCreation(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_REMOVED:
                    eventExecutor.execute(() -> processRouterDeletion(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_UPDATED:
                    eventExecutor.execute(() -> processRouterUpdate(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED:
                    eventExecutor.execute(() -> processRouterInternalNetworksAttached(event.subject(),
                            event.internal()));
                    break;
                case KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED:
                    eventExecutor.execute(() -> processRouterInternalNetworksDetached(event.subject(),
                            event.internal()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_ATTACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeAttached(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_DETACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeDetached(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_CHANGED:
                    eventExecutor.execute(() -> processRouterGatewayNodeChanged(event.subject(),
                            event.gateway()));
                    break;

                default:
                    //do nothing
                    break;
            }
        }

        private void processRouterCreation(KubevirtRouter router) {
            // When a router is created, we performs the election process to associate the router
            // to the specific gateway. After the election, KubevirtNetwork handler installs bunch of rules
            // to elected gateway node so that VMs associated to the router can ping to their gateway IP.
            // SNAT and floating ip rule setup is out of this handler's scope and would be done with the other handlers
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
            if (electedGw == null) {
                return;
            }

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, electedGw, true);
                }
            });
            kubevirtRouterService.updateRouter(router.updatedElectedGateway(electedGw.hostname()));
        }

        private void processRouterDeletion(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
            if (electedGw == null) {
                return;
            }

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, electedGw, false);
                }
            });
        }

        private void processRouterUpdate(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.electedGateway() == null) {
                return;
            }

            KubevirtNode electedGw = nodeService.node(router.electedGateway());

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, electedGw, true);
                }
            });
        }

        private void processRouterInternalNetworksAttached(KubevirtRouter router,
                                                           Set<String> attachedInternalNetworks) {
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
            if (electedGw == null) {
                return;
            }

            attachedInternalNetworks.forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, electedGw, true);
                }
            });
        }

        private void processRouterInternalNetworksDetached(KubevirtRouter router,
                                                           Set<String> detachedInternalNetworks) {
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(nodeService, router);
            if (electedGw == null) {
                return;
            }

            detachedInternalNetworks.forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, electedGw, false);
                }

                removeDetachedInternalNetworkRules(network, router, electedGw);
            });
        }

        private void removeDetachedInternalNetworkRules(KubevirtNetwork removedNetwork,
                                                        KubevirtRouter router,
                                                        KubevirtNode electedGw) {
            router.internal().stream().filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                    .forEach(networkId -> {
                        kubevirtPortService.ports(networkId).forEach(kubevirtPort -> {
                            setGatewayInterNetworkRoutingFromNetworkToPort(
                                    router, removedNetwork, kubevirtPort, electedGw, false);
                        });
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

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, gatewayNode, true);
                }
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

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, gatewayNode, false);
                }
            });
        }

        private void processRouterGatewayNodeChanged(KubevirtRouter router,
                                                     String disAssociatedGateway) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode oldGatewayNode = nodeService.node(disAssociatedGateway);
            if (oldGatewayNode == null) {
                return;
            }

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, oldGatewayNode, false);
                }
            });

            KubevirtNode newGatewayNode = nodeService.node(router.electedGateway());
            if (newGatewayNode == null) {
                return;
            }

            router.internal().forEach(networkName -> {
                KubevirtNetwork network = networkService.network(networkName);

                if (network != null) {
                    initGatewayNodeForInternalNetwork(network, router, newGatewayNode, true);
                }
            });
        }
    }

    private class InternalNetworkEventListener implements KubevirtNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNetworkEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NETWORK_CREATED:
                    eventExecutor.execute(() -> processNetworkCreation(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_UPDATED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkCreation(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            switch (network.type()) {
                case VXLAN:
                case GRE:
                case GENEVE:
                case STT:
                    initIntegrationTunnelBridge(network);
                    break;
                case FLAT:
                case VLAN:
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkRemoval(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            switch (network.type()) {
                case VXLAN:
                case GRE:
                case GENEVE:
                case STT:
                    purgeIntegrationTunnelBridge(network);
                    break;
                case FLAT:
                case VLAN:
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void initIntegrationTunnelBridge(KubevirtNetwork network) {
            if (network.segmentId() == null) {
                return;
            }

            nodeService.completeNodes(WORKER).forEach(n -> {
                createBridge(n, network);
                createPatchTenantInterface(n, network);
                setDefaultRulesForTenantNetwork(n, network);
            });
        }

        private void purgeIntegrationTunnelBridge(KubevirtNetwork network) {
            if (network.segmentId() == null) {
                return;
            }

            nodeService.completeNodes(WORKER).forEach(n -> {
                removeAllFlows(n, network);
                removePatchInterface(n, network);

                waitFor(5);

                removeBridge(n, network);
            });
        }
    }

    private class InternalNodeEventListener implements KubevirtNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case KUBEVIRT_NODE_INCOMPLETE:
                case KUBEVIRT_NODE_REMOVED:
                    eventExecutor.execute(() -> processNodeDeletion(event.subject()));
                    break;
                case KUBEVIRT_NODE_UPDATED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCompletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            if (node.type().equals(WORKER)) {
                for (KubevirtNetwork network : networkService.networks()) {
                    switch (network.type()) {
                        case VXLAN:
                        case GRE:
                        case GENEVE:
                        case STT:
                            if (network.segmentId() == null) {
                                continue;
                            }
                            createBridge(node, network);
                            createPatchTenantInterface(node, network);
                            setDefaultRulesForTenantNetwork(node, network);
                            setGatewayArpRulesForTenantNetwork(node, network);
                            setGatewayIcmpRulesForTenantNetwork(node, network);
                            setGatewayRuleToWorkerNodeWhenNodeCreated(node, network);
                            break;
                        case FLAT:
                        case VLAN:
                        default:
                            // do nothing
                            break;
                    }
                }
            } else if (node.type().equals(GATEWAY)) {
                updateGatewayNodeForRouter();
            }
        }

        private void processNodeDeletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }
            if (node.type().equals(GATEWAY)) {
                kubevirtRouterService.routers()
                        .stream()
                        .filter(router -> router.electedGateway().equals(node.hostname()))
                        .forEach(router -> {
                            router.internal().forEach(networkName -> {
                                KubevirtNetwork network = networkService.network(networkName);

                                if (network != null) {
                                    initGatewayNodeForInternalNetwork(network, router, node, false);
                                }
                            });
                        });
                updateGatewayNodeForRouter();
            }
        }

        private void updateGatewayNodeForRouter() {
            kubevirtRouterService.routers().forEach(router -> {
                KubevirtNode newGwNode = gatewayNodeForSpecifiedRouter(nodeService, router);

                if (newGwNode == null) {
                    return;
                }
                kubevirtRouterService.updateRouter(router.updatedElectedGateway(newGwNode.hostname()));
            });
        }
    }

    private class InternalKubevirtPortListener implements KubevirtPortListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtPortEvent event) {
            switch (event.type()) {
                case KUBEVIRT_PORT_CREATED:
                    eventExecutor.execute(() -> processPortCreation(event.subject()));
                    break;
                case KUBEVIRT_PORT_UPDATED:
                    eventExecutor.execute(() -> processPortUpdate(event.subject()));
                    break;
                case KUBEVIRT_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortDeletion(event.subject()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processPortCreation(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {

                router.internal().forEach(srcNetwork -> {
                    if (srcNetwork.equals(kubevirtPort.networkId())
                            || kubevirtNetworkService.network(srcNetwork) == null) {
                        return;
                    }
                    setGatewayInterNetworkRoutingFromNetworkToPort(router,
                            kubevirtNetworkService.network(srcNetwork),
                            kubevirtPort, gwNode, true);
                });
            }
        }

        private void processPortUpdate(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {

                router.internal().forEach(srcNetwork -> {
                    if (srcNetwork.equals(kubevirtPort.networkId())
                            || kubevirtNetworkService.network(srcNetwork) == null) {
                        return;
                    }
                    setGatewayInterNetworkRoutingFromNetworkToPort(router,
                            kubevirtNetworkService.network(srcNetwork),
                            kubevirtPort, gwNode, true);
                });
            }
        }

        private void processPortDeletion(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {

                router.internal().forEach(srcNetwork -> {
                    if (srcNetwork.equals(kubevirtPort.networkId())
                            || kubevirtNetworkService.network(srcNetwork) == null) {
                        return;
                    }
                    setGatewayInterNetworkRoutingFromNetworkToPort(router,
                            kubevirtNetworkService.network(srcNetwork),
                            kubevirtPort, gwNode, false);
                });
            }
        }
    }
}
