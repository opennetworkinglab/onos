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

package org.onosproject.drivers.p4runtime;

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
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiHeaderFieldModel;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiCounterType;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiPipeconfService;
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
 * Implementation of an interpreter that can be used for any P4 program based on default.p4 (i.e. those under
 * onos/tools/test/p4src).
 */
public class DefaultP4Interpreter extends AbstractHandlerBehaviour implements PiPipelineInterpreter {

    // FIXME: Should move this class out of the p4runtime drivers.
    // e.g. in a dedicated onos/pipeconf directory, along with any related P4 source code.

    public static final String TABLE0 = "table0";
    public static final String TABLE0_COUNTER = "table0_counter";
    public static final String ECMP = "ecmp";
    public static final String SEND_TO_CPU = "send_to_cpu";
    public static final String PORT = "port";
    public static final String DROP = "_drop";
    public static final String SET_EGRESS_PORT = "set_egress_port";
    public static final String EGRESS_PORT = "egress_port";
    public static final String INGRESS_PORT = "ingress_port";

    private static final PiTableId TABLE0_ID = PiTableId.of(TABLE0);
    private static final PiTableId ECMP_ID = PiTableId.of(ECMP);

    protected static final PiHeaderFieldId ETH_DST_ID = PiHeaderFieldId.of("ethernet", "dstAddr");
    protected static final PiHeaderFieldId ETH_SRC_ID = PiHeaderFieldId.of("ethernet", "srcAddr");
    protected static final PiHeaderFieldId ETH_TYPE_ID = PiHeaderFieldId.of("ethernet", "etherType");

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP = ImmutableBiMap.of(
            0, TABLE0_ID,
            1, ECMP_ID);

    private static final ImmutableBiMap<PiTableId, PiCounterId> TABLE_COUNTER_MAP = ImmutableBiMap.of(
            TABLE0_ID, PiCounterId.of(TABLE0_COUNTER, PiCounterType.DIRECT));

    private boolean targetAttributesInitialized = false;

    /*
    The following attributes are target-specific, i.e. they might change from one target to another.
     */
    private ImmutableBiMap<Criterion.Type, PiHeaderFieldId> criterionMap;
    private int portFieldBitWidth;

    /**
     * Populates target-specific attributes based on this device's pipeline model.
     */
    private synchronized void initTargetSpecificAttributes() {
        if (targetAttributesInitialized) {
            return;
        }

        DeviceId deviceId = this.handler().data().deviceId();
        PiPipeconfService pipeconfService = this.handler().get(PiPipeconfService.class);
        PiPipeconfId pipeconfId = pipeconfService.ofDevice(deviceId)
                .orElseThrow(() -> new RuntimeException(format(
                        "Unable to get current pipeconf for device %s", this.data().deviceId())));
        PiPipeconf pipeconf = pipeconfService.getPipeconf(pipeconfId)
                .orElseThrow(() -> new RuntimeException(format(
                        "Pipeconf %s is not registered", pipeconfId)));
        PiPipelineModel model = pipeconf.pipelineModel();

        this.portFieldBitWidth = extractPortFieldBitWidth(model);
        this.criterionMap = new ImmutableBiMap.Builder<Criterion.Type, PiHeaderFieldId>()
                .put(Criterion.Type.IN_PORT, extractInPortFieldId(model))
                .put(Criterion.Type.ETH_DST, ETH_DST_ID)
                .put(Criterion.Type.ETH_SRC, ETH_SRC_ID)
                .put(Criterion.Type.ETH_TYPE, ETH_TYPE_ID)
                .build();

        this.targetAttributesInitialized = true;
    }

