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
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of a BMv2 interpreter for the BMv2 default.json configuration.
 */
public final class Bmv2DefaultInterpreterImpl implements Bmv2Interpreter {

    public static final String TABLE0 = "table0";
    public static final String SEND_TO_CPU = "send_to_cpu";
    public static final String DROP = "_drop";
    public static final String SET_EGRESS_PORT = "set_egress_port";

    // Lazily populate field map.
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
            return Bmv2Action.builder().withName(DROP).build();
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new Bmv2InterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                OutputInstruction outInstruction = (OutputInstruction) instruction;
                if (outInstruction.port() == CONTROLLER) {
                    return Bmv2Action.builder().withName(SEND_TO_CPU).build();
                } else {
                    return buildEgressAction(outInstruction, configuration);
                }
            case NOACTION:
                return Bmv2Action.builder().withName(DROP).build();
            default:
                throw new Bmv2InterpreterException("Instruction type not supported: " + instruction.type().name());
        }
    }


    /**
     * Builds an egress action equivalent to the given output instruction for the given configuration.
     *
     * @param instruction   an output instruction
     * @param configuration a configuration
     * @return a BMv2 action
     * @throws Bmv2InterpreterException if the instruction cannot be translated to a BMv2 action
     */
    private Bmv2Action buildEgressAction(OutputInstruction instruction, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        Bmv2Action.Builder actionBuilder = Bmv2Action.builder();

        actionBuilder.withName(SET_EGRESS_PORT);

        if (instruction.port().isLogical()) {
            throw new Bmv2InterpreterException("Output on logic port not supported: " + instruction);
        }

        // Get the byte sequence for the out port with the right length
        long portNumber = instruction.port().toLong();
        int bitWidth = configuration.action(SET_EGRESS_PORT).runtimeData("port").bitWidth();
        try {
            ImmutableByteSequence outPort = Bmv2TranslatorUtils.fitByteSequence(
                    ImmutableByteSequence.copyFrom(portNumber), bitWidth);
            return Bmv2Action.builder().withName(SET_EGRESS_PORT).addParameter(outPort).build();
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new Bmv2InterpreterException(e.getMessage());
        }
    }
}
