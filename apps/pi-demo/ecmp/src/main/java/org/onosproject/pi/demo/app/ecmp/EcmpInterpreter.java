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

package org.onosproject.pi.demo.app.ecmp;

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
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableId;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.pi.runtime.PiPacketOperation.Type.PACKET_OUT;

/**
 * Implementation of a PiPipeline interpreter for the ecmp.json configuration.
 */
public class EcmpInterpreter extends AbstractHandlerBehaviour implements PiPipelineInterpreter {

    protected static final String ECMP_METADATA_HEADER_NAME = "ecmp_metadata_t";
    protected static final String ECMP_GROUP_ACTION_NAME = "ecmp_group";
    protected static final String GROUP_ID = "group_id";
    protected static final String SELECTOR = "selector";
    protected static final String GROUP_SIZE = "groupSize";
    protected static final String ECMP_GROUP_TABLE = "ecmp_group_table";
    protected static final String TABLE0 = "table0";
    private static final String SEND_TO_CPU = "send_to_cpu";
    private static final String PORT = "port";
    private static final String DROP = "drop";
    private static final String SET_EGRESS_PORT = "set_egress_port";
    private static final String EGRESS_PORT = "egress_port";
    private static final int PORT_NUMBER_BIT_WIDTH = 9;

    private static final PiHeaderFieldId IN_PORT_ID = PiHeaderFieldId.of("standard_metadata", "ingress_port");
    private static final PiHeaderFieldId ETH_DST_ID = PiHeaderFieldId.of("ethernet", "dstAddr");
    private static final PiHeaderFieldId ETH_SRC_ID = PiHeaderFieldId.of("ethernet", "srcAddr");
    private static final PiHeaderFieldId ETH_TYPE_ID = PiHeaderFieldId.of("ethernet", "etherType");

    private static final ImmutableBiMap<Criterion.Type, PiHeaderFieldId> CRITERION_MAP =
            new ImmutableBiMap.Builder<Criterion.Type, PiHeaderFieldId>()
                    .put(Criterion.Type.IN_PORT, IN_PORT_ID)
                    .put(Criterion.Type.ETH_DST, ETH_DST_ID)
                    .put(Criterion.Type.ETH_SRC, ETH_SRC_ID)
                    .put(Criterion.Type.ETH_TYPE, ETH_TYPE_ID)
                    .build();

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP = ImmutableBiMap.of(
            0, PiTableId.of(TABLE0),
            1, PiTableId.of(ECMP_GROUP_TABLE));

    public static final String INGRESS_PORT = "ingress_port";

    @Override
    public Optional<Integer> mapPiTableId(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_MAP.inverse().get(piTableId));
    }

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {

        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop for us.
            return actionWithName(DROP);
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new PiPipelineInterpreter.PiInterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) instruction;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    PiAction.builder()
                            .withId(PiActionId.of(SET_EGRESS_PORT))
                            .withParameter(new PiActionParam(PiActionParamId.of(PORT),
                                                             ImmutableByteSequence.copyFrom(port.toLong())))
                            .build();
                } else if (port.equals(CONTROLLER)) {
                    return actionWithName(SEND_TO_CPU);
                } else {
                    throw new PiInterpreterException("Egress on logical port not supported: " + port);
                }
            case NOACTION:
                return actionWithName(DROP);
            default:
                throw new PiInterpreterException("Instruction type not supported: " + instruction.type().name());
        }
    }

    private static PiAction actionWithName(String name) {
        return PiAction.builder().withId(PiActionId.of(name)).build();
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
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {
        TrafficTreatment treatment = packet.treatment();

        // ecmp.p4 supports only OUTPUT instructions.
        List<Instructions.OutputInstruction> outInstructions = treatment.allInstructions()
                .stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> (Instructions.OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT
            throw new PiInterpreterException("Treatment not supported: " + treatment);
        }

        ImmutableList.Builder<PiPacketOperation> builder = ImmutableList.builder();
        for (Instructions.OutputInstruction outInst : outInstructions) {
            if (outInst.port().isLogical() && !outInst.port().equals(FLOOD)) {
                throw new PiInterpreterException("Logical port not supported: " +
                                                         outInst.port());
            } else if (outInst.port().equals(FLOOD)) {
                //Since ecmp.p4 does not support flood for each port of the device
                // create a packet operation to send the packet out of that specific port
                for (Port port : handler().get(DeviceService.class).getPorts(packet.sendThrough())) {
                    builder.add(createPiPacketOperation(packet.data(), port.number().toLong()));
                }
            } else {
                builder.add(createPiPacketOperation(packet.data(), outInst.port().toLong()));
            }
        }
        return builder.build();
    }

    private PiPacketOperation createPiPacketOperation(ByteBuffer data, long portNumber) throws PiInterpreterException {
        //create the metadata
        PiPacketMetadata metadata = createPacketMetadata(portNumber);

        //Create the Packet operation
        return PiPacketOperation.builder()
                .withType(PACKET_OUT)
                .withData(ImmutableByteSequence.copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiPacketMetadata createPacketMetadata(long portNumber) throws PiInterpreterException {
        ImmutableByteSequence portValue = ImmutableByteSequence.copyFrom(portNumber);
        //FIXME remove hardcoded bitWidth and retrieve it from pipelineModel
        try {
            portValue = ImmutableByteSequence.fit(portValue, PORT_NUMBER_BIT_WIDTH);
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException("Port number too big: {}" +
                                                     portNumber + " causes " + e.getMessage());
        }
        return PiPacketMetadata.builder()
                .withId(PiPacketMetadataId.of(EGRESS_PORT))
                .withValue(portValue)
                .build();
    }

    @Override
    public InboundPacket mapInboundPacket(DeviceId deviceId, PiPacketOperation packetInOperation)
            throws PiInterpreterException {
        //We are assuming that the packet is ethernet type
        Ethernet ethPkt = new Ethernet();

        ethPkt.deserialize(packetInOperation.data().asArray(), 0, packetInOperation.data().size());

        //Returns the ingress port packet metadata
        Optional<PiPacketMetadata> packetMetadata = packetInOperation.metadatas()
                .stream().filter(metadata -> metadata.id().name().equals(INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {

            //Obtaining the ingress port as an immutable byte sequence
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();

            //Converting immutableByteSequence to short
            short s = portByteSequence.asReadOnlyBuffer().getShort();

            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));

            //FIXME should be optimizable with .asReadOnlyBytebuffer
            ByteBuffer rawData = ByteBuffer.wrap(packetInOperation.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);

        } else {
            throw new PiInterpreterException("Can't get packet metadata for" + INGRESS_PORT);
        }
    }
}