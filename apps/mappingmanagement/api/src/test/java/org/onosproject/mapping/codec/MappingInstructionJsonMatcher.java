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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;

/**
 * Hamcrest matcher for mapping instructions.
 */
public final class MappingInstructionJsonMatcher
        extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final MappingInstruction instruction;

    /**
     * A default constructor.
     *
     * @param instruction mapping instruction
     */
    private MappingInstructionJsonMatcher(MappingInstruction instruction) {
        this.instruction = instruction;
    }

    /**
     * Matches the contents of an unicast weight mapping instruction.
     *
     * @param node        JSON instruction to match
     * @param description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchUnicastWeightInstruction(JsonNode node,
                                                  Description description) {
        UnicastMappingInstruction.WeightMappingInstruction instructionToMatch =
                (UnicastMappingInstruction.WeightMappingInstruction) instruction;
        final String jsonSubtype = node.get(MappingInstructionCodec.SUBTYPE).textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = node.get(MappingInstructionCodec.TYPE).textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonWeight = node.get(MappingInstructionCodec.UNICAST_WEIGHT).intValue();
        final int weight = instructionToMatch.weight();
        if (jsonWeight != weight) {
            description.appendText("Unicast weight was " + jsonWeight);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of an unicast priority mapping instruction.
     *
     * @param node        JSON instruction to match
     * @param description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchUnicastPriorityInstruction(JsonNode node,
                                                    Description description) {
        UnicastMappingInstruction.PriorityMappingInstruction instructionToMatch =
                (UnicastMappingInstruction.PriorityMappingInstruction) instruction;
        final String jsonSubtype = node.get(MappingInstructionCodec.SUBTYPE).textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = node.get(MappingInstructionCodec.TYPE).textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonPriority = node.get(MappingInstructionCodec.UNICAST_PRIORITY).intValue();
        final int priority = instructionToMatch.priority();
        if (jsonPriority != priority) {
            description.appendText("Unicast priority was " + jsonPriority);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a multicast weight mapping instruction.
     *
     * @param node        JSON instruction to match
     * @param description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchMulticastWeightInstruction(JsonNode node,
                                                    Description description) {
        MulticastMappingInstruction.WeightMappingInstruction instructionToMatch =
                (MulticastMappingInstruction.WeightMappingInstruction) instruction;
        final String jsonSubtype = node.get(MappingInstructionCodec.SUBTYPE).textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = node.get(MappingInstructionCodec.TYPE).textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonWeight = node.get(MappingInstructionCodec.MULTICAST_WEIGHT).intValue();
        final int weight = instructionToMatch.weight();
        if (jsonWeight != weight) {
            description.appendText("Multicast weight was " + jsonWeight);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a multicast priority mapping instruction.
     *
     * @param node        JSON instruction to match
     * @param description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchMulticastPriorityInstruction(JsonNode node,
                                                      Description description) {
        MulticastMappingInstruction.PriorityMappingInstruction instructionToMatch =
                (MulticastMappingInstruction.PriorityMappingInstruction) instruction;
        final String jsonSubtype = node.get(MappingInstructionCodec.SUBTYPE).textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = node.get(MappingInstructionCodec.TYPE).textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonPriority = node.get(MappingInstructionCodec.MULTICAST_PRIORITY).intValue();
        final int priority = instructionToMatch.priority();
        if (jsonPriority != priority) {
            description.appendText("Multicast priority was " + jsonPriority);
            return false;
        }

        return true;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check type
        final JsonNode jsonTypeNode = jsonNode.get(MappingInstructionCodec.TYPE);
        final String jsonType = jsonTypeNode.textValue();
        final String type = instruction.type().name();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + type);
            return false;
        }

        if (instruction instanceof UnicastMappingInstruction.WeightMappingInstruction) {
            return matchUnicastWeightInstruction(jsonNode, description);
        } else if (instruction instanceof UnicastMappingInstruction.PriorityMappingInstruction) {
            return matchUnicastPriorityInstruction(jsonNode, description);
        } else if (instruction instanceof MulticastMappingInstruction.WeightMappingInstruction) {
            return matchMulticastWeightInstruction(jsonNode, description);
        } else if (instruction instanceof MulticastMappingInstruction.PriorityMappingInstruction) {
            return matchMulticastPriorityInstruction(jsonNode, description);
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(instruction.toString());
    }

    /**
     * Factory to allocate a mapping instruction matcher.
     *
     * @param instruction instruction object we are looking for
     * @return matcher
     */
    public static MappingInstructionJsonMatcher matchesInstruction(
            MappingInstruction instruction) {
        return new MappingInstructionJsonMatcher(instruction);
    }
}
