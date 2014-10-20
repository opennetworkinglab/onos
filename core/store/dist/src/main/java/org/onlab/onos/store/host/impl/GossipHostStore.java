package org.onlab.onos.store.host.impl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostClockService;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostStore;
import org.onlab.onos.net.host.HostStoreDelegate;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.impl.Timestamped;
import org.onlab.onos.store.serializers.DistributedStoreSerializers;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.onos.cluster.ControllerNodeToNodeId.toNodeId;
import static org.onlab.onos.net.host.HostEvent.Type.*;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

//TODO: multi-provider, annotation not supported.
/**
 * Manages inventory of end-station hosts in distributed data store
 * that uses optimistic replication and gossip based techniques.
 */
@Component(immediate = true)
@Service
public class GossipHostStore
        extends AbstractStore<HostEvent, HostStoreDelegate>
        implements HostStore {

    private final Logger log = getLogger(getClass());

    // Host inventory
    private final Map<HostId, StoredHost> hosts = new ConcurrentHashMap<>(2000000, 0.75f, 16);

    private final Map<HostId, Timestamped<Host>> removedHosts = new ConcurrentHashMap<>(2000000, 0.75f, 16);

    // Hosts tracked by their location
    private final Multimap<ConnectPoint, Host> locations = HashMultimap.create();

    private final Map<ConnectPoint, PortAddresses> portAddresses =
            new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostClockService hostClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.COMMON)
                    .register(InternalHostEvent.class)
                    .register(InternalHostRemovedEvent.class)
                    .register(HostFragmentId.class)
                    .register(HostAntiEntropyAdvertisement.class)
                    .build()
                    .populate(1);
        }
    };

    private ScheduledExecutorService executor;

    @Activate
    public void activate() {
        clusterCommunicator.addSubscriber(
                GossipHostStoreMessageSubjects.HOST_UPDATED,
                new InternalHostEventListener());
        clusterCommunicator.addSubscriber(
                GossipHostStoreMessageSubjects.HOST_REMOVED,
                new InternalHostRemovedEventListener());
        clusterCommunicator.addSubscriber(
                GossipHostStoreMessageSubjects.HOST_ANTI_ENTROPY_ADVERTISEMENT,
                new InternalHostAntiEntropyAdvertisementListener());

        executor =
                newSingleThreadScheduledExecutor(namedThreads("link-anti-entropy-%d"));

        // TODO: Make these configurable
        long initialDelaySec = 5;
        long periodSec = 5;
        // start anti-entropy thread
        executor.scheduleAtFixedRate(new SendAdvertisementTask(),
                    initialDelaySec, periodSec, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Timeout during executor shutdown");
            }
        } catch (InterruptedException e) {
            log.error("Error during executor shutdown", e);
        }

        hosts.clear();
        removedHosts.clear();
        locations.clear();
        portAddresses.clear();

        log.info("Stopped");
    }

    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId, HostId hostId,
                                        HostDescription hostDescription) {
        Timestamp timestamp = hostClockService.getTimestamp(hostId);
        HostEvent event = createOrUpdateHostInternal(providerId, hostId, hostDescription, timestamp);
        if (event != null) {
            log.info("Notifying peers of a host topology event for providerId: "
                    + "{}; hostId: {}; hostDescription: {}", providerId, hostId, hostDescription);
            try {
                notifyPeers(new InternalHostEvent(providerId, hostId, hostDescription, timestamp));
            } catch (IOException e) {
                log.error("Failed to notify peers of a host topology event for providerId: "
                        + "{}; hostId: {}; hostDescription: {}", providerId, hostId, hostDescription);
            }
        }
        return event;
    }

    private HostEvent createOrUpdateHostInternal(ProviderId providerId, HostId hostId,
                                        HostDescription hostDescription, Timestamp timestamp) {
        StoredHost host = hosts.get(hostId);
        if (host == null) {
            return createHost(providerId, hostId, hostDescription, timestamp);
        }
        return updateHost(providerId, host, hostDescription, timestamp);
    }

    // creates a new host and sends HOST_ADDED
    private HostEvent createHost(ProviderId providerId, HostId hostId,
                                 HostDescription descr, Timestamp timestamp) {
        synchronized (this) {
            // If this host was previously removed, first ensure
            // this new request is "newer"
            if (removedHosts.containsKey(hostId)) {
                if (removedHosts.get(hostId).isNewer(timestamp)) {
                    return null;
                } else {
                    removedHosts.remove(hostId);
                }
            }
            StoredHost newhost = new StoredHost(providerId, hostId,
                    descr.hwAddress(),
                    descr.vlan(),
                    new Timestamped<>(descr.location(), timestamp),
                    ImmutableSet.copyOf(descr.ipAddress()));
            hosts.put(hostId, newhost);
            locations.put(descr.location(), newhost);
            return new HostEvent(HOST_ADDED, newhost);
        }
    }

    // checks for type of update to host, sends appropriate event
    private HostEvent updateHost(ProviderId providerId, StoredHost host,
                                 HostDescription descr, Timestamp timestamp) {
        HostEvent event;
        if (!host.location.isNewer(timestamp) && !host.location().equals(descr.location())) {
            host.setLocation(new Timestamped<>(descr.location(), timestamp));
            return new HostEvent(HOST_MOVED, host);
        }

        if (host.ipAddresses().containsAll(descr.ipAddress())) {
            return null;
        }

        Set<IpPrefix> addresses = new HashSet<>(host.ipAddresses());
        addresses.addAll(descr.ipAddress());
        StoredHost updated = new StoredHost(providerId, host.id(),
                                            host.mac(), host.vlan(),
                                            host.location, addresses);
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
        Timestamp timestamp = hostClockService.getTimestamp(hostId);
        HostEvent event = removeHostInternal(hostId, timestamp);
        if (event != null) {
            log.info("Notifying peers of a host removed topology event for hostId: {}", hostId);
            try {
                notifyPeers(new InternalHostRemovedEvent(hostId, timestamp));
            } catch (IOException e) {
                log.info("Failed to notify peers of a host removed topology event for hostId: {}", hostId);
            }
        }
        return event;
    }

    private HostEvent removeHostInternal(HostId hostId, Timestamp timestamp) {
        synchronized (this) {
            Host host = hosts.remove(hostId);
            if (host != null) {
                locations.remove((host.location()), host);
                removedHosts.put(hostId, new Timestamped<>(host, timestamp));
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
        private Timestamped<HostLocation> location;

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
                          MacAddress mac, VlanId vlan, Timestamped<HostLocation> location,
                          Set<IpPrefix> ips, Annotations... annotations) {
            super(providerId, id, mac, vlan, location.value(), ips, annotations);
            this.location = location;
        }

        void setLocation(Timestamped<HostLocation> location) {
            this.location = location;
        }

        @Override
        public HostLocation location() {
            return location.value();
        }

        public Timestamp timestamp() {
            return location.timestamp();
        }
    }

    private void notifyPeers(InternalHostRemovedEvent event) throws IOException {
        broadcastMessage(GossipHostStoreMessageSubjects.HOST_REMOVED, event);
    }

    private void notifyPeers(InternalHostEvent event) throws IOException {
        broadcastMessage(GossipHostStoreMessageSubjects.HOST_UPDATED, event);
    }

    private void broadcastMessage(MessageSubject subject, Object event) throws IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                SERIALIZER.encode(event));
        clusterCommunicator.broadcast(message);
    }

    private void unicastMessage(NodeId peer,
                                MessageSubject subject,
                                Object event) throws IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                SERIALIZER.encode(event));
        clusterCommunicator.unicast(message, peer);
    }

    private void notifyDelegateIfNotNull(HostEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    private class InternalHostEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.info("Received host update event from peer: {}", message.sender());
            InternalHostEvent event = (InternalHostEvent) SERIALIZER.decode(message.payload());

            ProviderId providerId = event.providerId();
            HostId hostId = event.hostId();
            HostDescription hostDescription = event.hostDescription();
            Timestamp timestamp = event.timestamp();

            notifyDelegateIfNotNull(createOrUpdateHostInternal(providerId, hostId, hostDescription, timestamp));
        }
    }

    private class InternalHostRemovedEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.info("Received host removed event from peer: {}", message.sender());
            InternalHostRemovedEvent event = (InternalHostRemovedEvent) SERIALIZER.decode(message.payload());

            HostId hostId = event.hostId();
            Timestamp timestamp = event.timestamp();

            notifyDelegateIfNotNull(removeHostInternal(hostId, timestamp));
        }
    }

    private final class SendAdvertisementTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }

            try {
                final NodeId self = clusterService.getLocalNode().id();
                Set<ControllerNode> nodes = clusterService.getNodes();

                ImmutableList<NodeId> nodeIds = FluentIterable.from(nodes)
                        .transform(toNodeId())
                        .toList();

                if (nodeIds.size() == 1 && nodeIds.get(0).equals(self)) {
                    log.debug("No other peers in the cluster.");
                    return;
                }

                NodeId peer;
                do {
                    int idx = RandomUtils.nextInt(0, nodeIds.size());
                    peer = nodeIds.get(idx);
                } while (peer.equals(self));

                HostAntiEntropyAdvertisement ad = createAdvertisement();

                if (Thread.currentThread().isInterrupted()) {
                    log.info("Interrupted, quitting");
                    return;
                }

                try {
                    unicastMessage(peer, GossipHostStoreMessageSubjects.HOST_ANTI_ENTROPY_ADVERTISEMENT, ad);
                } catch (IOException e) {
                    log.debug("Failed to send anti-entropy advertisement to {}", peer);
                    return;
                }
            } catch (Exception e) {
                // catch all Exception to avoid Scheduled task being suppressed.
                log.error("Exception thrown while sending advertisement", e);
            }
        }
    }

    private HostAntiEntropyAdvertisement createAdvertisement() {
        final NodeId self = clusterService.getLocalNode().id();

        Map<HostFragmentId, Timestamp> timestamps = new HashMap<>(hosts.size());
        Map<HostId, Timestamp> tombstones = new HashMap<>(removedHosts.size());

        for (Entry<HostId, StoredHost> e : hosts.entrySet()) {

            final HostId hostId = e.getKey();
            final StoredHost hostInfo = e.getValue();
            final ProviderId providerId = hostInfo.providerId();
            timestamps.put(new HostFragmentId(hostId, providerId), hostInfo.timestamp());
        }

        for (Entry<HostId, Timestamped<Host>> e : removedHosts.entrySet()) {
            tombstones.put(e.getKey(), e.getValue().timestamp());
        }

        return new HostAntiEntropyAdvertisement(self, timestamps, tombstones);
    }

    private synchronized void handleAntiEntropyAdvertisement(HostAntiEntropyAdvertisement ad) {

        final NodeId sender = ad.sender();

        for (Entry<HostId, StoredHost> host : hosts.entrySet()) {
            // for each locally live Hosts...
            final HostId hostId = host.getKey();
            final StoredHost localHost = host.getValue();
            final ProviderId providerId = localHost.providerId();
            final HostFragmentId hostFragId = new HostFragmentId(hostId, providerId);
            final Timestamp localLiveTimestamp = localHost.timestamp();

            Timestamp remoteTimestamp = ad.timestamps().get(hostFragId);
            if (remoteTimestamp == null) {
                remoteTimestamp = ad.tombstones().get(hostId);
            }
            if (remoteTimestamp == null ||
                localLiveTimestamp.compareTo(remoteTimestamp) > 0) {

                // local is more recent, push
                // TODO: annotation is lost
                final HostDescription desc = new DefaultHostDescription(
                            localHost.mac(),
                            localHost.vlan(),
                            localHost.location(),
                            localHost.ipAddresses());
                try {
                    unicastMessage(sender, GossipHostStoreMessageSubjects.HOST_UPDATED,
                            new InternalHostEvent(providerId, hostId, desc, localHost.timestamp()));
                } catch (IOException e1) {
                    log.debug("Failed to send advertisement response", e1);
                }
            }

            final Timestamp remoteDeadTimestamp = ad.tombstones().get(hostId);
            if (remoteDeadTimestamp != null &&
                remoteDeadTimestamp.compareTo(localLiveTimestamp) > 0) {
                // sender has recent remove
                notifyDelegateIfNotNull(removeHostInternal(hostId, remoteDeadTimestamp));
            }
        }

        for (Entry<HostId, Timestamped<Host>> dead : removedHosts.entrySet()) {
            // for each locally dead Hosts
            final HostId hostId = dead.getKey();
            final Timestamp localDeadTimestamp = dead.getValue().timestamp();

            // TODO: pick proper ProviderId, when supporting multi-provider
            final ProviderId providerId = dead.getValue().value().providerId();
            final HostFragmentId hostFragId = new HostFragmentId(hostId, providerId);

            final Timestamp remoteLiveTimestamp = ad.timestamps().get(hostFragId);
            if (remoteLiveTimestamp != null &&
                localDeadTimestamp.compareTo(remoteLiveTimestamp) > 0) {
                // sender has zombie, push
                try {
                    unicastMessage(sender, GossipHostStoreMessageSubjects.HOST_REMOVED,
                            new InternalHostRemovedEvent(hostId, localDeadTimestamp));
                } catch (IOException e1) {
                    log.debug("Failed to send advertisement response", e1);
                }
            }
        }


        for (Entry<HostId, Timestamp> e : ad.tombstones().entrySet()) {
            // for each remote tombstone advertisement...
            final HostId hostId = e.getKey();
            final Timestamp adRemoveTimestamp = e.getValue();

            final StoredHost storedHost = hosts.get(hostId);
            if (storedHost == null) {
                continue;
            }
            if (adRemoveTimestamp.compareTo(storedHost.timestamp()) > 0) {
                // sender has recent remove info, locally remove
                notifyDelegateIfNotNull(removeHostInternal(hostId, adRemoveTimestamp));
            }
        }
    }

    private final class InternalHostAntiEntropyAdvertisementListener implements
            ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received Host Anti-Entropy advertisement from peer: {}", message.sender());
            HostAntiEntropyAdvertisement advertisement = SERIALIZER.decode(message.payload());
            handleAntiEntropyAdvertisement(advertisement);
        }
    }
}
