/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.ctl;

import com.google.common.collect.ImmutableBiMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.ByteSequenceFitException;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of a BMv2 interpreter for the BMv2 default.json configuration.
 */
public final class Bmv2DefaultInterpreterImpl implements Bmv2Interpreter {

    public static final String TABLE0 = "table0";
    public static final String SEND_TO_CPU = "send_to_cpu";
    public static final String PORT = "port";
    public static final String DROP = "_drop";
    public static final String SET_EGRESS_PORT = "set_egress_port";

    private static final ImmutableBiMap<Criterion.Type, String> CRITERION_MAP = ImmutableBiMap.of(
            Criterion.Type.IN_PORT, "standard_metadata.ingress_port",
            Criterion.Type.ETH_DST, "ethernet.dstAddr",
            Criterion.Type.ETH_SRC, "ethernet.srcAddr",
            Criterion.Type.ETH_TYPE, "ethernet.etherType");

    private static final ImmutableBiMap<Integer, String> TABLE_MAP = ImmutableBiMap.of(
            0, TABLE0);

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        return CRITERION_MAP;
    }

    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return TABLE_MAP;
    }

    @Override
    public Bmv2Action mapTreatment(TrafficTreatment treatment, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop for us.
            return actionWithName(DROP);
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new Bmv2InterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                OutputInstruction outInstruction = (OutputInstruction) instruction;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    return buildEgressAction(port, configuration);
                } else if (port.equals(CONTROLLER)) {
                    return actionWithName(SEND_TO_CPU);
                } else {
                    throw new Bmv2InterpreterException("Egress on logical port not supported: " + port);
                }
            case NOACTION:
                return actionWithName(DROP);
            default:
                throw new Bmv2InterpreterException("Instruction type not supported: " + instruction.type().name());
        }
    }

    private Bmv2Action buildEgressAction(PortNumber port, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        int portBitWidth = configuration.action(SET_EGRESS_PORT).runtimeData(PORT).bitWidth();

        try {
            ImmutableByteSequence portBs = fitByteSequence(copyFrom(port.toLong()), portBitWidth);
            return Bmv2Action.builder()
                    .withName(SET_EGRESS_PORT)
                    .addParameter(portBs)
                    .build();
        } catch (ByteSequenceFitException e) {
            throw new Bmv2InterpreterException(e.getMessage());
        }
    }

    private Bmv2Action actionWithName(String name) {
        return Bmv2Action.builder().withName(name).build();
    }
}
