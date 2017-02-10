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
package org.onosproject.mapping.instructions;

import java.util.Objects;

/**
 * Abstraction of an uni-cast mapping traffic engineering.
 */
public abstract class UnicastMappingInstruction implements MappingInstruction {

    private static final String SEPARATOR = ":";

    /**
     * Represents the type of Unicast traffic engineering.
     */
    public enum UnicastType {

        /**
         * Signifies the weight value that used in unicast traffic engineering.
         */
        WEIGHT,

        /**
         * Signifies the priority value that used in unicast traffic engineering.
         */
        PRIORITY
    }

    public abstract UnicastType subtype();

    @Override
    public final Type type() {
        return Type.UNICAST;
    }

    /**
     * Represents an unicast weight configuration instruction.
     */
    public static final class WeightMappingInstruction extends UnicastMappingInstruction {

        private final UnicastType subtype;
        private final int weight;

        WeightMappingInstruction(UnicastType subType, int weight) {
            this.subtype = subType;
            this.weight = weight;
        }

        @Override
        public UnicastType subtype() {
            return subtype;
        }

        /**
         * Returns weight value of unicast TE.
         *
         * @return weight value of unicast TE
         */
        public int weight() {
            return this.weight;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + weight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype, weight);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof WeightMappingInstruction) {
                WeightMappingInstruction that = (WeightMappingInstruction) obj;
                return  Objects.equals(weight, that.weight) &&
                        Objects.equals(subtype, that.subtype);
            }
            return false;
        }
    }

    /**
     * Represents an unicast priority configuration instruction.
     */
    public static final class PriorityMappingInstruction extends UnicastMappingInstruction {

        private final UnicastType subtype;
        private final int priority;

        PriorityMappingInstruction(UnicastType subType, int priority) {
            this.subtype = subType;
            this.priority = priority;
        }

        @Override
        public UnicastType subtype() {
            return this.subtype;
        }

        /**
         * Returns priority value of unicast TE.
         *
         * @return priority value of unicast TE
         */
        public int priority() {
            return this.priority;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + priority;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype, priority);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PriorityMappingInstruction) {
                PriorityMappingInstruction that = (PriorityMappingInstruction) obj;
                return  Objects.equals(priority, that.priority) &&
                        Objects.equals(subtype, that.subtype);
            }
            return false;
        }
    }
}
