package org.onlab.onos.cluster;

import org.onlab.packet.IpPrefix;

/**
 * Represents a controller instance as a member in a cluster.
 */
public interface ControllerNode {

    /** Represents the operational state of the instance. */
    public enum State {
        /**
         * Signifies that the instance is active and operating normally.
         */
        ACTIVE,

        /**
         * Signifies that the instance is inactive, which means either down or
         * up, but not operational.
         */
        INACTIVE
    }

    /**
     * Returns the instance identifier.
     *
     * @return instance identifier
     */
    NodeId id();

    /**
     * Returns the IP address of the controller instance.
     *
     * @return IP address
     */
    IpPrefix ip();

}
