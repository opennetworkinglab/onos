package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.flow.instructions.Instruction;
import org.slf4j.Logger;

@SuppressWarnings("rawtypes")
public class DefaultTrafficTreatment implements TrafficTreatment {

    private final List<Instruction> instructions;

    public DefaultTrafficTreatment(List<Instruction> instructions) {
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


        @Override
        public Builder add(Instruction instruction) {
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
