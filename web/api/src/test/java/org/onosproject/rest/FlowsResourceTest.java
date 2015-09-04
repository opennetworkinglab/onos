/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.codec.impl.FlowRuleCodec;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for Flows REST APIs.
 */
public class FlowsResourceTest extends ResourceTest {
    final FlowRuleService mockFlowService = createMock(FlowRuleService.class);
    CoreService mockCoreService = createMock(CoreService.class);

    final HashMap<DeviceId, Set<FlowEntry>> rules = new HashMap<>();

    final DeviceService mockDeviceService = createMock(DeviceService.class);

    final DeviceId deviceId1 = DeviceId.deviceId("1");
    final DeviceId deviceId2 = DeviceId.deviceId("2");
    final DeviceId deviceId3 = DeviceId.deviceId("3");
    final Device device1 = new DefaultDevice(null, deviceId1, Device.Type.OTHER,
            "", "", "", "", null);
    final Device device2 = new DefaultDevice(null, deviceId2, Device.Type.OTHER,
            "", "", "", "", null);

    final MockFlowEntry flow1 = new MockFlowEntry(deviceId1, 1);
    final MockFlowEntry flow2 = new MockFlowEntry(deviceId1, 2);

    final MockFlowEntry flow3 = new MockFlowEntry(deviceId2, 3);
    final MockFlowEntry flow4 = new MockFlowEntry(deviceId2, 4);

    final MockFlowEntry flow5 = new MockFlowEntry(deviceId2, 5);
    final MockFlowEntry flow6 = new MockFlowEntry(deviceId2, 6);

    /**
     * Mock class for a flow entry.
     */
    private static class MockFlowEntry implements FlowEntry {
        final DeviceId deviceId;
        final long baseValue;
        TrafficTreatment treatment;
        TrafficSelector selector;

        public MockFlowEntry(DeviceId deviceId, long id) {
            this.deviceId = deviceId;
            this.baseValue = id * 100;
        }

        @Override
        public FlowEntryState state() {
            return FlowEntryState.ADDED;
        }

        @Override
        public long life() {
            return baseValue + 11;
        }

        @Override
        public long packets() {
            return baseValue + 22;
        }

        @Override
        public long bytes() {
            return baseValue + 33;
        }

        @Override
        public long lastSeen() {
            return baseValue + 44;
        }

        @Override
        public int errType() {
            return 0;
        }

        @Override
        public int errCode() {
            return 0;
        }

        @Override
        public FlowId id() {
            final long id = baseValue + 55;
            return FlowId.valueOf(id);
        }

        @Override
        public GroupId groupId() {
            return new DefaultGroupId(3);
        }

        @Override
        public short appId() {
            return 2;
        }

        @Override
        public int priority() {
            return (int) (baseValue + 66);
        }

        @Override
        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public TrafficSelector selector() {
            return selector;
        }

        @Override
        public TrafficTreatment treatment() {
            return treatment;
        }

        @Override
        public int timeout() {
            return (int) (baseValue + 77);
        }

        @Override
        public boolean isPermanent() {
            return false;
        }

        @Override
        public int tableId() {
            return 0;
        }

        @Override
        public boolean exactMatch(FlowRule rule) {
            return false;
        }

        @Override
        public FlowRuleExtPayLoad payLoad() {
            return null;
        }
    }

