/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.MulticastMappingInstruction.MulticastType;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction.UnicastType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.instructions.ExtensionTreatment;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of mapping treatment.
 */
public final class DefaultMappingTreatment implements MappingTreatment {

    private final List<MappingInstruction> instructions;
    private final MappingAddress address;

    /**
     * Create a new mapping treatment from the specified list of mapping instructions.
     *
     * @param instructions mapping instructions
     */
    private DefaultMappingTreatment(MappingAddress address,
                                    List<MappingInstruction> instructions) {
        this.address = address;
        this.instructions = ImmutableList.copyOf(checkNotNull(instructions));
    }

    @Override
    public MappingAddress address() {
        return address;
    }

    @Override
    public List<MappingInstruction> instructions() {
        return ImmutableList.copyOf(instructions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, instructions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMappingTreatment) {
            DefaultMappingTreatment that = (DefaultMappingTreatment) obj;
            return Objects.equals(address, that.address) &&
                    Objects.equals(instructions, that.instructions);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .add("mapping instructions", instructions)
                .toString();
    }

    /**
     * Returns a new mapping treatment builder.
     *
     * @return mapping treatment builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new mapping treatment builder primed to produce entities
     * patterned after the supplied mapping treatment.
     *
     * @param treatment base mapping treatment
     * @return mapping treatment builder
     */
    public static Builder builder(MappingTreatment treatment) {
        return new Builder(treatment);
    }

    /**
     * Builds a mapping treatment.
     */
    public static final class Builder implements MappingTreatment.Builder {

        private List<MappingInstruction> instructions = Lists.newArrayList();
        private MappingAddress address;
        private Map<UnicastType, Integer> unicastTypeMap = Maps.newConcurrentMap();
        private Map<MulticastType, Integer> multicastTypeMap = Maps.newConcurrentMap();

        // creates a new builder
        private Builder() {
            initTypeMap();
        }

        // creates a new builder based off an existing mapping treatment
        private Builder(MappingTreatment treatment) {
            treatment.instructions().forEach(i -> instructions.add(i));
            address = treatment.address();
            initTypeMap();
        }

        /**
         * Initializes type map.
         */
        private void initTypeMap() {
            unicastTypeMap.put(UnicastType.WEIGHT, 0);
            unicastTypeMap.put(UnicastType.PRIORITY, 0);
            multicastTypeMap.put(MulticastType.WEIGHT, 0);
            multicastTypeMap.put(MulticastType.PRIORITY, 0);
        }

        @Override
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        @Override
        public Builder add(MappingInstruction instruction) {

            switch (instruction.type()) {
                case UNICAST:
                    unicastTypeMap.compute(((UnicastMappingInstruction)
                            instruction).subtype(), (k, v) -> v + 1);
                    instructions.add(instruction);
                    break;
                case MULTICAST:
                    multicastTypeMap.compute(((MulticastMappingInstruction)
                            instruction).subtype(), (k, v) -> v + 1);
                    instructions.add(instruction);
                    break;
                case EXTENSION:
                    instructions.add(instruction);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown mapping " +
                            "instruction type: " + instruction.type());
            }

            return this;
        }

        @Override
        public Builder setUnicastWeight(int weight) {
            return add(MappingInstructions.unicastWeight(weight));
        }

        @Override
        public Builder setUnicastPriority(int priority) {
            return add(MappingInstructions.unicastPriority(priority));
        }

        @Override
        public Builder setMulticastWeight(int weight) {
            return add(MappingInstructions.multicastWeight(weight));
        }

        @Override
        public Builder setMulticastPriority(int priority) {
            return add(MappingInstructions.multicastPriority(priority));
        }

        @Override
        public Builder extension(ExtensionTreatment extension, DeviceId deviceId) {
            return add(MappingInstructions.extension(extension, deviceId));
        }

        @Override
        public MappingTreatment build() {

            unicastTypeMap.forEach((k, v) -> checkArgument(v <= 1,
                    "Should not specify more than one " + k.toString()));
            multicastTypeMap.forEach((k, v) -> checkArgument(v <= 1,
                    "Should not specify more than one " + k.toString()));

            return new DefaultMappingTreatment(address, instructions);
        }
    }
}
