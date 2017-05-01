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
package org.onosproject.provider.host.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.ARP;
import org.onlab.packet.ChassisId;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContextAdapter;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyServiceAdapter;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.packet.VlanId.vlanId;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_UPDATED;

public class HostLocationProviderTest {
    private static final ProviderId PROVIDER_ID =
            new ProviderId("of", "org.onosproject.provider.host");

    private static final Integer INPORT = 10;
    private static final Integer INPORT2 = 11;
    private static final String DEV1 = "of:1";
    private static final String DEV2 = "of:2";
    private static final String DEV3 = "of:3";
    private static final String DEV4 = "of:4";
    private static final String DEV5 = "of:5";
    private static final String DEV6 = "of:6";

    private static final VlanId VLAN = vlanId();

    // IPv4 Host
    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress BCMAC = MacAddress.valueOf("ff:ff:ff:ff:ff:ff");
    private static final byte[] IP = new byte[]{10, 0, 0, 1};
    private static final IpAddress IP_ADDRESS =
            IpAddress.valueOf(IpAddress.Version.INET, IP);
    private static final HostLocation LOCATION =
            new HostLocation(deviceId(DEV1), portNumber(INPORT), 0L);
    private static final DefaultHost HOST =
            new DefaultHost(PROVIDER_ID, hostId(MAC), MAC,
                    VLAN, LOCATION,
                    ImmutableSet.of(IP_ADDRESS));

    // IPv6 Host
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:22:00:00:02");
    private static final MacAddress BCMAC2 = MacAddress.valueOf("33:33:00:00:00:01");
    private static final byte[] IP2 = Ip6Address.valueOf("1000::1").toOctets();
    private static final IpAddress IP_ADDRESS2 =
            IpAddress.valueOf(IpAddress.Version.INET6, IP2);
    private static final HostLocation LOCATION2 =
            new HostLocation(deviceId(DEV4), portNumber(INPORT), 0L);
    private static final DefaultHost HOST2 =
            new DefaultHost(PROVIDER_ID, hostId(MAC2), MAC2,
                    VLAN, LOCATION2,
                    ImmutableSet.of(IP_ADDRESS2));

    // DHCP Server
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:33:00:00:03");
    private static final byte[] IP3 = new byte[]{10, 0, 0, 2};
    private static final IpAddress IP_ADDRESS3 =
            IpAddress.valueOf(IpAddress.Version.INET, IP3);

    private static final HostLocation LOCATION3 =
            new HostLocation(deviceId(DEV1), portNumber(INPORT2), 0L);
    private static final DefaultHost HOST3 =
            new DefaultHost(PROVIDER_ID, hostId(MAC3), MAC3,
                    VLAN, LOCATION3,
                    ImmutableSet.of(IP_ADDRESS3));

    private static final ComponentContextAdapter CTX_FOR_REMOVE =
            new ComponentContextAdapter() {
                @Override
                public Dictionary getProperties() {
                    Hashtable<String, String> props = new Hashtable<>();
                    props.put("hostRemovalEnabled", "true");
                    return props;
                }
            };

    public static final ComponentContextAdapter CTX_FOR_NO_REMOVE =
            new ComponentContextAdapter() {
                @Override
                public Dictionary getProperties() {
                    return new Hashtable();
                }
            };

    private final HostLocationProvider provider = new HostLocationProvider();
    private final TestHostRegistry hostRegistry = new TestHostRegistry();
    private final TestTopologyService topoService = new TestTopologyService();
    private final TestDeviceService deviceService = new TestDeviceService();
    private final TestHostService hostService = new TestHostService();
    private final TestPacketService packetService = new TestPacketService();

    private PacketProcessor testProcessor;
    private CoreService coreService;
    private TestHostProviderService providerService;

    private ApplicationId appId =
            new DefaultApplicationId(100, "org.onosproject.provider.host");

