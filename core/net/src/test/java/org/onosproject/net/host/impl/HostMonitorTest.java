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
package org.onosproject.net.host.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HostMonitorTest {

    private static final IpAddress TARGET_IPV4_ADDR =
            IpAddress.valueOf("10.0.0.1");
    private static final IpAddress SOURCE_IPV4_ADDR =
            IpAddress.valueOf("10.0.0.99");
    private static final InterfaceIpAddress IA1 =
            new InterfaceIpAddress(SOURCE_IPV4_ADDR, IpPrefix.valueOf("10.0.0.0/24"));
    private MacAddress sourceMac = MacAddress.valueOf(1L);

    private static final IpAddress TARGET_IPV6_ADDR =
            IpAddress.valueOf("1000::1");
    private static final IpAddress SOURCE_IPV6_ADDR =
            IpAddress.valueOf("1000::f");
    private static final InterfaceIpAddress IA2 =
            new InterfaceIpAddress(SOURCE_IPV6_ADDR, IpPrefix.valueOf("1000::/64"));
    private MacAddress sourceMac2 = MacAddress.valueOf(2L);

    private EdgePortService edgePortService;

    private HostMonitor hostMonitor;

    @Before
    public void setUp() {
        edgePortService = createMock(EdgePortService.class);
        expect(edgePortService.isEdgePoint(anyObject(ConnectPoint.class)))
                .andReturn(true).anyTimes();
        replay(edgePortService);
    }

    @After
    public void shutdown() {
        hostMonitor.shutdown();
    }

    @Test
    public void testMonitorIpv4HostExists() throws Exception {
        testMonitorHostExists(TARGET_IPV4_ADDR);
    }

    @Test
    public void testMonitorIpv6HostExists() throws Exception {
        testMonitorHostExists(TARGET_IPV6_ADDR);
    }

    private void testMonitorHostExists(IpAddress hostIp) throws Exception {
        ProviderId id = new ProviderId("fake://", "id");

        Host host = createMock(Host.class);
        expect(host.providerId()).andReturn(id).anyTimes();
        replay(host);

        HostManager hostManager = createMock(HostManager.class);
        expect(hostManager.getHostsByIp(hostIp))
                .andReturn(Collections.singleton(host))
                .anyTimes();
        replay(hostManager);

        HostProvider hostProvider = createMock(HostProvider.class);
        expect(hostProvider.id()).andReturn(id).anyTimes();
        hostProvider.triggerProbe(host);
        expectLastCall().times(2);
        replay(hostProvider);

        hostMonitor = new HostMonitor(null, hostManager, null, edgePortService);

        hostMonitor.registerHostProvider(hostProvider);
        hostMonitor.addMonitoringFor(hostIp);

        hostMonitor.run(null);

        verify(hostProvider);
    }

    @Test
    public void testMonitorIpv4HostDoesNotExist() throws Exception {

        HostManager hostManager = createMock(HostManager.class);

        DeviceId devId = DeviceId.deviceId("fake");

        Device device = createMock(Device.class);
        expect(device.id()).andReturn(devId).anyTimes();
        replay(device);

        PortNumber portNum = PortNumber.portNumber(1L);

        Port port = createMock(Port.class);
        expect(port.number()).andReturn(portNum).anyTimes();
        replay(port);

        TestDeviceService deviceService = new TestDeviceService();
        deviceService.addDevice(device, Collections.singleton(port));

        ConnectPoint cp = new ConnectPoint(devId, portNum);

        expect(hostManager.getHostsByIp(TARGET_IPV4_ADDR))
                .andReturn(Collections.emptySet()).anyTimes();
        replay(hostManager);

        InterfaceService interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getMatchingInterface(TARGET_IPV4_ADDR))
                .andReturn(new Interface(Interface.NO_INTERFACE_NAME,
                        cp, Collections.singletonList(IA1), sourceMac, VlanId.NONE))
                .anyTimes();
        replay(interfaceService);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(packetService, hostManager, interfaceService, edgePortService);

        hostMonitor.addMonitoringFor(TARGET_IPV4_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(2, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        // Check the output port is correct
        assertEquals(1, packet.treatment().immediate().size());
        Instruction instruction = packet.treatment().immediate().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        OutputInstruction oi = (OutputInstruction) instruction;
        assertEquals(portNum, oi.port());

        // Check the output packet is correct (well the important bits anyway)
        final byte[] pktData = new byte[packet.data().remaining()];
        packet.data().get(pktData);
        Ethernet eth = Ethernet.deserializer().deserialize(pktData, 0, pktData.length);
        assertEquals(Ethernet.VLAN_UNTAGGED, eth.getVlanID());
        ARP arp = (ARP) eth.getPayload();
        assertArrayEquals(SOURCE_IPV4_ADDR.toOctets(),
                          arp.getSenderProtocolAddress());
        assertArrayEquals(sourceMac.toBytes(),
                          arp.getSenderHardwareAddress());
        assertArrayEquals(TARGET_IPV4_ADDR.toOctets(),
                          arp.getTargetProtocolAddress());
    }

    @Test
    public void testMonitorIpv6HostDoesNotExist() throws Exception {

        HostManager hostManager = createMock(HostManager.class);

        DeviceId devId = DeviceId.deviceId("fake");

        Device device = createMock(Device.class);
        expect(device.id()).andReturn(devId).anyTimes();
        replay(device);

        PortNumber portNum = PortNumber.portNumber(2L);

        Port port = createMock(Port.class);
        expect(port.number()).andReturn(portNum).anyTimes();
        replay(port);

        TestDeviceService deviceService = new TestDeviceService();
        deviceService.addDevice(device, Collections.singleton(port));

        ConnectPoint cp = new ConnectPoint(devId, portNum);

        expect(hostManager.getHostsByIp(TARGET_IPV6_ADDR))
                .andReturn(Collections.emptySet()).anyTimes();
        replay(hostManager);

        InterfaceService interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getMatchingInterface(TARGET_IPV6_ADDR))
                .andReturn(new Interface(Interface.NO_INTERFACE_NAME, cp,
                        Collections.singletonList(IA2), sourceMac2, VlanId.NONE))
                .anyTimes();
        replay(interfaceService);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(packetService, hostManager, interfaceService, edgePortService);

        hostMonitor.addMonitoringFor(TARGET_IPV6_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(2, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        // Check the output port is correct
        assertEquals(1, packet.treatment().immediate().size());
        Instruction instruction = packet.treatment().immediate().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        OutputInstruction oi = (OutputInstruction) instruction;
        assertEquals(portNum, oi.port());

        // Check the output packet is correct (well the important bits anyway)
        final byte[] pktData = new byte[packet.data().remaining()];
        packet.data().get(pktData);
        Ethernet eth = Ethernet.deserializer().deserialize(pktData, 0, pktData.length);
        assertEquals(Ethernet.VLAN_UNTAGGED, eth.getVlanID());
        IPv6 ipv6 = (IPv6) eth.getPayload();
        assertArrayEquals(SOURCE_IPV6_ADDR.toOctets(), ipv6.getSourceAddress());

        NeighborSolicitation ns =
                (NeighborSolicitation) ipv6.getPayload().getPayload();
        assertArrayEquals(sourceMac2.toBytes(), ns.getOptions().get(0).data());

        assertArrayEquals(TARGET_IPV6_ADDR.toOctets(), ns.getTargetAddress());
    }

    @Test
    public void testMonitorIpv4HostDoesNotExistWithVlan() throws Exception {

        HostManager hostManager = createMock(HostManager.class);

        DeviceId devId = DeviceId.deviceId("fake");
        short vlan = 5;

        Device device = createMock(Device.class);
        expect(device.id()).andReturn(devId).anyTimes();
        replay(device);

        PortNumber portNum = PortNumber.portNumber(1L);

        Port port = createMock(Port.class);
        expect(port.number()).andReturn(portNum).anyTimes();
        replay(port);

        TestDeviceService deviceService = new TestDeviceService();
        deviceService.addDevice(device, Collections.singleton(port));

        ConnectPoint cp = new ConnectPoint(devId, portNum);

        expect(hostManager.getHostsByIp(TARGET_IPV4_ADDR))
                .andReturn(Collections.emptySet()).anyTimes();
        replay(hostManager);

        InterfaceService interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getMatchingInterface(TARGET_IPV4_ADDR))
                .andReturn(new Interface(Interface.NO_INTERFACE_NAME, cp,
                        Collections.singletonList(IA1), sourceMac, VlanId.vlanId(vlan)))
                .anyTimes();
        replay(interfaceService);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(packetService, hostManager, interfaceService, edgePortService);

        hostMonitor.addMonitoringFor(TARGET_IPV4_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(2, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        // Check the output port is correct
        assertEquals(1, packet.treatment().immediate().size());
        Instruction instruction = packet.treatment().immediate().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        OutputInstruction oi = (OutputInstruction) instruction;
        assertEquals(portNum, oi.port());

        // Check the output packet is correct (well the important bits anyway)
        final byte[] pktData = new byte[packet.data().remaining()];
        packet.data().get(pktData);
        Ethernet eth = Ethernet.deserializer().deserialize(pktData, 0, pktData.length);
        assertEquals(vlan, eth.getVlanID());
        ARP arp = (ARP) eth.getPayload();
        assertArrayEquals(SOURCE_IPV4_ADDR.toOctets(),
                          arp.getSenderProtocolAddress());
        assertArrayEquals(sourceMac.toBytes(),
                          arp.getSenderHardwareAddress());
        assertArrayEquals(TARGET_IPV4_ADDR.toOctets(),
                          arp.getTargetProtocolAddress());
    }

    @Test
    public void testMonitorIpv6HostDoesNotExistWithVlan() throws Exception {

        HostManager hostManager = createMock(HostManager.class);

        DeviceId devId = DeviceId.deviceId("fake");
        short vlan = 5;

        Device device = createMock(Device.class);
        expect(device.id()).andReturn(devId).anyTimes();
        replay(device);

        PortNumber portNum = PortNumber.portNumber(1L);

        Port port = createMock(Port.class);
        expect(port.number()).andReturn(portNum).anyTimes();
        replay(port);

        TestDeviceService deviceService = new TestDeviceService();
        deviceService.addDevice(device, Collections.singleton(port));

        ConnectPoint cp = new ConnectPoint(devId, portNum);

        expect(hostManager.getHostsByIp(TARGET_IPV6_ADDR))
                .andReturn(Collections.emptySet()).anyTimes();
        replay(hostManager);

        InterfaceService interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getMatchingInterface(TARGET_IPV6_ADDR))
                .andReturn(new Interface(Interface.NO_INTERFACE_NAME, cp,
                        Collections.singletonList(IA2), sourceMac2, VlanId.vlanId(vlan)))
                .anyTimes();
        replay(interfaceService);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(packetService, hostManager, interfaceService, edgePortService);

        hostMonitor.addMonitoringFor(TARGET_IPV6_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(2, packetService.packets.size());
        OutboundPacket packet = packetService.packets.get(0);

        // Check the output port is correct
        assertEquals(1, packet.treatment().immediate().size());
        Instruction instruction = packet.treatment().immediate().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        OutputInstruction oi = (OutputInstruction) instruction;
        assertEquals(portNum, oi.port());

        // Check the output packet is correct (well the important bits anyway)
        final byte[] pktData = new byte[packet.data().remaining()];
        packet.data().get(pktData);
        Ethernet eth = Ethernet.deserializer().deserialize(pktData, 0, pktData.length);
        assertEquals(vlan, eth.getVlanID());
        IPv6 ipv6 = (IPv6) eth.getPayload();
        assertArrayEquals(SOURCE_IPV6_ADDR.toOctets(), ipv6.getSourceAddress());

        NeighborSolicitation ns =
                (NeighborSolicitation) ipv6.getPayload().getPayload();
        assertArrayEquals(sourceMac2.toBytes(), ns.getOptions().get(0).data());

        assertArrayEquals(TARGET_IPV6_ADDR.toOctets(), ns.getTargetAddress());
    }

    class TestPacketService extends PacketServiceAdapter {

        List<OutboundPacket> packets = new ArrayList<>();

        @Override
        public void emit(OutboundPacket packet) {
            packets.add(packet);
        }
    }

    class TestDeviceService extends DeviceServiceAdapter {

        List<Device> devices = Lists.newArrayList();
        Multimap<DeviceId, Port> devicePorts = HashMultimap.create();

        void addDevice(Device device, Set<Port> ports) {
            devices.add(device);
            for (Port p : ports) {
                devicePorts.put(device.id(), p);
            }
        }

        @Override
        public Iterable<Device> getDevices() {
            return devices;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            List<Port> ports = Lists.newArrayList();
            for (Port p : devicePorts.get(deviceId)) {
                ports.add(p);
            }
            return ports;
        }
    }
}
