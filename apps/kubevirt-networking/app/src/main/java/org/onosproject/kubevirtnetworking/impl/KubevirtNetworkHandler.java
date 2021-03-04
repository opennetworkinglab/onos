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
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
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

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.ICMP.CODE_ECHO_REQEUST;
import static org.onlab.packet.ICMP.TYPE_ECHO_REPLY;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_DHCP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FORWARDING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ARP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ICMP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_TO_TUNNEL_PREFIX;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_TO_TENANT_PREFIX;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.gatewayNodeForSpecifiedRouter;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getbrIntMacAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.segmentIdHex;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.NXM_NX_IP_TTL;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.NXM_OF_ICMP_TYPE;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveArpShaToThaExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveArpSpaToTpaExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveEthSrcToDstExtension;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildMoveIpSrcToDstExtension;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
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
    private static final long SLEEP_MS = 3000; // we wait 3s for init each node
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
    protected KubevirtNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterAdminService kubevirtRouterService;

    private final KubevirtNetworkListener networkListener = new InternalNetworkEventListener();
    private final KubevirtNodeListener nodeListener = new InternalNodeEventListener();

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
        networkService.addListener(networkListener);
        nodeService.addListener(nodeListener);
        leadershipService.runForLeadership(appId.name());

        kubevirtRouterService.addListener(kubevirtRouterlistener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkService.removeListener(networkListener);
        nodeService.removeListener(nodeListener);
        leadershipService.withdraw(appId.name());

        kubevirtRouterService.removeListener(kubevirtRouterlistener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void createBridge(KubevirtNode node, KubevirtNetwork network) {

        Device tunBridge = deviceService.getDevice(network.tenantDeviceId(node.hostname()));
        if (tunBridge != null) {
            log.warn("The tunnel bridge {} already exists at node {}",
                    network.tenantBridgeName(), node.hostname());
            setDefaultRules(node, network);
            return;
        }

        Device device = deviceService.getDevice(node.ovsdb());

        IpAddress serverIp = apiConfigService.apiConfig().ipAddress();
        ControllerInfo controlInfo =
                new ControllerInfo(serverIp, DEFAULT_OFPORT, DEFAULT_OF_PROTO);
        List<ControllerInfo> controllers = Lists.newArrayList(controlInfo);

        String dpid = network.tenantDeviceId(
                node.hostname()).toString().substring(DPID_BEGIN);

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(network.tenantBridgeName())
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(controllers);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void removeBridge(KubevirtNode node, KubevirtNetwork network) {
        Device device = deviceService.getDevice(node.ovsdb());

        BridgeName bridgeName = BridgeName.bridgeName(network.tenantBridgeName());

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.deleteBridge(bridgeName);
        deviceService.removeDevice(network.tenantDeviceId(node.hostname()));
    }

    private void createPatchInterface(KubevirtNode node, KubevirtNetwork network) {
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

        // tenant bridge -> tunnel bridge
        PatchDescription brTenantTunPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(network.tenantBridgeName())
                        .ifaceName(tenantToTunIntf)
                        .peer(tunToTenantIntf)
                        .build();

        ifaceConfig.addPatchMode(tenantToTunIntf, brTenantTunPatchDesc);

        // tunnel bridge -> tenant bridge
        PatchDescription brTunTenantPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(TUNNEL_BRIDGE)
                        .ifaceName(tunToTenantIntf)
                        .peer(tenantToTunIntf)
                        .build();
        ifaceConfig.addPatchMode(tunToTenantIntf, brTunTenantPatchDesc);
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

    private void setDefaultRules(KubevirtNode node, KubevirtNetwork network) {
        DeviceId deviceId = network.tenantDeviceId(node.hostname());

        while (!deviceService.isAvailable(deviceId)) {
            log.warn("Device {} is not ready for installing rules", deviceId);

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Failed to check device availability", e);
            }
        }

        flowService.connectTables(deviceId, TENANT_INBOUND_TABLE, TENANT_DHCP_TABLE);
        flowService.connectTables(deviceId, TENANT_DHCP_TABLE, TENANT_ARP_TABLE);
        flowService.connectTables(deviceId, TENANT_ARP_TABLE, TENANT_ICMP_TABLE);
        flowService.connectTables(deviceId, TENANT_ICMP_TABLE, TENANT_FORWARDING_TABLE);

        setDhcpRule(deviceId, true);
        setForwardingRule(deviceId, true);
        setGatewayArpRuleForInternalNetwork(network, TENANT_ARP_TABLE,
                network.tenantDeviceId(node.hostname()), true);
        setGatewayIcmpRuleForInternalNetwork(network, TENANT_ICMP_TABLE,
                network.tenantDeviceId(node.hostname()), true);

        log.info("Install default flow rules for tenant bridge {}", network.tenantBridgeName());
    }

    private void setDhcpRule(DeviceId deviceId, boolean install) {
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
        setGatewayArpRuleForInternalNetwork(network, PRE_FLAT_TABLE, electedGateway.intgBridge(), install);
        setGatewayIcmpRuleForInternalNetwork(network, PRE_FLAT_TABLE, electedGateway.intgBridge(), install);
    }


    private void setGatewayInterNetworkRouting(KubevirtNetwork network, KubevirtRouter router, boolean install) {
        router.internal().forEach(srcNetwork -> {
            if (srcNetwork.equals(network.networkId())) {
                return;
            }

        });
    }

    private void setGatewayArpRuleForInternalNetwork(KubevirtNetwork network,
                                                     int tableNum, DeviceId deviceId, boolean install) {
        Device device = deviceService.getDevice(deviceId);

        MacAddress brIntMacAddress = getbrIntMacAddress(deviceService, deviceId);

        if (brIntMacAddress == null) {
            log.error("Setting gateway arp rule for internal network because " +
                    "there's no br-int port for device {}", deviceId);
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
                .setArpSha(brIntMacAddress)
                .setArpSpa(Ip4Address.valueOf(network.gatewayIp().toString()))
                .setEthSrc(brIntMacAddress)
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
     * @param network kubevirt network
     * @param tableNum flow table number
     * @param deviceId device id of the selected gateway for the network
     * @param install install if true, remove otherwise
     */
    private void setGatewayIcmpRuleForInternalNetwork(KubevirtNetwork network,
                                              int tableNum, DeviceId deviceId, boolean install) {
        MacAddress brIntMacAddress = getbrIntMacAddress(deviceService, deviceId);

        if (brIntMacAddress == null) {
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
                .setEthSrc(brIntMacAddress)
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
                    initGatewayNodeForInternalNetwork(network, router, oldGatewayNode, true);
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

            nodeService.completeNodes().forEach(n -> {
                createBridge(n, network);
                createPatchInterface(n, network);
                setDefaultRules(n, network);
            });
        }

        private void purgeIntegrationTunnelBridge(KubevirtNetwork network) {
            if (network.segmentId() == null) {
                return;
            }

            nodeService.completeNodes().forEach(n -> {
                removePatchInterface(n, network);
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
                case KUBEVIRT_NODE_REMOVED:
                    eventExecutor.execute(() -> processNodeDeletion(event.subject()));
                    break;
                case KUBEVIRT_NODE_INCOMPLETE:
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
                            if (network.segmentId() == null) {
                                continue;
                            }
                            createBridge(node, network);
                            createPatchInterface(node, network);
                            setDefaultRules(node, network);
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
                for (KubevirtNetwork network : networkService.networks()) {
                    switch (network.type()) {
                        case FLAT:
                        case VLAN:
                            break;
                        case VXLAN:
                        case GRE:
                        case GENEVE:
                        default:
                            // do nothing
                            break;
                    }
                }
            }
        }

        private void processNodeDeletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            if (node.type().equals(GATEWAY)) {
                updateGatewayNodeForRouter();
                for (KubevirtNetwork network : networkService.networks()) {
                    switch (network.type()) {
                        case FLAT:
                        case VLAN:
                            break;
                        case VXLAN:
                        case GRE:
                        case GENEVE:
                        default:
                            // do nothing
                            break;
                    }
                }
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
}
