/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.impl;

import org.onlab.util.Frequency;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;

/**
 * Class that parses a FlowRule as passed by ONOS and
 * extracts info such as the OchSignal, Portnumber.
 *
 *
 * We iterate on the Selector. A Selector based on OCh is Rx
 */
public class FlowRuleParser {

    public FlowRuleParser(FlowRule r) {
        for (Criterion c : r.selector().criteria()) {
            if (c instanceof OchSignalCriterion) {
                rx = true;
                ochSignal = ((OchSignalCriterion) c).lambda();
            }
            if (c instanceof PortCriterion) {
                portNumber = ((PortCriterion) c).port();
            }
        }

        for (Instruction i : r.treatment().immediate()) {
            if (i instanceof
                    L0ModificationInstruction.ModOchSignalInstruction) {
                ochSignal =
                        ((L0ModificationInstruction.ModOchSignalInstruction) i)
                                .lambda();
            }
            if (i instanceof Instructions.OutputInstruction) {
                portNumber = ((Instructions.OutputInstruction) i).port();
            }
        }
    }

    public boolean isReceiver() {
        return rx;
    }

    public OchSignal getOchsignal() {
        return ochSignal;
    }

    public PortNumber getPortNumber() {
        return portNumber;
    }

    public Frequency getCentralFrequency() {
        return ochSignal.centralFrequency();
    }

    private boolean rx = false;
    private OchSignal ochSignal = null;
    private PortNumber portNumber = null;
}
