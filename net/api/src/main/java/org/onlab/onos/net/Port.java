package org.onlab.onos.net;

/**
 * Abstraction of a network port.
 */
public interface Port {

    // Notion of port state: enabled, disabled, blocked

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber number();

    /**
     * Returns the identifier of the network element to which this port belongs.
     *
     * @return parent network element
     */
    Element parent();

    // set of port attributes

}
