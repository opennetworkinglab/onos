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
 * Abstraction of an action mapping instruction.
 */
public abstract class ActionMappingInstruction implements MappingInstruction {

    /**
     * Represents the type of mapping action.
     */
    public enum ActionType {

        /**
         * Signifies that the traffic requires no action.
         */
        NO_ACTION,

        /**
         * Signifies that the traffic requires native forwarding.
         */
        NATIVE_FORWARD,

        /**
         * Signifies that the traffic requires forwarding with mapping information.
         */
        FORWARD,

        /**
         * Signifies that the traffic should be dropped.
         */
        DROP;
    }

    public abstract ActionType subtype();

    @Override
    public final Type type() {
        return Type.ACTION;
    }

    /**
     * Represents a No Action mapping instruction.
     */
    public static final class NoActionMappingInstruction
                                            extends ActionMappingInstruction {

        NoActionMappingInstruction() {
        }

        @Override
        public ActionType subtype() {
            return ActionType.NO_ACTION;
        }

        @Override
        public String toString() {
            return subtype().toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NoActionMappingInstruction) {
                return true;
            }
            return false;
        }
    }

    /**
     * Represents a Native Forward mapping instruction.
     */
    public static final class NativeForwardMappingInstruction
                                            extends ActionMappingInstruction {

        NativeForwardMappingInstruction() {
        }

        @Override
        public ActionType subtype() {
            return ActionType.NATIVE_FORWARD;
        }

        @Override
        public String toString() {
            return subtype().toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NativeForwardMappingInstruction) {
                return true;
            }
            return false;
        }
    }

    /**
     * Represents a Forward mapping instruction.
     */
    public static final class ForwardMappingInstruction
                                            extends ActionMappingInstruction {

        ForwardMappingInstruction() {
        }

        @Override
        public ActionType subtype() {
            return ActionType.FORWARD;
        }

        @Override
        public String toString() {
            return subtype().toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ForwardMappingInstruction) {
                return true;
            }
            return false;
        }
    }

    /**
     * Represents a Drop mapping instruction.
     */
    public static final class DropMappingInstruction
                                            extends ActionMappingInstruction {

        DropMappingInstruction() {
        }

        @Override
        public ActionType subtype() {
            return ActionType.DROP;
        }

        @Override
        public String toString() {
            return subtype().toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DropMappingInstruction) {
                return true;
            }
            return false;
        }
    }
}
