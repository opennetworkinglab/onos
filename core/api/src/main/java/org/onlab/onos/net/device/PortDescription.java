package org.onlab.onos.net.device;

import org.onlab.onos.net.Description;
import org.onlab.onos.net.PortNumber;

/**
 * Information about a port.
 */
public interface PortDescription extends Description {

    // TODO: possibly relocate this to a common ground so that this can also used by host tracking if required

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Indicates whether or not the port is up and active.
     *
     * @return true if the port is active and has carrier signal
     */
    boolean isEnabled();

}
