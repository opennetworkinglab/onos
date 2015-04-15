/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.sdnip;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.InterfaceAddress;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Manages the connectivity requirements between peers.
 */
public class PeerConnectivityManager {
    private static final int PRIORITY_OFFSET = 1000;

    private static final Logger log = LoggerFactory.getLogger(
            PeerConnectivityManager.class);

    private static final short BGP_PORT = 179;

    private final IntentSynchronizer intentSynchronizer;
    private final RoutingConfigurationService configService;

    private final ApplicationId appId;

    /**
     * Creates a new PeerConnectivityManager.
     *
     * @param appId              the application ID
     * @param intentSynchronizer the intent synchronizer
     * @param configService      the SDN-IP config service
     */
    public PeerConnectivityManager(ApplicationId appId,
                                   IntentSynchronizer intentSynchronizer,
                                   RoutingConfigurationService configService) {
        this.appId = appId;
        this.intentSynchronizer = intentSynchronizer;
        this.configService = configService;
    }

    /**
     * Starts the peer connectivity manager.
     */
    public void start() {
        if (configService.getInterfaces().isEmpty()) {
            log.warn("No interfaces found in configuration file");
        }

        if (configService.getBgpPeers().isEmpty()) {
            log.warn("No BGP peers found in configuration file");
        }

        if (configService.getBgpSpeakers().isEmpty()) {
            log.error("No BGP speakers found in configuration file");
        }

        setUpConnectivity();
    }

    /**
     * Stops the peer connectivity manager.
     */
    public void stop() {
    }

    /**
     * Sets up paths to establish connectivity between all internal
     * {@link BgpSpeaker}s and all external {@link BgpPeer}s.
     */
    private void setUpConnectivity() {
        List<PointToPointIntent> intents = new ArrayList<>();

        for (BgpSpeaker bgpSpeaker : configService.getBgpSpeakers()
                .values()) {
            log.debug("Start to set up BGP paths for BGP speaker: {}",
                      bgpSpeaker);

            for (BgpPeer bgpPeer : configService.getBgpPeers().values()) {

                log.debug("Start to set up BGP paths between BGP speaker: {} "
                                  + "to BGP peer: {}", bgpSpeaker, bgpPeer);

                intents.addAll(buildPeerIntents(bgpSpeaker, bgpPeer));
            }
        }

        // Submit all the intents.
        intentSynchronizer.submitPeerIntents(intents);
    }

    /**
     * Builds the required intents between a given internal BGP speaker and
     * external BGP peer.
     *
     * @param bgpSpeaker the BGP speaker
     * @param bgpPeer the BGP peer
     * @return the intents to install
     */
    private Collection<PointToPointIntent> buildPeerIntents(
            BgpSpeaker bgpSpeaker,
            BgpPeer bgpPeer) {
        List<PointToPointIntent> intents = new ArrayList<>();

        ConnectPoint bgpdConnectPoint = bgpSpeaker.connectPoint();

        List<InterfaceAddress> interfaceAddresses =
                bgpSpeaker.interfaceAddresses();

        Interface peerInterface = configService.getInterface(
                bgpPeer.connectPoint());

        if (peerInterface == null) {
            log.error("No interface found for peer {}", bgpPeer.ipAddress());
            return intents;
        }

        IpAddress bgpdAddress = null;
        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
            if (interfaceAddress.connectPoint().equals(
                    peerInterface.connectPoint())) {
                bgpdAddress = interfaceAddress.ipAddress();
                break;
            }
        }
        if (bgpdAddress == null) {
            log.debug("No IP address found for peer {} on interface {}",
                    bgpPeer, bgpPeer.connectPoint());
            return intents;
        }

        IpAddress bgpdPeerAddress = bgpPeer.ipAddress();
        ConnectPoint bgpdPeerConnectPoint = peerInterface.connectPoint();

        if (bgpdAddress.version() != bgpdPeerAddress.version()) {
            return intents;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        TrafficSelector selector;

        byte tcpProtocol;
        byte icmpProtocol;

        if (bgpdAddress.isIp4()) {
            tcpProtocol = IPv4.PROTOCOL_TCP;
            icmpProtocol = IPv4.PROTOCOL_ICMP;
        } else {
            tcpProtocol = IPv6.PROTOCOL_TCP;
            icmpProtocol = IPv6.PROTOCOL_ICMP6;
        }

        // Path from BGP speaker to BGP peer matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                bgpdAddress,
                bgpdPeerAddress,
                null,
                BGP_PORT);

        int priority = PRIORITY_OFFSET;

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdConnectPoint)
                .egressPoint(bgpdPeerConnectPoint)
                .priority(priority)
                .build());

        // Path from BGP speaker to BGP peer matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                bgpdAddress,
                bgpdPeerAddress,
                BGP_PORT,
                null);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdConnectPoint)
                .egressPoint(bgpdPeerConnectPoint)
                .priority(priority)
                .build());

        // Path from BGP peer to BGP speaker matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                bgpdPeerAddress,
                bgpdAddress,
                null,
                BGP_PORT);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdPeerConnectPoint)
                .egressPoint(bgpdConnectPoint)
                .priority(priority)
                .build());

        // Path from BGP peer to BGP speaker matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                bgpdPeerAddress,
                bgpdAddress,
                BGP_PORT,
                null);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdPeerConnectPoint)
                .egressPoint(bgpdConnectPoint)
                .priority(priority)
                .build());

        // ICMP path from BGP speaker to BGP peer
        selector = buildSelector(icmpProtocol,
                bgpdAddress,
                bgpdPeerAddress,
                null,
                null);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdConnectPoint)
                .egressPoint(bgpdPeerConnectPoint)
                .priority(priority)
                .build());

        // ICMP path from BGP peer to BGP speaker
        selector = buildSelector(icmpProtocol,
                bgpdPeerAddress,
                bgpdAddress,
                null,
                null);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(bgpdPeerConnectPoint)
                .egressPoint(bgpdConnectPoint)
                .priority(priority)
                .build());

        return intents;
    }

    /**
     * Builds a traffic selector based on the set of input parameters.
     *
     * @param ipProto IP protocol
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param srcTcpPort source TCP port, or null if shouldn't be set
     * @param dstTcpPort destination TCP port, or null if shouldn't be set
     * @return the new traffic selector
     */
    private TrafficSelector buildSelector(byte ipProto, IpAddress srcIp,
                                          IpAddress dstIp, Short srcTcpPort,
                                          Short dstTcpPort) {
        TrafficSelector.Builder builder = null;

        if (dstIp.isIp4()) {
            builder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(ipProto)
                    .matchIPSrc(IpPrefix.valueOf(srcIp,
                            IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchIPDst(IpPrefix.valueOf(dstIp,
                            IpPrefix.MAX_INET_MASK_LENGTH));
        } else {
            builder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV6)
                    .matchIPProtocol(ipProto)
                    .matchIPv6Src(IpPrefix.valueOf(srcIp,
                            IpPrefix.MAX_INET6_MASK_LENGTH))
                    .matchIPv6Dst(IpPrefix.valueOf(dstIp,
                            IpPrefix.MAX_INET6_MASK_LENGTH));
        }

        if (srcTcpPort != null) {
            builder.matchTcpSrc(srcTcpPort);
        }

        if (dstTcpPort != null) {
            builder.matchTcpDst(dstTcpPort);
        }

        return builder.build();
    }

}
