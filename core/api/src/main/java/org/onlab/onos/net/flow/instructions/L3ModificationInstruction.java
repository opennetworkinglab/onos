package org.onlab.onos.net.flow.instructions;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpPrefix;

/**
 * Abstraction of a single traffic treatment step.
 */
public abstract class L3ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L3SubType {
        /**
         * Ether src modification.
         */
        IP_SRC,

        /**
         * Ether dst modification.
         */
        IP_DST

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
        private final IpPrefix ip;

        public ModIPInstruction(L3SubType subType, IpPrefix addr) {

            this.subtype = subType;
            this.ip = addr;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public IpPrefix ip() {
            return this.ip;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("ip", ip).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModIPInstruction) {
                ModIPInstruction that = (ModIPInstruction) obj;
                return  Objects.equals(ip, that.ip) &&
                        Objects.equals(this.type(), that.type()) &&
                        Objects.equals(this.subtype(), that.subtype());

            }
            return false;
        }

    }
}
