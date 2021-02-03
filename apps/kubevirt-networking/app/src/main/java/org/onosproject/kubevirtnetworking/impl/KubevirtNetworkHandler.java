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
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
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
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_DHCP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.segmentIdHex;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
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

    public static final String INTEGRATION_TO_TUNNEL_PREFIX = "i-to-t-";
    public static final String TUNNEL_TO_INTEGRATION_PREFIX = "t-to-i-";

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

    private final KubevirtNetworkListener networkListener = new InternalNetworkEventListener();
    private final KubevirtNodeListener nodeListener = new InternalNodeEventListener();

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

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkService.removeListener(networkListener);
        nodeService.removeListener(nodeListener);
        leadershipService.withdraw(appId.name());
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

        String intToTunIntf =
                INTEGRATION_TO_TUNNEL_PREFIX + segmentIdHex(network.segmentId());
        String tunToIntIntf =
                TUNNEL_TO_INTEGRATION_PREFIX + segmentIdHex(network.segmentId());

        // integration bridge -> tunnel bridge
        PatchDescription brIntTunPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(network.tenantBridgeName())
                        .ifaceName(intToTunIntf)
                        .peer(tunToIntIntf)
                        .build();

        ifaceConfig.addPatchMode(intToTunIntf, brIntTunPatchDesc);

        // tunnel bridge -> integration bridge
        PatchDescription brTunIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(TUNNEL_BRIDGE)
                        .ifaceName(tunToIntIntf)
                        .peer(intToTunIntf)
                        .build();
        ifaceConfig.addPatchMode(tunToIntIntf, brTunIntPatchDesc);
    }

    private void removePatchInterface(KubevirtNode node, KubevirtNetwork network) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", node.ovsdb());
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        String tunToIntIntf = TUNNEL_TO_INTEGRATION_PREFIX + segmentIdHex(network.segmentId());

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
        flowService.connectTables(deviceId, TENANT_DHCP_TABLE, TENANT_FORWARDING_TABLE);

        setDhcpRule(deviceId, true);
        setForwardingRule(deviceId, true);

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

    public void setForwardingRule(DeviceId deviceId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.NORMAL)
                .build();

        flowService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                TENANT_FORWARDING_TABLE,
                install);
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
                    default:
                        // do nothing
                        break;
                }
            }
        }
    }
}
