/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.edgeservice.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyEvent.Type;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_ADDED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;

/**
 * Test of the edge port manager. Each device has ports '0' through 'numPorts - 1'
 * as specified by the variable 'numPorts'.
 */
public class EdgeManagerTest {

    private EdgeManager mgr;
    private int totalPorts = 10;
    private boolean alwaysReturnPorts = false;
    private final Set<ConnectPoint> infrastructurePorts = Sets.newConcurrentHashSet();
    private List<EdgePortEvent> events = Lists.newArrayList();
    private final Map<DeviceId, Device> devices = Maps.newConcurrentMap();
    private Set<OutboundPacket> packets = Sets.newConcurrentHashSet();
    private final EdgePortListener testListener = new TestListener(events);
    private TestDeviceManager testDeviceManager;
    private TestLinkService testLinkService;

    @Before
    public void setUp() {
        mgr = new EdgeManager();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        testDeviceManager = new TestDeviceManager(devices);
        mgr.deviceService = testDeviceManager;

        testLinkService = new TestLinkService();
        mgr.linkService = testLinkService;

        mgr.packetService = new TestPacketManager();
        mgr.activate();
        mgr.addListener(testListener);
    }

    @After
    public void tearDown() {
        mgr.removeListener(testListener);
        mgr.deactivate();
    }

    @Test
    public void testBasics() {
        //Setup
        int numDevices = 20;
        int numPorts = 4;
        defaultPopulator(numDevices, numPorts);

        assertEquals("Unexpected number of ports", numDevices * numPorts, infrastructurePorts.size());

        assertFalse("Expected isEdge to return false",
                    mgr.isEdgePoint(NetTestTools.connectPoint(Integer.toString(1), 1)));

        removeInfraPort(NetTestTools.connectPoint(Integer.toString(1), 1));
        postTopologyEvent(new LinkEvent(LINK_REMOVED, NetTestTools.link(Integer.toString(1), 1, "b", 2)));
        assertTrue("Expected isEdge to return true",
                   mgr.isEdgePoint(NetTestTools.connectPoint(Integer.toString(1), 1)));
    }

    @Test
    public void testLinkUpdates() {
        //Setup
        ConnectPoint testPoint, referencePoint;

        //Testing link removal
        postTopologyEvent(new LinkEvent(LINK_REMOVED, NetTestTools.link("a", 1, "b", 2)));

        assertTrue("The list contained an unexpected number of events", events.size() == 2);
        assertTrue("The first element is of the wrong type.",
                   events.get(0).type() == EDGE_PORT_ADDED);

        testPoint = events.get(0).subject();
        referencePoint = NetTestTools.connectPoint("a", 1);
        assertTrue("The port numbers of the first element are incorrect",
                   testPoint.port().toLong() == referencePoint.port().toLong());
        assertTrue("The device id of the first element is incorrect.",
                   testPoint.deviceId().equals(referencePoint.deviceId()));

        testPoint = events.get(1).subject();
        referencePoint = NetTestTools.connectPoint("b", 2);
        assertTrue("The port numbers of the second element are incorrect",
                   testPoint.port().toLong() == referencePoint.port().toLong());
        assertTrue("The device id of the second element is incorrect.",
                   testPoint.deviceId().equals(referencePoint.deviceId()));

        //Rebroadcast event to ensure it results in no additional events
        postTopologyEvent(new LinkEvent(LINK_REMOVED, NetTestTools.link("a", 1, "b", 2)));
        assertTrue("The list contained an unexpected number of events", events.size() == 2);

        //Testing link adding when links to remove exist
        events.clear();
        postTopologyEvent(new LinkEvent(LINK_ADDED, NetTestTools.link("a", 1, "b", 2)));

        assertTrue("The list contained an unexpected number of events", events.size() == 2);
        assertTrue("The first element is of the wrong type.",
                   events.get(0).type() == EDGE_PORT_REMOVED);
        assertTrue("The second element is of the wrong type.",
                   events.get(1).type() == EDGE_PORT_REMOVED);

        testPoint = events.get(0).subject();
        referencePoint = NetTestTools.connectPoint("a", 1);
        assertTrue("The port numbers of the first element are incorrect",
                   testPoint.port().toLong() == referencePoint.port().toLong());
        assertTrue("The device id of the first element is incorrect.",
                   testPoint.deviceId().equals(referencePoint.deviceId()));

        testPoint = events.get(1).subject();
        referencePoint = NetTestTools.connectPoint("b", 2);
        assertTrue("The port numbers of the second element are incorrect",
                   testPoint.port().toLong() == referencePoint.port().toLong());
        assertTrue("The device id of the second element is incorrect.",
                   testPoint.deviceId().equals(referencePoint.deviceId()));

        //Apparent duplicate of previous method tests removal when the elements have already been removed
        events.clear();
        postTopologyEvent(new LinkEvent(LINK_ADDED, NetTestTools.link("a", 1, "b", 2)));
        assertTrue("The list should contain no events, the removed elements don't exist.", events.size() == 0);
    }


