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

import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles arp packet.
 */
@Component(immediate = true)
public class KubevirtRoutingArpHandler {
    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterAdminService kubevirtRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkAdminService kubevirtNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService kubevirtFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    private final InternalRouterEventListener kubevirtRouterlistener = new InternalRouterEventListener();

    private final Timer timer = new Timer("kubevirtcni-routing-arphandler");
    private static final long SECONDS = 1000L;

    private ApplicationId appId;
    private NodeId localNodeId;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        kubevirtRouterService.addListener(kubevirtRouterlistener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        packetService.removeProcessor(packetProcessor);
        kubevirtRouterService.removeListener(kubevirtRouterlistener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }
    /**
     * Triggers ARP request to retrieve the peer router mac address.
     *
     * @param router kubevirt router
     * @param peerRouterIp peer router IP address
     */
    private void retrievePeerRouterMac(KubevirtRouter router, IpAddress peerRouterIp) {

        log.info("Sending ARP request to the peer router {} to retrieve the MAC address.",
                peerRouterIp.getIp4Address().toString());
        String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);

        if (routerSnatIp == null) {
            return;
        }

        IpAddress sourceIp = IpAddress.valueOf(routerSnatIp);

        MacAddress sourceMac = DEFAULT_GATEWAY_MAC;
        Ethernet ethRequest = ARP.buildArpRequest(sourceMac.toBytes(),
                sourceIp.toOctets(),
                peerRouterIp.toOctets(), VlanId.NO_VID);

        KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

        if (gatewayNode == null) {
            return;
        }

        PortNumber externalPatchPortNum = KubevirtNetworkingUtil.externalPatchPortNum(deviceService, gatewayNode);

        if (externalPatchPortNum == null) {
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(externalPatchPortNum)
                .build();

        packetService.emit(new DefaultOutboundPacket(
                gatewayNode.intgBridge(),
                treatment,
                ByteBuffer.wrap(ethRequest.serialize())));
    }

    /**
     * Sets default ARP flow rule to retrieve peer router mac address.
     *
     * @param routerSnatIp route Snat IP
     * @param peerRouterIp peer router IP
     * @param gatewayNodeId gateway node
     * @param install install if true, uninstall otherwise
     */
    private void setRuleArpRequestToController(IpAddress routerSnatIp,
                                              IpAddress peerRouterIp,
                                              String gatewayNodeId,
                                              boolean install) {
        KubevirtNode gatewayNode = kubevirtNodeService.node(gatewayNodeId);
        if (gatewayNode == null) {
            return;
        }

        if (routerSnatIp == null) {
            return;
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REPLY)
                .matchArpSpa(peerRouterIp.getIp4Address())
                .matchArpTpa(routerSnatIp.getIp4Address())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        kubevirtFlowRuleService.setRule(
                appId,
                gatewayNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_ARP_GATEWAY_RULE,
                GW_ENTRY_TABLE,
                install
        );
    }

    private class InternalRouterEventListener implements KubevirtRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtRouterEvent event) {
            switch (event.type()) {
                case KUBEVIRT_GATEWAY_NODE_ATTACHED:
                case KUBEVIRT_ROUTER_EXTERNAL_NETWORK_ATTACHED:
                    eventExecutor.execute(() -> processRouterExternalNetAttachedOrGwAttached(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_REMOVED:
                    eventExecutor.execute(() -> processRouterRemoved(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_EXTERNAL_NETWORK_DETACHED:
                    eventExecutor.execute(() -> processRouterExternalNetDetached(event.subject(),
                            event.externalIp(), event.externalPeerRouterIp()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_DETACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeDetached(event.subject(), event.gateway()));
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

        private void processRouterGatewayNodeChanged(KubevirtRouter router, String oldGateway) {
            if (!isRelevantHelper()) {
                return;
            }
            processRouterGatewayNodeDetached(router, oldGateway);
            processRouterExternalNetAttachedOrGwAttached(router);
        }

        private void processRouterExternalNetAttachedOrGwAttached(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

            if (gatewayNode == null) {
                return;
            }

            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
            if (routerSnatIp == null) {
                return;
            }

            if (router.peerRouter() != null &&
                    router.peerRouter().macAddress() == null &&
                    router.peerRouter().ipAddress() != null) {
                setRuleArpRequestToController(IpAddress.valueOf(routerSnatIp),
                        router.peerRouter().ipAddress(), gatewayNode.hostname(), true);

                retrievePeerRouterMac(router, router.peerRouter().ipAddress());
                checkPeerRouterMacRetrieved(router);

            }
        }

        private void checkPeerRouterMacRetrieved(KubevirtRouter router) {
            InternalTimerTask task = new InternalTimerTask(router.name(), router.peerRouter().ipAddress());
            timer.schedule(task, 5 * SECONDS, 60 * SECONDS);
        }

        private void processRouterExternalNetDetached(KubevirtRouter router, String routerSnatIp,
                                                      String peerRouterIp) {
            log.info("processRouterRemovedOrExternalNetDetached called");
            if (!isRelevantHelper()) {
                return;
            }
            if (router.electedGateway() == null) {
                return;
            }
            KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

            if (gatewayNode == null) {
                return;
            }

            if (routerSnatIp == null || peerRouterIp == null) {
                return;
            }
            setRuleArpRequestToController(IpAddress.valueOf(routerSnatIp),
                    IpAddress.valueOf(peerRouterIp), gatewayNode.hostname(), false);
        }


        private void processRouterRemoved(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.electedGateway() == null) {
                return;
            }
            KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());
            if (gatewayNode == null) {
                return;
            }

            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
            if (routerSnatIp == null) {
                return;
            }

            IpAddress peerRouterIp = router.peerRouter().ipAddress();
            if (peerRouterIp == null) {
                return;
            }

            setRuleArpRequestToController(IpAddress.valueOf(routerSnatIp),
                    peerRouterIp, gatewayNode.hostname(), false);
        }

        private void processRouterGatewayNodeDetached(KubevirtRouter router, String detachedGatewayNode) {
            if (!isRelevantHelper()) {
                return;
            }

            if (detachedGatewayNode == null) {
                return;
            }

            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
            if (routerSnatIp == null) {
                return;
            }

            if (router.peerRouter() != null && router.peerRouter().ipAddress() != null) {
                setRuleArpRequestToController(IpAddress.valueOf(routerSnatIp),
                        router.peerRouter().ipAddress(), detachedGatewayNode, false);
            }
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet != null && ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                processArpPacket(ethernet);
            }
        }

        private void processArpPacket(Ethernet ethernet) {
            ARP arp = (ARP) ethernet.getPayload();

            if (arp.getOpCode() == ARP.OP_REQUEST) {
                return;
            }

            IpAddress spa = Ip4Address.valueOf(arp.getSenderProtocolAddress());
            MacAddress sha = MacAddress.valueOf(arp.getSenderHardwareAddress());

            IpAddress tpa = Ip4Address.valueOf(arp.getTargetProtocolAddress());

            KubevirtRouter router = kubevirtRouterService.routers().stream()
                    .filter(r -> r.peerRouter() != null && r.peerRouter().ipAddress().equals(spa))
                    .filter(r -> {
                        String routerSnatIp = r.external().keySet().stream().findAny().orElse(null);
                        if (routerSnatIp == null) {
                            return false;
                        }
                        return IpAddress.valueOf(routerSnatIp).equals(tpa);
                    })
                    .findAny().orElse(null);

            if (router == null) {
                return;
            }

            KubevirtPeerRouter peerRouter = new KubevirtPeerRouter(spa, sha);
            log.info("Update peer router mac adress {} to router {}", peerRouter.macAddress(), router.name());

            kubevirtRouterService.updatePeerRouterMac(router.name(), sha);
        }
    }

    private class InternalTimerTask extends TimerTask {
        String routerName;
        IpAddress routerIpAddress;

        public InternalTimerTask(String routerName, IpAddress routerIpAddress) {
            this.routerName = routerName;
            this.routerIpAddress = routerIpAddress;
        }

        @Override
        public void run() {
            KubevirtRouter router = kubevirtRouterService.router(routerName);

            if (router == null) {
                return;
            }

            if (router.peerRouter().macAddress() != null) {
                log.info("Peer Router Mac for {} is retrieved. Stop this task..", routerName);
                this.cancel();
                return;
            }

            retrievePeerRouterMac(router, routerIpAddress);
        }
    }
}
