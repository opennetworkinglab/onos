package org.onlab.onos.net.trivial.host.impl;

import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_MOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_UPDATED;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Manages inventory of end-station hosts using trivial in-memory
 * implementation.
 */
public class SimpleHostStore {

    private final Map<HostId, Host> hosts = new ConcurrentHashMap<>();

    // hosts sorted based on their location
    private final Multimap<ConnectPoint, Host> locations = HashMultimap.create();

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
        Host host = hosts.get(hostId);
        if (host == null) {
            return createHost(providerId, hostId, hostDescription);
        }
        return updateHost(providerId, host, hostDescription);
    }

    // creates a new host and sends HOST_ADDED
    private HostEvent createHost(ProviderId providerId, HostId hostId,
                                 HostDescription descr) {
        DefaultHost newhost = new DefaultHost(providerId, hostId,
                                              descr.hwAddress(),
                                              descr.vlan(),
                                              descr.location(),
                                              descr.ipAddresses());
        synchronized (this) {
            hosts.put(hostId, newhost);
            locations.put(descr.location(), newhost);
        }
        return new HostEvent(HOST_ADDED, newhost);
    }

    // checks for type of update to host, sends appropriate event
    private HostEvent updateHost(ProviderId providerId, Host host,
                                 HostDescription descr) {
        DefaultHost updated;
        HostEvent event;
        if (!host.location().equals(descr.location())) {
            updated = new DefaultHost(providerId, host.id(),
                                      host.mac(),
                                      host.vlan(),
                                      descr.location(),
                                      host.ipAddresses());
            event = new HostEvent(HOST_MOVED, updated);

        } else if (!(host.ipAddresses().equals(descr.ipAddresses()))) {
            updated = new DefaultHost(providerId, host.id(),
                                      host.mac(),
                                      host.vlan(),
                                      descr.location(),
                                      descr.ipAddresses());
            event = new HostEvent(HOST_UPDATED, updated);
        } else {
            return null;
        }
        synchronized (this) {
            hosts.put(host.id(), updated);
            locations.remove(host.location(), host);
            locations.put(updated.location(), updated);
        }
        return event;
    }

    /**
     * Removes the specified host from the inventory.
     *
     * @param hostId host identification
     * @return remove even or null if host was not found
     */
    HostEvent removeHost(HostId hostId) {
        synchronized (this) {
            Host host = hosts.remove(hostId);
            if (host != null) {
                locations.remove((host.location()), host);
                return new HostEvent(HOST_REMOVED, host);
            }
            return null;
        }
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
        return Collections.unmodifiableSet(new HashSet<>(hosts.values()));
    }

    /**
     * Returns the host with the specified identifer.
     *
     * @param hostId host identification
     * @return host or null if not found
     */
    Host getHost(HostId hostId) {
        return hosts.get(hostId);
    }

    /**
     * Returns the set of all hosts within the specified VLAN.
     *
     * @param vlanId vlan id
     * @return set of hosts in the vlan
     */
    Set<Host> getHosts(VlanId vlanId) {
        Set<Host> vlanset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.vlan().equals(vlanId)) {
                vlanset.add(h);
            }
        }
        return vlanset;
    }

    /**
     * Returns the set of hosts with the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHosts(MacAddress mac) {
        Set<Host> macset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.mac().equals(mac)) {
                macset.add(h);
            }
        }
        return macset;
    }

    /**
     * Returns the set of hosts with the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHosts(IpAddress ip) {
        Set<Host> ipset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.ipAddresses().contains(ip)) {
                ipset.add(h);
            }
        }
        return ipset;
    }

    /**
     * Returns the set of hosts whose location falls on the given connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return ImmutableSet.copyOf(locations.get(connectPoint));
    }

    /**
     * Returns the set of hosts whose location falls on the given device.
     *
     * @param deviceId infrastructure device identifier
     * @return set of hosts
     */
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        Set<Host> hostset = new HashSet<>();
        for (ConnectPoint p : locations.keySet()) {
            if (p.deviceId().equals(deviceId)) {
                hostset.addAll(locations.get(p));
            }
        }
        return hostset;
    }

}
