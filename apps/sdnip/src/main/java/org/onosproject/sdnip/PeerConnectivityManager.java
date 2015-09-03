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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the connectivity requirements between peers.
 */
public class PeerConnectivityManager {
    private static final int PRIORITY_OFFSET = 1000;

    private static final String SUFFIX_DST = "dst";
    private static final String SUFFIX_SRC = "src";
    private static final String SUFFIX_ICMP = "icmp";

    private static final Logger log = LoggerFactory.getLogger(
            PeerConnectivityManager.class);

    private static final short BGP_PORT = 179;

    private final IntentSynchronizationService intentSynchronizer;
    private final NetworkConfigService configService;
    private final InterfaceService interfaceService;

    private final ApplicationId appId;
    private final ApplicationId routerAppId;

    // Just putting something random here for now. Figure out exactly what
    // indexes we need when we start making use of them.
    private final Multimap<BgpConfig.BgpSpeakerConfig, PointToPointIntent> peerIntents;

    /**
     * Creates a new PeerConnectivityManager.
     *
     * @param appId              the application ID
     * @param intentSynchronizer the intent synchronizer
     * @param configService      the SDN-IP config service
     * @param interfaceService   the interface service
     * @param routerAppId        application ID
     */
    public PeerConnectivityManager(ApplicationId appId,
                                   IntentSynchronizationService intentSynchronizer,
                                   NetworkConfigService configService,
                                   ApplicationId routerAppId,
                                   InterfaceService interfaceService) {
        this.appId = appId;
        this.intentSynchronizer = intentSynchronizer;
        this.configService = configService;
        this.routerAppId = routerAppId;
        this.interfaceService = interfaceService;

        peerIntents = HashMultimap.create();
    }

    /**
     * Starts the peer connectivity manager.
     */
    public void start() {
        setUpConnectivity();
    }

    /**
     * Stops the peer connectivity manager.
     */
    public void stop() {
    }

    /**
     * Sets up paths to establish connectivity between all internal
     * BGP speakers and external BGP peers.
     */
    private void setUpConnectivity() {
        BgpConfig config = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        if (config == null) {
            log.warn("No BgpConfig found");
            return;
        }

        for (BgpConfig.BgpSpeakerConfig bgpSpeaker : config.bgpSpeakers()) {
            log.debug("Start to set up BGP paths for BGP speaker: {}",
                    bgpSpeaker);

            buildSpeakerIntents(bgpSpeaker).forEach(i -> {
                peerIntents.put(bgpSpeaker, i);
                intentSynchronizer.submit(i);
            });

        }
    }

    private Collection<PointToPointIntent> buildSpeakerIntents(BgpConfig.BgpSpeakerConfig speaker) {
        List<PointToPointIntent> intents = new ArrayList<>();

        for (IpAddress peerAddress : speaker.peers()) {
            Interface peeringInterface = interfaceService.getMatchingInterface(peerAddress);

            if (peeringInterface == null) {
                log.debug("No peering interface found for peer {} on speaker {}",
                        peerAddress, speaker);
                continue;
            }

            IpAddress peeringAddress = null;
            for (InterfaceIpAddress address : peeringInterface.ipAddresses()) {
                if (address.subnetAddress().contains(peerAddress)) {
                    peeringAddress = address.ipAddress();
                    break;
                }
            }

            checkNotNull(peeringAddress);

            intents.addAll(buildIntents(speaker.connectPoint(), peeringAddress,
                    peeringInterface.connectPoint(), peerAddress));
        }

        return intents;
    }

    /**
     * Builds the required intents between the two pairs of connect points and
     * IP addresses.
     *
     * @param portOne the first connect point
     * @param ipOne the first IP address
     * @param portTwo the second connect point
     * @param ipTwo the second IP address
     * @return the intents to install
     */
    private Collection<PointToPointIntent> buildIntents(ConnectPoint portOne,
                                                        IpAddress ipOne,
                                                        ConnectPoint portTwo,
                                                        IpAddress ipTwo) {

        List<PointToPointIntent> intents = new ArrayList<>();

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        TrafficSelector selector;
        Key key;

        byte tcpProtocol;
        byte icmpProtocol;

        if (ipOne.isIp4()) {
            tcpProtocol = IPv4.PROTOCOL_TCP;
            icmpProtocol = IPv4.PROTOCOL_ICMP;
        } else {
            tcpProtocol = IPv6.PROTOCOL_TCP;
            icmpProtocol = IPv6.PROTOCOL_ICMP6;
        }

        // Path from BGP speaker to BGP peer matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                ipOne,
                ipTwo,
                null,
                BGP_PORT);

        key = buildKey(ipOne, ipTwo, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP speaker to BGP peer matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                ipOne,
                ipTwo,
                BGP_PORT,
                null);

        key = buildKey(ipOne, ipTwo, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP peer to BGP speaker matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                ipTwo,
                ipOne,
                null,
                BGP_PORT);

        key = buildKey(ipTwo, ipOne, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP peer to BGP speaker matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                ipTwo,
                ipOne,
                BGP_PORT,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP speaker to BGP peer
        selector = buildSelector(icmpProtocol,
                ipOne,
                ipTwo,
                null,
                null);

        key = buildKey(ipOne, ipTwo, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP peer to BGP speaker
        selector = buildSelector(icmpProtocol,
                ipTwo,
                ipOne,
                null,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
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
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(ipProto);

        if (dstIp.isIp4()) {
            builder.matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));
        } else {
            builder.matchIPv6Src(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET6_MASK_LENGTH))
                    .matchIPv6Dst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET6_MASK_LENGTH));
        }

        if (srcTcpPort != null) {
            builder.matchTcpSrc(TpPort.tpPort(srcTcpPort));
        }

        if (dstTcpPort != null) {
            builder.matchTcpDst(TpPort.tpPort(dstTcpPort));
        }

        return builder.build();
    }

    /**
     * Builds an intent Key for a point-to-point intent based off the source
     * and destination IP address, as well as a suffix String to distinguish
     * between different types of intents between the same source and
     * destination.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param suffix suffix string
     * @return
     */
    private Key buildKey(IpAddress srcIp, IpAddress dstIp, String suffix) {
        String keyString = new StringBuilder()
                .append(srcIp.toString())
                .append("-")
                .append(dstIp.toString())
                .append("-")
                .append(suffix)
                .toString();

        return Key.of(keyString, appId);
    }

}
