package org.onlab.onos.store.link.impl;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.AnnotationsUtil;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.Link.Type;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.Provided;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkStore;
import org.onlab.onos.net.link.LinkStoreDelegate;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.ClockService;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.common.impl.Timestamped;
import org.onlab.onos.store.serializers.DistributedStoreSerializers;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoPool;
import org.onlab.util.NewConcurrentHashMap;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.onlab.onos.net.DefaultAnnotations.union;
import static org.onlab.onos.net.DefaultAnnotations.merge;
import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.link.LinkEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static com.google.common.base.Predicates.notNull;

/**
 * Manages inventory of infrastructure links in distributed data store
 * that uses optimistic replication and gossip based techniques.
 */
@Component(immediate = true)
@Service
public class GossipLinkStore
        extends AbstractStore<LinkEvent, LinkStoreDelegate>
        implements LinkStore {

    private final Logger log = getLogger(getClass());

    // Link inventory
    private final ConcurrentMap<LinkKey, ConcurrentMap<ProviderId, Timestamped<LinkDescription>>> linkDescs =
        new ConcurrentHashMap<>();

    // Link instance cache
    private final ConcurrentMap<LinkKey, Link> links = new ConcurrentHashMap<>();

    // Egress and ingress link sets
    private final SetMultimap<DeviceId, LinkKey> srcLinks = createSynchronizedHashMultiMap();
    private final SetMultimap<DeviceId, LinkKey> dstLinks = createSynchronizedHashMultiMap();

    // Remove links
    private final Map<LinkKey, Timestamp> removedLinks = Maps.newHashMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoPool.newBuilder()
                    .register(DistributedStoreSerializers.COMMON)
                    .register(InternalLinkEvent.class)
                    .register(InternalLinkRemovedEvent.class)
                    .build()
                    .populate(1);
        }
    };

    @Activate
    public void activate() {

        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_UPDATE, new InternalLinkEventListener());
        clusterCommunicator.addSubscriber(
                GossipLinkStoreMessageSubjects.LINK_REMOVED, new InternalLinkRemovedEventListener());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
        return links.get(new LinkKey(src, dst));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egress = new HashSet<>();
        for (LinkKey linkKey : srcLinks.get(src.deviceId())) {
            if (linkKey.src().equals(src)) {
                egress.add(links.get(linkKey));
            }
        }
        return egress;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint dst) {
        Set<Link> ingress = new HashSet<>();
        for (LinkKey linkKey : dstLinks.get(dst.deviceId())) {
            if (linkKey.dst().equals(dst)) {
                ingress.add(links.get(linkKey));
            }
        }
        return ingress;
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {

        DeviceId dstDeviceId = linkDescription.dst().deviceId();
        Timestamp newTimestamp = clockService.getTimestamp(dstDeviceId);

        final Timestamped<LinkDescription> deltaDesc = new Timestamped<>(linkDescription, newTimestamp);

        LinkEvent event = createOrUpdateLinkInternal(providerId, deltaDesc);

        if (event != null) {
            log.info("Notifying peers of a link update topology event from providerId: "
                    + "{}  between src: {} and dst: {}",
                    providerId, linkDescription.src(), linkDescription.dst());
            try {
                notifyPeers(new InternalLinkEvent(providerId, deltaDesc));
            } catch (IOException e) {
                log.info("Failed to notify peers of a link update topology event from providerId: "
                        + "{}  between src: {} and dst: {}",
                        providerId, linkDescription.src(), linkDescription.dst());
            }
        }
        return event;
    }

    private LinkEvent createOrUpdateLinkInternal(
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {

        LinkKey key = new LinkKey(linkDescription.value().src(), linkDescription.value().dst());
        ConcurrentMap<ProviderId, Timestamped<LinkDescription>> descs = getLinkDescriptions(key);

        synchronized (descs) {
            // if the link was previously removed, we should proceed if and
            // only if this request is more recent.
            Timestamp linkRemovedTimestamp = removedLinks.get(key);
            if (linkRemovedTimestamp != null) {
                if (linkDescription.isNewer(linkRemovedTimestamp)) {
                    removedLinks.remove(key);
                } else {
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
            ConcurrentMap<ProviderId, Timestamped<LinkDescription>> existingLinkDescriptions,
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {

        // merge existing attributes and merge
        Timestamped<LinkDescription> existingLinkDescription = existingLinkDescriptions.get(providerId);
        if (existingLinkDescription != null && existingLinkDescription.isNewer(linkDescription)) {
            return null;
        }
        Timestamped<LinkDescription> newLinkDescription = linkDescription;
        if (existingLinkDescription != null) {
            SparseAnnotations merged = union(existingLinkDescription.value().annotations(),
                    linkDescription.value().annotations());
            newLinkDescription = new Timestamped<LinkDescription>(
                    new DefaultLinkDescription(
                        linkDescription.value().src(),
                        linkDescription.value().dst(),
                        linkDescription.value().type(), merged),
                    linkDescription.timestamp());
        }
        return existingLinkDescriptions.put(providerId, newLinkDescription);
    }

    // Creates and stores the link and returns the appropriate event.
    // Guarded by linkDescs value (=locking each Link)
    private LinkEvent createLink(LinkKey key, Link newLink) {

        if (newLink.providerId().isAncillary()) {
            // TODO: revisit ancillary only Link handling

            // currently treating ancillary only as down (not visible outside)
            return null;
        }

        links.put(key, newLink);
        srcLinks.put(newLink.src().deviceId(), key);
        dstLinks.put(newLink.dst().deviceId(), key);
        return new LinkEvent(LINK_ADDED, newLink);
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    // Guarded by linkDescs value (=locking each Link)
    private LinkEvent updateLink(LinkKey key, Link oldLink, Link newLink) {

        if (newLink.providerId().isAncillary()) {
            // TODO: revisit ancillary only Link handling

            // currently treating ancillary only as down (not visible outside)
            return null;
        }

        if ((oldLink.type() == INDIRECT && newLink.type() == DIRECT) ||
            !AnnotationsUtil.isEqual(oldLink.annotations(), newLink.annotations())) {

            links.put(key, newLink);
            // strictly speaking following can be ommitted
            srcLinks.put(oldLink.src().deviceId(), key);
            dstLinks.put(oldLink.dst().deviceId(), key);
            return new LinkEvent(LINK_UPDATED, newLink);
        }
        return null;
    }

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        final LinkKey key = new LinkKey(src, dst);

        DeviceId dstDeviceId = dst.deviceId();
        Timestamp timestamp = clockService.getTimestamp(dstDeviceId);

        LinkEvent event = removeLinkInternal(key, timestamp);

        if (event != null) {
            log.info("Notifying peers of a link removed topology event for a link "
                    + "between src: {} and dst: {}", src, dst);
            try {
                notifyPeers(new InternalLinkRemovedEvent(key, timestamp));
            } catch (IOException e) {
                log.error("Failed to notify peers of a link removed topology event for a link "
                        + "between src: {} and dst: {}", src, dst);
            }
        }
        return event;
    }

    private LinkEvent removeLinkInternal(LinkKey key, Timestamp timestamp) {
        ConcurrentMap<ProviderId, Timestamped<LinkDescription>> linkDescriptions =
                getLinkDescriptions(key);
        synchronized (linkDescriptions) {
            // accept removal request if given timestamp is newer than
            // the latest Timestamp from Primary provider
            ProviderId primaryProviderId = pickPrimaryProviderId(linkDescriptions);
            if (linkDescriptions.get(primaryProviderId).isNewer(timestamp)) {
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

    private static <K, V> SetMultimap<K, V> createSynchronizedHashMultiMap() {
        return synchronizedSetMultimap(HashMultimap.<K, V>create());
    }

    /**
     * @return primary ProviderID, or randomly chosen one if none exists
     */
    private ProviderId pickPrimaryProviderId(
            ConcurrentMap<ProviderId, Timestamped<LinkDescription>> providerDescs) {

        ProviderId fallBackPrimary = null;
        for (Entry<ProviderId, Timestamped<LinkDescription>> e : providerDescs.entrySet()) {
            if (!e.getKey().isAncillary()) {
                return e.getKey();
            } else if (fallBackPrimary == null) {
                // pick randomly as a fallback in case there is no primary
                fallBackPrimary = e.getKey();
            }
        }
        return fallBackPrimary;
    }

    private Link composeLink(ConcurrentMap<ProviderId, Timestamped<LinkDescription>> linkDescriptions) {
        ProviderId primaryProviderId = pickPrimaryProviderId(linkDescriptions);
        Timestamped<LinkDescription> base = linkDescriptions.get(primaryProviderId);

        ConnectPoint src = base.value().src();
        ConnectPoint dst = base.value().dst();
        Type type = base.value().type();
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        annotations = merge(annotations, base.value().annotations());

        for (Entry<ProviderId, Timestamped<LinkDescription>> e : linkDescriptions.entrySet()) {
            if (primaryProviderId.equals(e.getKey())) {
                continue;
            }

            // TODO: should keep track of Description timestamp
            // and only merge conflicting keys when timestamp is newer
            // Currently assuming there will never be a key conflict between
            // providers

            // annotation merging. not so efficient, should revisit later
            annotations = merge(annotations, e.getValue().value().annotations());
        }

        return new DefaultLink(primaryProviderId , src, dst, type, annotations);
    }

    private ConcurrentMap<ProviderId, Timestamped<LinkDescription>> getLinkDescriptions(LinkKey key) {
        return ConcurrentUtils.createIfAbsentUnchecked(linkDescs, key,
                NewConcurrentHashMap.<ProviderId, Timestamped<LinkDescription>>ifNeeded());
    }

    private final Function<LinkKey, Link> lookupLink = new LookupLink();
    private Function<LinkKey, Link> lookupLink() {
        return lookupLink;
    }

    private final class LookupLink implements Function<LinkKey, Link> {
        @Override
        public Link apply(LinkKey input) {
            return links.get(input);
        }
    }

    private static final Predicate<Provided> IS_PRIMARY = new IsPrimary();
    private static final Predicate<Provided> isPrimary() {
        return IS_PRIMARY;
    }

    private static final class IsPrimary implements Predicate<Provided> {

        @Override
        public boolean apply(Provided input) {
            return !input.providerId().isAncillary();
        }
    }

    private void notifyDelegateIfNotNull(LinkEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    // TODO: should we be throwing exception?
    private void broadcastMessage(MessageSubject subject, Object event) throws IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                SERIALIZER.encode(event));
        clusterCommunicator.broadcast(message);
    }

    private void notifyPeers(InternalLinkEvent event) throws IOException {
        broadcastMessage(GossipLinkStoreMessageSubjects.LINK_UPDATE, event);
    }

    private void notifyPeers(InternalLinkRemovedEvent event) throws IOException {
        broadcastMessage(GossipLinkStoreMessageSubjects.LINK_REMOVED, event);
    }

    private class InternalLinkEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.info("Received link event from peer: {}", message.sender());
            InternalLinkEvent event = (InternalLinkEvent) SERIALIZER.decode(message.payload());

            ProviderId providerId = event.providerId();
            Timestamped<LinkDescription> linkDescription = event.linkDescription();

            notifyDelegateIfNotNull(createOrUpdateLinkInternal(providerId, linkDescription));
        }
    }

    private class InternalLinkRemovedEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.info("Received link removed event from peer: {}", message.sender());
            InternalLinkRemovedEvent event = (InternalLinkRemovedEvent) SERIALIZER.decode(message.payload());

            LinkKey linkKey = event.linkKey();
            Timestamp timestamp = event.timestamp();

            notifyDelegateIfNotNull(removeLinkInternal(linkKey, timestamp));
        }
    }
}
