/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.flow.instructions;

import org.onosproject.net.OchSignal;

import java.util.Objects;

public abstract class L0ModificationInstruction implements Instruction {

    private static final String SEPARATOR = ":";

    /**
     * Represents the type of traffic treatment.
     */
    public enum L0SubType {
        /**
         * OCh (Optical Channel) modification.
         */
        OCH,
    }

    public abstract L0SubType subtype();

    @Override
    public final Type type() {
        return Type.L0MODIFICATION;
    }

    /**
     * Represents an L0 OCh (Optical Channel) modification instruction.
     */
    public static final class ModOchSignalInstruction extends L0ModificationInstruction {

        private final OchSignal lambda;

        ModOchSignalInstruction(OchSignal lambda) {
            this.lambda = lambda;
        }

        @Override
        public L0SubType subtype() {
            return L0SubType.OCH;
        }

        public OchSignal lambda() {
            return lambda;
        }

        @Override
        public int hashCode() {
            return lambda.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModOchSignalInstruction)) {
                return false;
            }
            final ModOchSignalInstruction that = (ModOchSignalInstruction) obj;
            return Objects.equals(this.lambda, that.lambda);
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + lambda;
        }
    }
}
