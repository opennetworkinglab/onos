/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onlab.packet.TpPort;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstraction of a single traffic treatment step.
 */
public abstract class L4ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L4SubType {
        /**
         * TCP src modification.
         */
        TCP_SRC,

        /**
         * TCP dst modification.
         */
        TCP_DST,

        /**
         * UDP src modification.
         */
        UDP_SRC,

        /**
         * UDP dst modification.
         */
        UDP_DST

        //TODO: remaining types
    }

    /**
     * Returns the subtype of the modification instruction.
     *
     * @return type of instruction
     */
    public abstract L4SubType subtype();

    @Override
    public Type type() {
        return Type.L4MODIFICATION;
    }

    /**
     * Represents a L4 src/dst modification instruction.
     */
    public static final class ModTransportPortInstruction extends L4ModificationInstruction {

        private final L4SubType subtype;
        private final TpPort port;

        public ModTransportPortInstruction(L4SubType subtype, TpPort port) {
            this.subtype = subtype;
            this.port = port;
        }

        @Override
        public L4SubType subtype() {
            return this.subtype;
        }

        public TpPort port() {
            return this.port;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("port", port).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), port);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModTransportPortInstruction) {
                ModTransportPortInstruction that = (ModTransportPortInstruction) obj;
                return  Objects.equals(port, that.port) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }
}
