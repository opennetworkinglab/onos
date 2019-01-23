/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.dhcprelay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.CircuitId;
import org.onlab.packet.dhcp.DhcpOption;
import org.onlab.packet.dhcp.DhcpRelayAgentOption;
import org.onlab.packet.dhcp.Dhcp6InterfaceIdOption;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onlab.packet.dhcp.Dhcp6IaNaOption;
import org.onlab.packet.dhcp.Dhcp6IaPdOption;
import org.onlab.packet.dhcp.Dhcp6IaAddressOption;
import org.onlab.packet.dhcp.Dhcp6IaPrefixOption;
import org.onlab.packet.dhcp.Dhcp6Option;
import org.onlab.packet.dhcp.Dhcp6ClientIdOption;
import org.onlab.packet.dhcp.Dhcp6Duid;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayStoreEvent;
import org.onosproject.dhcprelay.store.DhcpRelayCounters;
import org.onosproject.dhcprelay.store.DhcpRelayCountersStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.config.DefaultDhcpRelayConfig;
import org.onosproject.dhcprelay.config.DhcpServerConfig;
import org.onosproject.dhcprelay.config.IgnoreDhcpConfig;
import org.onosproject.dhcprelay.config.IndirectDhcpRelayConfig;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceServiceAdapter;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteStoreAdapter;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketContextAdapter;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.store.StoreDelegate;

import org.osgi.service.component.ComponentContext;
import org.onlab.packet.DHCP6;
import org.onlab.packet.IPv6;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onosproject.dhcprelay.DhcpRelayManager.DHCP_RELAY_APP;

public class DhcpRelayManagerTest {
    private static final int EVENT_PROCESSING_MS = 1000;
    private static final int PKT_PROCESSING_MS = 500;
    private static final short VLAN_LEN = 2;
    private static final short SEPARATOR_LEN = 1;
    private static final String CONFIG_FILE_PATH = "dhcp-relay.json";
    private static final DeviceId DEV_1_ID = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV_2_ID = DeviceId.deviceId("of:0000000000000002");
    // Ip address for interfaces
    private static final InterfaceIpAddress INTERFACE_IP = InterfaceIpAddress.valueOf("10.0.3.254/32");
    private static final InterfaceIpAddress INTERFACE_IP_V6 = InterfaceIpAddress.valueOf("2001:db8:1::254/128");
    private static final List<InterfaceIpAddress> INTERFACE_IPS = ImmutableList.of(INTERFACE_IP, INTERFACE_IP_V6);

    // DHCP client (will send without option 82)
    private static final Ip4Address IP_FOR_CLIENT = Ip4Address.valueOf("10.0.0.1");
    private static final Ip6Address IP_FOR_CLIENT_V6 = Ip6Address.valueOf("2001:db8:1::110");
    private static final IpPrefix PREFIX_FOR_CLIENT_V6 = IpPrefix.valueOf("2001:db8:10::/56");
    private static final IpPrefix PREFIX_FOR_ZERO = IpPrefix.valueOf("::/0");
    private static final MacAddress CLIENT_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId CLIENT_VLAN = VlanId.vlanId("100");
    private static final ConnectPoint CLIENT_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final MacAddress CLIENT_IFACE_MAC = MacAddress.valueOf("00:00:00:00:11:01");
    private static final HostLocation CLIENT_LOCATION = new HostLocation(CLIENT_CP, 0);
    private static final HostId CLIENT_HOST_ID = HostId.hostId(CLIENT_MAC, CLIENT_VLAN);
    private static final Ip6Address CLIENT_LL_IP_V6 = Ip6Address.valueOf("fe80::200:00ff:fe00:0001");
    private static final Host EXISTS_HOST = new DefaultHost(Dhcp4HandlerImpl.PROVIDER_ID,
                                                            CLIENT_HOST_ID, CLIENT_MAC, CLIENT_VLAN,
                                                            CLIENT_LOCATION, ImmutableSet.of(CLIENT_LL_IP_V6));
    private static final Interface CLIENT_INTERFACE = createInterface("C1",
                                                                      CLIENT_CP,
                                                                      INTERFACE_IPS,
                                                                      CLIENT_IFACE_MAC,
                                                                      CLIENT_VLAN,
                                                                      null);



    // Dual homing test
    private static final ConnectPoint CLIENT_DH_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/3");
    private static final HostLocation CLIENT_DH_LOCATION = new HostLocation(CLIENT_DH_CP, 0);
    private static final Interface CLIENT_DH_INTERFACE = createInterface("C1-DH",
                                                                         CLIENT_DH_CP,
                                                                         INTERFACE_IPS,
                                                                         CLIENT_IFACE_MAC,
                                                                         CLIENT_VLAN,
                                                                         null);


    // DHCP client 2 (will send with option 82, so the vlan should equals to vlan from server)
    private static final MacAddress CLIENT2_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId CLIENT2_VLAN = VlanId.NONE;
    private static final VlanId CLIENT2_VLAN_NATIVE = VlanId.vlanId("20");
    private static final ConnectPoint CLIENT2_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final MacAddress CLIENT2_IFACE_MAC = MacAddress.valueOf("00:00:00:00:11:01");
    private static final Interface CLIENT2_INTERFACE = createInterface("C2",
                                                                       CLIENT2_CP,
                                                                       INTERFACE_IPS,
                                                                       CLIENT2_IFACE_MAC,
                                                                       CLIENT2_VLAN,
                                                                       CLIENT2_VLAN_NATIVE);
    private static final VlanId CLIENT_BOGUS_VLAN = VlanId.vlanId("108");

    // Outer relay information
    private static final Ip4Address OUTER_RELAY_IP = Ip4Address.valueOf("10.0.6.253");
    private static final Ip6Address OUTER_RELAY_IP_V6 = Ip6Address.valueOf("2001:db8:1::4");
    private static final Ip6Address OUTER_RELAY_LL_IP_V6 = Ip6Address.valueOf("fe80::200:0102:0304:0501");
    private static final Set<IpAddress> OUTER_RELAY_IPS = ImmutableSet.of(OUTER_RELAY_IP,
            OUTER_RELAY_IP_V6,
            OUTER_RELAY_LL_IP_V6);
    private static final MacAddress OUTER_RELAY_MAC = MacAddress.valueOf("00:01:02:03:04:05");
    private static final VlanId OUTER_RELAY_VLAN = VlanId.NONE;
    private static final ConnectPoint OUTER_RELAY_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final HostLocation OUTER_REPLAY_HL = new HostLocation(OUTER_RELAY_CP, 0);
    private static final HostId OUTER_RELAY_HOST_ID = HostId.hostId(OUTER_RELAY_MAC, OUTER_RELAY_VLAN);
    private static final Host OUTER_RELAY_HOST = new DefaultHost(Dhcp4HandlerImpl.PROVIDER_ID,
                                                                 OUTER_RELAY_HOST_ID,
                                                                 OUTER_RELAY_MAC,
                                                                 OUTER_RELAY_VLAN,
                                                                 OUTER_REPLAY_HL,
                                                                 OUTER_RELAY_IPS);

    // DHCP Server
    private static final MacAddress SERVER_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId SERVER_VLAN = VlanId.NONE;
    private static final VlanId SERVER_VLAN_NATIVE = VlanId.vlanId("10");
    private static final ConnectPoint SERVER_CONNECT_POINT =
            ConnectPoint.deviceConnectPoint("of:0000000000000001/5");
    private static final HostLocation SERVER_LOCATION =
            new HostLocation(SERVER_CONNECT_POINT, 0);
    private static final Ip4Address GATEWAY_IP = Ip4Address.valueOf("10.0.5.253");
    private static final Ip6Address GATEWAY_IP_V6 = Ip6Address.valueOf("2000::105:253");
    private static final Ip4Address SERVER_IP = Ip4Address.valueOf("10.0.3.253");
    private static final Ip6Address SERVER_IP_V6 = Ip6Address.valueOf("2000::103:253");
    private static final Ip6Address SERVER_IP_V6_MCAST = Ip6Address.valueOf("ff02::1:2");
    private static final Set<IpAddress> DHCP_SERVER_IPS = ImmutableSet.of(SERVER_IP, SERVER_IP_V6);
    private static final HostId SERVER_HOST_ID = HostId.hostId(SERVER_MAC, SERVER_VLAN);
    private static final Host SERVER_HOST = new DefaultHost(Dhcp4HandlerImpl.PROVIDER_ID,
                                                            SERVER_HOST_ID,
                                                            SERVER_MAC,
                                                            SERVER_VLAN,
                                                            SERVER_LOCATION,
                                                            DHCP_SERVER_IPS);
    private static final MacAddress SERVER_IFACE_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final Interface SERVER_INTERFACE = createInterface("SERVER",
                                                                      SERVER_CONNECT_POINT,
                                                                      INTERFACE_IPS,
                                                                      SERVER_IFACE_MAC,
                                                                      SERVER_VLAN,
                                                                      SERVER_VLAN_NATIVE);

