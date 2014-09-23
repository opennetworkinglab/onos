package org.onlab.onos.net.host;

import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.HostId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

/**
 * Service for administering the inventory of end-station hosts.
 */
public interface HostAdminService {

    /**
     * Removes the end-station host with the specified identifier.
     *
     * @param hostId host identifier
     */
    void removeHost(HostId hostId);

    /**
     * Binds an IP address and optional MAC address to the given connection
     * point.
     * <p/>
     * This method will overwrite any previously held address information for
     * the connection point.
     *
     * @param ip the IP address to bind to the connection point. This parameter
     * is mandatory and cannot be null.
     * @param mac the optional MAC address to bind to the connection point. Can
     * be set to null if no MAC address needs to be bound.
     * @param connectPoint the connection point to bind the addresses to
     */
    void bindAddressesToPort(IpAddress ip, MacAddress mac, ConnectPoint connectPoint);

    /**
     * Removes all address information for the given connection point.
     *
     * @param connectPoint the connection point to remove address information
     */
    void unbindAddressesFromPort(ConnectPoint connectPoint);

    /**
     * Returns the addresses information for all connection points.
     *
     * @return the set of address bindings for all connection points
     */
    Set<PortAddresses> getAddressBindings();

    /**
     * Retrieves the addresses that have been bound to the given connection
     * point.
     *
     * @param connectPoint the connection point to retrieve address bindings
     * for
     * @return addresses bound to the port
     */
    PortAddresses getAddressBindingsForPort(ConnectPoint connectPoint);
}
