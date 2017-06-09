/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.CircuitId;
import org.onlab.packet.dhcp.DhcpOption;
import org.onlab.packet.dhcp.DhcpRelayAgentOption;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.dhcprelay.store.DhcpRelayStoreEvent;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceServiceAdapter;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteStoreAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostStoreAdapter;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketContextAdapter;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.StoreDelegate;
import org.osgi.service.component.ComponentContext;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DhcpRelayManagerTest {
    // Ip address for interfaces
    private static final InterfaceIpAddress INTERFACE_IP = InterfaceIpAddress.valueOf("10.0.3.254/32");
    private static final List<InterfaceIpAddress> INTERFACE_IPS = ImmutableList.of(INTERFACE_IP);

    // DHCP client (will send without option 82)
    private static final Ip4Address IP_FOR_CLIENT = Ip4Address.valueOf("10.0.0.1");
    private static final MacAddress CLIENT_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId CLIENT_VLAN = VlanId.vlanId("100");
    private static final ConnectPoint CLIENT_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final MacAddress CLIENT_IFACE_MAC = MacAddress.valueOf("00:00:00:00:11:01");
    private static final Interface CLIENT_INTERFACE = new Interface("C1",
                                                                    CLIENT_CP,
                                                                    INTERFACE_IPS,
                                                                    CLIENT_IFACE_MAC,
                                                                    CLIENT_VLAN);

    // DHCP client 2 (will send with option 82, so the vlan should equals to vlan from server)
    private static final MacAddress CLIENT2_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId CLIENT2_VLAN = VlanId.NONE;
    private static final ConnectPoint CLIENT2_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final MacAddress CLIENT2_IFACE_MAC = MacAddress.valueOf("00:00:00:00:11:01");
    private static final Interface CLIENT2_INTERFACE = new Interface("C2",
                                                                     CLIENT2_CP,
                                                                     INTERFACE_IPS,
                                                                     CLIENT2_IFACE_MAC,
                                                                     CLIENT2_VLAN);

    // Outer relay information
    private static final Ip4Address OUTER_RELAY_IP = Ip4Address.valueOf("10.0.5.253");
    private static final Set<IpAddress> OUTER_RELAY_IPS = ImmutableSet.of(OUTER_RELAY_IP);
    private static final MacAddress OUTER_RELAY_MAC = MacAddress.valueOf("00:01:02:03:04:05");
    private static final VlanId OUTER_RELAY_VLAN = VlanId.NONE;
    private static final ConnectPoint OUTER_RELAY_CP = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final HostLocation OUTER_REPLAY_HL = new HostLocation(OUTER_RELAY_CP, 0);
    private static final HostId OUTER_RELAY_HOST_ID = HostId.hostId(OUTER_RELAY_MAC, OUTER_RELAY_VLAN);
    private static final Host OUTER_RELAY_HOST = new DefaultHost(DhcpRelayManager.PROVIDER_ID,
                                                                 OUTER_RELAY_HOST_ID,
                                                                 OUTER_RELAY_MAC,
                                                                 OUTER_RELAY_VLAN,
                                                                 OUTER_REPLAY_HL,
                                                                 OUTER_RELAY_IPS);

    // DHCP Server
    private static final MacAddress SERVER_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId SERVER_VLAN = VlanId.NONE;
    private static final ConnectPoint SERVER_CONNECT_POINT =
            ConnectPoint.deviceConnectPoint("of:0000000000000001/5");
    private static final HostLocation SERVER_LOCATION =
            new HostLocation(SERVER_CONNECT_POINT, 0);
    private static final Ip4Address SERVER_IP = Ip4Address.valueOf("10.0.3.253");
    private static final Set<IpAddress> DHCP_SERVER_IPS = ImmutableSet.of(SERVER_IP);
    private static final HostId SERVER_HOST_ID = HostId.hostId(SERVER_MAC, SERVER_VLAN);
    private static final Host SERVER_HOST = new DefaultHost(DhcpRelayManager.PROVIDER_ID,
                                                            SERVER_HOST_ID,
                                                            SERVER_MAC,
                                                            SERVER_VLAN,
                                                            SERVER_LOCATION,
                                                            DHCP_SERVER_IPS);
    private static final MacAddress SERVER_IFACE_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final Interface SERVER_INTERFACE = new Interface("SERVER",
                                                                    SERVER_CONNECT_POINT,
                                                                    INTERFACE_IPS,
                                                                    SERVER_IFACE_MAC,
                                                                    SERVER_VLAN);

    // Components
    private static final ApplicationId APP_ID = TestApplicationId.create(DhcpRelayManager.DHCP_RELAY_APP);
    private static final DhcpRelayConfig CONFIG = new MockDhcpRelayConfig();
    private static final Set<Interface> INTERFACES = ImmutableSet.of(
            CLIENT_INTERFACE,
            CLIENT2_INTERFACE,
            SERVER_INTERFACE
    );

    private DhcpRelayManager manager;
    private MockPacketService packetService;
    private MockHostStore mockHostStore;
    private MockRouteStore mockRouteStore;
    private MockDhcpRelayStore mockDhcpRelayStore;

    @Before
    public void setup() {
        manager = new DhcpRelayManager();
        manager.cfgService = createNiceMock(NetworkConfigRegistry.class);

        expect(manager.cfgService.getConfig(anyObject(), anyObject()))
                .andReturn(CONFIG)
                .anyTimes();

        manager.coreService = createNiceMock(CoreService.class);
        expect(manager.coreService.registerApplication(anyString()))
                .andReturn(APP_ID).anyTimes();

        manager.hostService = createNiceMock(HostService.class);
        expect(manager.hostService.getHostsByIp(anyObject())).andReturn(ImmutableSet.of(SERVER_HOST));
        expect(manager.hostService.getHost(OUTER_RELAY_HOST_ID)).andReturn(OUTER_RELAY_HOST);

        packetService = new MockPacketService();
        manager.packetService = packetService;
        manager.compCfgService = createNiceMock(ComponentConfigService.class);

        mockHostStore = new MockHostStore();
        mockRouteStore = new MockRouteStore();
        mockDhcpRelayStore = new MockDhcpRelayStore();

        manager.hostStore = mockHostStore;
        manager.routeStore = mockRouteStore;
        manager.dhcpRelayStore = mockDhcpRelayStore;

        manager.interfaceService = new MockInterfaceService();

        // properties
        Dictionary<String, Object> dictionary = createNiceMock(Dictionary.class);
        expect(dictionary.get("arpEnabled")).andReturn(true).anyTimes();
        ComponentContext context = createNiceMock(ComponentContext.class);
        expect(context.getProperties()).andReturn(dictionary).anyTimes();

        EasyMock.replay(manager.cfgService, manager.coreService, manager.hostService,
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
        // send request
        packetService.processPacket(new TestDhcpRequestPacketContext(CLIENT_MAC,
                                                                     CLIENT_VLAN,
                                                                     CLIENT_CP,
                                                                     INTERFACE_IP.ipAddress().getIp4Address(),
                                                                     false));

        Set<Host> hosts = ImmutableSet.copyOf(mockHostStore.getHosts());
        assertEquals(0, hosts.size());
        assertEquals(0, mockRouteStore.routes.size());

        // send ack
        packetService.processPacket(new TestDhcpAckPacketContext(CLIENT_CP, CLIENT_MAC,
                                                                 CLIENT_VLAN, INTERFACE_IP.ipAddress().getIp4Address(),
                                                                 false));
        hosts = ImmutableSet.copyOf(mockHostStore.getHosts());
        assertEquals(1, hosts.size());
        assertEquals(0, mockRouteStore.routes.size());

        Host host = hosts.iterator().next();
        assertEquals(CLIENT_MAC, host.mac());
        assertEquals(CLIENT_VLAN, host.vlan());
        assertEquals(CLIENT_CP.deviceId(), host.location().elementId());
        assertEquals(CLIENT_CP.port(), host.location().port());
        assertEquals(1, host.ipAddresses().size());
        assertEquals(IP_FOR_CLIENT, host.ipAddresses().iterator().next());
        assertEquals(HostId.hostId(CLIENT_MAC, CLIENT_VLAN), host.id());
    }

    /**
     * Relay a DHCP packet with option 82 (Indirectly connected host).
     */
    @Test
    public void relayDhcpWithAgentInfo() {
        // Assume outer dhcp relay agent exists in store already
        // send request
        packetService.processPacket(new TestDhcpRequestPacketContext(CLIENT2_MAC,
                                                                     CLIENT2_VLAN,
                                                                     CLIENT2_CP,
                                                                     INTERFACE_IP.ipAddress().getIp4Address(),
                                                                     true));

        Set<Host> hosts = ImmutableSet.copyOf(mockHostStore.getHosts());
        assertEquals(0, hosts.size());
        assertEquals(0, mockRouteStore.routes.size());

        // send ack
        packetService.processPacket(new TestDhcpAckPacketContext(CLIENT2_CP,
                                                                 CLIENT2_MAC,
                                                                 CLIENT2_VLAN,
                                                                 INTERFACE_IP.ipAddress().getIp4Address(),
                                                                 true));

        hosts = ImmutableSet.copyOf(mockHostStore.getHosts());
        assertEquals(0, hosts.size());
        assertEquals(1, mockRouteStore.routes.size());

        Route route = mockRouteStore.routes.get(0);
        assertEquals(OUTER_RELAY_IP, route.nextHop());
        assertEquals(IP_FOR_CLIENT.toIpPrefix(), route.prefix());
        assertEquals(Route.Source.STATIC, route.source());
    }

    @Test
    public void testArpRequest() throws Exception {
        packetService.processPacket(new TestArpRequestPacketContext(CLIENT_INTERFACE));
        OutboundPacket outboundPacket = packetService.emitedPacket;
        byte[] outPacketData = outboundPacket.data().array();
        Ethernet eth = Ethernet.deserializer().deserialize(outPacketData, 0, outPacketData.length);

        assertEquals(eth.getEtherType(), Ethernet.TYPE_ARP);
        ARP arp = (ARP) eth.getPayload();
        assertArrayEquals(arp.getSenderHardwareAddress(), CLIENT_INTERFACE.mac().toBytes());
    }

    private static class MockDhcpRelayConfig extends DhcpRelayConfig {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public ConnectPoint getDhcpServerConnectPoint() {
            return SERVER_CONNECT_POINT;
        }

        public Ip4Address getDhcpServerIp() {
            return SERVER_IP;
        }

        public Ip4Address getDhcpGatewayIp() {
            return null;
        }
    }

    private class MockHostStore extends HostStoreAdapter {

        private final Map<HostId, HostDescription> hosts = Maps.newHashMap();

        @Override
        public HostEvent createOrUpdateHost(ProviderId providerId, HostId hostId,
                                            HostDescription hostDescription,
                                            boolean replaceIps) {
            hosts.put(hostId, hostDescription);

            // not necessary to return host event in this test.
            return null;
        }

        public HostDescription hostDesc(HostId hostId) {
            return hosts.get(hostId);
        }

        @Override
        public Iterable<Host> getHosts() {
            return hosts.values().stream()
                    .map(hd -> new DefaultHost(DhcpRelayManager.PROVIDER_ID,
                                               HostId.hostId(hd.hwAddress(), hd.vlan()),
                                               hd.hwAddress(),
                                               hd.vlan(), hd.locations(),
                                               hd.ipAddress(), false))
                    .collect(Collectors.toList());
        }
    }

    private class MockRouteStore extends RouteStoreAdapter {
        private List<Route> routes = Lists.newArrayList();

        @Override
        public void updateRoute(Route route) {
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

    private class MockPacketService extends PacketServiceAdapter {
        Set<PacketProcessor> packetProcessors = Sets.newHashSet();
        OutboundPacket emitedPacket;

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            packetProcessors.add(processor);
        }

        public void processPacket(PacketContext packetContext) {
            packetProcessors.forEach(p -> p.process(packetContext));
        }

        @Override
        public void emit(OutboundPacket packet) {
            this.emitedPacket = packet;
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
                CircuitId circuitId = new CircuitId("Custom cid", VlanId.NONE);
                byte[] cid = circuitId.serialize();
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
}
