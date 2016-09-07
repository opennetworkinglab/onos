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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.ExtensionTreatmentCodec;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.meter.MeterId;
import org.slf4j.Logger;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Decoding portion of the instruction codec.
 */
public final class DecodeInstructionCodecHelper {
    protected static final Logger log = getLogger(DecodeInstructionCodecHelper.class);
    private final ObjectNode json;
    private final CodecContext context;

    /**
     * Creates a decode instruction codec object.
     *
     * @param json JSON object to decode
     */
    public DecodeInstructionCodecHelper(ObjectNode json, CodecContext context) {
        this.json = json;
        this.context = context;
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

        if (subType.equals(L0ModificationInstruction.L0SubType.OCH.name())) {
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
     * Decodes a extension instruction.
     *
     * @return extension treatment
     */
    private Instruction decodeExtension() {
        ObjectNode node = (ObjectNode) json.get(InstructionCodec.EXTENSION);
        if (node != null) {
            DeviceId deviceId = getDeviceId();

            ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
            DeviceService deviceService = serviceDirectory.get(DeviceService.class);
            Device device = deviceService.getDevice(deviceId);

            if (device == null) {
                throw new IllegalArgumentException("Device not found");
            }

            if (device.is(ExtensionTreatmentCodec.class)) {
                ExtensionTreatmentCodec treatmentCodec = device.as(ExtensionTreatmentCodec.class);
                ExtensionTreatment treatment = treatmentCodec.decode(node, context);
                return Instructions.extension(treatment, deviceId);
            } else {
                throw new IllegalArgumentException(
                        "There is no codec to decode extension for device " + deviceId.toString());
            }
        }
        return null;
    }

    /**
     * Returns device identifier.
     *
     * @return device identifier
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private DeviceId getDeviceId() {
        JsonNode deviceIdNode = json.get(InstructionCodec.DEVICE_ID);
        if (deviceIdNode != null) {
            return DeviceId.deviceId(deviceIdNode.asText());
        }
        throw new IllegalArgumentException("Empty device identifier");
    }

    /**
     * Extracts port number of the given json node.
     *
     * @param jsonNode json node
     * @return port number
     */
    private PortNumber getPortNumber(ObjectNode jsonNode) {
        PortNumber portNumber;
        if (jsonNode.get(InstructionCodec.PORT).isLong() || jsonNode.get(InstructionCodec.PORT).isInt()) {
            portNumber = PortNumber
                    .portNumber(nullIsIllegal(jsonNode.get(InstructionCodec.PORT)
                            .asLong(), InstructionCodec.PORT
                            + InstructionCodec.MISSING_MEMBER_MESSAGE));
        } else if (jsonNode.get(InstructionCodec.PORT).isTextual()) {
            portNumber = PortNumber
                    .fromString(nullIsIllegal(jsonNode.get(InstructionCodec.PORT)
                            .textValue(), InstructionCodec.PORT
                            + InstructionCodec.MISSING_MEMBER_MESSAGE));
        } else {
            throw new IllegalArgumentException("Port value "
                    + jsonNode.get(InstructionCodec.PORT).toString()
                    + " is not supported");
        }
        return portNumber;
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
            return Instructions.createOutput(getPortNumber(json));
        } else if (type.equals(Instruction.Type.NOACTION.name())) {
            return Instructions.createNoAction();
        } else if (type.equals(Instruction.Type.TABLE.name())) {
            return Instructions.transition(nullIsIllegal(json.get(InstructionCodec.TABLE_ID)
                    .asInt(), InstructionCodec.TABLE_ID + InstructionCodec.MISSING_MEMBER_MESSAGE));
        } else if (type.equals(Instruction.Type.GROUP.name())) {
            GroupId groupId = new DefaultGroupId(nullIsIllegal(json.get(InstructionCodec.GROUP_ID)
                    .asInt(), InstructionCodec.GROUP_ID + InstructionCodec.MISSING_MEMBER_MESSAGE));
            return Instructions.createGroup(groupId);
        } else if (type.equals(Instruction.Type.METER.name())) {
            MeterId meterId = MeterId.meterId(nullIsIllegal(json.get(InstructionCodec.METER_ID)
                    .asLong(), InstructionCodec.METER_ID + InstructionCodec.MISSING_MEMBER_MESSAGE));
            return Instructions.meterTraffic(meterId);
        } else if (type.equals(Instruction.Type.QUEUE.name())) {
            long queueId = nullIsIllegal(json.get(InstructionCodec.QUEUE_ID)
                    .asLong(), InstructionCodec.QUEUE_ID + InstructionCodec.MISSING_MEMBER_MESSAGE);
            return Instructions.setQueue(queueId, getPortNumber(json));
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
        } else if (type.equals(Instruction.Type.EXTENSION.name())) {
            return decodeExtension();
        }
        throw new IllegalArgumentException("Instruction type "
                + type + " is not supported");
    }
}
