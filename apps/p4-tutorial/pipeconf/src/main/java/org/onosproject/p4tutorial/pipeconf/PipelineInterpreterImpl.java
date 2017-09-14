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

package org.onosproject.p4tutorial.pipeconf;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.Ethernet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableId;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onlab.util.ImmutableByteSequence.fit;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.pi.runtime.PiPacketOperation.Type.PACKET_OUT;

/**
 * Implementation of a PI interpreter for the main.p4 program.
 */
public final class PipelineInterpreterImpl extends AbstractHandlerBehaviour implements PiPipelineInterpreter {

    private static final String TABLE0 = "table0";
    private static final String IP_PROTO_FILTER_TABLE = "ip_proto_filter_table";
    private static final String SEND_TO_CPU = "send_to_cpu";
    private static final String PORT = "port";
    private static final String DROP = "_drop";
    private static final String SET_EGRESS_PORT = "set_egress_port";
    private static final String EGRESS_PORT = "egress_port";
    private static final String INGRESS_PORT = "ingress_port";
    private static final String ETHERNET = "ethernet";
    private static final String STANDARD_METADATA = "standard_metadata";
    private static final int PORT_FIELD_BITWIDTH = 9;

    private static final PiHeaderFieldId INGRESS_PORT_ID = PiHeaderFieldId.of(STANDARD_METADATA, "ingress_port");
    private static final PiHeaderFieldId ETH_DST_ID = PiHeaderFieldId.of(ETHERNET, "dst_addr");
    private static final PiHeaderFieldId ETH_SRC_ID = PiHeaderFieldId.of(ETHERNET, "src_addr");
    private static final PiHeaderFieldId ETH_TYPE_ID = PiHeaderFieldId.of(ETHERNET, "ether_type");
    private static final PiTableId TABLE0_ID = PiTableId.of(TABLE0);
    private static final PiTableId IP_PROTO_FILTER_TABLE_ID = PiTableId.of(IP_PROTO_FILTER_TABLE);

    private static final BiMap<Integer, PiTableId> TABLE_MAP = ImmutableBiMap.of(
            0, TABLE0_ID,
            1, IP_PROTO_FILTER_TABLE_ID);

    private static final BiMap<Criterion.Type, PiHeaderFieldId> CRITERION_MAP =
            new ImmutableBiMap.Builder<Criterion.Type, PiHeaderFieldId>()
                    .put(Criterion.Type.IN_PORT, INGRESS_PORT_ID)
                    .put(Criterion.Type.ETH_DST, ETH_DST_ID)
                    .put(Criterion.Type.ETH_SRC, ETH_SRC_ID)
                    .put(Criterion.Type.ETH_TYPE, ETH_TYPE_ID)
                    .build();

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {

        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop for us.
            return PiAction.builder()
                    .withId(PiActionId.of(DROP))
                    .build();
        } else if (treatment.allInstructions().size() > 1) {
            // We understand treatments with only 1 instruction.
            throw new PiInterpreterException("Treatment has multiple instructions");
        }

        // Get the first and only instruction.
        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                // We understand only instructions of type OUTPUT.
                Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) instruction;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    return PiAction.builder()
                            .withId(PiActionId.of(SET_EGRESS_PORT))
                            .withParameter(new PiActionParam(PiActionParamId.of(PORT), copyFrom(port.toLong())))
                            .build();
                } else if (port.equals(CONTROLLER)) {
                    return PiAction.builder()
                            .withId(PiActionId.of(SEND_TO_CPU))
                            .build();
                } else {
                    throw new PiInterpreterException(format("Output on logical port '%s' not supported", port));
                }
            default:
                throw new PiInterpreterException(format("Instruction of type '%s' not supported", instruction.type()));
        }
    }

    @Override
    public Optional<PiCounterId> mapTableCounter(PiTableId piTableId) {
        return Optional.empty();
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {

        TrafficTreatment treatment = packet.treatment();

        // We support only packet-out with OUTPUT instructions.
        List<Instructions.OutputInstruction> outInstructions = treatment.allInstructions().stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> (Instructions.OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT.
            throw new PiInterpreterException("Treatment not supported: " + treatment);
        }

        ImmutableList.Builder<PiPacketOperation> builder = ImmutableList.builder();

        for (Instructions.OutputInstruction outInst : outInstructions) {
            if (outInst.port().isLogical() && !outInst.port().equals(FLOOD)) {
                throw new PiInterpreterException(format("Output on logical port '%s' not supported", outInst.port()));
            } else if (outInst.port().equals(FLOOD)) {
                // Since main.p4 does not support flooding, we create a packet operation for each switch port.
                DeviceService deviceService = handler().get(DeviceService.class);
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
    public InboundPacket mapInboundPacket(DeviceId deviceId, PiPacketOperation packetIn)
            throws PiInterpreterException {
        // We assume that the packet is ethernet, which is fine since default.p4 can deparse only ethernet packets.
        Ethernet ethPkt = new Ethernet();

        ethPkt.deserialize(packetIn.data().asArray(), 0, packetIn.data().size());

        // Returns the ingress port packet metadata.
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas().stream()
                .filter(metadata -> metadata.id().name().equals(INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));
            return new DefaultInboundPacket(receivedFrom, ethPkt, packetIn.data().asReadOnlyBuffer());
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s", INGRESS_PORT, deviceId, packetIn));
        }
    }

    private PiPacketOperation createPiPacketOperation(ByteBuffer data, long portNumber) throws PiInterpreterException {
        PiPacketMetadata metadata = createPacketMetadata(portNumber);
        return PiPacketOperation.builder()
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiPacketMetadata createPacketMetadata(long portNumber) throws PiInterpreterException {
        try {
            return PiPacketMetadata.builder()
                    .withId(PiPacketMetadataId.of(EGRESS_PORT))
                    .withValue(fit(copyFrom(portNumber), PORT_FIELD_BITWIDTH))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format("Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }

    @Override
    public Optional<PiHeaderFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<Criterion.Type> mapPiHeaderFieldId(PiHeaderFieldId headerFieldId) {
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
