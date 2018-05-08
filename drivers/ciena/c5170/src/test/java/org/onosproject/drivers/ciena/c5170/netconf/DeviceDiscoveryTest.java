/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.drivers.ciena.c5170.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.drivers.ciena.c5170.Ciena5170DriversLoader;
import org.onosproject.drivers.netconf.MockCoreService;
import org.onosproject.drivers.netconf.MockDevice;
import org.onosproject.drivers.netconf.MockDeviceService;
import org.onosproject.drivers.netconf.MockDriverHandler;
import org.onosproject.drivers.netconf.MockTemplateRequestDriver;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

public class DeviceDiscoveryTest {

    Map<DeviceId, MockDriverHandler> mockDriverHandlers = new HashMap<DeviceId, MockDriverHandler>();
    MockTemplateRequestDriver mockRequestDriver = new MockTemplateRequestDriver();
    MockDeviceService deviceService = new MockDeviceService();

    Logger log = getLogger(DeviceDiscoveryTest.class);

    @Before
    public void setup() {
        MockCoreService coreService = new MockCoreService(101, "org.onosproject.drivers.netconf",
                "org.onosproject.linkdiscovery", "org.onosproject.drivers.ciena.c5170");

        // Load the mock responses for mock device "netconf:1.2.3.4:830"
        DeviceId mId = DeviceId.deviceId("netconf:1.2.3.4:830");
        mockRequestDriver.load(DeviceDiscoveryTest.class, "/templates/responses/device_1_2_3_4/%s.j2", mId,
                "systemInfo", "softwareVersion", "logicalPorts", "link-info", "port-stats", "chassis-mac");
        MockDriverHandler mockDriverHandler = new MockDriverHandler(Ciena5170DriversLoader.class,
                "/ciena-5170-drivers.xml", mId, coreService, deviceService);
        NetconfController controller = mockDriverHandler.get(NetconfController.class);
        mockDriverHandlers.put(mId, mockDriverHandler);
        mockRequestDriver.setDeviceMap(controller.getDevicesMap());

        // Load the mock responses for mock device "netconf:5.6.7.8:830"
        mId = DeviceId.deviceId("netconf:5.6.7.8:830");
        mockRequestDriver.load(DeviceDiscoveryTest.class, "/templates/responses/device_5_6_7_8/%s.j2", mId,
                "systemInfo", "softwareVersion", "logicalPorts", "link-info", "chassis-mac");
        mockDriverHandler = new MockDriverHandler(Ciena5170DriversLoader.class, "/ciena-5170-drivers.xml", mId,
                coreService, deviceService);
        controller = mockDriverHandler.get(NetconfController.class);
        mockDriverHandlers.put(mId, mockDriverHandler);

        mockRequestDriver.setDeviceMap(controller.getDevicesMap());
    }

    @Test
    public void deviceDescriptionTest() {
        Ciena5170DeviceDescription targetUnderTest = new Ciena5170DeviceDescription();
        Ciena5170DeviceDescription.TEMPLATE_MANAGER.setRequestDriver(mockRequestDriver);
        targetUnderTest.setHandler(mockDriverHandlers.get(DeviceId.deviceId("netconf:1.2.3.4:830")));

        DeviceDescription desc = targetUnderTest.discoverDeviceDetails();
        assertEquals("Chassis ID", "1C1161D18800", desc.chassisId().toString().toUpperCase());
        assertEquals("Manufacturer", "Ciena", desc.manufacturer());
        assertEquals("HW Version", "CN5170", desc.hwVersion());
        assertEquals("Serial Number", "1C1161D18800", desc.serialNumber());
        assertEquals("SW Version", "saos-01-01-00-0025", desc.swVersion());
    }

