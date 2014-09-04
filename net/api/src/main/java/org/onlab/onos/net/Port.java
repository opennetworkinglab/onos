package org.onlab.onos.net;

import java.util.Set;

/**
 * Abstraction of a network port.
 */
public interface Port {

    /**
     * Port state.
     */
    enum State {
        UP, DOWN, BLOCKED, UNKNOWN
    }

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber number();

    /**
     * Returns the port state(s).
     *
     * @return port state set
     */
    Set<State> state();

    /**
     * Indicates whether or not the port is currently up and active.
     *
     * @return true if the port is operational
     */
    boolean isEnabled();

    /**
     * Indicates whether or not the port is administratively blocked.
     *
     * @return true if the port is blocked
     */
    boolean isBlocked();

    /**
     * Returns the identifier of the network element to which this port belongs.
     *
     * @return parent network element
     */
    Element parent();

    // set of port attributes

}
