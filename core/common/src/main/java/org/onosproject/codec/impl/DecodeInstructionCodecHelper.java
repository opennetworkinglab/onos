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

import static org.onlab.util.Tools.nullIsIllegal;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Decoding portion of the instruction codec.
 */
public final class DecodeInstructionCodecHelper {
    private final ObjectNode json;

    /**
     * Creates a decode instruction codec object.
     *
     * @param json JSON object to decode
     */
    public DecodeInstructionCodecHelper(ObjectNode json) {
        this.json = json;
    }

    /**
     * Decodes a Layer 2 instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodeL2() {
        String subType = json.get(InstructionCodec.SUBTYPE).asText();

        if (subType.equals(L2ModificationInstruction.L2SubType.ETH_SRC.name())) {
            String mac = nullIsIllegal(json.get(InstructionCodec.MAC),
                    InstructionCodec.MAC + InstructionCodec.MISSING_MEMBER_MESSAGE).asText();
            return Instructions.modL2Src(MacAddress.valueOf(mac));
        } else if (subType.equals(L2ModificationInstruction.L2SubType.ETH_DST.name())) {
            String mac = nullIsIllegal(json.get(InstructionCodec.MAC),
                    InstructionCodec.MAC + InstructionCodec.MISSING_MEMBER_MESSAGE).asText();
            return Instructions.modL2Dst(MacAddress.valueOf(mac));
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_ID.name())) {
            short vlanId = (short) nullIsIllegal(json.get(InstructionCodec.VLAN_ID),
                    InstructionCodec.VLAN_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modVlanId(VlanId.vlanId(vlanId));
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_PCP.name())) {
            byte vlanPcp = (byte) nullIsIllegal(json.get(InstructionCodec.VLAN_PCP),
                    InstructionCodec.VLAN_PCP + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modVlanPcp(vlanPcp);
        } else if (subType.equals(L2ModificationInstruction.L2SubType.MPLS_LABEL.name())) {
            int label = nullIsIllegal(json.get(InstructionCodec.MPLS_LABEL),
                    InstructionCodec.MPLS_LABEL + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modMplsLabel(MplsLabel.mplsLabel(label));
        } else if (subType.equals(L2ModificationInstruction.L2SubType.MPLS_PUSH.name())) {
            return Instructions.pushMpls();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.MPLS_POP.name())) {
            return Instructions.popMpls();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.DEC_MPLS_TTL.name())) {
            return Instructions.decMplsTtl();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_POP.name())) {
            return Instructions.popVlan();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_PUSH.name())) {
            return Instructions.pushVlan();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.TUNNEL_ID.name())) {
            long tunnelId = nullIsIllegal(json.get(InstructionCodec.TUNNEL_ID),
                    InstructionCodec.TUNNEL_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asLong();
            return Instructions.modTunnelId(tunnelId);
        }
        throw new IllegalArgumentException("L2 Instruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes a Layer 3 instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodeL3() {
        String subType = json.get(InstructionCodec.SUBTYPE).asText();

        if (subType.equals(L3ModificationInstruction.L3SubType.IPV4_SRC.name())) {
            IpAddress ip = IpAddress.valueOf(nullIsIllegal(json.get(InstructionCodec.IP),
                    InstructionCodec.IP + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            return Instructions.modL3Src(ip);
        } else if (subType.equals(L3ModificationInstruction.L3SubType.IPV4_DST.name())) {
            IpAddress ip = IpAddress.valueOf(nullIsIllegal(json.get(InstructionCodec.IP),
                    InstructionCodec.IP + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            return Instructions.modL3Dst(ip);
        } else if (subType.equals(L3ModificationInstruction.L3SubType.IPV6_SRC.name())) {
            IpAddress ip = IpAddress.valueOf(nullIsIllegal(json.get(InstructionCodec.IP),
                    InstructionCodec.IP + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            return Instructions.modL3IPv6Src(ip);
        } else if (subType.equals(L3ModificationInstruction.L3SubType.IPV6_DST.name())) {
            IpAddress ip = IpAddress.valueOf(nullIsIllegal(json.get(InstructionCodec.IP),
                    InstructionCodec.IP + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            return Instructions.modL3IPv6Dst(ip);
        } else if (subType.equals(L3ModificationInstruction.L3SubType.IPV6_FLABEL.name())) {
            int flowLabel = nullIsIllegal(json.get(InstructionCodec.FLOW_LABEL),
                    InstructionCodec.FLOW_LABEL + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modL3IPv6FlowLabel(flowLabel);
        }
        throw new IllegalArgumentException("L3 Instruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes a Layer 0 instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodeL0() {
        String subType = json.get(InstructionCodec.SUBTYPE).asText();


        if (subType.equals(L0ModificationInstruction.L0SubType.LAMBDA.name())) {
            int lambda = nullIsIllegal(json.get(InstructionCodec.LAMBDA),
                    InstructionCodec.LAMBDA + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modL0Lambda(Lambda.indexedLambda(lambda));
        } else if (subType.equals(L0ModificationInstruction.L0SubType.OCH.name())) {
            String gridTypeString = nullIsIllegal(json.get(InstructionCodec.GRID_TYPE),
                    InstructionCodec.GRID_TYPE + InstructionCodec.MISSING_MEMBER_MESSAGE).asText();
            GridType gridType = GridType.valueOf(gridTypeString);
            if (gridType == null) {
                throw new IllegalArgumentException("Unknown grid type  "
                        + gridTypeString);
            }
            String channelSpacingString = nullIsIllegal(json.get(InstructionCodec.CHANNEL_SPACING),
                    InstructionCodec.CHANNEL_SPACING + InstructionCodec.MISSING_MEMBER_MESSAGE).asText();
            ChannelSpacing channelSpacing = ChannelSpacing.valueOf(channelSpacingString);
            if (channelSpacing == null) {
                throw new IllegalArgumentException("Unknown channel spacing  "
                        + channelSpacingString);
            }
            int spacingMultiplier = nullIsIllegal(json.get(InstructionCodec.SPACING_MULTIPLIER),
                    InstructionCodec.SPACING_MULTIPLIER + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            int slotGranularity = nullIsIllegal(json.get(InstructionCodec.SLOT_GRANULARITY),
                    InstructionCodec.SLOT_GRANULARITY + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return Instructions.modL0Lambda(new OchSignal(gridType, channelSpacing,
                    spacingMultiplier, slotGranularity));
        }
        throw new IllegalArgumentException("L0 Instruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes a Layer 1 instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodeL1() {
        String subType = json.get(InstructionCodec.SUBTYPE).asText();
        if (subType.equals(L1ModificationInstruction.L1SubType.ODU_SIGID.name())) {
            int tributaryPortNumber = nullIsIllegal(json.get(InstructionCodec.TRIBUTARY_PORT_NUMBER),
                    InstructionCodec.TRIBUTARY_PORT_NUMBER + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            int tributarySlotLen = nullIsIllegal(json.get(InstructionCodec.TRIBUTARY_SLOT_LEN),
                    InstructionCodec.TRIBUTARY_SLOT_LEN + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            byte[] tributarySlotBitmap = null;
            tributarySlotBitmap = HexString.fromHexString(
                    nullIsIllegal(json.get(InstructionCodec.TRIBUTARY_SLOT_BITMAP),
                    InstructionCodec.TRIBUTARY_SLOT_BITMAP + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            return Instructions.modL1OduSignalId(OduSignalId.oduSignalId(tributaryPortNumber, tributarySlotLen,
                    tributarySlotBitmap));
        }
        throw new IllegalArgumentException("L1 Instruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes a Layer 4 instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodeL4() {
        String subType = json.get(InstructionCodec.SUBTYPE).asText();

        if (subType.equals(L4ModificationInstruction.L4SubType.TCP_DST.name())) {
            TpPort tcpPort = TpPort.tpPort(nullIsIllegal(json.get(InstructionCodec.TCP_PORT),
                    InstructionCodec.TCP_PORT + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());
            return Instructions.modTcpDst(tcpPort);
        } else if (subType.equals(L4ModificationInstruction.L4SubType.TCP_SRC.name())) {
            TpPort tcpPort = TpPort.tpPort(nullIsIllegal(json.get(InstructionCodec.TCP_PORT),
                    InstructionCodec.TCP_PORT + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());
            return Instructions.modTcpSrc(tcpPort);
        } else if (subType.equals(L4ModificationInstruction.L4SubType.UDP_DST.name())) {
            TpPort udpPort = TpPort.tpPort(nullIsIllegal(json.get(InstructionCodec.UDP_PORT),
                    InstructionCodec.UDP_PORT + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());
            return Instructions.modUdpDst(udpPort);
        } else if (subType.equals(L4ModificationInstruction.L4SubType.UDP_SRC.name())) {
            TpPort udpPort = TpPort.tpPort(nullIsIllegal(json.get(InstructionCodec.UDP_PORT),
                    InstructionCodec.UDP_PORT + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());
            return Instructions.modUdpSrc(udpPort);
        }
        throw new IllegalArgumentException("L4 Instruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes the JSON into an instruction object.
     *
     * @return Criterion object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public Instruction decode() {
        String type = json.get(InstructionCodec.TYPE).asText();

        if (type.equals(Instruction.Type.OUTPUT.name())) {
            PortNumber portNumber;
            if (json.get(InstructionCodec.PORT).isLong() || json.get(InstructionCodec.PORT).isInt()) {
                portNumber = PortNumber
                        .portNumber(nullIsIllegal(json.get(InstructionCodec.PORT)
                                                          .asLong(), InstructionCodec.PORT
                                                          + InstructionCodec.MISSING_MEMBER_MESSAGE));
            } else if (json.get(InstructionCodec.PORT).isTextual()) {
                portNumber = PortNumber
                        .fromString(nullIsIllegal(json.get(InstructionCodec.PORT)
                                                          .textValue(), InstructionCodec.PORT
                                                          + InstructionCodec.MISSING_MEMBER_MESSAGE));
            } else {
                throw new IllegalArgumentException("Port value "
                                                           + json.get(InstructionCodec.PORT).toString()
                                                           + " is not supported");
            }
            return Instructions.createOutput(portNumber);
        } else if (type.equals(Instruction.Type.DROP.name())) {
            return Instructions.createDrop();
        } else if (type.equals(Instruction.Type.L0MODIFICATION.name())) {
            return decodeL0();
        } else if (type.equals(Instruction.Type.L1MODIFICATION.name())) {
            return decodeL1();
        } else if (type.equals(Instruction.Type.L2MODIFICATION.name())) {
            return decodeL2();
        } else if (type.equals(Instruction.Type.L3MODIFICATION.name())) {
            return decodeL3();
        } else if (type.equals(Instruction.Type.L4MODIFICATION.name())) {
            return decodeL4();
        }
        throw new IllegalArgumentException("Instruction type "
                + type + " is not supported");
    }

}