    @Before
    public void setUp() {

        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(appId.name()))
                .andReturn(appId).anyTimes();
        replay(coreService);

        provider.cfgService = new ComponentConfigAdapter();
        provider.coreService = coreService;

        provider.providerRegistry = hostRegistry;
        provider.topologyService = topoService;
        provider.packetService = packetService;
        provider.deviceService = deviceService;
        provider.hostService = hostService;

        provider.activate(CTX_FOR_NO_REMOVE);

        provider.eventHandler = MoreExecutors.newDirectExecutorService();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void events() {
        // New host. Expect one additional host description.
        testProcessor.process(new TestArpPacketContext(DEV1));
        assertThat("New host expected", providerService.descriptions.size(), is(1));

        // The host moved to new switch. Expect one additional host description.
        // The second host description should have a different location.
        testProcessor.process(new TestArpPacketContext(DEV2));
        assertThat("Host motion expected", providerService.descriptions.size(), is(2));
        HostLocation loc1 = providerService.descriptions.get(0).location();
        HostLocation loc2 = providerService.descriptions.get(1).location();
        assertNotEquals("Host location should be different", loc1, loc2);

        // The host was misheard on a spine. Expect no additional host description.
        testProcessor.process(new TestArpPacketContext(DEV3));
        assertThat("Host misheard on spine switch", providerService.descriptions.size(), is(2));

        providerService.clear();

        // New host. Expect one additional host description.
        testProcessor.process(new TestNaPacketContext(DEV4));
        assertThat("New host expected", providerService.descriptions.size(), is(1));

        // The host moved to new switch. Expect one additional host description.
        // The second host description should have a different location.
        testProcessor.process(new TestNaPacketContext(DEV5));
        assertThat("Host motion expected", providerService.descriptions.size(), is(2));
        loc1 = providerService.descriptions.get(0).location();
        loc2 = providerService.descriptions.get(1).location();
        assertNotEquals("Host location should be different", loc1, loc2);

        // The host was misheard on a spine. Expect no additional host description.
        testProcessor.process(new TestNaPacketContext(DEV6));
        assertThat("Host misheard on spine switch", providerService.descriptions.size(), is(2));
    }

    @Test
    public void removeHostByDeviceRemove() {
        provider.modified(CTX_FOR_REMOVE);
        testProcessor.process(new TestArpPacketContext(DEV1));
        testProcessor.process(new TestNaPacketContext(DEV4));

        Device device = new DefaultDevice(ProviderId.NONE, deviceId(DEV1), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(DEVICE_REMOVED, device));
        assertEquals("incorrect remove count", 2, providerService.locationRemoveCount);

        device = new DefaultDevice(ProviderId.NONE, deviceId(DEV4), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(DEVICE_REMOVED, device));
        assertEquals("incorrect remove count", 3, providerService.locationRemoveCount);
    }

    @Test
    public void removeHostByDeviceOffline() {
        provider.modified(CTX_FOR_REMOVE);
        testProcessor.process(new TestArpPacketContext(DEV1));
        testProcessor.process(new TestArpPacketContext(DEV4));

        Device device = new DefaultDevice(ProviderId.NONE, deviceId(DEV1), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device));
        assertEquals("incorrect remove count", 2, providerService.locationRemoveCount);

