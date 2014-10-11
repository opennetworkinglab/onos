package org.onlab.onos.net.host;

import org.onlab.onos.net.Description;
import org.onlab.onos.net.HostLocation;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Information describing host and its location.
 */
public interface HostDescription extends Description {

    /**
     * Returns the MAC address associated with this host (NIC).
     *
     * @return the MAC address of this host
     */
    MacAddress hwAddress();

    /**
     * Returns the VLAN associated with this host.
     *
     * @return the VLAN ID value
     */
    VlanId vlan();

    /**
     * Returns the location of the host on the network edge.
     *
     * @return the network location
     */
    HostLocation location();

    /**
     * Returns the IP address associated with this host's MAC.
     *
     * @return host IP address
     */
    IpPrefix ipAddress();

}
