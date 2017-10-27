/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.meter.MeterId;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for traffic treatment codec.
 */
public class TrafficTreatmentCodecTest {

    private MockCodecContext context;
    private JsonCodec<TrafficTreatment> trafficTreatmentCodec;

    @Before
    public void setUp() {
        context = new MockCodecContext();
        trafficTreatmentCodec = context.codec(TrafficTreatment.class);
        assertThat(trafficTreatmentCodec, notNullValue());
    }

    /**
     * Tests encoding of a traffic treatment object.
     */
    @Test
    public void testTrafficTreatmentEncode() {

        Instruction output = Instructions.createOutput(PortNumber.portNumber(0));
        Instruction modL2Src = Instructions.modL2Src(MacAddress.valueOf("11:22:33:44:55:66"));
        Instruction modL2Dst = Instructions.modL2Dst(MacAddress.valueOf("44:55:66:77:88:99"));
        MeterId meterId = MeterId.meterId(1);
        Instruction meter = Instructions.meterTraffic(meterId);
        Instruction transition = Instructions.transition(1);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        TrafficTreatment treatment = tBuilder
                .add(output)
                .add(modL2Src)
                .add(modL2Dst)
                .add(meter)
                .add(transition)
                .build();

        ObjectNode treatmentJson = trafficTreatmentCodec.encode(treatment, context);
        assertThat(treatmentJson, TrafficTreatmentJsonMatcher.matchesTrafficTreatment(treatment));
    }

    /**
     * Tests decoding of a traffic treatment JSON object.
     */
    @Test
    public void testTrafficTreatmentDecode() throws IOException {
        TrafficTreatment treatment = getTreatment("TrafficTreatment.json");

        List<Instruction> insts = treatment.immediate();
        assertThat(insts.size(), is(2));

        ImmutableSet<String> types = ImmutableSet.of("OUTPUT", "L2MODIFICATION");
        assertThat(types.contains(insts.get(0).type().name()), is(true));
        assertThat(types.contains(insts.get(1).type().name()), is(true));
    }

    public static final class TrafficTreatmentJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final TrafficTreatment trafficTreatment;

        private TrafficTreatmentJsonMatcher(TrafficTreatment trafficTreatment) {
            this.trafficTreatment = trafficTreatment;
        }

        /**
         * Filtered out the meter and table transition instructions.
         *
         * @param node JSON node
         * @return filtered JSON node
         */
        private int filteredSize(JsonNode node) {
            int counter = 0;
            for (int idx = 0; idx < node.size(); idx++) {
                String type = node.get(idx).get("type").asText();
                if (!"METER".equals(type) && !"TABLE".equals(type)) {
                    counter++;
                }
            }
            return counter;
        }

        private JsonNode getInstNode(JsonNode node, String name) {
            for (int idx = 0; idx < node.size(); idx++) {
                String type = node.get(idx).get("type").asText();
                if (type.equals(name)) {
                    return node.get(idx);
                }
            }
            return null;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check instructions
            final JsonNode jsonInstructions = jsonNode.get("instructions");

            if (trafficTreatment.immediate().size() != filteredSize(jsonInstructions)) {
                description.appendText("instructions array size of " +
                        Integer.toString(trafficTreatment.immediate().size()));
                return false;
            }

            for (final Instruction instruction : trafficTreatment.immediate()) {
                boolean instructionFound = false;
                for (int instructionIndex = 0; instructionIndex < jsonInstructions.size();
                     instructionIndex++) {
                    final String jsonType =
                            jsonInstructions.get(instructionIndex).get("type").asText();
                    final String instructionType = instruction.type().name();
                    if (jsonType.equals(instructionType)) {
                        instructionFound = true;
                    }
                }
                if (!instructionFound) {
                    description.appendText("instruction " + instruction.toString());
                    return false;
                }
            }

            // check metered
            JsonNode meterNode = getInstNode(jsonInstructions, "METER");
            String jsonMeterId = meterNode != null ? meterNode.get("meterId").asText() : null;
            if (trafficTreatment.metered() != null && !trafficTreatment.meters().isEmpty()) {
                Optional<Instructions.MeterInstruction> optional = trafficTreatment.meters().stream().filter(
                        meterInstruction -> StringUtils.equals(jsonMeterId, meterInstruction.meterId().toString()))
                        .findAny();
                if (!optional.isPresent()) {
                    description.appendText("meter id was " + jsonMeterId);
                    return false;
                }
            }

            // check table transition
            JsonNode tableNode = getInstNode(jsonInstructions, "TABLE");
            String jsonTableId = tableNode != null ? tableNode.get("tableId").asText() : null;
            if (trafficTreatment.tableTransition() != null) {
                String tableId = trafficTreatment.tableTransition().tableId().toString();
                if (!StringUtils.equals(jsonTableId, tableId)) {
                    description.appendText("table id was " + jsonMeterId);
                    return false;
                }
            }

            // TODO: check deferred

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(trafficTreatment.toString());
        }

        /**
         * Factory to allocate a traffic treatment.
         *
         * @param trafficTreatment traffic treatment object we are looking for
         * @return matcher
         */
        static TrafficTreatmentJsonMatcher matchesTrafficTreatment(TrafficTreatment trafficTreatment) {
            return new TrafficTreatmentJsonMatcher(trafficTreatment);
        }
    }

    /**
     * Reads in a traffic treatment from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded trafficTreatment
     * @throws IOException if processing the resource fails
     */
    private TrafficTreatment getTreatment(String resourceName) throws IOException {
        InputStream jsonStream = TrafficTreatmentCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        TrafficTreatment treatment = trafficTreatmentCodec.decode((ObjectNode) json, context);
        assertThat(treatment, notNullValue());
        return treatment;
    }
}
