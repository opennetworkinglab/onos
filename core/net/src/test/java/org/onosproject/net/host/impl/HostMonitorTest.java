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
package org.onosproject.net.host.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.After;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HostMonitorTest {

    private static final IpAddress TARGET_IP_ADDR =
            IpAddress.valueOf("10.0.0.1");
    private static final IpAddress SOURCE_ADDR =
            IpAddress.valueOf("10.0.0.99");
    private static final InterfaceIpAddress IA1 =
            new InterfaceIpAddress(SOURCE_ADDR, IpPrefix.valueOf("10.0.0.0/24"));
    private MacAddress sourceMac = MacAddress.valueOf(1L);

    private HostMonitor hostMonitor;

    @After
    public void shutdown() {
        hostMonitor.shutdown();
    }

    @Test
    public void testMonitorHostExists() throws Exception {
        ProviderId id = new ProviderId("fake://", "id");

        Host host = createMock(Host.class);
        expect(host.providerId()).andReturn(id);
        replay(host);

        HostManager hostManager = createMock(HostManager.class);
        expect(hostManager.getHostsByIp(TARGET_IP_ADDR))
                .andReturn(Collections.singleton(host));
        replay(hostManager);

        HostProvider hostProvider = createMock(HostProvider.class);
        expect(hostProvider.id()).andReturn(id).anyTimes();
        hostProvider.triggerProbe(host);
        expectLastCall().once();
        replay(hostProvider);

        hostMonitor = new HostMonitor(null, null, hostManager);

        hostMonitor.registerHostProvider(hostProvider);
        hostMonitor.addMonitoringFor(TARGET_IP_ADDR);

        hostMonitor.run(null);

        verify(hostProvider);
    }

    @Test
    public void testMonitorHostDoesNotExist() throws Exception {

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
        PortAddresses pa =
                new PortAddresses(cp, Collections.singleton(IA1), sourceMac, VlanId.NONE);

        expect(hostManager.getHostsByIp(TARGET_IP_ADDR))
                .andReturn(Collections.<Host>emptySet()).anyTimes();
        expect(hostManager.getAddressBindingsForPort(cp))
                .andReturn(Collections.singleton(pa)).anyTimes();
        replay(hostManager);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(deviceService, packetService, hostManager);

        hostMonitor.addMonitoringFor(TARGET_IP_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(1, packetService.packets.size());
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
        assertArrayEquals(SOURCE_ADDR.toOctets(),
                          arp.getSenderProtocolAddress());
        assertArrayEquals(sourceMac.toBytes(),
                          arp.getSenderHardwareAddress());
        assertArrayEquals(TARGET_IP_ADDR.toOctets(),
                          arp.getTargetProtocolAddress());
    }

    @Test
    public void testMonitorHostDoesNotExistWithVlan() throws Exception {

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
        PortAddresses pa =
                new PortAddresses(cp, Collections.singleton(IA1), sourceMac,
                                  VlanId.vlanId(vlan));

        expect(hostManager.getHostsByIp(TARGET_IP_ADDR))
                .andReturn(Collections.<Host>emptySet()).anyTimes();
        expect(hostManager.getAddressBindingsForPort(cp))
                .andReturn(Collections.singleton(pa)).anyTimes();
        replay(hostManager);

        TestPacketService packetService = new TestPacketService();


        // Run the test
        hostMonitor = new HostMonitor(deviceService, packetService, hostManager);

        hostMonitor.addMonitoringFor(TARGET_IP_ADDR);
        hostMonitor.run(null);


        // Check that a packet was sent to our PacketService and that it has
        // the properties we expect
        assertEquals(1, packetService.packets.size());
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
        assertArrayEquals(SOURCE_ADDR.toOctets(),
                          arp.getSenderProtocolAddress());
        assertArrayEquals(sourceMac.toBytes(),
                          arp.getSenderHardwareAddress());
        assertArrayEquals(TARGET_IP_ADDR.toOctets(),
                          arp.getTargetProtocolAddress());
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
        public int getDeviceCount() {
            return 0;
        }

        @Override
        public Iterable<Device> getDevices() {
            return devices;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return null;
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            List<Port> ports = Lists.newArrayList();
            for (Port p : devicePorts.get(deviceId)) {
                ports.add(p);
            }
            return ports;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void addListener(DeviceListener listener) {
        }

        @Override
        public void removeListener(DeviceListener listener) {
        }
    }
}
