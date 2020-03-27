/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.MPLS;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;

import java.util.Collections;

import static org.easymock.EasyMock.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.onlab.packet.ICMP.*;
import static org.onlab.packet.ICMP6.ECHO_REPLY;
import static org.onosproject.segmentrouting.TestUtils.*;

/**
 * Tests for IcmpHandler.
 */
public class IcmpHandlerTest {

    private IcmpHandler icmpHandler;
    private MockPacketService packetService;
    private SegmentRoutingManager segmentRoutingManager;
    private ApplicationId testApplicationId = TestApplicationId.create("test");

    @Before
    public void setUp() {

        // Init
        ObjectMapper mapper = new ObjectMapper();
        ConfigApplyDelegate delegate = config -> { };

        // Setup configuration for app
        SegmentRoutingAppConfig appConfig = new SegmentRoutingAppConfig();
        JsonNode appTree = mapper.createObjectNode();
        appConfig.init(testApplicationId, "icmp-handler-test", appTree, mapper, delegate);
        appConfig.setSuppressSubnet(Collections.emptySet());

        // Setup configuration for the devices
        SegmentRoutingDeviceConfig remoteLeafConfig = new SegmentRoutingDeviceConfig();
        JsonNode remoteLeafTree = mapper.createObjectNode();
        remoteLeafConfig.init(REMOTE_LEAF, "icmp-handler-test", remoteLeafTree, mapper, delegate);
        remoteLeafConfig.setNodeSidIPv4(REMOTE_LEAF_SID4)
                .setNodeSidIPv6(REMOTE_LEAF_SID6)
                .setRouterIpv4(REMOTE_LEAF_LB4)
                .setRouterIpv6(REMOTE_LEAF_LB6)
                .setIsEdgeRouter(true)
                .setRouterMac(REMOTE_MAC.toString());
        SegmentRoutingDeviceConfig localLeafConfig = new SegmentRoutingDeviceConfig();
        JsonNode localLeafTree = mapper.createObjectNode();
        localLeafConfig.init(LOCAL_LEAF, "icmp-handler-test", localLeafTree, mapper, delegate);
        localLeafConfig.setNodeSidIPv4(LOCAL_LEAF_SID4)
                .setRouterIpv4(LOCAL_LEAF_LB4)
                .setNodeSidIPv6(LOCAL_LEAF_SID6)
                .setRouterIpv6(LOCAL_LEAF_LB6)
                .setIsEdgeRouter(true)
                .setRouterMac(LOCAL_MAC.toString());
        SegmentRoutingDeviceConfig localLeaf1Config = new SegmentRoutingDeviceConfig();
        JsonNode localLeaf1Tree = mapper.createObjectNode();
        localLeaf1Config.init(LOCAL_LEAF1, "icmp-handler-test", localLeaf1Tree, mapper, delegate);
        localLeaf1Config.setNodeSidIPv4(LOCAL_LEAF1_SID4)
                .setRouterIpv4(LOCAL_LEAF1_LB4)
                .setNodeSidIPv6(LOCAL_LEAF1_SID6)
                .setRouterIpv6(LOCAL_LEAF1_LB6)
                .setIsEdgeRouter(true)
                .setRouterMac(LOCAL_MAC1.toString())
                .setPairDeviceId(LOCAL_LEAF2)
                .setPairLocalPort(P3);
        SegmentRoutingDeviceConfig localLeaf2Config = new SegmentRoutingDeviceConfig();
        JsonNode localLeaf2Tree = mapper.createObjectNode();
        localLeaf2Config.init(LOCAL_LEAF2, "icmp-handler-test", localLeaf2Tree, mapper, delegate);
        localLeaf2Config.setNodeSidIPv4(LOCAL_LEAF2_SID4)
                .setRouterIpv4(LOCAL_LEAF2_LB4)
                .setNodeSidIPv6(LOCAL_LEAF2_SID6)
                .setRouterIpv6(LOCAL_LEAF2_LB6)
                .setIsEdgeRouter(true)
                .setRouterMac(LOCAL_MAC2.toString())
                .setPairDeviceId(LOCAL_LEAF1)
                .setPairLocalPort(P3);

        // Setup configuration for ports
        InterfaceConfig remoteLeafPorts1Config = new InterfaceConfig();
        ArrayNode remoteLeafPorts1Tree = mapper.createArrayNode();
        remoteLeafPorts1Config.init(CP12, "icmp-handler-test", remoteLeafPorts1Tree, mapper, delegate);
        remoteLeafPorts1Config.addInterface(INTF1);
        InterfaceConfig remoteLeafPorts2Config = new InterfaceConfig();
        ArrayNode remoteLeafPorts2Tree = mapper.createArrayNode();
        remoteLeafPorts2Config.init(CP13, "icmp-handler-test", remoteLeafPorts2Tree, mapper, delegate);
        remoteLeafPorts2Config.addInterface(INTF2);
        InterfaceConfig localLeafPortsConfig = new InterfaceConfig();
        ArrayNode localLeafPortsTree = mapper.createArrayNode();
        localLeafPortsConfig.init(CP1011, "icmp-handler-test", localLeafPortsTree, mapper, delegate);
        localLeafPortsConfig.addInterface(INTF111);
        InterfaceConfig localLeaf1PortsConfig = new InterfaceConfig();
        ArrayNode localLeaf1PortsTree = mapper.createArrayNode();
        localLeaf1PortsConfig.init(CP2011, "icmp-handler-test", localLeaf1PortsTree, mapper, delegate);
        localLeaf1PortsConfig.addInterface(INTF211);
        InterfaceConfig localLeaf2Ports1Config = new InterfaceConfig();
        ArrayNode localLeaf2Ports1Tree = mapper.createArrayNode();
        localLeaf2Ports1Config.init(CP2021, "icmp-handler-test", localLeaf2Ports1Tree, mapper, delegate);
        localLeaf2Ports1Config.addInterface(INTF212);
        InterfaceConfig localLeaf2Ports2Config = new InterfaceConfig();
        ArrayNode localLeaf2Ports2Tree = mapper.createArrayNode();
        localLeaf2Ports2Config.init(CP2024, "icmp-handler-test", localLeaf2Ports2Tree, mapper, delegate);
        localLeaf2Ports2Config.addInterface(INTF213);

        // Apply config
        MockNetworkConfigRegistry mockNetworkConfigRegistry = new MockNetworkConfigRegistry();

        mockNetworkConfigRegistry.applyConfig(remoteLeafConfig);
        mockNetworkConfigRegistry.applyConfig(remoteLeafPorts1Config);
        mockNetworkConfigRegistry.applyConfig(remoteLeafPorts2Config);
        mockNetworkConfigRegistry.applyConfig(localLeafConfig);
        mockNetworkConfigRegistry.applyConfig(localLeafPortsConfig);
        mockNetworkConfigRegistry.applyConfig(localLeaf1Config);
        mockNetworkConfigRegistry.applyConfig(localLeaf1PortsConfig);
        mockNetworkConfigRegistry.applyConfig(localLeaf2Config);
        mockNetworkConfigRegistry.applyConfig(localLeaf2Ports1Config);
        mockNetworkConfigRegistry.applyConfig(localLeaf2Ports2Config);

        segmentRoutingManager = new SegmentRoutingManager();
        segmentRoutingManager.appId = testApplicationId;
        packetService = new MockPacketService();
        segmentRoutingManager.packetService = packetService;
        segmentRoutingManager.cfgService = mockNetworkConfigRegistry;
        segmentRoutingManager.neighbourResolutionService = new MockNeighbourResolutionService();
        segmentRoutingManager.interfaceService = new MockInterfaceService(ImmutableSet.of(
                INTF1, INTF2, INTF111, INTF211, INTF212, INTF213));
        segmentRoutingManager.deviceConfiguration = new DeviceConfiguration(segmentRoutingManager);
        segmentRoutingManager.ipHandler = new IpHandler(segmentRoutingManager);
        segmentRoutingManager.deviceService = createMock(DeviceService.class);
        segmentRoutingManager.routeService = new MockRouteService(ROUTE_STORE);
        segmentRoutingManager.hostService = new MockHostService(Collections.emptySet());
        icmpHandler = new IcmpHandler(segmentRoutingManager);
    }

