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
import com.eclipsesource.json.JsonValue;
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
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
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.intent.IntentTestsMocks.MockIntent;
import static org.onosproject.net.intent.MockIdGenerator.bindNewGenerator;

/**
 * Unit tests for Intents REST APIs.
 */
public class IntentsResourceTest extends ResourceTest {


    private static final String APPID = "appId";
    private static final String CRITERIA = "criteria";
    private static final String DEVICE_ID = "deviceId";
    private static final String ID = "id";
    private static final String INSTRUCTIONS = "instructions";
    private static final String PATHS = "paths";
    private static final String SELECTOR = "selector";
    private static final String SPACE = " ";
    private static final String TREATMENT = "treatment";
    private static final String TYPE = "type";

    final IntentService mockIntentService = createMock(IntentService.class);
    final CoreService mockCoreService = createMock(CoreService.class);
    final FlowRuleService mockFlowService = createMock(FlowRuleService.class);
    final HashSet<Intent> intents = new HashSet<>();
    final List<org.onosproject.net.intent.Intent> installableIntents = new ArrayList<>();
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "test");
    private IdGenerator mockGenerator;

    final DeviceId deviceId1 = DeviceId.deviceId("1");

    final TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
            .setEthDst(MacAddress.BROADCAST)
            .build();
    final TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
            .setEthDst(MacAddress.IPV4_MULTICAST)
            .build();

    final TrafficSelector selector1 = DefaultTrafficSelector.builder()
            .matchEthType((short) 3)
            .matchIPProtocol((byte) 9)
            .build();
    final TrafficSelector selector2 = DefaultTrafficSelector.builder()
            .matchEthType((short) 4)
            .matchIPProtocol((byte) 10)
            .build();

    final MockFlowEntry flow1 = new MockFlowEntry(deviceId1, 1, treatment1, selector1);
    final MockFlowEntry flow2 = new MockFlowEntry(deviceId1, 2, treatment2, selector2);

    final MockFlowRule flowRule1 = new MockFlowRule(deviceId1, 1, treatment1, selector1);
    final MockFlowRule flowRule2 = new MockFlowRule(deviceId1, 2, treatment2, selector2);

    private class MockResource implements NetworkResource {
        int id;

        MockResource(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Resource " + Integer.toString(id);
        }
    }

    /**
     * Mock class for a flow entry.
     */
    private static class MockFlowEntry implements FlowEntry {
        final DeviceId deviceId;
        final long baseValue;
        TrafficTreatment treatment;
        TrafficSelector selector;

        public MockFlowEntry(DeviceId deviceId, long id,
                             TrafficTreatment treatment,
                             TrafficSelector selector) {
            this.deviceId = deviceId;
            this.baseValue = id * 100;
            this.treatment = treatment;
            this.selector = selector;
        }

        @Override
        public FlowEntryState state() {
            return FlowEntryState.ADDED;
        }

        @Override
        public long life() {
            return life(SECONDS);
        }

        @Override
        public long life(TimeUnit timeUnit) {
            return SECONDS.convert(baseValue + 11, timeUnit);
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
            return 1;
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
        public int hardTimeout() {
            return 0;
        }

        @Override
        public FlowRule.FlowRemoveReason reason() {
            return  FlowRule.FlowRemoveReason.NO_REASON;
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
            return this.appId() == rule.appId() &&
                    this.deviceId().equals(rule.deviceId()) &&
                    this.id().equals(rule.id()) &&
                    this.treatment.equals(rule.treatment()) &&
                    this.selector().equals(rule.selector());
        }

        @Override
        public FlowRuleExtPayLoad payLoad() {
            return null;
        }

        @Override
        public String toString() {
            return id().id().toString();
        }
    }

    /**
     * Mock class for a flow rule.
     */
    private static class MockFlowRule implements FlowRule {

        final DeviceId deviceId;
        final long baseValue;
        TrafficTreatment treatment;
        TrafficSelector selector;

        public MockFlowRule(DeviceId deviceId,
                            long id,
                            TrafficTreatment treatment,
                            TrafficSelector selector) {
            this.deviceId = deviceId;
            this.baseValue = id * 100;
            this.treatment = treatment;
            this.selector = selector;
        }

        @Override
        public FlowId id() {
            long id = baseValue + 55;
            return FlowId.valueOf(id);
        }

        @Override
        public short appId() {
            return 1;
        }

        @Override
        public GroupId groupId() {
            return new DefaultGroupId(3);
        }

        @Override
        public int priority() {
            return 0;
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
        public int hardTimeout() {
            return 0;
        }

        @Override
        public FlowRemoveReason reason() {
            return FlowRemoveReason.NO_REASON;
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
     * Hamcrest matcher to check that an intent representation in JSON matches
     * the actual intent.
     */
    public static class IntentJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Intent intent;
        private String reason = "";

        public IntentJsonMatcher(Intent intentValue) {
            intent = intentValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonIntent) {
            // check id
            final String jsonId = jsonIntent.get("id").asString();
            if (!jsonId.equals(intent.id().toString())) {
                reason = "id " + intent.id().toString();
                return false;
            }

            // check application id

            final String jsonAppId = jsonIntent.get("appId").asString();
            final String appId = intent.appId().name();
            if (!jsonAppId.equals(appId)) {
                reason = "appId was " + jsonAppId;
                return false;
            }

            // check intent type
            final String jsonType = jsonIntent.get("type").asString();
            if (!jsonType.equals("MockIntent")) {
                reason = "type MockIntent";
                return false;
            }

            // check state field
            final String jsonState = jsonIntent.get("state").asString();
            if (!jsonState.equals("INSTALLED")) {
                reason = "state INSTALLED";
                return false;
            }

            // check resources array
            final JsonArray jsonResources = jsonIntent.get("resources").asArray();
            if (intent.resources() != null) {
                if (intent.resources().size() != jsonResources.size()) {
                    reason = "resources array size of " + Integer.toString(intent.resources().size());
                    return false;
                }
                for (final NetworkResource resource : intent.resources()) {
                    boolean resourceFound = false;
                    final String resourceString = resource.toString();
                    for (int resourceIndex = 0; resourceIndex < jsonResources.size(); resourceIndex++) {
                        final JsonValue value = jsonResources.get(resourceIndex);
                        if (value.asString().equals(resourceString)) {
                            resourceFound = true;
                        }
                    }
                    if (!resourceFound) {
                        reason = "resource " + resourceString;
                        return false;
                    }
                }
            } else if (jsonResources.size() != 0) {
                reason = "resources array empty";
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
     * Factory to allocate an intent matcher.
     *
     * @param intent intent object we are looking for
     * @return matcher
     */
    private static IntentJsonMatcher matchesIntent(Intent intent) {
        return new IntentJsonMatcher(intent);
    }

    /**
     * Factory to allocate an IntentRelatedFlows matcher.
     *
     * @param pathEntries list of path conatining flow entries of a particular intent
     * @param expectedAppId expected app id we are looking for
     * @return matcher
     */
    private static IntentStatsJsonMatcher matchesRelatedFlowEntries(
            List<List<FlowEntry>> pathEntries,
            final String expectedAppId) {
        return new IntentStatsJsonMatcher(pathEntries, expectedAppId);
    }

    /**
     * Hamcrest matcher to check that an list of flowEntries in JSON matches
     * the actual list of flow entries.
     */
    public static class IntentStatsJsonMatcher extends
            TypeSafeMatcher<JsonObject> {

        private final List<List<FlowEntry>> pathEntries;
        private final String expectedAppId;
        private String reason = "";

        public IntentStatsJsonMatcher(
                final List<List<FlowEntry>> pathEntries,
                final String expectedAppId) {
            this.pathEntries = pathEntries;
            this.expectedAppId = expectedAppId;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonIntent) {
            int jsonPathIndex = 0;
            JsonArray jsonPaths = jsonIntent.get(PATHS).asArray();

            if (pathEntries != null) {

                if (pathEntries.size() != jsonPaths.size()) {
                    reason = "path entries array size of " +
                            Integer.toString(pathEntries.size());
                    return false;
                }

                for (List<FlowEntry> flowEntries : pathEntries) {
                    JsonArray jsonFlowEntries = jsonPaths.get(
                            jsonPathIndex++).asArray();

                    if (flowEntries.size() != jsonFlowEntries.size()) {
                        reason = "flow entries array size of " +
                                Integer.toString(pathEntries.size());

                        return false;
                    }

                    int jsonFlowEntryIndex = 0;
                    for (FlowEntry flow : flowEntries) {

                        JsonObject jsonFlow = jsonFlowEntries.get(
                                jsonFlowEntryIndex++).asObject();

                        String jsonId = jsonFlow.get(ID).asString();
                        String flowId = Long.toString(flow.id().value());
                        if (!jsonId.equals(flowId)) {
                            reason = ID + SPACE + flow.id();
                            return false;
                        }

                        // check application id
                        String jsonAppId = jsonFlow.get(APPID).asString();
                        if (!jsonAppId.equals(expectedAppId)) {
                            reason = APPID + SPACE + Short.toString(flow.appId());
                            return false;
                        }

                        // check device id
                        String jsonDeviceId =
                                jsonFlow.get(DEVICE_ID).asString();

                        if (!jsonDeviceId.equals(flow.deviceId().toString())) {
                            reason = DEVICE_ID + SPACE + flow.deviceId();
                            return false;
                        }

                        if (!checkFlowTreatment(flow, jsonFlow)) {
                            return false;
                        }

                        if (!checkFlowSelector(flow, jsonFlow)) {
                            return false;
                        }

                    }

                }
            } else if (pathEntries.size() != 0) {
                reason = "pathEntries array empty";
                return false;
            }

            return true;
        }

        // check treatment and instructions array.
        private boolean checkFlowTreatment(FlowEntry flow, JsonObject jsonFlow) {

            if (flow.treatment() != null) {
                JsonObject jsonTreatment =
                        jsonFlow.get(TREATMENT).asObject();
                JsonArray jsonInstructions =
                        jsonTreatment.get(INSTRUCTIONS).asArray();

                if (flow.treatment().immediate().size() !=
                        jsonInstructions.size()) {
                    reason = "instructions array size of " +
                            flow.treatment().immediate().size();

                    return false;
                }
                for (Instruction instruction :
                        flow.treatment().immediate()) {
                    boolean instructionFound = false;
                    for (int instructionIndex = 0;
                         instructionIndex < jsonInstructions.size();
                         instructionIndex++) {
                        String jsonType =
                                jsonInstructions.get(instructionIndex)
                                        .asObject().get(TYPE).asString();

                        String instructionType =
                                instruction.type().name();

                        if (jsonType.equals(instructionType)) {
                            instructionFound = true;
                        }
                    }
                    if (!instructionFound) {
                        reason = INSTRUCTIONS + SPACE + instruction;
                        return false;
                    }
                }
            }
            return true;
        }

        // check selector and criteria array.
        private boolean checkFlowSelector(FlowEntry flow, JsonObject jsonFlow) {

            if (flow.selector() != null) {
                JsonObject jsonTreatment =
                        jsonFlow.get(SELECTOR).asObject();

                JsonArray jsonCriteria =
                        jsonTreatment.get(CRITERIA).asArray();

                if (flow.selector().criteria().size() != jsonCriteria.size()) {
                    reason = CRITERIA + " array size of " +
                            Integer.toString(flow.selector().criteria().size());
                    return false;
                }
                for (Criterion criterion : flow.selector().criteria()) {
                    boolean criterionFound = false;

                    for (int criterionIndex = 0;
                         criterionIndex < jsonCriteria.size();
                         criterionIndex++) {
                        String jsonType =
                                jsonCriteria.get(criterionIndex)
                                        .asObject().get(TYPE).asString();
                        String criterionType = criterion.type().name();
                        if (jsonType.equals(criterionType)) {
                            criterionFound = true;
                        }
                    }
                    if (!criterionFound) {
                        reason = "criterion " + criterion;
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
     * Hamcrest matcher to check that an intent is represented properly in a JSON
     * array of intents.
     */
    public static class IntentJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Intent intent;
        private String reason = "";

        public IntentJsonArrayMatcher(Intent intentValue) {
            intent = intentValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean intentFound = false;
            final int expectedAttributes = 5;
            for (int jsonIntentIndex = 0; jsonIntentIndex < json.size();
                 jsonIntentIndex++) {

                final JsonObject jsonIntent = json.get(jsonIntentIndex).asObject();

                if (jsonIntent.names().size() != expectedAttributes) {
                    reason = "Found an intent with the wrong number of attributes";
                    return false;
                }

                final String jsonIntentId = jsonIntent.get("id").asString();
                if (jsonIntentId.equals(intent.id().toString())) {
                    intentFound = true;

                    //  We found the correct intent, check attribute values
                    assertThat(jsonIntent, matchesIntent(intent));
                }
            }
            if (!intentFound) {
                reason = "Intent with id " + intent.id().toString() + " not found";
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
     * Factory to allocate an intent array matcher.
     *
     * @param intent intent object we are looking for
     * @return matcher
     */
    private static IntentJsonArrayMatcher hasIntent(Intent intent) {
        return new IntentJsonArrayMatcher(intent);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        expect(mockIntentService.getIntents()).andReturn(intents).anyTimes();
        expect(mockIntentService.getIntentState(anyObject()))
                .andReturn(IntentState.INSTALLED)
                .anyTimes();
        // Register the services needed for the test
        final CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(IntentService.class, mockIntentService)
                        .add(FlowRuleService.class, mockFlowService)
                        .add(CodecService.class, codecService)
                        .add(CoreService.class, mockCoreService);

        BaseResource.setServiceDirectory(testDirectory);

        bindNewGenerator();
    }

    /**
     * Tears down and verifies test mocks and environment.
     */
    @After
    public void tearDownTest() {
        verify(mockIntentService);
    }

    /**
     * Tests the result of the rest api GET when there are no intents.
     */
    @Test
    public void testIntentsEmptyArray() {
        replay(mockIntentService);
        final WebTarget wt = target();
        final String response = wt.path("intents").request().get(String.class);
        assertThat(response, is("{\"intents\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when intents are defined.
     */
    @Test
    public void testIntentsArray() {
        replay(mockIntentService);

        final Intent intent1 = new MockIntent(1L, Collections.emptyList());
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent2 = new MockIntent(2L, resources);

        intents.add(intent1);
        intents.add(intent2);
        final WebTarget wt = target();
        final String response = wt.path("intents").request().get(String.class);
        assertThat(response, containsString("{\"intents\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("intents"));

        final JsonArray jsonIntents = result.get("intents").asArray();
        assertThat(jsonIntents, notNullValue());

        assertThat(jsonIntents, hasIntent(intent1));
        assertThat(jsonIntents, hasIntent(intent2));
    }

    /**
     * Tests the result of a rest api GET for a single intent.
     */
    @Test
    public void testIntentsSingle() {
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent = new MockIntent(3L, resources);

        intents.add(intent);

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0", APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0x0", APP_ID)))
                .andReturn(null)
                .anyTimes();
        replay(mockIntentService);
        expect(mockCoreService.getAppId(APP_ID.name()))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        final WebTarget wt = target();

        // Test get using key string
        final String response = wt.path("intents/" + APP_ID.name()
                + "/0").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, matchesIntent(intent));

        // Test get using numeric value
        final String responseNumeric = wt.path("intents/" + APP_ID.name()
                + "/0x0").request().get(String.class);
        final JsonObject resultNumeric = Json.parse(responseNumeric).asObject();
        assertThat(resultNumeric, matchesIntent(intent));
    }

    /**
     * Tests the result of a rest api GET for related flows for single intent.
     */
    @Test
    public void testRelatedFlowsForIntents() {
        List<FlowEntry> flowEntries = new ArrayList<>();
        flowEntries.add(flow1);
        flowEntries.add(flow2);
        List<List<FlowEntry>> paths = new ArrayList<>();
        paths.add(flowEntries);
        List<FlowRule> flowRules = new ArrayList<>();
        flowRules.add(flowRule1);
        flowRules.add(flowRule2);
        FlowRuleIntent flowRuleIntent = new FlowRuleIntent(
                APP_ID,
                flowRules,
                new HashSet<NetworkResource>());
        Intent intent = new MockIntent(3L);
        installableIntents.add(flowRuleIntent);
        intents.add(intent);

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0", APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0x0", APP_ID)))
                .andReturn(null)
                .anyTimes();
        expect(mockIntentService.getInstallableIntents(intent.key()))
                .andReturn(installableIntents)
                .anyTimes();
        replay(mockIntentService);

        expect(mockFlowService.getFlowEntries(deviceId1))
                .andReturn(flowEntries).anyTimes();
        replay(mockFlowService);

        expect(mockCoreService.getAppId(APP_ID.name()))
                .andReturn(APP_ID).anyTimes();
        expect(mockCoreService.getAppId(APP_ID.id()))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        final WebTarget wt = target();

        // Test get using key string
        final String response = wt.path("intents/relatedflows/" + APP_ID.name()
                + "/0").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, matchesRelatedFlowEntries(paths, APP_ID.name()));

        // Test get using numeric value
        final String responseNumeric = wt.path("intents/relatedflows/" + APP_ID.name()
                + "/0x0").request().get(String.class);
        final JsonObject resultNumeric = Json.parse(responseNumeric).asObject();
        assertThat(resultNumeric, matchesRelatedFlowEntries(paths, APP_ID.name()));
    }

    /**
     * Tests that a fetch of a non-existent intent object throws an exception.
     */
    @Test
    public void testBadGet() {

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(null)
                .anyTimes();
        replay(mockIntentService);

        WebTarget wt = target();
        try {
            wt.path("intents/0").request().get(String.class);
            fail("Fetch of non-existent intent did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(),
                    containsString("HTTP 404 Not Found"));
        }
    }

    /**
     * Tests creating an intent with POST.
     */
    @Test
    public void testPost() {
        ApplicationId testId = new DefaultApplicationId(2, "myApp");
        expect(mockCoreService.getAppId("myApp"))
                .andReturn(testId);
        replay(mockCoreService);

        mockIntentService.submit(anyObject());
        expectLastCall();
        replay(mockIntentService);

        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-intent.json");
        WebTarget wt = target();

        Response response = wt.path("intents")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/intents/myApp/"));
    }

    /**
     * Tests creating an intent with POST and illegal JSON.
     */
    @Test
    public void testBadPost() {
        replay(mockCoreService);
        replay(mockIntentService);

        String json = "this is invalid!";
        WebTarget wt = target();

        Response response = wt.path("intents")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(json));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_BAD_REQUEST));
    }

    /**
     * Tests removing an intent with DELETE.
     */
    @Test
    public void testRemove() {
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent = new MockIntent(3L, resources);
        final ApplicationId appId = new DefaultApplicationId(2, "app");
        IntentService fakeManager = new FakeIntentManager();

        expect(mockCoreService.getAppId("app"))
                .andReturn(appId).once();
        replay(mockCoreService);

        mockIntentService.withdraw(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();
        expect(mockIntentService.getIntent(Key.of(2, appId)))
                .andReturn(intent)
                .once();
        expect(mockIntentService.getIntent(Key.of("0x2", appId)))
                .andReturn(null)
                .once();

        mockIntentService.addListener(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();
        mockIntentService.removeListener(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();

        replay(mockIntentService);

        WebTarget wt = target();

        Response response = wt.path("intents/app/0x2")
                .request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
    }

    /**
     * Tests removal of a non existent intent with DELETE.
     */
    @Test
    public void testBadRemove() {
        final ApplicationId appId = new DefaultApplicationId(2, "app");

        expect(mockCoreService.getAppId("app"))
                .andReturn(appId).once();
        replay(mockCoreService);

        expect(mockIntentService.getIntent(Key.of(2, appId)))
                .andReturn(null)
                .once();
        expect(mockIntentService.getIntent(Key.of("0x2", appId)))
                .andReturn(null)
                .once();

        replay(mockIntentService);

        WebTarget wt = target();

        Response response = wt.path("intents/app/0x2")
                .request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
    }

}
