/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.host.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_UPDATED;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
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
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Manages the inventory of hosts using a {@code EventuallyConsistentMap}.
 */
@Component(immediate = true)
@Service
public class ECHostStore
    extends AbstractStore<HostEvent, HostStoreDelegate>
    implements HostStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private EventuallyConsistentMap<HostId, DefaultHost> hosts;

    private final ConcurrentHashMap<HostId, HostLocation> locations =
            new ConcurrentHashMap<>();

    private EventuallyConsistentMapListener<HostId, DefaultHost> hostLocationTracker =
            new HostLocationTracker();

    @Activate
    public void activate() {
        KryoNamespace.Builder hostSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);

        hosts = storageService.<HostId, DefaultHost>eventuallyConsistentMapBuilder()
                .withName("onos-hosts")
                .withSerializer(hostSerializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        hosts.addListener(hostLocationTracker);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        hosts.removeListener(hostLocationTracker);
        hosts.destroy();
        locations.clear();

        log.info("Stopped");
    }

    // TODO No longer need to return HostEvent
    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId,
                                        HostId hostId,
                                        HostDescription hostDescription,
                                        boolean replaceIPs) {
        // TODO: We need a way to detect conflicting changes and abort update.
        //       (BOC) Compute might do this for us.

        hosts.compute(hostId, (id, existingHost) -> {
            HostLocation location = hostDescription.location();

            final Set<IpAddress> addresses;
            if (existingHost == null || replaceIPs) {
                addresses = ImmutableSet.copyOf(hostDescription.ipAddress());
            } else {
                addresses = Sets.newHashSet(existingHost.ipAddresses());
                addresses.addAll(hostDescription.ipAddress());
            }

            final Annotations annotations;
            if (existingHost != null) {
                annotations = merge((DefaultAnnotations) existingHost.annotations(),
                                    hostDescription.annotations());
            } else {
                annotations = hostDescription.annotations();
            }

            return new DefaultHost(providerId,
                                   hostId,
                                   hostDescription.hwAddress(),
                                   hostDescription.vlan(),
                                   location,
                                   addresses,
                                   annotations);
        });

        return null;
    }

    // TODO No longer need to return HostEvent
    @Override
    public HostEvent removeHost(HostId hostId) {
        hosts.remove(hostId);
        return null;
    }

    // TODO No longer need to return HostEvent
    @Override
    public HostEvent removeIp(HostId hostId, IpAddress ipAddress) {
        hosts.compute(hostId, (id, existingHost) -> {
            if (existingHost != null) {
                checkState(Objects.equals(hostId.mac(), existingHost.mac()),
                        "Existing and new MAC addresses differ.");
                checkState(Objects.equals(hostId.vlanId(), existingHost.vlan()),
                        "Existing and new VLANs differ.");

                Set<IpAddress> addresses = existingHost.ipAddresses();
                if (addresses != null && addresses.contains(ipAddress)) {
                    addresses = new HashSet<>(existingHost.ipAddresses());
                    addresses.remove(ipAddress);
                    return new DefaultHost(existingHost.providerId(),
                            hostId,
                            existingHost.mac(),
                            existingHost.vlan(),
                            existingHost.location(),
                            ImmutableSet.copyOf(addresses),
                            existingHost.annotations());
                } else {
                    return existingHost;
                }
            }
            return null;
        });
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
        return filter(hosts.values(), host -> Objects.equals(host.vlan(), vlanId));
    }

    @Override
    public Set<Host> getHosts(MacAddress mac) {
        return filter(hosts.values(), host -> Objects.equals(host.mac(), mac));
    }

    @Override
    public Set<Host> getHosts(IpAddress ip) {
        return filter(hosts.values(), host -> host.ipAddresses().contains(ip));
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        Set<Host> filtered = hosts.entrySet().stream()
                .filter(entry -> entry.getValue().location().equals(connectPoint))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(filtered);
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        Set<Host> filtered = hosts.entrySet().stream()
                .filter(entry -> entry.getValue().location().deviceId().equals(deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(filtered);
    }

    private Set<Host> filter(Collection<DefaultHost> collection, Predicate<DefaultHost> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toSet());
    }

    private class HostLocationTracker implements EventuallyConsistentMapListener<HostId, DefaultHost> {
        @Override
        public void event(EventuallyConsistentMapEvent<HostId, DefaultHost> event) {
            DefaultHost host = checkNotNull(event.value());
            if (event.type() == PUT) {
                HostLocation prevLocation = locations.put(host.id(), host.location());
                if (prevLocation == null) {
                    notifyDelegate(new HostEvent(HOST_ADDED, host));
                } else if (!Objects.equals(prevLocation, host.location())) {
                    notifyDelegate(new HostEvent(host, prevLocation));
                } else {
                    notifyDelegate(new HostEvent(HOST_UPDATED, host));
                }
            } else if (event.type() == REMOVE) {
                if (locations.remove(host.id()) != null) {
                    notifyDelegate(new HostEvent(HOST_REMOVED, host));
                }
            }
        }
    }
}
