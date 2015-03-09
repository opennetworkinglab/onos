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
import com.google.common.collect.Lists;
import org.apache.commons.collections.ListUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default traffic treatment implementation.
 */
public final class DefaultTrafficTreatment implements TrafficTreatment {

    private final List<Instruction> immediate;
    private final List<Instruction> deferred;
    private final Instructions.TableTypeTransition table;

    private final boolean hasClear;

    private static final DefaultTrafficTreatment EMPTY
            = new DefaultTrafficTreatment(Collections.emptyList());

    /**
     * Creates a new traffic treatment from the specified list of instructions.
     *
     * @param instructions treatment instructions
     */
    private DefaultTrafficTreatment(List<Instruction> instructions) {
        this.immediate = ImmutableList.copyOf(instructions);
        this.deferred = ImmutableList.of();
        this.hasClear = false;
        this.table = null;
    }

    private DefaultTrafficTreatment(List<Instruction> deferred,
                                   List<Instruction> immediate,
                                   Instructions.TableTypeTransition table,
                                   boolean clear) {
        this.immediate = ImmutableList.copyOf(immediate);
        this.deferred = ImmutableList.copyOf(deferred);
        this.table = table;
        this.hasClear = clear;

    }

    @Override
    public List<Instruction> instructions() {
        return immediate;
    }

    @Override
    public List<Instruction> deferred() {
        return deferred;
    }

    @Override
    public List<Instruction> immediate() {
        return immediate;
    }

    @Override
    public List<Instruction> allInstructions() {
        return ListUtils.union(immediate, deferred);
    }

    @Override
    public Instructions.TableTypeTransition tableTransition() {
        return table;
    }

    @Override
    public Boolean clearedDeferred() {
        return hasClear;
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
     * Returns an empty traffic treatment.
     *
     * @return empty traffic treatment
     */
    public static TrafficTreatment emptyTreatment() {
        return EMPTY;
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
        return Objects.hash(immediate, deferred, table);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTrafficTreatment) {
            DefaultTrafficTreatment that = (DefaultTrafficTreatment) obj;
            return Objects.equals(immediate, that.immediate) &&
                    Objects.equals(deferred, that.deferred) &&
                    Objects.equals(table, that.table);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("immediate", immediate)
                .add("deferred", deferred)
                .add("transition", table == null ? "None" : table.toString())
                .add("cleared", hasClear)
                .toString();
    }

    /**
     * Builds a list of treatments following the following order.
     * Modifications -&gt; Group -&gt; Output (including drop)
     */
    public static final class Builder implements TrafficTreatment.Builder {

        boolean drop = false;

        boolean clear = false;

        Instructions.TableTypeTransition table;

        List<Instruction> deferred = Lists.newLinkedList();

        List<Instruction> immediate = Lists.newLinkedList();

        List<Instruction> current = immediate;

        // Creates a new builder
        private Builder() {
        }

        // Creates a new builder based off an existing treatment
        //FIXME only works for immediate instruction sets.
        private Builder(TrafficTreatment treatment) {
            for (Instruction instruction : treatment.instructions()) {
                add(instruction);
            }
        }

        @Override
        public Builder add(Instruction instruction) {

            switch (instruction.type()) {
                case DROP:
                case OUTPUT:
                case GROUP:
                case L0MODIFICATION:
                case L2MODIFICATION:
                case L3MODIFICATION:
                    current.add(instruction);
                    break;
                case TABLE:
                    table = (Instructions.TableTypeTransition) instruction;
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

        public Builder stripVlan() {
            return add(Instructions.stripVlanId());
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
        public Builder decNwTtl() {
            return add(Instructions.decNwTtl());
        }

        @Override
        public Builder copyTtlIn() {
            return add(Instructions.copyTtlIn());
        }

        @Override
        public Builder copyTtlOut() {
            return add(Instructions.copyTtlOut());
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
        public Builder popMpls(Short etherType) {
            return add(Instructions.popMpls(etherType));
        }

        @Override
        public Builder setMpls(MplsLabel mplsLabel) {
            return add(Instructions.modMplsLabel(mplsLabel));
        }

        @Override
        public Builder decMplsTtl() {
            return add(Instructions.decMplsTtl());
        }

        @Override
        public Builder setLambda(short lambda) {
            return add(Instructions.modL0Lambda(lambda));
        }

        @Override
        public Builder group(GroupId groupId) {
            return add(Instructions.createGroup(groupId));
        }

        @Override
        public Builder popVlan() {
            return add(Instructions.popVlan());
        }

        @Override
        public Builder pushVlan() {
            return add(Instructions.pushVlan());
        }

        @Override
        public Builder transition(FlowRule.Type type) {
            return add(Instructions.transition(type));
        }

        @Override
        public Builder immediate() {
            current = immediate;
            return this;
        }

        @Override
        public Builder deferred() {
            current = deferred;
            return this;
        }

        @Override
        public Builder wipeDeferred() {
            clear = true;
            return this;
        }

        @Override
        public TrafficTreatment build() {
            if (deferred.size() == 0 && immediate.size() == 0
                    && table == null && !clear) {
                drop();
            }
            return new DefaultTrafficTreatment(deferred, immediate, table, clear);
        }

    }

}
