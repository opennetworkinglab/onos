package org.onlab.onos.net.flow.instructions;

/**
 * Abstraction of a single traffic treatment step.
 */
public interface Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum Type {
        /**
         * Signifies that the traffic should be dropped.
         */
        DROP,

        /**
         * Signifies that the traffic should be output to a port.
         */
        OUTPUT,

        /**
         * Signifies that.... (do we need this?)
         */
        GROUP,

        /**
         * Signifies that the traffic should be modified in L2 way.
         */
        L2MODIFICATION,

        /**
         * Signifies that the traffic should be modified in L3 way.
         */
        L3MODIFICATION
    }

    // TODO: Create factory class 'Instructions' that will have various factory
    // to create specific instructions.

    /**
     * Returns the type of instruction.
     * @return type of instruction
     */
    public Type type();

}
