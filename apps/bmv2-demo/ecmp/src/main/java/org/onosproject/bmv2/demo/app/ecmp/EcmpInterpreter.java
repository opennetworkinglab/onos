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

package org.onosproject.bmv2.demo.app.ecmp;

import com.google.common.collect.ImmutableBiMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of a BMv2 interpreter for the ecmp.json configuration.
 */
public class EcmpInterpreter implements Bmv2Interpreter {

    protected static final String ECMP_METADATA = "ecmp_metadata";
    protected static final String SELECTOR = "selector";
    protected static final String GROUP_ID = "groupId";
    protected static final String GROUP_SIZE = "groupSize";
    protected static final String ECMP_GROUP = "ecmp_group";
    protected static final String ECMP_GROUP_TABLE = "ecmp_group_table";
    protected static final String TABLE0 = "table0";
    protected static final String SEND_TO_CPU = "send_to_cpu";
    protected static final String DROP = "_drop";
    protected static final String SET_EGRESS_PORT = "set_egress_port";
    protected static final String PORT = "port";

    private static final ImmutableBiMap<Criterion.Type, String> CRITERION_TYPE_MAP = ImmutableBiMap.of(
            Criterion.Type.IN_PORT, "standard_metadata.ingress_port",
            Criterion.Type.ETH_DST, "ethernet.dstAddr",
            Criterion.Type.ETH_SRC, "ethernet.srcAddr",
            Criterion.Type.ETH_TYPE, "ethernet.etherType");

    private static final ImmutableBiMap<Integer, String> TABLE_ID_MAP = ImmutableBiMap.of(
            0, TABLE0,
            1, ECMP_GROUP_TABLE);

    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return TABLE_ID_MAP;
    }

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        return CRITERION_TYPE_MAP;
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

    private static Bmv2Action buildEgressAction(PortNumber port, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        int portBitWidth = configuration.action(SET_EGRESS_PORT).runtimeData(PORT).bitWidth();

        try {
            ImmutableByteSequence portBs = fitByteSequence(ImmutableByteSequence.copyFrom(port.toLong()), portBitWidth);
            return Bmv2Action.builder()
                    .withName(SET_EGRESS_PORT)
                    .addParameter(portBs)
                    .build();
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new Bmv2InterpreterException(e.getMessage());
        }
    }

    private static Bmv2Action actionWithName(String name) {
        return Bmv2Action.builder().withName(name).build();
    }
}
