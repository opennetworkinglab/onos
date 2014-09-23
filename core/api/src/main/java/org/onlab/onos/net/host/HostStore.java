package org.onlab.onos.net.host;

import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Manages inventory of end-station hosts; not intended for direct use.
 */
public interface HostStore {

    /**
     * Creates a new host or updates the existing one based on the specified
     * description.
     *
     * @param providerId      provider identification
     * @param hostId          host identification
     * @param hostDescription host description data
     * @return appropriate event or null if no change resulted
     */
    HostEvent createOrUpdateHost(ProviderId providerId, HostId hostId,
                                 HostDescription hostDescription);

    /**
     * Removes the specified host from the inventory.
     *
     * @param hostId host identification
     * @return remove event or null if host was not found
     */
    HostEvent removeHost(HostId hostId);

    /**
     * Returns the number of hosts in the store.
     *
     * @return host count
     */
    int getHostCount();

    /**
     * Returns a collection of all hosts in the store.
     *
     * @return iterable collection of all hosts
     */
    Iterable<Host> getHosts();

    /**
     * Returns the host with the specified identifer.
     *
     * @param hostId host identification
     * @return host or null if not found
     */
    Host getHost(HostId hostId);

    /**
     * Returns the set of all hosts within the specified VLAN.
     *
     * @param vlanId vlan id
     * @return set of hosts in the vlan
     */
    Set<Host> getHosts(VlanId vlanId);

    /**
     * Returns the set of hosts with the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHosts(MacAddress mac);

    /**
     * Returns the set of hosts with the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHosts(IpPrefix ip);

    /**
     * Returns the set of hosts whose location falls on the given connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint);

    /**
     * Returns the set of hosts whose location falls on the given device.
     *
     * @param deviceId infrastructure device identifier
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(DeviceId deviceId);

    /**
     * Updates the address information for a given port. The given address
     * information is added to any previously held information for the port.
     *
     * @param addresses the port and address information
     */
    void updateAddressBindings(PortAddresses addresses);

    /**
     * Removes the given addresses from the set of address information held for
     * a port.
     *
     * @param addresses the port and address information
     */
    void removeAddressBindings(PortAddresses addresses);

    /**
     * Removes any previously stored address information for a given connection
     * point.
     *
     * @param connectPoint the connection point
     */
    void clearAddressBindings(ConnectPoint connectPoint);

    /**
     * Returns the address bindings stored for all connection points.
     *
     * @return the set of address bindings
     */
    Set<PortAddresses> getAddressBindings();

    /**
     * Returns the address bindings for a particular connection point.
     *
     * @param connectPoint the connection point to return address information
     * for
     * @return address information for the connection point
     */
    PortAddresses getAddressBindingsForPort(ConnectPoint connectPoint);
}
