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
package org.onosproject.driver.optical.flowrule;

import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.OchSignalTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Cross connect abstraction based on a flow rule.
 */
public class CrossConnectFlowRule extends DefaultFlowRule implements CrossConnect {
    private PortNumber addDrop;
    private OchSignal ochSignal;
    private boolean isAddRule;

    public CrossConnectFlowRule(FlowRule rule, List<PortNumber> linePorts) {
        super(rule);

        Set<Criterion> criteria = rule.selector().criteria();
        List<Instruction> instructions = rule.treatment().immediate();

        // Proper cross connect has criteria for input port, OChSignal and OCh signal type.
        // Instruction must be output to port.
        checkArgument(criteria.size() == 3, "Wrong size of flow rule criteria for cross connect.");
        checkArgument(instructions.size() == 1, "Wrong size of flow rule instructions for cross connect.");
        // FIXME: Ensure criteria has exactly one of each match type
        criteria.forEach(
                c -> checkArgument(c instanceof OchSignalCriterion ||
                        c instanceof OchSignalTypeCriterion ||
                        c instanceof PortCriterion,
                        "Incompatible flow rule criteria for cross connect: " + criteria
                )
        );
        checkArgument(instructions.get(0).type() == Instruction.Type.OUTPUT,
                "Incompatible flow rule instructions for cross connect: " + instructions);

        ochSignal = criteria.stream()
                .filter(c -> c instanceof OchSignalCriterion)
                .map(c -> ((OchSignalCriterion) c).lambda())
                .findAny()
                .orElse(null);

        // Add or drop rule?
        Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) instructions.get(0);
        if (linePorts.contains(outInstruction.port())) {
            addDrop = criteria.stream()
                    .filter(c -> c instanceof PortCriterion)
                    .map(c -> ((PortCriterion) c).port())
                    .findAny()
                    .orElse(null);
            isAddRule = true;
        } else {
            addDrop = outInstruction.port();
            isAddRule = false;
        }
    }

    @Override
    public PortNumber addDrop() {
        return addDrop;
    }

    @Override
    public OchSignal ochSignal() {
        return ochSignal;
    }

    @Override
    public boolean isAddRule() {
        return isAddRule;
    }
}
