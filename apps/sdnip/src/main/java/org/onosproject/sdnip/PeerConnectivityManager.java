/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final Map<Key, PointToPointIntent> peerIntents;

    private final InternalNetworkConfigListener configListener
            = new InternalNetworkConfigListener();

    private final InternalInterfaceListener interfaceListener
            = new InternalInterfaceListener();

    /**
     * Creates a new PeerConnectivityManager.
     *
     * @param appId              the application ID
     * @param intentSynchronizer the intent synchronizer
     * @param configService      the network config service
     * @param routerAppId        application ID
     * @param interfaceService   the interface service
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

        peerIntents = new HashMap<>();
    }

    /**
     * Starts the peer connectivity manager.
     */
    public void start() {
        configService.addListener(configListener);
        interfaceService.addListener(interfaceListener);
        setUpConnectivity();
    }

    /**
     * Stops the peer connectivity manager.
     */
    public void stop() {
        configService.removeListener(configListener);
        interfaceService.removeListener(interfaceListener);
    }

    /**
     * Sets up paths to establish connectivity between all internal
     * BGP speakers and external BGP peers.
     */
    private void setUpConnectivity() {
        BgpConfig config = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        Set<BgpConfig.BgpSpeakerConfig> bgpSpeakers;

        if (config == null) {
            log.warn("No BGP config available");
            bgpSpeakers = Collections.emptySet();
        } else {
            bgpSpeakers = config.bgpSpeakers();
        }

        Map<Key, PointToPointIntent> existingIntents = new HashMap<>(peerIntents);

        for (BgpConfig.BgpSpeakerConfig bgpSpeaker : bgpSpeakers) {
            log.debug("Start to set up BGP paths for BGP speaker: {}",
                    bgpSpeaker);

            buildSpeakerIntents(bgpSpeaker).forEach(i -> {
                PointToPointIntent intent = existingIntents.remove(i.key());
                if (intent == null || !IntentUtils.intentsAreEqual(i, intent)) {
                    peerIntents.put(i.key(), i);
                    intentSynchronizer.submit(i);
                }
            });
        }

        // Remove any remaining intents that we used to have that we don't need
        // anymore
        existingIntents.values().forEach(i -> {
            peerIntents.remove(i.key());
            intentSynchronizer.withdraw(i);
        });
    }

    private Collection<PointToPointIntent> buildSpeakerIntents(BgpConfig.BgpSpeakerConfig speaker) {
        List<PointToPointIntent> intents = new ArrayList<>();

        // Get the BGP Speaker VLAN Id
        VlanId bgpSpeakerVlanId = speaker.vlan();

        for (IpAddress peerAddress : speaker.peers()) {
            Interface peeringInterface = interfaceService.getMatchingInterface(peerAddress);

            if (peeringInterface == null) {
                log.debug("No peering interface found for peer {} on speaker {}",
                        peerAddress, speaker);
                continue;
            }

            IpAddress bgpSpeakerAddress = null;
            for (InterfaceIpAddress address : peeringInterface.ipAddressesList()) {
                if (address.subnetAddress().contains(peerAddress)) {
                    bgpSpeakerAddress = address.ipAddress();
                    break;
                }
            }

            checkNotNull(bgpSpeakerAddress);

            VlanId peerVlanId = peeringInterface.vlan();

            intents.addAll(buildIntents(speaker.connectPoint(), bgpSpeakerVlanId,
                                        bgpSpeakerAddress,
                                        peeringInterface.connectPoint(),
                                        peerVlanId,
                                        peerAddress));
        }

        return intents;
    }

    /**
     * Builds the required intents between a BGP speaker and an external router.
     *
     * @param portOne the BGP speaker connect point
     * @param vlanOne the BGP speaker VLAN
     * @param ipOne the BGP speaker IP address
     * @param portTwo the external BGP peer connect point
     * @param vlanTwo the external BGP peer VLAN
     * @param ipTwo the external BGP peer IP address
     * @return the intents to install
     */
    private Collection<PointToPointIntent> buildIntents(ConnectPoint portOne,
                                                        VlanId vlanOne,
                                                        IpAddress ipOne,
                                                        ConnectPoint portTwo,
                                                        VlanId vlanTwo,
                                                        IpAddress ipTwo) {

        List<PointToPointIntent> intents = new ArrayList<>();

        TrafficTreatment.Builder treatmentToPeer = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder treatmentToSpeaker = DefaultTrafficTreatment.builder();

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

        // Add VLAN treatment for traffic going from BGP speaker to BGP peer
        if (!vlanOne.equals(vlanTwo)) {
            if (vlanTwo.equals(VlanId.NONE)) {
                treatmentToPeer.popVlan();
            } else {
                treatmentToPeer.setVlanId(vlanTwo);
            }
        }

        // Path from BGP speaker to BGP peer matching destination TCP port 179

        selector = buildSelector(tcpProtocol,
                vlanOne,
                ipOne,
                ipTwo,
                null,
                BGP_PORT);

        key = buildKey(ipOne, ipTwo, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToPeer.build())
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP speaker to BGP peer matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanOne,
                ipOne,
                ipTwo,
                BGP_PORT,
                null);

        key = buildKey(ipOne, ipTwo, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToPeer.build())
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP speaker to BGP peer
        selector = buildSelector(icmpProtocol,
                                 vlanOne,
                                 ipOne,
                                 ipTwo,
                                 null,
                                 null);

        key = buildKey(ipOne, ipTwo, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                            .appId(appId)
                            .key(key)
                            .selector(selector)
                            .treatment(treatmentToPeer.build())
                            .ingressPoint(portOne)
                            .egressPoint(portTwo)
                            .priority(PRIORITY_OFFSET)
                            .build());

        // Add VLAN treatment for traffic going from BGP peer to BGP speaker
        if (!vlanTwo.equals(vlanOne)) {
            if (vlanOne.equals(VlanId.NONE)) {
                treatmentToSpeaker.popVlan();
            } else {
                treatmentToSpeaker.setVlanId(vlanOne);
            }
        }

        // Path from BGP peer to BGP speaker matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                null,
                BGP_PORT);

        key = buildKey(ipTwo, ipOne, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP peer to BGP speaker matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                BGP_PORT,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP peer to BGP speaker
        selector = buildSelector(icmpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                null,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
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
    private TrafficSelector buildSelector(byte ipProto, VlanId ingressVlanId,
                                          IpAddress srcIp,
                                          IpAddress dstIp, Short srcTcpPort,
                                          Short dstTcpPort) {
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder().matchIPProtocol(ipProto);

        // Match on any VLAN Id if a VLAN Id configured on the ingress interface
        if (!ingressVlanId.equals(VlanId.NONE)) {
            builder.matchVlanId(VlanId.ANY);
        }

        if (dstIp.isIp4()) {
            builder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));
        } else {
            builder.matchEthType(Ethernet.TYPE_IPV6)
                    .matchIPv6Src(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET6_MASK_LENGTH))
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
     * @return intent key
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

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_REGISTERED:
                break;
            case CONFIG_UNREGISTERED:
                break;
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
            case CONFIG_REMOVED:
                if (event.configClass() == RoutingService.CONFIG_CLASS) {
                    setUpConnectivity();
                }
                break;
            default:
                break;
            }
        }
    }

    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            switch (event.type()) {
            case INTERFACE_ADDED:
            case INTERFACE_UPDATED:
            case INTERFACE_REMOVED:
                setUpConnectivity();
                break;
            default:
                break;
            }
        }
    }

}
