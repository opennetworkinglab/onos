package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.slf4j.Logger;

public final class DefaultTrafficTreatment implements TrafficTreatment {

    private final List<Instruction> instructions;

    private DefaultTrafficTreatment(List<Instruction> instructions) {
        this.instructions = Collections.unmodifiableList(instructions);
    }

    @Override
    public List<Instruction> instructions() {
        return instructions;
    }

    /**
     * Builds a list of treatments following the following order.
     * Modifications -> Group -> Output (including drop)
     *
     */

    public static class Builder implements TrafficTreatment.Builder {

        private final Logger log = getLogger(getClass());

        boolean drop = false;

        List<Instruction> outputs = new LinkedList<>();

        // TODO: should be a list of instructions based on group objects
        List<Instruction> groups = new LinkedList<>();

        // TODO: should be a list of instructions based on modification objects
        List<Instruction> modifications = new LinkedList<>();

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
            case L2MODIFICATION:
            case L3MODIFICATION:
                // TODO: enforce modification order if any
                modifications.add(instruction);
                break;
            case GROUP:
                groups.add(instruction);
                break;
            default:
                log.warn("Unknown instruction type {}", instruction.type());
            }
            return this;
        }

        @Override
        public void drop() {
            add(Instructions.createDrop());
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
        public Builder setIpSrc(IpPrefix addr) {
            return add(Instructions.modL3Src(addr));
        }

        @Override
        public Builder setIpDst(IpPrefix addr) {
            return add(Instructions.modL3Dst(addr));
        }

        @Override
        public TrafficTreatment build() {

            //If we are dropping should we just return an emptry list?
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
