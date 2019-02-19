/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.mfwd.impl;

import org.onosproject.net.FilteredConnectPoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The multicast forwarding component.  This component is responsible for
 * handling live multicast traffic by modifying multicast state and forwarding
 * packets that do not yet have state installed.
 */
@Component(immediate = true)
public class McastForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MulticastRouteService mcastRouteManager;

    protected McastIntentManager mcastIntentManager;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private static ApplicationId appId;

    /**
     * Active MulticastForwardingIntent.
     */
    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.mfwd");

        mcastIntentManager = new McastIntentManager();
        mcastRouteManager.addListener(mcastIntentManager);

        packetService.addProcessor(processor, PacketProcessor.director(2));

        // Build a traffic selector for all multicast traffic
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(IpPrefix.IPV4_MULTICAST_PREFIX);

        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        log.info("Started");
    }

    /**
     * Deactivate Multicast Forwarding Intent.
     */
    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        mcastRouteManager.removeListener(mcastIntentManager);
        mcastIntentManager.withdrawAllIntents();
        processor = null;
        log.info("Stopped");
    }

    /**
     * Get the application ID, used by the McastIntentManager.
     *
     * @return the application ID
     */
    public static ApplicationId getAppId() {
        return appId;
    }

    /**
     * Forward the packet to it's multicast destinations.
     *
     * @param context The packet context
     * @param egressList The list of egress ports which the multicast packet is intended for.
     */
    private void forwardPacketToDst(PacketContext context, ArrayList<ConnectPoint> egressList) {

        // Send the pack out each of the respective egress ports
        for (ConnectPoint egress : egressList) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(egress.port()).build();

            OutboundPacket packet = new DefaultOutboundPacket(
                    egress.deviceId(),
                    treatment,
                    context.inPacket().unparsed());

            packetService.emit(packet);
        }
    }

    public static McastRoute createStaticRoute(String source, String group) {
        checkNotNull(source, "Must provide a source");
        checkNotNull(group, "Must provide a group");
        IpAddress ipSource = IpAddress.valueOf(source);
        IpAddress ipGroup = IpAddress.valueOf(group);
        return createStaticcreateRoute(ipSource, ipGroup);
    }

    public static McastRoute createStaticcreateRoute(IpAddress source, IpAddress group) {
        checkNotNull(source, "Must provide a source");
        checkNotNull(group, "Must provide a group");
        McastRoute.Type type = McastRoute.Type.STATIC;
        return new McastRoute(source, group, type);
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */

    private class ReactivePacketProcessor implements PacketProcessor {
        /**
         * Process incoming packets.
         *
         * @param context packet processing context
         */
        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4 &&
                    ethPkt.getEtherType() != Ethernet.TYPE_IPV6) {
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
                // Ignore ipv6 at the moment.
                return;
            }

            IPv4 ip = (IPv4) ethPkt.getPayload();
            IpAddress saddr = Ip4Address.valueOf(ip.getSourceAddress());
            IpAddress gaddr = IpAddress.valueOf(ip.getDestinationAddress());

            log.debug("Packet ({}, {}) has been punted\n" +
                            "\tingress port: {}\n",
                    saddr.toString(),
                    gaddr.toString(),
                    context.inPacket().receivedFrom().toString());

            // Don't allow PIM/IGMP packets to be handled here.
            byte proto = ip.getProtocol();
            if (proto == IPv4.PROTOCOL_PIM || proto == IPv4.PROTOCOL_IGMP) {
                return;
            }

            IpPrefix spfx = IpPrefix.valueOf(saddr, 32);
            IpPrefix gpfx = IpPrefix.valueOf(gaddr, 32);

            // TODO do we want to add a type for Mcast?
            McastRoute mRoute = new McastRoute(saddr, gaddr, McastRoute.Type.STATIC);

            ConnectPoint ingress = mcastRouteManager.fetchSource(mRoute);

            // An ingress port already exists. Log error.
            if (ingress != null) {
                log.error(McastForwarding.class.getSimpleName() + " received packet which already has a route.");
                return;
            } else {
                //add ingress port
                mcastRouteManager.addSource(mRoute, pkt.receivedFrom());
            }

            ArrayList<ConnectPoint> egressList = (ArrayList<ConnectPoint>) mcastRouteManager.fetchSinks(mRoute);
            //If there are no egress ports set return, otherwise forward the packets to their expected port.
            if (egressList.isEmpty()) {
                return;
            }

            // Send the pack out each of the egress devices & port
            forwardPacketToDst(context, egressList);
        }

    }

    private class McastIntentManager implements McastListener {

        private Map<McastRoute, Key> intentHashMap;

        public McastIntentManager() {
            intentHashMap = new HashMap<>();
        }

        @Override
        public void event(McastEvent event) {
            McastRoute route = event.subject().route();
            if (intentHashMap.containsKey(route)) {
                withdrawIntent(intentHashMap.get(route));
            }
            Key routeKey = setIntent(route);
            intentHashMap.put(route, routeKey);
        }

        private Key setIntent(McastRoute route) {

            ConnectPoint ingressPoint = mcastRouteManager.fetchSource(route);
            Set<FilteredConnectPoint> filteredEgressPoints = new HashSet<>();
            mcastRouteManager.fetchSinks(route).iterator()
                .forEachRemaining(point -> filteredEgressPoints.add(new FilteredConnectPoint(point)));


            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

            if (ingressPoint == null) {
                log.warn("Can't set intent without an ingress or egress connect points");
                return null;
            }

            selector.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(route.group().toIpPrefix())
                    .matchIPSrc(route.source().toIpPrefix());

            SinglePointToMultiPointIntent.Builder builder = SinglePointToMultiPointIntent.builder()
                    .appId(appId)
                    .selector(selector.build())
                    .treatment(treatment)
                    .filteredIngressPoint(new FilteredConnectPoint(ingressPoint));

            // allowing intent to be pushed without egress points means we can drop packets.
            if (!filteredEgressPoints.isEmpty()) {
                builder.filteredEgressPoints(filteredEgressPoints);
            }

            SinglePointToMultiPointIntent intent = builder.build();
            intentService.submit(intent);

            return intent.key();
        }

        public void withdrawAllIntents() {
            for (Map.Entry<McastRoute, Key> entry : intentHashMap.entrySet()) {
                withdrawIntent(entry.getValue());
            }
            intentHashMap.clear();
        }

        public void withdrawIntent(Key key) {
            if (key == null) {
                // nothing to withdraw
                return;
            }
            Intent intent = intentService.getIntent(key);
            intentService.withdraw(intent);
        }
    }
}