    // Ping to our gateway
    @Test
    public void testPing4MyGateway() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_MY, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_MY.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_MY.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_MY.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV4_MY.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to our gateway
    @Test
    public void testPing6MyGateway() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_MY, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_MY.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_MY.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_MY.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV6_MY.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to a gateway attached to our leaf
    @Test
    public void testPing4LocalGateway() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_LOCAL, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_LOCAL.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_LOCAL.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_LOCAL.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_LOCAL.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV4_MY.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a gateway attached to our leaf
    @Test
    public void testPing6LocalGateway() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LOCAL, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LOCAL.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LOCAL.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LOCAL.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LOCAL.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV6_MY.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to a gateway attached only to the pair leaf (routing through spine)
    @Test
    public void testPing4RemoteGatewaySamePair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_SAME, CP2025);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_SAME.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_SAME.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_SAME.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(LOCAL_LEAF1_SID4));
        assertTrue(mpls.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_SAME.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a gateway attached only to the pair leaf (routing through spine)
    @Test
    public void testPing6RemoteGatewaySamePair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_SAME, CP2025);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_SAME.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_SAME.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_SAME.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(LOCAL_LEAF1_SID6));
        assertTrue(mpls.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_SAME.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to a gateway but destination leaf is down
    @Test
    public void testPing4RemoteGatewayLeafDown() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF))
                .andReturn(false)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4, CP11);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4.getSourceMAC());
        assertNull(ethernet);

        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a gateway but destination leaf is down
    @Test
    public void testPing6RemoteGatewayLeafDown() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF))
                .andReturn(false)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6, CP11);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6.getSourceMAC());
        assertNull(ethernet);

        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to a gateway but one of the destination leaf is down
    @Test
    public void testPing4RemoteGatewayLeaf1Down() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(false)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV41, CP11);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV41.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV41.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV41.getSourceMAC()));
        assertThat(ethernet.getEtherType(), is(Ethernet.MPLS_UNICAST));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(LOCAL_LEAF2_SID4));
        assertTrue(mpls.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a gateway but one of the destination leaf is down
    @Test
    public void testPing6RemoteGatewayLeaf2Down() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(false)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV61, CP11);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV61.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV61.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV61.getSourceMAC()));
        assertThat(ethernet.getEtherType(), is(Ethernet.MPLS_UNICAST));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(LOCAL_LEAF1_SID6));
        assertTrue(mpls.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a link local address
    @Test
    public void testPing6LinkLocalAddress() {
        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LL, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LL.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LL.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LL.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LL.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV6_LL.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
    }

    // Ping to the looback of our leaf
    @Test
    public void testPing4Loopback() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_LOOPBACK, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_LOOPBACK.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_LOOPBACK.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_LOOPBACK.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_LOOPBACK.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV4_MY.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to the looback of our leaf
    @Test
    public void testPing6Loopback() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(REMOTE_LEAF))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LOOPBACK, CP12);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LOOPBACK.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LOOPBACK.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LOOPBACK.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LOOPBACK.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV6_MY.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to the looback of our leaf (pair)
    @Test
    public void testPing4LoopbackPair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_LOOPBACK_PAIR, CP2011);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_LOOPBACK_PAIR.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to the looback of our leaf (pair)
    @Test
    public void testPing6LoopbackPair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LOOPBACK_PAIR, CP2021);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LOOPBACK_PAIR.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to the loopback of the leaf but hashing of the bond interfaces sends to wrong leaf
    @Test
    public void testPing4LoopbackPairDifferentLeaf() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_LOOPBACK_PAIR, CP2021);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_LOOPBACK_PAIR.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to the loopback of the leaf but hashing of the bond interfaces sends to wrong leaf
    @Test
    public void testPing6LoopbackPairDifferentLeaf() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LOOPBACK_PAIR, CP2011);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LOOPBACK_PAIR.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping loopback of a destination that is down but
    // hashing of the bond interfaces sends to other leaf
    @Test
    public void testPing4LoopbackPairDifferentLeafDown() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(false)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_LOOPBACK_PAIR, CP2021);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_LOOPBACK_PAIR.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 loopback of a destination that is down but
    // hashing of the bond interfaces sends to other leaf
    @Test
    public void testPing6LoopbackPairDifferentLeafDown() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(false)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_LOOPBACK_PAIR, CP2011);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_LOOPBACK_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_LOOPBACK_PAIR.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping to a dh gateway
    @Test
    public void testPing4GatewayPair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmp(ETH_REQ_IPV4_GATEWAY_PAIR, CP2011);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV4_GATEWAY_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV4_GATEWAY_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV4_GATEWAY_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv4);
        IPv4 ip = (IPv4) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV4_GATEWAY_PAIR.toInt()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV41.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REPLY));
        assertThat(icmp.getIcmpCode(), is(CODE_ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

    // Ping6 to a dh gateway
    @Test
    public void testPing6GatewayPair() {
        // Expected behavior
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF1))
                .andReturn(true)
                .times(1);
        expect(segmentRoutingManager.deviceService.isAvailable(LOCAL_LEAF2))
                .andReturn(true)
                .times(1);
        replay(segmentRoutingManager.deviceService);

        // Process
        icmpHandler.processIcmpv6(ETH_REQ_IPV6_GATEWAY_PAIR, CP2021);

        // Verify packet-out
        Ethernet ethernet = packetService.getEthernetPacket(ETH_REQ_IPV6_GATEWAY_PAIR.getSourceMAC());
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_REQ_IPV6_GATEWAY_PAIR.getDestinationMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_REQ_IPV6_GATEWAY_PAIR.getSourceMAC()));
        assertTrue(ethernet.getPayload() instanceof IPv6);
        IPv6 ip = (IPv6) ethernet.getPayload();
        assertThat(ip.getSourceAddress(), is(DST_IPV6_GATEWAY_PAIR.toOctets()));
        assertThat(ip.getDestinationAddress(), is(SRC_IPV61.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REPLY));
        // Verify behavior
        verify(segmentRoutingManager.deviceService);
    }

}
