package org.onlab.onos.net.proxyarp.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;

/**
 * Tests for the {@link ProxyArpManager} class.
 */
public class ProxyArpManagerTest {

    private static final int NUM_DEVICES = 4;
    private static final int NUM_PORTS_PER_DEVICE = 3;
    private static final int NUM_FLOOD_PORTS = 4;

    private static final IpPrefix IP1 = IpPrefix.valueOf("10.0.0.1/24");
    private static final IpPrefix IP2 = IpPrefix.valueOf("10.0.0.2/24");

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 2);
    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:22:00:00:02");
    private static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN1);

    private static final DeviceId DID1 = getDeviceId(1);
    private static final DeviceId DID2 = getDeviceId(2);
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final HostLocation LOC1 = new HostLocation(DID1, P1, 123L);
    private static final HostLocation LOC2 = new HostLocation(DID2, P1, 123L);

    private ProxyArpManager proxyArp;

    private TestPacketService packetService;

    private DeviceService deviceService;
    private LinkService linkService;
    private HostService hostService;

    @Before
    public void setUp() throws Exception {
        proxyArp = new ProxyArpManager();
        packetService = new TestPacketService();
        proxyArp.packetService = packetService;

        // Create a host service mock here. Must be replayed by tests once the
        // expectations have been set up
        hostService = createMock(HostService.class);
        proxyArp.hostService = hostService;

        createTopology();
        proxyArp.deviceService = deviceService;
        proxyArp.linkService = linkService;

        proxyArp.activate();
    }

    /**
     * Creates a fake topology to feed into the ARP module.
     * <p/>
     * The default topology is a unidirectional ring topology. Each switch has
     * 3 ports. Ports 2 and 3 have the links to neighbor switches, and port 1
     * is free (edge port).
     */
    private void createTopology() {
        deviceService = createMock(DeviceService.class);
        linkService = createMock(LinkService.class);

        deviceService.addListener(anyObject(DeviceListener.class));
        linkService.addListener(anyObject(LinkListener.class));

        createDevices(NUM_DEVICES, NUM_PORTS_PER_DEVICE);
        createLinks(NUM_DEVICES);
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

            expect(deviceService.getPorts(devId)).andReturn(ports);
        }

        expect(deviceService.getDevices()).andReturn(devices);
        replay(deviceService);
    }

    /**
     * Creates the links for the fake topology.
     * NB: Only unidirectional links are created, as for this purpose all we
     * need is to occupy the ports with some link.
     */
    private void createLinks(int numDevices) {
        List<Link> links = new ArrayList<Link>();

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
     * Tests {@link ProxyArpManager#known(IpPrefix)} in the case where the
     * IP address is not known.
     * Verifies the method returns false.
     */
    @Test
    public void testNotKnown() {
        expect(hostService.getHostsByIp(IP1)).andReturn(Collections.<Host>emptySet());
        replay(hostService);

        assertFalse(proxyArp.known(IP1));
    }

    /**
     * Tests {@link ProxyArpManager#known(IpPrefix)} in the case where the
     * IP address is known.
     * Verifies the method returns true.
     */
    @Test
    public void testKnown() {
        Host host1 = createMock(Host.class);
        Host host2 = createMock(Host.class);

        expect(hostService.getHostsByIp(IP1))
                .andReturn(Sets.newHashSet(host1, host2));
        replay(hostService);

        assertTrue(proxyArp.known(IP1));
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet)} in the case where the
     * destination host is known.
     * Verifies the correct ARP reply is sent out the correct port.
     */
    @Test
    public void testReplyKnown() {
        Host replyer = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC2,
                Collections.singleton(IP1));

        Host requestor = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC1,
                Collections.singleton(IP2));

        expect(hostService.getHostsByIp(IpPrefix.valueOf(IP1.toOctets())))
                .andReturn(Collections.singleton(replyer));
        expect(hostService.getHost(HID2)).andReturn(requestor);

        replay(hostService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, MAC2, null, IP2, IP1);

        proxyArp.reply(arpRequest);

        assertEquals(1, packetService.packets.size());
        Ethernet arpReply = buildArp(ARP.OP_REPLY, MAC1, MAC2, IP1, IP2);
        verifyPacketOut(arpReply, LOC1, packetService.packets.get(0));
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet)} in the case where the
     * destination host is not known.
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyUnknown() {
        Host requestor = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC1,
                Collections.singleton(IP2));

        expect(hostService.getHostsByIp(IpPrefix.valueOf(IP1.toOctets())))
                .andReturn(Collections.<Host>emptySet());
        expect(hostService.getHost(HID2)).andReturn(requestor);

        replay(hostService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, MAC2, null, IP2, IP1);

        proxyArp.reply(arpRequest);

        verifyFlood(arpRequest);
    }

    /**
     * Tests {@link ProxyArpManager#reply(Ethernet)} in the case where the
     * destination host is known for that IP address, but is not on the same
     * VLAN as the source host.
     * Verifies the ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testReplyDifferentVlan() {
        Host replyer = new DefaultHost(PID, HID1, MAC1, VLAN2, LOC2,
                Collections.singleton(IP1));

        Host requestor = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC1,
                Collections.singleton(IP2));

        expect(hostService.getHostsByIp(IpPrefix.valueOf(IP1.toOctets())))
                .andReturn(Collections.singleton(replyer));
        expect(hostService.getHost(HID2)).andReturn(requestor);

        replay(hostService);

        Ethernet arpRequest = buildArp(ARP.OP_REQUEST, MAC2, null, IP2, IP1);

        proxyArp.reply(arpRequest);

        verifyFlood(arpRequest);
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet)} in the case where the
     * destination host is known.
     * Verifies the correct ARP request is sent out the correct port.
     */
    @Test
    public void testForwardToHost() {
        Host host1 = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1,
                Collections.singleton(IP1));

        expect(hostService.getHost(HID1)).andReturn(host1);
        replay(hostService);

        Ethernet arpRequest = buildArp(ARP.OP_REPLY, MAC2, MAC1, IP2, IP1);

        proxyArp.forward(arpRequest);

        assertEquals(1, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        verifyPacketOut(arpRequest, LOC1, packet);
    }

    /**
     * Tests {@link ProxyArpManager#forward(Ethernet)} in the case where the
     * destination host is not known.
     * Verifies the correct ARP request is flooded out the correct edge ports.
     */
    @Test
    public void testForwardFlood() {
        expect(hostService.getHost(HID1)).andReturn(null);
        replay(hostService);

        Ethernet arpRequest = buildArp(ARP.OP_REPLY, MAC2, MAC1, IP2, IP1);

        proxyArp.forward(arpRequest);

        verifyFlood(arpRequest);
    }

    /**
     * Verifies that the given packet was flooded out all available edge ports.
     *
     * @param packet the packet that was expected to be flooded
     */
    private void verifyFlood(Ethernet packet) {
        assertEquals(NUM_FLOOD_PORTS, packetService.packets.size());

        Collections.sort(packetService.packets,
            new Comparator<OutboundPacket>() {
                @Override
                public int compare(OutboundPacket o1, OutboundPacket o2) {
                    return o1.sendThrough().uri().compareTo(o2.sendThrough().uri());
                }
            });

        for (int i = 0; i < NUM_FLOOD_PORTS; i++) {
            ConnectPoint cp = new ConnectPoint(getDeviceId(i + 1), PortNumber.portNumber(1));

            OutboundPacket outboundPacket = packetService.packets.get(i);
            verifyPacketOut(packet, cp, outboundPacket);
        }
    }

    /**
     * Verifies the given packet was sent out the given port.
     *
     * @param expected the packet that was expected to be sent
     * @param outPort the port the packet was expected to be sent out
     * @param actual the actual OutboundPacket to verify
     */
    private void verifyPacketOut(Ethernet expected, ConnectPoint outPort,
            OutboundPacket actual) {
        assertTrue(Arrays.equals(expected.serialize(), actual.data().array()));
        assertEquals(1, actual.treatment().instructions().size());
        assertEquals(outPort.deviceId(), actual.sendThrough());
        Instruction instruction = actual.treatment().instructions().get(0);
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

    /**
     * Builds an ARP packet with the given parameters.
     *
     * @param opcode opcode of the ARP packet
     * @param srcMac source MAC address
     * @param dstMac destination MAC address, or null if this is a request
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @return the ARP packet
     */
    private Ethernet buildArp(short opcode, MacAddress srcMac, MacAddress dstMac,
            IpPrefix srcIp, IpPrefix dstIp) {
        Ethernet eth = new Ethernet();

        if (dstMac == null) {
            eth.setDestinationMACAddress(MacAddress.BROADCAST_MAC);
        } else {
            eth.setDestinationMACAddress(dstMac.getAddress());
        }

        eth.setSourceMACAddress(srcMac.getAddress());
        eth.setEtherType(Ethernet.TYPE_ARP);
        eth.setVlanID(VLAN1.toShort());

        ARP arp = new ARP();
        arp.setOpCode(opcode);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);

        arp.setProtocolAddressLength((byte) IpPrefix.INET_LEN);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        arp.setSenderHardwareAddress(srcMac.getAddress());

        if (dstMac == null) {
            arp.setTargetHardwareAddress(MacAddress.ZERO_MAC_ADDRESS);
        } else {
            arp.setTargetHardwareAddress(dstMac.getAddress());
        }

        arp.setSenderProtocolAddress(srcIp.toOctets());
        arp.setTargetProtocolAddress(dstIp.toOctets());

        eth.setPayload(arp);
        return eth;
    }

    /**
     * Test PacketService implementation that simply stores OutboundPackets
     * passed to {@link #emit(OutboundPacket)} for later verification.
     */
    class TestPacketService implements PacketService {

        List<OutboundPacket> packets = new ArrayList<>();

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
        }

        @Override
        public void removeProcessor(PacketProcessor processor) {
        }

        @Override
        public void emit(OutboundPacket packet) {
            packets.add(packet);
        }
    }
}