    @Test
    public void testDeviceUpdates() {
        //Setup

        Device referenceDevice;
        DeviceEvent event;
        int numDevices = 10;
        int numInfraPorts = 5;
        totalPorts = 10;

        defaultPopulator(numDevices, numInfraPorts);

        events.clear();

        //Test response to device added events

        referenceDevice = NetTestTools.device("11");
        devices.put(referenceDevice.id(), referenceDevice);
        for (int port = 1; port <= numInfraPorts; port++) {
            infrastructurePorts.add(NetTestTools.connectPoint("11", port));
        }
        event = new DeviceEvent(DEVICE_ADDED, referenceDevice,
                                new DefaultPort(referenceDevice, PortNumber.portNumber(1), true));
        postTopologyEvent(event);

        //Check that ports were populated correctly
        assertTrue("Unexpected number of new ports added",
                   mgr.deviceService.getPorts(NetTestTools.did("11")).size() == 10);

        //Check that of the ten ports the half that are infrastructure ports aren't added
        assertEquals("Unexpected number of new edge ports added", (totalPorts - numInfraPorts), events.size());

        for (int index = 0; index < numInfraPorts; index++) {
            assertTrue("Unexpected type of event", events.get(index).type() == EDGE_PORT_ADDED);
        }
        //Names here are irrelevant, the first 5 ports are populated as infrastructure, 6-10 are edge
        for (int index = 0; index < events.size(); index++) {
            assertEquals("Port added had unexpected port number.",
                         events.get(index).subject().port(),
                         NetTestTools.connectPoint("a", index + numInfraPorts + 1).port());
        }
        events.clear();

        //Repost the event to test repeated posts
        postTopologyEvent(event);
        assertEquals("The redundant notification should not have created additional notifications.",
                     0, events.size());
        //Calculate the size of the returned iterable of edge points.
        Iterable<ConnectPoint> pts = mgr.getEdgePoints();
        Iterator pointIterator = pts.iterator();
        int count = 0;
        for (; pointIterator.hasNext(); count++) {
            pointIterator.next();
        }
        assertEquals("Unexpected number of edge points", (numDevices + 1) * numInfraPorts, count);
        //Testing device removal
        events.clear();
        event = (new DeviceEvent(DEVICE_REMOVED, referenceDevice,
                                 new DefaultPort(referenceDevice, PortNumber.portNumber(1), true)));
        postTopologyEvent(event);

        assertEquals("There should be five new events from removal of edge points",
                     totalPorts - numInfraPorts, events.size());
        for (int index = 0; index < events.size(); index++) {
            //Assert that the correct port numbers were removed in the correct order
            assertThat("Port removed had unexpected port number.",
                       events.get(index).subject().port().toLong(),
                       is(greaterThanOrEqualTo((long) numInfraPorts)));
            //Assert that the events are of the correct type
            assertEquals("Unexpected type of event", events.get(index).type(), EDGE_PORT_REMOVED);
        }
        events.clear();
        //Rebroadcast event to check that it triggers no new behavior
        postTopologyEvent(event);
        assertEquals("Rebroadcast of removal event should not produce additional events",
                     0, events.size());

        //Testing device status change, changed from unavailable to available
        events.clear();
        //Make sure that the devicemanager shows the device as available.
        addDevice(referenceDevice, "1", 5);
        devices.put(referenceDevice.id(), referenceDevice);

        event = new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, referenceDevice);
        postTopologyEvent(event);
        //An earlier setup set half of the reference device ports to infrastructure
        assertEquals("An unexpected number of events were generated.", totalPorts - numInfraPorts, events.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("The event was not of the right type", events.get(i).type(), EDGE_PORT_ADDED);
        }
        events.clear();
        postTopologyEvent(event);
        assertEquals("No events should have been generated for a set of existing ports.", 0, events.size());

