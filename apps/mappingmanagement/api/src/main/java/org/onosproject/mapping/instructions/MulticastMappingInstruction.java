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
 * Abstraction of a multi-cast mapping traffic engineering.
 */
public abstract class MulticastMappingInstruction implements MappingInstruction {

    private static final String SEPARATOR = ":";

    /**
     * Represents the type of Multicast traffic engineering.
     */
    public enum MulticastType {

        /**
         * Signifies the weight value that used in multicast traffic engineering.
         */
        WEIGHT,

        /**
         * Signifies the priority value that used in multicast traffic engineering.
         */
        PRIORITY
    }

    /**
     * Obtains the subtype.
     *
     * @return subtype
     */
    public abstract MulticastType subtype();

    @Override
    public final Type type() {
        return Type.MULTICAST;
    }

    /**
     * Represents a multicast weight configuration instruction.
     */
    public static final class WeightMappingInstruction extends MulticastMappingInstruction {

        private final MulticastType subtype;
        private final int weight;

        /**
         * Default constructor for weight mapping instruction.
         *
         * @param subType multicast subtype
         * @param weight  weight value
         */
        WeightMappingInstruction(MulticastType subType, int weight) {
            this.subtype = subType;
            this.weight = weight;
        }

        @Override
        public MulticastType subtype() {
            return this.subtype;
        }

        /**
         * Returns weight value of multicast TE.
         *
         * @return weight value of multicast TE
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
     * Represents a multicast priority configuration instruction.
     */
    public static final class PriorityMappingInstruction extends MulticastMappingInstruction {

        private final MulticastType subtype;
        private final int priority;

        /**
         * Default constructor for priority mapping instruction.
         *
         * @param subType  multicast subtype
         * @param priority priority value
         */
        PriorityMappingInstruction(MulticastType subType, int priority) {
            this.subtype = subType;
            this.priority = priority;
        }

        @Override
        public MulticastType subtype() {
            return this.subtype;
        }

        /**
         * Returns priority value of multicast TE.
         *
         * @return priority value of multicast TE
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
