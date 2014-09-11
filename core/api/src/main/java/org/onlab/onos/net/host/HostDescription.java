package org.onlab.onos.net.host;

import java.util.Set;

import org.onlab.onos.net.Description;
import org.onlab.onos.net.HostLocation;
import org.onlab.packet.IPAddress;
import org.onlab.packet.MACAddress;

/**
 * Information describing host and its location.
 */
public interface HostDescription extends Description {

    /**
     * Returns the MAC address associated with this host (NIC).
     *
     * @return the MAC address of this host
     */
    MACAddress hwAddress();

    /**
     * Returns the VLAN associated with this host.
     *
     * @return the VLAN ID value
     */
    short vlan();

    /**
     * Returns the location of the host on the network edge.
     *
     * @return the network location
     */
    HostLocation location();

    /**
     * Returns zero or more IP address(es) associated with this host's MAC.
     *
     * @return a set of IP addresses.
     */
    Set<IPAddress> ipAddresses();

}