        //Test removal when state changes when the device becomes unavailable

        //Ensure that the deviceManager shows the device as unavailable
        removeDevice(referenceDevice);
        // This variable copies the behavior of the topology by returning ports
        // attached to an unavailable device this behavior is necessary for the
        // following event to execute properly, if these statements are removed
        // no events will be generated since no ports will be provided in
        // getPorts() to EdgeManager.
        alwaysReturnPorts = true;
        postTopologyEvent(event);
        alwaysReturnPorts = false;
        assertEquals("An unexpected number of events were created.", totalPorts - numInfraPorts, events.size());
        for (int i = 0; i < 5; i++) {
            EdgePortEvent edgeEvent = events.get(i);
            assertEquals("The event is of an unexpected type.",
                         EdgePortEvent.Type.EDGE_PORT_REMOVED, edgeEvent.type());
            assertThat("The event pertains to an unexpected port",
                       edgeEvent.subject().port().toLong(),
                       is(greaterThanOrEqualTo((long) numInfraPorts)));
        }
    }


    @Test
    public void testInternalCache() {
        int numDevices = 10;
        //Number of infrastructure ports per device
        int numPorts = 5;
        //Total ports per device when requesting all devices
        totalPorts = 10;
        defaultPopulator(numDevices, numPorts);
        for (int i = 0; i < numDevices; i++) {
            Device newDevice = NetTestTools.device(Integer.toString(i));
            devices.put(newDevice.id(), newDevice);
            postTopologyEvent(new DeviceEvent(DEVICE_ADDED, newDevice));
        }
        //Check all ports have correct designations
        ConnectPoint testPoint;
        for (int deviceNum = 0; deviceNum < numDevices; deviceNum++) {
            for (int portNum = 1; portNum <= totalPorts; portNum++) {
                testPoint = NetTestTools.connectPoint(Integer.toString(deviceNum), portNum);
                if (portNum <= numPorts) {
                    assertFalse("This should not be an edge point", mgr.isEdgePoint(testPoint));
                } else {
                    assertTrue("This should be an edge point", mgr.isEdgePoint(testPoint));
                }
            }
        }
        int count = 0;
        for (ConnectPoint ignored : mgr.getEdgePoints()) {
            count++;
        }
        assertEquals("There are an unexpeceted number of edge points returned.",
                     (totalPorts - numPorts) * numDevices, count);
        for (int deviceNumber = 0; deviceNumber < numDevices; deviceNumber++) {
            count = 0;
            for (ConnectPoint ignored : mgr.getEdgePoints(NetTestTools.did("1"))) {
                count++;
            }
            assertEquals("This element has an unexpected number of edge points.", (totalPorts - numPorts), count);
        }
    }

    @Test
    public void testEmit() {
        byte[] arr = new byte[10];
        Device referenceDevice;
        DeviceEvent event;
        int numDevices = 10;
        int numInfraPorts = 5;
        totalPorts = 10;
        defaultPopulator(numDevices, numInfraPorts);
        for (byte byteIndex = 0; byteIndex < arr.length; byteIndex++) {
            arr[byteIndex] = byteIndex;
        }
        for (int i = 0; i < numDevices; i++) {
            referenceDevice = NetTestTools.device(Integer.toString(i));
            testDeviceManager.listener.event(new DeviceEvent(DEVICE_ADDED, referenceDevice,
                                                             new DefaultPort(referenceDevice,
                                                                             PortNumber.portNumber(1),
                                                                             true)));
        }

        mgr.emitPacket(ByteBuffer.wrap(arr), Optional.empty());

        assertEquals("There were an unexpected number of emitted packets",
                     (totalPorts - numInfraPorts) * numDevices, packets.size());
        Iterator<OutboundPacket> packetIter = packets.iterator();
        OutboundPacket packet;
        while (packetIter.hasNext()) {
            packet = packetIter.next();
            assertEquals("The packet had an incorrect payload.", arr, packet.data().array());
        }
        //Start testing emission to a specific device
        packets.clear();
        mgr.emitPacket(NetTestTools.did(Integer.toString(1)), ByteBuffer.wrap(arr), Optional.empty());

        assertEquals("Unexpected number of outbound packets were emitted.",
                     totalPorts - numInfraPorts, packets.size());
        packetIter = packets.iterator();
        while (packetIter.hasNext()) {
            packet = packetIter.next();
            assertEquals("The packet had an incorrect payload", arr, packet.data().array());
        }
    }


    /**
     * Creates TopologyEvent triggered by {@code event}.
     *
     * @param event reason of the TopologyEvent
     * @return TopologyEvent
     */
    private TopologyEvent topologyEventOf(Event event) {
        return new TopologyEvent(Type.TOPOLOGY_CHANGED, null, ImmutableList.of(event));
    }


    /**
     * Post Event dispatched from TopologyManager.
     *
     * @param event Event
     */
    private void postTopologyEvent(Event event) {
        if (event instanceof DeviceEvent) {
            testDeviceManager.listener.event((DeviceEvent) event);
        }
        if (event instanceof LinkEvent) {
            testLinkService.listener.event((LinkEvent) event);
        }
        //testTopologyManager.listener.event(topologyEventOf(event));
    }


    /**
     * @param numDevices    the number of devices to populate.
     * @param numInfraPorts the number of ports to be set as infrastructure on each device, numbered base 0, ports 0
     *                      through numInfraPorts - 1
     */
    private void defaultPopulator(int numDevices, int numInfraPorts) {
        for (int device = 0; device < numDevices; device++) {
            String str = Integer.toString(device);
            Device deviceToAdd = NetTestTools.device(str);
            devices.put(deviceToAdd.id(), deviceToAdd);
            testDeviceManager.listener.event(new DeviceEvent(DEVICE_ADDED, deviceToAdd));
            for (int port = 1; port <= numInfraPorts; port++) {
                testLinkService.listener.event(new LinkEvent(LINK_ADDED, NetTestTools.link(str, port, "other", 1)));
                infrastructurePorts.add(NetTestTools.connectPoint(str, port));
            }
        }
    }

    /**
     * Adds the specified device with the specified number of edge ports so long as it is less than the total ports.
     *
     * @param device        The device to be added
     * @param deviceName    The name given to generate the devices DID
     * @param numInfraPorts The number of ports to be added numbered 1 ... numInfraPorts
     */
    private void addDevice(Device device, String deviceName, int numInfraPorts) {
        if (!devices.keySet().contains(device.id())) {
            devices.put(device.id(), device);
            for (int i = 1; i <= numInfraPorts && i <= totalPorts; i++) {
                infrastructurePorts.add(NetTestTools.connectPoint(deviceName, i));
            }
        }
    }

    private void removeDevice(Device device) {
        devices.remove(device.id());
    }

    private void removeInfraPort(ConnectPoint port) {
        infrastructurePorts.remove(port);
    }

    private class TestDeviceManager extends DeviceServiceAdapter {
        private DeviceListener listener;

        private Map<DeviceId, Device> devices;

        public TestDeviceManager(Map<DeviceId, Device> devices) {
            this.devices = devices;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            for (DeviceId id : devices.keySet()) {
                if (id.equals(deviceId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            List<Port> ports = new ArrayList<>();
            Device device = devices.get(deviceId);
            if (device == null && !alwaysReturnPorts) {
                return ports;
            }
            for (int portNum = 1; portNum <= totalPorts; portNum++) {
                //String is generated using 'of:' + the passed name, this creates a
                ports.add(new DefaultPort(device, PortNumber.portNumber(portNum), true));
            }
            return ports;
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return devices.values();
        }


        @Override
        public void addListener(DeviceListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(DeviceListener listener) {
            this.listener = null;
        }
    }

    private class TestLinkService extends LinkServiceAdapter {

        private LinkListener listener;

        @Override
        public Set<Link> getLinks(ConnectPoint connectPoint) {
            if (infrastructurePorts.contains(connectPoint)) {
                return Collections.singleton(NetTestTools.link("1", 1, "2", 1));
            } else {
                return Collections.emptySet();
            }
        }

        @Override
        public void addListener(LinkListener listener) {
            this.listener = listener;
        }
    }

    private class TestPacketManager extends PacketServiceAdapter {
        @Override
        public void emit(OutboundPacket packet) {
            packets.add(packet);
        }
    }

    private class TestListener implements EdgePortListener {
        private List<EdgePortEvent> events;

        public TestListener(List<EdgePortEvent> events) {
            this.events = events;
        }

        @Override
        public void event(EdgePortEvent event) {
            events.add(event);
        }
    }
}
