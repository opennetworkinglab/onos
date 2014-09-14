package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.PortNumber;
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

        List<Instruction<PortNumber>> outputs = new LinkedList<>();

        // TODO: should be a list of instructions based on group objects
        List<Instruction<Object>> groups = new LinkedList<>();

        // TODO: should be a list of instructions based on modification objects
        List<Instruction<Object>> modifications = new LinkedList<>();


        @SuppressWarnings("unchecked")
        @Override
        public Builder add(Instruction instruction) {
            switch (instruction.type()) {
            case OUTPUT:
            case DROP:
                // TODO: should check that there is only one drop instruction.
                outputs.add(instruction);
                break;
            case MODIFICATION:
                // TODO: enforce modification order if any
                modifications.add(instruction);
            case GROUP:
                groups.add(instruction);
            default:
                log.warn("Unknown instruction type {}", instruction.type());
            }
            return this;
        }

        @Override
        public TrafficTreatment build() {
            List<Instruction> instructions = new LinkedList<Instruction>();
            instructions.addAll(modifications);
            instructions.addAll(groups);
            instructions.addAll(outputs);

            return new DefaultTrafficTreatment(instructions);
        }

    }

}
