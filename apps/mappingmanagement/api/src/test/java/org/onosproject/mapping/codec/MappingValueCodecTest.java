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
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for MappingValueCodec.
 */
public class MappingValueCodecTest {

    private static final String TREATMENTS = "treatments";
    private static final String ADDRESS = "address";
    private static final String IPV4_STRING = "1.2.3.4";
    private static final String PORT_STRING = "32";
    private static final IpPrefix IPV4_PREFIX =
            IpPrefix.valueOf(IPV4_STRING + "/" + PORT_STRING);

    private static final int UNICAST_WEIGHT = 1;
    private static final int UNICAST_PRIORITY = 1;
    private static final int MULTICAST_WEIGHT = 2;
    private static final int MULTICAST_PRIORITY = 2;

    private CodecContext context;
    private JsonCodec<MappingValue> valueCodec;
    private MappingCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping value codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);
        valueCodec = context.codec(MappingValue.class);
        assertThat(valueCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a mapping value object.
     */
    @Test
    public void testMappingValueEncode() {
        MappingInstruction unicastWeight = MappingInstructions.unicastWeight(UNICAST_WEIGHT);
        MappingInstruction unicastPriority = MappingInstructions.unicastPriority(UNICAST_PRIORITY);
        MappingInstruction multicastWeight = MappingInstructions.multicastWeight(MULTICAST_WEIGHT);
        MappingInstruction multicastPriority = MappingInstructions.multicastPriority(MULTICAST_PRIORITY);

        MappingAddress address = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX);

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

        ObjectNode valueJson = valueCodec.encode(value, context);
        assertThat(valueJson, MappingValueJsonMatcher.matchesMappingValue(value));
    }

    /**
     * Tests decoding of a mapping value JSON object.
     */
    @Test
    public void testMappingValueDecode() throws IOException {
        MappingValue value = getValue("MappingValue.json");
        assertThat(value.treatments().get(0).address().toString(),
                            is("IPV4:" + IPV4_STRING + "/" + PORT_STRING));
    }


    /**
     * Hamcrest matcher for mapping value.
     */
    public static final class MappingValueJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MappingValue mappingValue;

        /**
         * A default constructor.
         *
         * @param mappingValue mapping value
         */
        private MappingValueJsonMatcher(MappingValue mappingValue) {
            this.mappingValue = mappingValue;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check mapping treatments
            final JsonNode jsonTreatments = jsonNode.get(TREATMENTS);
            if (mappingValue.treatments().size() != jsonTreatments.size()) {
                description.appendText("mapping treatments array size of " +
                        Integer.toString(mappingValue.treatments().size()));
                return false;
            }

            for (final MappingTreatment treatment : mappingValue.treatments()) {
                boolean treatmentFound = false;
                for (int treatmentIdx = 0; treatmentIdx < jsonTreatments.size();
                        treatmentIdx++) {
                    final String jsonAddress =
                            jsonTreatments.get(treatmentIdx).get(ADDRESS)
                                    .get(MappingAddressCodec.IPV4).textValue();
                    final String address = treatment.address().toString();
                    if (address.contains(jsonAddress)) {
                        treatmentFound = true;
                    }
                }

                if (!treatmentFound) {
                    description.appendText("mapping treatment " + treatment.toString());
                    return false;
                }
            }

            // check mapping action
            final JsonNode jsonActionNode = jsonNode.get(MappingValueCodec.ACTION);

            assertThat(jsonActionNode, MappingActionJsonMatcher.matchesAction(mappingValue.action()));

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mappingValue.toString());
        }

        /**
         * Factory to allocate a mapping value.
         *
         * @param mappingValue mapping value object we are looking for
         * @return matcher
         */
        static MappingValueJsonMatcher matchesMappingValue(MappingValue mappingValue) {
            return new MappingValueJsonMatcher(mappingValue);
        }
    }

    /**
     * Reads in a mapping value from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingKey
     * @throws IOException if processing the resource fails
     */
    private MappingValue getValue(String resourceName) throws IOException {
        InputStream jsonStream = MappingValueCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingValue value = valueCodec.decode((ObjectNode) json, context);
        assertThat(value, notNullValue());
        return value;
    }
}
