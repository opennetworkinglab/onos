package org.onlab.onos.net.flow.instructions;

import org.onlab.packet.MACAddress;

/**
 * Abstraction of a single traffic treatment step.
 * @param <T> the type parameter for the instruction
 */
public abstract class L2ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum SubType {
        /**
         * Ether src modification.
         */
        L2_SRC,

        /**
         * Ether dst modification.
         */
        L2_DST,

        /**
         * Ethertype modification.
         */
        L2_TYPE,

        /**
         * VLAN id modification.
         */
        VLAN_ID,

        /**
         * VLAN priority modification.
         */
        VLAN_PCP
    }

    // TODO: Create factory class 'Instructions' that will have various factory
    // to create specific instructions.

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
     * Represents a L2 src/dst modification instruction.
     */
    public static final class ModEtherInstruction extends L2ModificationInstruction {

        private final SubType subtype;
        private final MACAddress mac;

        public ModEtherInstruction(SubType subType, MACAddress addr) {
            this.subtype = subType;
            this.mac = addr;
        }

        @Override
        public SubType subtype() {
            return this.subtype;
        }

        public MACAddress mac() {
            return this.mac;
        }

    }

    /**
     * Represents a L2 type modification instruction.
     */
    public static final class ModEtherTypeInstruction extends L2ModificationInstruction {

        public final short l2Type;

        public ModEtherTypeInstruction(short l2Type) {
            this.l2Type = l2Type;
        }

        @Override
        public SubType subtype() {
            return SubType.L2_TYPE;
        }

        public short l2Type() {
            return this.l2Type;
        }

    }

    /**
     * Represents a VLAN id modification instruction.
     */
    public static final class ModVlanIdInstruction extends L2ModificationInstruction {

        public final Short vlanId;

        public ModVlanIdInstruction(Short vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public SubType subtype() {
            return SubType.VLAN_ID;
        }

        public Short vlanId() {
            return this.vlanId;
        }

    }


}
