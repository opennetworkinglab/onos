/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.sdnip;

import java.util.ArrayList;
import java.util.List;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.sdnip.bgp.BgpConstants;
import org.onlab.onos.sdnip.config.BgpPeer;
import org.onlab.onos.sdnip.config.BgpSpeaker;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.onos.sdnip.config.InterfaceAddress;
import org.onlab.onos.sdnip.config.SdnIpConfigService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connectivity requirements between peers.
 */
public class PeerConnectivityManager {

    private static final Logger log = LoggerFactory.getLogger(
            PeerConnectivityManager.class);

    private final SdnIpConfigService configService;
    private final InterfaceService interfaceService;
    private final IntentService intentService;

    private final ApplicationId appId;

    /**
     * Creates a new PeerConnectivityManager.
     *
     * @param appId             the application ID
     * @param configService     the SDN-IP config service
     * @param interfaceService  the interface service
     * @param intentService     the intent service
     */
    public PeerConnectivityManager(ApplicationId appId,
                                   SdnIpConfigService configService,
                                   InterfaceService interfaceService,
                                   IntentService intentService) {
        this.appId = appId;
        this.configService = configService;
        this.interfaceService = interfaceService;
        this.intentService = intentService;
    }

    /**
     * Starts the peer connectivity manager.
     */
    public void start() {
        // TODO are any of these errors?
        if (interfaceService.getInterfaces().isEmpty()) {

            log.warn("The interface in configuration file is empty. "
                             + "Thus, the SDN-IP application can not be started.");
        } else if (configService.getBgpPeers().isEmpty()) {

            log.warn("The BGP peer in configuration file is empty."
                             + "Thus, the SDN-IP application can not be started.");
        } else if (configService.getBgpSpeakers() == null) {

            log.error("The BGP speaker in configuration file is empty. "
                              + "Thus, the SDN-IP application can not be started.");
            return;
        }

        setUpConnectivity();
    }

    /**
     * Sets up paths to establish connectivity between all internal
     * {@link BgpSpeaker}s and all external {@link BgpPeer}s.
     */
    private void setUpConnectivity() {
        for (BgpSpeaker bgpSpeaker : configService.getBgpSpeakers()
                .values()) {
            log.debug("Start to set up BGP paths for BGP speaker: {}",
                      bgpSpeaker);

            for (BgpPeer bgpPeer : configService.getBgpPeers().values()) {

                log.debug("Start to set up BGP paths between BGP speaker: {} "
                                  + "to BGP peer: {}", bgpSpeaker, bgpPeer);

                buildPeerIntents(bgpSpeaker, bgpPeer);
            }
        }
    }

    /**
     * Builds the required intents between a given internal BGP speaker and
     * external BGP peer.
     *
     * @param bgpSpeaker the BGP speaker
     * @param bgpPeer the BGP peer
     */
    private void buildPeerIntents(BgpSpeaker bgpSpeaker, BgpPeer bgpPeer) {
        List<Intent> intents = new ArrayList<Intent>();

        ConnectPoint bgpdConnectPoint = bgpSpeaker.connectPoint();

        List<InterfaceAddress> interfaceAddresses =
                bgpSpeaker.interfaceAddresses();

        Interface peerInterface = interfaceService.getInterface(
                bgpPeer.connectPoint());

        if (peerInterface == null) {
            log.error("No interface found for peer {}", bgpPeer.ipAddress());
            return;
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
            return;
        }

        IpAddress bgpdPeerAddress = bgpPeer.ipAddress();
        ConnectPoint bgpdPeerConnectPoint = peerInterface.connectPoint();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .build();

        TrafficSelector selector;

        // install intent for BGP path from BGPd to BGP peer matching
        // destination TCP port 179
        selector = buildSelector(IPv4.PROTOCOL_TCP,
                                 bgpdAddress,
                                 bgpdPeerAddress,
                                 null,
                                 (short) BgpConstants.BGP_PORT);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdConnectPoint, bgpdPeerConnectPoint));

        // install intent for BGP path from BGPd to BGP peer matching
        // source TCP port 179
        selector = buildSelector(IPv4.PROTOCOL_TCP,
                                 bgpdAddress,
                                 bgpdPeerAddress,
                                 (short) BgpConstants.BGP_PORT,
                                 null);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdConnectPoint, bgpdPeerConnectPoint));

        // install intent for reversed BGP path from BGP peer to BGPd
        // matching destination TCP port 179
        selector = buildSelector(IPv4.PROTOCOL_TCP,
                                 bgpdPeerAddress,
                                 bgpdAddress,
                                 null,
                                 (short) BgpConstants.BGP_PORT);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdPeerConnectPoint, bgpdConnectPoint));

        // install intent for reversed BGP path from BGP peer to BGPd
        // matching source TCP port 179
        selector = buildSelector(IPv4.PROTOCOL_TCP,
                                 bgpdPeerAddress,
                                 bgpdAddress,
                                 (short) BgpConstants.BGP_PORT,
                                 null);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdPeerConnectPoint, bgpdConnectPoint));

        // install intent for ICMP path from BGPd to BGP peer
        selector = buildSelector(IPv4.PROTOCOL_ICMP,
                                 bgpdAddress,
                                 bgpdPeerAddress,
                                 null,
                                 null);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdConnectPoint, bgpdPeerConnectPoint));

        // install intent for reversed ICMP path from BGP peer to BGPd
        selector = buildSelector(IPv4.PROTOCOL_ICMP,
                                 bgpdPeerAddress,
                                 bgpdAddress,
                                 null,
                                 null);

        intents.add(new PointToPointIntent(appId, selector, treatment,
                               bgpdPeerConnectPoint, bgpdConnectPoint));

        // Submit all the intents.
        // TODO submit as a batch
        for (Intent intent : intents) {
            intentService.submit(intent);
        }
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
                                 IpAddress dstIp, Short srcTcpPort, Short dstTcpPort) {
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(ipProto)
                .matchIPSrc(IpPrefix.valueOf(srcIp,
                        IpPrefix.MAX_INET_MASK_LENGTH))
                .matchIPDst(IpPrefix.valueOf(dstIp,
                        IpPrefix.MAX_INET_MASK_LENGTH));

        if (srcTcpPort != null) {
            builder.matchTcpSrc(srcTcpPort);
        }

        if (dstTcpPort != null) {
            builder.matchTcpDst(dstTcpPort);
        }

        return builder.build();
    }

}
