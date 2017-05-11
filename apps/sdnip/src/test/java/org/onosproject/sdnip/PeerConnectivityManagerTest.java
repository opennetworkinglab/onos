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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdnip.config.SdnIpConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.onosproject.routing.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for PeerConnectivityManager.
 */
public class PeerConnectivityManagerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = TestApplicationId.create("foo");

    private static final ApplicationId CONFIG_APP_ID = APPID;

    private PeerConnectivityManager peerConnectivityManager;
    private IntentSynchronizationService intentSynchronizer;
    private InterfaceService interfaceService;
    private NetworkConfigService networkConfigService;

    private Set<BgpConfig.BgpSpeakerConfig> bgpSpeakers;
    private Map<String, Interface> interfaces;

    private BgpConfig bgpConfig;

    private SdnIpConfig sdnIpConfig;

    private List<PointToPointIntent> intentList;

    private final DeviceId deviceId1 =
            DeviceId.deviceId("of:0000000000000001");
    private final DeviceId deviceId2 =
            DeviceId.deviceId("of:0000000000000002");
    private final DeviceId deviceId3 =
            DeviceId.deviceId("of:0000000000000003");

    // Interfaces connected to BGP speakers
    private final ConnectPoint s1Eth100 =
            new ConnectPoint(deviceId1, PortNumber.portNumber(100));
    private final ConnectPoint s2Eth100 =
            new ConnectPoint(deviceId2, PortNumber.portNumber(100));
    private final ConnectPoint s3Eth100 =
            new ConnectPoint(deviceId3, PortNumber.portNumber(100));

    // Interfaces connected to BGP peers
    private final ConnectPoint s1Eth1 =
            new ConnectPoint(deviceId1, PortNumber.portNumber(1));
    private final ConnectPoint s2Eth1 =
            new ConnectPoint(deviceId2, PortNumber.portNumber(1));
    private final ConnectPoint s3Eth1 =
            new ConnectPoint(deviceId3, PortNumber.portNumber(1));

    private static final VlanId NO_VLAN = VlanId.NONE;
    private static final VlanId VLAN10 = VlanId.vlanId(Short.valueOf("10"));
    private static final VlanId VLAN20 = VlanId.vlanId(Short.valueOf("20"));
    private static final VlanId VLAN30 = VlanId.vlanId(Short.valueOf("30"));

    @Before
    public void setUp() {
        super.setUp();

        interfaceService = createMock(InterfaceService.class);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().anyTimes();
        networkConfigService = createMock(NetworkConfigService.class);
        networkConfigService.addListener(anyObject(NetworkConfigListener.class));
        expectLastCall().anyTimes();
        bgpConfig = createMock(BgpConfig.class);
        sdnIpConfig = createMock(SdnIpConfig.class);

        // These will set expectations on routingConfig and interfaceService
        bgpSpeakers = setUpBgpSpeakers();
        interfaces = Collections.unmodifiableMap(setUpInterfaces());

        initPeerConnectivity();
        intentList = setUpIntentList();
    }

    /**
     * Sets up BGP speakers.
     *
     * @return configured BGP speakers as a map from speaker name to speaker
     */
    private Set<BgpConfig.BgpSpeakerConfig> setUpBgpSpeakers() {

        BgpConfig.BgpSpeakerConfig speaker1 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(),
                NO_VLAN, s1Eth100,
                Collections.singleton(IpAddress.valueOf("192.168.10.1")));

        BgpConfig.BgpSpeakerConfig speaker2 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(),
                NO_VLAN, s1Eth100,
                Sets.newHashSet(IpAddress.valueOf("192.168.20.1"),
                IpAddress.valueOf("192.168.30.1")));

        BgpConfig.BgpSpeakerConfig speaker3 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(),
                VLAN30, s3Eth100,
                Sets.newHashSet(IpAddress.valueOf("192.168.40.1"),
                                IpAddress.valueOf("192.168.50.1")));

        Set<BgpConfig.BgpSpeakerConfig> bgpSpeakers = Sets.newHashSet();
        bgpSpeakers.add(speaker1);
        bgpSpeakers.add(speaker2);
        bgpSpeakers.add(speaker3);

        return bgpSpeakers;
    }

    /**
     * Sets up logical interfaces, which emulate the configured interfaces
     * in the SDN-IP application.
     *
     * @return configured interfaces as a map from interface name to Interface
     */
    private Map<String, Interface> setUpInterfaces() {

        Map<String, Interface> configuredInterfaces = new HashMap<>();

        String interfaceSw1Eth1 = "s1-eth1";
        InterfaceIpAddress ia1 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.10.101"),
                                   IpPrefix.valueOf("192.168.10.0/24"));
        Interface intfsw1eth1 = new Interface(interfaceSw1Eth1, s1Eth1,
                Collections.singletonList(ia1),
                MacAddress.valueOf("00:00:00:00:00:01"),
                VlanId.NONE);

        configuredInterfaces.put(interfaceSw1Eth1, intfsw1eth1);

        String interfaceSw2Eth1 = "s2-eth1";
        InterfaceIpAddress ia2 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"),
                                   IpPrefix.valueOf("192.168.20.0/24"));
        Interface intfsw2eth1 = new Interface(interfaceSw2Eth1, s2Eth1,
                Collections.singletonList(ia2),
                MacAddress.valueOf("00:00:00:00:00:02"),
                VlanId.NONE);

        configuredInterfaces.put(interfaceSw2Eth1, intfsw2eth1);

        String interfaceSw2Eth1intf2 = "s2-eth1_2";
        InterfaceIpAddress ia3 =
                new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"),
                        IpPrefix.valueOf("192.168.30.0/24"));
        Interface intfsw2eth1intf2 = new Interface(interfaceSw2Eth1intf2, s2Eth1,
                Collections.singletonList(ia3),
                MacAddress.valueOf("00:00:00:00:00:03"),
                VlanId.NONE);

        configuredInterfaces.put(interfaceSw2Eth1intf2, intfsw2eth1intf2);

        String interfaceSw3Eth1 = "s3-eth1";
        InterfaceIpAddress ia4 =
                new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"),
                                       IpPrefix.valueOf("192.168.40.0/24"));
        Interface intfsw3eth1 = new Interface(Interface.NO_INTERFACE_NAME,
                s3Eth1,
                ImmutableList.of(ia4),
                MacAddress.valueOf("00:00:00:00:00:04"),
                VLAN10);

        configuredInterfaces.put(interfaceSw3Eth1, intfsw3eth1);

        String interfaceSw3Eth1intf2 = "s3-eth1_2";
        InterfaceIpAddress ia5 =
                new InterfaceIpAddress(IpAddress.valueOf("192.168.50.101"),
                                       IpPrefix.valueOf("192.168.50.0/24"));
        Interface intfsw3eth1intf2 = new Interface(Interface.NO_INTERFACE_NAME,
                s3Eth1,
                ImmutableList.of(ia5),
                MacAddress.valueOf("00:00:00:00:00:05"),
                VLAN20);

        configuredInterfaces.put(interfaceSw3Eth1intf2, intfsw3eth1intf2);

        expect(interfaceService.getInterfacesByPort(s1Eth1))
                .andReturn(Collections.singleton(intfsw1eth1)).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.10.101")))
                .andReturn(Collections.singleton(intfsw1eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.10.1")))
                .andReturn(intfsw1eth1).anyTimes();

        expect(interfaceService.getInterfacesByPort(s2Eth1))
                .andReturn(Collections.singleton(intfsw2eth1)).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.20.101")))
                .andReturn(Collections.singleton(intfsw2eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.20.1")))
                .andReturn(intfsw2eth1).anyTimes();

        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.30.101")))
                .andReturn(Collections.singleton(intfsw2eth1intf2)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.30.1")))
                .andReturn(intfsw2eth1intf2).anyTimes();

        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.40.101")))
                .andReturn(Collections.singleton(intfsw3eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.40.1")))
                .andReturn(intfsw3eth1).anyTimes();

        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.50.101")))
                .andReturn(Collections.singleton(intfsw3eth1intf2)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.50.1")))
                .andReturn(intfsw3eth1intf2).anyTimes();

        // Non-existent interface used during one of the tests
        expect(interfaceService.getInterfacesByPort(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000100"),
                PortNumber.portNumber(1))))
                    .andReturn(null).anyTimes();

        expect(interfaceService.getInterfaces()).andReturn(
                Sets.newHashSet(configuredInterfaces.values())).anyTimes();

        return configuredInterfaces;
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
     * @param srcVlanId ingress VlanId
     * @param dstVlanId egress VlanId
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcTcpPort source TCP port to match
     * @param dstTcpPort destination TCP port to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void bgpPathintentConstructor(VlanId srcVlanId, VlanId dstVlanId,
                                          String srcPrefix, String dstPrefix,
                                          Short srcTcpPort, Short dstTcpPort,
                                          ConnectPoint srcConnectPoint,
                                          ConnectPoint dstConnectPoint) {

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix));

        if (!srcVlanId.equals(VlanId.NONE)) {
            builder.matchVlanId(srcVlanId);
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        if (!dstVlanId.equals(VlanId.NONE)) {
            treatment.setVlanId(dstVlanId);
        }

        if (srcTcpPort != null) {
            builder.matchTcpSrc(TpPort.tpPort(srcTcpPort));
        }
        if (dstTcpPort != null) {
            builder.matchTcpDst(TpPort.tpPort(dstTcpPort));
        }

        Key key = Key.of(srcPrefix.split("/")[0] + "-" + dstPrefix.split("/")[0]
                + "-" + ((srcTcpPort == null) ? "dst" : "src"), APPID);

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(builder.build())
                .treatment(treatment.build())
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
                NO_VLAN, NO_VLAN,
                "192.168.10.101/32", "192.168.10.1/32",
                null, bgpPort,
                s1Eth100, s1Eth1);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.10.101/32", "192.168.10.1/32",
                bgpPort, null,
                s1Eth100, s1Eth1);

        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.10.1/32", "192.168.10.101/32",
                null, bgpPort,
                s1Eth1, s1Eth100);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.10.1/32", "192.168.10.101/32",
                bgpPort, null,
                s1Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer2
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.101/32", "192.168.20.1/32",
                null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.101/32", "192.168.20.1/32",
                bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.1/32", "192.168.20.101/32",
                null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.1/32", "192.168.20.101/32",
                bgpPort, null,
                s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker2 and BGP peer1
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.101/32", "192.168.30.1/32",
                null, bgpPort,
                s1Eth100, s2Eth1);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.101/32", "192.168.30.1/32",
                bgpPort, null,
                s1Eth100, s2Eth1);

        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.1/32", "192.168.30.101/32",
                null, bgpPort,
                s2Eth1, s1Eth100);
        bgpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.1/32", "192.168.30.101/32",
                bgpPort, null,
                s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker3 and BGP peer4
        bgpPathintentConstructor(
                VLAN30, VLAN10,
                "192.168.40.101/32", "192.168.40.1/32",
                null, bgpPort,
                s3Eth100, s3Eth1);
        bgpPathintentConstructor(
                VLAN30, VLAN10,
                "192.168.40.101/32", "192.168.40.1/32",
                bgpPort, null,
                s3Eth100, s3Eth1);

        bgpPathintentConstructor(
                VLAN10, VLAN30,
                "192.168.40.1/32", "192.168.40.101/32",
                null, bgpPort,
                s3Eth1, s3Eth100);
        bgpPathintentConstructor(
                VLAN10, VLAN30,
                "192.168.40.1/32", "192.168.40.101/32",
                bgpPort, null,
                s3Eth1, s3Eth100);

        // Start to build intents between BGP speaker3 and BGP peer5
        bgpPathintentConstructor(
                VLAN30, VLAN20,
                "192.168.50.101/32", "192.168.50.1/32",
                null, bgpPort,
                s3Eth100, s3Eth1);
        bgpPathintentConstructor(
                VLAN30, VLAN20,
                "192.168.50.101/32", "192.168.50.1/32",
                bgpPort, null,
                s3Eth100, s3Eth1);

        bgpPathintentConstructor(
                VLAN20, VLAN30,
                "192.168.50.1/32", "192.168.50.101/32",
                null, bgpPort,
                s3Eth1, s3Eth100);
        bgpPathintentConstructor(
                VLAN20, VLAN30,
                "192.168.50.1/32", "192.168.50.101/32",
                bgpPort, null,
                s3Eth1, s3Eth100);
    }

    /**
     * Constructs a BGP intent and put it into the intentList.
     * <p/>
     * The purpose of this method is too simplify the setUpBgpIntents() method,
     * and to make the setUpBgpIntents() easy to read.
     *
     * @param srcVlanId ingress VlanId
     * @param dstVlanId egress VlanId
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void icmpPathintentConstructor(VlanId srcVlanId, VlanId dstVlanId,
                                           String srcPrefix, String dstPrefix,
                                           ConnectPoint srcConnectPoint,
                                           ConnectPoint dstConnectPoint) {

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix));

        if (!srcVlanId.equals(VlanId.NONE)) {
            builder.matchVlanId(srcVlanId);
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        if (!dstVlanId.equals(VlanId.NONE)) {
            treatment.setVlanId(dstVlanId);
        }

        Key key = Key.of(srcPrefix.split("/")[0] + "-" + dstPrefix.split("/")[0]
                + "-" + "icmp", APPID);

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(builder.build())
                .treatment(treatment.build())
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
                NO_VLAN, NO_VLAN,
                "192.168.10.101/32", "192.168.10.1/32",
                s1Eth100, s1Eth1);
        icmpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.10.1/32", "192.168.10.101/32",
                s1Eth1, s1Eth100);

        // Start to build intents between BGP speaker1 and BGP peer2
        icmpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.101/32", "192.168.20.1/32",
                s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.20.1/32", "192.168.20.101/32",
                s2Eth1, s1Eth100);

        icmpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.101/32", "192.168.30.1/32",
                s1Eth100, s2Eth1);
        icmpPathintentConstructor(
                NO_VLAN, NO_VLAN,
                "192.168.30.1/32", "192.168.30.101/32",
                s2Eth1, s1Eth100);

        // Start to build intents between BGP speaker3 and BGP peer 4
        icmpPathintentConstructor(
                VLAN10, VLAN30,
                "192.168.40.1/32", "192.168.40.101/32",
                s3Eth1, s3Eth100);
        icmpPathintentConstructor(
                VLAN30, VLAN10,
                "192.168.40.101/32", "192.168.40.1/32",
                s3Eth100, s3Eth1);

        // Start to build intents between BGP speaker3 and BGP peer 5
        icmpPathintentConstructor(
                VLAN20, VLAN30,
                "192.168.50.1/32", "192.168.50.101/32",
                s3Eth1, s3Eth100);
        icmpPathintentConstructor(
                VLAN30, VLAN20,
                "192.168.50.101/32", "192.168.50.1/32",
                s3Eth100, s3Eth1);
    }

    /**
     * Initializes peer connectivity testing environment.
     */
    private void initPeerConnectivity() {
        expect(bgpConfig.bgpSpeakers()).andReturn(bgpSpeakers).anyTimes();
        replay(bgpConfig);
        expect(networkConfigService.getConfig(APPID, BgpConfig.class))
                .andReturn(bgpConfig).anyTimes();

        expect(sdnIpConfig.encap()).andReturn(EncapsulationType.NONE).anyTimes();
        replay(sdnIpConfig);
        expect(networkConfigService.getConfig(APPID, SdnIpConfig.class))
                .andReturn(sdnIpConfig).anyTimes();

        replay(networkConfigService);
        replay(interfaceService);

        intentSynchronizer = createMock(IntentSynchronizationService.class);
        replay(intentSynchronizer);

        peerConnectivityManager =
            new PeerConnectivityManager(APPID, intentSynchronizer,
                                        networkConfigService,
                                        CONFIG_APP_ID,
                                        interfaceService);
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
        reset(intentSynchronizer);

        // Setup the expected intents
        for (Intent intent : intentList) {
            intentSynchronizer.submit(eqExceptId(intent));
        }
        replay(intentSynchronizer);

        // Running the interface to be tested.
        peerConnectivityManager.start();

        verify(intentSynchronizer);
    }

    /**
     *  Tests a corner case, when there are no interfaces in the configuration.
     */
    @Test
    public void testNullInterfaces() {
        reset(interfaceService);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().anyTimes();

        expect(interfaceService.getInterfaces()).andReturn(
                Sets.newHashSet()).anyTimes();
        expect(interfaceService.getInterfacesByPort(s2Eth1))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getInterfacesByPort(s1Eth1))
        .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.10.101")))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.10.1")))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.20.101")))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.20.1")))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.30.101")))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.30.1")))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.40.101")))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.40.1")))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf("192.168.50.101")))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf("192.168.50.1")))
                .andReturn(null).anyTimes();

        replay(interfaceService);

        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerConnectivityManager.start();
        verify(intentSynchronizer);
    }

    /**
     *  Tests a corner case, when there is no BGP speakers in the configuration.
     */
    @Test
    public void testNullBgpSpeakers() {
        reset(bgpConfig);
        expect(bgpConfig.bgpSpeakers()).andReturn(Collections.emptySet()).anyTimes();
        replay(bgpConfig);

        reset(sdnIpConfig);
        expect(sdnIpConfig.encap()).andReturn(EncapsulationType.NONE).anyTimes();
        replay(sdnIpConfig);

        // We don't expect any intents in this case
        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerConnectivityManager.start();
        verify(intentSynchronizer);
    }

    /**
     * Tests a corner case, when there is no Interface configured for one BGP
     * peer.
     */
    @Test
    public void testNoPeerInterface() {
        IpAddress ip = IpAddress.valueOf("1.1.1.1");
        bgpSpeakers.clear();
        bgpSpeakers.add(new BgpConfig.BgpSpeakerConfig(Optional.of("foo"),
                VlanId.NONE, s1Eth100, Collections.singleton(ip)));
        reset(interfaceService);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expect(interfaceService.getMatchingInterface(ip)).andReturn(null).anyTimes();
        replay(interfaceService);

        // We don't expect any intents in this case
        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerConnectivityManager.start();
        verify(intentSynchronizer);
    }

}
