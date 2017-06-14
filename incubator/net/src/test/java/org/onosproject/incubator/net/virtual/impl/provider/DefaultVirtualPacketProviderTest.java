/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl.provider;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.virtual.DefaultVirtualDevice;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.DefaultVirtualPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminServiceAdapter;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProviderService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DefaultVirtualPacketProviderTest {
    private static final String SRC_MAC_ADDR = "00:00:00:00:00:00";
    private static final String DST_MAC_ADDR = "00:00:00:00:00:01";
    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final DeviceId DID1 = DeviceId.deviceId("of:001");
    private static final DeviceId DID2 = DeviceId.deviceId("of:002");
    private static final PortNumber PORT_NUM1 = PortNumber.portNumber(1);
    private static final PortNumber PORT_NUM2 = PortNumber.portNumber(2);
    private static final PortNumber PORT_NUM3 = PortNumber.portNumber(3);
    private static final PortNumber PORT_NUM4 = PortNumber.portNumber(4);

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set("foo", "bar").build();

    private static final Device DEV1 =
            new DefaultDevice(PID, DID1, Device.Type.SWITCH, "", "", "", "", null);
    private static final Device DEV2 =
            new DefaultDevice(PID, DID2, Device.Type.SWITCH, "", "", "", "", null);
    private static final Port PORT11 =
            new DefaultPort(DEV1, PORT_NUM1, true, ANNOTATIONS);
    private static final Port PORT12 =
            new DefaultPort(DEV1, PORT_NUM2, true, ANNOTATIONS);
    private static final Port PORT21 =
            new DefaultPort(DEV2, PORT_NUM3, true, ANNOTATIONS);
    private static final Port PORT22 =
            new DefaultPort(DEV2, PORT_NUM4, true, ANNOTATIONS);

    private static final ConnectPoint CP11 = new ConnectPoint(DID1, PORT_NUM1);
    private static final ConnectPoint CP12 = new ConnectPoint(DID1, PORT_NUM2);
    private static final ConnectPoint CP21 = new ConnectPoint(DID2, PORT_NUM3);
    private static final ConnectPoint CP22 = new ConnectPoint(DID2, PORT_NUM4);
    private static final Link LINK1 = DefaultLink.builder()
            .src(CP12).dst(CP21).providerId(PID).type(Link.Type.DIRECT).build();

    private static final TenantId TENANT_ID = TenantId.tenantId("1");
    private static final NetworkId VNET_ID = NetworkId.networkId(1);
    private static final DeviceId VDID = DeviceId.deviceId("of:100");

    private static final PortNumber VPORT_NUM1 = PortNumber.portNumber(10);
    private static final PortNumber VPORT_NUM2 = PortNumber.portNumber(11);

    private static final VirtualNetwork VNET = new DefaultVirtualNetwork(
            VNET_ID, TenantId.tenantId("t1"));
    private static final VirtualDevice VDEV =
            new DefaultVirtualDevice(VNET_ID, VDID);
    private static final VirtualPort VPORT1 =
            new DefaultVirtualPort(VNET_ID, VDEV, VPORT_NUM1, CP11);
    private static final VirtualPort VPORT2 =
            new DefaultVirtualPort(VNET_ID, VDEV, VPORT_NUM2, CP22);
    private static final ConnectPoint VCP11 = new ConnectPoint(VDID, VPORT_NUM1);
    private static final ConnectPoint VCP12 = new ConnectPoint(VDID, VPORT_NUM2);

    protected DefaultVirtualPacketProvider virtualProvider;
    protected TestPacketService testPacketService;
    protected TestVirtualPacketProviderService providerService;

    private VirtualProviderManager providerManager;

    private ApplicationId vAppId;

    @Before
    public void setUp() {
        virtualProvider = new DefaultVirtualPacketProvider();

        virtualProvider.coreService = new CoreServiceAdapter();
        virtualProvider.vnaService =
                new TestVirtualNetworkAdminService();

        providerService = new TestVirtualPacketProviderService();

        testPacketService = new TestPacketService();
        virtualProvider.packetService = testPacketService;

        providerManager = new VirtualProviderManager();
        virtualProvider.providerRegistryService = providerManager;
        providerManager.registerProviderService(VNET_ID, providerService);

        virtualProvider.activate();
        vAppId = new TestApplicationId(0, "Virtual App");

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);

        virtualProvider.startPacketHandling();
    }

    @After
    public void tearDown() {
        virtualProvider.deactivate();
        virtualProvider.coreService = null;
        virtualProvider.vnaService = null;
    }


    /** Test the virtual outbound packet is delivered to a proper (physical)
     *  device.
     */
    @Test
    public void devirtualizePacket() {
        TrafficTreatment tr = DefaultTrafficTreatment.builder()
                .setOutput(VPORT_NUM1).build();
        ByteBuffer data = ByteBuffer.wrap("abc".getBytes());

        OutboundPacket vOutPacket = new DefaultOutboundPacket(VDID, tr, data);

        virtualProvider.emit(VNET_ID, vOutPacket);

        assertEquals("The count should be 1", 1,
                     testPacketService.getRequestedPacketCount());

        OutboundPacket pOutPacket = testPacketService.getRequestedPacket(0);

        assertEquals("The packet should be requested on DEV1", DID1,
                     pOutPacket.sendThrough());

        PortNumber outPort = pOutPacket.treatment()
                .allInstructions()
                .stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .map(i -> (Instructions.OutputInstruction) i)
                .map(i -> i.port())
                .findFirst().get();
        assertEquals("The packet should be out at PORT1 of DEV1", PORT_NUM1,
                     outPort);
    }

    /** Test the physical packet context is delivered to a proper (physical)
     *  virtual network and device.
     */
    @Test
    public void virtualizePacket() {
        Ethernet eth = new Ethernet();
        eth.setSourceMACAddress(SRC_MAC_ADDR);
        eth.setDestinationMACAddress(DST_MAC_ADDR);
        eth.setVlanID((short) 1);
        eth.setPayload(null);

        InboundPacket pInPacket =
                new DefaultInboundPacket(CP22, eth,
                                         ByteBuffer.wrap(eth.serialize()));

        PacketContext pContext =
                new TestPacketContext(System.nanoTime(), pInPacket, null, false);

        testPacketService.sendTestPacketContext(pContext);

        PacketContext vContext = providerService.getRequestedPacketContext(0);
        InboundPacket vInPacket = vContext.inPacket();

        assertEquals("the packet should be received from VCP12",
                     VCP12, vInPacket.receivedFrom());

        assertEquals("VLAN tag should be excludede", VlanId.UNTAGGED,
                     vInPacket.parsed().getVlanID());
    }

    private class TestPacketContext extends DefaultPacketContext {

        /**
         * Creates a new packet context.
         *
         * @param time   creation time
         * @param inPkt  inbound packet
         * @param outPkt outbound packet
         * @param block  whether the context is blocked or not
         */
        protected TestPacketContext(long time, InboundPacket inPkt,
                                    OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {

        }
    }

    private static class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

    private static class TestVirtualNetworkAdminService
            extends VirtualNetworkAdminServiceAdapter {

        @Override
        public Set<VirtualNetwork> getVirtualNetworks(TenantId tenantId) {
            return ImmutableSet.of(VNET);
        }

        @Override
        public Set<VirtualDevice> getVirtualDevices(NetworkId networkId) {
            return ImmutableSet.of(VDEV);
        }

        @Override
        public Set<VirtualPort> getVirtualPorts(NetworkId networkId,
                                                DeviceId deviceId) {
            return ImmutableSet.of(VPORT1, VPORT2);
        }

        @Override
        public Set<TenantId> getTenantIds() {
            return ImmutableSet.of(TENANT_ID);
        }

    }

    private static class TestVirtualPacketProviderService
            extends AbstractVirtualProviderService<VirtualPacketProvider>
            implements VirtualPacketProviderService {

        static List<PacketContext> requestedContext = new LinkedList();
        static List<NetworkId> requestedNetworkId = new LinkedList();

        @Override
        public VirtualPacketProvider provider() {
            return null;
        }

        PacketContext getRequestedPacketContext(int index) {
            return requestedContext.get(index);
        }

        @Override
        public void processPacket(PacketContext context) {
            requestedContext.add(context);
        }
    }

    private static class TestPacketService extends PacketServiceAdapter {
        static List<OutboundPacket> requestedPacket = new LinkedList();
        static PacketProcessor processor = null;

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            this.processor = processor;
        }

        @Override
        public void emit(OutboundPacket packet) {
            requestedPacket.add(packet);
        }

        OutboundPacket getRequestedPacket(int index) {
            return requestedPacket.get(index);
        }

        int getRequestedPacketCount() {
            return requestedPacket.size();
        }

        void sendTestPacketContext(PacketContext context) {
            processor.process(context);
        }
    }
}
