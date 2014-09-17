package org.onlab.onos.net.host;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.Set;

/**
 * Service for interacting with the inventory of end-station hosts.
 */
public interface HostService {

    /**
     * Returns the number of end-station hosts known to the system.
     *
     * @return number of end-station hosts
     */
    public int getHostCount();

    /**
     * Returns a collection of all end-station hosts.
     *
     * @return collection of hosts
     */
    Iterable<Host> getHosts();

    /**
     * Returns the host with the specified identifier.
     *
     * @param hostId host identifier
     * @return host or null if one with the given identifier is not known
     */
    Host getHost(HostId hostId);

    /**
     * Returns the set of hosts that belong to the specified VLAN.
     *
     * @param vlanId vlan identifier
     * @return set of hosts in the given vlan id
     */
    // FIXME: change long to VLanId
    Set<Host> getHostsByVlan(VlanId vlanId);

    /**
     * Returns the set of hosts that have the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHostsByMac(MacAddress mac);

    /**
     * Returns the set of hosts that have the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHostsByIp(IpAddress ip);

    /**
     * Returns the set of hosts whose most recent location is the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts connected to the connection point
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint);

    /**
     * Returns the set of hosts whose most recent location is the specified
     * infrastructure device.
     *
     * @param deviceId device identifier
     * @return set of hosts connected to the device
     */
    Set<Host> getConnectedHosts(DeviceId deviceId);

    /**
     * Adds the specified host listener.
     *
     * @param listener host listener
     */
    void addListener(HostListener listener);

    /**
     * Removes the specified host listener.
     *
     * @param listener host listener
     */
    void removeListener(HostListener listener);

}
