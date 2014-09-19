package org.onlab.onos.net;

import java.util.Set;

import org.onlab.packet.IpAddress;

/**
 * Abstraction of a network port.
 */
public interface Port {

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber number();

    /**
     * Indicates whether or not the port is currently up and active.
     *
     * @return true if the port is operational
     */
    boolean isEnabled();

    /**
     * Returns the parent network element to which this port belongs.
     *
     * @return parent network element
     */
    Element element();

    // set of port attributes

    /**
     * Returns the set of IP addresses that are logically configured on this
     * port.
     *
     * @return the set of IP addresses configured on the port. The set is empty
     * if no addresses are configured.
     */
    Set<IpAddress> ipAddresses();
}
