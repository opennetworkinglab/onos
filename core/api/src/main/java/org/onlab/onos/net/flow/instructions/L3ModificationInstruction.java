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
    public enum L3SubType implements SubType {
        /**
         * Ether src modification.
         */
        L3_SRC,

        /**
         * Ether dst modification.
         */
        L3_DST,

        /**
         * Ethertype modification.
         */
        L3_PROTO,

        //TODO: remaining types
    }

    /**
     * Returns the subtype of the modification instruction.
     * @return type of instruction
     */
    public abstract SubType subtype();

    @Override
    public Type type() {
        return Type.MODIFICATION;
    }

    /**
     * Represents a L3 src/dst modification instruction.
     */
    public static final class ModIPInstruction extends L3ModificationInstruction {

        private final SubType subtype;
        private final IPAddress ip;

        public ModIPInstruction(SubType subType, IPAddress addr) {
            this.subtype = subType;
            this.ip = addr;
        }

        @Override
        public SubType subtype() {
            return this.subtype;
        }

        public IPAddress ip() {
            return this.ip;
        }

    }

    /**
     * Represents a L3 proto modification instruction.
     */
    public static final class ModIPProtoInstruction extends L3ModificationInstruction {

        public final Byte proto;

        public ModIPProtoInstruction(Byte proto) {
            this.proto = proto;
        }

        @Override
        public SubType subtype() {
            return L3SubType.L3_PROTO;
        }

        public short proto() {
            return this.proto;
        }

    }
}
