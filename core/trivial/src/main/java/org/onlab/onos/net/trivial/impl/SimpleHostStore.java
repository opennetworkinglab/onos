package org.onlab.onos.net.trivial.impl;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IPv4;
import org.onlab.packet.MACAddress;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages inventory of end-station hosts using trivial in-memory
 * implementation.
 */
public class SimpleHostStore {

    private final Map<HostId, Host> hosts = new ConcurrentHashMap<>();

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
                                 HostDescription hostDescription) {
        return null;
    }

    /**
     * Removes the specified host from the inventory.
     *
     * @param hostId host identification
     * @return remove even or null if host was not found
     */
    HostEvent removeHost(HostId hostId) {
        return null;
    }

    /**
     * Returns the number of hosts in the store.
     *
     * @return host count
     */
    int getHostCount() {
        return hosts.size();
    }

    /**
     * Returns a collection of all hosts in the store.
     *
     * @return iterable collection of all hosts
     */
    Iterable<Host> getHosts() {
        return null;
    }

    /**
     * Returns the host with the specified identifer.
     *
     * @param hostId host identification
     * @return host or null if not found
     */
    Host getHost(HostId hostId) {
        return null;
    }

    /**
     * Returns the set of all hosts within the specified VLAN.
     *
     * @param vlanId vlan id
     * @return set of hosts in the vlan
     */
    Set<Host> getHosts(long vlanId) {
        return null;
    }

    /**
     * Returns the set of hosts with the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHosts(MACAddress mac) {
        return null;
    }

    /**
     * Returns the set of hosts with the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHosts(IPv4 ip) {
        return null;
    }

    /**
     * Returns the set of hosts whose location falls on the given connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return null;
    }

    /**
     * Returns the set of hosts whose location falls on the given device.
     *
     * @param deviceId infrastructure device identifier
     * @return set of hosts
     */
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        return null;
    }

}
