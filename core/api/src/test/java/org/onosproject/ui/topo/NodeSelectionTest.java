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

package org.onosproject.ui.topo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.LinkServiceAdapter;

import static org.junit.Assert.*;
import static org.onosproject.net.Link.Type.DIRECT;

/**
 * Unit tests for {@link NodeSelection}.
 */
public class NodeSelectionTest {

    private static class FakeDevice extends DefaultDevice {
        FakeDevice(DeviceId id) {
            super(null, id, null, null, null, null, null, null);
        }
    }

    private static class FakeHost extends DefaultHost {
        FakeHost(HostId id) {
            super(null, id, null, null, ImmutableSet.of(), ImmutableSet.of(), false);
        }
    }

    private static class FakeLink extends DefaultLink {
        FakeLink(ConnectPoint src, ConnectPoint dst) {
            super(null, src, dst, DIRECT, Link.State.ACTIVE);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String IDS = "ids";
    private static final String HOVER = "hover";

    private static final DeviceId DEVICE_1_ID = DeviceId.deviceId("Device1");
    private static final DeviceId DEVICE_2_ID = DeviceId.deviceId("Device2");
    private static final HostId HOST_A_ID = HostId.hostId("aa:aa:aa:aa:aa:aa/1");
    private static final HostId HOST_B_ID = HostId.hostId("bb:bb:bb:bb:bb:bb/2");
    private static final String LINK_1_ID = "Device1/1-Device2/2";
    private static final ConnectPoint CP_SRC = ConnectPoint.deviceConnectPoint("Device1/1");
    private static final ConnectPoint CP_DST = ConnectPoint.deviceConnectPoint("Device2/2");

    private static final Device DEVICE_1 = new FakeDevice(DEVICE_1_ID);
    private static final Device DEVICE_2 = new FakeDevice(DEVICE_2_ID);
    private static final Host HOST_A = new FakeHost(HOST_A_ID);
    private static final Host HOST_B = new FakeHost(HOST_B_ID);
    private static final Link LINK_A = new FakeLink(CP_SRC, CP_DST);
    private static final Link LINK_B = new FakeLink(CP_DST, CP_SRC);

    // ==================
    // == FAKE SERVICES
    private static class FakeDevices extends DeviceServiceAdapter {
        @Override
        public Device getDevice(DeviceId deviceId) {
            if (DEVICE_1_ID.equals(deviceId)) {
                return DEVICE_1;
            }
            if (DEVICE_2_ID.equals(deviceId)) {
                return DEVICE_2;
            }
            return null;
        }
    }

    private static class FakeHosts extends HostServiceAdapter {
        @Override
        public Host getHost(HostId hostId) {
            if (HOST_A_ID.equals(hostId)) {
                return HOST_A;
            }
            if (HOST_B_ID.equals(hostId)) {
                return HOST_B;
            }
            return null;
        }
    }

    private static class FakeLinks extends LinkServiceAdapter {
        @Override
        public Link getLink(ConnectPoint src, ConnectPoint dst) {
            if (CP_SRC.equals(src) && CP_DST.equals(dst)) {
                return LINK_A;
            } else if (CP_SRC.equals(dst) && CP_DST.equals(src)) {
                return LINK_B;
            }
            return null;
        }
    }

    private DeviceService deviceService = new FakeDevices();
    private HostService hostService = new FakeHosts();
    private LinkService linkService = new FakeLinks();

    private NodeSelection ns;

    private ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    private ArrayNode arrayNode() {
        return mapper.createArrayNode();
    }

    private NodeSelection createNodeSelection(ObjectNode payload) {
        return new NodeSelection(payload, deviceService, hostService, linkService);
    }

    // selection JSON payload creation methods
    private ObjectNode emptySelection() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        return payload;
    }

    private ObjectNode oneDeviceSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(DEVICE_1_ID.toString());
        return payload;
    }