    // Relay agent config
    private static final Ip4Address RELAY_AGENT_IP = Ip4Address.valueOf("10.0.4.254");

    private static final List<TrafficSelector> DHCP_SELECTORS = buildClientDhcpSelectors();

    // Components
    private static final ApplicationId APP_ID = TestApplicationId.create(DhcpRelayManager.DHCP_RELAY_APP);
    private static final DefaultDhcpRelayConfig CONFIG = new MockDefaultDhcpRelayConfig();
    private static final IndirectDhcpRelayConfig CONFIG_INDIRECT = new MockIndirectDhcpRelayConfig();
    private static final Set<Interface> INTERFACES = ImmutableSet.of(
            CLIENT_INTERFACE,
            CLIENT2_INTERFACE,
            SERVER_INTERFACE,
            CLIENT_DH_INTERFACE
    );
    private static final String NON_ONOS_CID = "Non-ONOS circuit ID";
    private static final VlanId IGNORED_VLAN = VlanId.vlanId("100");
    private static final int IGNORE_CONTROL_PRIORITY = PacketPriority.CONTROL.priorityValue() + 1000;

    private DhcpRelayManager manager;
    private MockPacketService packetService;
    private MockRouteStore mockRouteStore;
    private MockDhcpRelayStore mockDhcpRelayStore;
    private MockDhcpRelayCountersStore mockDhcpRelayCountersStore;
    private HostProviderService mockHostProviderService;
    private FlowObjectiveService flowObjectiveService;
    private DeviceService deviceService;
    private Dhcp4HandlerImpl v4Handler;
    private Dhcp6HandlerImpl v6Handler;

    private static Interface createInterface(String name, ConnectPoint connectPoint,
                                             List<InterfaceIpAddress> interfaceIps,
                                             MacAddress macAddress,
                                             VlanId vlanId,
                                             VlanId vlanNative) {

        if (vlanId.equals(VlanId.NONE)) {
            return new Interface(name, connectPoint, interfaceIps, macAddress, vlanId,
                                 null, null, vlanNative);
        } else {
            return new Interface(name, connectPoint, interfaceIps, macAddress, vlanId,
                                 null, ImmutableSet.of(vlanId), null);
        }
    }

    @Before
    public void setup() {
        manager = new DhcpRelayManager();
        manager.cfgService = createNiceMock(NetworkConfigRegistry.class);

        expect(manager.cfgService.getConfig(APP_ID, DefaultDhcpRelayConfig.class))
                .andReturn(CONFIG)
                .anyTimes();

        expect(manager.cfgService.getConfig(APP_ID, IndirectDhcpRelayConfig.class))
                .andReturn(CONFIG_INDIRECT)
                .anyTimes();

        manager.coreService = createNiceMock(CoreService.class);
        expect(manager.coreService.registerApplication(anyString()))
                .andReturn(APP_ID).anyTimes();

        manager.hostService = createNiceMock(HostService.class);

        expect(manager.hostService.getHostsByIp(OUTER_RELAY_IP_V6))
                .andReturn(ImmutableSet.of(OUTER_RELAY_HOST)).anyTimes();
        expect(manager.hostService.getHostsByIp(SERVER_IP))
                .andReturn(ImmutableSet.of(SERVER_HOST)).anyTimes();
        expect(manager.hostService.getHostsByIp(SERVER_IP_V6))
                .andReturn(ImmutableSet.of(SERVER_HOST)).anyTimes();
        expect(manager.hostService.getHostsByIp(GATEWAY_IP))
                .andReturn(ImmutableSet.of(SERVER_HOST)).anyTimes();
        expect(manager.hostService.getHostsByIp(GATEWAY_IP_V6))
                .andReturn(ImmutableSet.of(SERVER_HOST)).anyTimes();
        expect(manager.hostService.getHostsByIp(CLIENT_LL_IP_V6))
                .andReturn(ImmutableSet.of(EXISTS_HOST)).anyTimes();

        expect(manager.hostService.getHost(OUTER_RELAY_HOST_ID)).andReturn(OUTER_RELAY_HOST).anyTimes();

        packetService = new MockPacketService();
        manager.packetService = packetService;
        manager.compCfgService = createNiceMock(ComponentConfigService.class);
        deviceService = createNiceMock(DeviceService.class);

        Device device = createNiceMock(Device.class);
        expect(device.is(Pipeliner.class)).andReturn(true).anyTimes();

        expect(deviceService.getDevice(DEV_1_ID)).andReturn(device).anyTimes();
        expect(deviceService.getDevice(DEV_2_ID)).andReturn(device).anyTimes();
        replay(deviceService, device);

        mockRouteStore = new MockRouteStore();
        mockDhcpRelayStore = new MockDhcpRelayStore();
        mockDhcpRelayCountersStore = new MockDhcpRelayCountersStore();

        manager.dhcpRelayStore = mockDhcpRelayStore;

        manager.deviceService = deviceService;

        manager.interfaceService = new MockInterfaceService();
        flowObjectiveService = EasyMock.niceMock(FlowObjectiveService.class);
        mockHostProviderService = createNiceMock(HostProviderService.class);
        v4Handler = new Dhcp4HandlerImpl();
        v4Handler.providerService = mockHostProviderService;
        v4Handler.dhcpRelayStore = mockDhcpRelayStore;
        v4Handler.hostService = manager.hostService;
        v4Handler.interfaceService = manager.interfaceService;
        v4Handler.packetService = manager.packetService;
        v4Handler.routeStore = mockRouteStore;
        v4Handler.coreService = createNiceMock(CoreService.class);
        v4Handler.flowObjectiveService = flowObjectiveService;
        v4Handler.appId = TestApplicationId.create(Dhcp4HandlerImpl.DHCP_V4_RELAY_APP);
        v4Handler.deviceService = deviceService;
        manager.v4Handler = v4Handler;

        v6Handler = new Dhcp6HandlerImpl();
        v6Handler.dhcpRelayStore = mockDhcpRelayStore;
        v6Handler.dhcpRelayCountersStore = mockDhcpRelayCountersStore;
        v6Handler.hostService = manager.hostService;
        v6Handler.interfaceService = manager.interfaceService;
        v6Handler.packetService = manager.packetService;
        v6Handler.routeStore = mockRouteStore;
        v6Handler.providerService = mockHostProviderService;
        v6Handler.coreService = createNiceMock(CoreService.class);
        v6Handler.flowObjectiveService = flowObjectiveService;
        v6Handler.appId = TestApplicationId.create(Dhcp6HandlerImpl.DHCP_V6_RELAY_APP);
        v6Handler.deviceService = deviceService;
        manager.v6Handler = v6Handler;

        // properties
        Dictionary<String, Object> dictionary = createNiceMock(Dictionary.class);
        expect(dictionary.get("arpEnabled")).andReturn(true).anyTimes();
        expect(dictionary.get("dhcpPollInterval")).andReturn(120).anyTimes();
        ComponentContext context = createNiceMock(ComponentContext.class);
        expect(context.getProperties()).andReturn(dictionary).anyTimes();

        replay(manager.cfgService, manager.coreService, manager.hostService,
               manager.compCfgService, dictionary, context);
        manager.activate(context);
    }

    @After
    public void tearDown() {
        manager.deactivate();
    }

