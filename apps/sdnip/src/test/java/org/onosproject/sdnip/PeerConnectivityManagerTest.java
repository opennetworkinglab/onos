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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.InterfaceAddress;
import org.onosproject.routing.config.RoutingConfigurationService;

import com.google.common.collect.Sets;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.onosproject.sdnip.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for PeerConnectivityManager.
 */
public class PeerConnectivityManagerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new ApplicationId() {
        @Override
        public short id() {
            return 0;
        }

        @Override
        public String name() {
            return "foo";
        }
    };

    private PeerConnectivityManager peerConnectivityManager;
    private IntentSynchronizer intentSynchronizer;
    private RoutingConfigurationService routingConfig;
    private IntentService intentService;

    private Map<String, BgpSpeaker> bgpSpeakers;
    private Map<String, Interface> interfaces;
    private Map<IpAddress, BgpPeer> peers;

    private Map<String, BgpSpeaker> configuredBgpSpeakers;
    private Map<String, Interface> configuredInterfaces;
    private Map<IpAddress, BgpPeer> configuredPeers;
    private List<PointToPointIntent> intentList;

    private final String dpid1 = "00:00:00:00:00:00:00:01";
    private final String dpid2 = "00:00:00:00:00:00:00:02";

    private final DeviceId deviceId1 =
            DeviceId.deviceId(SdnIp.dpidToUri(dpid1));
    private final DeviceId deviceId2 =
            DeviceId.deviceId(SdnIp.dpidToUri(dpid2));

    // Interfaces connected to BGP speakers
    private final ConnectPoint s1Eth100 =
            new ConnectPoint(deviceId1, PortNumber.portNumber(100));
    private final ConnectPoint s2Eth100 =
            new ConnectPoint(deviceId2, PortNumber.portNumber(100));

    // Interfaces connected to BGP peers
    private final ConnectPoint s1Eth1 =
            new ConnectPoint(deviceId1, PortNumber.portNumber(1));
    private final ConnectPoint s2Eth1 =
            new ConnectPoint(deviceId2, PortNumber.portNumber(1));

    private final TrafficTreatment noTreatment =
            DefaultTrafficTreatment.emptyTreatment();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        routingConfig = createMock(RoutingConfigurationService.class);

        // These will set expectations on routingConfig
        bgpSpeakers = Collections.unmodifiableMap(setUpBgpSpeakers());
        interfaces = Collections.unmodifiableMap(setUpInterfaces());
        peers = Collections.unmodifiableMap(setUpPeers());

        initPeerConnectivity();
        intentList = setUpIntentList();
    }

    /**
     * Sets up BGP speakers.
     *
     * @return configured BGP speakers as a map from speaker name to speaker
     */
    private Map<String, BgpSpeaker> setUpBgpSpeakers() {

        configuredBgpSpeakers = new HashMap<>();

        BgpSpeaker bgpSpeaker1 = new BgpSpeaker(
                "bgpSpeaker1",
                "00:00:00:00:00:00:00:01", 100,
                "00:00:00:00:00:01");
        List<InterfaceAddress> interfaceAddresses1 = new LinkedList<>();
        interfaceAddresses1.add(new InterfaceAddress(dpid1, 1, "192.168.10.101"));
        interfaceAddresses1.add(new InterfaceAddress(dpid2, 1, "192.168.20.101"));
        bgpSpeaker1.setInterfaceAddresses(interfaceAddresses1);
        configuredBgpSpeakers.put(bgpSpeaker1.name(), bgpSpeaker1);

        // BGP speaker2 is attached to the same switch port with speaker1
        BgpSpeaker bgpSpeaker2 = new BgpSpeaker(
                "bgpSpeaker2",
                "00:00:00:00:00:00:00:01", 100,
                "00:00:00:00:00:02");
        List<InterfaceAddress> interfaceAddresses2 = new LinkedList<>();
        interfaceAddresses2.add(new InterfaceAddress(dpid1, 1, "192.168.10.102"));
        interfaceAddresses2.add(new InterfaceAddress(dpid2, 1, "192.168.20.102"));
        bgpSpeaker2.setInterfaceAddresses(interfaceAddresses2);
        configuredBgpSpeakers.put(bgpSpeaker2.name(), bgpSpeaker2);

        BgpSpeaker bgpSpeaker3 = new BgpSpeaker(
                "bgpSpeaker3",
                "00:00:00:00:00:00:00:02", 100,
                "00:00:00:00:00:03");
        List<InterfaceAddress> interfaceAddresses3 = new LinkedList<>();
        interfaceAddresses3.add(new InterfaceAddress(dpid1, 1, "192.168.10.103"));
        interfaceAddresses3.add(new InterfaceAddress(dpid2, 1, "192.168.20.103"));
        bgpSpeaker3.setInterfaceAddresses(interfaceAddresses3);
        configuredBgpSpeakers.put(bgpSpeaker3.name(), bgpSpeaker3);

        return configuredBgpSpeakers;
    }

    /**
     * Sets up logical interfaces, which emulate the configured interfaces
     * in SDN-IP application.
     *
     * @return configured interfaces as a MAP from Interface name to Interface
     */
    private Map<String, Interface> setUpInterfaces() {

        configuredInterfaces = new HashMap<>();

        String interfaceSw1Eth1 = "s1-eth1";
        InterfaceIpAddress ia1 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.10.1"),
                                   IpPrefix.valueOf("192.168.10.0/24"));
        Interface intfsw1eth1 = new Interface(s1Eth1,
                Collections.singleton(ia1),
                MacAddress.valueOf("00:00:00:00:00:01"),
                VlanId.NONE);

        configuredInterfaces.put(interfaceSw1Eth1, intfsw1eth1);
        String interfaceSw2Eth1 = "s2-eth1";
        InterfaceIpAddress ia2 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.20.2"),
                                   IpPrefix.valueOf("192.168.20.0/24"));
        Interface intfsw2eth1 = new Interface(s2Eth1,
                Collections.singleton(ia2),
                MacAddress.valueOf("00:00:00:00:00:02"),
                VlanId.NONE);
        configuredInterfaces.put(interfaceSw2Eth1, intfsw2eth1);

        expect(routingConfig.getInterface(s1Eth1))
                .andReturn(intfsw1eth1).anyTimes();
        expect(routingConfig.getInterface(s2Eth1))
                .andReturn(intfsw2eth1).anyTimes();

        // Non-existent interface used during one of the tests
        expect(routingConfig.getInterface(new ConnectPoint(
                    DeviceId.deviceId(SdnIp.dpidToUri("00:00:00:00:00:00:01:00")),
                    PortNumber.portNumber(1))))
                    .andReturn(null).anyTimes();

        expect(routingConfig.getInterfaces()).andReturn(
                Sets.newHashSet(configuredInterfaces.values())).anyTimes();

        return configuredInterfaces;
    }

    /**
     * Sets up BGP daemon peers.
     *
     * @return configured BGP peers as a MAP from peer IP address to BgpPeer
     */
    private Map<IpAddress, BgpPeer> setUpPeers() {

        configuredPeers = new HashMap<>();

        String peerSw1Eth1 = "192.168.10.1";
        configuredPeers.put(IpAddress.valueOf(peerSw1Eth1),
                new BgpPeer(dpid1, 1, peerSw1Eth1));

        // Two BGP peers are connected to switch 2 port 1.
        String peer1Sw2Eth1 = "192.168.20.1";
        configuredPeers.put(IpAddress.valueOf(peer1Sw2Eth1),
                new BgpPeer(dpid2, 1, peer1Sw2Eth1));

        String peer2Sw2Eth1 = "192.168.20.2";
        configuredPeers.put(IpAddress.valueOf(peer2Sw2Eth1),
                new BgpPeer(dpid2, 1, peer2Sw2Eth1));

        return configuredPeers;
    }

    /**
     * Sets up expected point to point intent list.
     *
     * @return point to point intent list
     */
    private List<PointToPointIntent> setUpIntentList() {

        intentList = new ArrayList<>();

        setUpBgpIntents();
        setUpIcmpIntents();

        return intentList;

    }

    /**
     * Constructs a BGP intent and put it into the intentList.
     * <p/>
     * The purpose of this method is too simplify the setUpBgpIntents() method,
     * and to make the setUpBgpIntents() easy to read.
     *
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcTcpPort source TCP port to match
     * @param dstTcpPort destination TCP port to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void bgpPathintentConstructor(String srcPrefix, String dstPrefix,
            Short srcTcpPort, Short dstTcpPort,
            ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix));

        if (srcTcpPort != null) {
            builder.matchTcpSrc(srcTcpPort);
        }
        if (dstTcpPort != null) {
            builder.matchTcpDst(dstTcpPort);
        }

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APPID)
                .selector(builder.build())
                .treatment(noTreatment)
                .ingressPoint(srcConnectPoint)
                .egressPoint(dstConnectPoint)
                .build();

        intentList.add(intent);
    }

    /**
     * Sets up intents for BGP paths.
     */
    private void setUpBgpIntents() {

        Short bgpPort = 179;

        // Start to build intents between BGP speaker1 and BGP peer1
        bgpPathintentConstructor(
                "192.168.10.101/32", "192.168.10.1/32", null, bgpPort,
                s1Eth100, s1Eth1);
        bgpPathintentConstructor(
                "192.168.10.101/32", "192.168.10.1/32", bgpPort, null,
                s1Eth100, s1Eth1);

        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.101/32", null, bgpPort,
                s1Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.101/32", bgpPort, null,
                s1Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer2
        bgpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.1/32", null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.1/32", bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.101/32", null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.101/32", bgpPort, null,
                s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer3
        bgpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.2/32", null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.2/32", bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.101/32", null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.101/32", bgpPort, null,
                s2Eth1, s1Eth100);

        //
        // Start to build intents between BGP speaker2 and BGP peer1
        bgpPathintentConstructor(
                "192.168.10.102/32", "192.168.10.1/32", null, bgpPort,
                s1Eth100, s1Eth1);
        bgpPathintentConstructor(
                "192.168.10.102/32", "192.168.10.1/32", bgpPort, null,
                s1Eth100, s1Eth1);

        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.102/32", null, bgpPort,
                s1Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.102/32", bgpPort, null,
                s1Eth1, s1Eth100);
        // Start to build intents between BGP speaker2 and BGP peer2
        bgpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.1/32", null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.1/32", bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.102/32", null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.102/32", bgpPort, null,
                s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker2 and BGP peer3
        bgpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.2/32", null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.2/32", bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.102/32", null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.102/32", bgpPort, null,
                s2Eth1, s1Eth100);

        //
        // Start to build intents between BGP speaker3 and BGP peer1
        bgpPathintentConstructor(
                "192.168.10.103/32", "192.168.10.1/32", null, bgpPort,
                s2Eth100, s1Eth1);
        bgpPathintentConstructor(
                "192.168.10.103/32", "192.168.10.1/32", bgpPort, null,
                s2Eth100, s1Eth1);

        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.103/32", null, bgpPort,
                s1Eth1, s2Eth100);
        bgpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.103/32", bgpPort, null,
                s1Eth1, s2Eth100);

        // Start to build intents between BGP speaker3 and BGP peer2
        bgpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.1/32", null, bgpPort,
                s2Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.1/32", bgpPort, null,
                s2Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.103/32", null, bgpPort,
                s2Eth1, s2Eth100);
        bgpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.103/32", bgpPort, null,
                s2Eth1, s2Eth100);

        // Start to build intents between BGP speaker3 and BGP peer3
        bgpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.2/32", null, bgpPort,
                s2Eth100, s2Eth1);
        bgpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.2/32", bgpPort, null,
                s2Eth100, s2Eth1);

        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.103/32", null, bgpPort,
                s2Eth1, s2Eth100);
        bgpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.103/32", bgpPort, null,
                s2Eth1, s2Eth100);
    }

    /**
     * Constructs a BGP intent and put it into the intentList.
     * <p/>
     * The purpose of this method is too simplify the setUpBgpIntents() method,
     * and to make the setUpBgpIntents() easy to read.
     *
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void icmpPathintentConstructor(String srcPrefix, String dstPrefix,
            ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix))
                .build();

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(noTreatment)
                .ingressPoint(srcConnectPoint)
                .egressPoint(dstConnectPoint)
                .build();

        intentList.add(intent);
    }

    /**
     * Sets up intents for ICMP paths.
     */
    private void setUpIcmpIntents() {

        // Start to build intents between BGP speaker1 and BGP peer1
        icmpPathintentConstructor(
                "192.168.10.101/32", "192.168.10.1/32", s1Eth100, s1Eth1);
        icmpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.101/32", s1Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer2
        icmpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.1/32", s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.101/32", s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer3
        icmpPathintentConstructor(
                "192.168.20.101/32", "192.168.20.2/32", s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.101/32", s2Eth1, s1Eth100);

        //
        // Start to build intents between BGP speaker2 and BGP peer1
        icmpPathintentConstructor(
                "192.168.10.102/32", "192.168.10.1/32", s1Eth100, s1Eth1);
        icmpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.102/32", s1Eth1, s1Eth100);

        // Start to build intents between BGP speaker2 and BGP peer2
        icmpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.1/32", s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.102/32", s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker2 and BGP peer3
        icmpPathintentConstructor(
                "192.168.20.102/32", "192.168.20.2/32", s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.102/32", s2Eth1, s1Eth100);

        //
        // Start to build intents between BGP speaker3 and BGP peer1
        icmpPathintentConstructor(
                "192.168.10.103/32", "192.168.10.1/32", s2Eth100, s1Eth1);
        icmpPathintentConstructor(
                "192.168.10.1/32", "192.168.10.103/32", s1Eth1, s2Eth100);

        // Start to build intents between BGP speaker3 and BGP peer2
        icmpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.1/32", s2Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.1/32", "192.168.20.103/32", s2Eth1, s2Eth100);

        // Start to build intents between BGP speaker3 and BGP peer3
        icmpPathintentConstructor(
                "192.168.20.103/32", "192.168.20.2/32", s2Eth100, s2Eth1);
        icmpPathintentConstructor(
                "192.168.20.2/32", "192.168.20.103/32", s2Eth1, s2Eth100);

    }

    /**
     * Initializes peer connectivity testing environment.
     *
     * @throws TestUtilsException if exceptions when using TestUtils
     */
    private void initPeerConnectivity() throws TestUtilsException {

        expect(routingConfig.getBgpPeers()).andReturn(peers).anyTimes();
        expect(routingConfig.getBgpSpeakers()).andReturn(bgpSpeakers).anyTimes();
        replay(routingConfig);

        intentService = createMock(IntentService.class);
        replay(intentService);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService,
                                                    null, routingConfig);
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);

        peerConnectivityManager =
            new PeerConnectivityManager(APPID, intentSynchronizer,
                                        routingConfig);
    }

    /**
     * Tests whether peer connectivity manager can set up correct BGP and
     * ICMP intents according to specific configuration.
     * <p/>
     * Two tricky cases included in the configuration are: 2 peers on a same
     * switch port, peer on the same switch with BGPd.
     */
    @Test
    public void testConnectionSetup() {

        reset(intentService);

        // Setup the expected intents
        for (Intent intent : intentList) {
            intentService.submit(eqExceptId(intent));
        }
        replay(intentService);

        // Running the interface to be tested.
        peerConnectivityManager.start();

        verify(intentService);

    }

    /**
     *  Tests a corner case, when there are no interfaces in the configuration.
     */
    @Test
    public void testNullInterfaces() {
        reset(routingConfig);
        expect(routingConfig.getInterfaces()).andReturn(
                Sets.<Interface>newHashSet()).anyTimes();
        expect(routingConfig.getInterface(s2Eth1))
                .andReturn(null).anyTimes();
        expect(routingConfig.getInterface(s1Eth1))
        .andReturn(null).anyTimes();

        expect(routingConfig.getBgpPeers()).andReturn(peers).anyTimes();
        expect(routingConfig.getBgpSpeakers()).andReturn(bgpSpeakers).anyTimes();
        replay(routingConfig);

        reset(intentService);
        replay(intentService);
        peerConnectivityManager.start();
        verify(intentService);
    }

    /**
     *  Tests a corner case, when there are no BGP peers in the configuration.
     */
    @Test
    public void testNullBgpPeers() {
        reset(routingConfig);
        expect(routingConfig.getInterfaces()).andReturn(
                Sets.newHashSet(interfaces.values())).anyTimes();

        expect(routingConfig.getBgpPeers()).andReturn(new HashMap<>()).anyTimes();
        expect(routingConfig.getBgpSpeakers()).andReturn(bgpSpeakers).anyTimes();
        replay(routingConfig);

        reset(intentService);
        replay(intentService);
        peerConnectivityManager.start();
        verify(intentService);
    }

    /**
     *  Tests a corner case, when there is no BGP speakers in the configuration.
     */
    @Test
    public void testNullBgpSpeakers() {
        reset(routingConfig);
        expect(routingConfig.getInterfaces()).andReturn(
                Sets.newHashSet(interfaces.values())).anyTimes();

        expect(routingConfig.getBgpPeers()).andReturn(peers).anyTimes();
        expect(routingConfig.getBgpSpeakers()).andReturn(
                Collections.emptyMap()).anyTimes();
        replay(routingConfig);

        reset(intentService);
        replay(intentService);
        peerConnectivityManager.start();
        verify(intentService);
    }

    /**
     * Tests a corner case, when there is no Interface configured for one BGP
     * peer.
     */
    @Test
    public void testNoPeerInterface() {
        String peerSw100Eth1 = "192.168.200.1";
        configuredPeers.put(IpAddress.valueOf(peerSw100Eth1),
                new BgpPeer("00:00:00:00:00:00:01:00", 1, peerSw100Eth1));
        testConnectionSetup();
    }

    /**
     * Tests a corner case, when there is no Interface configured for one BGP
     * speaker.
     */
    @Ignore
    @Test
    public void testNoSpeakerInterface() {
        BgpSpeaker bgpSpeaker100 = new BgpSpeaker(
                "bgpSpeaker100",
                "00:00:00:00:00:00:01:00", 100,
                "00:00:00:00:01:00");
        List<InterfaceAddress> interfaceAddresses100 = new LinkedList<>();
        interfaceAddresses100.add(new InterfaceAddress(dpid1, 1, "192.168.10.201"));
        interfaceAddresses100.add(new InterfaceAddress(dpid2, 1, "192.168.20.201"));
        bgpSpeaker100.setInterfaceAddresses(interfaceAddresses100);
        configuredBgpSpeakers.put(bgpSpeaker100.name(), bgpSpeaker100);
        testConnectionSetup();
    }
}
