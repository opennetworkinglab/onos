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
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.codec.impl.MeterCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for meters REST APIs.
 */
public class MetersResourceTest extends ResourceTest {
    final MeterService mockMeterService = createMock(MeterService.class);
    CoreService mockCoreService = createMock(CoreService.class);
    final DeviceService mockDeviceService = createMock(DeviceService.class);

    final HashMap<DeviceId, Set<Meter>> meters = new HashMap<>();

    final DeviceId deviceId1 = DeviceId.deviceId("1");
    final DeviceId deviceId2 = DeviceId.deviceId("2");
    final DeviceId deviceId3 = DeviceId.deviceId("3");
    final Device device1 = new DefaultDevice(null, deviceId1, Device.Type.OTHER,
            "", "", "", "", null);
    final Device device2 = new DefaultDevice(null, deviceId2, Device.Type.OTHER,
            "", "", "", "", null);

    final MockMeter meter1 = new MockMeter(deviceId1, 1, 111, 1);
    final MockMeter meter2 = new MockMeter(deviceId1, 2, 222, 2);
    final MockMeter meter3 = new MockMeter(deviceId2, 3, 333, 3);
    final MockMeter meter4 = new MockMeter(deviceId2, 4, 444, 4);
    final MockMeter meter5 = new MockMeter(deviceId3, 5, 555, 5);

    /**
     * Mock class for a meter.
     */
    private static class MockMeter implements Meter {

        final DeviceId deviceId;
        final ApplicationId appId;
        final MeterId meterId;
        final long baseValue;
        final List<Band> bandList;

        public MockMeter(DeviceId deviceId, int appId, long meterId, int id) {
            this.deviceId = deviceId;
            this.appId = new DefaultApplicationId(appId, String.valueOf(appId));
            this.baseValue = id * 200;
            this.meterId = MeterId.meterId(meterId);

            Band band = DefaultBand.builder()
                    .ofType(Band.Type.REMARK)
                    .withRate(10)
                    .dropPrecedence((short) 20)
                    .burstSize(30).build();

            this.bandList = new ArrayList<>();
            this.bandList.add(band);
        }

        @Override
        public DeviceId deviceId() {
            return this.deviceId;
        }

        @Override
        public MeterId id() {
            return this.meterId;
        }

        @Override
        public ApplicationId appId() {
            return this.appId;
        }

        @Override
        public Unit unit() {
            return Unit.KB_PER_SEC;
        }

        @Override
        public boolean isBurst() {
            return false;
        }

        @Override
        public Collection<Band> bands() {
            return this.bandList;
        }

        @Override
        public MeterState state() {
            return MeterState.ADDED;
        }

        @Override
        public long life() {
            return baseValue + 11;
        }

        @Override
        public long referenceCount() {
            return baseValue + 22;
        }

        @Override
        public long packetsSeen() {
            return baseValue + 33;
        }

        @Override
        public long bytesSeen() {
            return baseValue + 44;
        }
    }