    private static PiHeaderFieldId extractInPortFieldId(PiPipelineModel model) {
        /*
        For the targets we currently support, the field name is "ingress_port", but we miss the header name, which is
        target-specific. We know table0 defines that field as a match key, we look for it and we get the header name.
         */
        PiTableModel tableModel = model.table(TABLE0).orElseThrow(() -> new RuntimeException(format(
                "No such table '%s' in pipeline model", TABLE0)));
        PiHeaderFieldModel fieldModel = tableModel.matchFields().stream()
                .filter(m -> m.field().type().name().equals(INGRESS_PORT))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(format(
                        "No such match field in table '%s' with name '%s'", TABLE0, INGRESS_PORT)))
                .field();
        return PiHeaderFieldId.of(fieldModel.header().name(), INGRESS_PORT);
    }

    private static int extractPortFieldBitWidth(PiPipelineModel model) {
        /*
        Get it form the set_egress_port action parameters.
         */
        return model
                .action(SET_EGRESS_PORT).orElseThrow(() -> new RuntimeException(format(
                        "No such action '%s' in pipeline model", SET_EGRESS_PORT)))
                .param(PORT).orElseThrow(() -> new RuntimeException(format(
                        "No such parameter '%s' of action '%s' in pipeline model", PORT, SET_EGRESS_PORT)))
                .bitWidth();
    }


    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId) throws PiInterpreterException {
        initTargetSpecificAttributes();
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
                return outputPiAction(outInstruction);
            case PROTOCOL_INDEPENDENT:
                PiInstruction piInstruction = (PiInstruction) instruction;
                return (PiAction) piInstruction.action();
            case NOACTION:
                return actionWithName(DROP);
            default:
                throw new PiInterpreterException(format("Instruction type '%s' not supported", instruction.type()));
        }
    }

    private PiAction outputPiAction(Instructions.OutputInstruction outInstruction) throws PiInterpreterException {
        PortNumber port = outInstruction.port();
        if (!port.isLogical()) {
            try {
                return PiAction.builder()
                        .withId(PiActionId.of(SET_EGRESS_PORT))
                        .withParameter(new PiActionParam(PiActionParamId.of(PORT),
                                                         fit(copyFrom(port.toLong()), portFieldBitWidth)))
                        .build();
            } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                throw new PiInterpreterException(e.getMessage());
            }
        } else if (port.equals(CONTROLLER)) {
            return actionWithName(SEND_TO_CPU);
        } else {
            throw new PiInterpreterException(format("Egress on logical port '%s' not supported", port));
        }
    }

    @Override
    public Optional<PiCounterId> mapTableCounter(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_COUNTER_MAP.get(piTableId));
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {
        TrafficTreatment treatment = packet.treatment();

        // default.p4 supports only OUTPUT instructions.
        List<Instructions.OutputInstruction> outInstructions = treatment.allInstructions()
                .stream()
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
                // Since default.p4 does not support flooding, we create a packet operation for each switch port.
                for (Port port : handler().get(DeviceService.class).getPorts(packet.sendThrough())) {
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
        // Assuming that the packet is ethernet, which is fine since default.p4 can deparse only ethernet packets.
        Ethernet ethPkt = new Ethernet();

        ethPkt.deserialize(packetIn.data().asArray(), 0, packetIn.data().size());

        // Returns the ingress port packet metadata.
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas()
                .stream().filter(metadata -> metadata.id().name().equals(INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));
            ByteBuffer rawData = ByteBuffer.wrap(packetIn.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
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
        initTargetSpecificAttributes();
        try {
            return PiPacketMetadata.builder()
                    .withId(PiPacketMetadataId.of(EGRESS_PORT))
                    .withValue(fit(copyFrom(portNumber), portFieldBitWidth))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format("Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }

    /**
     * Returns an action instance with no runtime parameters.
     */
    private PiAction actionWithName(String name) {
        return PiAction.builder().withId(PiActionId.of(name)).build();
    }

    @Override
    public Optional<PiHeaderFieldId> mapCriterionType(Criterion.Type type) {
        initTargetSpecificAttributes();
        return Optional.ofNullable(criterionMap.get(type));
    }

    @Override
    public Optional<Criterion.Type> mapPiHeaderFieldId(PiHeaderFieldId headerFieldId) {
        initTargetSpecificAttributes();
        return Optional.ofNullable(criterionMap.inverse().get(headerFieldId));
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
