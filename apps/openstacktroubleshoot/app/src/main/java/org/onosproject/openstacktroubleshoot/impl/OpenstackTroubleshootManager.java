/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktroubleshoot.impl;

import com.google.common.collect.Sets;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMPEcho;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacktroubleshoot.api.OpenstackTroubleshootService;
import org.onosproject.openstacktroubleshoot.api.Reachability;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.packet.ICMP.TYPE_ECHO_REPLY;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.PortNumber.TABLE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.openstacknetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_EXTERNAL_ROUTER_MAC;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_PROBE_RULE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.onosproject.openstacktroubleshoot.util.OpenstackTroubleshootUtil.getSegId;

/**
 * Implementation of openstack troubleshoot app.
 */
@Component(immediate = true, service = OpenstackTroubleshootService.class)
public class OpenstackTroubleshootManager implements OpenstackTroubleshootService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int VID_TAG_RULE_INSTALL_TIMEOUT_MS = 1000;
    private static final int ICMP_RULE_INSTALL_TIMEOUT_MS = 1000;
    private static final int ICMP_REPLY_TIMEOUT_MS = 3000;
    private static final String SERIALIZER_NAME = "openstack-troubleshoot";
    private static final byte TTL = 64;
    private static final short INITIAL_SEQ = 1;
    private static final short MAX_ICMP_GEN = 3;
    private static final int PREFIX_LENGTH = 32;
    private static final int ICMP_PROCESSOR_PRIORITY = 99;

    private static final MacAddress LOCAL_MAC = MacAddress.valueOf("11:22:33:44:55:66");

    private static final String ICMP_COUNTER_NAME = "icmp-id-counter";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private ConsistentMap<String, Reachability> icmpReachabilityMap;
    private AtomicCounter icmpIdCounter;

    private static final KryoNamespace SERIALIZER_DEFAULT_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Reachability.class)
            .register(DefaultReachability.class)
            .build();

    private Set<String> icmpIds = Sets.newConcurrentHashSet();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {

        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        packetService.addProcessor(packetProcessor,
                            PacketProcessor.director(ICMP_PROCESSOR_PRIORITY));

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        icmpReachabilityMap = storageService.<String, Reachability>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_DEFAULT_MAP))
                .withName(SERIALIZER_NAME)
                .withApplicationId(appId)
                .build();

        icmpIdCounter = storageService.getAtomicCounter(ICMP_COUNTER_NAME);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {

        packetService.removeProcessor(packetProcessor);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public Reachability probeEastWest(InstancePort srcPort, InstancePort dstPort) {

        Reachability.Builder rBuilder = DefaultReachability.builder()
                                                        .srcIp(srcPort.ipAddress())
                                                        .dstIp(dstPort.ipAddress());

        if (srcPort.equals(dstPort)) {
            // self probing should always return true
            rBuilder.isReachable(true);
            return rBuilder.build();
        }  else {
            if (srcPort.state() == ACTIVE && dstPort.state() == ACTIVE) {

                // if the two ports are located in different types of networks,
                // we immediately return unreachable state
                if (!osNetworkService.networkType(srcPort.networkId())
                        .equals(osNetworkService.networkType(dstPort.networkId()))) {
                    rBuilder.isReachable(false);
                    return rBuilder.build();
                }

                // install flow rules to enforce ICMP_REQUEST to be tagged and direct to ACL table
                eventExecutor.execute(() -> setVidTagRule(srcPort, true));

                // install flow rules to enforce forwarding ICMP_REPLY to controller
                eventExecutor.execute(() -> setEastWestIcmpReplyRule(srcPort, true));

                timeoutPredicate(1, VID_TAG_RULE_INSTALL_TIMEOUT_MS,
                        this::checkVidTagRule, srcPort.ipAddress().toString());

                timeoutPredicate(1, ICMP_RULE_INSTALL_TIMEOUT_MS,
                        this::checkEastWestIcmpReplyRule, srcPort.ipAddress().toString());

                // send out ICMP ECHO request
                sendIcmpEchoRequest(srcPort, dstPort, null, Direction.EAST_WEST);

                BooleanSupplier checkReachability = () -> icmpReachabilityMap.asJavaMap()
                        .values().stream().allMatch(Reachability::isReachable);

                timeoutSupplier(1, ICMP_REPLY_TIMEOUT_MS, checkReachability);

                // uninstall ICMP_REQUEST VID tagging rules
                eventExecutor.execute(() -> setVidTagRule(srcPort, false));

                // uninstall ICMP_REPLY enforcing rules
                eventExecutor.execute(() -> setEastWestIcmpReplyRule(srcPort, false));

                return icmpReachabilityMap.asJavaMap()
                                          .get(String.valueOf(icmpIdCounter.get()));

            } else {
                rBuilder.isReachable(false);
                return rBuilder.build();
            }
        }
    }

    @Override
    public Reachability probeNorthSouth(InstancePort port) {
        Optional<OpenstackNode> gw = osNodeService.completeNodes(GATEWAY).stream().findFirst();

        if (!gw.isPresent()) {
            log.warn("Gateway is not available to troubleshoot north-south traffic.");
            return null;
        }

        // install flow rules to enforce forwarding ICMP_REPLY to controller
        eventExecutor.execute(() -> setNorthSouthIcmpReplyRule(port, gw.get(), true));

        timeoutPredicate(1, ICMP_RULE_INSTALL_TIMEOUT_MS,
                this::checkNorthSouthIcmpReplyRule, port.ipAddress().toString());

        // send out ICMP ECHO request
        sendIcmpEchoRequest(null, port, gw.get(), Direction.NORTH_SOUTH);

        BooleanSupplier checkReachability = () -> icmpReachabilityMap.asJavaMap()
                .values().stream().allMatch(Reachability::isReachable);

        timeoutSupplier(1, ICMP_REPLY_TIMEOUT_MS, checkReachability);

        // uninstall ICMP_REPLY enforcing rules
        eventExecutor.execute(() -> setNorthSouthIcmpReplyRule(port, gw.get(), false));

        return icmpReachabilityMap.asJavaMap().get(String.valueOf(icmpIdCounter.get()));
    }

    /**
     * Checks whether east-west ICMP reply rule is added or not.
     *
     * @param dstIp    IP address
     * @return true if ICMP reply rule is added, false otherwise
     */
    private boolean checkEastWestIcmpReplyRule(String dstIp) {
        return checkFlowRule(dstIp, IPV4_DST);
    }

    /**
     * Checks whether north-south ICMP reply rule is added or not.
     *
     * @param srcIp    IP address
     * @return true if ICMP reply rule is added, false otherwise
     */
    private boolean checkNorthSouthIcmpReplyRule(String srcIp) {
        return checkFlowRule(srcIp, IPV4_SRC);
    }

    /**
     * Checks whether ICMP request VID tagging rule is added or not.
     *
     * @param srcIp source IP address
     * @return true if the rule is added, false otherwise
     */
    private boolean checkVidTagRule(String srcIp) {
        return checkFlowRule(srcIp, IPV4_SRC);
    }

    /**
     * Checks whether flow rules with the given IP and criterion are added or not.
     *
     * @param ip        IP address
     * @param ipType    IP criterion type (IPV4_DST or IPV4_SRC)
     * @return true if the flow rule is added, false otherwise
     */
    private boolean checkFlowRule(String ip, Criterion.Type ipType) {
        for (FlowEntry entry : flowRuleService.getFlowEntriesById(appId)) {
            TrafficSelector selector = entry.selector();

            IPCriterion ipCriterion = (IPCriterion) selector.getCriterion(ipType);

            if (ipCriterion != null &&
                    ip.equals(ipCriterion.ip().address().toString()) &&
                    entry.state() == ADDED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Installs/uninstalls a flow rule to match ingress fake ICMP request packets,
     * and tags VNI/VID, direct the tagged packet to ACL table.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setVidTagRule(InstancePort port, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(port.ipAddress(), PREFIX_LENGTH))
                .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder()
                .setTunnelId(getSegId(osNetworkService, port))
                .transition(ACL_EGRESS_TABLE);

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                tb.build(),
                PRIORITY_ICMP_PROBE_RULE,
                VTAG_TABLE,
                install);
    }

    /**
     * Installs/uninstalls a flow rule to match north-south ICMP reply packets,
     * direct all ICMP reply packets to the controller.
     *
     * @param port      instance port
     * @param gw        gateway node
     * @param install   installation flag
     */
    private void setNorthSouthIcmpReplyRule(InstancePort port, OpenstackNode gw,
                                            boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(port.ipAddress(), PREFIX_LENGTH))
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(ICMP.TYPE_ECHO_REPLY)
                .matchTunnelId(getSegId(osNetworkService, port))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setIpSrc(instancePortService.floatingIp(port.portId()))
                .setEthSrc(port.macAddress())
                .setEthDst(DEFAULT_EXTERNAL_ROUTER_MAC)
                .punt()
                .build();

        osFlowRuleService.setRule(
                appId,
                gw.intgBridge(),
                selector,
                treatment,
                PRIORITY_ICMP_PROBE_RULE,
                GW_COMMON_TABLE,
                install);
    }

    /**
     * Installs/uninstalls a flow rule to match east-west ICMP reply packets,
     * direct all ICMP reply packets to the controller.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setEastWestIcmpReplyRule(InstancePort port, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(port.ipAddress(), PREFIX_LENGTH))
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(ICMP.TYPE_ECHO_REPLY)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                treatment,
                PRIORITY_ICMP_PROBE_RULE,
                FORWARDING_TABLE,
                install);
    }

    /**
     * Sends out ICMP ECHO REQUEST to destined VM.
     *
     * @param srcPort   source instance port
     * @param dstPort   destination instance port
     */
    private void sendIcmpEchoRequest(InstancePort srcPort, InstancePort dstPort,
                                     OpenstackNode gateway, Direction direction) {

        short icmpSeq = INITIAL_SEQ;

        short icmpId = (short) icmpIdCounter.incrementAndGet();

        for (int i = 0; i < MAX_ICMP_GEN; i++) {
            packetService.emit(buildIcmpOutputPacket(srcPort, dstPort, gateway,
                    icmpId, icmpSeq, direction));
            icmpSeq++;
        }
    }

    /**
     * Builds ICMP Outbound packet.
     *
     * @param srcPort   source instance port
     * @param dstPort   destination instance port
     * @param icmpId    ICMP identifier
     * @param icmpSeq   ICMP sequence number
     */
    private OutboundPacket buildIcmpOutputPacket(InstancePort srcPort,
                                                 InstancePort dstPort,
                                                 OpenstackNode gateway,
                                                 short icmpId,
                                                 short icmpSeq,
                                                 Direction direction) {

        Ethernet ethFrame;
        IpAddress srcIp;
        IpAddress dstIp;
        DeviceId deviceId;

        if (direction == Direction.EAST_WEST) {
            ethFrame = constructEastWestIcmpPacket(srcPort, dstPort, icmpId, icmpSeq);
            srcIp = srcPort.ipAddress();
            dstIp = dstPort.ipAddress();
            deviceId = srcPort.deviceId();
        } else if (direction == Direction.NORTH_SOUTH) {
            ethFrame = constructNorthSouthIcmpPacket(dstPort, icmpId, icmpSeq);
            srcIp = clusterService.getLocalNode().ip();
            dstIp = instancePortService.floatingIp(dstPort.portId());
            deviceId = gateway.intgBridge();
        } else {
            log.warn("Invalid traffic direction {}", direction);
            return null;
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        // we send out the packet to ingress table (index is 0) of source OVS
        // to enforce the Outbound packet to go through the ingress and egress
        // pipeline
        tBuilder.setOutput(TABLE);

        Reachability reachability = DefaultReachability.builder()
                                            .srcIp(srcIp)
                                            .dstIp(dstIp)
                                            .isReachable(false)
                                            .build();

        icmpReachabilityMap.put(String.valueOf(icmpId), reachability);
        icmpIds.add(String.valueOf(icmpId));

        return new DefaultOutboundPacket(
                        deviceId,
                        tBuilder.build(),
                        ByteBuffer.wrap(ethFrame.serialize()));
    }

    /**
     * Constructs an ICMP packet with given source and destination IP/MAC.
     *
     * @param srcIp     source IP address
     * @param dstIp     destination IP address
     * @param srcMac    source MAC address
     * @param dstMac    destination MAC address
     * @param icmpId    ICMP identifier
     * @param icmpSeq   ICMP sequence number
     * @return an ethernet frame which contains ICMP payload
     */
    private Ethernet constructIcmpPacket(IpAddress srcIp, IpAddress dstIp,
                                         MacAddress srcMac, MacAddress dstMac,
                                         short icmpId, short icmpSeq) {

        // Ethernet frame
        Ethernet ethFrame = new Ethernet();

        ethFrame.setEtherType(TYPE_IPV4);
        ethFrame.setSourceMACAddress(srcMac);
        ethFrame.setDestinationMACAddress(dstMac);

        // IP packet
        IPv4 iPacket = new IPv4();
        iPacket.setDestinationAddress(dstIp.toString());
        iPacket.setSourceAddress(srcIp.toString());
        iPacket.setTtl(TTL);
        iPacket.setProtocol(IPv4.PROTOCOL_ICMP);

        // ICMP packet
        ICMP icmp = new ICMP();
        icmp.setIcmpType(TYPE_ECHO_REQUEST)
                .setIcmpCode(TYPE_ECHO_REQUEST)
                .resetChecksum();

        // ICMP ECHO packet
        ICMPEcho icmpEcho = new ICMPEcho();
        icmpEcho.setIdentifier(icmpId)
                .setSequenceNum(icmpSeq);

        ByteBuffer byteBufferIcmpEcho = ByteBuffer.wrap(icmpEcho.serialize());

        try {
            icmp.setPayload(ICMPEcho.deserializer().deserialize(byteBufferIcmpEcho.array(),
                    0, ICMPEcho.ICMP_ECHO_HEADER_LENGTH));
        } catch (DeserializationException e) {
            log.warn("Failed to deserialize ICMP ECHO REQUEST packet");
        }

        ByteBuffer byteBufferIcmp = ByteBuffer.wrap(icmp.serialize());

        try {
            iPacket.setPayload(ICMP.deserializer().deserialize(byteBufferIcmp.array(),
                    0,
                    byteBufferIcmp.array().length));
        } catch (DeserializationException e) {
            log.warn("Failed to deserialize ICMP packet");
        }

        ethFrame.setPayload(iPacket);

        return ethFrame;
    }

    /**
     * Constructs an east-west ICMP packet with given source and destination IP/MAC.
     *
     * @param srcPort   source instance port
     * @param dstPort   destination instance port
     * @param icmpId    ICMP identifier
     * @param icmpSeq   ICMP sequence number
     * @return an ethernet frame which contains ICMP payload
     */
    private Ethernet constructEastWestIcmpPacket(InstancePort srcPort,
                                                 InstancePort dstPort,
                                                 short icmpId, short icmpSeq) {
        boolean isRemote = true;

        if (srcPort.deviceId().equals(dstPort.deviceId()) &&
                osNetworkService.gatewayIp(srcPort.portId())
                        .equals(osNetworkService.gatewayIp(dstPort.portId()))) {
            isRemote = false;
        }

        // if the source and destination VMs are located in different OVS,
        // we will assign fake gateway MAC as the destination MAC
        MacAddress dstMac = isRemote ? DEFAULT_GATEWAY_MAC : dstPort.macAddress();

        return constructIcmpPacket(srcPort.ipAddress(), dstPort.ipAddress(),
                                    srcPort.macAddress(), dstMac, icmpId, icmpSeq);
    }

    /**
     * Constructs a north-south ICMP packet with the given destination IP/MAC.
     *
     * @param dstPort   destination instance port
     * @param icmpId    ICMP identifier
     * @param icmpSeq   ICMP sequence number
     * @return an ethernet frame which contains ICMP payload
     */
    private Ethernet constructNorthSouthIcmpPacket(InstancePort dstPort,
                                                   short icmpId, short icmpSeq) {

        IpAddress localIp = clusterService.getLocalNode().ip();
        IpAddress fip = instancePortService.floatingIp(dstPort.portId());

        if (fip != null) {
            return constructIcmpPacket(localIp, fip, LOCAL_MAC, dstPort.macAddress(),
                    icmpId, icmpSeq);
        } else {
            return null;
        }
    }

    /**
     * Handles ICMP ECHO REPLY packets.
     *
     * @param ipPacket  IP packet
     * @param icmp ICMP packet
     */
    private void handleIcmpEchoReply(IPv4 ipPacket, ICMP icmp) {

        String icmpKey = icmpId(icmp);

        String srcIp = IPv4.fromIPv4Address(ipPacket.getDestinationAddress());
        String dstIp = IPv4.fromIPv4Address(ipPacket.getSourceAddress());

        Reachability reachability = DefaultReachability.builder()
                                                .srcIp(IpAddress.valueOf(srcIp))
                                                .dstIp(IpAddress.valueOf(dstIp))
                                                .isReachable(false)
                                                .build();

        icmpReachabilityMap.computeIfPresent(icmpKey, (key, value) -> {
            if (value.equals(reachability)) {

                log.debug("src: {}, dst: {} is reachable!", value.dstIp(), value.srcIp());

                return DefaultReachability.builder()
                        .srcIp(IpAddress.valueOf(srcIp))
                        .dstIp(IpAddress.valueOf(dstIp))
                        .isReachable(true)
                        .build();
            }
            return reachability;
        });
    }

    /**
     * Obtains an unique ICMP key.
     *
     * @param icmp ICMP packet
     * @return  ICMP key
     */
    private String icmpId(ICMP icmp) {
        ICMPEcho echo = (ICMPEcho) icmp.getPayload();
        checkNotNull(echo);

        short icmpId = echo.getIdentifier();

        return String.valueOf(icmpId);
    }

    /**
     * Holds the current thread unit the timeout expires, during the hold the
     * thread periodically execute the given method.
     *
     * @param count         count of unit
     * @param unit          unit
     * @param predicate     predicate
     * @param predicateArg  predicate argument
     */
    private void timeoutPredicate(long count, int unit,
                                  Predicate<String> predicate, String predicateArg) {
        long timeoutExpiredMs = System.currentTimeMillis() + unit * count;

        while (true) {

            long waitMs = timeoutExpiredMs - System.currentTimeMillis();

            if (predicate.test(predicateArg)) {
                break;
            }

            if (waitMs <= 0) {
                break;
            }
        }
    }

    /**
     * Holds the current thread unit the timeout expires, during the hold the
     * thread periodically execute the given method.
     *
     * @param count     count of unit
     * @param unit      unit
     * @param supplier  boolean supplier
     */
    private void timeoutSupplier(long count, int unit, BooleanSupplier supplier) {
        long timeoutExpiredMs = System.currentTimeMillis() + unit * count;

        while (true) {

            long waitMs = timeoutExpiredMs - System.currentTimeMillis();

            if (supplier.getAsBoolean()) {
                break;
            }

            if (waitMs <= 0) {
                break;
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
            if (ethernet == null || ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                return;
            }

            IPv4 iPacket = (IPv4) ethernet.getPayload();
            if (iPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
                eventExecutor.execute(() -> processIcmpPacket(context, ethernet));
            }
        }

        /**
         * Processes the received ICMP packet.
         *
         * @param context   packet context
         * @param ethernet  ethernet
         */
        private void processIcmpPacket(PacketContext context, Ethernet ethernet) {
            IPv4 ipPacket = (IPv4) ethernet.getPayload();
            ICMP icmp = (ICMP) ipPacket.getPayload();
            log.trace("Processing ICMP packet source MAC:{}, source IP:{}," +
                            "dest MAC:{}, dest IP:{}",
                    ethernet.getSourceMAC(),
                    IpAddress.valueOf(ipPacket.getSourceAddress()),
                    ethernet.getDestinationMAC(),
                    IpAddress.valueOf(ipPacket.getDestinationAddress()));

            String icmpId = icmpId(icmp);

            // if the ICMP ID is not contained in ICMP ID set, we do not handle it
            if (!icmpIds.contains(icmpId)) {
                return;
            }

            switch (icmp.getIcmpType()) {
                case TYPE_ECHO_REPLY:
                    handleIcmpEchoReply(ipPacket, icmp);
                    context.block();
                    icmpIds.remove(icmpId);
                    break;
                default:
                    break;
            }
        }
    }
}
