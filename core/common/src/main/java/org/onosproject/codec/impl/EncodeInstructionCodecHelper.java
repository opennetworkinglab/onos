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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.HexString;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.ExtensionTreatmentCodec;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * JSON encoding of Instructions.
 */
public final class EncodeInstructionCodecHelper {
    protected static final Logger log = getLogger(EncodeInstructionCodecHelper.class);
    private final Instruction instruction;
    private final CodecContext context;

    /**
     * Creates an instruction object encoder.
     *
     * @param instruction instruction to encode
     * @param context     codec context for the encoding
     */
    public EncodeInstructionCodecHelper(Instruction instruction, CodecContext context) {
        this.instruction = instruction;
        this.context = context;
    }


    /**
     * Encode an L0 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL0(ObjectNode result) {
        L0ModificationInstruction l0Instruction = (L0ModificationInstruction) instruction;
        result.put(InstructionCodec.SUBTYPE, l0Instruction.subtype().name());

        switch (l0Instruction.subtype()) {
            case OCH:
                L0ModificationInstruction.ModOchSignalInstruction ochSignalInstruction =
                        (L0ModificationInstruction.ModOchSignalInstruction) l0Instruction;
                OchSignal ochSignal = ochSignalInstruction.lambda();
                result.put(InstructionCodec.GRID_TYPE, ochSignal.gridType().name());
                result.put(InstructionCodec.CHANNEL_SPACING, ochSignal.channelSpacing().name());
                result.put(InstructionCodec.SPACING_MULTIPLIER, ochSignal.spacingMultiplier());
                result.put(InstructionCodec.SLOT_GRANULARITY, ochSignal.slotGranularity());
                break;

            default:
                log.info("Cannot convert L0 subtype of {}", l0Instruction.subtype());
        }
    }

    /**
     * Encode an L1 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL1(ObjectNode result) {
        L1ModificationInstruction l1Instruction = (L1ModificationInstruction) instruction;
        result.put(InstructionCodec.SUBTYPE, l1Instruction.subtype().name());

        switch (l1Instruction.subtype()) {
            case ODU_SIGID:
                final L1ModificationInstruction.ModOduSignalIdInstruction oduSignalIdInstruction =
                        (L1ModificationInstruction.ModOduSignalIdInstruction) l1Instruction;
                OduSignalId oduSignalId = oduSignalIdInstruction.oduSignalId();

                ObjectNode child = result.putObject("oduSignalId");

                child.put(InstructionCodec.TRIBUTARY_PORT_NUMBER, oduSignalId.tributaryPortNumber());
                child.put(InstructionCodec.TRIBUTARY_SLOT_LEN, oduSignalId.tributarySlotLength());
                child.put(InstructionCodec.TRIBUTARY_SLOT_BITMAP,
                        HexString.toHexString(oduSignalId.tributarySlotBitmap()));
                break;
            default:
                log.info("Cannot convert L1 subtype of {}", l1Instruction.subtype());
                break;
        }
    }

    /**
     * Encode an L2 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL2(ObjectNode result) {
        L2ModificationInstruction l2Instruction = (L2ModificationInstruction) instruction;
        result.put(InstructionCodec.SUBTYPE, l2Instruction.subtype().name());

        switch (l2Instruction.subtype()) {
            case ETH_SRC:
            case ETH_DST:
                final L2ModificationInstruction.ModEtherInstruction modEtherInstruction =
                        (L2ModificationInstruction.ModEtherInstruction) l2Instruction;
                result.put(InstructionCodec.MAC, modEtherInstruction.mac().toString());
                break;
            case VLAN_ID:
                final L2ModificationInstruction.ModVlanIdInstruction modVlanIdInstruction =
                        (L2ModificationInstruction.ModVlanIdInstruction) l2Instruction;
                result.put(InstructionCodec.VLAN_ID, modVlanIdInstruction.vlanId().toShort());
                break;
            case VLAN_PCP:
                final L2ModificationInstruction.ModVlanPcpInstruction modVlanPcpInstruction =
                        (L2ModificationInstruction.ModVlanPcpInstruction) l2Instruction;
                result.put(InstructionCodec.VLAN_PCP, modVlanPcpInstruction.vlanPcp());
                break;
            case MPLS_LABEL:
                final L2ModificationInstruction.ModMplsLabelInstruction modMplsLabelInstruction =
                        (L2ModificationInstruction.ModMplsLabelInstruction) l2Instruction;
                result.put(InstructionCodec.MPLS_LABEL, modMplsLabelInstruction.label().toInt());
                break;
            case MPLS_PUSH:
                final L2ModificationInstruction.ModMplsHeaderInstruction pushHeaderInstructions =
                        (L2ModificationInstruction.ModMplsHeaderInstruction) l2Instruction;
                result.put(InstructionCodec.ETHERNET_TYPE,
                        pushHeaderInstructions.ethernetType().toShort());
                break;
            case TUNNEL_ID:
                final L2ModificationInstruction.ModTunnelIdInstruction modTunnelIdInstruction =
                        (L2ModificationInstruction.ModTunnelIdInstruction) l2Instruction;
                result.put(InstructionCodec.TUNNEL_ID, modTunnelIdInstruction.tunnelId());
                break;
            case MPLS_BOS:
                final L2ModificationInstruction.ModMplsBosInstruction modMplsBosInstruction =
                        (L2ModificationInstruction.ModMplsBosInstruction) l2Instruction;
                result.put(InstructionCodec.MPLS_BOS, modMplsBosInstruction.mplsBos());
            case MPLS_POP:
            case DEC_MPLS_TTL:
                break;
            default:
                log.info("Cannot convert L2 subtype of {}", l2Instruction.subtype());
                break;
        }
    }

    /**
     * Encode an L3 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL3(ObjectNode result) {
        L3ModificationInstruction l3Instruction = (L3ModificationInstruction) instruction;
        result.put(InstructionCodec.SUBTYPE, l3Instruction.subtype().name());
        switch (l3Instruction.subtype()) {
            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                        (L3ModificationInstruction.ModIPInstruction) l3Instruction;
                result.put(InstructionCodec.IP, modIPInstruction.ip().toString());
                break;
            case IPV6_FLABEL:
                final L3ModificationInstruction.ModIPv6FlowLabelInstruction
                        modFlowLabelInstruction =
                        (L3ModificationInstruction.ModIPv6FlowLabelInstruction) l3Instruction;
                result.put(InstructionCodec.FLOW_LABEL, modFlowLabelInstruction.flowLabel());
                break;
            case TTL_IN:
            case TTL_OUT:
            case DEC_TTL:
                break;
            default:
                log.info("Cannot convert L3 subtype of {}", l3Instruction.subtype());
                break;
        }
    }

    /**
     * Encode a L4 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL4(ObjectNode result) {
        L4ModificationInstruction l4Instruction = (L4ModificationInstruction) instruction;
        result.put(InstructionCodec.SUBTYPE, l4Instruction.subtype().name());
        switch (l4Instruction.subtype()) {
            case TCP_DST:
            case TCP_SRC:
                final L4ModificationInstruction.ModTransportPortInstruction modTcpPortInstruction =
                        (L4ModificationInstruction.ModTransportPortInstruction) l4Instruction;
                result.put(InstructionCodec.TCP_PORT, modTcpPortInstruction.port().toInt());
                break;
            case UDP_DST:
            case UDP_SRC:
                final L4ModificationInstruction.ModTransportPortInstruction modUdpPortInstruction =
                        (L4ModificationInstruction.ModTransportPortInstruction) l4Instruction;
                result.put(InstructionCodec.UDP_PORT, modUdpPortInstruction.port().toInt());
                break;
            default:
                log.info("Cannot convert L4 subtype of {}", l4Instruction.subtype());
                break;
        }
    }


    /**
     * Encodes a extension instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeExtension(ObjectNode result) {
        final Instructions.ExtensionInstructionWrapper extensionInstruction =
                (Instructions.ExtensionInstructionWrapper) instruction;

        DeviceId deviceId = extensionInstruction.deviceId();

        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        DeviceService deviceService = serviceDirectory.get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);

        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }

        if (device.is(ExtensionTreatmentCodec.class)) {
            ExtensionTreatmentCodec treatmentCodec = device.as(ExtensionTreatmentCodec.class);
            ObjectNode node = treatmentCodec.encode(extensionInstruction.extensionInstruction(), context);
            result.set(InstructionCodec.EXTENSION, node);
        } else {
            throw new IllegalArgumentException(
                    "There is no codec to encode extension for device " + deviceId.toString());
        }
    }

    /**
     * Encodes the given instruction into JSON.
     *
     * @return JSON object node representing the instruction
     */
    public ObjectNode encode() {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(InstructionCodec.TYPE, instruction.type().toString());

        switch (instruction.type()) {
            case OUTPUT:
                final Instructions.OutputInstruction outputInstruction =
                        (Instructions.OutputInstruction) instruction;
                result.put(InstructionCodec.PORT, outputInstruction.port().toString());
                break;

            case NOACTION:
                break;

            case GROUP:
                final Instructions.GroupInstruction groupInstruction =
                        (Instructions.GroupInstruction) instruction;
                result.put(InstructionCodec.GROUP_ID, groupInstruction.groupId().toString());
                break;

            case METER:
                final Instructions.MeterInstruction meterInstruction =
                        (Instructions.MeterInstruction) instruction;
                result.put(InstructionCodec.METER_ID, meterInstruction.meterId().toString());
                break;

            case TABLE:
                final Instructions.TableTypeTransition tableTransitionInstruction =
                        (Instructions.TableTypeTransition) instruction;
                result.put(InstructionCodec.TABLE_ID, tableTransitionInstruction.tableId().toString());
                break;

            case QUEUE:
                final Instructions.SetQueueInstruction setQueueInstruction =
                        (Instructions.SetQueueInstruction) instruction;
                result.put(InstructionCodec.QUEUE_ID, setQueueInstruction.queueId());
                result.put(InstructionCodec.PORT, setQueueInstruction.port().toString());
                break;

            case L0MODIFICATION:
                encodeL0(result);
                break;

            case L1MODIFICATION:
                encodeL1(result);
                break;

            case L2MODIFICATION:
                encodeL2(result);
                break;

            case L3MODIFICATION:
                encodeL3(result);
                break;

            case L4MODIFICATION:
                encodeL4(result);
                break;

            case EXTENSION:
                encodeExtension(result);
                break;

            default:
                log.info("Cannot convert instruction type of {}", instruction.type());
                break;
        }
        return result;
    }

}
