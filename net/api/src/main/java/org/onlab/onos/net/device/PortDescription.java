package org.onlab.onos.net.device;

import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;

import java.util.Set;

/**
 * Information about a port.
 */
public interface PortDescription {

    // TODO: possibly relocate this to a common ground so that this can also used by host tracking if required

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Returns the port state set.
     *
     * @return set of port states
     */
    Set<Port.State> portState();

}
