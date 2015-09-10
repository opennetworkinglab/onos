/*
 * Copyright 2015 Open Networking Laboratory
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
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NodeSelection}.
 */
public class NodeSelectionTest {

    private static class FakeDevice implements Device {

        private final DeviceId id;

        FakeDevice(DeviceId id) {
            this.id = id;
        }

        @Override
        public DeviceId id() {
            return id;
        }

        @Override
        public Type type() {
            return null;
        }

        @Override
        public String manufacturer() {
            return null;
        }

        @Override
        public String hwVersion() {
            return null;
        }

        @Override
        public String swVersion() {
            return null;
        }

        @Override
        public String serialNumber() {
            return null;
        }

        @Override
        public ChassisId chassisId() {
            return null;
        }

        @Override
        public Annotations annotations() {
            return null;
        }

        @Override
        public ProviderId providerId() {
            return null;
        }
    }

    private static class FakeHost implements Host {

        private final HostId id;

        FakeHost(HostId id) {
            this.id = id;
        }

        @Override
        public HostId id() {
            return id;
        }

        @Override
        public MacAddress mac() {
            return null;
        }

        @Override
        public VlanId vlan() {
            return null;
        }

        @Override
        public Set<IpAddress> ipAddresses() {
            return null;
        }

        @Override
        public HostLocation location() {
            return null;
        }

        @Override
        public Annotations annotations() {
            return null;
        }

        @Override
        public ProviderId providerId() {
            return null;
        }
    }



    private final ObjectMapper mapper = new ObjectMapper();

    private static final String IDS = "ids";
    private static final String HOVER = "hover";

    private static final DeviceId DEVICE_1_ID = DeviceId.deviceId("Device-1");
    private static final DeviceId DEVICE_2_ID = DeviceId.deviceId("Device-2");
    private static final HostId HOST_A_ID = HostId.hostId("aa:aa:aa:aa:aa:aa/1");
    private static final HostId HOST_B_ID = HostId.hostId("bb:bb:bb:bb:bb:bb/2");

    private static final Device DEVICE_1 = new FakeDevice(DEVICE_1_ID);
    private static final Device DEVICE_2 = new FakeDevice(DEVICE_2_ID);
    private static final Host HOST_A = new FakeHost(HOST_A_ID);
    private static final Host HOST_B = new FakeHost(HOST_B_ID);

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

    private DeviceService deviceService = new FakeDevices();
    private HostService hostService = new FakeHosts();

    private NodeSelection ns;

    private ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    private ArrayNode arrayNode() {
        return mapper.createArrayNode();
    }

    private NodeSelection createNodeSelection(ObjectNode payload) {
        return new NodeSelection(payload, deviceService, hostService);
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
