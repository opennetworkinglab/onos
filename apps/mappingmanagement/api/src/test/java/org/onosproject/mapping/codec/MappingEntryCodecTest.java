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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.DefaultMapping;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingCodecRegistrator;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEntry.MappingEntryState;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingKeyCodecTest.MappingKeyJsonMatcher;
import org.onosproject.mapping.codec.MappingValueCodecTest.MappingValueJsonMatcher;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for MappingEntryCodec.
 */
public class MappingEntryCodecTest {

    private static final String IPV4_STRING = "1.2.3.4";
    private static final String PORT_STRING = "32";
    private static final IpPrefix IPV4_PREFIX =
            IpPrefix.valueOf(IPV4_STRING + "/" + PORT_STRING);

    private static final int UNICAST_WEIGHT = 1;
    private static final int UNICAST_PRIORITY = 1;
    private static final int MULTICAST_WEIGHT = 2;
    private static final int MULTICAST_PRIORITY = 2;

    private static final long ID = 1L;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("lisp:5.6.7.8");
    private static final MappingEntryState STATE = MappingEntryState.ADDED;

    private CodecContext context;
    private JsonCodec<MappingEntry> entryCodec;
    private MappingCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping entry codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);
        entryCodec = context.codec(MappingEntry.class);
        assertThat(entryCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a mapping entry object.
     */
    @Test
    public void testMappingEntryEncode() {
        MappingAddress address = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX);

        MappingInstruction unicastWeight = MappingInstructions.unicastWeight(UNICAST_WEIGHT);
        MappingInstruction unicastPriority = MappingInstructions.unicastPriority(UNICAST_PRIORITY);
        MappingInstruction multicastWeight = MappingInstructions.multicastWeight(MULTICAST_WEIGHT);
        MappingInstruction multicastPriority = MappingInstructions.multicastPriority(MULTICAST_PRIORITY);

        MappingKey key = DefaultMappingKey.builder()
                .withAddress(address)
                .build();

        MappingTreatment treatment = DefaultMappingTreatment.builder()
                .add(unicastWeight)
                .add(unicastPriority)
                .add(multicastWeight)
                .add(multicastPriority)
                .withAddress(address)
                .build();

        MappingAction action = MappingActions.noAction();

        MappingValue value = DefaultMappingValue.builder()
                .add(treatment)
                .withAction(action)
                .build();

        Mapping mapping = DefaultMapping.builder()
                                .withId(ID)
                                .forDevice(DEVICE_ID)
                                .withKey(key)
                                .withValue(value)
                                .build();

        MappingEntry entry = new DefaultMappingEntry(mapping, STATE);

        ObjectNode entryJson = entryCodec.encode(entry, context);
        assertThat(entryJson, MappingEntryJsonMatcher.matchesMappingEntry(entry));
    }

    /**
     * Hamcrest matcher for mapping entry.
     */
    public static final class MappingEntryJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MappingEntry mappingEntry;

        /**
         * A default constructor.
         *
         * @param mappingEntry mapping entry
         */
        private MappingEntryJsonMatcher(MappingEntry mappingEntry) {
            this.mappingEntry = mappingEntry;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check mapping id
            final JsonNode jsonIdNode = jsonNode.get(MappingEntryCodec.ID);
            final long jsonId = jsonIdNode.asLong();
            final long id = mappingEntry.id().value();
            if (jsonId != id) {
                description.appendText("mapping id was " + id);
                return false;
            }

            // check device id
            final JsonNode jsonDeviceIdNode = jsonNode.get(MappingEntryCodec.DEVICE_ID);
            final String jsonDeviceId = jsonDeviceIdNode.textValue();
            final String deviceId = mappingEntry.deviceId().toString();
            if (!jsonDeviceId.equals(deviceId)) {
                description.appendText("device id was " + deviceId);
                return false;
            }

            // check state
            final JsonNode jsonStateNode = jsonNode.get(MappingEntryCodec.STATE);
            final String jsonState = jsonStateNode.textValue();
            final String state = mappingEntry.state().name();
            if (!jsonState.equals(state)) {
                description.appendText("state was " + state);
                return false;
            }

            // check mapping key
            final JsonNode jsonKeyNode = jsonNode.get(MappingEntryCodec.KEY);
            assertThat(jsonKeyNode, MappingKeyJsonMatcher.matchesMappingKey(mappingEntry.key()));

            // check mapping value
            final JsonNode jsonValueNode = jsonNode.get(MappingEntryCodec.VALUE);
            assertThat(jsonValueNode, MappingValueJsonMatcher.matchesMappingValue(mappingEntry.value()));

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mappingEntry.toString());
        }

        /**
         * Factory to allocate a mapping entry matcher.
         *
         * @param entry mapping entry object we are looking for
         * @return matcher
         */
        public static MappingEntryJsonMatcher matchesMappingEntry(MappingEntry entry) {
            return new MappingEntryJsonMatcher(entry);
        }
    }
}
