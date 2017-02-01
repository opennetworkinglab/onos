/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.castor;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the connectivity requirements between peers.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConnectivityManager implements ConnectivityManagerService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CastorStore castorStore;

    private static final int PRIORITY_OFFSET = 1000;
    private static final int FLOW_PRIORITY = 500;
    private static final String SUFFIX_DST = "dst";
    private static final String SUFFIX_SRC = "src";
    private static final String SUFFIX_ICMP = "icmp";

    private static final Logger log = LoggerFactory.getLogger(ConnectivityManager.class);

    private static final short BGP_PORT = 179;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.getAppId(Castor.CASTOR_APP);
    }

    @Deactivate
    public void deactivate() {
    }

    /**
     * Inputs the Route Servers.
     */
    @Override
    public void start(Peer server) {
        //routeServers.add(server);
        //allPeers.add(server);
        castorStore.storePeer(server);
        castorStore.storeServer(server);
    }

    /**
     * Stops the peer connectivity manager.
     */
    public void stop() {};

    /**
     * Sets up paths to establish connectivity between all internal.
     * BGP speakers and external BGP peers.
     */
    @Override
    public void setUpConnectivity(Peer peer) {

        if (!castorStore.getCustomers().contains(peer)) {
            castorStore.storePeer(peer);
            castorStore.storeCustomer(peer);
        }

        for (Peer routeServer : castorStore.getServers()) {
            log.debug("Start to set up BGP paths for BGP peer and Route Server"
                    + peer + "&" + routeServer);

            buildSpeakerIntents(routeServer, peer).forEach(i -> {
                    castorStore.storePeerIntent(i.key(), i);
                    intentSynchronizer.submit(i);
                });
        }
    }

    private Collection<PointToPointIntent> buildSpeakerIntents(Peer speaker, Peer peer) {
        List<PointToPointIntent> intents = new ArrayList<>();

        IpAddress peerAddress = IpAddress.valueOf(peer.getIpAddress());
        IpAddress speakerAddress = IpAddress.valueOf(speaker.getIpAddress());

        checkNotNull(peerAddress);

        intents.addAll(buildIntents(ConnectPoint.deviceConnectPoint(speaker.getPort()), speakerAddress,
                ConnectPoint.deviceConnectPoint(peer.getPort()), peerAddress));

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
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder().matchIPProtocol(ipProto);

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

    @Override
    public void setUpL2(Peer peer) {

        // First update all the previous existing intents. Update ingress points.

        if (!castorStore.getLayer2Intents().isEmpty()) {
            updateExistingL2Intents(peer);
        }

        Set<ConnectPoint> ingressPorts = new HashSet<>();
        ConnectPoint egressPort = ConnectPoint.deviceConnectPoint(peer.getPort());

        for (Peer inPeer : castorStore.getAllPeers()) {
            if (!inPeer.getName().equals(peer.getName())) {
                ingressPorts.add(ConnectPoint.deviceConnectPoint(inPeer.getPort()));
            }
        }
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        MacAddress macAddress = castorStore.getAddressMap().get(IpAddress.valueOf(peer.getIpAddress()));
        selector.matchEthDst(macAddress);
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        Key key = Key.of(peer.getIpAddress(), appId);

        MultiPointToSinglePointIntent intent = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment)
                .ingressPoints(ingressPorts)
                .egressPoint(egressPort)
                .priority(FLOW_PRIORITY)
                .build();
        intentSynchronizer.submit(intent);
        castorStore.storeLayer2Intent(peer.getIpAddress(), intent);
        castorStore.removeCustomer(peer);
        peer.setL2(true);
        castorStore.storeCustomer(peer);
    }

    /**
     * Updates the existing layer 2 flows. Whenever a new Peer is added, it is also
     * added as the ingress point to the existing layer two flows.
     *
     * @param peer The Peer being added.
     */
    private void updateExistingL2Intents(Peer peer) {

        Collection<MultiPointToSinglePointIntent> oldIntents = castorStore.getLayer2Intents().values();

        for (MultiPointToSinglePointIntent oldIntent : oldIntents) {

            Set<ConnectPoint> ingressPoints = oldIntent.ingressPoints();
            ConnectPoint egressPoint = oldIntent.egressPoint();
            if (ingressPoints.add(ConnectPoint.deviceConnectPoint(peer.getPort()))) {

                MultiPointToSinglePointIntent updatedMp2pIntent =
                        MultiPointToSinglePointIntent.builder()
                                .appId(appId)
                                .key(oldIntent.key())
                                .selector(oldIntent.selector())
                                .treatment(oldIntent.treatment())
                                .ingressPoints(ingressPoints)
                                .egressPoint(egressPoint)
                                .priority(oldIntent.priority())
                                .build();

                //layer2Intents.put(peer.getIpAddress(), updatedMp2pIntent);
                castorStore.storeLayer2Intent(peer.getIpAddress(), updatedMp2pIntent);
                intentSynchronizer.submit(updatedMp2pIntent);
            }
        }
    }

    @Override
    public void deletePeer(Peer peer) {

        if (castorStore.getCustomers().contains(peer)) {

            deletel3(peer);

            for (Peer customer : castorStore.getCustomers()) {
                if (customer.getIpAddress().equals(peer.getIpAddress()) && customer.getl2Status()) {
                    deleteL2(customer);
                    updateL2AfterDeletion(customer);
                }
            }
            castorStore.removeCustomer(peer);
        }
    }

    /**
     * Delete all the flows between the Peer being deleted and the Route Servers
     * to kill the BGP sessions. It uses the keys to retrive the previous intents
     * and withdraw them.
     *
     * @param peer The Peer being deleted.
     */
    private void deletel3(Peer peer) {

        List<Key> keys = new LinkedList<>();
        IpAddress ipTwo = IpAddress.valueOf(peer.getIpAddress());

        for (Peer server : castorStore.getServers()) {
            IpAddress ipOne = IpAddress.valueOf(server.getIpAddress());
            keys.add(buildKey(ipOne, ipTwo, SUFFIX_SRC));
            keys.add(buildKey(ipTwo, ipOne, SUFFIX_DST));
            keys.add(buildKey(ipOne, ipTwo, SUFFIX_ICMP));
            keys.add(buildKey(ipTwo, ipOne, SUFFIX_ICMP));
        }
        for (Key keyDel : keys) {

            PointToPointIntent intent = castorStore.getPeerIntents().get(keyDel);
            intentSynchronizer.withdraw(intent);
            castorStore.removePeerIntent(keyDel);
        }
    }

    /**
     * Deletes the layer two flows for the peer being deleted.
     *
     * @param peer The Peer being deleted.
     */
    private void deleteL2(Peer peer) {
        intentSynchronizer.withdraw(castorStore.getLayer2Intents().get(peer.getIpAddress()));
        castorStore.removeLayer2Intent(peer.getIpAddress());
    }

    /**
     * Updates all the layer 2 flows after successful deletion of a Peer.
     * The Peer being deleted is removed from the ingress points of all
     * other flows.
     *
     * @param peer The Peer being deleted.
     */
    private void updateL2AfterDeletion(Peer peer) {
        Collection<MultiPointToSinglePointIntent> oldIntents = castorStore.getLayer2Intents().values();
        Map<String, MultiPointToSinglePointIntent> intents = new HashMap<>();

        for (MultiPointToSinglePointIntent oldIntent : oldIntents) {

            Set<ConnectPoint> ingressPoints = oldIntent.ingressPoints();
            ConnectPoint egressPoint = oldIntent.egressPoint();
            if (ingressPoints.remove(ConnectPoint.deviceConnectPoint(peer.getPort()))) {

                MultiPointToSinglePointIntent updatedMp2pIntent =
                        MultiPointToSinglePointIntent.builder()
                                .appId(appId)
                                .key(oldIntent.key())
                                .selector(oldIntent.selector())
                                .treatment(oldIntent.treatment())
                                .ingressPoints(ingressPoints)
                                .egressPoint(egressPoint)
                                .priority(oldIntent.priority())
                                .build();

                intents.put(peer.getIpAddress(), updatedMp2pIntent);
                intentSynchronizer.submit(updatedMp2pIntent);
            }
        }
        for (String key : intents.keySet()) {
            castorStore.storeLayer2Intent(key, intents.get(key));
        }
    }
}
