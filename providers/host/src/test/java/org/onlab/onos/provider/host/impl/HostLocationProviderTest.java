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
package org.onlab.onos.provider.host.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.packet.DefaultInboundPacket;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.Topology;

import org.onlab.onos.net.topology.TopologyServiceAdapter;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

public class HostLocationProviderTest {

    private static final Integer INPORT = 10;
    private static final String DEV1 = "of:1";
    private static final String DEV2 = "of:2";
    private static final String DEV3 = "of:3";

    private static final VlanId VLAN = VlanId.vlanId();
    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress BCMAC = MacAddress.valueOf("ff:ff:ff:ff:ff:ff");
    private static final byte[] IP = new byte[]{10, 0, 0, 1};

    private final HostLocationProvider provider = new HostLocationProvider();
    private final TestHostRegistry hostService = new TestHostRegistry();
    private final TestTopologyService topoService = new TestTopologyService();
    private final TestPacketService packetService = new TestPacketService();

    private PacketProcessor testProcessor;
    private TestHostProviderService providerService;

    @Before
    public void setUp() {
        provider.providerRegistry = hostService;
        provider.topologyService = topoService;
        provider.pktService = packetService;

        provider.activate();

    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void events() {
        // new host


        testProcessor.process(new TestPacketContext(DEV1));
        assertNotNull("new host expected", providerService.added);
        assertNull("host motion unexpected", providerService.moved);

        // the host moved to new switch
        testProcessor.process(new TestPacketContext(DEV2));
        assertNotNull("host motion expected", providerService.moved);

        // the host was misheard on a spine
        testProcessor.process(new TestPacketContext(DEV3));
        assertNull("host misheard on spine switch", providerService.spine);
    }

    @After
    public void tearDown() {
        provider.deactivate();
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

        DeviceId added = null;
        DeviceId moved = null;
        DeviceId spine = null;

        protected TestHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription) {
            DeviceId descr = hostDescription.location().deviceId();
            if (added == null) {
                added = descr;
            } else if ((moved == null) && !descr.equals(added)) {
                moved = descr;
            } else {
                spine = descr;
            }
        }

        @Override
        public void hostVanished(HostId hostId) {
        }

    }

    private class TestPacketService implements PacketService {

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            testProcessor = processor;
        }

        @Override
        public void removeProcessor(PacketProcessor processor) {

        }

        @Override
        public void emit(OutboundPacket packet) {

        }
    }


    private class TestTopologyService extends TopologyServiceAdapter {
        @Override
        public boolean isInfrastructure(Topology topology,
                                        ConnectPoint connectPoint) {
            //simulate DPID3 as an infrastructure switch
            if ((connectPoint.deviceId()).equals(DeviceId.deviceId(DEV3))) {
                return true;
            }
            return false;
        }
    }

    private class TestPacketContext implements PacketContext {

        private final String deviceId;

        public TestPacketContext(String deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public long time() {
            return 0;
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
            ConnectPoint receivedFrom = new ConnectPoint(DeviceId.deviceId(deviceId),
                                                         PortNumber.portNumber(INPORT));
            return new DefaultInboundPacket(receivedFrom, eth,
                                            ByteBuffer.wrap(eth.serialize()));
        }

        @Override
        public OutboundPacket outPacket() {
            return null;
        }

        @Override
        public TrafficTreatment.Builder treatmentBuilder() {
            return null;
        }

        @Override
        public void send() {

        }

        @Override
        public boolean block() {
            return false;
        }

        @Override
        public boolean isHandled() {
            return false;
        }
    }
}
