/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.rabbitmq.listener;

import static com.google.common.collect.ImmutableSet.of;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.link;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.topology.TopologyEvent.Type.TOPOLOGY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.link.LinkEvent.Type.*;
import static org.onosproject.net.Device.Type.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.ONOSLLDP;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyProviderRegistry;
import org.onosproject.net.topology.TopologyProviderService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rabbitmq.api.MQService;
import org.onosproject.rabbitmq.api.Manageable;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
/**
 * Junit tests for packet in, device, topology and link events.
 */
@RunWith(EasyMockRunner.class)
public class MQEventHandlerTest extends AbstractEventTest {

    private final MQEventHandler mqEventHandler = new MQEventHandler();
    private static final DeviceId DID1 = deviceId("of:0000000000000001");
    private static final DeviceId DID2 = deviceId("of:0000000000000002");
    private static final DeviceId DID3 = deviceId("of:0000000000000003");

    private ApplicationId appId = new DefaultApplicationId(100,
                                      "org.onosproject.rabbitmq");
    private final TestPacketService packetService = new TestPacketService();
    private final TestDeviceService deviceService = new TestDeviceService();
    private PacketProcessor testProcessor;
    private DeviceListener deviceListener;
    private static Port pd1;
    private static Port pd2;
    private static Port pd3;
    private static Port pd4;
    private CoreService coreService;
    @Mock
    ComponentContext context;
    @Mock
    private Manageable manageSender;

    private static final ProviderId PID = new ProviderId("of", "foo");
    private TestLinkListener testLinkListener = new TestLinkListener();
    @Mock
    private LinkService linkService;
    @Mock
    Topology topology;
    @Mock
    protected TopologyService service;
    protected TopologyProviderRegistry registry;
    @Mock
    protected TopologyProviderService providerService;
    protected TestProvider provider;
    protected TestTopologyListener listener = new TestTopologyListener();
    @Mock
    MQService mqService;

