/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.web;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.glassfish.jersey.server.ResourceConfig;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingId;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingActionCodec;
import org.onosproject.mapping.codec.MappingAddressCodec;
import org.onosproject.mapping.codec.MappingEntryCodec;
import org.onosproject.mapping.codec.MappingInstructionCodec;
import org.onosproject.mapping.codec.MappingKeyCodec;
import org.onosproject.mapping.codec.MappingTreatmentCodec;
import org.onosproject.mapping.codec.MappingValueCodec;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.web.api.MappingsWebApplication;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;
import static org.onosproject.mapping.instructions.MappingInstructions.multicastPriority;
import static org.onosproject.mapping.instructions.MappingInstructions.multicastWeight;
import static org.onosproject.mapping.instructions.MappingInstructions.unicastPriority;
import static org.onosproject.mapping.instructions.MappingInstructions.unicastWeight;

/**
 * Unit tests for Mappings REST APIs.
 */
public class MappingsWebResourceTest extends ResourceTest {

    private static final String IPV4_STRING_1 = "1.2.3.4";
    private static final String IPV4_STRING_2 = "5.6.7.8";
    private static final String PORT_STRING = "32";
    private static final IpPrefix IPV4_PREFIX_1 =
            IpPrefix.valueOf(IPV4_STRING_1 + "/" + PORT_STRING);
    private static final IpPrefix IPV4_PREFIX_2 =
            IpPrefix.valueOf(IPV4_STRING_2 + "/" + PORT_STRING);

    private static final int UNICAST_WEIGHT = 1;
    private static final int UNICAST_PRIORITY = 1;
    private static final int MULTICAST_WEIGHT = 2;
    private static final int MULTICAST_PRIORITY = 2;

    private static final int DIFF_VALUE = 99;

    private static final String ID = "id";
    private static final String DATABASE = "database";

    private static final String PREFIX = "mappings";

    private final MappingService mockMappingService = createMock(MappingService.class);

    private final Map<DeviceId, Set<MappingEntry>> mappings = Maps.newHashMap();

    private final DeviceService mockDeviceService = createMock(DeviceService.class);
    private final DeviceId deviceId1 = DeviceId.deviceId("1");
    private final DeviceId deviceId2 = DeviceId.deviceId("2");
    private final Device device1 = new DefaultDevice(null, deviceId1, Device.Type.ROUTER,
            "", "", "", "", null);
    private final Device device2 = new DefaultDevice(null, deviceId2, Device.Type.ROUTER,
            "", "", "", "", null);

    private final MockMappingEntry mapping1 = new MockMappingEntry(deviceId1, 1);
    private final MockMappingEntry mapping2 = new MockMappingEntry(deviceId1, 2);

    private final MockMappingEntry mapping3 = new MockMappingEntry(deviceId2, 3);
    private final MockMappingEntry mapping4 = new MockMappingEntry(deviceId2, 4);

    private final Set<MappingEntry> mappingEntries = ImmutableSet.of(mapping1, mapping2, mapping3, mapping4);

    /**
     * Constructs a mappings web resource test instance.
     */
    public MappingsWebResourceTest() {
        super(ResourceConfig.forApplicationClass(MappingsWebApplication.class));
    }

    /**
     * Mock class for a mapping entry.
     */
    private static class MockMappingEntry implements MappingEntry {
        static final short UNIQUE_SHORT = 2;
        final DeviceId deviceId;
        MappingKey key;
        MappingValue value;
        final long baseValue;

        MockMappingEntry(DeviceId deviceId, long id) {
            this.deviceId = deviceId;
            this.baseValue = id * 100;
        }

        @Override
        public MappingId id() {
            final long id = baseValue + 11;
            return MappingId.valueOf(id);
        }

        @Override
        public short appId() {
            return UNIQUE_SHORT;
        }

        @Override
        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public MappingKey key() {
            return key;
        }

        @Override
        public MappingValue value() {
            return value;
        }

        @Override
        public MappingEntryState state() {
            return MappingEntryState.ADDED;
        }
    }

