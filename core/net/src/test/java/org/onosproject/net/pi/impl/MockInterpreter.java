/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.impl;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableId;

import java.util.Collection;
import java.util.Optional;

import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Mock interpreter implementation.
 */
public class MockInterpreter extends AbstractHandlerBehaviour implements PiPipelineInterpreter {

    static final String TABLE0 = "table0";
    static final String SEND_TO_CPU = "send_to_cpu_0";
    static final String PORT = "port";
    static final String DROP = "_drop_0";
    static final String SET_EGRESS_PORT = "set_egress_port_0";

    static final PiHeaderFieldId IN_PORT_ID = PiHeaderFieldId.of("standard_metadata", "ingress_port");
    static final PiHeaderFieldId ETH_DST_ID = PiHeaderFieldId.of("ethernet_t", "dstAddr");
    static final PiHeaderFieldId ETH_SRC_ID = PiHeaderFieldId.of("ethernet_t", "srcAddr");
    static final PiHeaderFieldId ETH_TYPE_ID = PiHeaderFieldId.of("ethernet_t", "etherType");

    private static final ImmutableBiMap<Criterion.Type, PiHeaderFieldId> CRITERION_MAP = ImmutableBiMap.of(
            Criterion.Type.IN_PORT, IN_PORT_ID,
            Criterion.Type.ETH_DST, ETH_DST_ID,
            Criterion.Type.ETH_SRC, ETH_SRC_ID,
            Criterion.Type.ETH_TYPE, ETH_TYPE_ID);

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP = ImmutableBiMap.of(
            0, PiTableId.of(TABLE0));

    @Override
    public PiTableAction mapTreatment(TrafficTreatment treatment, PiPipeconf pipeconf) throws PiInterpreterException {

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
                OutputInstruction outInstruction = (OutputInstruction) instruction;
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

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet, PiPipeconf pipeconf)
            throws PiInterpreterException {
        return ImmutableList.of();
    }

    /**
     * Returns an action instance with no runtime parameters.
     */
    private PiAction actionWithName(String name) {
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

}
