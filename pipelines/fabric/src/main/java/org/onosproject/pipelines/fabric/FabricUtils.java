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
 */

package org.onosproject.pipelines.fabric;

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.Optional;

/**
 * Utility class for fabric pipeliner.
 */
public final class FabricUtils {

    private FabricUtils() {
        // Hides constructor.
    }

    public static Optional<Instructions.OutputInstruction> getOutputInstruction(TrafficTreatment treatment) {
        return treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                .map(inst -> (Instructions.OutputInstruction) inst)
                .findFirst();
    }

    public static PortNumber getOutputPort(TrafficTreatment treatment) {
        return getOutputInstruction(treatment)
                .map(Instructions.OutputInstruction::port)
                .orElse(null);
    }
}
