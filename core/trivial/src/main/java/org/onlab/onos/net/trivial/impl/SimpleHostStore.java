package org.onlab.onos.net.trivial.impl;

import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_MOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostStore;
import org.onlab.onos.net.host.HostStoreDelegate;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.slf4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Manages inventory of end-station hosts using trivial in-memory
 * implementation.
 */
@Component(immediate = true)
@Service
public class SimpleHostStore
        extends AbstractStore<HostEvent, HostStoreDelegate>
        implements HostStore {

    private final Logger log = getLogger(getClass());

    // Host inventory
    private final Map<HostId, Host> hosts = new ConcurrentHashMap<>();

    // Hosts tracked by their location
    private final Multimap<ConnectPoint, Host> locations = HashMultimap.create();

    private final Map<ConnectPoint, PortAddresses> portAddresses =
            new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId, HostId hostId,
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

    @Override
    public HostEvent removeHost(HostId hostId) {
        synchronized (this) {
            Host host = hosts.remove(hostId);
            if (host != null) {
                locations.remove((host.location()), host);
                return new HostEvent(HOST_REMOVED, host);
            }
            return null;
        }
    }

    @Override
    public int getHostCount() {
        return hosts.size();
    }

    @Override
    public Iterable<Host> getHosts() {
        return Collections.unmodifiableSet(new HashSet<>(hosts.values()));
    }

    @Override
    public Host getHost(HostId hostId) {
        return hosts.get(hostId);
    }

    @Override
    public Set<Host> getHosts(VlanId vlanId) {
        Set<Host> vlanset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.vlan().equals(vlanId)) {
                vlanset.add(h);
            }
        }
        return vlanset;
    }

    @Override
    public Set<Host> getHosts(MacAddress mac) {
        Set<Host> macset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.mac().equals(mac)) {
                macset.add(h);
            }
        }
        return macset;
    }

    @Override
    public Set<Host> getHosts(IpPrefix ip) {
        Set<Host> ipset = new HashSet<>();
        for (Host h : hosts.values()) {
            if (h.ipAddresses().contains(ip)) {
                ipset.add(h);
            }
        }
        return ipset;
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return ImmutableSet.copyOf(locations.get(connectPoint));
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        Set<Host> hostset = new HashSet<>();
        for (ConnectPoint p : locations.keySet()) {
            if (p.deviceId().equals(deviceId)) {
                hostset.addAll(locations.get(p));
            }
        }
        return hostset;
    }

    @Override
    public void updateAddressBindings(PortAddresses addresses) {
        synchronized (portAddresses) {
            PortAddresses existing = portAddresses.get(addresses.connectPoint());
            if (existing == null) {
                portAddresses.put(addresses.connectPoint(), addresses);
            } else {
                Set<IpPrefix> union = Sets.union(existing.ips(), addresses.ips())
                        .immutableCopy();

                MacAddress newMac = (addresses.mac() == null) ? existing.mac()
                        : addresses.mac();

                PortAddresses newAddresses =
                        new PortAddresses(addresses.connectPoint(), union, newMac);

                portAddresses.put(newAddresses.connectPoint(), newAddresses);
            }
        }
    }

    @Override
    public void removeAddressBindings(PortAddresses addresses) {
        synchronized (portAddresses) {
            PortAddresses existing = portAddresses.get(addresses.connectPoint());
            if (existing != null) {
                Set<IpPrefix> difference =
                        Sets.difference(existing.ips(), addresses.ips()).immutableCopy();

                // If they removed the existing mac, set the new mac to null.
                // Otherwise, keep the existing mac.
                MacAddress newMac = existing.mac();
                if (addresses.mac() != null && addresses.mac().equals(existing.mac())) {
                    newMac = null;
                }

                PortAddresses newAddresses =
                        new PortAddresses(addresses.connectPoint(), difference, newMac);

                portAddresses.put(newAddresses.connectPoint(), newAddresses);
            }
        }
    }

    @Override
    public void clearAddressBindings(ConnectPoint connectPoint) {
        synchronized (portAddresses) {
            portAddresses.remove(connectPoint);
        }
    }

    @Override
    public Set<PortAddresses> getAddressBindings() {
        synchronized (portAddresses) {
            return new HashSet<>(portAddresses.values());
        }
    }

    @Override
    public PortAddresses getAddressBindingsForPort(ConnectPoint connectPoint) {
        PortAddresses addresses;

        synchronized (portAddresses) {
            addresses = portAddresses.get(connectPoint);
        }

        if (addresses == null) {
            addresses = new PortAddresses(connectPoint, null, null);
        }

        return addresses;
    }

}
