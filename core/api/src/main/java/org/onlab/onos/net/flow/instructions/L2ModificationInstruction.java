package org.onlab.onos.net.flow.instructions;

import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;

/**
 * Abstraction of a single traffic treatment step.
 * @param <T> the type parameter for the instruction
 */
public abstract class L2ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L2SubType {
        /**
         * Ether src modification.
         */
        L2_SRC,

        /**
         * Ether dst modification.
         */
        L2_DST,

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

    public abstract L2SubType subtype();

    @Override
    public Type type() {
        return Type.L2MODIFICATION;
    }

    /**
     * Represents a L2 src/dst modification instruction.
     */
    public static final class ModEtherInstruction extends L2ModificationInstruction {

        private final L2SubType subtype;
        private final MACAddress mac;

        public ModEtherInstruction(L2SubType subType, MACAddress addr) {
            this.subtype = subType;
            this.mac = addr;
        }

        @Override
        public L2SubType subtype() {
            return this.subtype;
        }

        public MACAddress mac() {
            return this.mac;
        }

    }

    /**
     * Represents a VLAN id modification instruction.
     */
    public static final class ModVlanIdInstruction extends L2ModificationInstruction {

        public final VLANID vlanId;

        public ModVlanIdInstruction(VLANID vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.VLAN_ID;
        }

        public VLANID vlanId() {
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
        public L2SubType subtype() {
            return L2SubType.VLAN_PCP;
        }

        public Byte vlanPcp() {
            return this.vlanPcp;
        }

    }


}
