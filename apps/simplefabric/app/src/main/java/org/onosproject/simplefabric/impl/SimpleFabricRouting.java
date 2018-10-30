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
package org.onosproject.simplefabric.impl;

import com.google.common.collect.ImmutableList;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.intent.constraint.HashedPathSelectionConstraint;
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.simplefabric.api.FabricNetwork;
import org.onosproject.simplefabric.api.FabricSubnet;
import org.onosproject.simplefabric.api.FabricRoute;
import org.onosproject.simplefabric.api.SimpleFabricEvent;
import org.onosproject.simplefabric.api.SimpleFabricListener;
import org.onosproject.simplefabric.api.SimpleFabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.simplefabric.api.Constants.ALLOW_ETH_ADDRESS_SELECTOR;
import static org.onosproject.simplefabric.api.Constants.ALLOW_IPV6;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_BORDER_BASE;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_BORDER_FORWARD;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_BORDER_INTERCEPT;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_BORDER_STEP;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_LOCAL_FORWARD;
import static org.onosproject.simplefabric.api.Constants.PRI_REACTIVE_LOCAL_INTERCEPT;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_ALLOW_LINK_CP;
import static org.onosproject.simplefabric.api.Constants.ROUTING_APP_ID;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_HASHED_PATH_SELECTION;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_MATCH_IP_PROTO;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_SINGLE_TO_SINGLE;


/**
 * SimpleFabricRouting handles Routing.
 */
@Component(immediate = true, enabled = false)
public class SimpleFabricRouting {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SimpleFabricService simpleFabric;

    private ImmutableList<Constraint> reactiveConstraints
            = ImmutableList.of(new PartialFailureConstraint());
            //= ImmutableList.of();
            // NOTE: SHOULD NOT use HashedPathSelectionConstraint
            //       for unpredictable srcCp of Link appears as reactive packet traffic

    private Set<FlowRule> interceptFlowRules = new HashSet<>();
    private Set<Key> toBePurgedIntentKeys = new HashSet<>();
            // NOTE: manage purged intents by key for intentService.getIntent() supports key only

    private final InternalSimpleFabricListener simpleFabricListener = new InternalSimpleFabricListener();
    private InternalRoutingProcessor processor = new InternalRoutingProcessor();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(ROUTING_APP_ID);
        log.info("simple fabric routing starting with app id {}", appId.toString());

        // NOTE: may not clear at init for MIGHT generate pending_remove garbages
        //       use flush event from simple fabric cli command

        if (REACTIVE_HASHED_PATH_SELECTION) {
            reactiveConstraints = ImmutableList.of(new PartialFailureConstraint(),
                                                   new HashedPathSelectionConstraint());
        } else {
            reactiveConstraints = ImmutableList.of(new PartialFailureConstraint());
        }

        processor = new InternalRoutingProcessor();
        packetService.addProcessor(processor, PacketProcessor.director(2));
        simpleFabric.addListener(simpleFabricListener);

        registerIntercepts();
        refreshIntercepts();

