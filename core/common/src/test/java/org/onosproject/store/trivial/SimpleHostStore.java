/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.trivial;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostStore;
import org.onosproject.net.host.HostStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_MOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

// TODO: multi-provider, annotation not supported.
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
    private final Map<HostId, StoredHost> hosts = new ConcurrentHashMap<>(2000000, 0.75f, 16);

    // Hosts tracked by their location
    private final Multimap<ConnectPoint, Host> locations = HashMultimap.create();

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
                                        HostDescription hostDescription,
                                        boolean replaceIps) {
        //TODO We need a way to detect conflicting changes and abort update.
        StoredHost host = hosts.get(hostId);
        HostEvent hostEvent;
        if (host == null) {
            hostEvent = createHost(providerId, hostId, hostDescription);
        } else {
            hostEvent = updateHost(providerId, host, hostDescription, replaceIps);
        }
        notifyDelegate(hostEvent);
        return hostEvent;
    }

    // creates a new host and sends HOST_ADDED
    private HostEvent createHost(ProviderId providerId, HostId hostId,
                                 HostDescription descr) {
        StoredHost newhost = new StoredHost(providerId, hostId,
                                            descr.hwAddress(),
                                            descr.vlan(),
                                            descr.location(),
                                            ImmutableSet.copyOf(descr.ipAddress()),
                                            descr.annotations());
        synchronized (this) {
            hosts.put(hostId, newhost);
            locations.put(descr.location(), newhost);
        }
        return new HostEvent(HOST_ADDED, newhost);
    }

    // checks for type of update to host, sends appropriate event
    private HostEvent updateHost(ProviderId providerId, StoredHost host,
                                 HostDescription descr, boolean replaceIps) {
        HostEvent event;
        if (!host.location().equals(descr.location())) {
            host.setLocation(descr.location());
            return new HostEvent(HOST_MOVED, host);
        }

        if (host.ipAddresses().containsAll(descr.ipAddress()) &&
                descr.annotations().keys().isEmpty()) {
            return null;
        }

        final Set<IpAddress> addresses;
        if (replaceIps) {
            addresses = ImmutableSet.copyOf(descr.ipAddress());
        } else {
            addresses = new HashSet<>(host.ipAddresses());
            addresses.addAll(descr.ipAddress());
        }

        Annotations annotations = merge((DefaultAnnotations) host.annotations(),
                                        descr.annotations());
        StoredHost updated = new StoredHost(providerId, host.id(),
                                            host.mac(), host.vlan(),
                                            descr.location(), addresses,
                                            annotations);
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
                HostEvent hostEvent = new HostEvent(HOST_REMOVED, host);
                notifyDelegate(hostEvent);
                return hostEvent;
            }
            return null;
        }
    }

    @Override
    public HostEvent removeIp(HostId hostId, IpAddress ipAddress) {
        return null;
    }

    @Override
    public int getHostCount() {
        return hosts.size();
    }

    @Override
    public Iterable<Host> getHosts() {
        return ImmutableSet.copyOf(hosts.values());
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
    public Set<Host> getHosts(IpAddress ip) {
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

    // Auxiliary extension to allow location to mutate.
    private static final class StoredHost extends DefaultHost {
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
                          Set<IpAddress> ips, Annotations... annotations) {
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
