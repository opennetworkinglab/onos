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

package org.onosproject.drivers.bmv2.translators;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.model.Bmv2Model;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.Map;

/**
 * Implementation of a Bmv2 flow rule translator configuration for the
 * simple.p4 model.
 */
@Beta
public class Bmv2SimpleTranslatorConfig extends Bmv2DefaultTranslatorConfig {

    // Lazily populate field map.
    private static final Map<String, Criterion.Type> FIELD_MAP = ImmutableMap.of(
            "standard_metadata.ingress_port", Criterion.Type.IN_PORT,
            "ethernet.dstAddr", Criterion.Type.ETH_DST,
            "ethernet.srcAddr", Criterion.Type.ETH_SRC,
            "ethernet.etherType", Criterion.Type.ETH_TYPE);

    /**
     * Creates a new simple pipeline translator configuration.
     */
    public Bmv2SimpleTranslatorConfig(Bmv2Model model) {
        // Populate fieldMap.
        super(model, FIELD_MAP);
    }

    private static Bmv2Action buildDropAction() {
        return Bmv2Action.builder()
                .withName("_drop")
                .build();
    }

    private static Bmv2Action buildPushToCpAction() {
        return Bmv2Action.builder()
                .withName("send_to_cpu")
                .build();
    }

    private static Bmv2Action buildFwdAction(Instructions.OutputInstruction inst)
            throws Bmv2FlowRuleTranslatorException {

        Bmv2Action.Builder actionBuilder = Bmv2Action.builder();

        actionBuilder.withName("fwd");

        if (inst.port().isLogical()) {
            if (inst.port() == PortNumber.CONTROLLER) {
                return buildPushToCpAction();
            } else {
                throw new Bmv2FlowRuleTranslatorException(
                        "Output logic port number not supported: " + inst);
            }
        }

        actionBuilder.addParameter(
                ImmutableByteSequence.copyFrom((short) inst.port().toLong()));

        return actionBuilder.build();
    }

    @Override
    public Bmv2Action buildAction(TrafficTreatment treatment)
            throws Bmv2FlowRuleTranslatorException {


        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop.
            return buildDropAction();
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new Bmv2FlowRuleTranslatorException(
                    "Treatment not supported, more than 1 instructions found: "
                            + treatment.toString());
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                return buildFwdAction((Instructions.OutputInstruction) instruction);
            case NOACTION:
                return buildDropAction();
            default:
                throw new Bmv2FlowRuleTranslatorException(
                        "Instruction type not supported: "
                                + instruction.type().name());
        }
    }
}
