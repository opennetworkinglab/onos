/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.host.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.InterfaceIpAddress;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

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
            new PortAddresses(cp, Collections.singleton(IA1), sourceMac);

        expect(hostManager.getHostsByIp(TARGET_IP_ADDR))
                .andReturn(Collections.<Host>emptySet()).anyTimes();
        expect(hostManager.getAddressBindingsForPort(cp))
                .andReturn(pa).anyTimes();
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
        assertEquals(1, packet.treatment().instructions().size());
        Instruction instruction = packet.treatment().instructions().get(0);
        assertTrue(instruction instanceof OutputInstruction);
        OutputInstruction oi = (OutputInstruction) instruction;
        assertTrue(oi.port().equals(portNum));

        // Check the output packet is correct (well the important bits anyway)
        Ethernet eth = new Ethernet();
        eth.deserialize(packet.data().array(), 0, packet.data().array().length);
        ARP arp = (ARP) eth.getPayload();
        assertTrue(Arrays.equals(arp.getSenderProtocolAddress(),
                                 SOURCE_ADDR.toOctets()));
        assertTrue(Arrays.equals(arp.getSenderHardwareAddress(), sourceMac.toBytes()));
        assertTrue(Arrays.equals(arp.getTargetProtocolAddress(),
                                 TARGET_IP_ADDR.toOctets()));
    }

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

    class TestDeviceService implements DeviceService {

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
