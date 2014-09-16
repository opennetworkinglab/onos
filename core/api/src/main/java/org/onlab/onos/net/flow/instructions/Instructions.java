package org.onlab.onos.net.flow.instructions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction.SubType;
import org.onlab.packet.MACAddress;
/**
 * Factory class for creating various traffic treatment instructions.
 */
public final class Instructions {


    // Ban construction
    private Instructions() {}

    /**
     * Creates an output instruction using the specified port number. This can
     * include logical ports such as CONTROLLER, FLOOD, etc.
     *
     * @param number port number
     * @return output instruction
     */
    public static OutputInstruction createOutput(final PortNumber number) {
        checkNotNull(number, "PortNumber cannot be null");
        return new OutputInstruction(number);
    }

    /**
     * Creates a drop instruction.
     * @return drop instruction
     */
    public static DropInstruction createDrop() {
        return new DropInstruction();
    }

    /**
     * Creates a l2 src modification.
     * @param addr the mac address to modify to.
     * @return a l2 modification
     */
    public static L2ModificationInstruction modL2Src(MACAddress addr) {
        checkNotNull(addr, "Src l2 address cannot be null");
        return new ModEtherInstruction(SubType.L2_SRC, addr);
    }

    /**
     * Creates a L2 dst modification.
     * @param addr the mac address to modify to.
     * @return a L2 modification
     */
    public static L2ModificationInstruction modL2Dst(MACAddress addr) {
        checkNotNull(addr, "Dst l2 address cannot be null");
        return new L2ModificationInstruction.ModEtherInstruction(SubType.L2_DST, addr);
    }

    /**
     * Creates a L2 type modification.
     * @param l2Type the type to change to
     * @return a L2 modifications
     */
    public static L2ModificationInstruction modL2Type(Short l2Type) {
        checkNotNull(l2Type, "L2 type cannot be null");
        return new L2ModificationInstruction.ModEtherTypeInstruction(l2Type);
    }

    public static L2ModificationInstruction modVlanId(Short vlanId) {
        checkNotNull(vlanId, "VLAN id cannot be null");
        return new L2ModificationInstruction.ModVlanIdInstruction(vlanId);
    }

    /*
     *  Output instructions
     */

    public static final class DropInstruction implements Instruction {
        @Override
        public Type type() {
            return Type.DROP;
        }
    }


    public static final class OutputInstruction implements Instruction {
        private final PortNumber port;

        private OutputInstruction(PortNumber port) {
            this.port = port;
        }

        public PortNumber port() {
            return port;
        }

        @Override
        public Type type() {
            return Type.OUTPUT;
        }
    }

}
