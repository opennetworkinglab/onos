package org.onlab.onos.net.flow.instructions;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Abstraction of a single traffic treatment step.
 * @param <T> the type parameter for the instruction
 */
public abstract class L2ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L2SubType implements SubType {
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

    @Override
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
        private final MacAddress mac;

        public ModEtherInstruction(SubType subType, MacAddress addr) {
            this.subtype = subType;
            this.mac = addr;
        }

        @Override
        public SubType subtype() {
            return this.subtype;
        }

        public MacAddress mac() {
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
            return L2SubType.L2_TYPE;
        }

        public short l2Type() {
            return this.l2Type;
        }

    }

    /**
     * Represents a VLAN id modification instruction.
     */
    public static final class ModVlanIdInstruction extends L2ModificationInstruction {

        public final VlanId vlanId;

        public ModVlanIdInstruction(VlanId vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public SubType subtype() {
            return L2SubType.VLAN_ID;
        }

        public VlanId vlanId() {
            return this.vlanId;
        }

    }

    /**
     * Represents a VLAN PCP modification instruction.
     */
    public static final class ModVlanPcpInstruction extends L2ModificationInstruction {

        public final Byte vlanPcp;

        public ModVlanPcpInstruction(Byte vlanPcp) {
            this.vlanPcp = vlanPcp;
        }

        @Override
        public SubType subtype() {
            return L2SubType.VLAN_PCP;
        }

        public Byte vlanPcp() {
            return this.vlanPcp;
        }

    }


}
