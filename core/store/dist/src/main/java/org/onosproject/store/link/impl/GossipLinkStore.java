/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.link.impl;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.AnnotationsUtil;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.LinkKey;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.minPriority;
import static org.onosproject.cluster.ControllerNodeToNodeId.toNodeId;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.DefaultAnnotations.union;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.link.LinkEvent.Type.*;
import static org.onosproject.store.link.impl.GossipLinkStoreMessageSubjects.LINK_ANTI_ENTROPY_ADVERTISEMENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure links in distributed data store
 * that uses optimistic replication and gossip based techniques.
 */
@Component(immediate = true, enabled = false)
@Service
public class GossipLinkStore
        extends AbstractStore<LinkEvent, LinkStoreDelegate>
        implements LinkStore {

    // Timeout in milliseconds to process links on remote master node
    private static final int REMOTE_MASTER_TIMEOUT = 1000;

    private final Logger log = getLogger(getClass());

    // Link inventory
    private final ConcurrentMap<LinkKey, Map<ProviderId, Timestamped<LinkDescription>>> linkDescs =
        new ConcurrentHashMap<>();

    // Link instance cache
    private final ConcurrentMap<LinkKey, Link> links = new ConcurrentHashMap<>();

    // Egress and ingress link sets
    private final SetMultimap<DeviceId, LinkKey> srcLinks = createSynchronizedHashMultiMap();
    private final SetMultimap<DeviceId, LinkKey> dstLinks = createSynchronizedHashMultiMap();

    // Remove links
    private final Map<LinkKey, Timestamp> removedLinks = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(InternalLinkEvent.class)
                    .register(InternalLinkRemovedEvent.class)
                    .register(LinkAntiEntropyAdvertisement.class)
                    .register(LinkFragmentId.class)
                    .register(LinkInjectedEvent.class)
                    .build();
        }
    };

    private ExecutorService executor;

    private ScheduledExecutorService backgroundExecutors;

    @Activate
    public void activate() {

        executor = Executors.newCachedThreadPool(groupedThreads("onos/link", "fg-%d"));

        backgroundExecutors =
                newSingleThreadScheduledExecutor(minPriority(groupedThreads("onos/link", "bg-%d")));

        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_UPDATE,
                new InternalLinkEventListener(), executor);
        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_REMOVED,
                new InternalLinkRemovedEventListener(), executor);
        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_ANTI_ENTROPY_ADVERTISEMENT,
                new InternalLinkAntiEntropyAdvertisementListener(), backgroundExecutors);
        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_INJECTED,
                new LinkInjectedEventListener(), executor);

        long initialDelaySec = 5;
        long periodSec = 5;
        // start anti-entropy thread
        backgroundExecutors.scheduleAtFixedRate(new SendAdvertisementTask(),
                    initialDelaySec, periodSec, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        executor.shutdownNow();

        backgroundExecutors.shutdownNow();
        try {
            if (!backgroundExecutors.awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Timeout during executor shutdown");
            }
        } catch (InterruptedException e) {
            log.error("Error during executor shutdown", e);
        }

        linkDescs.clear();
        links.clear();
        srcLinks.clear();
        dstLinks.clear();
        log.info("Stopped");
    }

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        // lock for iteration
        synchronized (srcLinks) {
            return FluentIterable.from(srcLinks.get(deviceId))
            .transform(lookupLink())
            .filter(notNull())
            .toSet();
        }
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        // lock for iteration
        synchronized (dstLinks) {
            return FluentIterable.from(dstLinks.get(deviceId))
            .transform(lookupLink())
            .filter(notNull())
            .toSet();
        }
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        return links.get(linkKey(src, dst));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egress = new HashSet<>();
        //
        // Change `srcLinks` to ConcurrentMap<DeviceId, (Concurrent)Set>
        // to remove this synchronized block, if we hit performance issue.
        // SetMultiMap#get returns wrapped collection to provide modifiable-view.
        // And the wrapped collection is not concurrent access safe.
        //
        // Our use case here does not require returned collection to be modifiable,
        // so the wrapped collection forces us to lock the whole multiset,
        // for benefit we don't need.
        //
        // Same applies to `dstLinks`
        synchronized (srcLinks) {
            for (LinkKey linkKey : srcLinks.get(src.deviceId())) {
                if (linkKey.src().equals(src)) {
                    Link link = links.get(linkKey);
                    if (link != null) {
                        egress.add(link);
                    } else {
                        log.debug("Egress link for {} was null, skipped", linkKey);
                    }
                }
            }
        }
        return egress;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint dst) {
        Set<Link> ingress = new HashSet<>();
        synchronized (dstLinks) {
            for (LinkKey linkKey : dstLinks.get(dst.deviceId())) {
                if (linkKey.dst().equals(dst)) {
                    Link link = links.get(linkKey);
                    if (link != null) {
                        ingress.add(link);
                    } else {
                        log.debug("Ingress link for {} was null, skipped", linkKey);
                    }
                }
            }
        }
        return ingress;
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {

        final DeviceId dstDeviceId = linkDescription.dst().deviceId();
        final NodeId localNode = clusterService.getLocalNode().id();
        final NodeId dstNode = mastershipService.getMasterFor(dstDeviceId);

        // Process link update only if we're the master of the destination node,
        // otherwise signal the actual master.
        LinkEvent linkEvent = null;
        if (localNode.equals(dstNode)) {

            Timestamp newTimestamp = deviceClockService.getTimestamp(dstDeviceId);

            final Timestamped<LinkDescription> deltaDesc = new Timestamped<>(linkDescription, newTimestamp);

            LinkKey key = linkKey(linkDescription.src(), linkDescription.dst());
            final Timestamped<LinkDescription> mergedDesc;
            Map<ProviderId, Timestamped<LinkDescription>> map = getOrCreateLinkDescriptions(key);

            synchronized (map) {
                linkEvent = createOrUpdateLinkInternal(providerId, deltaDesc);
                mergedDesc = map.get(providerId);
            }

            if (linkEvent != null) {
                log.debug("Notifying peers of a link update topology event from providerId: "
                                + "{}  between src: {} and dst: {}",
                        providerId, linkDescription.src(), linkDescription.dst());
                notifyPeers(new InternalLinkEvent(providerId, mergedDesc));
            }

        } else {
            // FIXME Temporary hack for NPE (ONOS-1171).
            // Proper fix is to implement forwarding to master on ConfigProvider
            // redo ONOS-490
            if (dstNode == null) {
                // silently ignore
                return null;
            }


            LinkInjectedEvent linkInjectedEvent = new LinkInjectedEvent(providerId, linkDescription);

            // TODO check unicast return value
            clusterCommunicator.unicast(linkInjectedEvent,
                    GossipLinkStoreMessageSubjects.LINK_INJECTED,
                    SERIALIZER::encode,
                    dstNode);
        }

        return linkEvent;
    }

    @Override
    public LinkEvent removeOrDownLink(ConnectPoint src, ConnectPoint dst) {
        Link link = getLink(src, dst);
        if (link == null) {
            return null;
        }

        if (link.isDurable()) {
            // FIXME: this is not the right thing to call for the gossip store; will not sync link state!!!
            return link.state() == INACTIVE ? null :
                    updateLink(linkKey(link.src(), link.dst()), link,
                               new DefaultLink(link.providerId(),
                                               link.src(), link.dst(),
                                               link.type(), INACTIVE,
                                               link.isDurable(),
                                               link.annotations()));
        }
        return removeLink(src, dst);
    }

    private LinkEvent createOrUpdateLinkInternal(
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {

        final LinkKey key = linkKey(linkDescription.value().src(),
                linkDescription.value().dst());
        Map<ProviderId, Timestamped<LinkDescription>> descs = getOrCreateLinkDescriptions(key);

        synchronized (descs) {
            // if the link was previously removed, we should proceed if and
            // only if this request is more recent.
            Timestamp linkRemovedTimestamp = removedLinks.get(key);
            if (linkRemovedTimestamp != null) {
                if (linkDescription.isNewerThan(linkRemovedTimestamp)) {
                    removedLinks.remove(key);
                } else {
                    log.trace("Link {} was already removed ignoring.", key);
                    return null;
                }
            }

            final Link oldLink = links.get(key);
            // update description
            createOrUpdateLinkDescription(descs, providerId, linkDescription);
            final Link newLink = composeLink(descs);
            if (oldLink == null) {
                return createLink(key, newLink);
            }
            return updateLink(key, oldLink, newLink);
        }
    }

    // Guarded by linkDescs value (=locking each Link)
    private Timestamped<LinkDescription> createOrUpdateLinkDescription(
            Map<ProviderId, Timestamped<LinkDescription>> descs,
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {

        // merge existing annotations
        Timestamped<LinkDescription> existingLinkDescription = descs.get(providerId);
        if (existingLinkDescription != null && existingLinkDescription.isNewer(linkDescription)) {
            log.trace("local info is more up-to-date, ignoring {}.", linkDescription);
            return null;
        }
        Timestamped<LinkDescription> newLinkDescription = linkDescription;
        if (existingLinkDescription != null) {
            // we only allow transition from INDIRECT -> DIRECT
            final Type newType;
            if (existingLinkDescription.value().type() == DIRECT) {
                newType = DIRECT;
            } else {
                newType = linkDescription.value().type();
            }
            SparseAnnotations merged = union(existingLinkDescription.value().annotations(),
                    linkDescription.value().annotations());
            newLinkDescription = new Timestamped<>(
                    new DefaultLinkDescription(
                        linkDescription.value().src(),
                        linkDescription.value().dst(),
                        newType, merged),
                    linkDescription.timestamp());
        }
        return descs.put(providerId, newLinkDescription);
    }

    // Creates and stores the link and returns the appropriate event.
    // Guarded by linkDescs value (=locking each Link)
    private LinkEvent createLink(LinkKey key, Link newLink) {
        links.put(key, newLink);
        srcLinks.put(newLink.src().deviceId(), key);
        dstLinks.put(newLink.dst().deviceId(), key);
        return new LinkEvent(LINK_ADDED, newLink);
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    // Guarded by linkDescs value (=locking each Link)
    private LinkEvent updateLink(LinkKey key, Link oldLink, Link newLink) {
        // Note: INDIRECT -> DIRECT transition only
        // so that BDDP discovered Link will not overwrite LDDP Link
        if (oldLink.state() != newLink.state() ||
            (oldLink.type() == INDIRECT && newLink.type() == DIRECT) ||
            !AnnotationsUtil.isEqual(oldLink.annotations(), newLink.annotations())) {

            links.put(key, newLink);
            // strictly speaking following can be omitted
            srcLinks.put(oldLink.src().deviceId(), key);
            dstLinks.put(oldLink.dst().deviceId(), key);
            return new LinkEvent(LINK_UPDATED, newLink);
        }
        return null;
    }

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        final LinkKey key = linkKey(src, dst);

        DeviceId dstDeviceId = dst.deviceId();
        Timestamp timestamp = null;
        try {
            timestamp = deviceClockService.getTimestamp(dstDeviceId);
        } catch (IllegalStateException e) {
            log.debug("Failed to remove link {}, was not the master", key);
            // there are times when this is called before mastership
            // handoff correctly completes.
            return null;
        }

        LinkEvent event = removeLinkInternal(key, timestamp);

        if (event != null) {
            log.debug("Notifying peers of a link removed topology event for a link "
                    + "between src: {} and dst: {}", src, dst);
            notifyPeers(new InternalLinkRemovedEvent(key, timestamp));
        }
        return event;
    }

    private static Timestamped<LinkDescription> getPrimaryDescription(
                Map<ProviderId, Timestamped<LinkDescription>> linkDescriptions) {

        synchronized (linkDescriptions) {
            for (Entry<ProviderId, Timestamped<LinkDescription>>
                    e : linkDescriptions.entrySet()) {

                if (!e.getKey().isAncillary()) {
                    return e.getValue();
                }
            }
        }
        return null;
    }


    // TODO: consider slicing out as Timestamp utils
    /**
     * Checks is timestamp is more recent than timestamped object.
     *
     * @param timestamp to check if this is more recent then other
     * @param timestamped object to be tested against
     * @return true if {@code timestamp} is more recent than {@code timestamped}
     *         or {@code timestamped is null}
     */
    private static boolean isMoreRecent(Timestamp timestamp, Timestamped<?> timestamped) {
        checkNotNull(timestamp);
        if (timestamped == null) {
            return true;
        }
        return timestamp.compareTo(timestamped.timestamp()) > 0;
    }

    private LinkEvent removeLinkInternal(LinkKey key, Timestamp timestamp) {
        Map<ProviderId, Timestamped<LinkDescription>> linkDescriptions
            = getOrCreateLinkDescriptions(key);

        synchronized (linkDescriptions) {
            if (linkDescriptions.isEmpty()) {
                // never seen such link before. keeping timestamp for record
                removedLinks.put(key, timestamp);
                return null;
            }
            // accept removal request if given timestamp is newer than
            // the latest Timestamp from Primary provider
            Timestamped<LinkDescription> prim = getPrimaryDescription(linkDescriptions);
            if (!isMoreRecent(timestamp, prim)) {
                // outdated remove request, ignore
                return null;
            }
            removedLinks.put(key, timestamp);
            Link link = links.remove(key);
            linkDescriptions.clear();
            if (link != null) {
                srcLinks.remove(link.src().deviceId(), key);
                dstLinks.remove(link.dst().deviceId(), key);
                return new LinkEvent(LINK_REMOVED, link);
            }
            return null;
        }
    }

    /**
     * Creates concurrent readable, synchronized HashMultimap.
     *
     * @return SetMultimap
     */
    private static <K, V> SetMultimap<K, V> createSynchronizedHashMultiMap() {
        return synchronizedSetMultimap(
               Multimaps.newSetMultimap(new ConcurrentHashMap<>(),
                                       () -> Sets.newConcurrentHashSet()));
    }

    /**
     * @return primary ProviderID, or randomly chosen one if none exists
     */
    private static ProviderId pickBaseProviderId(
            Map<ProviderId, Timestamped<LinkDescription>> linkDescriptions) {

        ProviderId fallBackPrimary = null;
        for (Entry<ProviderId, Timestamped<LinkDescription>> e : linkDescriptions.entrySet()) {
            if (!e.getKey().isAncillary()) {
                // found primary
                return e.getKey();
            } else if (fallBackPrimary == null) {
                // pick randomly as a fallback in case there is no primary
                fallBackPrimary = e.getKey();
            }
        }
        return fallBackPrimary;
    }

    // Guarded by linkDescs value (=locking each Link)
    private Link composeLink(Map<ProviderId, Timestamped<LinkDescription>> descs) {
        ProviderId baseProviderId = pickBaseProviderId(descs);
        Timestamped<LinkDescription> base = descs.get(baseProviderId);

        ConnectPoint src = base.value().src();
        ConnectPoint dst = base.value().dst();
        Type type = base.value().type();
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        annotations = merge(annotations, base.value().annotations());

        for (Entry<ProviderId, Timestamped<LinkDescription>> e : descs.entrySet()) {
            if (baseProviderId.equals(e.getKey())) {
                continue;
            }

            // Note: In the long run we should keep track of Description timestamp
            // and only merge conflicting keys when timestamp is newer
            // Currently assuming there will never be a key conflict between
            // providers

            // annotation merging. not so efficient, should revisit later
            annotations = merge(annotations, e.getValue().value().annotations());
        }

        boolean isDurable = Objects.equals(annotations.value(AnnotationKeys.DURABLE), "true");
        return new DefaultLink(baseProviderId, src, dst, type, ACTIVE, isDurable, annotations);
    }

    private Map<ProviderId, Timestamped<LinkDescription>> getOrCreateLinkDescriptions(LinkKey key) {
        Map<ProviderId, Timestamped<LinkDescription>> r;
        r = linkDescs.get(key);
        if (r != null) {
            return r;
        }
        r = new HashMap<>();
        final Map<ProviderId, Timestamped<LinkDescription>> concurrentlyAdded;
        concurrentlyAdded = linkDescs.putIfAbsent(key, r);
        if (concurrentlyAdded != null) {
            return concurrentlyAdded;
        } else {
            return r;
        }
    }

    private final Function<LinkKey, Link> lookupLink = new LookupLink();

    /**
     * Returns a Function to lookup Link instance using LinkKey from cache.
     *
     * @return lookup link function
     */
    private Function<LinkKey, Link> lookupLink() {
        return lookupLink;
    }

    private final class LookupLink implements Function<LinkKey, Link> {
        @Override
        public Link apply(LinkKey input) {
            if (input == null) {
                return null;
            } else {
                return links.get(input);
            }
        }
    }

    private void notifyDelegateIfNotNull(LinkEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    private void broadcastMessage(MessageSubject subject, Object event) {
        clusterCommunicator.broadcast(event, subject, SERIALIZER::encode);
    }

    private void unicastMessage(NodeId recipient, MessageSubject subject, Object event) throws IOException {
        clusterCommunicator.unicast(event, subject, SERIALIZER::encode, recipient);
    }

    private void notifyPeers(InternalLinkEvent event) {
        broadcastMessage(GossipLinkStoreMessageSubjects.LINK_UPDATE, event);
    }

    private void notifyPeers(InternalLinkRemovedEvent event) {
        broadcastMessage(GossipLinkStoreMessageSubjects.LINK_REMOVED, event);
    }

    // notify peer, silently ignoring error
    private void notifyPeer(NodeId peer, InternalLinkEvent event) {
        try {
            unicastMessage(peer, GossipLinkStoreMessageSubjects.LINK_UPDATE, event);
        } catch (IOException e) {
            log.debug("Failed to notify peer {} with message {}", peer, event);
        }
    }

    // notify peer, silently ignoring error
    private void notifyPeer(NodeId peer, InternalLinkRemovedEvent event) {
        try {
            unicastMessage(peer, GossipLinkStoreMessageSubjects.LINK_REMOVED, event);
        } catch (IOException e) {
            log.debug("Failed to notify peer {} with message {}", peer, event);
        }
    }

    private final class SendAdvertisementTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.debug("Interrupted, quitting");
                return;
            }

            try {
                final NodeId self = clusterService.getLocalNode().id();
                Set<ControllerNode> nodes = clusterService.getNodes();

                ImmutableList<NodeId> nodeIds = FluentIterable.from(nodes)
                        .transform(toNodeId())
                        .toList();

                if (nodeIds.size() == 1 && nodeIds.get(0).equals(self)) {
                    log.trace("No other peers in the cluster.");
                    return;
                }

                NodeId peer;
                do {
                    int idx = RandomUtils.nextInt(0, nodeIds.size());
                    peer = nodeIds.get(idx);
                } while (peer.equals(self));

                LinkAntiEntropyAdvertisement ad = createAdvertisement();

                if (Thread.currentThread().isInterrupted()) {
                    log.debug("Interrupted, quitting");
                    return;
                }

                try {
                    unicastMessage(peer, LINK_ANTI_ENTROPY_ADVERTISEMENT, ad);
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

    private LinkAntiEntropyAdvertisement createAdvertisement() {
        final NodeId self = clusterService.getLocalNode().id();

        Map<LinkFragmentId, Timestamp> linkTimestamps = new HashMap<>(linkDescs.size());
        Map<LinkKey, Timestamp> linkTombstones = new HashMap<>(removedLinks.size());

        linkDescs.forEach((linkKey, linkDesc) -> {
            synchronized (linkDesc) {
                for (Map.Entry<ProviderId, Timestamped<LinkDescription>> e : linkDesc.entrySet()) {
                    linkTimestamps.put(new LinkFragmentId(linkKey, e.getKey()), e.getValue().timestamp());
                }
            }
        });

        linkTombstones.putAll(removedLinks);

        return new LinkAntiEntropyAdvertisement(self, linkTimestamps, linkTombstones);
    }

    private void handleAntiEntropyAdvertisement(LinkAntiEntropyAdvertisement ad) {

        final NodeId sender = ad.sender();
        boolean localOutdated = false;

        for (Entry<LinkKey, Map<ProviderId, Timestamped<LinkDescription>>>
                l : linkDescs.entrySet()) {

            final LinkKey key = l.getKey();
            final Map<ProviderId, Timestamped<LinkDescription>> link = l.getValue();
            synchronized (link) {
                Timestamp localLatest = removedLinks.get(key);

                for (Entry<ProviderId, Timestamped<LinkDescription>> p : link.entrySet()) {
                    final ProviderId providerId = p.getKey();
                    final Timestamped<LinkDescription> pDesc = p.getValue();

                    final LinkFragmentId fragId = new LinkFragmentId(key, providerId);
                    // remote
                    Timestamp remoteTimestamp = ad.linkTimestamps().get(fragId);
                    if (remoteTimestamp == null) {
                        remoteTimestamp = ad.linkTombstones().get(key);
                    }
                    if (remoteTimestamp == null ||
                        pDesc.isNewerThan(remoteTimestamp)) {
                        // I have more recent link description. update peer.
                        notifyPeer(sender, new InternalLinkEvent(providerId, pDesc));
                    } else {
                        final Timestamp remoteLive = ad.linkTimestamps().get(fragId);
                        if (remoteLive != null &&
                            remoteLive.compareTo(pDesc.timestamp()) > 0) {
                            // I have something outdated
                            localOutdated = true;
                        }
                    }

                    // search local latest along the way
                    if (localLatest == null ||
                        pDesc.isNewerThan(localLatest)) {
                        localLatest = pDesc.timestamp();
                    }
                }
                // Tests if remote remove is more recent then local latest.
                final Timestamp remoteRemove = ad.linkTombstones().get(key);
                if (remoteRemove != null) {
                    if (localLatest != null &&
                        localLatest.compareTo(remoteRemove) < 0) {
                        // remote remove is more recent
                        notifyDelegateIfNotNull(removeLinkInternal(key, remoteRemove));
                    }
                }
            }
        }

        // populate remove info if not known locally
        for (Entry<LinkKey, Timestamp> remoteRm : ad.linkTombstones().entrySet()) {
            final LinkKey key = remoteRm.getKey();
            final Timestamp remoteRemove = remoteRm.getValue();
            // relying on removeLinkInternal to ignore stale info
            notifyDelegateIfNotNull(removeLinkInternal(key, remoteRemove));
        }

        if (localOutdated) {
            // send back advertisement to speed up convergence
            try {
                unicastMessage(sender, LINK_ANTI_ENTROPY_ADVERTISEMENT,
                                createAdvertisement());
            } catch (IOException e) {
                log.debug("Failed to send back active advertisement");
            }
        }
    }

    private final class InternalLinkEventListener
            implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.trace("Received link event from peer: {}", message.sender());
            InternalLinkEvent event = SERIALIZER.decode(message.payload());

            ProviderId providerId = event.providerId();
            Timestamped<LinkDescription> linkDescription = event.linkDescription();

            try {
                notifyDelegateIfNotNull(createOrUpdateLinkInternal(providerId, linkDescription));
            } catch (Exception e) {
                log.warn("Exception thrown handling link event", e);
            }
        }
    }

    private final class InternalLinkRemovedEventListener
            implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.trace("Received link removed event from peer: {}", message.sender());
            InternalLinkRemovedEvent event = SERIALIZER.decode(message.payload());

            LinkKey linkKey = event.linkKey();
            Timestamp timestamp = event.timestamp();

            try {
                notifyDelegateIfNotNull(removeLinkInternal(linkKey, timestamp));
            } catch (Exception e) {
                log.warn("Exception thrown handling link removed", e);
            }
        }
    }

    private final class InternalLinkAntiEntropyAdvertisementListener
            implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            log.trace("Received Link Anti-Entropy advertisement from peer: {}", message.sender());
            LinkAntiEntropyAdvertisement advertisement = SERIALIZER.decode(message.payload());
            try {
                handleAntiEntropyAdvertisement(advertisement);
            } catch (Exception e) {
                log.warn("Exception thrown while handling Link advertisements", e);
                throw e;
            }
        }
    }

    private final class LinkInjectedEventListener
            implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.trace("Received injected link event from peer: {}", message.sender());
            LinkInjectedEvent linkInjectedEvent = SERIALIZER.decode(message.payload());

            ProviderId providerId = linkInjectedEvent.providerId();
            LinkDescription linkDescription = linkInjectedEvent.linkDescription();

            final DeviceId deviceId = linkDescription.dst().deviceId();
            if (!deviceClockService.isTimestampAvailable(deviceId)) {
                // workaround for ONOS-1208
                log.warn("Not ready to accept update. Dropping {}", linkDescription);
                return;
            }

            try {
                createOrUpdateLink(providerId, linkDescription);
            } catch (Exception e) {
                log.warn("Exception thrown while handling link injected event", e);
            }
        }
    }
}