    @Before
    public void setUp() {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(appId.name()))
                          .andReturn(appId).anyTimes();
        replay(coreService);
        mqEventHandler.deviceService = deviceService;
        mqEventHandler.packetService = packetService;
        mqEventHandler.eventExecutor = MoreExecutors.newDirectExecutorService();
        linkService.addListener(testLinkListener);
        mqEventHandler.linkService = linkService;
        mqEventHandler.topologyService = service;
        mqEventHandler.activate(context);
    }

    @After
    public void tearDown() {
        mqEventHandler.deactivate();
        mqEventHandler.deviceService = null;
        mqEventHandler.packetService = null;
    }

    private DeviceEvent deviceEvent(DeviceEvent.Type type, DeviceId did) {
        return new DeviceEvent(type, deviceService.getDevice(did));

    }

    private Port port(DeviceId did, long port, boolean enabled) {
        return new DefaultPort(deviceService.getDevice(did),
                               portNumber(port), enabled);
    }

    private DeviceEvent portEvent(DeviceEvent.Type type, DeviceId did,
                                  Port port) {
        return new DeviceEvent(type, deviceService.getDevice(did), port);
    }

    @Test
    public void switchAdd() {
        DeviceEvent de = deviceEvent(DEVICE_ADDED, DID1);
        deviceListener.event(de);

    }

    @Test
    public void switchRemove() {
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID1));
        deviceListener.event(deviceEvent(DEVICE_REMOVED, DID1));
    }

    @Test
    public void switchUpdate() {
        deviceListener.event(deviceEvent(DEVICE_UPDATED, DID1));
        deviceListener.event(deviceEvent(DEVICE_REMOVED, DID1));
    }

    @Test
    public void switchSuspend() {
        deviceListener.event(deviceEvent(DEVICE_SUSPENDED, DID1));
        deviceListener.event(deviceEvent(DEVICE_REMOVED, DID1));
    }

    @Test
    public void portUp() {
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(PORT_ADDED, DID1, port(DID1, 3, true)));
    }

    @Test
    public void portDown() {
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(PORT_ADDED, DID1, port(DID1, 1, false)));
    }

    @Test
    public void portRemoved() {
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(PORT_ADDED, DID1, port(DID1, 3, true)));
        deviceListener.event(portEvent(PORT_REMOVED, DID1,
                                       port(DID1, 3, true)));
    }

    @Test
    public void unknownPktCtx() {
        // Note: DID3 hasn't been added to TestDeviceService
        PacketContext pktCtx = new TestPacketContext(device1(DID3));
        testProcessor.process(pktCtx);
        assertFalse("Context should still be free", pktCtx.isHandled());
    }

    private DefaultDevice device1(DeviceId did) {
        return new DefaultDevice(ProviderId.NONE, did, SWITCH,
                                 "TESTMF", "TESTHW", "TESTSW", "TESTSN",
                                 new ChassisId());
    }

    @Test
    public void knownPktCtx() {
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID1));
        deviceListener.event(deviceEvent(DEVICE_ADDED, DID2));
        PacketContext pktCtx = new TestPacketContext(
                                       deviceService.getDevice(DID2));
        /*
         * EasyMock.expectLastCall(); EasyMock.replay(manageSender);
         */
        testProcessor.process(pktCtx);
    }

    private class TestDeviceService extends DeviceServiceAdapter {

        private final Map<DeviceId, Device> devices = new HashMap<>();
        private final ArrayListMultimap<DeviceId, Port> ports =
                                                  ArrayListMultimap.create();

        public TestDeviceService() {
            Device d1 = new DefaultDevice(ProviderId.NONE, DID1,
                                          SWITCH, "TESTMF", "TESTHW",
                                          "TESTSW", "TESTSN", new ChassisId());
            Device d2 = new DefaultDevice(ProviderId.NONE, DID2, SWITCH,
                                          "TESTMF", "TESTHW", "TESTSW",
                                          "TESTSN", new ChassisId());
            devices.put(DID1, d1);
            devices.put(DID2, d2);
            pd1 = new DefaultPort(d1, portNumber(1), true);
            pd2 = new DefaultPort(d1, portNumber(2), true);
            pd3 = new DefaultPort(d2, portNumber(1), true);
            pd4 = new DefaultPort(d2, portNumber(2), true);
            ports.putAll(DID1, Lists.newArrayList(pd1, pd2));
            ports.putAll(DID2, Lists.newArrayList(pd3, pd4));
        }

        @Override
        public int getDeviceCount() {
            return devices.values().size();
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.copyOf(devices.values());
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }

        @Override
        public void addListener(DeviceListener listener) {
            deviceListener = listener;

        }

        @Override
        public void removeListener(DeviceListener listener) {

        }
    }

    private class TestPacketService extends PacketServiceAdapter {
        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            testProcessor = processor;
        }
    }

    private class TestPacketContext implements PacketContext {

        protected Device device;
        protected boolean blocked = false;

        public TestPacketContext(Device dev) {
            device = dev;
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public InboundPacket inPacket() {
            ONOSLLDP lldp = ONOSLLDP.onosSecureLLDP(deviceService.getDevice(DID1)
                                              .id().toString(),
                                                    device.chassisId(),
                                                    (int) pd1.number().toLong(), "", "test");

            Ethernet ethPacket = new Ethernet();
            ethPacket.setEtherType(Ethernet.TYPE_LLDP);
            ethPacket.setDestinationMACAddress(MacAddress.ONOS_LLDP);
            ethPacket.setPayload(lldp);
            ethPacket.setPad(true);

            ethPacket.setSourceMACAddress("DE:AD:BE:EF:BA:11");

            ConnectPoint cp = new ConnectPoint(device.id(), pd3.number());

            return new DefaultInboundPacket(cp, ethPacket,
                                            ByteBuffer.wrap(ethPacket
                                            .serialize()));

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
            blocked = true;
            return blocked;
        }

        @Override
        public boolean isHandled() {
            return blocked;
        }
    }

    private void submitTopologyGraph() {
        Set<Device> devices = of(device("a"), device("b"), device("c"),
                                 device("d"), device("e"), device("f"));
        Set<Link> links = of(link("a", 1, "b", 1), link("b", 1, "a", 1),
                             link("b", 2, "c", 1), link("c", 1, "b", 2),
                             link("c", 2, "d", 1), link("d", 1, "c", 2),
                             link("d", 2, "a", 2), link("a", 2, "d", 2),
                             link("e", 1, "f", 1), link("f", 1, "e", 1));
        GraphDescription data = new DefaultGraphDescription(4321L,
                                     System.currentTimeMillis(),
                                     devices, links);
        providerService.topologyChanged(data, null);
    }

    protected void validateEvents(Enum... types) {
        int i = 0;
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }

    @Test
    public void testCreateTopology() {
        submitTopologyGraph();
        validateEvents(TOPOLOGY_CHANGED);
    }

    private class TestProvider extends AbstractProvider
                               implements TopologyProvider {
        public TestProvider() {
            super(PID);
        }

        @Override
        public void triggerRecompute() {
        }
    }

    private class TestTopologyListener implements TopologyListener {
        final List<TopologyEvent> events = new ArrayList<>();

        @Override
        public void event(TopologyEvent event) {
            mqService.publish(event);
        }
    }

    private Link createLink() {
        return DefaultLink.builder().providerId(new ProviderId("of", "foo"))
                .src(new ConnectPoint(deviceId("of:foo"), portNumber(1)))
                .dst(new ConnectPoint(deviceId("of:bar"), portNumber(2)))
                .type(Link.Type.INDIRECT).build();
    }

    @Test
    public void testAddLink() throws Exception {
        Link link = createLink();
        LinkEvent event = new LinkEvent(LINK_ADDED, link, 123L);
        validateEvent(event, LINK_ADDED, link, 123L);
    }

    @Test
    public void testUpdateLink() throws Exception {
        Link link = createLink();
        LinkEvent event = new LinkEvent(LINK_UPDATED, link, 123L);
        validateEvent(event, LINK_UPDATED, link, 123L);
    }

    @Test
    public void testRemoveLink() throws Exception {
        Link link = createLink();
        LinkEvent event = new LinkEvent(LINK_ADDED, link, 123L);
        validateEvent(event, LINK_ADDED, link, 123L);
        LinkEvent event1 = new LinkEvent(LINK_REMOVED, link, 123L);
        validateEvent(event1, LINK_REMOVED, link, 123L);
    }

    private class TestLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            mqService.publish(event);
        }

    }

}
