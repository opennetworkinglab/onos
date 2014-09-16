package org.onlab.onos.net.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.net.PortNumber;
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

    // TODO: Move these out into separate classes and to flow.instruction package
    public static DropInstruction createDrop() {
        return new DropInstruction();
    }

    // TODO: add create methods

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