        device = new DefaultDevice(ProviderId.NONE, deviceId(DEV4), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device));
        assertEquals("incorrect remove count", 3, providerService.locationRemoveCount);
    }

    @Test
    public void removeHostByDevicePortDown() {
        provider.modified(CTX_FOR_REMOVE);
        testProcessor.process(new TestArpPacketContext(DEV1));
        testProcessor.process(new TestArpPacketContext(DEV4));

        Device device = new DefaultDevice(ProviderId.NONE, deviceId(DEV1), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(PORT_UPDATED, device,
                new DefaultPort(device, portNumber(INPORT), false)));
        assertEquals("incorrect remove count", 1, providerService.locationRemoveCount);

        device = new DefaultDevice(ProviderId.NONE, deviceId(DEV4), SWITCH,
                                          "m", "h", "s", "n", new ChassisId(0L));
        deviceService.listener.event(new DeviceEvent(PORT_UPDATED, device,
                new DefaultPort(device, portNumber(INPORT), false)));
        assertEquals("incorrect remove count", 2, providerService.locationRemoveCount);
    }

    /**
     * When receiving ARP, updates location and IP.
     */
    @Test
    public void receiveArp() {
        testProcessor.process(new TestArpPacketContext(DEV1));
        assertThat("receiveArp. One host description expected",
                providerService.descriptions.size(), is(1));
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION));
        assertThat(descr.hwAddress(), is(MAC));
        assertThat(descr.ipAddress().toArray()[0], is(IP_ADDRESS));
        assertThat(descr.vlan(), is(VLAN));
    }

    /**
     * When receiving IPv4, updates location only.
     */
    @Test
    public void receiveIpv4() {
        testProcessor.process(new TestIpv4PacketContext(DEV1));
        assertThat("receiveIpv4. One host description expected",
                providerService.descriptions.size(), is(1));
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION));
        assertThat(descr.hwAddress(), is(MAC));
        assertThat(descr.ipAddress().size(), is(0));
        assertThat(descr.vlan(), is(VLAN));
    }

    /**
     * When receiving DHCP REQUEST, update MAC, location of client.
     * When receiving DHCP ACK, update MAC, location of server and IP of client.
     */
    @Test
    public void receiveDhcp() {
        // DHCP Request
        testProcessor.process(new TestDhcpRequestPacketContext(DEV1));
        assertThat("receiveDhcpRequest. One host description expected",
                providerService.descriptions.size(), is(1));
        // Should learn the MAC and location of DHCP client
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION));
        assertThat(descr.hwAddress(), is(MAC));
        assertThat(descr.ipAddress().size(), is(0));
        assertThat(descr.vlan(), is(VLAN));

        // DHCP Ack
        testProcessor.process(new TestDhcpAckPacketContext(DEV1));
        assertThat("receiveDhcpAck. Two additional host descriptions expected",
                providerService.descriptions.size(), is(3));
        // Should learn the IP of DHCP client
        HostDescription descr2 = providerService.descriptions.get(1);
        assertThat(descr2.location(), is(LOCATION));
        assertThat(descr2.hwAddress(), is(MAC));
        assertThat(descr2.ipAddress().size(), is(1));
        assertTrue(descr2.ipAddress().contains(IP_ADDRESS));
        assertThat(descr2.vlan(), is(VLAN));
        // Should also learn the MAC, location of DHCP server
        HostDescription descr3 = providerService.descriptions.get(2);
        assertThat(descr3.location(), is(LOCATION3));
        assertThat(descr3.hwAddress(), is(MAC3));
        assertThat(descr3.ipAddress().size(), is(0));
        assertThat(descr3.vlan(), is(VLAN));

    }

    /**
     * When receiving NeighborAdvertisement, updates location and IP.
     */
    @Test
    public void receiveNa() {
        testProcessor.process(new TestNaPacketContext(DEV4));
        assertThat("receiveNa. One host description expected",
                providerService.descriptions.size(), is(1));
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION2));
        assertThat(descr.hwAddress(), is(MAC2));
        assertThat(descr.ipAddress().toArray()[0], is(IP_ADDRESS2));
        assertThat(descr.vlan(), is(VLAN));
    }

    /**
     * When receiving NeighborSolicitation, updates location and IP.
     */
    @Test
    public void receiveNs() {
        testProcessor.process(new TestNsPacketContext(DEV4));
        assertThat("receiveNs. One host description expected",
                providerService.descriptions.size(), is(1));
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION2));
        assertThat(descr.hwAddress(), is(MAC2));
        assertThat(descr.ipAddress().toArray()[0], is(IP_ADDRESS2));
        assertThat(descr.vlan(), is(VLAN));
    }

    /**
     * When receiving RouterAdvertisement, ignores it.
     */
    @Test
    public void receivesRa() {
        testProcessor.process(new TestRAPacketContext(DEV4));
        assertThat("receivesRa. No host description expected",
                providerService.descriptions.size(), is(0));
    }

    /**
     * When receiving RouterSolicitation, ignores it.
     */
    @Test
    public void receiveRs() {
        testProcessor.process(new TestRSPacketContext(DEV4));
        assertThat("receiveRs. No host description expected",
                providerService.descriptions.size(), is(0));
    }

    /**
     * When receiving Duplicate Address Detection (DAD), ignores it.
     */
    @Test
    public void receiveDad() {
        testProcessor.process(new TestDadPacketContext(DEV4));
        assertThat("receiveDad. No host description expected",
                providerService.descriptions.size(), is(0));
    }

    /**
     * When receiving IPv6 multicast packet, ignores it.
     */
    @Test
    public void receiveIpv6Multicast() {
        testProcessor.process(new TestIpv6McastPacketContext(DEV4));
        assertThat("receiveIpv6Multicast. No host description expected",
                providerService.descriptions.size(), is(0));
    }

    /**
     * When receiving IPv6 unicast packet, updates location only.
     */
    @Test
    public void receiveIpv6Unicast() {
        testProcessor.process(new TestIpv6PacketContext(DEV4));
        assertThat("receiveIpv6Unicast. One host description expected",
                providerService.descriptions.size(), is(1));
        HostDescription descr = providerService.descriptions.get(0);
        assertThat(descr.location(), is(LOCATION2));
        assertThat(descr.hwAddress(), is(MAC2));
        assertThat(descr.ipAddress().size(), is(0));
        assertThat(descr.vlan(), is(VLAN));
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.coreService = null;
        provider.providerRegistry = null;
    }

    private class TestHostRegistry implements HostProviderRegistry {

        @Override
        public HostProviderService register(HostProvider provider) {
            providerService = new TestHostProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(HostProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

    private class TestHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {

        List<HostDescription> descriptions = Lists.newArrayList();
        int hostRemoveCount;
        int ipRemoveCount;
        int locationRemoveCount;

        public void clear() {
            descriptions.clear();
            hostRemoveCount = 0;
            ipRemoveCount = 0;
            locationRemoveCount = 0;
        }

        protected TestHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {
            descriptions.add(hostDescription);
        }

        @Override
        public void hostVanished(HostId hostId) {
            hostRemoveCount++;
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {
            ipRemoveCount++;
        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {
            locationRemoveCount++;
        }

    }

    private class TestPacketService extends PacketServiceAdapter {
        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            testProcessor = processor;
        }
    }

    private class TestTopologyService extends TopologyServiceAdapter {
        @Override
        public boolean isInfrastructure(Topology topology,
                                        ConnectPoint connectPoint) {
            //simulate DPID3 as an infrastructure switch
            if ((connectPoint.deviceId()).equals(deviceId(DEV3)) ||
                    connectPoint.deviceId().equals(deviceId(DEV6))) {
                return true;
            }
            return false;
        }
    }

    /**
     * Generates ARP packet.
     */
    private class TestArpPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestArpPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            ARP arp = new ARP();
            arp.setSenderProtocolAddress(IP)
                    .setSenderHardwareAddress(MAC.toBytes())
                    .setTargetHardwareAddress(BCMAC.toBytes())
                    .setTargetProtocolAddress(IP);

            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_ARP)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC.toBytes())
                    .setDestinationMACAddress(BCMAC)
                    .setPayload(arp);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates IPv4 Unicast packet.
     */
    private class TestIpv4PacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestIpv4PacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            IPv4 ipv4 = new IPv4();
            ipv4.setDestinationAddress("10.0.0.1");
            ipv4.setSourceAddress(IP_ADDRESS.toString());
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC)
                    .setDestinationMACAddress(MacAddress.valueOf("00:00:00:00:00:01"))
                    .setPayload(ipv4);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }
    /**
     * Generates DHCP REQUEST packet.
     */
    private class TestDhcpRequestPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestDhcpRequestPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            byte[] dhcpMsgType = new byte[1];
            dhcpMsgType[0] = (byte) DHCPPacketType.DHCPREQUEST.getValue();

            DHCPOption dhcpOption = new DHCPOption();
            dhcpOption.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            dhcpOption.setData(dhcpMsgType);
            dhcpOption.setLength((byte) 1);
            DHCP dhcp = new DHCP();
            dhcp.setOptions(Collections.singletonList(dhcpOption));
            dhcp.setClientHardwareAddress(MAC.toBytes());
            UDP udp = new UDP();
            udp.setPayload(dhcp);
            udp.setSourcePort(UDP.DHCP_CLIENT_PORT);
            udp.setDestinationPort(UDP.DHCP_SERVER_PORT);
            IPv4 ipv4 = new IPv4();
            ipv4.setPayload(udp);
            ipv4.setDestinationAddress(IP_ADDRESS3.toString());
            ipv4.setSourceAddress(IP_ADDRESS.toString());
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC)
                    .setDestinationMACAddress(MAC3)
                    .setPayload(ipv4);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                    portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                    ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates DHCP ACK packet.
     */
    private class TestDhcpAckPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestDhcpAckPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            byte[] dhcpMsgType = new byte[1];
            dhcpMsgType[0] = (byte) DHCPPacketType.DHCPACK.getValue();

            DHCPOption dhcpOption = new DHCPOption();
            dhcpOption.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            dhcpOption.setData(dhcpMsgType);
            dhcpOption.setLength((byte) 1);
            DHCP dhcp = new DHCP();
            dhcp.setOptions(Collections.singletonList(dhcpOption));
            dhcp.setClientHardwareAddress(MAC.toBytes());
            dhcp.setYourIPAddress(IP_ADDRESS.getIp4Address().toInt());
            UDP udp = new UDP();
            udp.setPayload(dhcp);
            udp.setSourcePort(UDP.DHCP_SERVER_PORT);
            udp.setDestinationPort(UDP.DHCP_CLIENT_PORT);
            IPv4 ipv4 = new IPv4();
            ipv4.setPayload(udp);
            ipv4.setDestinationAddress(IP_ADDRESS.toString());
            ipv4.setSourceAddress(IP_ADDRESS3.toString());
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC3)
                    .setDestinationMACAddress(MAC)
                    .setPayload(ipv4);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                    portNumber(INPORT2));
            return new DefaultInboundPacket(receivedFrom, eth,
                    ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates NeighborAdvertisement packet.
     */
    private class TestNaPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestNaPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            NeighborAdvertisement na = new NeighborAdvertisement();
            ICMP6 icmp6 = new ICMP6();
            icmp6.setPayload(na);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(icmp6);
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::1").toOctets());
            ipv6.setSourceAddress(IP2);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(BCMAC2)
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates NeighborSolicitation packet.
     */
    private class TestNsPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestNsPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            NeighborSolicitation ns = new NeighborSolicitation();
            ICMP6 icmp6 = new ICMP6();
            icmp6.setPayload(ns);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(icmp6);
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::1:ff00:0000").toOctets());
            ipv6.setSourceAddress(IP2);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(BCMAC2)
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates Duplicate Address Detection packet.
     */
    private class TestDadPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestDadPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            NeighborSolicitation ns = new NeighborSolicitation();
            ICMP6 icmp6 = new ICMP6();
            icmp6.setPayload(ns);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(icmp6);
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::1").toOctets());
            ipv6.setSourceAddress(Ip6Address.valueOf("::").toOctets());
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(BCMAC2)
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates Router Solicitation packet.
     */
    private class TestRSPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestRSPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            RouterSolicitation ns = new RouterSolicitation();
            ICMP6 icmp6 = new ICMP6();
            icmp6.setPayload(ns);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(icmp6);
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::2").toOctets());
            ipv6.setSourceAddress(Ip6Address.valueOf("::").toOctets());
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(MacAddress.valueOf("33:33:00:00:00:02"))
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates Router Advertisement packet.
     */
    private class TestRAPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestRAPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            RouterAdvertisement ns = new RouterAdvertisement();
            ICMP6 icmp6 = new ICMP6();
            icmp6.setPayload(ns);
            IPv6 ipv6 = new IPv6();
            ipv6.setPayload(icmp6);
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::1").toOctets());
            ipv6.setSourceAddress(IP2);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(MacAddress.valueOf("33:33:00:00:00:01"))
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates IPv6 Multicast packet.
     */
    private class TestIpv6McastPacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestIpv6McastPacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            IPv6 ipv6 = new IPv6();
            ipv6.setDestinationAddress(Ip6Address.valueOf("ff02::1").toOctets());
            ipv6.setSourceAddress(IP2);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2.toBytes())
                    .setDestinationMACAddress(MacAddress.valueOf("33:33:00:00:00:01"))
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    /**
     * Generates IPv6 Unicast packet.
     */
    private class TestIpv6PacketContext extends PacketContextAdapter {
        private final String deviceId;

        public TestIpv6PacketContext(String deviceId) {
            super(0, null, null, false);
            this.deviceId = deviceId;
        }

        @Override
        public InboundPacket inPacket() {
            IPv6 ipv6 = new IPv6();
            ipv6.setDestinationAddress(Ip6Address.valueOf("1000::1").toOctets());
            ipv6.setSourceAddress(IP2);
            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV6)
                    .setVlanID(VLAN.toShort())
                    .setSourceMACAddress(MAC2)
                    .setDestinationMACAddress(MacAddress.valueOf("00:00:00:00:00:01"))
                    .setPayload(ipv6);
            ConnectPoint receivedFrom = new ConnectPoint(deviceId(deviceId),
                                                         portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }
    }

    private class TestDeviceService extends DeviceServiceAdapter {
        private DeviceListener listener;

        @Override
        public void addListener(DeviceListener listener) {
            this.listener = listener;
        }

        @Override
        public Iterable<Device> getDevices() {
            return Collections.emptyList();
        }
    }

    private class TestHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            ConnectPoint cp1 = new ConnectPoint(deviceId(DEV1), portNumber(INPORT));
            ConnectPoint cp2 = new ConnectPoint(deviceId(DEV4), portNumber(INPORT));
            ConnectPoint cp3 = new ConnectPoint(deviceId(DEV1), portNumber(INPORT2));
            if (connectPoint.equals(cp1)) {
                return ImmutableSet.of(HOST);
            } else if (connectPoint.equals(cp2)) {
                return ImmutableSet.of(HOST2);
            } else if (connectPoint.equals(cp3)) {
                return ImmutableSet.of(HOST3);
            } else {
                return ImmutableSet.of();
            }
        }

        @Override
        public Set<Host> getConnectedHosts(DeviceId deviceId) {
            if (deviceId.equals(deviceId(DEV1))) {
                return ImmutableSet.of(HOST, HOST3);
            } else if (deviceId.equals(deviceId(DEV4))) {
                return ImmutableSet.of(HOST2);
            } else {
                return ImmutableSet.of();
            }
        }

        @Override
        public Host getHost(HostId hostId) {
            if (hostId.equals(HostId.hostId(MAC, VLAN))) {
                return HOST;
            } else if (hostId.equals(HostId.hostId(MAC2, VLAN))) {
                return HOST2;
            } else if (hostId.equals(HostId.hostId(MAC3, VLAN))) {
                return HOST3;
            }
            return null;
        }

    }
}