    /**
     * Populates some meters used as testing data.
     */
    private void setupMockMeters() {
        final Set<Meter> meters1 = new HashSet<>();
        meters1.add(meter1);
        meters1.add(meter2);

        final Set<Meter> meters2 = new HashSet<>();
        meters2.add(meter3);
        meters2.add(meter4);

        meters.put(deviceId1, meters1);
        meters.put(deviceId2, meters2);

        Set<Meter> allMeters = new HashSet<>();
        for (DeviceId deviceId : meters.keySet()) {
            allMeters.addAll(meters.get(deviceId));
        }

        expect(mockMeterService.getAllMeters()).andReturn(allMeters).anyTimes();
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        // Mock device service
        expect(mockDeviceService.getDevice(deviceId1))
                .andReturn(device1);
        expect(mockDeviceService.getDevice(deviceId2))
                .andReturn(device2);
        expect(mockDeviceService.getDevices())
                .andReturn(ImmutableSet.of(device1, device2));

        // Mock Core Service
        expect(mockCoreService.getAppId(anyShort()))
                .andReturn(NetTestTools.APP_ID).anyTimes();
        expect(mockCoreService.registerApplication(MeterCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        // Register the services needed for the test
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(MeterService.class, mockMeterService)
                        .add(DeviceService.class, mockDeviceService)
                        .add(CodecService.class, codecService)
                        .add(CoreService.class, mockCoreService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up and verifies the mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockMeterService);
        verify(mockCoreService);
    }

    /**
     * Hamcrest matcher to check that a meter representation in JSON matches
     * the actual meter.
     */
    public static class MeterJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Meter meter;
        private String reason = "";

        public MeterJsonMatcher(Meter meterValue) {
            this.meter = meterValue;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonMeter) {

            // check application id
            final String jsonAppId = jsonMeter.get("appId").asString();
            final String appId = meter.appId().name();
            if (!jsonAppId.equals(appId)) {
                reason = "appId " + meter.appId().name();
                return false;
            }

            // check device id
            final String jsonDeviceId = jsonMeter.get("deviceId").asString();
            if (!jsonDeviceId.equals(meter.deviceId().toString())) {
                reason = "deviceId " + meter.deviceId();
                return false;
            }

            // check band array
            if (meter.bands() != null) {
                final JsonArray jsonBands = jsonMeter.get("bands").asArray();
                if (meter.bands().size() != jsonBands.size()) {
                    reason = "bands array size of " +
                            Integer.toString(meter.bands().size());
                    return false;
                }
                for (final Band band : meter.bands()) {
                    boolean bandFound = false;
                    for (int bandIndex = 0; bandIndex < jsonBands.size(); bandIndex++) {
                        final String jsonType = jsonBands.get(bandIndex).asObject().get("type").asString();
                        final String bandType = band.type().name();
                        if (jsonType.equals(bandType)) {
                            bandFound = true;
                        }
                    }
                    if (!bandFound) {
                        reason = "meter band " + band.toString();
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    private static MeterJsonMatcher matchesMeter(Meter meter) {
        return new MeterJsonMatcher(meter);
    }

    /**
     * Hamcrest matcher to check that a meter is represented properly in a JSON
     * array of meters.
     */
    public static class MeterJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Meter meter;
        private String reason = "";

        public MeterJsonArrayMatcher(Meter meterValue) {
            meter = meterValue;
        }

        @Override
        protected boolean matchesSafely(JsonArray json) {
            boolean meterFound = false;
            for (int jsonMeterIndex = 0; jsonMeterIndex < json.size(); jsonMeterIndex++) {
                final JsonObject jsonMeter = json.get(jsonMeterIndex).asObject();

                final String meterId = meter.id().toString();
                final String jsonMeterId = jsonMeter.get("id").asString();
                if (jsonMeterId.equals(meterId)) {
                    meterFound = true;

                    assertThat(jsonMeter, matchesMeter(meter));
                }
            }
            if (!meterFound) {
                reason = "Meter with id " + meter.id().toString() + " not found";
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
     * Factory to allocate a meter array matcher.
     *
     * @param meter meter object we are looking for
     * @return matcher
     */
    private static MeterJsonArrayMatcher hasMeter(Meter meter) {
        return new MeterJsonArrayMatcher(meter);
    }

    @Test
    public void testMeterEmptyArray() {
        expect(mockMeterService.getAllMeters()).andReturn(null).anyTimes();
        replay(mockMeterService);
        replay(mockDeviceService);
        final WebTarget wt = target();
        final String response = wt.path("meters").request().get(String.class);
        assertThat(response, is("{\"meters\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are active meters.
     */
    @Test
    public void testMetersPopulatedArray() {
        setupMockMeters();
        replay(mockMeterService);
        replay(mockDeviceService);
        final WebTarget wt = target();
        final String response = wt.path("meters").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("meters"));
        final JsonArray jsonMeters = result.get("meters").asArray();
        assertThat(jsonMeters, notNullValue());
        assertThat(jsonMeters, hasMeter(meter1));
        assertThat(jsonMeters, hasMeter(meter2));
        assertThat(jsonMeters, hasMeter(meter3));
        assertThat(jsonMeters, hasMeter(meter4));
    }

    /**
     * Tests the results of a rest api GET for a device.
     */
    @Test
    public void testMeterSingleDevice() {
        setupMockMeters();

        final Set<Meter> meters1 = new HashSet<>();
        meters1.add(meter1);
        meters1.add(meter2);

        expect(mockMeterService.getMeters(anyObject())).andReturn(meters1).anyTimes();
        replay(mockMeterService);
        replay(mockDeviceService);

        final WebTarget wt = target();
        final String response = wt.path("meters/" + deviceId1.toString()).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("meters"));
        final JsonArray jsonMeters = result.get("meters").asArray();
        assertThat(jsonMeters, notNullValue());
        assertThat(jsonMeters, hasMeter(meter1));
        assertThat(jsonMeters, hasMeter(meter2));
    }

    /**
     * Tests the result of a rest api GET for a device with meter id.
     */
    @Test
    public void testMeterSingleDeviceWithId() {
        setupMockMeters();

        expect(mockMeterService.getMeter(anyObject(), anyObject()))
                .andReturn(meter5).anyTimes();
        replay(mockMeterService);
        replay(mockDeviceService);

        final WebTarget wt = target();
        final String response = wt.path("meters/" + deviceId3.toString()
                + "/" + meter5.id().id()).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("meters"));
        final JsonArray jsonMeters = result.get("meters").asArray();
        assertThat(jsonMeters, notNullValue());
        assertThat(jsonMeters, hasMeter(meter5));
    }

    /**
     * Test whether the REST API returns 404 if no entry has been found.
     */
    @Test
    public void testMeterByDeviceIdAndMeterId() {
        setupMockMeters();

        expect(mockMeterService.getMeter(anyObject(), anyObject()))
                .andReturn(null).anyTimes();
        replay(mockMeterService);

        final WebTarget wt = target();
        final Response response = wt.path("meters/" + deviceId3.toString()
                + "/" + "888").request().get();

        assertEquals(404, response.getStatus());
    }

    /**
     * Tests creating a meter with POST.
     */
    @Test
    public void testPost() {
        mockMeterService.submit(anyObject());
        expectLastCall().andReturn(meter5).anyTimes();
        replay(mockMeterService);

        WebTarget wt = target();
        InputStream jsonStream = MetersResourceTest.class
                .getResourceAsStream("post-meter.json");

        Response response = wt.path("meters/of:0000000000000001")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/meters/of:0000000000000001/"));
    }

    /**
     * Tests deleting a meter.
     */
    @Test
    public void testDelete() {
        setupMockMeters();
        expect(mockMeterService.getMeter(anyObject(), anyObject()))
                .andReturn(meter5).anyTimes();
        mockMeterService.withdraw(anyObject(), anyObject());
        expectLastCall();
        replay(mockMeterService);

        WebTarget wt = target();

        String location = "/meters/3/555";

        Response deleteResponse = wt.path(location)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertThat(deleteResponse.getStatus(),
                is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