    /**
     * Relay a DHCP packet without option 82.
     * Should add new host to host store after dhcp ack.
     */
    @Test
    public void relayDhcpWithoutAgentInfo() {
        replay(mockHostProviderService);
        // send request
        packetService.processPacket(new TestDhcpRequestPacketContext(CLIENT_MAC,
                                                                     CLIENT_VLAN,
                                                                     CLIENT_CP,
                                                                     INTERFACE_IP.ipAddress().getIp4Address(),
                                                                     false));
        // won't trigger the host provider service
        verify(mockHostProviderService);
        reset(mockHostProviderService);

        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

        HostId expectHostId = HostId.hostId(CLIENT_MAC, CLIENT_VLAN);
        Capture<HostDescription> capturedHostDesc = newCapture();
        mockHostProviderService.hostDetected(eq(expectHostId), capture(capturedHostDesc), eq(false));
        replay(mockHostProviderService);
        // send ack
        packetService.processPacket(new TestDhcpAckPacketContext(CLIENT_CP, CLIENT_MAC,
                                                                 CLIENT_VLAN, INTERFACE_IP.ipAddress().getIp4Address(),
                                                                 false));
        assertAfter(PKT_PROCESSING_MS, () -> verify(mockHostProviderService));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

        HostDescription host = capturedHostDesc.getValue();
        assertAfter(PKT_PROCESSING_MS, () -> assertFalse(host.configured()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(CLIENT_CP.deviceId(), host.location().elementId()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(CLIENT_CP.port(), host.location().port()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(1, host.ipAddress().size()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(IP_FOR_CLIENT, host.ipAddress().iterator().next()));
    }

    /**
     * Relay a DHCP packet with option 82 (Indirectly connected host).
     */
    @Test
    public void relayDhcpWithAgentInfo() {
        replay(mockHostProviderService);
        // Assume outer dhcp relay agent exists in store already
        // send request
        packetService.processPacket(new TestDhcpRequestPacketContext(CLIENT2_MAC,
                CLIENT2_VLAN,
                CLIENT2_CP,
                INTERFACE_IP.ipAddress().getIp4Address(),
                true));
        // No routes
        assertEquals(0, mockRouteStore.routes.size());

        // Make sure the REQUEST packet has been processed before start sending ACK
        assertAfter(PKT_PROCESSING_MS, () -> assertNotNull(packetService.emittedPacket));

        // send ack
        packetService.processPacket(new TestDhcpAckPacketContext(CLIENT2_CP,
                                                                 CLIENT2_MAC,
                                                                 CLIENT2_VLAN,
                                                                 INTERFACE_IP.ipAddress().getIp4Address(),
                                                                 true));

        // won't trigger the host provider service
        verify(mockHostProviderService);
        reset(mockHostProviderService);
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(1, mockRouteStore.routes.size()));

        Route route = mockRouteStore.routes.get(0);
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(OUTER_RELAY_IP, route.nextHop()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(IP_FOR_CLIENT.toIpPrefix(), route.prefix()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(Route.Source.DHCP, route.source()));
    }

    @Test
    public void testWithRelayAgentConfig() throws DeserializationException {
        manager.v4Handler
                .setDefaultDhcpServerConfigs(ImmutableList.of(new MockDhcpServerConfig(RELAY_AGENT_IP)));
        manager.v4Handler
                .setIndirectDhcpServerConfigs(ImmutableList.of(new MockDhcpServerConfig(RELAY_AGENT_IP)));
        packetService.processPacket(new TestDhcpRequestPacketContext(CLIENT2_MAC,
                                                                     CLIENT2_VLAN,
                                                                     CLIENT2_CP,
                                                                     INTERFACE_IP.ipAddress().getIp4Address(),
                                                                     true));
        assertAfter(PKT_PROCESSING_MS, () -> assertNotNull(packetService.emittedPacket));
        OutboundPacket outPacket = packetService.emittedPacket;
        byte[] outData = outPacket.data().array();
        Ethernet eth = Ethernet.deserializer().deserialize(outData, 0, outData.length);
        IPv4 ip = (IPv4) eth.getPayload();
        UDP udp = (UDP) ip.getPayload();
        DHCP dhcp = (DHCP) udp.getPayload();
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(RELAY_AGENT_IP.toInt(), dhcp.getGatewayIPAddress()));
    }

    @Test
    public void testArpRequest() throws Exception {
        packetService.processPacket(new TestArpRequestPacketContext(CLIENT_INTERFACE));
        assertAfter(PKT_PROCESSING_MS, () -> assertNotNull(packetService.emittedPacket));
        OutboundPacket outboundPacket = packetService.emittedPacket;
        byte[] outPacketData = outboundPacket.data().array();
        Ethernet eth = Ethernet.deserializer().deserialize(outPacketData, 0, outPacketData.length);

        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(eth.getEtherType(), Ethernet.TYPE_ARP));
        ARP arp = (ARP) eth.getPayload();
        assertAfter(PKT_PROCESSING_MS, () ->
                assertArrayEquals(arp.getSenderHardwareAddress(), CLIENT_INTERFACE.mac().toBytes()));
    }

    /**
     * Ignores specific vlans from specific devices if config.
     *
     * @throws Exception the exception from this test
     */
    @Test
    public void testIgnoreVlan() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        IgnoreDhcpConfig config = new IgnoreDhcpConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(IgnoreDhcpConfig.KEY);
        config.init(APP_ID, IgnoreDhcpConfig.KEY, json, om, null);

        Capture<Objective> capturedFromDev1 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_1_ID), capture(capturedFromDev1));
        expectLastCall().times(DHCP_SELECTORS.size());
        Capture<Objective> capturedFromDev2 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_2_ID), capture(capturedFromDev2));
        expectLastCall().times(DHCP_SELECTORS.size());
        replay(flowObjectiveService);
        manager.updateConfig(config);
        verify(flowObjectiveService);

        List<Objective> objectivesFromDev1 = capturedFromDev1.getValues();
        List<Objective> objectivesFromDev2 = capturedFromDev2.getValues();

        assertTrue(objectivesFromDev1.containsAll(objectivesFromDev2));
        assertTrue(objectivesFromDev2.containsAll(objectivesFromDev1));

        for (int index = 0; index < objectivesFromDev1.size(); index++) {
            TrafficSelector selector =
                    DefaultTrafficSelector.builder(DHCP_SELECTORS.get(index))
                    .matchVlanId(IGNORED_VLAN)
                    .build();
            ForwardingObjective fwd = (ForwardingObjective) objectivesFromDev1.get(index);
            assertEquals(selector, fwd.selector());
            assertEquals(DefaultTrafficTreatment.emptyTreatment(), fwd.treatment());
            assertEquals(IGNORE_CONTROL_PRIORITY, fwd.priority());
            assertEquals(ForwardingObjective.Flag.VERSATILE, fwd.flag());
            assertEquals(Objective.Operation.ADD, fwd.op());
            fwd.context().ifPresent(ctx -> {
                ctx.onSuccess(fwd);
            });
        }
        objectivesFromDev2.forEach(obj -> obj.context().ifPresent(ctx -> ctx.onSuccess(obj)));
        assertEquals(2, v4Handler.ignoredVlans.size());
        assertEquals(2, v6Handler.ignoredVlans.size());
    }

    /**
     * "IgnoreVlan" policy should be removed when the config removed.
     */
    @Test
    public void testRemoveIgnoreVlan() {
        v4Handler.ignoredVlans.put(DEV_1_ID, IGNORED_VLAN);
        v4Handler.ignoredVlans.put(DEV_2_ID, IGNORED_VLAN);
        v6Handler.ignoredVlans.put(DEV_1_ID, IGNORED_VLAN);
        v6Handler.ignoredVlans.put(DEV_2_ID, IGNORED_VLAN);
        IgnoreDhcpConfig config = new IgnoreDhcpConfig();

        Capture<Objective> capturedFromDev1 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_1_ID), capture(capturedFromDev1));
        expectLastCall().times(DHCP_SELECTORS.size());
        Capture<Objective> capturedFromDev2 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_2_ID), capture(capturedFromDev2));
        expectLastCall().times(DHCP_SELECTORS.size());
        replay(flowObjectiveService);
        manager.removeConfig(config);
        verify(flowObjectiveService);

        List<Objective> objectivesFromDev1 = capturedFromDev1.getValues();
        List<Objective> objectivesFromDev2 = capturedFromDev2.getValues();

        assertTrue(objectivesFromDev1.containsAll(objectivesFromDev2));
        assertTrue(objectivesFromDev2.containsAll(objectivesFromDev1));

        for (int index = 0; index < objectivesFromDev1.size(); index++) {
            TrafficSelector selector =
                    DefaultTrafficSelector.builder(DHCP_SELECTORS.get(index))
                    .matchVlanId(IGNORED_VLAN)
                    .build();
            ForwardingObjective fwd = (ForwardingObjective) objectivesFromDev1.get(index);
            assertEquals(selector, fwd.selector());
            assertEquals(DefaultTrafficTreatment.emptyTreatment(), fwd.treatment());
            assertEquals(IGNORE_CONTROL_PRIORITY, fwd.priority());
            assertEquals(ForwardingObjective.Flag.VERSATILE, fwd.flag());
            assertEquals(Objective.Operation.REMOVE, fwd.op());
            fwd.context().ifPresent(ctx -> {
                ctx.onSuccess(fwd);
            });
        }
        objectivesFromDev2.forEach(obj -> obj.context().ifPresent(ctx -> ctx.onSuccess(obj)));
        assertEquals(0, v4Handler.ignoredVlans.size());
        assertEquals(0, v6Handler.ignoredVlans.size());
    }

    /**
     * Should ignore ignore rules installation when device not available.
     */
    @Test
    public void testIgnoreUnknownDevice() throws IOException {
        reset(manager.deviceService);
        Device device = createNiceMock(Device.class);
        expect(device.is(Pipeliner.class)).andReturn(true).anyTimes();

        expect(manager.deviceService.getDevice(DEV_1_ID)).andReturn(device).anyTimes();
        expect(manager.deviceService.getDevice(DEV_2_ID)).andReturn(null).anyTimes();

        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        IgnoreDhcpConfig config = new IgnoreDhcpConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(IgnoreDhcpConfig.KEY);
        config.init(APP_ID, IgnoreDhcpConfig.KEY, json, om, null);

        Capture<Objective> capturedFromDev1 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_1_ID), capture(capturedFromDev1));
        expectLastCall().times(DHCP_SELECTORS.size());
        replay(flowObjectiveService, manager.deviceService, device);

        manager.updateConfig(config);
        capturedFromDev1.getValues().forEach(obj -> obj.context().ifPresent(ctx -> ctx.onSuccess(obj)));

        assertEquals(1, v4Handler.ignoredVlans.size());
        assertEquals(1, v6Handler.ignoredVlans.size());
    }

    /**
     * Should try install ignore rules when device comes up.
     */
    @Test
    public void testInstallIgnoreRuleWhenDeviceComesUp() throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        IgnoreDhcpConfig config = new IgnoreDhcpConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(IgnoreDhcpConfig.KEY);
        config.init(APP_ID, IgnoreDhcpConfig.KEY, json, om, null);

        reset(manager.cfgService, flowObjectiveService, manager.deviceService);
        expect(manager.cfgService.getConfig(APP_ID, IgnoreDhcpConfig.class))
                .andReturn(config).anyTimes();

        Device device = createNiceMock(Device.class);
        expect(device.is(Pipeliner.class)).andReturn(true).anyTimes();
        expect(device.id()).andReturn(DEV_1_ID).anyTimes();
        expect(manager.deviceService.getDevice(DEV_1_ID)).andReturn(device).anyTimes();
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device);
        Capture<Objective> capturedFromDev1 = newCapture(CaptureType.ALL);
        flowObjectiveService.apply(eq(DEV_1_ID), capture(capturedFromDev1));
        expectLastCall().times(DHCP_SELECTORS.size());
        replay(manager.cfgService, flowObjectiveService, manager.deviceService, device);

        manager.deviceListener.event(event);

        // Wait until all flow objective events are captured before triggering onSuccess
        int expectFlowObjCount = Dhcp4HandlerImpl.DHCP_SELECTORS.size() + Dhcp6HandlerImpl.DHCP_SELECTORS.size();
        assertAfter(EVENT_PROCESSING_MS, () -> assertEquals(expectFlowObjCount, capturedFromDev1.getValues().size()));
        capturedFromDev1.getValues().forEach(obj -> obj.context().ifPresent(ctx -> ctx.onSuccess(obj)));

        assertAfter(EVENT_PROCESSING_MS, () -> assertEquals(1, v4Handler.ignoredVlans.size()));
        assertAfter(EVENT_PROCESSING_MS, () -> assertEquals(1, v6Handler.ignoredVlans.size()));
    }

    /**
     * Relay a DHCP6 packet without relay option
     * Note: Should add new host to host store after dhcp ack.
     */
    @Test
    public void relayDhcp6WithoutAgentInfo() {
        replay(mockHostProviderService);
        // send request
        packetService.processPacket(new TestDhcp6RequestPacketContext(DHCP6.MsgType.REQUEST.value(),
                                                                     CLIENT_MAC,
                                                                     CLIENT_VLAN,
                                                                     CLIENT_CP,
                                                                     INTERFACE_IP_V6.ipAddress().getIp6Address(),
                                                                     0));

        verify(mockHostProviderService);
        reset(mockHostProviderService);
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

        Capture<HostDescription> capturedHostDesc = newCapture();
        mockHostProviderService.hostDetected(eq(HostId.hostId(CLIENT_MAC, CLIENT_VLAN)),
                                             capture(capturedHostDesc), eq(false));
        replay(mockHostProviderService);
        // send reply
        packetService.processPacket(new TestDhcp6ReplyPacketContext(DHCP6.MsgType.REPLY.value(),
                                                                    CLIENT_CP, CLIENT_MAC,
                                                                    CLIENT_VLAN,
                                                                    INTERFACE_IP_V6.ipAddress().getIp6Address(),
                                                                    0, false, CLIENT_VLAN));
        assertAfter(PKT_PROCESSING_MS, () -> verify(mockHostProviderService));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

        HostDescription host = capturedHostDesc.getValue();
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(CLIENT_VLAN, host.vlan()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(CLIENT_CP.deviceId(), host.location().elementId()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(CLIENT_CP.port(), host.location().port()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(1, host.ipAddress().size()));
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(IP_FOR_CLIENT_V6, host.ipAddress().iterator().next()));

        // send release
        packetService.processPacket(new TestDhcp6RequestPacketContext(DHCP6.MsgType.RELEASE.value(),
                CLIENT_MAC,
                CLIENT_VLAN,
                CLIENT_CP,
                INTERFACE_IP_V6.ipAddress().getIp6Address(),
                0));

        assertAfter(PKT_PROCESSING_MS,
                () -> assertNull(manager.hostService.getHost(HostId.hostId(CLIENT_MAC, CLIENT_VLAN))));
    }

    /**
     * Relay a DHCP6 packet with Relay Message opion (Indirectly connected host).
     */
    @Test
    public void relayDhcp6WithAgentInfo() {
        replay(mockHostProviderService);
        // Assume outer dhcp6 relay agent exists in store already
        // send request
        packetService.processPacket(new TestDhcp6RequestPacketContext(DHCP6.MsgType.REQUEST.value(),
                CLIENT2_MAC,
                CLIENT2_VLAN,
                CLIENT2_CP,
                OUTER_RELAY_IP_V6,
                1));

        assertEquals(0, mockRouteStore.routes.size());

        // Make sure the REQUEST packet has been processed before start sending ACK
        assertAfter(PKT_PROCESSING_MS, () -> assertNotNull(packetService.emittedPacket));

        // send reply
        packetService.processPacket(new TestDhcp6ReplyPacketContext(DHCP6.MsgType.REPLY.value(), CLIENT2_CP,
                CLIENT2_MAC,
                CLIENT2_VLAN,
                OUTER_RELAY_IP_V6,
                1, false, CLIENT2_VLAN));

        // won't trigger the host provider service
        verify(mockHostProviderService);
        reset(mockHostProviderService);
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(2, mockRouteStore.routes.size())); // ipAddress and prefix

        assertAfter(PKT_PROCESSING_MS, () ->
                assertTrue(mockRouteStore.routes.stream().anyMatch(rt -> rt.prefix().contains(IP_FOR_CLIENT_V6))));

        assertAfter(PKT_PROCESSING_MS, () ->
                assertTrue(mockRouteStore.routes.stream().anyMatch(rt -> rt.prefix().contains(PREFIX_FOR_CLIENT_V6))));

        // send release msg
        packetService.processPacket(new TestDhcp6RequestPacketContext(DHCP6.MsgType.RELEASE.value(),
                CLIENT2_MAC,
                CLIENT2_VLAN,
                CLIENT2_CP,
                OUTER_RELAY_IP_V6,
                1));
        assertAfter(PKT_PROCESSING_MS, () ->
                assertFalse(mockRouteStore.routes.stream().anyMatch(rt -> rt.prefix().contains(IP_FOR_CLIENT_V6))));

        assertAfter(PKT_PROCESSING_MS, () ->
                assertFalse(mockRouteStore.routes.stream().anyMatch(rt -> rt.prefix().contains(PREFIX_FOR_CLIENT_V6))));

        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

    }

    /**
     * Relay a DHCP6 packet with Relay Message opion (Indirectly connected host) and server responded
     * with vlan differnt from client interface vlan.
     */
    @Test
    public void relayDhcp6WithAgentInfoWrongVlan() {
        replay(mockHostProviderService);
        // Assume outer dhcp6 relay agent exists in store already
        // send request
        packetService.processPacket(new TestDhcp6RequestPacketContext(DHCP6.MsgType.REQUEST.value(),
                CLIENT2_MAC,
                CLIENT2_VLAN,
                CLIENT2_CP,
                INTERFACE_IP_V6.ipAddress().getIp6Address(),
                1));

        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size()));

        // send reply
        packetService.processPacket(new TestDhcp6ReplyPacketContext(DHCP6.MsgType.REPLY.value(),
                CLIENT2_CP,
                CLIENT2_MAC,
                CLIENT2_VLAN,
                INTERFACE_IP_V6.ipAddress().getIp6Address(),
                1, true,
                CLIENT_BOGUS_VLAN // mismatch
        ));

        // won't trigger the host provider service
        verify(mockHostProviderService);
        reset(mockHostProviderService);
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(0, mockRouteStore.routes.size())); // ipAddress and prefix

    }


    @Test
    public void testDhcp4DualHome() {
        PacketContext packetContext =
                new TestDhcpAckPacketContext(CLIENT_DH_CP, CLIENT_MAC, CLIENT_VLAN,
                                             INTERFACE_IP.ipAddress().getIp4Address(),
                                             false);
        reset(manager.hostService);
        expect(manager.hostService.getHost(CLIENT_HOST_ID)).andReturn(EXISTS_HOST).anyTimes();
        Capture<HostDescription> capturedHostDesc = newCapture();
        mockHostProviderService.hostDetected(eq(CLIENT_HOST_ID), capture(capturedHostDesc), eq(false));
        replay(mockHostProviderService, manager.hostService);
        packetService.processPacket(packetContext);
        assertAfter(PKT_PROCESSING_MS, () -> verify(mockHostProviderService));

        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(capturedHostDesc.hasCaptured()));
        HostDescription hostDesc = capturedHostDesc.getValue();
        Set<HostLocation> hostLocations = hostDesc.locations();

        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(2, hostLocations.size()));
        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(hostLocations.contains(CLIENT_LOCATION)));
        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(hostLocations.contains(CLIENT_DH_LOCATION)));
    }

    @Test
    public void testDhcp6DualHome() {
        PacketContext packetContext =
                new TestDhcp6ReplyPacketContext(DHCP6.MsgType.REPLY.value(),
                                                CLIENT_DH_CP, CLIENT_MAC, CLIENT_VLAN,
                                                INTERFACE_IP_V6.ipAddress().getIp6Address(),
                                                0, false, CLIENT_VLAN);
        reset(manager.hostService);
        expect(manager.hostService.getHostsByIp(CLIENT_LL_IP_V6)).andReturn(ImmutableSet.of(EXISTS_HOST)).anyTimes();

        // FIXME: currently DHCPv6 has a bug, we can't get correct vlan of client......
        // XXX: The vlan relied from DHCP6 handler might be wrong, do hack here
        HostId hostId = HostId.hostId(CLIENT_MAC, VlanId.NONE);
        expect(manager.hostService.getHost(hostId)).andReturn(EXISTS_HOST).anyTimes();

        // XXX: sometimes this will work, sometimes not
         expect(manager.hostService.getHost(CLIENT_HOST_ID)).andReturn(EXISTS_HOST).anyTimes();

        Capture<HostDescription> capturedHostDesc = newCapture();

        // XXX: also a hack here
        mockHostProviderService.hostDetected(eq(hostId), capture(capturedHostDesc), eq(false));
        expectLastCall().anyTimes();

        mockHostProviderService.hostDetected(eq(CLIENT_HOST_ID), capture(capturedHostDesc), eq(false));
        expectLastCall().anyTimes();
        replay(mockHostProviderService, manager.hostService);
        packetService.processPacket(packetContext);
        assertAfter(PKT_PROCESSING_MS, () -> verify(mockHostProviderService));

        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(capturedHostDesc.hasCaptured()));
        HostDescription hostDesc = capturedHostDesc.getValue();
        Set<HostLocation> hostLocations = hostDesc.locations();
        assertAfter(PKT_PROCESSING_MS, () -> assertEquals(2, hostLocations.size()));
        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(hostLocations.contains(CLIENT_LOCATION)));
        assertAfter(PKT_PROCESSING_MS, () -> assertTrue(hostLocations.contains(CLIENT_DH_LOCATION)));
    }

    private static class MockDefaultDhcpRelayConfig extends DefaultDhcpRelayConfig {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public List<DhcpServerConfig> dhcpServerConfigs() {
            return ImmutableList.of(new MockDhcpServerConfig(null));
        }
    }

    private static class MockIndirectDhcpRelayConfig extends IndirectDhcpRelayConfig {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public List<DhcpServerConfig> dhcpServerConfigs() {
            return ImmutableList.of(new MockDhcpServerConfig(null));
        }
    }

    private static class MockDhcpServerConfig extends DhcpServerConfig {
        Ip4Address relayAgentIp;

        /**
         * Create mocked version DHCP server config.
         *
         * @param relayAgentIp the relay agent Ip config; null if we don't need it
         */
        public MockDhcpServerConfig(Ip4Address relayAgentIp) {
            this.relayAgentIp = relayAgentIp;
            this.relayAgentIps.put(DEV_1_ID, Pair.of(relayAgentIp, null));
            this.relayAgentIps.put(DEV_2_ID, Pair.of(relayAgentIp, null));
        }

        @Override
        public Optional<Ip4Address> getRelayAgentIp4(DeviceId deviceId) {
            return Optional.ofNullable(this.relayAgentIps.get(deviceId).getLeft());
        }

        @Override
        public Optional<ConnectPoint> getDhcpServerConnectPoint() {
            return Optional.of(SERVER_CONNECT_POINT);
        }

        @Override
        public Optional<Ip4Address> getDhcpServerIp4() {
            return Optional.of(SERVER_IP);
        }

        @Override
        public Optional<Ip4Address> getDhcpGatewayIp4() {
            return Optional.of(GATEWAY_IP);
        }

        @Override
        public Optional<Ip6Address> getDhcpServerIp6() {
            return Optional.of(SERVER_IP_V6);
        }

        @Override
        public Optional<Ip6Address> getDhcpGatewayIp6() {
            return Optional.of(GATEWAY_IP_V6);
        }
    }

    private class MockRouteStore extends RouteStoreAdapter {
        private List<Route> routes = Lists.newArrayList();

        @Override
        public void updateRoute(Route route) {
            routes.add(route);
        }

        @Override
        public void removeRoute(Route route) {
            routes.remove(route);
        }

        @Override
        public void replaceRoute(Route route) {
            routes.remove(route);
            routes.add(route);
        }
    }

    private class MockInterfaceService extends InterfaceServiceAdapter {

        @Override
        public Set<Interface> getInterfaces() {
            return INTERFACES;
        }

        @Override
        public Set<Interface> getInterfacesByIp(IpAddress ip) {
            return INTERFACES.stream()
                    .filter(iface -> {
                        return iface.ipAddressesList().stream()
                                .anyMatch(ifaceIp -> ifaceIp.ipAddress().equals(ip));
                    })
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint port) {
            return INTERFACES.stream()
                    .filter(iface -> iface.connectPoint().equals(port))
                    .collect(Collectors.toSet());
        }
    }

    private class MockDhcpRelayStore implements DhcpRelayStore {
        StoreDelegate<DhcpRelayStoreEvent> delegate;
        private Map<HostId, DhcpRecord> records = Maps.newHashMap();

        @Override
        public void updateDhcpRecord(HostId hostId, DhcpRecord dhcpRecord) {
            records.put(hostId, dhcpRecord);
            DhcpRelayStoreEvent event = new DhcpRelayStoreEvent(DhcpRelayStoreEvent.Type.UPDATED,
                                                                dhcpRecord);
            if (delegate != null) {
                delegate.notify(event);
            }
        }

        @Override
        public Optional<DhcpRecord> getDhcpRecord(HostId hostId) {
            return Optional.ofNullable(records.get(hostId));
        }

        @Override
        public Collection<DhcpRecord> getDhcpRecords() {
            return records.values();
        }

        @Override
        public Optional<DhcpRecord> removeDhcpRecord(HostId hostId) {
            DhcpRecord dhcpRecord = records.remove(hostId);
            if (dhcpRecord != null) {
                DhcpRelayStoreEvent event = new DhcpRelayStoreEvent(DhcpRelayStoreEvent.Type.REMOVED,
                                                                    dhcpRecord);
                if (delegate != null) {
                    delegate.notify(event);
                }
            }
            return Optional.ofNullable(dhcpRecord);
        }

        @Override
        public void setDelegate(StoreDelegate<DhcpRelayStoreEvent> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void unsetDelegate(StoreDelegate<DhcpRelayStoreEvent> delegate) {
            this.delegate = null;
        }

        @Override
        public boolean hasDelegate() {
            return this.delegate != null;
        }
    }

    private class MockDhcpRelayCountersStore implements DhcpRelayCountersStore {
        private Map<String, DhcpRelayCounters> counters = Maps.newHashMap();

        public void incrementCounter(String coutnerClass, String counterName) {
            DhcpRelayCounters countersRecord;

            DhcpRelayCounters classCounters = counters.get(coutnerClass);
            if (classCounters == null) {
                classCounters = new DhcpRelayCounters();
            }
            classCounters.incrementCounter(counterName);
            counters.put(coutnerClass, classCounters);
        }

        @Override
        public Set<Map.Entry<String, DhcpRelayCounters>> getAllCounters() {
            return counters.entrySet();
        }

        @Override
        public Optional<DhcpRelayCounters> getCounters(String counterClass) {
            DhcpRelayCounters classCounters = counters.get(counterClass);
            if (classCounters == null) {
                return Optional.empty();
            }
            return Optional.of(classCounters);
        }

        @Override
        public void resetAllCounters() {
            counters.clear();
        }

        @Override
        public void resetCounters(String counterClass) {
            DhcpRelayCounters classCounters = counters.get(counterClass);
            classCounters.resetCounters();
            counters.put(counterClass, classCounters);
        }
    }


    private class MockPacketService extends PacketServiceAdapter {
        Set<PacketProcessor> packetProcessors = Sets.newHashSet();
        OutboundPacket emittedPacket;

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            packetProcessors.add(processor);
        }

        public void processPacket(PacketContext packetContext) {
            packetProcessors.forEach(p -> p.process(packetContext));
        }

        @Override
        public void emit(OutboundPacket packet) {
            this.emittedPacket = packet;
        }
    }



    /**
     * Generates DHCP REQUEST packet.
     */
    private class TestDhcpRequestPacketContext extends PacketContextAdapter {


        private InboundPacket inPacket;

        public TestDhcpRequestPacketContext(MacAddress clientMac, VlanId vlanId,
                                            ConnectPoint clientCp,
                                            Ip4Address clientGwAddr,
                                            boolean withNonOnosRelayInfo) {
            super(0, null, null, false);
            byte[] dhcpMsgType = new byte[1];
            dhcpMsgType[0] = (byte) DHCP.MsgType.DHCPREQUEST.getValue();

            DhcpOption dhcpOption = new DhcpOption();
            dhcpOption.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            dhcpOption.setData(dhcpMsgType);
            dhcpOption.setLength((byte) 1);
            DhcpOption endOption = new DhcpOption();
            endOption.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());

            DHCP dhcp = new DHCP();
            dhcp.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcp.setHardwareAddressLength((byte) 6);
            dhcp.setClientHardwareAddress(clientMac.toBytes());
            if (withNonOnosRelayInfo) {
                DhcpRelayAgentOption relayOption = new DhcpRelayAgentOption();
                DhcpOption circuitIdOption = new DhcpOption();
                CircuitId circuitId = new CircuitId("Custom option", VlanId.NONE);
                byte[] cid = circuitId.serialize();
                circuitIdOption.setCode(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
                circuitIdOption.setLength((byte) cid.length);
                circuitIdOption.setData(cid);
                relayOption.setCode(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue());
                relayOption.addSubOption(circuitIdOption);
                dhcp.setOptions(ImmutableList.of(dhcpOption, relayOption, endOption));
                dhcp.setGatewayIPAddress(OUTER_RELAY_IP.getIp4Address().toInt());
            } else {
                dhcp.setOptions(ImmutableList.of(dhcpOption, endOption));
            }


            UDP udp = new UDP();
            udp.setPayload(dhcp);
            udp.setSourcePort(UDP.DHCP_CLIENT_PORT);
            udp.setDestinationPort(UDP.DHCP_SERVER_PORT);

            IPv4 ipv4 = new IPv4();
            ipv4.setPayload(udp);
            ipv4.setDestinationAddress(SERVER_IP.toInt());
            ipv4.setSourceAddress(clientGwAddr.toInt());

            Ethernet eth = new Ethernet();
            if (withNonOnosRelayInfo) {
                eth.setEtherType(Ethernet.TYPE_IPV4)
                        .setVlanID(vlanId.toShort())
                        .setSourceMACAddress(OUTER_RELAY_MAC)
                        .setDestinationMACAddress(MacAddress.BROADCAST)
                        .setPayload(ipv4);
            } else {
                eth.setEtherType(Ethernet.TYPE_IPV4)
                        .setVlanID(vlanId.toShort())
                        .setSourceMACAddress(clientMac)
                        .setDestinationMACAddress(MacAddress.BROADCAST)
                        .setPayload(ipv4);
            }

            this.inPacket = new DefaultInboundPacket(clientCp, eth,
                                                     ByteBuffer.wrap(eth.serialize()));
        }

        @Override
        public InboundPacket inPacket() {
            return this.inPacket;
        }
    }

    /**
     * Generates DHCP ACK packet.
     */
    private class TestDhcpAckPacketContext extends PacketContextAdapter {
        private InboundPacket inPacket;

        public TestDhcpAckPacketContext(ConnectPoint clientCp, MacAddress clientMac,
                                        VlanId clientVlan, Ip4Address clientGwAddr,
                                        boolean withNonOnosRelayInfo) {
            super(0, null, null, false);

            byte[] dhcpMsgType = new byte[1];
            dhcpMsgType[0] = (byte) DHCP.MsgType.DHCPACK.getValue();

            DhcpOption dhcpOption = new DhcpOption();
            dhcpOption.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            dhcpOption.setData(dhcpMsgType);
            dhcpOption.setLength((byte) 1);

            DhcpOption endOption = new DhcpOption();
            endOption.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());

            DHCP dhcp = new DHCP();
            if (withNonOnosRelayInfo) {
                DhcpRelayAgentOption relayOption = new DhcpRelayAgentOption();
                DhcpOption circuitIdOption = new DhcpOption();
                String circuitId = NON_ONOS_CID;
                byte[] cid = circuitId.getBytes(Charsets.US_ASCII);
                circuitIdOption.setCode(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
                circuitIdOption.setLength((byte) cid.length);
                circuitIdOption.setData(cid);
                relayOption.setCode(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue());
                relayOption.addSubOption(circuitIdOption);
                dhcp.setOptions(ImmutableList.of(dhcpOption, relayOption, endOption));
                dhcp.setGatewayIPAddress(OUTER_RELAY_IP.getIp4Address().toInt());
            } else {
                CircuitId cid = new CircuitId(clientCp.toString(), clientVlan);
                byte[] circuitId = cid.serialize();
                DhcpOption circuitIdSubOption = new DhcpOption();
                circuitIdSubOption.setCode(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
                circuitIdSubOption.setData(circuitId);
                circuitIdSubOption.setLength((byte) circuitId.length);

                DhcpRelayAgentOption relayInfoOption = new DhcpRelayAgentOption();
                relayInfoOption.setCode(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue());
                relayInfoOption.addSubOption(circuitIdSubOption);
                dhcp.setOptions(ImmutableList.of(dhcpOption, relayInfoOption, endOption));
                dhcp.setGatewayIPAddress(clientGwAddr.toInt());
            }
            dhcp.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcp.setHardwareAddressLength((byte) 6);
            dhcp.setClientHardwareAddress(clientMac.toBytes());
            dhcp.setYourIPAddress(IP_FOR_CLIENT.toInt());

            UDP udp = new UDP();
            udp.setPayload(dhcp);
            udp.setSourcePort(UDP.DHCP_SERVER_PORT);
            udp.setDestinationPort(UDP.DHCP_CLIENT_PORT);
            IPv4 ipv4 = new IPv4();
            ipv4.setPayload(udp);
            ipv4.setDestinationAddress(IP_FOR_CLIENT.toString());
            ipv4.setSourceAddress(SERVER_IP.toString());
            Ethernet eth = new Ethernet();
            if (withNonOnosRelayInfo) {
                eth.setEtherType(Ethernet.TYPE_IPV4)
                        .setVlanID(SERVER_VLAN.toShort())
                        .setSourceMACAddress(SERVER_MAC)
                        .setDestinationMACAddress(OUTER_RELAY_MAC)
                        .setPayload(ipv4);
            } else {
                eth.setEtherType(Ethernet.TYPE_IPV4)
                        .setVlanID(SERVER_VLAN.toShort())
                        .setSourceMACAddress(SERVER_MAC)
                        .setDestinationMACAddress(CLIENT_MAC)
                        .setPayload(ipv4);
            }

            this.inPacket = new DefaultInboundPacket(SERVER_CONNECT_POINT, eth,
                                                     ByteBuffer.wrap(eth.serialize()));

        }

        @Override
        public InboundPacket inPacket() {
            return this.inPacket;
        }
    }

    private class TestArpRequestPacketContext extends PacketContextAdapter {
        private InboundPacket inPacket;

        public TestArpRequestPacketContext(Interface fromInterface) {
            super(0, null, null, false);
            ARP arp = new ARP();
            arp.setOpCode(ARP.OP_REQUEST);

            IpAddress targetIp = fromInterface.ipAddressesList().get(0).ipAddress();
            arp.setTargetProtocolAddress(targetIp.toOctets());
            arp.setTargetHardwareAddress(MacAddress.BROADCAST.toBytes());
            arp.setSenderHardwareAddress(MacAddress.NONE.toBytes());
            arp.setSenderProtocolAddress(Ip4Address.valueOf(0).toOctets());
            arp.setHardwareAddressLength((byte) MacAddress.MAC_ADDRESS_LENGTH);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_ARP);
            eth.setSourceMACAddress(MacAddress.NONE);
            eth.setDestinationMACAddress(MacAddress.BROADCAST);
            eth.setVlanID(fromInterface.vlan().toShort());
            eth.setPayload(arp);

            this.inPacket = new DefaultInboundPacket(fromInterface.connectPoint(), eth,
                                                     ByteBuffer.wrap(eth.serialize()));
        }

        @Override
        public InboundPacket inPacket() {
            return this.inPacket;
        }
    }

    /**
     * Generates DHCP6 REQUEST packet.
     */
    private void buildDhcp6Packet(DHCP6 dhcp6, byte msgType, Ip6Address ip6Addr, IpPrefix prefix) {

        // build address option
        Dhcp6IaAddressOption iaAddressOption = new Dhcp6IaAddressOption();
        iaAddressOption.setCode(DHCP6.OptionCode.IAADDR.value());
        iaAddressOption.setIp6Address(ip6Addr);
        iaAddressOption.setPreferredLifetime(3600);
        iaAddressOption.setValidLifetime(1200);
        iaAddressOption.setLength((short) Dhcp6IaAddressOption.DEFAULT_LEN);

        Dhcp6ClientIdOption clientIdOption = new Dhcp6ClientIdOption();
        Dhcp6Duid dhcp6Duip = new Dhcp6Duid();
        dhcp6Duip.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        dhcp6Duip.setHardwareType((short) 0x01);   // Ethernet
        dhcp6Duip.setDuidTime(1234);
        dhcp6Duip.setLinkLayerAddress(CLIENT_MAC.toBytes());
        clientIdOption.setDuid(dhcp6Duip);

        Dhcp6IaNaOption iaNaOption = new Dhcp6IaNaOption();
        iaNaOption.setCode(DHCP6.OptionCode.IA_NA.value());
        iaNaOption.setIaId(0);
        iaNaOption.setT1(302400);
        iaNaOption.setT2(483840);
        List<Dhcp6Option> iaNaSubOptions = new ArrayList<Dhcp6Option>();
        iaNaSubOptions.add(iaAddressOption);
        iaNaOption.setOptions(iaNaSubOptions);
        iaNaOption.setLength((short) (Dhcp6IaNaOption.DEFAULT_LEN + iaAddressOption.getLength()));

        // build prefix option
        Dhcp6IaPrefixOption iaPrefixOption = new Dhcp6IaPrefixOption();
        iaPrefixOption.setCode(DHCP6.OptionCode.IAPREFIX.value());
        iaPrefixOption.setIp6Prefix(prefix.address().getIp6Address());
        iaPrefixOption.setPrefixLength((byte) prefix.prefixLength());
        iaPrefixOption.setPreferredLifetime(3601);
        iaPrefixOption.setValidLifetime(1201);
        iaPrefixOption.setLength((short) Dhcp6IaPrefixOption.DEFAULT_LEN);

        Dhcp6IaPdOption iaPdOption = new Dhcp6IaPdOption();
        iaPdOption.setCode(DHCP6.OptionCode.IA_PD.value());
        iaPdOption.setIaId(0);
        iaPdOption.setT1(302401);
        iaPdOption.setT2(483841);
        List<Dhcp6Option> iaPdSubOptions = new ArrayList<Dhcp6Option>();
        iaPdSubOptions.add(iaPrefixOption);
        iaPdOption.setOptions(iaPdSubOptions);
        iaPdOption.setLength((short) (Dhcp6IaPdOption.DEFAULT_LEN + iaPrefixOption.getLength()));

        dhcp6.setMsgType(msgType);
        List<Dhcp6Option> dhcp6Options = new ArrayList<Dhcp6Option>();
        dhcp6Options.add(iaNaOption);
        dhcp6Options.add(clientIdOption);
        dhcp6Options.add(iaPdOption);
        dhcp6.setOptions(dhcp6Options);

    }

    private void buildRelayMsg(DHCP6 dhcp6Relay, byte msgType, Ip6Address linkAddr,
                               Ip6Address peerAddr, byte hop, byte[] interfaceIdBytes,
                               DHCP6 dhcp6Payload) {

        dhcp6Relay.setMsgType(msgType);

        dhcp6Relay.setLinkAddress(linkAddr.toOctets());
        dhcp6Relay.setPeerAddress(peerAddr.toOctets());
        dhcp6Relay.setHopCount(hop);
        List<Dhcp6Option> options = new ArrayList<Dhcp6Option>();

        // interfaceId  option
        Dhcp6Option interfaceId = new Dhcp6Option();
        interfaceId.setCode(DHCP6.OptionCode.INTERFACE_ID.value());


        interfaceId.setData(interfaceIdBytes);
        interfaceId.setLength((short) interfaceIdBytes.length);
        Dhcp6InterfaceIdOption interfaceIdOption = new Dhcp6InterfaceIdOption(interfaceId);
        byte[] optionData = interfaceIdOption.getData();
        ByteBuffer bb = ByteBuffer.wrap(interfaceIdBytes);

        byte[] macAddr = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        byte[] port =  new byte[optionData.length - MacAddress.MAC_ADDRESS_LENGTH -
                                VLAN_LEN - SEPARATOR_LEN * 2];
        short vlan;
        bb.get(macAddr);
        bb.get();  // separator
        bb.get(port);
        bb.get();  // separator
        vlan = bb.getShort();
        interfaceIdOption.setMacAddress(MacAddress.valueOf(macAddr));
        interfaceIdOption.setInPort(port);
        interfaceIdOption.setVlanId(vlan);

        options.add(interfaceIdOption);

        // relay message option
        Dhcp6Option relayMsgOption = new Dhcp6Option();
        relayMsgOption.setCode(DHCP6.OptionCode.RELAY_MSG.value());
        byte[] dhcp6PayloadByte = dhcp6Payload.serialize();
        relayMsgOption.setLength((short) dhcp6PayloadByte.length);
        relayMsgOption.setPayload(dhcp6Payload);
        Dhcp6RelayOption relayOpt = new Dhcp6RelayOption(relayMsgOption);

        options.add(relayOpt);

        dhcp6Relay.setOptions(options);
    }
    private byte[] buildInterfaceId(MacAddress clientMac, short vlanId, ConnectPoint clientCp) {
        String inPortString = "-" + clientCp.toString() + ":";
        byte[] clientSoureMacBytes = clientMac.toBytes();
        byte[] inPortStringBytes = inPortString.getBytes();
        byte[] vlanIdBytes = new byte[2];
        vlanIdBytes[0] = (byte) ((vlanId >> 8) & 0xff);  // high-order byte first
        vlanIdBytes[1] = (byte) (vlanId & 0xff);
        byte[] interfaceIdBytes = new byte[clientSoureMacBytes.length +  inPortStringBytes.length + vlanIdBytes.length];

        System.arraycopy(clientSoureMacBytes, 0, interfaceIdBytes, 0, clientSoureMacBytes.length);
        System.arraycopy(inPortStringBytes, 0, interfaceIdBytes, clientSoureMacBytes.length, inPortStringBytes.length);
        System.arraycopy(vlanIdBytes, 0, interfaceIdBytes, clientSoureMacBytes.length + inPortStringBytes.length,
                vlanIdBytes.length);

        return interfaceIdBytes;
    }

    private static List<TrafficSelector> buildClientDhcpSelectors() {
        return Streams.concat(Dhcp4HandlerImpl.DHCP_SELECTORS.stream(),
                              Dhcp6HandlerImpl.DHCP_SELECTORS.stream())
                .collect(Collectors.toList());
    }

    private class TestDhcp6RequestPacketContext extends PacketContextAdapter {


        private InboundPacket inPacket;


        public TestDhcp6RequestPacketContext(byte msgType, MacAddress clientMac, VlanId vlanId,
                                            ConnectPoint clientCp,
                                            Ip6Address clientGwAddr,
                                            int relayLevel) {
            super(0, null, null, false);

            DHCP6 dhcp6 = new DHCP6();
            if (relayLevel > 0) {
                DHCP6 dhcp6Payload = new DHCP6();
                buildDhcp6Packet(dhcp6Payload, msgType,
                                 IP_FOR_CLIENT_V6,
                        msgType == DHCP6.MsgType.REQUEST.value() ? PREFIX_FOR_ZERO : PREFIX_FOR_CLIENT_V6);
                DHCP6 dhcp6Parent = null;
                DHCP6 dhcp6Child = dhcp6Payload;
                for (int i = 0; i < relayLevel; i++) {
                    dhcp6Parent = new DHCP6();
                    byte[] interfaceId = buildInterfaceId(clientMac, vlanId.toShort(), clientCp);
                    buildRelayMsg(dhcp6Parent, DHCP6.MsgType.RELAY_FORW.value(),
                            INTERFACE_IP_V6.ipAddress().getIp6Address(),
                            OUTER_RELAY_IP_V6,
                            (byte) (relayLevel - 1), interfaceId,
                            dhcp6Child);
                    dhcp6Child = dhcp6Parent;
                }
                if (dhcp6Parent != null) {
                    dhcp6 = dhcp6Parent;
                }
            } else {
                buildDhcp6Packet(dhcp6, msgType,
                                        IP_FOR_CLIENT_V6,
                        msgType == DHCP6.MsgType.REQUEST.value() ? PREFIX_FOR_ZERO : PREFIX_FOR_CLIENT_V6);
            }

            UDP udp = new UDP();
            udp.setPayload(dhcp6);
            if (relayLevel > 0) {
                udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
            } else {
                udp.setSourcePort(UDP.DHCP_V6_CLIENT_PORT);
            }
            udp.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);

            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(udp);
            ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
            ipv6.setDestinationAddress(SERVER_IP_V6_MCAST.toOctets());
            ipv6.setSourceAddress(clientGwAddr.toOctets());

            Ethernet eth = new Ethernet();
            if (relayLevel > 0) {
                eth.setEtherType(Ethernet.TYPE_IPV6)
                        .setVlanID(OUTER_RELAY_VLAN.toShort())
                        .setSourceMACAddress(OUTER_RELAY_MAC)
                        .setDestinationMACAddress(MacAddress.valueOf("33:33:00:01:00:02"))
                        .setPayload(ipv6);
            } else {
                eth.setEtherType(Ethernet.TYPE_IPV6)
                        .setVlanID(vlanId.toShort())
                        .setSourceMACAddress(clientMac)
                        .setDestinationMACAddress(MacAddress.valueOf("33:33:00:01:00:02"))
                        .setPayload(ipv6);
            }
            this.inPacket = new DefaultInboundPacket(clientCp, eth,
                                                     ByteBuffer.wrap(eth.serialize()));
        }

        @Override
        public InboundPacket inPacket() {
            return this.inPacket;
        }
    }

    /**
     * Generates DHCP6 REPLY  packet.
     */

    private class TestDhcp6ReplyPacketContext extends PacketContextAdapter {
        private InboundPacket inPacket;

        public TestDhcp6ReplyPacketContext(byte msgType, ConnectPoint clientCp, MacAddress clientMac,
                                        VlanId clientVlan, Ip6Address clientGwAddr,
                                        int relayLevel, boolean overWriteFlag, VlanId overWriteVlan) {
            super(0, null, null, false);


            DHCP6 dhcp6Payload = new DHCP6();
            buildDhcp6Packet(dhcp6Payload, msgType,
                    IP_FOR_CLIENT_V6,
                    PREFIX_FOR_CLIENT_V6);
            byte[] interfaceId = null;
            if (relayLevel > 0) {
                interfaceId = buildInterfaceId(OUTER_RELAY_MAC,
                                               overWriteFlag ? overWriteVlan.toShort() : OUTER_RELAY_VLAN.toShort(),
                                                OUTER_RELAY_CP);
            } else {
                interfaceId = buildInterfaceId(clientMac, clientVlan.toShort(), clientCp);
            }
            DHCP6 dhcp6 = new DHCP6();
            buildRelayMsg(dhcp6, DHCP6.MsgType.RELAY_REPL.value(),
                          INTERFACE_IP_V6.ipAddress().getIp6Address(),
                          CLIENT_LL_IP_V6,
                          (byte) 0, interfaceId,
                          dhcp6Payload);

            DHCP6 dhcp6Parent = null;
            DHCP6 dhcp6Child = dhcp6;
            for (int i = 0; i < relayLevel; i++) {   // relayLevel 0 : no relay
                dhcp6Parent = new DHCP6();

                buildRelayMsg(dhcp6Parent, DHCP6.MsgType.RELAY_REPL.value(),
                        INTERFACE_IP_V6.ipAddress().getIp6Address(),
                        OUTER_RELAY_IP_V6,
                        (byte) relayLevel, interfaceId,
                        dhcp6Child);

                dhcp6Child = dhcp6Parent;
            }
            if (dhcp6Parent != null) {
                dhcp6 = dhcp6Parent;
            }


            UDP udp = new UDP();
            udp.setPayload(dhcp6);
            udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
            udp.setDestinationPort(UDP.DHCP_V6_CLIENT_PORT);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(udp);
            ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
            ipv6.setDestinationAddress(IP_FOR_CLIENT_V6.toOctets());
            ipv6.setSourceAddress(SERVER_IP_V6.toOctets());
            Ethernet eth = new Ethernet();
            if (relayLevel > 0) {
                eth.setEtherType(Ethernet.TYPE_IPV6)
                        .setVlanID(SERVER_VLAN.toShort())
                        .setSourceMACAddress(SERVER_MAC)
                        .setDestinationMACAddress(OUTER_RELAY_MAC)
                        .setPayload(ipv6);
            } else {
                eth.setEtherType(Ethernet.TYPE_IPV6)
                        .setVlanID(SERVER_VLAN.toShort())
                        .setSourceMACAddress(SERVER_MAC)
                        .setDestinationMACAddress(CLIENT_MAC)
                        .setPayload(ipv6);
            }

            this.inPacket = new DefaultInboundPacket(SERVER_CONNECT_POINT, eth,
                                                     ByteBuffer.wrap(eth.serialize()));

        }

        @Override
        public InboundPacket inPacket() {
            return this.inPacket;
        }
    }

}
