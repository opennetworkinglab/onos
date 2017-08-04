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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.mapping.codec.MappingInstructionJsonMatcher.matchesInstruction;

/**
 * Unit tests for MappingInstructionCodec.
 */
public class MappingInstructionCodecTest {

    private CodecContext context;
    private JsonCodec<MappingInstruction> instructionCodec;
    private MappingCodecRegistrator registrator;

    private static final int UNICAST_WEIGHT = 1;
    private static final int UNICAST_PRIORITY = 1;
    private static final int MULTICAST_WEIGHT = 2;
    private static final int MULTICAST_PRIORITY = 2;

    private static final String UNICAST_TYPE_STRING = "UNICAST";
    private static final String WEIGHT_SUBTYPE_STRING = "WEIGHT";

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping instruction codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);

        instructionCodec = context.codec(MappingInstruction.class);
        assertThat(instructionCodec, notNullValue());
    }

    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests the encoding of unicast weight instruction.
     */
    @Test
    public void unicastWeightInstrutionTest() {
        final UnicastMappingInstruction.WeightMappingInstruction instruction =
                (UnicastMappingInstruction.WeightMappingInstruction)
                        MappingInstructions.unicastWeight(UNICAST_WEIGHT);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of unicast priority instruction.
     */
    @Test
    public void unicastPriorityInstructionTest() {
        final UnicastMappingInstruction.PriorityMappingInstruction instruction =
                (UnicastMappingInstruction.PriorityMappingInstruction)
                MappingInstructions.unicastPriority(UNICAST_PRIORITY);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of multicast weight instruction.
     */
    @Test
    public void multicastWeightInstructionTest() {
        final MulticastMappingInstruction.WeightMappingInstruction instruction =
                (MulticastMappingInstruction.WeightMappingInstruction)
                        MappingInstructions.multicastWeight(MULTICAST_WEIGHT);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of multicast priority instruction.
     */
    @Test
    public void multicastPriorityInstructionTest() {
        final MulticastMappingInstruction.PriorityMappingInstruction instruction =
                (MulticastMappingInstruction.PriorityMappingInstruction)
                        MappingInstructions.multicastPriority(MULTICAST_PRIORITY);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the decoding of mapping instruction from JSON object.
     *
     * @throws IOException if processing the resource fails
     */
    @Test
    public void testMappingInstructionDecode() throws IOException {
        UnicastMappingInstruction instruction = (UnicastMappingInstruction) getInstruction("MappingInstruction.json");
        assertThat(instruction.type().toString(), is(UNICAST_TYPE_STRING));
        assertThat(instruction.subtype().toString(), is(WEIGHT_SUBTYPE_STRING));
    }

    /**
     * Reads in a mapping instruction from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingInstruction
     * @throws IOException if processing the resource fails
     */
    private MappingInstruction getInstruction(String resourceName) throws IOException {
        InputStream jsonStream = MappingInstructionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingInstruction instruction = instructionCodec.decode((ObjectNode) json, context);
        assertThat(instruction, notNullValue());
        return instruction;
    }
}