    private ObjectNode oneHostSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(HOST_A_ID.toString());
        return payload;
    }
    private ObjectNode oneLinkSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(LINK_1_ID.toString());
        return payload;
    }

    private ObjectNode twoHostsOneDeviceSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(HOST_A_ID.toString());
        ids.add(DEVICE_1_ID.toString());
        ids.add(HOST_B_ID.toString());
        return payload;
    }

    private ObjectNode oneHostAndHoveringDeviceSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(HOST_A_ID.toString());
        payload.put(HOVER, DEVICE_2_ID.toString());
        return payload;
    }

    private ObjectNode twoDevicesOneHostAndHoveringHostSelected() {
        ObjectNode payload = objectNode();
        ArrayNode ids = arrayNode();
        payload.set(IDS, ids);
        ids.add(HOST_A_ID.toString());
        ids.add(DEVICE_1_ID.toString());
        ids.add(DEVICE_2_ID.toString());
        payload.put(HOVER, HOST_B_ID.toString());
        return payload;
    }


    @Test
    public void basic() {
        ns = createNodeSelection(emptySelection());
        assertEquals("unexpected devices", 0, ns.devices().size());
        assertEquals("unexpected devices w/hover", 0, ns.devicesWithHover().size());
        assertEquals("unexpected hosts", 0, ns.hosts().size());
        assertEquals("unexpected hosts w/hover", 0, ns.hostsWithHover().size());
        assertTrue("unexpected selection", ns.none());
        assertNull("hover?", ns.hovered());
    }

    @Test
    public void oneDevice() {
        ns = createNodeSelection(oneDeviceSelected());
        assertEquals("missing device", 1, ns.devices().size());
        assertTrue("missing device 1", ns.devices().contains(DEVICE_1));
        assertEquals("missing device w/hover", 1, ns.devicesWithHover().size());
        assertTrue("missing device 1 w/hover", ns.devicesWithHover().contains(DEVICE_1));
        assertEquals("unexpected hosts", 0, ns.hosts().size());
        assertEquals("unexpected hosts w/hover", 0, ns.hostsWithHover().size());
        assertFalse("unexpected selection", ns.none());
        assertNull("hover?", ns.hovered());
    }

    @Test
    public void oneHost() {
        ns = createNodeSelection(oneHostSelected());
        assertEquals("unexpected devices", 0, ns.devices().size());
        assertEquals("unexpected devices w/hover", 0, ns.devicesWithHover().size());
        assertEquals("missing host", 1, ns.hosts().size());
        assertTrue("missing host A", ns.hosts().contains(HOST_A));
        assertEquals("missing host w/hover", 1, ns.hostsWithHover().size());
        assertTrue("missing host A w/hover", ns.hostsWithHover().contains(HOST_A));
        assertFalse("unexpected selection", ns.none());
        assertNull("hover?", ns.hovered());
    }

    @Test
    public void oneLink() {
        ns = createNodeSelection(oneLinkSelected());
        assertEquals("unexpected devices", 0, ns.devices().size());
        assertEquals("unexpected devices w/hover", 0, ns.devicesWithHover().size());
        assertEquals("unexpected hosts", 0, ns.hosts().size());
        assertEquals("unexpected hosts w/hover", 0, ns.hostsWithHover().size());
        assertEquals("missing link", 1, ns.links().size());
        assertTrue("missing link A", ns.links().contains(LINK_A));
        assertEquals("missing link w/hover", 1, ns.linksWithHover().size());
        assertTrue("missing link A w/hover", ns.linksWithHover().contains(LINK_A));
        assertFalse("unexpected selection", ns.none());
        assertNull("hover?", ns.hovered());
    }

    @Test
    public void twoHostsOneDevice() {
        ns = createNodeSelection(twoHostsOneDeviceSelected());
        assertEquals("missing device", 1, ns.devices().size());
        assertTrue("missing device 1", ns.devices().contains(DEVICE_1));
        assertEquals("missing device w/hover", 1, ns.devicesWithHover().size());
        assertTrue("missing device 1 w/hover", ns.devicesWithHover().contains(DEVICE_1));
        assertEquals("unexpected hosts", 2, ns.hosts().size());
        assertTrue("missing host A", ns.hosts().contains(HOST_A));
        assertTrue("missing host B", ns.hosts().contains(HOST_B));
        assertEquals("unexpected hosts w/hover", 2, ns.hostsWithHover().size());
        assertTrue("missing host A w/hover", ns.hostsWithHover().contains(HOST_A));
        assertTrue("missing host B w/hover", ns.hostsWithHover().contains(HOST_B));
        assertFalse("unexpected selection", ns.none());
        assertNull("hover?", ns.hovered());
    }

    @Test
    public void oneHostAndHoveringDevice() {
        ns = createNodeSelection(oneHostAndHoveringDeviceSelected());
        assertEquals("unexpected devices", 0, ns.devices().size());
        assertEquals("unexpected devices w/hover", 1, ns.devicesWithHover().size());
        assertTrue("missing device 2 w/hover", ns.devicesWithHover().contains(DEVICE_2));
        assertEquals("missing host", 1, ns.hosts().size());
        assertTrue("missing host A", ns.hosts().contains(HOST_A));
        assertEquals("missing host w/hover", 1, ns.hostsWithHover().size());
        assertTrue("missing host A w/hover", ns.hostsWithHover().contains(HOST_A));
        assertFalse("unexpected selection", ns.none());
        assertEquals("missing hover device 2", DEVICE_2, ns.hovered());
    }

    @Test
    public void twoDevicesOneHostAndHoveringHost() {
        ns = createNodeSelection(twoDevicesOneHostAndHoveringHostSelected());
        assertEquals("missing devices", 2, ns.devices().size());
        assertTrue("missing device 1", ns.devices().contains(DEVICE_1));
        assertTrue("missing device 2", ns.devices().contains(DEVICE_2));
        assertEquals("missing devices w/hover", 2, ns.devicesWithHover().size());
        assertTrue("missing device 1 w/hover", ns.devicesWithHover().contains(DEVICE_1));
        assertTrue("missing device 2 w/hover", ns.devicesWithHover().contains(DEVICE_2));
        assertEquals("missing host", 1, ns.hosts().size());
        assertTrue("missing host A", ns.hosts().contains(HOST_A));
        assertEquals("missing host w/hover", 2, ns.hostsWithHover().size());
        assertTrue("missing host A w/hover", ns.hostsWithHover().contains(HOST_A));
        assertTrue("missing host B w/hover", ns.hostsWithHover().contains(HOST_B));
        assertFalse("unexpected selection", ns.none());
        assertEquals("missing hover host B", HOST_B, ns.hovered());
    }
}
