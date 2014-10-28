package org.onlab.onos.net;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.Set;

/**
 * Abstraction of an end-station host on the network, essentially a NIC.
 */
public interface Host extends Element {

    /**
     * Host identification.
     *
     * @return host id
     */
    @Override
    HostId id();

    /**
     * Returns the host MAC address.
     *
     * @return mac address
     */
    MacAddress mac();

    /**
     * Returns the VLAN ID tied to this host.
     *
     * @return VLAN ID value
     */
    VlanId vlan();

    /**
     * Returns set of IP addresses currently bound to the host MAC address.
     *
     * @return set of IP addresses; empty if no IP address is bound
     */
    Set<IpAddress> ipAddresses();

    /**
     * Returns the most recent host location where the host attaches to the
     * network edge.
     *
     * @return host location
     */
    HostLocation location();

    // TODO: explore capturing list of recent locations to aid in mobility

}
