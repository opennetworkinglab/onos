package org.onlab.onos.net.flow;

import org.onlab.onos.net.PortNumber;

/**
 * Factory class for creating various traffic treatment instructions.
 */
public final class Instructions {

    // Ban construction
    private Instructions() {
    }

    /**
     * Creates an output instruction using the specified port number. This can
     * include logical ports such as CONTROLLER, FLOOD, etc.
     *
     * @param number port number
     * @return output instruction
     */
    public static Instruction createOutput(PortNumber number) {
        return null;
    }

    // TODO: add create methods

}
