/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import org.onosproject.net.OchSignal;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

public abstract class L0ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L0SubType {
        /**
         * Lambda modification.
         */
        LAMBDA,
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
     * Represents a L0 lambda modification instruction.
     */
    public static final class ModLambdaInstruction extends L0ModificationInstruction {

        private final L0SubType subtype;
        private final short lambda;

        ModLambdaInstruction(L0SubType subType, short lambda) {
            this.subtype = subType;
            this.lambda = lambda;
        }

        @Override
        public L0SubType subtype() {
            return this.subtype;
        }

        public short lambda() {
            return this.lambda;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("lambda", lambda).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype, lambda);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModLambdaInstruction) {
                ModLambdaInstruction that = (ModLambdaInstruction) obj;
                return  Objects.equals(lambda, that.lambda) &&
                        Objects.equals(subtype, that.subtype);
            }
            return false;
        }
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
            return MoreObjects.toStringHelper(this)
                    .add("lambda", lambda)
                    .toString();
        }
    }
}
