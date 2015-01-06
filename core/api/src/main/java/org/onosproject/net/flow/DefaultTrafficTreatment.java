/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Default traffic treatment implementation.
 */
public final class DefaultTrafficTreatment implements TrafficTreatment {

    private final List<Instruction> instructions;

    /**
     * Creates a new traffic treatment from the specified list of instructions.
     *
     * @param instructions treatment instructions
     */
    private DefaultTrafficTreatment(List<Instruction> instructions) {
        this.instructions = ImmutableList.copyOf(instructions);
    }

    @Override
    public List<Instruction> instructions() {
        return instructions;
    }

    /**
     * Returns a new traffic treatment builder.
     *
     * @return traffic treatment builder
     */
    public static TrafficTreatment.Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new traffic treatment builder primed to produce entities
     * patterned after the supplied treatment.
     *
     * @param treatment base treatment
     * @return traffic treatment builder
     */
    public static TrafficTreatment.Builder builder(TrafficTreatment treatment) {
        return new Builder(treatment);
    }

    //FIXME: Order of instructions may affect hashcode
    @Override
    public int hashCode() {
        return Objects.hash(instructions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTrafficTreatment) {
            DefaultTrafficTreatment that = (DefaultTrafficTreatment) obj;
            return Objects.equals(instructions, that.instructions);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("instructions", instructions)
                .toString();
    }

    /**
     * Builds a list of treatments following the following order.
     * Modifications -&gt; Group -&gt; Output (including drop)
     */
    public static final class Builder implements TrafficTreatment.Builder {

        boolean drop = false;

        List<Instruction> outputs = new LinkedList<>();

        // TODO: should be a list of instructions based on group objects
        List<Instruction> groups = new LinkedList<>();

        // TODO: should be a list of instructions based on modification objects
        List<Instruction> modifications = new LinkedList<>();

        // Creates a new builder
        private Builder() {
        }

        // Creates a new builder based off an existing treatment
        private Builder(TrafficTreatment treatment) {
            for (Instruction instruction : treatment.instructions()) {
                add(instruction);
            }
        }

        @Override
        public Builder add(Instruction instruction) {
            if (drop) {
                return this;
            }
            switch (instruction.type()) {
                case DROP:
                    drop = true;
                    break;
                case OUTPUT:
                    outputs.add(instruction);
                    break;
                case L0MODIFICATION:
                case L2MODIFICATION:
                case L3MODIFICATION:
                    // TODO: enforce modification order if any
                    modifications.add(instruction);
                    break;
                case GROUP:
                    groups.add(instruction);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown instruction type: " +
                                                               instruction.type());
            }
            return this;
        }

        @Override
        public Builder drop() {
            return add(Instructions.createDrop());
        }

        @Override
        public Builder punt() {
            return add(Instructions.createOutput(PortNumber.CONTROLLER));
        }

        @Override
        public Builder setOutput(PortNumber number) {
            return add(Instructions.createOutput(number));
        }

        @Override
        public Builder setEthSrc(MacAddress addr) {
            return add(Instructions.modL2Src(addr));
        }

        @Override
        public Builder setEthDst(MacAddress addr) {
            return add(Instructions.modL2Dst(addr));
        }

        @Override
        public Builder setVlanId(VlanId id) {
            return add(Instructions.modVlanId(id));
        }

        @Override
        public Builder setVlanPcp(Byte pcp) {
            return add(Instructions.modVlanPcp(pcp));
        }

        @Override
        public Builder setIpSrc(IpAddress addr) {
            return add(Instructions.modL3Src(addr));
        }

        @Override
        public Builder setIpDst(IpAddress addr) {
            return add(Instructions.modL3Dst(addr));
        }

        @Override
        public Builder pushMpls() {
            return add(Instructions.pushMpls());
        }

        @Override
        public Builder popMpls() {
            return add(Instructions.popMpls());
        }


        @Override
        public Builder setMpls(Integer mplsLabel) {
            return add(Instructions.modMplsLabel(mplsLabel));
        }

        @Override
        public Builder setLambda(short lambda) {
            return add(Instructions.modL0Lambda(lambda));
        }

        @Override
        public TrafficTreatment build() {

            //If we are dropping should we just return an empty list?
            List<Instruction> instructions = new LinkedList<Instruction>();
            instructions.addAll(modifications);
            instructions.addAll(groups);
            if (!drop) {
                instructions.addAll(outputs);
            }

            return new DefaultTrafficTreatment(instructions);
        }

    }

}
