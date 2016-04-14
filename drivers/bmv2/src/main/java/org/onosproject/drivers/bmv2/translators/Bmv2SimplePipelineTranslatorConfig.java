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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.model.Bmv2Model;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Implementation of a Bmv2 flow rule translator configuration for the
 * simple_pipeline.p4 model.
 */
@Beta
public class Bmv2SimplePipelineTranslatorConfig implements Bmv2FlowRuleTranslator.TranslatorConfig {

    private static final String JSON_CONFIG_PATH = "/simple_pipeline.json";
    private final Map<String, Criterion.Type> fieldMap = Maps.newHashMap();
    private final Bmv2Model model;

    /**
     * Creates a new simple pipeline translator configuration.
     */
    public Bmv2SimplePipelineTranslatorConfig() {

        this.model = getModel();

        // populate fieldMap
        fieldMap.put("standard_metadata.ingress_port", Criterion.Type.IN_PORT);
        fieldMap.put("ethernet.dstAddr", Criterion.Type.ETH_DST);
        fieldMap.put("ethernet.srcAddr", Criterion.Type.ETH_SRC);
        fieldMap.put("ethernet.etherType", Criterion.Type.ETH_TYPE);
    }

    private static Bmv2Action buildDropAction() {
        return Bmv2Action.builder()
                .withName("_drop")
                .build();
    }

    private static Bmv2Action buildFwdAction(Instructions.OutputInstruction inst)
            throws Bmv2FlowRuleTranslatorException {

        Bmv2Action.Builder actionBuilder = Bmv2Action.builder();

        actionBuilder.withName("fwd");

        if (inst.port().isLogical()) {
            throw new Bmv2FlowRuleTranslatorException(
                    "Output logic port numbers not supported: " + inst);
        }

        actionBuilder.addParameter(
                ImmutableByteSequence.copyFrom((short) inst.port().toLong()));

        return actionBuilder.build();
    }

    private static Bmv2Model getModel() {
        InputStream inputStream = Bmv2SimplePipelineTranslatorConfig.class
                .getResourceAsStream(JSON_CONFIG_PATH);
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader bufReader = new BufferedReader(reader);
        JsonObject json = null;
        try {
            json = Json.parse(bufReader).asObject();
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse JSON file: " + e.getMessage());
        }

        return Bmv2Model.parse(json);
    }

    @Override
    public Bmv2Model model() {
        return this.model;
    }

    @Override
    public Map<String, Criterion.Type> fieldToCriterionTypeMap() {
        return fieldMap;
    }

    @Override
    public Bmv2Action buildAction(TrafficTreatment treatment)
            throws Bmv2FlowRuleTranslatorException {


        if (treatment.allInstructions().size() == 0) {
            // no instructions means drop
            return buildDropAction();
        } else if (treatment.allInstructions().size() > 1) {
            // otherwise, we understand treatments with only 1 instruction
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
