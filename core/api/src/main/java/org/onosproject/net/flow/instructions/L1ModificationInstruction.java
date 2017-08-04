/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.net.OduSignalId;

import java.util.Objects;

public abstract class L1ModificationInstruction implements Instruction {

    public static final String SEPARATOR = ":";

    /**
     * Represents the type of traffic treatment.
     */
    public enum L1SubType {
        /**
         * ODU (Optical channel Data Unit) Signal Id modification.
         */
        ODU_SIGID
    }

    public abstract L1SubType subtype();

    @Override
    public final Type type() {
        return Type.L1MODIFICATION;
    }

    /**
     * Represents an L1 ODU (Optical channel Data Unit) Signal Id modification instruction.
     */
    public static final class ModOduSignalIdInstruction extends L1ModificationInstruction {

        private final OduSignalId oduSignalId;

        ModOduSignalIdInstruction(OduSignalId oduSignalId) {
            this.oduSignalId = oduSignalId;
        }

        @Override
        public L1SubType subtype() {
            return L1SubType.ODU_SIGID;
        }

        public OduSignalId oduSignalId() {
            return oduSignalId;
        }

        @Override
        public int hashCode() {
            return oduSignalId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModOduSignalIdInstruction)) {
                return false;
            }
            final ModOduSignalIdInstruction that = (ModOduSignalIdInstruction) obj;
            return Objects.equals(this.oduSignalId, that.oduSignalId);
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + oduSignalId;
        }
    }

}
