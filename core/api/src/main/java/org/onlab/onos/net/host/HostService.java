package org.onlab.onos.net.host;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Host;

import java.util.Set;

/**
 * Service for interacting with the inventory of end-station hosts.
 */
public interface HostService {

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
    Host getHost(ElementId hostId); // TODO: change to HostId

    // TODO: determine which ones make sense or which we care to support
    // Set<Host> getHostsByVlan(VlanId vlan);
    // Set<Host> getHostsByMac(MacAddress mac);
    // Set<Host> getHostsByIp(IpAddress ip);

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
