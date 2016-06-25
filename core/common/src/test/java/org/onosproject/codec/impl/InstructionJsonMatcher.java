/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onlab.util.HexString;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import org.onosproject.net.flow.instructions.Instructions.MeterInstruction;
import org.onosproject.net.flow.instructions.Instructions.NoActionInstruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.SetQueueInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModOchSignalInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction.ModOduSignalIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsHeaderInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;

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
    private boolean matchModMplsHeaderInstruction(JsonNode instructionJson,
                                                  Description description) {
        ModMplsHeaderInstruction instructionToMatch =
                (ModMplsHeaderInstruction) instruction;
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

    // TODO: need to add matchModVlanHeaderInstruction

    /**
     * Matches the contents of an output instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchOutputInstruction(JsonNode instructionJson,
                                           Description description) {
        final String jsonType = instructionJson.get("type").textValue();
        OutputInstruction instructionToMatch = (OutputInstruction) instruction;
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        if (instructionJson.get("port").isLong() ||
                instructionJson.get("port").isInt()) {
            final long jsonPort = instructionJson.get("port").asLong();
            if (instructionToMatch.port().toLong() != (jsonPort)) {
                description.appendText("port was " + jsonPort);
                return false;
            }
        } else if (instructionJson.get("port").isTextual()) {
            final String jsonPort = instructionJson.get("port").textValue();
            if (!instructionToMatch.port().toString().equals(jsonPort)) {
                description.appendText("port was " + jsonPort);
                return false;
            }
        } else {
            final String jsonPort = instructionJson.get("port").toString();
            description.appendText("Unmatching types ");
            description.appendText("instructionToMatch " + instructionToMatch.port().toString());
            description.appendText("jsonPort " + jsonPort);
        }

        return true;
    }

    /**
     * Matches the contents of a group instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchGroupInstruction(JsonNode instructionJson,
                                          Description description) {
        final String jsonType = instructionJson.get("type").textValue();
        GroupInstruction instructionToMatch = (GroupInstruction) instruction;
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final int jsonGroupId = instructionJson.get("groupId").intValue();
        if (instructionToMatch.groupId().id() != jsonGroupId) {
            description.appendText("groupId was " + jsonGroupId);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a meter instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchMeterInstruction(JsonNode instructionJson,
                                          Description description) {
        final String jsonType = instructionJson.get("type").textValue();
        MeterInstruction instructionToMatch = (MeterInstruction) instruction;
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final long jsonMeterId = instructionJson.get("meterId").longValue();
        if (instructionToMatch.meterId().id() != jsonMeterId) {
            description.appendText("meterId was " + jsonMeterId);
            return false;
        }

        return true;
    }

    /**
     * Matches the contents of a set queue instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchSetQueueInstruction(JsonNode instructionJson,
                                             Description description) {
        final String jsonType = instructionJson.get("type").textValue();
        SetQueueInstruction instructionToMatch = (SetQueueInstruction) instruction;
        if (!instructionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final long jsonQueueId = instructionJson.get("queueId").longValue();
        if (instructionToMatch.queueId() != jsonQueueId) {
            description.appendText("queueId was " + jsonQueueId);
            return false;
        }

        if (instructionJson.get("port").isLong() ||
                instructionJson.get("port").isInt()) {
            final long jsonPort = instructionJson.get("port").asLong();
            if (instructionToMatch.port().toLong() != (jsonPort)) {
                description.appendText("port was " + jsonPort);
                return false;
            }
        } else if (instructionJson.get("port").isTextual()) {
            final String jsonPort = instructionJson.get("port").textValue();
            if (!instructionToMatch.port().toString().equals(jsonPort)) {
                description.appendText("port was " + jsonPort);
                return false;
            }
        } else {
            final String jsonPort = instructionJson.get("port").toString();
            description.appendText("Unmatching types ");
            description.appendText("instructionToMatch " + instructionToMatch.port().toString());
            description.appendText("jsonPort " + jsonPort);
        }

        return true;
    }

    /**
     * Matches the contents of a mod OCh singal instruction.
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
     * Matches the contents of a mod ODU singal Id instruction.
     *
     * @param instructionJson JSON instruction to match
     * @param description Description object used for recording errors
     * @return true if contents matches, false otherwise
     */
    private boolean matchModOduSingalIdInstruction(JsonNode instructionJson,
                                                   Description description) {
        ModOduSignalIdInstruction instructionToMatch =
                (ModOduSignalIdInstruction) instruction;
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
        final JsonNode jsonOduSignal = instructionJson.get("oduSignalId");
        int jsonTpn = jsonOduSignal.get("tributaryPortNumber").intValue();
        int jsonTsLen = jsonOduSignal.get("tributarySlotLength").intValue();
        byte[] tributaryBitMap = HexString.fromHexString(jsonOduSignal.get("tributarySlotBitmap").asText());
        OduSignalId  jsonOduSignalId = OduSignalId.oduSignalId(jsonTpn, jsonTsLen, tributaryBitMap);
        if (!instructionToMatch.oduSignalId().equals(jsonOduSignalId)) {
            description.appendText("oduSignalId was " + instructionToMatch);
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
        final int label = instructionToMatch.label().toInt();
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

        if (instruction instanceof ModMplsHeaderInstruction) {
            return matchModMplsHeaderInstruction(jsonInstruction, description);
        } else if (instruction instanceof OutputInstruction) {
            return matchOutputInstruction(jsonInstruction, description);
        } else if (instruction instanceof GroupInstruction) {
            return matchGroupInstruction(jsonInstruction, description);
        } else if (instruction instanceof MeterInstruction) {
            return matchMeterInstruction(jsonInstruction, description);
        } else if (instruction instanceof SetQueueInstruction) {
            return matchSetQueueInstruction(jsonInstruction, description);
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
            return matchModIPv6FlowLabelInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModMplsLabelInstruction) {
            return matchModMplsLabelInstruction(jsonInstruction, description);
        } else if (instruction instanceof ModOduSignalIdInstruction) {
            return matchModOduSingalIdInstruction(jsonInstruction, description);
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
