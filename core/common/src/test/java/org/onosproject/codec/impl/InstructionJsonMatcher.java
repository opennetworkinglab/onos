/*
 * Copyright 2015 Open Networking Laboratory
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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.flow.instructions.Instruction;

import com.fasterxml.jackson.databind.JsonNode;

import static org.onosproject.net.flow.instructions.Instructions.*;
import static org.onosproject.net.flow.instructions.L0ModificationInstruction.*;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.*;
import static org.onosproject.net.flow.instructions.L3ModificationInstruction.*;

/**
 * Hamcrest matcher for instructions.
 */
public final class InstructionJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Instruction instruction;

    private InstructionJsonMatcher(Instruction instructionValue) {
        instruction = instructionValue;
    }

    /**
     * Matches the contents of a push header instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchPushHeaderInstruction(JsonNode instructionJson,
                                               Description description) {
        PushHeaderInstructions instructionToMatch =
                (PushHeaderInstructions) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final JsonNode ethJson = instructionJson.get("ethernetType");
        if (ethJson == null) {
            description.appendText("ethernetType was not null");
            return false;
        }

        if (instructionToMatch.ethernetType().toShort() != ethJson.asInt()) {
            description.appendText("ethernetType was " + ethJson);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of an output instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchOutputInstruction(JsonNode instructionJson,
                                           Description description) {
        OutputInstruction instructionToMatch = (OutputInstruction) instruction;

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final long jsonPort = instructionJson.get("port").asLong();
        if (instructionToMatch.port().toLong() != jsonPort) {
            description.appendText("port was " + jsonPort);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod lambda instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModLambdaInstruction(JsonNode instructionJson,
                                              Description description) {
        ModLambdaInstruction instructionToMatch =
                (ModLambdaInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final long jsonLambda = instructionJson.get("lambda").shortValue();
        if (instructionToMatch.lambda() != jsonLambda) {
            description.appendText("lambda was " + jsonLambda);
            return false;
        }

        return true;
    }

    /**
     * Matches teh contents of a mod OCh singal instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents matches, false otherwise
     */
    private boolean matchModOchSingalInstruction(JsonNode instructionJson,
                                                 Description description) {
        ModOchSignalInstruction instructionToMatch =
                (ModOchSignalInstruction) instruction;

        String jsonSubType = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubType)) {
            description.appendText("subtype was " + jsonSubType);
            return false;
        }

        String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        String jsonGridType = instructionJson.get("gridType").textValue();
        if (!instructionToMatch.lambda().gridType().name().equals(jsonGridType)) {
            description.appendText("gridType was " + jsonGridType);
            return false;
        }

        String jsonChannelSpacing = instructionJson.get("channelSpacing").textValue();
        if (!instructionToMatch.lambda().channelSpacing().name().equals(jsonChannelSpacing)) {
            description.appendText("channelSpacing was " + jsonChannelSpacing);
            return false;
        }

        int jsonSpacingMultiplier = instructionJson.get("spacingMultiplier").intValue();
        if (instructionToMatch.lambda().spacingMultiplier() != jsonSpacingMultiplier) {
            description.appendText("spacingMultiplier was " + jsonSpacingMultiplier);
            return false;
        }

        int jsonSlotGranularity = instructionJson.get("slotGranularity").intValue();
        if (instructionToMatch.lambda().slotGranularity() != jsonSlotGranularity) {
            description.appendText("slotGranularity was " + jsonSlotGranularity);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod Ethernet instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModEtherInstruction(JsonNode instructionJson,
                                             Description description) {
        ModEtherInstruction instructionToMatch =
                (ModEtherInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final String jsonMac = instructionJson.get("mac").textValue();
        final String mac = instructionToMatch.mac().toString();
        if (!mac.equals(jsonMac)) {
            description.appendText("mac was " + jsonMac);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod vlan id instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModVlanIdInstruction(JsonNode instructionJson,
                                           Description description) {
        ModVlanIdInstruction instructionToMatch =
                (ModVlanIdInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final short jsonVlanId = instructionJson.get("vlanId").shortValue();
        final short vlanId = instructionToMatch.vlanId().toShort();
        if (jsonVlanId != vlanId) {
            description.appendText("vlan id was " + jsonVlanId);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod vlan pcp instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModVlanPcpInstruction(JsonNode instructionJson,
                                              Description description) {
        ModVlanPcpInstruction instructionToMatch =
                (ModVlanPcpInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final short jsonVlanPcp = instructionJson.get("vlanPcp").shortValue();
        final short vlanId = instructionToMatch.vlanPcp();
        if (jsonVlanPcp != vlanId) {
            description.appendText("vlan pcp was " + jsonVlanPcp);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod ip instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModIpInstruction(JsonNode instructionJson,
                                          Description description) {
        ModIPInstruction instructionToMatch =
                (ModIPInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final String jsonIp = instructionJson.get("ip").textValue();
        final String ip = instructionToMatch.ip().toString();
        if (!ip.equals(jsonIp)) {
            description.appendText("ip was " + jsonIp);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod IPv6 Flow Label instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModIPv6FlowLabelInstruction(JsonNode instructionJson,
                                                     Description description) {
        ModIPv6FlowLabelInstruction instructionToMatch =
                (ModIPv6FlowLabelInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonFlowLabel = instructionJson.get("flowLabel").intValue();
        final int flowLabel = instructionToMatch.flowLabel();
        if (flowLabel != jsonFlowLabel) {
            description.appendText("IPv6 flow label was " + jsonFlowLabel);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a mod MPLS label instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchModMplsLabelInstruction(JsonNode instructionJson,
                                          Description description) {
        ModMplsLabelInstruction instructionToMatch =
                (ModMplsLabelInstruction) instruction;
        final String jsonSubtype = instructionJson.get("subtype").textValue();
        if (!instructionToMatch.subtype().name().equals(jsonSubtype)) {
            description.appendText("subtype was " + jsonSubtype);
            return false;
        }

        final String jsonType = instructionJson.get("type").textValue();
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonLabel = instructionJson.get("label").intValue();
        final int label = instructionToMatch.mplsLabel().toInt();
        if (label != jsonLabel) {
            description.appendText("MPLS label was " + jsonLabel);
            return false;
        }

        return true;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonInstruction, Description description) {

        // check type
        final JsonNode jsonTypeNode = jsonInstruction.get("type");
        final String jsonType = jsonTypeNode.textValue();
        final String type = instruction.type().name();
        if (!jsonType.equals(type)) {
                description.appendText("type was " + type);
                return false;
        }

        if (instruction instanceof PushHeaderInstructions) {
            return matchPushHeaderInstruction(jsonInstruction, description);
        } else if (instruction instanceof DropInstruction) {
            return true;
        } else if (instruction instanceof OutputInstruction) {
            return matchOutputInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModLambdaInstruction) {
            return matchModLambdaInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModOchSignalInstruction) {
            return matchModOchSingalInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModEtherInstruction) {
            return matchModEtherInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModVlanIdInstruction) {
            return matchModVlanIdInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModVlanPcpInstruction) {
            return matchModVlanPcpInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModIPInstruction) {
            return matchModIpInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModIPv6FlowLabelInstruction) {
            return matchModIPv6FlowLabelInstruction(jsonInstruction,
                                                    description);
        } else if (instruction instanceof ModMplsLabelInstruction) {
            return matchModMplsLabelInstruction(jsonInstruction, description);
        } else if (instruction instanceof NoActionInstruction) {
            return true;
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(instruction.toString());
    }

    /**
     * Factory to allocate an instruction matcher.
     *
     * @param instruction instruction object we are looking for
     * @return matcher
     */
    public static InstructionJsonMatcher matchesInstruction(Instruction instruction) {
        return new InstructionJsonMatcher(instruction);
    }
}
