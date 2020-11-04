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

import org.apache.commons.net.util.SubnetUtils;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sServiceService;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.IpAddress.Version.INET;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.k8snetworking.api.Constants.ARP_PROXY_MODE;
import static org.onosproject.k8snetworking.api.Constants.ARP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.NODE_FAKE_IP_STR;
import static org.onosproject.k8snetworking.api.Constants.NODE_FAKE_MAC_STR;
import static org.onosproject.k8snetworking.api.Constants.NODE_IP_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_ARP_CONTROL_RULE;
import static org.onosproject.k8snetworking.api.Constants.SERVICE_FAKE_MAC_STR;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_PREFIX;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.ARP_MODE;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.ARP_MODE_DEFAULT;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.GATEWAY_MAC;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.GATEWAY_MAC_DEFAULT;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.allK8sDevices;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getGatewayIp;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getPropertyValue;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.unshiftIpDomain;

/**
 * Handles ARP packet from containers.
 */
@Component(
        immediate = true,
        property = {
                GATEWAY_MAC + "=" + GATEWAY_MAC_DEFAULT,
                ARP_MODE + "=" + ARP_MODE_DEFAULT
        }
)
public class K8sSwitchingArpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String GATEWAY_MAC = "gatewayMac";
    private static final String ARP_MODE = "arpMode";

    private static final KryoNamespace SERIALIZER_HOST_MAC = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeAdminService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostService k8sHostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sServiceService k8sServiceService;

    /** Fake MAC address for virtual network subnet gateway. */
    private String gatewayMac = GATEWAY_MAC_DEFAULT;

    /** ARP processing mode, broadcast | proxy (default). */
    protected String arpMode = ARP_MODE_DEFAULT;

    private ConsistentMap<IpAddress, MacAddress> extHostMacStore;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalNodeEventListener k8sNodeListener = new InternalNodeEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sNodeService.addListener(k8sNodeListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        extHostMacStore = storageService.<IpAddress, MacAddress>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_HOST_MAC))
                .withName("k8s-host-mac-store")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        packetService.removeProcessor(packetProcessor);
        k8sNodeService.removeListener(k8sNodeListener);
        leadershipService.withdraw(appId.name());
        configService.unregisterProperties(getClass(), false);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    /**
     * Processes ARP request packets.
     *
     * @param context   packet context
     * @param ethPacket ethernet packet
     */
    private void processPacketIn(PacketContext context, Ethernet ethPacket) {
        // if the ARP mode is configured as broadcast mode, we simply ignore ARP packet_in
        if (ARP_BROADCAST_MODE.equals(getArpMode())) {
            return;
        }

        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();

        if (!allK8sDevices(k8sNodeService, k8sHostService).contains(deviceId)) {
            return;
        }

        ARP arpPacket = (ARP) ethPacket.getPayload();
        if (arpPacket.getOpCode() == ARP.OP_REQUEST) {
            processArpRequest(context, ethPacket);
        } else if (arpPacket.getOpCode() == ARP.OP_REPLY) {
            processArpReply(context, ethPacket);
        }
    }

    private void processArpRequest(PacketContext context, Ethernet ethPacket) {
        ARP arpPacket = (ARP) ethPacket.getPayload();
        K8sPort srcK8sPort = k8sNetworkService.ports().stream()
                .filter(p -> p.macAddress().equals(ethPacket.getSourceMAC()))
                .findAny().orElse(null);

        PortNumber srcPortNum = context.inPacket().receivedFrom().port();
        DeviceId srcDeviceId = context.inPacket().receivedFrom().deviceId();
        boolean isEntryPort = false;

        for (K8sNode node : k8sNodeService.completeNodes()) {
            if (srcDeviceId.equals(node.intgBridge()) &&
                    srcPortNum.equals(node.intgEntryPortNum())) {
                isEntryPort = true;
            }
        }

        // if the ARP request is not initiated from regular k8s ports nor
        // integration bridge entry port, we simply ignore the ARP request...
        if (srcK8sPort == null && !isEntryPort) {
            log.warn("Failed to find source port(MAC:{})", ethPacket.getSourceMAC());
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

        // look up the MAC address from regular k8s ports
        MacAddress replyMac = k8sNetworkService.ports().stream()
                //        .filter(p -> p.networkId().equals(srcPort.networkId()))
                .filter(p -> p.ipAddress().equals(targetIp))
                .map(K8sPort::macAddress)
                .findAny().orElse(null);

        // look up the MAC address from special integration entry port (e.g., LOCAL, k8s-int-os)
        for (K8sNetwork network : k8sNetworkService.networks()) {
            if (network.gatewayIp().equals(targetIp)) {
                K8sNode node = k8sNodeService.node(network.name());
                replyMac = node.intgEntryPortMac();
            }
        }

        if (replyMac == null) {
            String cidr = k8sNetworkService.networks().stream()
                    .map(K8sNetwork::cidr).findAny().orElse(null);

            if (cidr != null) {
                String unshiftedIp = unshiftIpDomain(targetIp.toString(),
                        SHIFTED_IP_PREFIX, cidr);

                replyMac = k8sNetworkService.ports().stream()
                        .filter(p -> p.ipAddress().equals(IpAddress.valueOf(unshiftedIp)))
                        .map(K8sPort::macAddress)
                        .findAny().orElse(null);
            }
        }

        if (replyMac == null) {
            Set<String> serviceIps = k8sServiceService.services().stream()
                    .map(s -> s.getSpec().getClusterIP())
                    .collect(Collectors.toSet());
            if (serviceIps.contains(targetIp.toString())) {
                replyMac = MacAddress.valueOf(SERVICE_FAKE_MAC_STR);
            }
        }

        if (replyMac == null) {

            if (targetIp.toString().startsWith(NODE_IP_PREFIX)) {
                String targetIpPrefix = targetIp.toString().split("\\.")[1];
                String nodePrefix = NODE_IP_PREFIX + "." + targetIpPrefix;

                String origNodeCidr = k8sNodeService.completeNodes().stream()
                        .map(n -> n.nodeIp().toString()).findAny().orElse(null);

                if (origNodeCidr != null) {
                    String origNodeIp = unshiftIpDomain(targetIp.toString(),
                            nodePrefix, origNodeCidr);
                    IpPrefix k8sNodeIpCidr = IpPrefix.valueOf(IpAddress.valueOf(origNodeCidr), 24);
                    SubnetUtils k8sNodeSubnet = new SubnetUtils(k8sNodeIpCidr.toString());
                    String k8sNodeGateway = getGatewayIp(k8sNodeIpCidr.toString()).toString();
                    String seekIp = "";

                    if (!k8sNodeSubnet.getInfo().isInRange(origNodeIp)) {
                        replyMac = extHostMacStore.asJavaMap().get(IpAddress.valueOf(k8sNodeGateway));
                        seekIp = k8sNodeGateway;
                    } else {
                        replyMac = extHostMacStore.asJavaMap().get(IpAddress.valueOf(origNodeIp));
                        seekIp = origNodeIp;
                    }

                    // if the source hosts are not in k8s cluster range,
                    // we need to manually learn their MAC addresses
                    if (replyMac == null) {
                        ConnectPoint cp = context.inPacket().receivedFrom();
                        K8sNode k8sNode = k8sNodeService.node(cp.deviceId());

                        if (k8sNode != null) {
                            // we use fake IP and MAC address as a source to
                            // query destination MAC address
                            setArpRequest(MacAddress.valueOf(NODE_FAKE_MAC_STR).toBytes(),
                                    IpAddress.valueOf(NODE_FAKE_IP_STR).toOctets(),
                                    IpAddress.valueOf(seekIp).toOctets(),
                                    k8sNode);
                            context.block();
                            return;
                        }
                    }
                }
            }
        }

        if (replyMac == null) {
            replyMac = MacAddress.valueOf(gatewayMac);
        }

        Ethernet ethReply = ARP.buildArpReply(
                targetIp.getIp4Address(),
                replyMac,
                ethPacket);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));

        context.block();
    }

    private void processArpReply(PacketContext context, Ethernet ethPacket) {
        ARP arpPacket = (ARP) ethPacket.getPayload();

        IpAddress srcIp = IpAddress.valueOf(INET, arpPacket.getSenderProtocolAddress());
        MacAddress srcMac = MacAddress.valueOf(arpPacket.getSenderHardwareAddress());
        IpAddress dstIp = IpAddress.valueOf(INET, arpPacket.getTargetProtocolAddress());

        if (dstIp.equals(IpAddress.valueOf(NODE_FAKE_IP_STR))) {
            // we only add the host IP - MAC map store once,
            // mutable MAP scenario is not considered for now
            if (!extHostMacStore.containsKey(srcIp)) {
                extHostMacStore.put(srcIp, srcMac);
            }

            K8sNode k8sNode = k8sNodeService.nodes().stream()
                    .filter(n -> n.nodeIp().equals(srcIp))
                    .findAny().orElse(null);

            if (k8sNode == null) {
                return;
            } else {
                if (k8sNode.nodeInfo().nodeMac() != null) {
                    return;
                }
            }

            // we update node MAC address which will be referred in node port scenario
            K8sNodeInfo nodeInfo = new K8sNodeInfo(k8sNode.nodeIp(), srcMac);
            K8sNode updatedNode = k8sNode.updateNodeInfo(nodeInfo);
            k8sNodeService.updateNode(updatedNode);
        }
    }

    private void setArpRequest(byte[] senderMac, byte[] senderIp,
                               byte[] targetIp, K8sNode k8sNode) {
        Ethernet ethRequest = ARP.buildArpRequest(senderMac,
                                                  senderIp, targetIp, VlanId.NO_VID);

        // TODO: we need to find a way of sending out ARP request to learn
        //  MAC addresses in NORMAL mode
        PortNumber k8sExtToOsPatchPort = k8sNode.portNumByName(k8sNode.extBridge(),
                k8sNode.k8sExtToOsPatchPortName());
        if (k8sExtToOsPatchPort == null) {
            log.warn("Kubernetes external to OpenStack patch port is null");
            return;
        }
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(k8sExtToOsPatchPort)
                .build();

        packetService.emit(new DefaultOutboundPacket(
                k8sNode.extBridge(),
                treatment,
                ByteBuffer.wrap(ethRequest.serialize())));
    }

    private String getArpMode() {
        Set<ConfigProperty> properties = configService.getProperties(this.getClass().getName());
        return getPropertyValue(properties, ARP_MODE);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String updatedMac = Tools.get(properties, GATEWAY_MAC);
        gatewayMac = updatedMac != null ? updatedMac : GATEWAY_MAC_DEFAULT;
        log.info("Configured. Gateway MAC is {}", gatewayMac);
    }

    /**
     * An internal packet processor which processes ARP request, and results in
     * packet-out ARP reply.
     */
    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            eventExecutor.execute(() -> processPacketIn(context, ethPacket));
        }
    }

    /**
     * An internal kubernetes node listener which is used for listening kubernetes
     * node activity. As long as a node is in complete state, we will install
     * default ARP rule to handle ARP request.
     */
    private class InternalNodeEventListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            K8sNode k8sNode = event.subject();
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(k8sNode));
                    break;
                case K8S_NODE_INCOMPLETE:
                    eventExecutor.execute(() -> processNodeIncompletion(k8sNode));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultArpRule(node, true);
            learnK8sNodeMac(node);
        }

        private void processNodeIncompletion(K8sNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultArpRule(node, false);
        }

        private void setDefaultArpRule(K8sNode node, boolean install) {

            if (getArpMode() == null) {
                return;
            }

            switch (getArpMode()) {
                case ARP_PROXY_MODE:
                    setDefaultArpRuleForProxyMode(node, install);
                    break;
                case ARP_BROADCAST_MODE:
                    // TODO: need to implement broadcast mode
                    log.warn("Not implemented yet.");
                    break;
                default:
                    log.warn("Invalid ARP mode {}. Please use either " +
                            "broadcast or proxy mode.", getArpMode());
                    break;
            }
        }

        private void setDefaultArpRuleForProxyMode(K8sNode node, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            k8sFlowRuleService.setRule(
                    appId,
                    node.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_ARP_CONTROL_RULE,
                    ARP_TABLE,
                    install
            );
        }

        private void learnK8sNodeMac(K8sNode k8sNode) {
            // if we already have a learned MAC address, we skip learning process
            if (k8sNode.nodeMac() != null) {
                return;
            }

            setArpRequest(MacAddress.valueOf(NODE_FAKE_MAC_STR).toBytes(),
                    IpAddress.valueOf(NODE_FAKE_IP_STR).toOctets(),
                    k8sNode.nodeIp().toOctets(),
                    k8sNode);
        }
    }
}
