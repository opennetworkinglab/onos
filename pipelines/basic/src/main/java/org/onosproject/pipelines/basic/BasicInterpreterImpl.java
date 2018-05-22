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

package org.onosproject.pipelines.basic;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiControlMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_DROP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_NOACTION_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_PRM_PORT_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_SEND_TO_CPU_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_SET_EGRESS_PORT_TABLE0_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_SET_EGRESS_PORT_WCMP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_ETH_DST_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_ETH_SRC_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_ETH_TYPE_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_IN_PORT_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_IPV4_DST_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_IPV4_SRC_ID;
import static org.onosproject.pipelines.basic.BasicConstants.PKT_META_EGRESS_PORT_ID;
import static org.onosproject.pipelines.basic.BasicConstants.PKT_META_INGRESS_PORT_ID;
import static org.onosproject.pipelines.basic.BasicConstants.PORT_BITWIDTH;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_TABLE0_ID;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_WCMP_TABLE_ID;

/**
 * Interpreter implementation for basic.p4.
 */
public class BasicInterpreterImpl extends AbstractHandlerBehaviour
        implements PiPipelineInterpreter {

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP =
            new ImmutableBiMap.Builder<Integer, PiTableId>()
                    .put(0, TBL_TABLE0_ID)
                    .build();

    private static final ImmutableBiMap<Criterion.Type, PiMatchFieldId> CRITERION_MAP =
            new ImmutableBiMap.Builder<Criterion.Type, PiMatchFieldId>()
                    .put(Criterion.Type.IN_PORT, HDR_IN_PORT_ID)
                    .put(Criterion.Type.ETH_DST, HDR_ETH_DST_ID)
                    .put(Criterion.Type.ETH_SRC, HDR_ETH_SRC_ID)
                    .put(Criterion.Type.ETH_TYPE, HDR_ETH_TYPE_ID)
                    .put(Criterion.Type.IPV4_SRC, HDR_IPV4_SRC_ID)
                    .put(Criterion.Type.IPV4_DST, HDR_IPV4_DST_ID)
                    .build();

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {
        if (treatment.allInstructions().size() == 0) {
            // No actions means drop.
            return PiAction.builder().withId(ACT_DROP_ID).build();
        } else if (treatment.allInstructions().size() > 1) {
            // We understand treatments with only 1 instruction.
            throw new PiInterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);
        switch (instruction.type()) {
            case OUTPUT:
                if (piTableId == TBL_TABLE0_ID) {
                    return outputPiAction((OutputInstruction) instruction, ACT_SET_EGRESS_PORT_TABLE0_ID);
                } else if (piTableId == TBL_WCMP_TABLE_ID) {
                    return outputPiAction((OutputInstruction) instruction, ACT_SET_EGRESS_PORT_WCMP_ID);
                } else {
                    throw new PiInterpreterException(
                            "Output instruction not supported in table " + piTableId);
                }
            case NOACTION:
                return PiAction.builder().withId(ACT_NOACTION_ID).build();
            default:
                throw new PiInterpreterException(format(
                        "Instruction type '%s' not supported", instruction.type()));
        }
    }

    private PiAction outputPiAction(OutputInstruction outInstruction, PiActionId piActionId)
            throws PiInterpreterException {
        PortNumber port = outInstruction.port();
        if (!port.isLogical()) {
            try {
                return PiAction.builder()
                        .withId(piActionId)
                        .withParameter(new PiActionParam(ACT_PRM_PORT_ID,
                                                         copyFrom(port.toLong()).fit(PORT_BITWIDTH)))
                        .build();
            } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                throw new PiInterpreterException(e.getMessage());
            }
        } else if (port.equals(CONTROLLER)) {
            return PiAction.builder().withId(ACT_SEND_TO_CPU_ID).build();
        } else {
            throw new PiInterpreterException(format(
                    "Egress on logical port '%s' not supported", port));
        }
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {
        TrafficTreatment treatment = packet.treatment();

        // basic.p4 supports only OUTPUT instructions.
        List<OutputInstruction> outInstructions = treatment
                .allInstructions()
                .stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> (OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT.
            throw new PiInterpreterException("Treatment not supported: " + treatment);
        }

        ImmutableList.Builder<PiPacketOperation> builder = ImmutableList.builder();
        for (OutputInstruction outInst : outInstructions) {
            if (outInst.port().isLogical() && !outInst.port().equals(FLOOD)) {
                throw new PiInterpreterException(format(
                        "Output on logical port '%s' not supported", outInst.port()));
            } else if (outInst.port().equals(FLOOD)) {
                // Since basic.p4 does not support flooding, we create a packet
                // operation for each switch port.
                final DeviceService deviceService = handler().get(DeviceService.class);
                for (Port port : deviceService.getPorts(packet.sendThrough())) {
                    builder.add(createPiPacketOperation(packet.data(), port.number().toLong()));
                }
            } else {
                builder.add(createPiPacketOperation(packet.data(), outInst.port().toLong()));
            }
        }
        return builder.build();
    }

    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn)
            throws PiInterpreterException {
        // Assuming that the packet is ethernet, which is fine since basic.p4
        // can deparse only ethernet packets.
        Ethernet ethPkt;
        try {
            ethPkt = Ethernet.deserializer().deserialize(packetIn.data().asArray(), 0,
                                                         packetIn.data().size());
        } catch (DeserializationException dex) {
            throw new PiInterpreterException(dex.getMessage());
        }

        // Returns the ingress port packet metadata.
        Optional<PiControlMetadata> packetMetadata = packetIn.metadatas()
                .stream().filter(m -> m.id().equals(PKT_META_INGRESS_PORT_ID))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(packetIn.deviceId(), PortNumber.portNumber(s));
            ByteBuffer rawData = ByteBuffer.wrap(packetIn.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s",
                    PKT_META_INGRESS_PORT_ID, packetIn.deviceId(), packetIn));
        }
    }

    private PiPacketOperation createPiPacketOperation(ByteBuffer data, long portNumber)
            throws PiInterpreterException {
        PiControlMetadata metadata = createPacketMetadata(portNumber);
        return PiPacketOperation.builder()
                .forDevice(this.data().deviceId())
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiControlMetadata createPacketMetadata(long portNumber) throws PiInterpreterException {
        try {
            return PiControlMetadata.builder()
                    .withId(PKT_META_EGRESS_PORT_ID)
                    .withValue(copyFrom(portNumber).fit(PORT_BITWIDTH))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format(
                    "Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }

    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<Criterion.Type> mapPiMatchFieldId(PiMatchFieldId headerFieldId) {
        return Optional.ofNullable(CRITERION_MAP.inverse().get(headerFieldId));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_MAP.get(flowRuleTableId));
    }

    @Override
    public Optional<Integer> mapPiTableId(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_MAP.inverse().get(piTableId));
    }
}
