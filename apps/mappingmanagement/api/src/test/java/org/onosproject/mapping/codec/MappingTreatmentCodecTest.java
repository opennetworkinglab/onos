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
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for MappingTreatmentCodec.
 */
public class MappingTreatmentCodecTest {

    private static final String INSTRUCTIONS = "instructions";
    private static final String TYPE = "type";

    private static final int UNICAST_WEIGHT = 1;
    private static final int UNICAST_PRIORITY = 1;
    private static final int MULTICAST_WEIGHT = 2;
    private static final int MULTICAST_PRIORITY = 2;
    private static final String ASN_NUMBER = "ASN2000";

    private CodecContext context;
    private JsonCodec<MappingTreatment> treatmentCodec;
    private MappingCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping action codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);
        treatmentCodec = context.codec(MappingTreatment.class);
        assertThat(treatmentCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a mapping treatment object.
     */
    @Test
    public void testMappingTreatmentEncode() {
        MappingInstruction unicastWeight = MappingInstructions.unicastWeight(UNICAST_WEIGHT);
        MappingInstruction unicastPriority = MappingInstructions.unicastPriority(UNICAST_PRIORITY);
        MappingInstruction multicastWeight = MappingInstructions.multicastWeight(MULTICAST_WEIGHT);
        MappingInstruction multicastPriority = MappingInstructions.multicastPriority(MULTICAST_PRIORITY);

        MappingAddress address = MappingAddresses.asMappingAddress(ASN_NUMBER);

        MappingTreatment treatment = DefaultMappingTreatment.builder()
                                                .add(unicastWeight)
                                                .add(unicastPriority)
                                                .add(multicastWeight)
                                                .add(multicastPriority)
                                                .withAddress(address)
                                                .build();

        ObjectNode treatmentJson = treatmentCodec.encode(treatment, context);
        assertThat(treatmentJson, MappingTreatmentJsonMatcher.matchesMappingTreatment(treatment));
    }

    /**
     * Tests decoding of a mapping treatment JSON object.
     */
    @Test
    public void testMappingTreatmentDecode() throws IOException {
        MappingTreatment treatment = getTreatment("MappingTreatment.json");

        List<MappingInstruction> insts = treatment.instructions();
        assertThat(insts.size(), is(2));

        ImmutableSet<String> types = ImmutableSet.of("UNICAST", "MULTICAST");
        assertThat(types.contains(insts.get(0).type().name()), is(true));
        assertThat(types.contains(insts.get(1).type().name()), is(true));
    }

    /**
     * Hamcrest matcher for mapping treatment.
     */
    public static final class MappingTreatmentJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MappingTreatment mappingTreatment;

        /**
         * A default constructor.
         *
         * @param mappingTreatment mapping treatment
         */
        private MappingTreatmentJsonMatcher(MappingTreatment mappingTreatment) {
            this.mappingTreatment = mappingTreatment;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check mapping instruction
            final JsonNode jsonInstructions = jsonNode.get(INSTRUCTIONS);
            if (mappingTreatment.instructions().size() != jsonInstructions.size()) {
                description.appendText("mapping instructions array size of " +
                        Integer.toString(mappingTreatment.instructions().size()));
                return false;
            }

            for (final MappingInstruction instruction : mappingTreatment.instructions()) {
                boolean instructionFound = false;
                for (int instructionIdx = 0; instructionIdx < jsonInstructions.size();
                        instructionIdx++) {
                    final String jsonType =
                            jsonInstructions.get(instructionIdx).get(TYPE).asText();
                    final String instructionType = instruction.type().name();
                    if (jsonType.equals(instructionType)) {
                        instructionFound = true;
                    }
                }
                if (!instructionFound) {
                    description.appendText("mapping instruction " + instruction.toString());
                    return false;
                }
            }

            // check address
            final JsonNode jsonAddressNode = jsonNode.get(MappingKeyCodec.ADDRESS);

            assertThat(jsonAddressNode,
                    MappingAddressJsonMatcher.matchesMappingAddress(mappingTreatment.address()));

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mappingTreatment.toString());
        }

        /**
         * Factory to allocate a mapping treatment.
         *
         * @param mappingTreatment mapping treatment object we are looking for
         * @return matcher
         */
        static MappingTreatmentJsonMatcher matchesMappingTreatment(MappingTreatment mappingTreatment) {
            return new MappingTreatmentJsonMatcher(mappingTreatment);
        }
    }

    /**
     * Reads in a mapping treatment from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingTreatment
     * @throws IOException if processing the resource fails
     */
    private MappingTreatment getTreatment(String resourceName) throws IOException {
        InputStream jsonStream = MappingTreatmentCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingTreatment treatment = treatmentCodec.decode((ObjectNode) json, context);
        assertThat(treatment, notNullValue());
        return treatment;
    }
}
