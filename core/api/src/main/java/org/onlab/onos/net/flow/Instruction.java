package org.onlab.onos.net.flow;

/**
 * Abstraction of a single traffic treatment step.
 * @param <T> the type parameter for the instruction
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
         * Signifies that the traffic should be modified in some way.
         */
        MODIFICATION
    }

    // TODO: Create factory class 'Instructions' that will have various factory
    // to create specific instructions.

    /**
     * Returns the type of instruction not to be confused
     * with the instruction's java type.
     * @return type of instruction
     */
    public Type type();

}