    /**
     * Populates some mappings used as testing data.
     */
    private void setupMockMappings() {
        MappingAddress address1 = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX_1);
        MappingAddress address2 = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX_2);

        MappingInstruction unicastWeight1 = unicastWeight(UNICAST_WEIGHT);
        MappingInstruction unicastPriority1 = unicastPriority(UNICAST_PRIORITY);
        MappingInstruction multicastWeight1 = multicastWeight(MULTICAST_WEIGHT);
        MappingInstruction multicastPriority1 = multicastPriority(MULTICAST_PRIORITY);

        MappingInstruction unicastWeight2 = unicastWeight(UNICAST_WEIGHT + DIFF_VALUE);
        MappingInstruction unicastPriority2 = unicastPriority(UNICAST_PRIORITY + DIFF_VALUE);
        MappingInstruction multicastWeight2 = multicastWeight(MULTICAST_WEIGHT + DIFF_VALUE);
        MappingInstruction multicastPriority2 = multicastPriority(MULTICAST_PRIORITY + DIFF_VALUE);

        MappingKey key1 = DefaultMappingKey.builder()
                .withAddress(address1)
                .build();

        MappingTreatment treatment1 = DefaultMappingTreatment.builder()
                .add(unicastWeight1)
                .add(unicastPriority1)
                .add(multicastWeight1)
                .add(multicastPriority1)
                .withAddress(address1)
                .build();

        MappingAction action1 = MappingActions.noAction();

        MappingValue value1 = DefaultMappingValue.builder()
                .add(treatment1)
                .withAction(action1)
                .build();

        MappingKey key2 = DefaultMappingKey.builder()
                .withAddress(address2)
                .build();

        MappingTreatment treatment2 = DefaultMappingTreatment.builder()
                .add(unicastWeight2)
                .add(unicastPriority2)
                .add(multicastWeight2)
                .add(multicastPriority2)
                .withAddress(address2)
                .build();

        MappingAction action2 = MappingActions.forward();

        MappingValue value2 = DefaultMappingValue.builder()
                .add(treatment2)
                .withAction(action2)
                .build();

        mapping1.key = key1;
        mapping2.key = key2;
        mapping3.key = key1;
        mapping4.key = key2;

        mapping1.value = value1;
        mapping2.value = value2;
        mapping3.value = value1;
        mapping4.value = value2;

        final Set<MappingEntry> mappings1 = Sets.newHashSet();
        mappings1.add(mapping1);
        mappings1.add(mapping2);

        final Set<MappingEntry> mappings2 = Sets.newHashSet();
        mappings2.add(mapping3);
        mappings2.add(mapping4);

        mappings.put(deviceId1, mappings1);
        mappings.put(deviceId2, mappings2);
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {

        // Register the services needed for the test
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(MappingEntry.class, new MappingEntryCodec());
        codecService.registerCodec(MappingAddress.class, new MappingAddressCodec());
        codecService.registerCodec(MappingInstruction.class, new MappingInstructionCodec());
        codecService.registerCodec(MappingAction.class, new MappingActionCodec());
        codecService.registerCodec(MappingTreatment.class, new MappingTreatmentCodec());
        codecService.registerCodec(MappingKey.class, new MappingKeyCodec());
        codecService.registerCodec(MappingValue.class, new MappingValueCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(MappingService.class, mockMappingService)
                        .add(DeviceService.class, mockDeviceService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up and verifies the mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockMappingService);
    }

    public static class MappingEntryJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {

        private final MappingEntry mapping;
        private String reason = "";

        MappingEntryJsonArrayMatcher(MappingEntry mappingValue) {
            mapping = mappingValue;
        }

        @Override
        protected boolean matchesSafely(JsonArray json) {

            boolean mappingFound = false;

            for (int jsonMappingIndex = 0; jsonMappingIndex < json.size();
                 jsonMappingIndex++) {

                final JsonObject jsonMapping = json.get(jsonMappingIndex).asObject();

                final String mappingId = Long.toString(mapping.id().value());
                final String jsonMappingId = jsonMapping.get(ID).asString();
                if (jsonMappingId.equals(mappingId)) {
                    mappingFound = true;
                    assertThat(jsonMapping, matchesMapping(mapping));
                }
            }

            if (!mappingFound) {
                reason = "Mapping with id " + mapping.id().toString() + " not found";
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
     * Hamcrest matcher for mapping entry.
     */
    public static final class MappingEntryJsonMatcher
            extends TypeSafeMatcher<JsonObject> {

        private final MappingEntry mappingEntry;
        private String reason = "";

        /**
         * A default constructor.
         *
         * @param mappingEntry mapping entry
         */
        private MappingEntryJsonMatcher(MappingEntry mappingEntry) {
            this.mappingEntry = mappingEntry;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonObject) {
            // check mapping id
            final String jsonId = jsonObject.get("id").asString();
            final String mappingId = Long.toString(mappingEntry.id().value());

            if (!jsonId.equals(mappingId)) {
                reason = "id " + mappingEntry.id().toString();
                return false;
            }

            // check device id
            final String jsonDeviceId = jsonObject.get("deviceId").asString();
            final String deviceId = mappingEntry.deviceId().toString();
            if (!jsonDeviceId.equals(deviceId)) {
                reason = "deviceId " + mappingEntry.deviceId();
                return false;
            }

            // check state
            final String jsonState = jsonObject.get("state").asString();
            final String state = mappingEntry.state().name();
            if (!jsonState.equals(state)) {
                reason = "state " + mappingEntry.state().name();
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
     * Factory to allocate a mapping matcher.
     *
     * @param mapping mapping object we are looking for
     * @return matcher
     */
    private static MappingEntryJsonMatcher matchesMapping(MappingEntry mapping) {
        return new MappingEntryJsonMatcher(mapping);
    }

    /**
     * Factory to allocate a mapping array matcher.
     *
     * @param mapping mapping object we are looking for
     * @return matcher
     */
    private static MappingEntryJsonArrayMatcher hasMapping(MappingEntry mapping) {
        return new MappingEntryJsonArrayMatcher(mapping);
    }

    /**
     * Tests the result of the rest api GET when there are no mappings.
     */
    @Test
    public void testMappingsEmptyArray() {
        expect(mockMappingService.getAllMappingEntries(anyObject()))
                .andReturn(null).anyTimes();
        replay(mockMappingService);
        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/" + DATABASE).request().get(String.class);
        assertThat(response, is("{\"mappings\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are active mappings.
     */
    @Test
    public void testMappingsPopulateArray() {
        setupMockMappings();
        expect(mockMappingService.getAllMappingEntries(anyObject()))
                .andReturn(mappingEntries).once();
        replay(mockMappingService);
        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/" + DATABASE).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("mappings"));
        final JsonArray jsonMappings = result.get("mappings").asArray();
        assertThat(jsonMappings, notNullValue());
        assertThat(jsonMappings, hasMapping(mapping1));
        assertThat(jsonMappings, hasMapping(mapping2));
        assertThat(jsonMappings, hasMapping(mapping3));
        assertThat(jsonMappings, hasMapping(mapping4));
    }

    /**
     * Tests the result of the rest api GET with a device ID when there are
     * no mappings.
     */
    @Test
    public void testMappingsByDevIdEmptyArray() {
        expect(mockDeviceService.getDevice(deviceId1)).andReturn(device1);
        expect(mockMappingService.getMappingEntries(MAP_DATABASE, deviceId1))
                .andReturn(null).anyTimes();
        replay(mockDeviceService);
        replay(mockMappingService);

        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/" + deviceId1 + "/" + DATABASE)
                .request().get(String.class);
        assertThat(response, is("{\"mappings\":[]}"));
    }

    /**
     * Tests the result of the rest api GET with a device ID when there are
     * active mappings.
     */
    @Test
    public void testMappingsByDevIdPopulateArray() {
        setupMockMappings();
        expect(mockDeviceService.getDevice(deviceId2)).andReturn(device2);
        expect(mockMappingService.getMappingEntries(MAP_DATABASE, deviceId2))
                .andReturn(mappings.get(deviceId2)).once();
        replay(mockDeviceService);
        replay(mockMappingService);

        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/" + deviceId2 + "/" + DATABASE)
                .request().get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("mappings"));
        final JsonArray jsonMappings = result.get("mappings").asArray();
        assertThat(jsonMappings, notNullValue());
        assertThat(jsonMappings, hasMapping(mapping3));
        assertThat(jsonMappings, hasMapping(mapping4));
    }
}