    @Test
    public void discoverPortDetailsTest() {
        Ciena5170DeviceDescription targetUnderTest = new Ciena5170DeviceDescription();
        Ciena5170DeviceDescription.TEMPLATE_MANAGER.setRequestDriver(mockRequestDriver);
        targetUnderTest.setHandler(mockDriverHandlers.get(DeviceId.deviceId("netconf:1.2.3.4:830")));
        List<PortDescription> ports = targetUnderTest.discoverPortDetails();
        assertEquals(44, ports.size());
        Map<Long, Integer> speedCounts = new HashMap<Long, Integer>();
        for (PortDescription port : ports) {
            if (port.portNumber().toLong() >= 41L && port.portNumber().toLong() <= 44L) {
                assertEquals(String.format("PORT %s enable status", port.portNumber()), true, port.isEnabled());
            } else {
                assertEquals(String.format("PORT %s enable status", port.portNumber()), false, port.isEnabled());

            }
            Long speed = port.portSpeed();
            Integer count = speedCounts.get(speed);
            if (count == null) {
                speedCounts.put(speed, 1);
            } else {
                speedCounts.put(speed, count + 1);
            }
        }

        assertEquals("PORT count with speed 1G", new Integer(40), speedCounts.get(10000L));
        assertEquals("PORT count with speed 10G", new Integer(4), speedCounts.get(100000L));
    }

    @Test
    public void discoverPortStatisticsTest() {
        Ciena5170DeviceDescription targetUnderTest = new Ciena5170DeviceDescription();
        Ciena5170DeviceDescription.TEMPLATE_MANAGER.setRequestDriver(mockRequestDriver);
        targetUnderTest.setHandler(mockDriverHandlers.get(DeviceId.deviceId("netconf:1.2.3.4:830")));
        Collection<PortStatistics> stats = targetUnderTest.discoverPortStatistics();
        assertEquals("PORT STAT COUNT", 44, stats.size());
        Optional<PortStatistics> stat = stats.stream().filter(s -> s.portNumber().toLong() == 44L).findFirst();
        assertTrue("Port 44 stats exist", stat.isPresent());
        assertEquals("Port 44 pkt out", 4587L, stat.get().packetsSent());
        assertEquals("Port 44 pkt in", 411155469L, stat.get().packetsReceived());
        assertEquals("Port 44 bytes out", 524744L, stat.get().bytesSent());
        assertEquals("Port 44 bytes in", 105255079746L, stat.get().bytesReceived());
        assertEquals("Port 44 errors out", 0L, stat.get().packetsTxErrors());
        assertEquals("Port 44 errors in", 0L, stat.get().packetsRxErrors());
    }

    @Test
    public void getLinksTest() {
        Ciena5170DeviceDescription targetUnderTest1 = new Ciena5170DeviceDescription();
        Ciena5170DeviceDescription targetUnderTest2 = new Ciena5170DeviceDescription();
        Ciena5170DeviceDescription.TEMPLATE_MANAGER.setRequestDriver(mockRequestDriver);

        DeviceId mId = DeviceId.deviceId("netconf:1.2.3.4:830");
        targetUnderTest1.setHandler(mockDriverHandlers.get(mId));
        deviceService.addDevice(new MockDevice(mId, targetUnderTest1.discoverDeviceDetails()));

        mId = DeviceId.deviceId("netconf:5.6.7.8:830");
        targetUnderTest2.setHandler(mockDriverHandlers.get(mId));
        deviceService.addDevice(new MockDevice(mId, targetUnderTest2.discoverDeviceDetails()));

        Set<LinkDescription> links1 = targetUnderTest1.getLinks();
        Set<LinkDescription> links2 = targetUnderTest2.getLinks();

        assertEquals("A to B link count", 1, links1.size());
        assertEquals("B to A link count", 1, links2.size());
        LinkDescription a2b = links1.toArray(new LinkDescription[0])[0];
        LinkDescription b2a = links2.toArray(new LinkDescription[0])[0];
        assertEquals("A to B src and dest", a2b.src(), b2a.dst());
        assertEquals("B to A src and dest", b2a.src(), a2b.dst());
    }
}
