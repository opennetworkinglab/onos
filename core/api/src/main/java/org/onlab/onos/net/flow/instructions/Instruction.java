package org.onlab.onos.net.flow.instructions;

/**
 * Abstraction of a single traffic treatment step.
 */
public interface Instruction {

    interface SubType {}

    public enum NoneSubType implements SubType {
        NONE;
    }

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
     * Returns the type of instruction.
     * @return type of instruction
     */
    public Type type();

    /**
     * Returns the subtype of the modification instruction.
     * @return type of instruction
     */
    public SubType subtype();

}
