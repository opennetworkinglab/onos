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
package org.onosproject.store.host.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.host.HostEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of hosts using a {@code EventuallyConsistentMap}.
 */
@Component(immediate = true)
@Service
public class DistributedHostStore
    extends AbstractStore<HostEvent, HostStoreDelegate>
    implements HostStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<HostId, DefaultHost> hostsConsistentMap;
    private Map<HostId, DefaultHost> hosts;

    private MapEventListener<HostId, DefaultHost> hostLocationTracker =
            new HostLocationTracker();

    @Activate
    public void activate() {
        KryoNamespace.Builder hostSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);

        hostsConsistentMap = storageService.<HostId, DefaultHost>consistentMapBuilder()
                .withName("onos-hosts")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(hostSerializer.build()))
                .build();

        hosts = hostsConsistentMap.asJavaMap();


        hostsConsistentMap.addListener(hostLocationTracker);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        hostsConsistentMap.removeListener(hostLocationTracker);

        log.info("Stopped");
    }

    private boolean shouldUpdate(DefaultHost existingHost,
                                 ProviderId providerId,
                                 HostId hostId,
                                 HostDescription hostDescription,
                                 boolean replaceIPs) {
        if (existingHost == null) {
            return true;
        }

        if (!Objects.equals(existingHost.providerId(), providerId) ||
                !Objects.equals(existingHost.mac(), hostDescription.hwAddress()) ||
                !Objects.equals(existingHost.vlan(), hostDescription.vlan()) ||
                !Objects.equals(existingHost.location(), hostDescription.location())) {
            return true;
        }

        if (replaceIPs) {
            if (!Objects.equals(hostDescription.ipAddress(),
                                existingHost.ipAddresses())) {
                return true;
            }
        } else {
            if (!existingHost.ipAddresses().containsAll(hostDescription.ipAddress())) {
                return true;
            }
        }

        // check to see if any of the annotations provided by hostDescription
        // differ from those in the existing host
        return hostDescription.annotations().keys().stream()
                    .anyMatch(k -> !Objects.equals(hostDescription.annotations().value(k),
                                                   existingHost.annotations().value(k)));


    }

    // TODO No longer need to return HostEvent
    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId,
                                        HostId hostId,
                                        HostDescription hostDescription,
                                        boolean replaceIPs) {
        hostsConsistentMap.computeIf(hostId,
                       existingHost -> shouldUpdate(existingHost, providerId, hostId,
                                                    hostDescription, replaceIPs),
                       (id, existingHost) -> {
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

    private class HostLocationTracker implements MapEventListener<HostId, DefaultHost> {
        @Override
        public void event(MapEvent<HostId, DefaultHost> event) {
            Host host;
            switch (event.type()) {
                case INSERT:
                    host = checkNotNull(event.newValue().value());
                    notifyDelegate(new HostEvent(HOST_ADDED, host));
                    break;
                case UPDATE:
                    host = checkNotNull(event.newValue().value());
                    Host prevHost = checkNotNull(event.oldValue().value());
                    if (!Objects.equals(prevHost.location(), host.location())) {
                        notifyDelegate(new HostEvent(HOST_MOVED, host, prevHost));
                    } else if (!Objects.equals(prevHost, host)) {
                        notifyDelegate(new HostEvent(HOST_UPDATED, host, prevHost));
                    }
                    break;
                case REMOVE:
                    host = checkNotNull(event.oldValue().value());
                    notifyDelegate(new HostEvent(HOST_REMOVED, host));
                    break;
                default:
                    log.warn("Unknown map event type: {}", event.type());
            }
        }
    }
}
