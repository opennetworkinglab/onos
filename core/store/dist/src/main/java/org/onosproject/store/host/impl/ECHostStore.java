package org.onosproject.store.host.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
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
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostStore;
import org.onosproject.net.host.HostStoreDelegate;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.net.host.HostEvent.Type;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
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

    // Hosts tracked by their location
    private final SetMultimap<ConnectPoint, Host> locations =
            Multimaps.synchronizedSetMultimap(
                    HashMultimap.<ConnectPoint, Host>create());

    private final SetMultimap<ConnectPoint, PortAddresses> portAddresses =
            Multimaps.synchronizedSetMultimap(
                    HashMultimap.<ConnectPoint, PortAddresses>create());

    private EventuallyConsistentMap<HostId, DefaultHost> hosts;

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
        portAddresses.clear();

        log.info("Stopped");
    }

    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId,
            HostId hostId,
            HostDescription hostDescription) {
        DefaultHost currentHost = hosts.get(hostId);
        if (currentHost == null) {
            DefaultHost newhost = new DefaultHost(
                        providerId,
                        hostId,
                        hostDescription.hwAddress(),
                        hostDescription.vlan(),
                        hostDescription.location(),
                        ImmutableSet.copyOf(hostDescription.ipAddress()));
            hosts.put(hostId, newhost);
            return new HostEvent(HOST_ADDED, newhost);
        }
        return updateHost(providerId, hostId, hostDescription, currentHost);
    }

    @Override
    public HostEvent removeHost(HostId hostId) {
        Host host = hosts.remove(hostId);
        return host != null ? new HostEvent(HOST_REMOVED, host) : null;
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
        return ImmutableSet.copyOf(locations.get(connectPoint));
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        return locations.entries()
                .stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .map(entry -> entry.getValue())
                .collect(Collectors.toSet());
    }

    @Override
    public void updateAddressBindings(PortAddresses addresses) {
        portAddresses.put(addresses.connectPoint(), addresses);
    }

    @Override
    public void removeAddressBindings(PortAddresses addresses) {
        portAddresses.remove(addresses.connectPoint(), addresses);
    }

    @Override
    public void clearAddressBindings(ConnectPoint connectPoint) {
        portAddresses.removeAll(connectPoint);
    }

    @Override
    public Set<PortAddresses> getAddressBindings() {
        return ImmutableSet.copyOf(portAddresses.values());
    }

    @Override
    public Set<PortAddresses> getAddressBindingsForPort(ConnectPoint connectPoint) {
        synchronized (portAddresses) {
            Set<PortAddresses> addresses = portAddresses.get(connectPoint);
            return addresses == null ? Collections.emptySet() : ImmutableSet.copyOf(addresses);
        }
    }

    private Set<Host> filter(Collection<DefaultHost> collection, Predicate<DefaultHost> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toSet());
    }

    // checks for type of update to host, sends appropriate event
    private HostEvent updateHost(ProviderId providerId,
                                 HostId hostId,
                                 HostDescription descr,
                                 DefaultHost currentHost) {

        final boolean hostMoved = !currentHost.location().equals(descr.location());
        if (hostMoved ||
                !currentHost.ipAddresses().containsAll(descr.ipAddress()) ||
                !descr.annotations().keys().isEmpty()) {

            Set<IpAddress> addresses = Sets.newHashSet(currentHost.ipAddresses());
            addresses.addAll(descr.ipAddress());
            Annotations annotations = merge((DefaultAnnotations) currentHost.annotations(),
                                            descr.annotations());

            DefaultHost updatedHost = new DefaultHost(providerId, currentHost.id(),
                                                currentHost.mac(), currentHost.vlan(),
                                                descr.location(),
                                                addresses,
                                                annotations);

            // TODO: We need a way to detect conflicting changes and abort update.
            hosts.put(hostId, updatedHost);
            locations.remove(currentHost.location(), currentHost);
            locations.put(updatedHost.location(), updatedHost);

            HostEvent.Type eventType = hostMoved ? Type.HOST_MOVED : Type.HOST_UPDATED;
            return new HostEvent(eventType, updatedHost);
        }
        return null;
    }

    private class HostLocationTracker implements EventuallyConsistentMapListener<HostId, DefaultHost> {

        @Override
        public void event(EventuallyConsistentMapEvent<HostId, DefaultHost> event) {
            DefaultHost host = checkNotNull(event.value());
            if (event.type() == PUT) {
                locations.put(host.location(), host);
            } else if (event.type() == REMOVE) {
                locations.remove(host.location(), host);
            }
        }
    }
}
