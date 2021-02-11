/*
 * Copyright 2015-present Open Networking Foundation
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
import com.google.common.collect.Maps;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.codec.CodecContext;
import org.onosproject.core.GroupId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.ExtensionTreatmentCodec;
import org.onosproject.net.flow.StatTriggerField;
import org.onosproject.net.flow.StatTriggerFlag;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.slf4j.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.codec.impl.InstructionCodec.STAT_PACKET_COUNT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Decoding portion of the instruction codec.
 */
public final class DecodeInstructionCodecHelper {
    private static final Logger log = getLogger(DecodeInstructionCodecHelper.class);
    private final ObjectNode json;
    private final CodecContext context;
    private static final Pattern ETHTYPE_PATTERN = Pattern.compile("0x([0-9a-fA-F]{4})");

    /**
     * Creates a decode instruction codec object.
     *
     * @param json JSON object to decode
     * @param context codec context
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
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();

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
            if (json.has(InstructionCodec.ETHERNET_TYPE)) {
                return Instructions.popMpls(getEthType());
            }
            return Instructions.popMpls();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.DEC_MPLS_TTL.name())) {
            return Instructions.decMplsTtl();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_POP.name())) {
            return Instructions.popVlan();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.VLAN_PUSH.name())) {
            if (json.has(InstructionCodec.ETHERNET_TYPE)) {
                return Instructions.pushVlan(getEthType());
            }
            return Instructions.pushVlan();
        } else if (subType.equals(L2ModificationInstruction.L2SubType.TUNNEL_ID.name())) {
            long tunnelId = nullIsIllegal(json.get(InstructionCodec.TUNNEL_ID),
                    InstructionCodec.TUNNEL_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asLong();
            return Instructions.modTunnelId(tunnelId);
        } else if (subType.equals(L2ModificationInstruction.L2SubType.MPLS_BOS.name())) {
            return Instructions.modMplsBos(json.get("bos").asBoolean());
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
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();

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
        } else  if (subType.equals(L3ModificationInstruction.L3SubType.TTL_IN.name())) {
            return Instructions.copyTtlIn();
        } else  if (subType.equals(L3ModificationInstruction.L3SubType.TTL_OUT.name())) {
            return Instructions.copyTtlOut();
        } else  if (subType.equals(L3ModificationInstruction.L3SubType.DEC_TTL.name())) {
            return Instructions.decNwTtl();
        } else  if (subType.equals(L3ModificationInstruction.L3SubType.IP_DSCP.name())) {
            int ipDscp = nullIsIllegal(json.get(InstructionCodec.IP_DSCP),
                InstructionCodec.IP_DSCP + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            if ((ipDscp < Byte.MIN_VALUE) || (ipDscp > Byte.MAX_VALUE)) {
                throw new IllegalArgumentException("Value " + ipDscp + " must be single byte");
            }
            return Instructions.modIpDscp((byte) ipDscp);
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
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();

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
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();
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
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();

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
     * Decodes a protocol-independent instruction.
     *
     * @return instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Instruction decodePi() {
        String subType = nullIsIllegal(json.get(InstructionCodec.SUBTYPE),
                                       InstructionCodec.SUBTYPE + InstructionCodec.ERROR_MESSAGE).asText();

        if (subType.equals(PiTableAction.Type.ACTION.name())) {
            PiActionId piActionId = PiActionId.of(nullIsIllegal(
                    json.get(InstructionCodec.PI_ACTION_ID),
                    InstructionCodec.PI_ACTION_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asText());
            JsonNode params = json.get(InstructionCodec.PI_ACTION_PARAMS);

            PiAction.Builder builder = PiAction.builder();
            PiActionParam piActionParam;
            PiActionParamId piActionParamId;
            if (params != null) {
                for (Map.Entry<String, String> param : ((Map<String, String>)
                        (context.mapper().convertValue(params, Map.class))).entrySet()) {
                    piActionParamId = PiActionParamId.of(param.getKey());
                    piActionParam = new PiActionParam(piActionParamId,
                                                      ImmutableByteSequence.copyFrom(
                                                              HexString.fromHexString(param.getValue(), null)));
                    builder.withParameter(piActionParam);
                }
            }

            return Instructions.piTableAction(builder.withId(piActionId).build());
        } else if (subType.equals(PiTableAction.Type.ACTION_PROFILE_GROUP_ID.name())) {
            PiActionProfileGroupId piActionGroupId = PiActionProfileGroupId.of(nullIsIllegal(
                    json.get(InstructionCodec.PI_ACTION_PROFILE_GROUP_ID),
                    InstructionCodec.PI_ACTION_PROFILE_GROUP_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());

            return Instructions.piTableAction(piActionGroupId);
        } else if (subType.equals(PiTableAction.Type.ACTION_PROFILE_MEMBER_ID.name())) {
            PiActionProfileMemberId piActionProfileMemberId = PiActionProfileMemberId.of(nullIsIllegal(
                    json.get(InstructionCodec.PI_ACTION_PROFILE_MEMBER_ID),
                    InstructionCodec.PI_ACTION_PROFILE_MEMBER_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());

            return Instructions.piTableAction(piActionProfileMemberId);
        }
        // TODO: implement JSON decoder for ACTION_SET
        throw new IllegalArgumentException("Protocol-independent Instruction subtype "
                                                   + subType + " is not supported");
    }

    private Instruction decodeStatTrigger() {
        String statTriggerFlag = nullIsIllegal(json.get(InstructionCodec.STAT_TRIGGER_FLAG),
                InstructionCodec.STAT_TRIGGER_FLAG + InstructionCodec.ERROR_MESSAGE).asText();

        StatTriggerFlag flag = null;

        if (statTriggerFlag.equals(StatTriggerFlag.ONLY_FIRST.name())) {
            flag = StatTriggerFlag.ONLY_FIRST;
        } else if (statTriggerFlag.equals(StatTriggerFlag.PERIODIC.name())) {
            flag = StatTriggerFlag.PERIODIC;
        } else {
            throw new IllegalArgumentException("statTriggerFlag "
                    + statTriggerFlag + " is not supported");
        }
        if (!json.has(InstructionCodec.STAT_THRESHOLDS)) {
            throw new IllegalArgumentException("statThreshold is not added");
        }
        JsonNode statThresholdsNode = nullIsIllegal(json.get(InstructionCodec.STAT_THRESHOLDS),
                InstructionCodec.STAT_THRESHOLDS + InstructionCodec.ERROR_MESSAGE);
        Map<StatTriggerField, Long> statThresholdMap = getStatThreshold(statThresholdsNode);
        if (statThresholdMap.isEmpty()) {
            throw new IllegalArgumentException("statThreshold must have at least one property");
        }
        return Instructions.statTrigger(statThresholdMap, flag);
    }

    private Map<StatTriggerField, Long> getStatThreshold(JsonNode statThresholdNode) {
        Map<StatTriggerField, Long> statThresholdMap = Maps.newEnumMap(StatTriggerField.class);
        for (JsonNode jsonNode : statThresholdNode) {
            if (jsonNode.hasNonNull(InstructionCodec.STAT_BYTE_COUNT)) {
                JsonNode byteCountNode = jsonNode.get(InstructionCodec.STAT_BYTE_COUNT);
                if (!byteCountNode.isNull() && byteCountNode.isNumber()) {
                    statThresholdMap.put(StatTriggerField.BYTE_COUNT, byteCountNode.asLong());
                }
            } else if (jsonNode.hasNonNull(STAT_PACKET_COUNT)) {
                JsonNode packetCount = jsonNode.get(STAT_PACKET_COUNT);
                if (!packetCount.isNull() && packetCount.isNumber()) {
                    statThresholdMap.put(StatTriggerField.PACKET_COUNT, packetCount.asLong());
                }
            } else if (jsonNode.hasNonNull(InstructionCodec.STAT_DURATION)) {
                JsonNode duration = jsonNode.get(InstructionCodec.STAT_DURATION);
                if (!duration.isNull() && duration.isNumber()) {
                    statThresholdMap.put(StatTriggerField.DURATION, duration.asLong());
                }
            } else {
                log.error("Unsupported stat {}", jsonNode.toString());
            }
        }

        return statThresholdMap;
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
        JsonNode portNode = nullIsIllegal(jsonNode.get(InstructionCodec.PORT),
                InstructionCodec.PORT + InstructionCodec.ERROR_MESSAGE);
        if (portNode.isLong() || portNode.isInt()) {
            portNumber = PortNumber.portNumber(portNode.asLong());
        } else if (portNode.isTextual()) {
            portNumber = PortNumber.fromString(portNode.textValue());
        } else {
            throw new IllegalArgumentException("Port value "
                    + portNode.toString()
                    + " is not supported");
        }
        return portNumber;
    }

    /**
     * Returns Ethernet type.
     *
     * @return ethernet type
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private EthType getEthType() {
        String ethTypeStr = nullIsIllegal(json.get(InstructionCodec.ETHERNET_TYPE),
                  InstructionCodec.ETHERNET_TYPE + InstructionCodec.MISSING_MEMBER_MESSAGE).asText();
        Matcher matcher = ETHTYPE_PATTERN.matcher(ethTypeStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("ETHERNET_TYPE must be a four digit hex string starting with 0x");
        }
        short ethernetType = (short) Integer.parseInt(matcher.group(1), 16);
        return new EthType(ethernetType);
    }

    /**
     * Decodes the JSON into an instruction object.
     *
     * @return Criterion object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public Instruction decode() {
        String type = nullIsIllegal(json.get(InstructionCodec.TYPE),
                InstructionCodec.TYPE + InstructionCodec.ERROR_MESSAGE).asText();

        if (type.equals(Instruction.Type.OUTPUT.name())) {
            return Instructions.createOutput(getPortNumber(json));
        } else if (type.equals(Instruction.Type.NOACTION.name())) {
            return Instructions.createNoAction();
        } else if (type.equals(Instruction.Type.TABLE.name())) {
            return Instructions.transition(nullIsIllegal(json.get(InstructionCodec.TABLE_ID),
                    InstructionCodec.TABLE_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asInt());
        } else if (type.equals(Instruction.Type.GROUP.name())) {
            // a group id should be an unsigned integer
            Long id = nullIsIllegal(json.get(InstructionCodec.GROUP_ID),
                    InstructionCodec.GROUP_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asLong();
            GroupId groupId = new GroupId(id.intValue());
            return Instructions.createGroup(groupId);
        } else if (type.equals(Instruction.Type.METER.name())) {
            MeterId meterId = MeterId.meterId(nullIsIllegal(json.get(InstructionCodec.METER_ID),
                    InstructionCodec.METER_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asLong());
            return Instructions.meterTraffic(meterId);
        } else if (type.equals(Instruction.Type.QUEUE.name())) {
            long queueId = nullIsIllegal(json.get(InstructionCodec.QUEUE_ID),
                    InstructionCodec.QUEUE_ID + InstructionCodec.MISSING_MEMBER_MESSAGE).asLong();
            if (json.get(InstructionCodec.PORT) == null ||
                    json.get(InstructionCodec.PORT).isNull()) {
                return Instructions.setQueue(queueId, null);
            } else {
                return Instructions.setQueue(queueId, getPortNumber(json));
            }
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
        } else if (type.equals(Instruction.Type.STAT_TRIGGER.name())) {
            return decodeStatTrigger();
        } else if (type.equals(Instruction.Type.PROTOCOL_INDEPENDENT.name())) {
            return decodePi();
        }

        throw new IllegalArgumentException("Instruction type "
                                                   + type + " is not supported");
    }
}
