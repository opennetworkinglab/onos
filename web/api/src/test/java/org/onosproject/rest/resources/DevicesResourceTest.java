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
package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.TestBehaviourImpl;
import org.onosproject.net.driver.TestBehaviour;
import org.onosproject.net.driver.TestBehaviourTwo;
import org.onosproject.net.driver.TestBehaviourTwoImpl;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for devices REST APIs.
 */
public class DevicesResourceTest extends ResourceTest {
    DeviceService mockDeviceService;
    DriverService mockDriverService;
    DefaultDriver driver = new DefaultDriver("ovs", new ArrayList<>(), "Circus", "lux", "1.2a",
            ImmutableMap.of(TestBehaviour.class,
                    TestBehaviourImpl.class,
                    TestBehaviourTwo.class,
                    TestBehaviourTwoImpl.class),
            ImmutableMap.of("foo", "bar"));

    /**
     * Hamcrest matcher to check that an device representation in JSON matches
     * the actual device.
     */
    public static class DeviceJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Device device;
        private String reason = "";

        public DeviceJsonMatcher(Device deviceValue) {
            device = deviceValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonDevice) {
            // check id
            String jsonId = jsonDevice.get("id").asString();
            if (!jsonId.equals(device.id().toString())) {
                reason = "id " + device.id().toString();
                return false;
            }

            // check type
            String jsonType = jsonDevice.get("type").asString();
            if (!jsonType.equals(device.type().toString())) {
                reason = "appId " + device.type().toString();
                return false;
            }

            // check manufacturer
            String jsonManufacturer = jsonDevice.get("mfr").asString();
            if (!jsonManufacturer.equals(device.manufacturer())) {
                reason = "manufacturer " + device.manufacturer();
                return false;
            }

            // check HW version field
            String jsonHwVersion = jsonDevice.get("hw").asString();

            if (!jsonHwVersion.equals(device.hwVersion())) {
                reason = "hw Version " + device.hwVersion();
                return false;
            }

            // check SW version field
            String jsonSwVersion = jsonDevice.get("sw").asString();
            if (!jsonSwVersion.equals(device.swVersion())) {
                reason = "sw Version " + device.swVersion();
                return false;
            }

            // check serial number field
            String jsonSerialNumber = jsonDevice.get("serial").asString();
            if (!jsonSerialNumber.equals(device.serialNumber())) {
                reason = "serial number " + device.serialNumber();
                return false;
            }

            // check chassis id field
            String jsonChassisId = jsonDevice.get("chassisId").asString();
            if (!jsonChassisId.equals(device.chassisId().toString())) {
                reason = "Chassis id " + device.chassisId().toString();
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an device matcher.
     *
     * @param device device object we are looking for
     * @return matcher
     */
    private static DeviceJsonMatcher matchesDevice(Device device) {
        return new DeviceJsonMatcher(device);
    }

    /**
     * Hamcrest matcher to check that an device is represented properly in a JSON
     * array of devices.
     */
    private static class DeviceJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Device device;
        private String reason = "";

        public DeviceJsonArrayMatcher(Device deviceValue) {
            device = deviceValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            final int minExpectedAttributes = 9;
            final int maxExpectedAttributes = 10;

            boolean deviceFound = false;

            for (int jsonDeviceIndex = 0; jsonDeviceIndex < json.size();
                 jsonDeviceIndex++) {

                JsonObject jsonDevice = json.get(jsonDeviceIndex).asObject();

                if (jsonDevice.names().size() < minExpectedAttributes ||
                    jsonDevice.names().size() > maxExpectedAttributes) {
                    reason = "Found a device with the wrong number of attributes";
                    return false;
                }

                String jsonDeviceId = jsonDevice.get("id").asString();
                if (jsonDeviceId.equals(device.id().toString())) {
                    deviceFound = true;

                    //  We found the correct device, check attribute values
                    assertThat(jsonDevice, matchesDevice(device));
                }
            }
            if (!deviceFound) {
                reason = "Device with id " + device.id().toString() + " not found";
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an device array matcher.
     *
     * @param device device object we are looking for
     * @return matcher
     */
    private static DeviceJsonArrayMatcher hasDevice(Device device) {
        return new DeviceJsonArrayMatcher(device);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpMocks() {
        mockDeviceService = createMock(DeviceService.class);
        mockDriverService = createMock(DriverService.class);
        expect(mockDeviceService.isAvailable(isA(DeviceId.class)))
                .andReturn(true)
                .anyTimes();
        expect(mockDeviceService.getRole(isA(DeviceId.class)))
                .andReturn(MastershipRole.MASTER)
                .anyTimes();


        // Register the services needed for the test
        CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(DeviceService.class, mockDeviceService)
                        .add(DriverService.class, mockDriverService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Verifies test mocks.
     */
    @After
    public void tearDownMocks() {
        verify(mockDeviceService);
    }

    /**
     * Tests the result of the rest api GET when there are no devices.
     */
    @Test
    public void testDevicesEmptyArray() {
        expect(mockDeviceService.getDevices()).andReturn(ImmutableList.of());
        replay(mockDeviceService);

        WebTarget wt = target();
        String response = wt.path("devices").request().get(String.class);
        assertThat(response, is("{\"devices\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are devices present.
     */
    @Test
    public void testDevices() {
        Device device1 = device("dev1");
        Device device2 = device("dev2");
        Device device3 = device("dev3");

        expect(mockDeviceService.getDevices())
                .andReturn(ImmutableList.of(device1, device2, device3))
                .anyTimes();

        replay(mockDeviceService);

        expect(mockDriverService.getDriver(did("dev1")))
                .andReturn(driver)
                .anyTimes();

        expect(mockDriverService.getDriver(did("dev2")))
                .andReturn(driver)
                .anyTimes();

        expect(mockDriverService.getDriver(did("dev3")))
                .andReturn(driver)
                .anyTimes();

        replay(mockDriverService);



        WebTarget wt = target();
        String response = wt.path("devices").request().get(String.class);
        assertThat(response, containsString("{\"devices\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("devices"));

        JsonArray jsonDevices = result.get("devices").asArray();
        assertThat(jsonDevices, notNullValue());
        assertThat(jsonDevices.size(), is(3));

        assertThat(jsonDevices, hasDevice(device1));
        assertThat(jsonDevices, hasDevice(device2));
        assertThat(jsonDevices, hasDevice(device3));
    }

    /**
     * Tests the result of a rest api GET for a single device.
     */
    @Test
    public void testDevicesSingle() {

        String deviceIdString = "testdevice";
        DeviceId deviceId = did(deviceIdString);
        Device device = device(deviceIdString);
        expect(mockDeviceService.getDevice(deviceId))
                .andReturn(device)
                .once();
        replay(mockDeviceService);
        expect(mockDriverService.getDriver(deviceId))
                .andReturn(driver)
                .anyTimes();
        replay(mockDriverService);

        WebTarget wt = target();
        String response = wt.path("devices/" + deviceId).request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, matchesDevice(device));
    }

    /**
     * Tests the result of a rest api GET for the ports of a single device.
     */
    @Test
    public void testDeviceAndPorts() {

        String deviceIdString = "testdevice";
        DeviceId deviceId = did(deviceIdString);
        Device device = device(deviceIdString);


        Port port1 = new DefaultPort(device, portNumber(1), true);
        Port port2 = new DefaultPort(device, portNumber(2), true);
        Port port3 = new DefaultPort(device, portNumber(3), true);
        List<Port> ports = ImmutableList.of(port1, port2, port3);

        expect(mockDeviceService.getDevice(deviceId))
                .andReturn(device)
                .once();

        expect(mockDeviceService.getPorts(deviceId))
                .andReturn(ports)
                .once();
        replay(mockDeviceService);

        expect(mockDriverService.getDriver(deviceId))
                .andReturn(driver)
                .anyTimes();
        replay(mockDriverService);


        WebTarget wt = target();
        String response =
                wt.path("devices/" + deviceId + "/ports").request()
                    .get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, matchesDevice(device));

        JsonArray jsonPorts = result.get("ports").asArray();
        assertThat(jsonPorts.size(), is(3));
        for (int portIndex = 0; portIndex < jsonPorts.size(); portIndex++) {
            JsonObject jsonPort = jsonPorts.get(portIndex).asObject();

            assertThat(jsonPort.get("port").asString(),
                       is(Integer.toString(portIndex + 1)));
            assertThat(jsonPort.get("isEnabled").asBoolean(),
                       is(true));
            assertThat(jsonPort.get("type").asString(),
                       equalTo("copper"));
            assertThat(jsonPort.get("portSpeed").asLong(),
                    is(1000L));
        }
    }

    /**
     * Tests that a fetch of a non-existent device object throws an exception.
     */
    @Test
    public void testBadGet() {

        expect(mockDeviceService.getDevice(isA(DeviceId.class)))
                .andReturn(null)
                .anyTimes();
        replay(mockDeviceService);

        WebTarget wt = target();
        try {
            wt.path("devices/0").request().get(String.class);
            fail("Fetch of non-existent device did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(),
                    containsString("HTTP 404 Not Found"));
        }
    }
}
