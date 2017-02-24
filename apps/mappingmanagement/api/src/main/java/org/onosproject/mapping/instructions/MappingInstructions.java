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

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.instructions.ExtensionTreatment;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.mapping.instructions.MulticastMappingInstruction.*;
import static org.onosproject.mapping.instructions.UnicastMappingInstruction.*;

/**
 * Factory class for creating various mapping instructions.
 */
public final class MappingInstructions {

    private static final String SEPARATOR = ":";

    /**
     * Prevents instantiation from external.
     */
    private MappingInstructions() {}

    /**
     * Creates an unicast weight instruction.
     *
     * @param weight weight value
     * @return an unicast mapping instruction
     */
    public static UnicastMappingInstruction unicastWeight(int weight) {
        return new UnicastMappingInstruction.WeightMappingInstruction(
                                            UnicastType.WEIGHT, weight);
    }

    /**
     * Creates an unicast priority instruction.
     *
     * @param priority priority value
     * @return an unicast mapping instruction
     */
    public static UnicastMappingInstruction unicastPriority(int priority) {
        return new UnicastMappingInstruction.PriorityMappingInstruction(
                                            UnicastType.PRIORITY, priority);
    }

    /**
     * Creates a multicast weight instruction.
     *
     * @param weight weight value
     * @return a multicast mapping instruction
     */
    public static MulticastMappingInstruction multicastWeight(int weight) {
        return new MulticastMappingInstruction.WeightMappingInstruction(
                                                MulticastType.WEIGHT, weight);
    }

    /**
     * Creates a multicast priority instruction.
     *
     * @param priority priority value
     * @return a multicast mapping instruction
     */
    public static MulticastMappingInstruction multicastPriority(int priority) {
        return new MulticastMappingInstruction.PriorityMappingInstruction(
                                                MulticastType.PRIORITY, priority);
    }

    /**
     * Creates an extension mapping instruction.
     *
     * @param extension extension mapping instruction
     * @param deviceId device identifier
     * @return extension mapping instruction
     */
    public static ExtensionMappingInstructionWrapper extension(ExtensionTreatment extension,
                                                               DeviceId deviceId) {
        checkNotNull(extension, "Extension instruction cannot be null");
        checkNotNull(deviceId, "Device ID cannot be null");
        return new ExtensionMappingInstructionWrapper(extension, deviceId);
    }

    /**
     * Extension mapping instruction.
     */
    public static class ExtensionMappingInstructionWrapper implements MappingInstruction {

        private final ExtensionTreatment extensionTreatment;
        private final DeviceId deviceId;

        /**
         * Defaults constructor for extension mapping instruction wrapper.
         *
         * @param extension extension treatment
         * @param deviceId  device identifier
         */
        ExtensionMappingInstructionWrapper(ExtensionTreatment extension, DeviceId deviceId) {
            this.extensionTreatment = extension;
            this.deviceId = deviceId;
        }

        /**
         * Obtains the extension treatment.
         *
         * @return extension treatment
         */
        public ExtensionTreatment extensionMappingInstruction() {
            return extensionTreatment;
        }

        /**
         * Obtains the device identifier.
         *
         * @return device identifer
         */
        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public Type type() {
            return Type.EXTENSION;
        }

        @Override
        public String toString() {
            return type().toString() + SEPARATOR + deviceId + "/" + extensionTreatment;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type().ordinal(), extensionTreatment, deviceId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ExtensionMappingInstructionWrapper) {
                ExtensionMappingInstructionWrapper that =
                                        (ExtensionMappingInstructionWrapper) obj;
                return Objects.equals(extensionTreatment, that.extensionTreatment)
                        && Objects.equals(deviceId, that.deviceId);

            }
            return false;
        }
    }
}
