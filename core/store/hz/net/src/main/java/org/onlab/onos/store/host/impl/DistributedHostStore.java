package org.onlab.onos.store.host.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.net.host.HostEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * TEMPORARY: Manages inventory of end-station hosts using distributed
 * structures implementation.
 */
//FIXME: I LIE I AM NOT DISTRIBUTED
@Component(immediate = true)
@Service
public class DistributedHostStore
        extends AbstractStore<HostEvent, HostStoreDelegate>
        implements HostStore {

    private final Logger log = getLogger(getClass());

    // Host inventory
    private final Map<HostId, StoredHost> hosts = new ConcurrentHashMap<>(2000000, 0.75f, 16);

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
        StoredHost host = hosts.get(hostId);
        if (host == null) {
            return createHost(providerId, hostId, hostDescription);
        }
        return updateHost(providerId, host, hostDescription);
    }

    // creates a new host and sends HOST_ADDED
    private HostEvent createHost(ProviderId providerId, HostId hostId,
                                 HostDescription descr) {
        StoredHost newhost = new StoredHost(providerId, hostId,
                                            descr.hwAddress(),
                                            descr.vlan(),
                                            descr.location(),
                                            ImmutableSet.of(descr.ipAddress()));
        synchronized (this) {
            hosts.put(hostId, newhost);
            locations.put(descr.location(), newhost);
        }
        return new HostEvent(HOST_ADDED, newhost);
    }

    // checks for type of update to host, sends appropriate event
    private HostEvent updateHost(ProviderId providerId, StoredHost host,
                                 HostDescription descr) {
        HostEvent event;
        if (!host.location().equals(descr.location())) {
            host.setLocation(descr.location());
            return new HostEvent(HOST_MOVED, host);
        }

        if (host.ipAddresses().contains(descr.ipAddress())) {
            return null;
        }

        Set<IpPrefix> addresses = new HashSet<>(host.ipAddresses());
        addresses.add(descr.ipAddress());
        StoredHost updated = new StoredHost(providerId, host.id(),
                                            host.mac(), host.vlan(),
                                            descr.location(), addresses);
        event = new HostEvent(HOST_UPDATED, updated);
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
        return ImmutableSet.<Host>copyOf(hosts.values());
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

    // Auxiliary extension to allow location to mutate.
    private class StoredHost extends DefaultHost {
        private HostLocation location;

        /**
         * Creates an end-station host using the supplied information.
         *
         * @param providerId  provider identity
         * @param id          host identifier
         * @param mac         host MAC address
         * @param vlan        host VLAN identifier
         * @param location    host location
         * @param ips         host IP addresses
         * @param annotations optional key/value annotations
         */
        public StoredHost(ProviderId providerId, HostId id,
                          MacAddress mac, VlanId vlan, HostLocation location,
                          Set<IpPrefix> ips, Annotations... annotations) {
            super(providerId, id, mac, vlan, location, ips, annotations);
            this.location = location;
        }

        void setLocation(HostLocation location) {
            this.location = location;
        }

        @Override
        public HostLocation location() {
            return location;
        }
    }
}
