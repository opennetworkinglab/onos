package org.onlab.onos.net.host;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

/**
 * Represents address information bound to a port.
 */
public interface PortAddresses {

    /**
     * Returns the connection point this address information is bound to.
     *
     * @return the connection point
     */
    ConnectPoint connectPoint();

    /**
     * Returns the IP address bound to the port.
     *
     * @return the IP address
     */
    IpAddress ip();

    /**
     * Returns the MAC address bound to the port.
     *
     * @return the MAC address if one is bound, otherwise null
     */
    MacAddress mac();
}
