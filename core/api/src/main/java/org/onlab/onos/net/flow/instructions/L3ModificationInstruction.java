package org.onlab.onos.net.flow.instructions;

import org.onlab.packet.IPAddress;

/**
 * Abstraction of a single traffic treatment step.
 * @param <T> the type parameter for the instruction
 */
public abstract class L3ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L3SubType {
        /**
         * Ether src modification.
         */
        L3_SRC,

        /**
         * Ether dst modification.
         */
        L3_DST

        //TODO: remaining types
    }

    /**
     * Returns the subtype of the modification instruction.
     * @return type of instruction
     */
    public abstract L3SubType subtype();

    @Override
    public Type type() {
        return Type.L3MODIFICATION;
    }

    /**
     * Represents a L3 src/dst modification instruction.
     */
    public static final class ModIPInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final IPAddress ip;

        public ModIPInstruction(L3SubType subType, IPAddress addr) {
            this.subtype = subType;
            this.ip = addr;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public IPAddress ip() {
            return this.ip;
        }

    }
}
