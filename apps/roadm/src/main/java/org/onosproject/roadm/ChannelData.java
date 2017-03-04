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
package org.onosproject.roadm;

import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

/**
 * Representation of an internal ROADM connection.
 */
public final class ChannelData {
    private PortNumber inPort;
    private PortNumber outPort;
    private OchSignal ochSignal;

    private ChannelData(PortNumber inPort, PortNumber outPort, OchSignal ochSignal) {
        this.inPort = inPort;
        this.outPort = outPort;
        this.ochSignal = ochSignal;
    }

    /**
     * Returns a ChannelData representation from a flow rule. The rule must contain
     * a Criterion.Type.IN_PORT selector, Criterion.Type.OCH_SIGID selector, and
     * Instruction.Type.OUTPUT instruction.
     *
     * @param rule the flow rule representing the connection
     * @return ChannelData representation of the connection
     */
    public static ChannelData fromFlow(FlowRule rule) {
        checkNotNull(rule);

        Criterion in = rule.selector().getCriterion(Criterion.Type.IN_PORT);
        checkNotNull(in);
        PortNumber inPort = ((PortCriterion) in).port();

        Criterion och = rule.selector().getCriterion(Criterion.Type.OCH_SIGID);
        OchSignal ochSignal = och == null ? null : ((OchSignalCriterion) och).lambda();

        PortNumber outPort = null;
        List<Instruction> instructions = rule.treatment().allInstructions();
        for (Instruction ins : instructions) {
            if (ins.type() == Instruction.Type.OUTPUT) {
                outPort = ((Instructions.OutputInstruction) ins).port();
            }
        }
        checkNotNull(outPort);

        return new ChannelData(inPort, outPort, ochSignal);
    }

    /**
     * Returns the input port.
     *
     * @return input port
     */
    public PortNumber inPort() {
        return inPort;
    }

    /**
     * Returns the output port.
     *
     * @return output port
     */
    public PortNumber outPort() {
        return outPort;
    }

    /**
     * Returns the channel signal.
     *
     * @return channel signal
     */
    public OchSignal ochSignal() {
        return ochSignal;
    }
}
