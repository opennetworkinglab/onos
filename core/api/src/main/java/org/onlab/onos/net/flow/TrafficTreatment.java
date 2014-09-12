package org.onlab.onos.net.flow;

import java.util.List;

/**
 * Abstraction of network traffic treatment.
 */
public interface TrafficTreatment {

    /**
     * Returns list of instructions on how to treat traffic.
     *
     * @return list of treatment instructions
     */
    List<Instruction> instructions();

    /**
     * Builder of traffic treatment entities.
     */
    public interface Builder {

        /**
         * Adds a traffic treatment instruction. If a same type instruction has
         * already been added, it will be replaced by this one.
         *
         * @param instruction new instruction
         */
        void add(Instruction instruction);

        /**
         * Builds an immutable traffic treatment descriptor.
         *
         * @return traffic treatment
         */
        TrafficTreatment build();
    }

}
