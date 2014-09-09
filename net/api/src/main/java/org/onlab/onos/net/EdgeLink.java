package org.onlab.onos.net;

/**
 * Abstraction of a link between an end-station host and the network
 * infrastructure.
 */
public interface EdgeLink extends Link {

    /**
     * Returns the host identification.
     *
     * @return host identifier
     */
    HostId hostId();

    /**
     * Returns the connection point where the host attaches to the
     * network infrastructure.
     *
     * @return host connection point
     */
    ConnectPoint connectPoint();

}
