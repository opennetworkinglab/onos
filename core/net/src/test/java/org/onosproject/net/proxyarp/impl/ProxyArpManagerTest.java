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
package org.onosproject.net.proxyarp.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.proxyarp.ProxyArpStore;
import org.onosproject.net.proxyarp.ProxyArpStoreDelegate;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link ProxyArpManager} class.
 */
public class ProxyArpManagerTest {

    private static final int NUM_DEVICES = 10;
    private static final int NUM_PORTS_PER_DEVICE = 3;
    private static final int LAST_CONF_DEVICE_INTF_VLAN_IP = 3;
    private static final int LAST_CONF_DEVICE_INTF_VLAN = 6;

    private static final Ip4Address IP1 = Ip4Address.valueOf("192.168.1.1");
    private static final Ip4Address IP2 = Ip4Address.valueOf("192.168.1.2");
    private static final Ip6Address IP3 = Ip6Address.valueOf("1000:ffff::1");
    private static final Ip6Address IP4 = Ip6Address.valueOf("1000:ffff::2");

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 2);
    private static final VlanId VLAN10 = VlanId.vlanId((short) 10);

    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:00:00:00:04");
    private static final MacAddress MAC10 = MacAddress.valueOf("00:00:00:00:00:0A");

    private static final MacAddress SOLICITED_MAC3 = MacAddress.valueOf("33:33:FF:00:00:01");

    private static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN1);
    private static final HostId HID3 = HostId.hostId(MAC3, VLAN1);
    private static final HostId HID4 = HostId.hostId(MAC4, VLAN1);
    private static final HostId HID10 = HostId.hostId(MAC10, VLAN10);

    private static final HostId SOLICITED_HID3 = HostId.hostId(SOLICITED_MAC3, VLAN1);

    private static final DeviceId DID1 = getDeviceId(1);
    private static final DeviceId DID2 = getDeviceId(2);

    private static final PortNumber P1 = PortNumber.portNumber(1);

    private static final HostLocation LOC1 = new HostLocation(DID1, P1, 123L);
    private static final HostLocation LOC2 = new HostLocation(DID2, P1, 123L);

    private final byte[] zeroMacAddress = MacAddress.ZERO.toBytes();

    // The first three devices in the topology have interfaces configured
    // with VLANs and IPs
    private final List<ConnectPoint> configIpCPoints = new ArrayList<>();

    // Other three devices in the topology (from 4 to 6) have interfaces
    // configured only with VLANs
    private final List<ConnectPoint> configVlanCPoints = new ArrayList<>();

    // Remaining devices in the network (id > 6) don't have any interface
    // configured.
    private final List<ConnectPoint> noConfigCPoints = new ArrayList<>();

    private ProxyArpManager proxyArp;

    private TestPacketService packetService;
    private DeviceService deviceService;
    private EdgePortService edgePortService;
    private LinkService linkService;
    private HostService hostService;
    private InterfaceService interfaceService;

    @Before
    public void setUp() throws Exception {
        proxyArp = new ProxyArpManager();
        packetService = new TestPacketService();
        proxyArp.packetService = packetService;
        proxyArp.store = new TestProxyArpStoreAdapter();

        // Create a host service mock here.
        hostService = createMock(HostService.class);
        proxyArp.hostService = hostService;

        // Create an edge port service.
        edgePortService = createMock(EdgePortService.class);
        proxyArp.edgeService = edgePortService;

        // Create interface service
        interfaceService = createMock(InterfaceService.class);
        proxyArp.interfaceService = interfaceService;

        // Create the topology
        createTopology();
        proxyArp.deviceService = deviceService;
        proxyArp.linkService = linkService;

        setupNoConfigCPoints();
        setupconfigIpCPoints();
        setupconfigVlanCPoints();

        proxyArp.activate();
    }

    /**
     * Creates a fake topology to feed into the ARP module.
     * <p>
     * The default topology is a unidirectional ring topology. Each switch has
     * 3 ports. Ports 2 and 3 have the links to neighbor switches, and port 1
     * is free (edge port).
     * The first half of the switches have IP addresses configured on their
     * free ports (port 1). The second half of the switches have no IP
     * addresses configured.
     */
    private void createTopology() {
        deviceService = createMock(DeviceService.class);
        linkService = createMock(LinkService.class);

        deviceService.addListener(anyObject(DeviceListener.class));
        linkService.addListener(anyObject(LinkListener.class));

        createDevices(NUM_DEVICES, NUM_PORTS_PER_DEVICE);
        createLinks(NUM_DEVICES);
        addIntfConfig();
        popluateEdgePortService();
    }

    /**
     * Creates the devices for the fake topology.
     */
    private void createDevices(int numDevices, int numPorts) {
        List<Device> devices = new ArrayList<>();

        for (int i = 1; i <= numDevices; i++) {
            DeviceId devId = getDeviceId(i);
            Device device = createMock(Device.class);
            expect(device.id()).andReturn(devId).anyTimes();
            replay(device);

            devices.add(device);

            List<Port> ports = new ArrayList<>();
            for (int j = 1; j <= numPorts; j++) {
                Port port = createMock(Port.class);
                expect(port.number()).andReturn(PortNumber.portNumber(j)).anyTimes();
                replay(port);
                ports.add(port);
            }

            expect(deviceService.getPorts(devId)).andReturn(ports).anyTimes();
            expect(deviceService.getDevice(devId)).andReturn(device).anyTimes();
        }

        expect(deviceService.getDevices()).andReturn(devices).anyTimes();
        replay(deviceService);
    }

    /**
     * Creates the links for the fake topology.
     * NB: Only unidirectional links are created, as for this purpose all we
     * need is to occupy the ports with some link.
     */
    private void createLinks(int numDevices) {
        List<Link> links = new ArrayList<>();

        for (int i = 1; i <= numDevices; i++) {
            ConnectPoint src = new ConnectPoint(
                    getDeviceId(i),
                    PortNumber.portNumber(2));
            ConnectPoint dst = new ConnectPoint(
                    getDeviceId((i + 1 > numDevices) ? 1 : i + 1),
                    PortNumber.portNumber(3));

            Link link = createMock(Link.class);
            expect(link.src()).andReturn(src).anyTimes();
            expect(link.dst()).andReturn(dst).anyTimes();
            replay(link);

            links.add(link);
        }

        expect(linkService.getLinks()).andReturn(links).anyTimes();
        replay(linkService);
    }

    /**
     * On the first three devices two config interfaces are binded on port 1.
     * The first one with VLAN1, the second one with VLAN equals to none.
     * Both interfaces have an IP.
     * On devices 4, 5 and 6 it's binded a config interface on port 1.
     * The interface is configured with VLAN 1 and no IP.
     */
    private void addIntfConfig() {
        Set<Interface> interfaces = Sets.newHashSet();

        Set<Interface> vlanOneSet = new HashSet<>();

        for (int i = 1; i <= LAST_CONF_DEVICE_INTF_VLAN_IP; i++) {
            ConnectPoint cp = new ConnectPoint(getDeviceId(i), P1);

            // Interface addresses for IPv4
            Ip4Prefix prefix1 = Ip4Prefix.valueOf("10.0." + (2 * i - 1) + ".0/24");
            Ip4Address addr1 = Ip4Address.valueOf("10.0." + (2 * i - 1) + ".1");
            Ip4Prefix prefix2 = Ip4Prefix.valueOf("10.0." + (2 * i) + ".0/24");
            Ip4Address addr2 = Ip4Address.valueOf("10.0." + (2 * i) + ".1");
            InterfaceIpAddress ia1 = new InterfaceIpAddress(addr1, prefix1);
            InterfaceIpAddress ia2 = new InterfaceIpAddress(addr2, prefix2);

            // Interface addresses for IPv6
            Ip6Prefix prefix3 = Ip6Prefix.valueOf((2 * i - 1) + "000::0/64");
            Ip6Address addr3 = Ip6Address.valueOf((2 * i - 1) + "000::1");
            Ip6Prefix prefix4 = Ip6Prefix.valueOf((2 * i) + "000::0/64");
            Ip6Address addr4 = Ip6Address.valueOf((2 * i) + "000::2");
            InterfaceIpAddress ia3 = new InterfaceIpAddress(addr3, prefix3);
            InterfaceIpAddress ia4 = new InterfaceIpAddress(addr4, prefix4);

            // Setting up interfaces
            Interface intf1 = new Interface(Interface.NO_INTERFACE_NAME, cp,
                    Lists.newArrayList(ia1, ia3), MacAddress.valueOf(2 * i - 1), VlanId.vlanId((short) 1));
            Interface intf2 = new Interface(Interface.NO_INTERFACE_NAME, cp,
                    Lists.newArrayList(ia2, ia4), MacAddress.valueOf(2 * i), VlanId.NONE);

            interfaces.add(intf1);
            interfaces.add(intf2);

            vlanOneSet.add(intf1);

            expect(interfaceService.getInterfacesByPort(cp))
                    .andReturn(Sets.newHashSet(intf1, intf2)).anyTimes();
        }
        for (int i = LAST_CONF_DEVICE_INTF_VLAN_IP + 1; i <= LAST_CONF_DEVICE_INTF_VLAN; i++) {
            ConnectPoint cp = new ConnectPoint(getDeviceId(i), P1);
            Interface intf1 = new Interface(Interface.NO_INTERFACE_NAME, cp, Collections.emptyList(),
                    MacAddress.NONE,
                    VlanId.vlanId((short) 1));

            interfaces.add(intf1);
            vlanOneSet.add(intf1);

            expect(interfaceService.getInterfacesByPort(cp))
                    .andReturn(Sets.newHashSet(intf1)).anyTimes();
        }
        expect(interfaceService.getInterfacesByVlan(VLAN1))
                .andReturn(vlanOneSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN10))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();

        for (int i = LAST_CONF_DEVICE_INTF_VLAN + 1; i <= NUM_DEVICES; i++) {
            ConnectPoint cp = new ConnectPoint(getDeviceId(i),
                    P1);
            expect(interfaceService.getInterfacesByPort(cp))
                    .andReturn(Collections.emptySet()).anyTimes();
        }
    }

    /**
     * Populates edge ports in the EdgePortService to return all port 1
     * as edge ports.
     */
    private void popluateEdgePortService() {
        Set<ConnectPoint> edgeConnectPoints = new HashSet<>();

        for (int i = 1; i <= NUM_DEVICES; i++) {
            for (int j = 1; j <= NUM_PORTS_PER_DEVICE; j++) {
                ConnectPoint edgeConnectPoint = new ConnectPoint(
                        getDeviceId(i),
                        PortNumber.portNumber(1));
                ConnectPoint noEdgeConnectPointOne = new ConnectPoint(
                        getDeviceId(i),
                        PortNumber.portNumber(2));
                ConnectPoint noEdgeConnectPointTwo = new ConnectPoint(
                        getDeviceId(i),
                        PortNumber.portNumber(3));

                edgeConnectPoints.add(edgeConnectPoint);

                expect(edgePortService.isEdgePoint(edgeConnectPoint))
                        .andReturn(true).anyTimes();
                expect(edgePortService.isEdgePoint(noEdgeConnectPointOne))
                        .andReturn(false).anyTimes();
                expect(edgePortService.isEdgePoint(noEdgeConnectPointTwo))
                        .andReturn(false).anyTimes();
            }
        }
        expect(edgePortService.getEdgePoints())
                .andReturn(edgeConnectPoints).anyTimes();

        replay(edgePortService);
    }

    /**
     * Creates a list of connect points used to verify floodling on ports
     * with no interfaces configured (all ports without interface config).
     */
    private void setupNoConfigCPoints() {
        for (int i = NUM_DEVICES / 2 + 2; i <= NUM_DEVICES; i++) {
            ConnectPoint connectPoint = new ConnectPoint(
                    getDeviceId(i),
                    PortNumber.portNumber(1));
            noConfigCPoints.add(connectPoint);
        }
    }

    /**
     * Creates a list of connect points used to verify floodling on ports
     * with interfaces configured (both VLAN and IP).
     */
    private void setupconfigIpCPoints() {
        for (int i = 1; i <= 3; i++) {
            ConnectPoint connectPoint = new ConnectPoint(
                    getDeviceId(i),
                    PortNumber.portNumber(1));
            configIpCPoints.add(connectPoint);
        }
    }

    /**
     * Creates a list of connect points used to verify floodling on ports
     * with interfaces configured (both VLAN and IP).
     */
    private void setupconfigVlanCPoints() {
        for (int i = LAST_CONF_DEVICE_INTF_VLAN_IP + 1; i <= LAST_CONF_DEVICE_INTF_VLAN; i++) {
            ConnectPoint connectPoint = new ConnectPoint(
                    getDeviceId(i),
                    PortNumber.portNumber(1));
            configVlanCPoints.add(connectPoint);
        }
    }

    /**
     * Tests {@link ProxyArpManager#isKnown(org.onlab.packet.IpAddress)} in the
     * case where the IP address is not known.
     * Verifies the method returns false.
     */
    @Test
    public void testNotKnown() {
        expect(hostService.getHostsByIp(IP1)).andReturn(Collections.emptySet());
        replay(hostService);
        replay(interfaceService);

        assertFalse(proxyArp.isKnown(IP1));
    }

    /**
     * Tests {@link ProxyArpManager#isKnown(org.onlab.packet.IpAddress)} in the
     * case where the IP address is known.
     * Verifies the method returns true.
     */
    @Test
    public void testKnown() {
        Host host1 = createMock(Host.class);
        Host host2 = createMock(Host.class);

        expect(hostService.getHostsByIp(IP1))
                .andReturn(Sets.newHashSet(host1, host2));
        replay(hostService);
        replay(interfaceService);

        assertTrue(proxyArp.isKnown(IP1));
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is known.
     * Two host using the same VLAN are registered on the host service on devices 5 and 6.
     * Host on port 6 asks for the MAC of the device on port 5.
     * Since the destination mac address is known, the request is not flooded to anywhere
     * and ONOS directly builds an ARP reply, sended back to the requester on device 6.
     * It's verified that a proper ARP reply is received on port 1 of device 6.
     */
    @Test
    public void testReplyKnown() {
        Host requestor = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(NUM_DEVICES),
                Collections.singleton(IP1));

        Host replyer = new DefaultHost(PID, HID2, MAC2, VLAN1, getLocation(NUM_DEVICES - 1),
                Collections.singleton(IP2));

        expect(hostService.getHostsByIp(IP2))
                .andReturn(Collections.singleton(replyer));
        expect(hostService.getHost(HID1)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, IP1, IP2);

        proxyArp.reply(arpRequest, getLocation(NUM_DEVICES));

        assertEquals(1, packetService.packets.size());
        Ethernet arpReply = buildArp(ARP.OP_REPLY, VLAN1, MAC2, MAC1, IP2, IP1);
        verifyPacketOut(arpReply, getLocation(NUM_DEVICES), packetService.packets.get(0));
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is known.
     * Verifies the correct NDP reply is sent out the correct port.
     */
    @Test
    public void testReplyKnownIpv6() {
        Host replyer = new DefaultHost(PID, HID3, MAC3, VLAN1, getLocation(4),
                                       Collections.singleton(IP3));

        Host requestor = new DefaultHost(PID, HID4, MAC4, VLAN1, getLocation(5),
                                         Collections.singleton(IP4));

        expect(hostService.getHostsByIp(IP3))
                .andReturn(Collections.singleton(replyer));
        expect(hostService.getHost(HID4)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC4, SOLICITED_MAC3,
                IP4, IP3);

        proxyArp.reply(ndpRequest, getLocation(5));

        assertEquals(1, packetService.packets.size());
        Ethernet ndpReply = buildNdp(ICMP6.NEIGHBOR_ADVERTISEMENT,
                MAC3, MAC4, IP3, IP4);
        verifyPacketOut(ndpReply, getLocation(5), packetService.packets.get(0));
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is not known.
     * Only a requestor is present (on device 6, port 1). The device has a VLAN configured
     * which is not configured anywhere in the system.
     * Since the destination is not known, and since the ARP request can't be sent out of
     * interfaces configured, the ARP request is flooded out of ports 4 and 5.
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyUnknown() {
        Host requestor = new DefaultHost(PID, HID10, MAC10, VLAN10, getLocation(NUM_DEVICES),
                Collections.singleton(IP1));

        expect(hostService.getHostsByIp(IP2))
                .andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(IP1))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID10)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN10, MAC10, null, IP1, IP2);

        proxyArp.reply(arpRequest, getLocation(NUM_DEVICES));

        verifyFlood(arpRequest, noConfigCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is not known.
     * Verifies the NDP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyUnknownIpv6() {
        Host requestor = new DefaultHost(PID, HID4, MAC4, VLAN1, getLocation(NUM_DEVICES),
                                         Collections.singleton(IP4));

        expect(hostService.getHostsByIp(IP3))
                .andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(IP4))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID4)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC4, SOLICITED_MAC3,
                IP4, IP3);

        proxyArp.reply(ndpRequest, getLocation(NUM_DEVICES));

        verifyFlood(ndpRequest, noConfigCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is known for that IP address, but is not on the same
     * VLAN as the source host.
     * An host is connected on device 6, port 1 where no interfaces are defined. It sends
     * ARP requests from VLAN10, not configured anywhere in the network. Another host with
     * the IP address requested lives on device 5, port 1 in the network. Anyway, since the
     * host uses another VLAN it's not found and the ARP packet is flooded out of port
     * 4 and 5.
     *
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyDifferentVlan() {
        Host requestor = new DefaultHost(PID, HID10, MAC10, VLAN10, getLocation(NUM_DEVICES),
                Collections.singleton(IP1));

        Host replyer = new DefaultHost(PID, HID2, MAC2, VLAN2, getLocation(NUM_DEVICES - 1),
                Collections.singleton(IP2));

        expect(hostService.getHostsByIp(IP2))
                .andReturn(Collections.singleton(replyer));
        expect(interfaceService.getInterfacesByIp(IP1))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID10)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN10, MAC10, null, IP1, IP2);

        proxyArp.reply(arpRequest, getLocation(NUM_DEVICES));

        verifyFlood(arpRequest, noConfigCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * a vlan packet comes in from a port without interfaces configured. The destination
     * host is unknown for that IP address and there are some interfaces configured on
     * the same vlan.
     * It's expected to see the ARP request going out through ports with no interfaces
     * configured, devices 4 and 5, port 1.
     *
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testConfiguredVlan() {
        Host requestor = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(NUM_DEVICES),
                Collections.singleton(IP1));

        expect(hostService.getHostsByIp(IP2))
                .andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(IP1))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID1)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, IP1, IP2);

        proxyArp.reply(arpRequest, getLocation(NUM_DEVICES));

        verifyFlood(arpRequest, noConfigCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * a vlan packet comes in from a port without interfaces configured. The destination
     * host is not known for that IP address and there are some interfaces configured on
     * the same vlan.
     * It's expected to see the ARP request going out through ports with no interfaces
     * configured, devices 4 and 5, port 1.
     *
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testConfiguredVlanOnInterfaces() {
        Host requestor = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(6),
                Collections.singleton(IP1));

        expect(hostService.getHostsByIp(IP2))
                .andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(IP1))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID1)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, IP1, IP2);

        proxyArp.reply(arpRequest, getLocation(6));

        verifyFlood(arpRequest, configVlanCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet, ConnectPoint)} in the case where the
     * destination host is known for that IP address, but is not on the same
     * VLAN as the source host.
     * Verifies the NDP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyDifferentVlanIpv6() {
        Host requestor = new DefaultHost(PID, HID4, MAC4, VLAN1, getLocation(NUM_DEVICES),
                                         Collections.singleton(IP4));

        Host replyer = new DefaultHost(PID, HID3, MAC3, VLAN2, getLocation(NUM_DEVICES - 1),
                Collections.singleton(IP3));

        expect(hostService.getHostsByIp(IP3))
                .andReturn(Collections.singleton(replyer));
        expect(interfaceService.getInterfacesByIp(IP4))
                .andReturn(Collections.emptySet());
        expect(hostService.getHost(HID4)).andReturn(requestor);

        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC4, SOLICITED_MAC3,
                IP4, IP3);

        proxyArp.reply(ndpRequest, getLocation(NUM_DEVICES));

        verifyFlood(ndpRequest, noConfigCPoints);
    }

    /**
     * Test ARP request from external network to an internal host.
     */
    @Test
    public void testReplyToRequestForUs() {
        Ip4Address theirIp = Ip4Address.valueOf("10.0.1.254");
        Ip4Address ourFirstIp = Ip4Address.valueOf("10.0.1.1");
        Ip4Address ourSecondIp = Ip4Address.valueOf("10.0.2.1");
        MacAddress firstMac = MacAddress.valueOf(1L);
        MacAddress secondMac = MacAddress.valueOf(2L);

        Host requestor = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1,
                Collections.singleton(theirIp));

        expect(hostService.getHost(HID1)).andReturn(requestor);
        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, theirIp, ourFirstIp);

        proxyArp.reply(arpRequest, LOC1);

        assertEquals(1, packetService.packets.size());
        Ethernet arpReply = buildArp(ARP.OP_REPLY, VLAN1, firstMac, MAC1, ourFirstIp, theirIp);
        verifyPacketOut(arpReply, LOC1, packetService.packets.get(0));

        // Test a request for the second address on that port
        packetService.packets.clear();
        arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, theirIp, ourSecondIp);

        proxyArp.reply(arpRequest, LOC1);

        assertEquals(1, packetService.packets.size());
        arpReply = buildArp(ARP.OP_REPLY, VLAN1, secondMac, MAC1, ourSecondIp, theirIp);
        verifyPacketOut(arpReply, LOC1, packetService.packets.get(0));
    }

    /**
     * Test NDP request from external network to an internal host.
     */
    @Test
    public void testReplyToRequestForUsIpv6() {
        Ip6Address theirIp = Ip6Address.valueOf("1000::ffff");
        Ip6Address ourFirstIp = Ip6Address.valueOf("1000::1");
        Ip6Address ourSecondIp = Ip6Address.valueOf("2000::2");
        MacAddress firstMac = MacAddress.valueOf(1L);
        MacAddress secondMac = MacAddress.valueOf(2L);

        Host requestor = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC1,
                                         Collections.singleton(theirIp));

        expect(hostService.getHost(HID2)).andReturn(requestor);
        expect(hostService.getHostsByIp(ourFirstIp))
                .andReturn(Collections.singleton(requestor));
        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC2,
                MacAddress.valueOf("33:33:ff:00:00:01"),
                theirIp,
                ourFirstIp);

        proxyArp.reply(ndpRequest, LOC1);
        assertEquals(1, packetService.packets.size());

        Ethernet ndpReply = buildNdp(ICMP6.NEIGHBOR_ADVERTISEMENT,
                firstMac,
                MAC2,
                ourFirstIp,
                theirIp);
        verifyPacketOut(ndpReply, LOC1, packetService.packets.get(0));

        // Test a request for the second address on that port
        packetService.packets.clear();
        ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC2,
                MacAddress.valueOf("33:33:ff:00:00:01"),
                theirIp,
                ourSecondIp);
        proxyArp.reply(ndpRequest, LOC1);
        assertEquals(1, packetService.packets.size());

        ndpReply = buildNdp(ICMP6.NEIGHBOR_ADVERTISEMENT,
                secondMac,
                MAC2,
                ourSecondIp,
                theirIp);
        verifyPacketOut(ndpReply, LOC1, packetService.packets.get(0));
    }

    /**
     * Request for a valid external IPv4 address but coming in the wrong port.
     */
    @Test
    public void testReplyExternalPortBadRequest() {
        replay(hostService); // no further host service expectations
        replay(interfaceService);

        Ip4Address theirIp = Ip4Address.valueOf("10.0.1.254");

        // Request for a valid external IP address but coming in the wrong port
        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, theirIp,
                Ip4Address.valueOf("10.0.3.1"));
        proxyArp.reply(arpRequest, LOC1);
        assertEquals(0, packetService.packets.size());

        // Request for a valid internal IP address but coming in an external port
        packetService.packets.clear();
        arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, MAC1, null, theirIp, IP1);
        proxyArp.reply(arpRequest, LOC1);
        assertEquals(0, packetService.packets.size());
    }

    /**
     * Request for a valid external IPv6 address but coming in the wrong port.
     */
    @Test
    public void testReplyExternalPortBadRequestIpv6() {
        replay(hostService); // no further host service expectations
        replay(interfaceService);

        Ip6Address theirIp = Ip6Address.valueOf("1000::ffff");

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC1,
                MacAddress.valueOf("33:33:ff:00:00:01"),
                theirIp,
                Ip6Address.valueOf("3000::1"));
        proxyArp.reply(ndpRequest, LOC1);
        assertEquals(0, packetService.packets.size());

        // Request for a valid internal IP address but coming in an external port
        packetService.packets.clear();
        ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC1,
                MacAddress.valueOf("33:33:ff:00:00:01"),
                theirIp,
                IP3);
        proxyArp.reply(ndpRequest, LOC1);
        assertEquals(0, packetService.packets.size());
    }

    /**
     * Test ARP request from internal network to an external host.
     */
    @Test
    public void testReplyToRequestFromUs() {
        Ip4Address ourIp = Ip4Address.valueOf("10.0.1.1");
        MacAddress ourMac = MacAddress.valueOf(1L);
        Ip4Address theirIp = Ip4Address.valueOf("10.0.1.100");

        expect(hostService.getHostsByIp(theirIp)).andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(ourIp))
                .andReturn(Collections.singleton(new Interface(Interface.NO_INTERFACE_NAME, getLocation(1),
                        Collections.singletonList(new InterfaceIpAddress(ourIp, IpPrefix.valueOf("10.0.1.1/24"))),
                        ourMac, VLAN1)));
        expect(hostService.getHost(HostId.hostId(ourMac, VLAN1))).andReturn(null);
        replay(hostService);
        replay(interfaceService);

        // This is a request from something inside our network (like a BGP
        // daemon) to an external host.
        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, VLAN1, ourMac, null, ourIp, theirIp);
        //Ensure the packet is allowed through (it is not to an internal port)

        proxyArp.reply(arpRequest, getLocation(5));
        assertEquals(1, packetService.packets.size());
        verifyPacketOut(arpRequest, getLocation(1), packetService.packets.get(0));

        // The same request from a random external port should fail
        packetService.packets.clear();
        proxyArp.reply(arpRequest, getLocation(2));
        assertEquals(0, packetService.packets.size());
    }

    /**
     * Test NDP request from internal network to an external host.
     */
    @Test
    public void testReplyToRequestFromUsIpv6() {
        Ip6Address ourIp = Ip6Address.valueOf("1000::1");
        MacAddress ourMac = MacAddress.valueOf(1L);
        Ip6Address theirIp = Ip6Address.valueOf("1000::100");

        expect(hostService.getHostsByIp(theirIp)).andReturn(Collections.emptySet());
        expect(interfaceService.getInterfacesByIp(ourIp))
                .andReturn(Collections.singleton(new Interface(Interface.NO_INTERFACE_NAME, getLocation(1),
                        Collections.singletonList(new InterfaceIpAddress(
                                ourIp,
                                IpPrefix.valueOf("1000::1/64"))),
                                ourMac,
                                VLAN1)));
        expect(hostService.getHost(HostId.hostId(ourMac, VLAN1))).andReturn(null);
        replay(hostService);
        replay(interfaceService);

        // This is a request from something inside our network (like a BGP
        // daemon) to an external host.
        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                ourMac,
                MacAddress.valueOf("33:33:ff:00:00:01"),
                ourIp,
                theirIp);

        proxyArp.reply(ndpRequest, getLocation(5));
        assertEquals(1, packetService.packets.size());
        verifyPacketOut(ndpRequest, getLocation(1), packetService.packets.get(0));

        // The same request from a random external port should fail
        packetService.packets.clear();
        proxyArp.reply(ndpRequest, getLocation(2));
        assertEquals(0, packetService.packets.size());
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet, ConnectPoint)} in the case where the
     * destination host is known.
     * Verifies the correct ARP request is sent out the correct port.
     */
    @Test
    public void testForwardToHost() {
        Host host1 = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1,
                Collections.singleton(IP1));
        Host host2 = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC2,
                                     Collections.singleton(IP2));

        expect(hostService.getHost(HID1)).andReturn(host1);
        expect(hostService.getHost(HID2)).andReturn(host2);
        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REPLY, VLAN1, MAC2, MAC1, IP2, IP1);

        proxyArp.forward(arpRequest, LOC2);

        assertEquals(1, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        verifyPacketOut(arpRequest, LOC1, packet);
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet, ConnectPoint)} in the case where the
     * destination host is known.
     * Verifies the correct ARP request is sent out the correct port.
     */
    @Test
    public void testForwardToHostIpv6() {
        Host host1 = new DefaultHost(PID, HID3, MAC3, VLAN1, LOC1,
                                     Collections.singleton(IP3));
        Host host2 = new DefaultHost(PID, HID4, MAC4, VLAN1, LOC2,
                                     Collections.singleton(IP4));

        expect(hostService.getHost(SOLICITED_HID3)).andReturn(host1);
        expect(hostService.getHost(HID4)).andReturn(host2);
        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC4, SOLICITED_MAC3,
                IP4, IP3);

        proxyArp.forward(ndpRequest, LOC2);

        assertEquals(1, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        verifyPacketOut(ndpRequest, LOC1, packet);
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet, ConnectPoint)} in the case where the
     * destination host is not known.
     * Verifies the correct ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testForwardFlood() {
        expect(hostService.getHost(HID2)).andReturn(null);
        replay(hostService);
        replay(interfaceService);

        Ethernet arpRequest = buildArp(ARP.OP_REPLY, VLAN1, MAC1, MAC2, IP1, IP2);

        proxyArp.forward(arpRequest, getLocation(NUM_DEVICES));

        verifyFlood(arpRequest, noConfigCPoints);
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet, ConnectPoint)} in the case where the
     * destination host is not known.
     * Verifies the correct NDP request is flooded out the correct edge ports.
     */
    @Test
    public void testForwardFloodIpv6() {
        expect(hostService.getHost(SOLICITED_HID3)).andReturn(null);
        replay(hostService);
        replay(interfaceService);

        Ethernet ndpRequest = buildNdp(ICMP6.NEIGHBOR_SOLICITATION,
                MAC4, SOLICITED_MAC3,
                IP4, IP3);

        proxyArp.forward(ndpRequest, getLocation(NUM_DEVICES));

        verifyFlood(ndpRequest, noConfigCPoints);
    }

    /**
     * Verifies that the given packet was flooded out all available edge ports,
     * except for the input port.
     *
     * @param packet the packet that was expected to be flooded
     * @param connectPoints the connectPoints where the outpacket should be
     *                      observed
     */
    private void verifyFlood(Ethernet packet, List<ConnectPoint> connectPoints) {

        // There should be 1 less than NUM_FLOOD_PORTS; the inPort should be excluded.
        assertEquals(connectPoints.size() - 1, packetService.packets.size());

        Collections.sort(packetService.packets,
                (o1, o2) -> o1.sendThrough().uri().compareTo(o2.sendThrough().uri()));

        for (int i = 0; i < connectPoints.size() - 1; i++) {
            OutboundPacket outboundPacket = packetService.packets.get(i);
            verifyPacketOut(packet, connectPoints.get(i), outboundPacket);
        }
    }

    /**
     * Verifies the given packet was sent out the given port.
     *
     * @param expected the packet that was expected to be sent
     * @param outPort  the port the packet was expected to be sent out
     * @param actual   the actual OutboundPacket to verify
     */
    private void verifyPacketOut(Ethernet expected, ConnectPoint outPort,
                                 OutboundPacket actual) {
        assertArrayEquals(expected.serialize(), actual.data().array());
        assertEquals(1, actual.treatment().immediate().size());
        assertEquals(outPort.deviceId(), actual.sendThrough());
        Instruction instruction = actual.treatment().immediate().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        assertEquals(outPort.port(), ((OutputInstruction) instruction).port());
    }

    /**
     * Returns the device ID of the ith device.
     *
     * @param i device to get the ID of
     * @return the device ID
     */
    private static DeviceId getDeviceId(int i) {
        return DeviceId.deviceId("" + i);
    }

    private static HostLocation getLocation(int i) {
        return new HostLocation(new ConnectPoint(getDeviceId(i), P1), 123L);
    }

    /**
     * Builds an ARP packet with the given parameters.
     *
     * @param opcode opcode of the ARP packet
     * @param srcMac source MAC address
     * @param dstMac destination MAC address, or null if this is a request
     * @param srcIp  source IP address
     * @param dstIp  destination IP address
     * @return the ARP packet
     */
    private Ethernet buildArp(short opcode, VlanId vlanId, MacAddress srcMac,
                              MacAddress dstMac, Ip4Address srcIp, Ip4Address dstIp) {
        Ethernet eth = new Ethernet();

        if (dstMac == null) {
            eth.setDestinationMACAddress(MacAddress.BROADCAST);
        } else {
            eth.setDestinationMACAddress(dstMac);
        }

        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_ARP);
        eth.setVlanID(vlanId.toShort());

        ARP arp = new ARP();
        arp.setOpCode(opcode);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);

        arp.setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        arp.setSenderHardwareAddress(srcMac.toBytes());

        if (dstMac == null) {
            arp.setTargetHardwareAddress(zeroMacAddress);
        } else {
            arp.setTargetHardwareAddress(dstMac.toBytes());
        }

        arp.setSenderProtocolAddress(srcIp.toOctets());
        arp.setTargetProtocolAddress(dstIp.toOctets());

        eth.setPayload(arp);
        return eth;
    }

    /**
     * Builds an NDP packet with the given parameters.
     *
     * @param type NeighborSolicitation or NeighborAdvertisement
     * @param srcMac source MAC address
     * @param dstMac destination MAC address, or null if this is a request
     * @param srcIp  source IP address
     * @param dstIp  destination IP address
     * @return the NDP packet
     */
    private Ethernet buildNdp(byte type, MacAddress srcMac, MacAddress dstMac,
                              Ip6Address srcIp, Ip6Address dstIp) {
        assertThat(type, anyOf(
                is(ICMP6.NEIGHBOR_SOLICITATION),
                is(ICMP6.NEIGHBOR_ADVERTISEMENT)
        ));
        assertNotNull(srcMac);
        assertNotNull(dstMac);
        assertNotNull(srcIp);
        assertNotNull(dstIp);

        IPacket ndp;
        if (type == ICMP6.NEIGHBOR_SOLICITATION) {
            ndp = new NeighborSolicitation().setTargetAddress(dstIp.toOctets());
        } else {
            ndp = new NeighborAdvertisement()
                    .setSolicitedFlag((byte) 1)
                    .setOverrideFlag((byte) 1)
                    .setTargetAddress(srcIp.toOctets())
                    .addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                       srcMac.toBytes());
        }

        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(type);
        icmp6.setIcmpCode((byte) 0);
        icmp6.setPayload(ndp);

        IPv6 ipv6 = new IPv6();
        ipv6.setDestinationAddress(dstIp.toOctets());
        ipv6.setSourceAddress(srcIp.toOctets());
        ipv6.setNextHeader(IPv6.PROTOCOL_ICMP6);
        ipv6.setHopLimit((byte) 255);
        ipv6.setPayload(icmp6);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(dstMac);
        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setVlanID(VLAN1.toShort());
        eth.setPayload(ipv6);

        return eth;
    }

    /**
     * Test PacketService implementation that simply stores OutboundPackets
     * passed to {@link #emit(OutboundPacket)} for later verification.
     */
    class TestPacketService extends PacketServiceAdapter {

        List<OutboundPacket> packets = new ArrayList<>();

        @Override
        public void emit(OutboundPacket packet) {
            packets.add(packet);
        }

    }

    private class TestProxyArpStoreAdapter implements ProxyArpStore {
        @Override
        public void forward(ConnectPoint outPort, Host subject, ByteBuffer packet) {
            TrafficTreatment tt = DefaultTrafficTreatment.builder().setOutput(outPort.port()).build();
            packetService.emit(new DefaultOutboundPacket(outPort.deviceId(), tt, packet));
        }

        @Override
        public void setDelegate(ProxyArpStoreDelegate delegate) {
        }
    }
}