        log.info("simple fabric routing started");
    }

    @Deactivate
    public void deactivate() {
        log.info("simple fabric routing stopping");

        packetService.removeProcessor(processor);
        simpleFabric.removeListener(simpleFabricListener);

        withdrawIntercepts();

        // NOTE: may not clear at init for MIGHT generate pending_remove garbages
        //       use flush event from simple fabric cli command

        toBePurgedIntentKeys.clear();

        flowRuleService.removeFlowRulesById(appId);

        processor = null;

        log.info("simple fabric routing stopped");
    }

    /**
     * Request packet in via the PacketService.
     */
    private void registerIntercepts() {
        // register default intercepts on packetService for broder routing intercepts

        packetService.requestPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
            PacketPriority.REACTIVE, appId);

        if (ALLOW_IPV6) {
            packetService.requestPackets(
                DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV6).build(),
                PacketPriority.REACTIVE, appId);
        }

        log.info("simple fabric routing ip packet intercepts started");
    }

    /**
     * Cancel request for packet in via PacketService.
     */
    private void withdrawIntercepts() {
        // unregister default intercepts on packetService

        packetService.cancelPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
            PacketPriority.REACTIVE, appId);

        if (ALLOW_IPV6) {
            packetService.cancelPackets(
                DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV6).build(),
                PacketPriority.REACTIVE, appId);
        }

        log.info("simple fabric routing ip packet intercepts stopped");
    }

    /**
     * Refresh device flow rules for intercepts on local fabricSubnets.
     */
    private void refreshIntercepts() {
        Set<FlowRule> newInterceptFlowRules = new HashSet<>();
        for (Device device : deviceService.getAvailableDevices()) {
            for (FabricSubnet subnet : simpleFabric.defaultFabricSubnets()) {
                newInterceptFlowRules.add(generateInterceptFlowRule(true, device.id(), subnet.prefix()));
                // check if this devices has the fabricSubnet, then add ip broadcast flue rule
                FabricNetwork fabricNetwork = simpleFabric.fabricNetwork(subnet.networkName());
                if (fabricNetwork != null && fabricNetwork.contains(device.id())) {
                    newInterceptFlowRules.add(generateLocalSubnetIpBctFlowRule(device.id(), subnet.prefix(),
                            fabricNetwork));
                }
                // JUST FOR FLOW RULE TEST ONLY
                //newInterceptFlowRules.add(generateTestFlowRule(device.id(), subnet.ipPrefix()));
            }
            for (FabricRoute route : simpleFabric.fabricRoutes()) {
                newInterceptFlowRules.add(generateInterceptFlowRule(false, device.id(), route.prefix()));
            }
        }

        if (!newInterceptFlowRules.equals(interceptFlowRules)) {
            // NOTE: DO NOT REMOVE INTERCEPT FLOW RULES FOR FAILED DEVICE FLOW UPDATE MIGHT BE BLOCKED
            /*
            interceptFlowRules.stream()
                .filter(rule -> !newInterceptFlowRules.contains(rule))
                .forEach(rule -> {
                    flowRuleService.removeFlowRules(rule);
                    log.info("simple fabric reactive routing remove intercept flow rule: {}", rule);
                });
            */
            newInterceptFlowRules.stream()
                .filter(rule -> !interceptFlowRules.contains(rule))
                .forEach(rule -> {
                    flowRuleService.applyFlowRules(rule);
                    log.info("simple fabric routing apply intercept flow rule: {}", rule);
                });
            interceptFlowRules = newInterceptFlowRules;
        }
    }

    private FlowRule generateInterceptFlowRule(boolean isDstLocalSubnet, DeviceId deviceId, IpPrefix prefix) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (prefix.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            if (prefix.prefixLength() > 0) {
                selector.matchIPDst(prefix);
            }
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            if (prefix.prefixLength() > 0) {
                selector.matchIPv6Dst(prefix);
            }
        }
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(reactivePriority(false, isDstLocalSubnet, prefix.prefixLength()))
                .withSelector(selector.build())
                .withTreatment(DefaultTrafficTreatment.builder().punt().build())
                .fromApp(appId)
                .makePermanent()
                .forTable(0).build();
        return rule;
    }

    private FlowRule generateLocalSubnetIpBctFlowRule(DeviceId deviceId, IpPrefix prefix, FabricNetwork fabricNetwork) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        IpPrefix bctPrefix;
        if (prefix.isIp4()) {
            bctPrefix = Ip4Prefix.valueOf(prefix.getIp4Prefix().address().toInt() |
                                              ~Ip4Address.makeMaskPrefix(prefix.prefixLength()).toInt(),
                                          Ip4Address.BIT_LENGTH);
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(bctPrefix);
        } else {
            byte[] p = prefix.getIp6Prefix().address().toOctets();
            byte[] m = Ip6Address.makeMaskPrefix(prefix.prefixLength()).toOctets();
            for (int i = 0; i < p.length; i++) {
                 p[i] |= ~m[i];
            }
            bctPrefix = Ip6Prefix.valueOf(p, Ip6Address.BIT_LENGTH);
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchIPv6Dst(bctPrefix);
        }
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        Set<ConnectPoint> newEgressPoints = new HashSet<>();
        for (Interface iface : fabricNetwork.interfaces()) {
            if (iface.connectPoint().deviceId().equals(deviceId)) {
                treatment.setOutput(iface.connectPoint().port());
            }
        }
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(reactivePriority(true, true, bctPrefix.prefixLength()))
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .forTable(0).build();
        return rule;
    }

    /**
     * Refresh routes by examining network resource status.
     */
    private void refreshRouteIntents() {
        for (Intent entry : intentService.getIntents()) {
            if (!appId.equals(entry.appId())) {
                continue;
            }

            MultiPointToSinglePointIntent intent = (MultiPointToSinglePointIntent) entry;

            if (!intentService.isLocal(intent.key())) {
                if (toBePurgedIntentKeys.contains(intent.key())) {
                    toBePurgedIntentKeys.remove(intent.key());  // clear non local intent
                }
                continue;
            }

            try {
                switch (intentService.getIntentState(intent.key())) {
                //case FAILED:   // failed intent is not auto removed
                case WITHDRAWN:
                    log.warn("intent found failed or withdrawn; "
                             +  "remove and try to purge intent: key={}", intent.key());
                    // purge intents here without withdraw
                    intentService.purge(intentService.getIntent(intent.key()));
                    toBePurgedIntentKeys.add(intent.key());
                    continue;
                default: // no action
                    break;
                }
            } catch (Exception e) {
                log.warn("intent status lookup failed: error={}", e);
                continue;  // this intent seems invalid; no action
            }

            // dummy loop to break on remove cases
            if (!deviceService.isAvailable(intent.egressPoint().deviceId())) {
                log.info("refresh route intents; remove intent for no device: key={}", intent.key());
                intentService.withdraw(intentService.getIntent(intent.key()));
                toBePurgedIntentKeys.add(intent.key());
                continue;
            }
            if (!(simpleFabric.fabricNetwork(intent.egressPoint(), VlanId.NONE) != null ||
                  (REACTIVE_ALLOW_LINK_CP &&
                   !linkService.getEgressLinks(intent.egressPoint()).isEmpty()))) {
                log.info("refresh route intents; remove intent for egress point not available: key={}", intent.key());
                intentService.withdraw(intentService.getIntent(intent.key()));
                toBePurgedIntentKeys.add(intent.key());
                continue;
            }

            // MAY NEED TO CHECK: intent.egressPoint and intent.treatment's dstMac is valid against hosts
            if (REACTIVE_SINGLE_TO_SINGLE && !REACTIVE_ALLOW_LINK_CP) {
                // single path intent only; no need to check ingress points
                continue;
            }

            Set<FilteredConnectPoint> newIngressPoints = new HashSet<>();
            boolean ingressPointChanged = false;
            for (FilteredConnectPoint cp : intent.filteredIngressPoints()) {
                if (deviceService.isAvailable(cp.connectPoint().deviceId()) &&
                    (simpleFabric.fabricNetwork(cp.connectPoint(), VlanId.NONE) != null ||
                     (REACTIVE_ALLOW_LINK_CP &&
                      !linkService.getIngressLinks(cp.connectPoint()).isEmpty()))) {
                    newIngressPoints.add(cp);
                } else {
                    log.info("refresh route ingress cp of "
                             + "not in 2Networks nor links: {}", cp);
                    ingressPointChanged = true;
                }
            }
            if (newIngressPoints.isEmpty()) {
                log.info("refresh route intents; "
                          + "remove intent for no ingress nor egress point available: key={}", intent.key());
                intentService.withdraw(intentService.getIntent(intent.key()));
                toBePurgedIntentKeys.add(intent.key());
                continue;
            }
            // update ingress points
            if (ingressPointChanged) {
                MultiPointToSinglePointIntent updatedIntent =
                    MultiPointToSinglePointIntent.builder()
                        .appId(appId)
                        .key(intent.key())
                        .selector(intent.selector())
                        .treatment(intent.treatment())
                        .filteredIngressPoints(newIngressPoints)
                        .filteredEgressPoint(intent.filteredEgressPoint())
                        .priority(intent.priority())
                        .constraints(intent.constraints())
                        .build();
                log.info("refresh route update intent: key={} updatedIntent={}",
                        intent.key(), updatedIntent);
                toBePurgedIntentKeys.remove(intent.key());   // may remove from old purged entry
                intentService.submit(updatedIntent);
            }
        }
    }

    private void checkIntentsPurge() {
        // check intents to be purge
        if (!toBePurgedIntentKeys.isEmpty()) {
            Set<Key> removeKeys = new HashSet<>();
            for (Key key : toBePurgedIntentKeys) {
                if (!intentService.isLocal(key)) {
                    removeKeys.add(key);
                    continue;
                }
                Intent intentToPurge = intentService.getIntent(key);
                if (intentToPurge == null) {
                    log.info("purged intent: key={}", key);
                    removeKeys.add(key);
                } else {
                    switch (intentService.getIntentState(key)) {
                    // case FAILED:  // not auto removed
                    case WITHDRAWN:
                        log.info("try to purge intent: key={}", key);
                        intentService.purge(intentToPurge);
                        break;
                    case INSTALL_REQ:
                    case INSTALLED:
                    case INSTALLING:
                    case RECOMPILING:
                    case COMPILING:
                        log.warn("not to purge for active intent: key={}", key);
                        removeKeys.add(key);
                        break;
                    case WITHDRAW_REQ:
                    case WITHDRAWING:
                    case PURGE_REQ:
                    case CORRUPT:
                    default:
                        // no action
                        break;
                    }
                }
            }
            toBePurgedIntentKeys.removeAll(removeKeys);
        }
    }

    public void withdrawAllReactiveIntents() {
        // check all intents of this app
        // NOTE: cli calls are handling within the cli called node only; so should not user inents.isLocal()
        Set<Intent> myIntents = new HashSet<>();
        for (Intent intent : intentService.getIntents()) {
            if (appId.equals(intent.appId())) {
                myIntents.add(intent);
            }
        }
        // withdraw all my intents
        for (Intent intent : myIntents) {
            switch (intentService.getIntentState(intent.key())) {
            case FAILED:
                intentService.purge(intent);
                toBePurgedIntentKeys.add(intent.key());
                break;
            case WITHDRAWN:
                intentService.purge(intent);
                toBePurgedIntentKeys.add(intent.key());
                break;
            case INSTALL_REQ:
            case INSTALLED:
            case INSTALLING:
            case RECOMPILING:
            case COMPILING:
                intentService.withdraw(intent);
                toBePurgedIntentKeys.add(intent.key());
                break;
            case WITHDRAW_REQ:
            case WITHDRAWING:
                toBePurgedIntentKeys.add(intent.key());
                break;
            case PURGE_REQ:
            case CORRUPT:
            default:
                // no action
                break;
            }
        }
    }

    /**
     * Internal Packet Handling.
     */
    private class InternalRoutingProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }
            ConnectPoint srcCp = pkt.receivedFrom();
            IpAddress srcIp;
            IpAddress dstIp;
            byte ipProto = 0;  /* 0 or tcp, udp */

            switch (EthType.EtherType.lookup(ethPkt.getEtherType())) {
            case IPV4:
                IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                srcIp = IpAddress.valueOf(ipv4Packet.getSourceAddress());
                dstIp = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
                ipProto = ipv4Packet.getProtocol();
                break;
            case IPV6:
                IPv6 ipv6Packet = (IPv6) ethPkt.getPayload();
                srcIp = IpAddress.valueOf(IpAddress.Version.INET6, ipv6Packet.getSourceAddress());
                dstIp = IpAddress.valueOf(IpAddress.Version.INET6, ipv6Packet.getDestinationAddress());
                ipProto = ipv6Packet.getNextHeader();
                break;
            default:
                return;  // ignore unknow ether type packets
            }
            if (ipProto != 6 && ipProto != 17) {
                ipProto = 0;  /* handle special for TCP and UDP only */
            }

            if (!checkVirtualGatewayIpPacket(pkt, srcIp, dstIp)) {
                ipPacketReactiveProcessor(context, ethPkt, srcCp, srcIp, dstIp, ipProto);
                // TODO: add ReactiveRouting for dstIp to srcIp with discovered egressCp as srcCp
            }
        }
    }

    /**
     * handle Packet with dstIp=virtualGatewayIpAddresses.
     * returns true(handled) or false(not for virtual gateway)
     */
    private boolean checkVirtualGatewayIpPacket(InboundPacket pkt, IpAddress srcIp, IpAddress dstIp) {
        Ethernet ethPkt = pkt.parsed();  // assume valid

        MacAddress mac = simpleFabric.vMacForIp(dstIp);
        if (mac == null || !simpleFabric.isVirtualGatewayMac(ethPkt.getDestinationMAC())) {
            /* Destination MAC should be any of virtual gateway macs */
            return false;
        } else if (dstIp.isIp4()) {
            IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
            if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_ICMP) {
                ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();

                if (icmpPacket.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
                    log.info("IPV4 ICMP ECHO request to virtual gateway: "
                              + "srcIp={} dstIp={} proto={}", srcIp, dstIp, ipv4Packet.getProtocol());
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .setOutput(pkt.receivedFrom().port()).build();
                    OutboundPacket packet =
                        new DefaultOutboundPacket(pkt.receivedFrom().deviceId(), treatment,
                                ByteBuffer.wrap(icmpPacket.buildIcmpReply(pkt.parsed()).serialize()));
                    packetService.emit(packet);
                    return true;
                }
            }
            log.warn("IPV4 packet to virtual gateway dropped: "
                     + "srcIp={} dstIp={} proto={}", srcIp, dstIp, ipv4Packet.getProtocol());
            return true;

        } else if (dstIp.isIp6()) {
            // TODO: not tested yet (2017-07-20)
            IPv6 ipv6Packet = (IPv6) ethPkt.getPayload();
            if (ipv6Packet.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                ICMP6 icmp6Packet = (ICMP6) ipv6Packet.getPayload();

                if (icmp6Packet.getIcmpType() == ICMP6.ECHO_REQUEST) {
                    log.info("IPV6 ICMP6 ECHO request to virtual gateway: srcIp={} dstIp={} nextHeader={}",
                             srcIp, dstIp, ipv6Packet.getNextHeader());
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .setOutput(pkt.receivedFrom().port()).build();
                    OutboundPacket packet =
                        new DefaultOutboundPacket(pkt.receivedFrom().deviceId(), treatment,
                                ByteBuffer.wrap(icmp6Packet.buildIcmp6Reply(pkt.parsed()).serialize()));
                    packetService.emit(packet);
                    return true;
                }
            }
            log.warn("IPV6 packet to virtual gateway dropped: srcIp={} dstIp={} nextHeader={}",
                     srcIp, dstIp, ipv6Packet.getNextHeader());
            return true;

        }
        return false;  // unknown traffic
    }

    /**
     * Routes packet reactively.
     */
    private void ipPacketReactiveProcessor(PacketContext context, Ethernet ethPkt, ConnectPoint srcCp,
                                           IpAddress srcIp, IpAddress dstIp, byte ipProto) {
        /* check reactive handling and forward packet */
        log.trace("ip packet: srcCp={} srcIp={} dstIp={} ipProto={}",
                  srcCp, srcIp, dstIp, ipProto);

        EncapsulationType encap = EncapsulationType.NONE;

        // prefix and nextHop for local Subnet
        IpPrefix srcPrefix = srcIp.toIpPrefix();
        IpPrefix dstPrefix = dstIp.toIpPrefix();
        IpAddress srcNextHop = srcIp;
        IpAddress dstNextHop = dstIp;
        MacAddress treatmentSrcMac = ethPkt.getDestinationMAC();
        int borderRoutePrefixLength = 0;
        boolean updateMac = simpleFabric.isVirtualGatewayMac(ethPkt.getDestinationMAC());

        // check subnet local or route
        FabricSubnet srcSubnet = simpleFabric.fabricSubnet(srcIp);
        if (srcSubnet == null) {
            FabricRoute route = simpleFabric.fabricRoute(srcIp);
            if (route == null) {
                log.warn("unknown srcIp; drop: srcCp={} srcIp={} dstIp={} ipProto={}",
                         srcCp, srcIp, dstIp, ipProto);
                return;
            }
            srcPrefix = route.prefix();
            srcNextHop = route.nextHop();
            borderRoutePrefixLength = route.prefix().prefixLength();
        }
        FabricSubnet dstSubnet = simpleFabric.fabricSubnet(dstIp);
        if (dstSubnet == null) {
            FabricRoute route = simpleFabric.fabricRoute(dstIp);
            if (route == null) {
                log.warn("unknown dstIp; drop: srcCp={} srcIp={} dstIp={} ipProto={}",
                         srcCp, srcIp, dstIp, ipProto);
                return;
            }
            dstPrefix = route.prefix();
            dstNextHop = route.nextHop();
            borderRoutePrefixLength = route.prefix().prefixLength();
        }

        if (dstSubnet != null) {
            // destination is local subnet ip
            if (ALLOW_ETH_ADDRESS_SELECTOR && dstSubnet.equals(srcSubnet)) {
                // NOTE: if ALLOW_ETH_ADDRESS_SELECTOR=false; isForward is always false
                FabricNetwork fabricNetwork = simpleFabric.fabricNetwork(dstSubnet.networkName());
                treatmentSrcMac = ethPkt.getSourceMAC();
                if (fabricNetwork != null && fabricNetwork.isForward()) {
                    // NOTE: no reactive route action but do forward packet for L2Forward do not handle packet
                    // update mac only if dstMac is virtualGatewayMac, else assume valid mac already for the l2 network
                    log.info("LOCAL FORWARD ONLY: "
                             + "srcCp={} srcIp={} dstIp={} srcMac={} dstMac={} vlanId={} ipProto={} updateMac={}",
                             context.inPacket().receivedFrom(),
                             srcIp, dstIp, ethPkt.getSourceMAC(), ethPkt.getDestinationMAC(),
                             ethPkt.getVlanID(), ipProto, updateMac);
                    forwardPacketToDstIp(context, dstIp, treatmentSrcMac, updateMac);
                    return;
                }
            }
            encap = dstSubnet.encapsulation();
            if (encap == EncapsulationType.NONE && srcSubnet != null) {
               encap = srcSubnet.encapsulation();
            }
        } else {
            // destination is external network
            if (srcSubnet == null) {
                // both are externel network
                log.warn("INVALID PACKET: srcIp and dstIp are both NON-LOCAL: "
                         + "srcCP={} srcIp={} dstIp={} srcMac={} dstMac={} vlanId={} ipProto={} updateMac={}",
                         context.inPacket().receivedFrom(),
                         srcIp, dstIp, ethPkt.getSourceMAC(), ethPkt.getDestinationMAC(),
                         ethPkt.getVlanID(), ipProto, updateMac);
                return;
            }
            encap = srcSubnet.encapsulation();
        }

        log.info("REGI AND FORWARD: "
                 + "srcCP={} srcIp={} dstIp={} srcMac={} dstMac={} vlanId={} ipProto={} updateMac={}",
                 context.inPacket().receivedFrom(),
                 srcIp, dstIp, ethPkt.getSourceMAC(), ethPkt.getDestinationMAC(),
                 ethPkt.getVlanID(), ipProto, updateMac);
        setUpConnectivity(srcCp, ipProto, srcPrefix, dstPrefix, dstNextHop, treatmentSrcMac, encap, updateMac,
                          dstSubnet != null, borderRoutePrefixLength);
        forwardPacketToDstIp(context, dstNextHop, treatmentSrcMac, updateMac);
    }

    /**
     * Emits the specified packet onto the network.
     */
    private void forwardPacketToDstIp(PacketContext context, IpAddress nextHopIp,
                                      MacAddress srcMac, boolean updateMac) {
        Set<Host> hosts = hostService.getHostsByIp(nextHopIp);
        Host dstHost;
        if (!hosts.isEmpty()) {
            dstHost = hosts.iterator().next();
        } else {
            // NOTE: hostService.requestMac(nextHopIp); NOT IMPLEMENTED in ONOS HostManager.java; do it myself
            log.warn("forward packet nextHopIp host_mac unknown: nextHopIp={}", nextHopIp);
            hostService.startMonitoringIp(nextHopIp);
            simpleFabric.requestMac(nextHopIp);
            // CONSIDER: make flood on all port of the dstHost's DefaultFabricNetwork
            return;
        }
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(dstHost.location().port()).build();
        OutboundPacket outPacket;
        if (updateMac) {
            // NOTE: eth address update by treatment is NOT applied, so update mac myself
            outPacket = new DefaultOutboundPacket(dstHost.location().deviceId(), treatment,
                                ByteBuffer.wrap(context.inPacket().parsed()
                                          .setSourceMACAddress(srcMac)
                                          .setDestinationMACAddress(dstHost.mac()).serialize()));
        } else {
            outPacket = new DefaultOutboundPacket(dstHost.location().deviceId(), treatment,
                                context.inPacket().unparsed());
        }
        // be quiet on normal situation
        log.info("forward packet: nextHopIP={} srcCP={} dstCP={}",
                 nextHopIp, context.inPacket().receivedFrom(), dstHost.location());
        packetService.emit(outPacket);
    }

    /**
     * Update intents for connectivity.
     *
     * ToHost: dstPrefix = dstHostIp.toIpPrefix(), nextHopIp = destHostIp
     * ToInternet: dstPrefix = route.prefix(), nextHopIp = route.nextHopIp
     * returns intent submitted or not
     */
    private boolean setUpConnectivity(ConnectPoint srcCp, byte ipProto, IpPrefix srcPrefix, IpPrefix dstPrefix,
                                      IpAddress nextHopIp, MacAddress treatmentSrcMac,
                                      EncapsulationType encap, boolean updateMac,
                                      boolean isDstLocalSubnet, int borderRoutePrefixLength) {
        if (!(simpleFabric.fabricNetwork(srcCp, VlanId.NONE) != null ||
             (REACTIVE_ALLOW_LINK_CP && !linkService.getIngressLinks(srcCp).isEmpty()))) {
            log.warn("NO REGI for srcCp not in DefaultFabricNetwork; srcCp={} srcPrefix={} dstPrefix={} nextHopIp={}",
                      srcCp, srcPrefix, dstPrefix, nextHopIp);
            return false;
        }

        MacAddress nextHopMac = null;
        ConnectPoint egressPoint = null;
        for (Host host : hostService.getHostsByIp(nextHopIp)) {
            if (host.mac() != null) {
                nextHopMac = host.mac();
                egressPoint = host.location();
                break;
            }
        }
        if (nextHopMac == null || egressPoint == null) {
            log.info("NO REGI for unknown nextHop Cp and Mac: srcPrefix={} dstPrefix={} nextHopIp={}",
                     srcPrefix, dstPrefix, nextHopIp);
            hostService.startMonitoringIp(nextHopIp);
            simpleFabric.requestMac(nextHopIp);
            return false;
        }
        TrafficTreatment treatment;
        if (updateMac && ALLOW_ETH_ADDRESS_SELECTOR) {
            treatment = generateSetMacTreatment(nextHopMac, treatmentSrcMac);
        } else {
            treatment = DefaultTrafficTreatment.builder().build();
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (dstPrefix.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            if (REACTIVE_SINGLE_TO_SINGLE && srcPrefix.prefixLength() > 0) {
                selector.matchIPSrc(srcPrefix);
            }
            if (dstPrefix.prefixLength() > 0) {
                selector.matchIPDst(dstPrefix);
            }
            if (ipProto != 0 && REACTIVE_MATCH_IP_PROTO) {
                selector.matchIPProtocol(ipProto);
            }
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            if (REACTIVE_SINGLE_TO_SINGLE && srcPrefix.prefixLength() > 0) {
                selector.matchIPv6Src(srcPrefix);
            }
            if (dstPrefix.prefixLength() > 0) {
                selector.matchIPv6Dst(dstPrefix);
            }
            if (ipProto != 0 && REACTIVE_MATCH_IP_PROTO) {
                selector.matchIPProtocol(ipProto);
            }
        }

        Key key;
        String keyProtoTag = "";
        if (REACTIVE_MATCH_IP_PROTO) {
            keyProtoTag = "-p" + ipProto;
        }
        if (REACTIVE_SINGLE_TO_SINGLE) {
            // allocate intent per (srcPrefix, dstPrefix)
            key = Key.of(srcPrefix.toString() + "-to-" + dstPrefix.toString() + keyProtoTag, appId);
        } else {
            // allocate intent per (srcDeviceId, dstPrefix)
            key = Key.of(srcCp.deviceId().toString() + "-to-" +  dstPrefix.toString() + keyProtoTag, appId);
        }

        // check and merge already existing ingress points
        Set<FilteredConnectPoint> ingressPoints = new HashSet<>();
        MultiPointToSinglePointIntent existingIntent = (MultiPointToSinglePointIntent) intentService.getIntent(key);
        if (existingIntent != null) {
            ingressPoints.addAll(existingIntent.filteredIngressPoints());
            if (!ingressPoints.add(new FilteredConnectPoint(srcCp))  // alread exists and dst not changed
                    && egressPoint.equals(existingIntent.egressPoint())
                    && treatment.equals(existingIntent.treatment())) {
                log.warn("srcCP is already in mp2p intent: srcPrefix={} dstPrefix={} srcCp={}",
                         srcPrefix, dstPrefix, srcCp);
                return false;
            }
            log.info("update mp2p intent: srcPrefix={} dstPrefix={} srcCp={}",
                     srcPrefix, dstPrefix, srcCp);
        } else {
            log.info("create mp2p intent: srcPrefix={} dstPrefix={} srcCp={}",
                     srcPrefix, dstPrefix, srcCp);
            ingressPoints.add(new FilteredConnectPoint(srcCp));
        }

        // priority for forwarding case
        int priority = reactivePriority(true, isDstLocalSubnet, borderRoutePrefixLength);

        MultiPointToSinglePointIntent newIntent = MultiPointToSinglePointIntent.builder()
            .key(key)
            .appId(appId)
            .selector(selector.build())
            .treatment(treatment)
            .filteredIngressPoints(ingressPoints)
            .filteredEgressPoint(new FilteredConnectPoint(egressPoint))
            .priority(priority)
            .constraints(buildConstraints(reactiveConstraints, encap))
            .build();
        log.info("submit mp2p intent: srcPrefix={} dstPrefix={} srcCp={} "
                 + "newIntent={} nextHopIp={} nextHopMac={} priority={}",
                 srcPrefix, dstPrefix, ingressPoints, newIntent, nextHopIp, nextHopMac, priority);
        toBePurgedIntentKeys.remove(newIntent.key());
        intentService.submit(newIntent);
        return true;
    }

    // generate treatment to target
    private TrafficTreatment generateSetMacTreatment(MacAddress dstMac, MacAddress srcMac) {
        return DefaultTrafficTreatment.builder()
                   // NOTE: Cisco Switch requires both src and dst mac set
                   .setEthDst(dstMac)
                   .setEthSrc(srcMac)
                   .build();
    }

    // monitor border peers for routeService lookup to be effective
    private void monitorBorderPeers() {
        for (FabricRoute route : simpleFabric.fabricRoutes()) {
            hostService.startMonitoringIp(route.nextHop());
            simpleFabric.requestMac(route.nextHop());
        }
    }

    // priority calculator
    private int reactivePriority(boolean isForward, boolean isDstLocalSubnet, int borderRoutePrefixLength) {
        if (isDstLocalSubnet) {  // -> dst:localSubnet
            if (isForward) {
                return PRI_REACTIVE_LOCAL_FORWARD;
            } else {  // isInterncept
                return PRI_REACTIVE_LOCAL_INTERCEPT;
            }
        } else {  // -> dst:boarderRouteNextHop
            int offset;
            if (isForward) {
                offset = PRI_REACTIVE_BORDER_FORWARD;
            } else {  // isIntercept
                offset = PRI_REACTIVE_BORDER_INTERCEPT;
            }
           return PRI_REACTIVE_BORDER_BASE
                  + borderRoutePrefixLength * PRI_REACTIVE_BORDER_STEP + offset;
        }
    }

    // constraints generator
    private List<Constraint> buildConstraints(List<Constraint> constraints, EncapsulationType encap) {
        if (!encap.equals(EncapsulationType.NONE)) {
            List<Constraint> newConstraints = new ArrayList<>(constraints);
            constraints.stream()
                .filter(c -> c instanceof EncapsulationConstraint)
                .forEach(newConstraints::remove);
            newConstraints.add(new EncapsulationConstraint(encap));
            return ImmutableList.copyOf(newConstraints);
        }
        return constraints;
    }

    // Dump Cli Handler
    private void dump(String subject, PrintStream out) {
        if ("intents".equals(subject)) {
            out.println("Routing Route Intents:\n");
            for (Intent entry : intentService.getIntents()) {
                if (appId.equals(entry.appId())) {
                    MultiPointToSinglePointIntent intent = (MultiPointToSinglePointIntent) entry;
                    out.println("    " + intent.key().toString()
                                + " to " + intent.egressPoint().toString()
                                + " set " + intent.treatment().immediate().toString()
                                + " from " + intent.ingressPoints().toString());
                }
            }
            out.println("");

            out.println("Routing Intercept Flow Rules:\n");
            List<FlowRule> rules = new ArrayList(interceptFlowRules);
            Collections.sort(rules, new Comparator<FlowRule>() {
                    @Override
                    public int compare(FlowRule a, FlowRule b) {
                        int r = a.deviceId().toString().compareTo(b.deviceId().toString());
                        return (r != 0) ? r : Integer.compare(b.priority(), a.priority());  // descending on priority
                    }
                });
            for (FlowRule rule : rules) {
                out.println("    device=" + rule.deviceId().toString()
                          + " priority=" + rule.priority()
                          + " selector=" + rule.selector().criteria().toString());
            }
            out.println("");
            out.println("Routing Intents to Be Purged:\n");
            for (Key key: toBePurgedIntentKeys) {
                out.println("    " + key.toString());
            }
            out.println("");

        } else if ("reactive-intents".equals(subject)) {
            for (Intent entry : intentService.getIntents()) {
                if (appId.equals(entry.appId())) {
                    MultiPointToSinglePointIntent intent = (MultiPointToSinglePointIntent) entry;
                    out.println(intent.key().toString()
                                + " to " + intent.egressPoint().toString()
                                + " set " + intent.treatment().immediate().toString()
                                + " from " + intent.ingressPoints().toString());
                }
            }
        }
    }

    // Listener
    private class InternalSimpleFabricListener implements SimpleFabricListener {
        @Override
        public void event(SimpleFabricEvent event) {
            switch (event.type()) {
            case SIMPLE_FABRIC_UPDATED:
                refreshIntercepts();
                refreshRouteIntents();
                checkIntentsPurge();
                break;
            case SIMPLE_FABRIC_FLUSH:
                withdrawAllReactiveIntents();
                checkIntentsPurge();
                break;
            case SIMPLE_FABRIC_IDLE:
                refreshIntercepts();
                refreshRouteIntents();
                checkIntentsPurge();
                monitorBorderPeers();
                break;
            case SIMPLE_FABRIC_DUMP:
                dump(event.subject(), event.out());
                break;
            default:
                break;
            }
        }
    }

}