    /**
     * Populates some flows used as testing data.
     */
    private void setupMockFlows() {
        flow2.treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(new IndexedLambda((short) 4)))
                .add(Instructions.modL0Lambda(new IndexedLambda((short) 5)))
                .setEthDst(MacAddress.BROADCAST)
                .build();
        flow2.selector = DefaultTrafficSelector.builder()
                .matchEthType((short) 3)
                .matchIPProtocol((byte) 9)
                .build();
        flow4.treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(new IndexedLambda((short) 6)))
                .build();
        final Set<FlowEntry> flows1 = new HashSet<>();
        flows1.add(flow1);
        flows1.add(flow2);

        final Set<FlowEntry> flows2 = new HashSet<>();
        flows2.add(flow3);
        flows2.add(flow4);

        rules.put(deviceId1, flows1);
        rules.put(deviceId2, flows2);

        expect(mockFlowService.getFlowEntries(deviceId1))
                .andReturn(rules.get(deviceId1)).anyTimes();
        expect(mockFlowService.getFlowEntries(deviceId2))
                .andReturn(rules.get(deviceId2)).anyTimes();
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
        expect(mockCoreService.registerApplication(FlowRuleCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        // Register the services needed for the test
        final CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(FlowRuleService.class, mockFlowService)
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
        verify(mockFlowService);
        verify(mockCoreService);
    }

    /**
     * Hamcrest matcher to check that a flow representation in JSON matches
     * the actual flow entry.
     */
    public static class FlowJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final FlowEntry flow;
        private final String expectedAppId;
        private String reason = "";

        public FlowJsonMatcher(FlowEntry flowValue, String expectedAppIdValue) {
            flow = flowValue;
            expectedAppId = expectedAppIdValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonFlow) {
            // check id
            final String jsonId = jsonFlow.get("id").asString();
            final String flowId = Long.toString(flow.id().value());
            if (!jsonId.equals(flowId)) {
                reason = "id " + flow.id().toString();
                return false;
            }

            // check application id
            final String jsonAppId = jsonFlow.get("appId").asString();
            if (!jsonAppId.equals(expectedAppId)) {
                reason = "appId " + Short.toString(flow.appId());
                return false;
            }

            // check device id
            final String jsonDeviceId = jsonFlow.get("deviceId").asString();
            if (!jsonDeviceId.equals(flow.deviceId().toString())) {
                reason = "deviceId " + flow.deviceId();
                return false;
            }

            // check treatment and instructions array
            if (flow.treatment() != null) {
                final JsonObject jsonTreatment = jsonFlow.get("treatment").asObject();
                final JsonArray jsonInstructions = jsonTreatment.get("instructions").asArray();
                if (flow.treatment().immediate().size() != jsonInstructions.size()) {
                    reason = "instructions array size of " +
                            Integer.toString(flow.treatment().immediate().size());
                    return false;
                }
                for (final Instruction instruction : flow.treatment().immediate()) {
                    boolean instructionFound = false;
                    for (int instructionIndex = 0; instructionIndex < jsonInstructions.size(); instructionIndex++) {
                        final String jsonType =
                                jsonInstructions.get(instructionIndex)
                                        .asObject().get("type").asString();
                        final String instructionType = instruction.type().name();
                        if (jsonType.equals(instructionType)) {
                            instructionFound = true;
                        }
                    }
                    if (!instructionFound) {
                        reason = "instruction " + instruction.toString();
                        return false;
                    }
                }
            }

            // check selector and criteria array
            if (flow.selector() != null) {
                final JsonObject jsonTreatment = jsonFlow.get("selector").asObject();
                final JsonArray jsonCriteria = jsonTreatment.get("criteria").asArray();
                if (flow.selector().criteria().size() != jsonCriteria.size()) {
                    reason = "criteria array size of " +
                            Integer.toString(flow.selector().criteria().size());
                    return false;
                }
                for (final Criterion criterion : flow.selector().criteria()) {
                    boolean criterionFound = false;

                    for (int criterionIndex = 0; criterionIndex < jsonCriteria.size(); criterionIndex++) {
                        final String jsonType =
                                jsonCriteria.get(criterionIndex)
                                        .asObject().get("type").asString();
                        final String criterionType = criterion.type().name();
                        if (jsonType.equals(criterionType)) {
                            criterionFound = true;
                        }
                    }
                    if (!criterionFound) {
                        reason = "criterion " + criterion.toString();
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

    /**
     * Factory to allocate a flow matcher.
     *
     * @param flow flow object we are looking for
     * @return matcher
     */
    private static FlowJsonMatcher matchesFlow(FlowEntry flow, String expectedAppName) {
        return new FlowJsonMatcher(flow, expectedAppName);
    }

    /**
     * Hamcrest matcher to check that a flow is represented properly in a JSON
     * array of flows.
     */
    public static class FlowJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final FlowEntry flow;
        private String reason = "";

        public FlowJsonArrayMatcher(FlowEntry flowValue) {
            flow = flowValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean flowFound = false;

            for (int jsonFlowIndex = 0; jsonFlowIndex < json.size();
                 jsonFlowIndex++) {

                final JsonObject jsonFlow = json.get(jsonFlowIndex).asObject();

                final String flowId = Long.toString(flow.id().value());
                final String jsonFlowId = jsonFlow.get("id").asString();
                if (jsonFlowId.equals(flowId)) {
                    flowFound = true;

                    //  We found the correct flow, check attribute values
                    assertThat(jsonFlow, matchesFlow(flow, APP_ID.name()));
                }
            }
            if (!flowFound) {
                reason = "Flow with id " + flow.id().toString() + " not found";
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
     * Factory to allocate a flow array matcher.
     *
     * @param flow flow object we are looking for
     * @return matcher
     */
    private static FlowJsonArrayMatcher hasFlow(FlowEntry flow) {
        return new FlowJsonArrayMatcher(flow);
    }

    /**
     * Tests the result of the rest api GET when there are no flows.
     */
    @Test
    public void testFlowsEmptyArray() {
        expect(mockFlowService.getFlowEntries(deviceId1))
                .andReturn(null).anyTimes();
        expect(mockFlowService.getFlowEntries(deviceId2))
                .andReturn(null).anyTimes();
        replay(mockFlowService);
        replay(mockDeviceService);
        final WebResource rs = resource();
        final String response = rs.path("flows").get(String.class);
        assertThat(response, is("{\"flows\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are active flows.
     */
    @Test
    public void testFlowsPopulatedArray() {
        setupMockFlows();
        replay(mockFlowService);
        replay(mockDeviceService);
        final WebResource rs = resource();
        final String response = rs.path("flows").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("flows"));
        final JsonArray jsonFlows = result.get("flows").asArray();
        assertThat(jsonFlows, notNullValue());
        assertThat(jsonFlows, hasFlow(flow1));
        assertThat(jsonFlows, hasFlow(flow2));
        assertThat(jsonFlows, hasFlow(flow3));
        assertThat(jsonFlows, hasFlow(flow4));
    }

    /**
     * Tests the result of a rest api GET for a device.
     */
    @Test
    public void testFlowsSingleDevice() {
        setupMockFlows();
        final Set<FlowEntry> flows = new HashSet<>();
        flows.add(flow5);
        flows.add(flow6);
        expect(mockFlowService.getFlowEntries(anyObject()))
                .andReturn(flows).anyTimes();
        replay(mockFlowService);
        replay(mockDeviceService);
        final WebResource rs = resource();
        final String response = rs.path("flows/" + deviceId3).get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("flows"));
        final JsonArray jsonFlows = result.get("flows").asArray();
        assertThat(jsonFlows, notNullValue());
        assertThat(jsonFlows, hasFlow(flow5));
        assertThat(jsonFlows, hasFlow(flow6));
    }

    /**
     * Tests the result of a rest api GET for a device.
     */
    @Test
    public void testFlowsSingleDeviceWithFlowId() {
        setupMockFlows();
        final Set<FlowEntry> flows = new HashSet<>();
        flows.add(flow5);
        flows.add(flow6);
        expect(mockFlowService.getFlowEntries(anyObject()))
                .andReturn(flows).anyTimes();
        replay(mockFlowService);
        replay(mockDeviceService);
        final WebResource rs = resource();
        final String response = rs.path("flows/" + deviceId3 + "/"
                + Long.toString(flow5.id().value())).get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("flows"));
        final JsonArray jsonFlows = result.get("flows").asArray();
        assertThat(jsonFlows, notNullValue());
        assertThat(jsonFlows, hasFlow(flow5));
        assertThat(jsonFlows, not(hasFlow(flow6)));
    }

    /**
     * Tests that a fetch of a non-existent device object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(mockFlowService.getFlowEntries(deviceId1))
                .andReturn(null).anyTimes();
        expect(mockFlowService.getFlowEntries(deviceId2))
                .andReturn(null).anyTimes();
        replay(mockFlowService);
        replay(mockDeviceService);

        WebResource rs = resource();
        try {
            rs.path("flows/0").get(String.class);
            fail("Fetch of non-existent device did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                    containsString("returned a response status of"));
        }
    }

    /**
     * Tests creating a flow with POST.
     */
    @Test
    public void testPost() {


        mockFlowService.applyFlowRules(anyObject());
        expectLastCall();
        replay(mockFlowService);

        WebResource rs = resource();
        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-flow.json");

        ClientResponse response = rs.path("flows/of:0000000000000001")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/flows/of:0000000000000001/"));
    }

    /**
     * Tests deleting a flow.
     */
    @Test
    public void testDelete() {
        setupMockFlows();
        mockFlowService.removeFlowRules(anyObject());
        expectLastCall();
        replay(mockFlowService);

        WebResource rs = resource();

        String location = "/flows/1/155";

        ClientResponse deleteResponse = rs.path(location)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(deleteResponse.getStatus(),
                is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
