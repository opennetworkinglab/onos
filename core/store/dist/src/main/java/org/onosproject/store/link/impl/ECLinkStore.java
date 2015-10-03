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
package org.onosproject.store.link.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.DefaultAnnotations.union;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_UPDATED;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedExecutors;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.AnnotationsUtil;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Link.Type;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;

/**
 * Manages the inventory of links using a {@code EventuallyConsistentMap}.
 */
@Component(immediate = true, enabled = true)
@Service
public class ECLinkStore
    extends AbstractStore<LinkEvent, LinkStoreDelegate>
    implements LinkStore {

    private final Logger log = getLogger(getClass());

    private final Map<LinkKey, Link> links = Maps.newConcurrentMap();
    private EventuallyConsistentMap<Provided<LinkKey>, LinkDescription> linkDescriptions;

    private static final MessageSubject LINK_INJECT_MESSAGE = new MessageSubject("inject-link-request");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private EventuallyConsistentMapListener<Provided<LinkKey>, LinkDescription> linkTracker =
            new InternalLinkTracker();

    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(Provided.class)
                    .build();
        }
    };

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MastershipBasedTimestamp.class)
                .register(Provided.class);

        linkDescriptions = storageService.<Provided<LinkKey>, LinkDescription>eventuallyConsistentMapBuilder()
                .withName("onos-link-descriptions")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> {
                    try {
                        return v == null ? null : deviceClockService.getTimestamp(v.dst().deviceId());
                    } catch (IllegalStateException e) {
                        return null;
                    }
                }).build();

        clusterCommunicator.addSubscriber(LINK_INJECT_MESSAGE,
                SERIALIZER::decode,
                this::injectLink,
                SERIALIZER::encode,
                SharedExecutors.getPoolThreadExecutor());

        linkDescriptions.addListener(linkTracker);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        linkDescriptions.removeListener(linkTracker);
        linkDescriptions.destroy();
        links.clear();
        clusterCommunicator.removeSubscriber(LINK_INJECT_MESSAGE);

        log.info("Stopped");
    }

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return links.values();
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return filter(links.values(), link -> deviceId.equals(link.src().deviceId()));
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        return filter(links.values(), link -> deviceId.equals(link.dst().deviceId()));
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        return links.get(linkKey(src, dst));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        return filter(links.values(), link -> src.equals(link.src()));
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint dst) {
        return filter(links.values(), link -> dst.equals(link.dst()));
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {
        final DeviceId dstDeviceId = linkDescription.dst().deviceId();
        final NodeId dstNodeId = mastershipService.getMasterFor(dstDeviceId);

        // Process link update only if we're the master of the destination node,
        // otherwise signal the actual master.
        if (clusterService.getLocalNode().id().equals(dstNodeId)) {
            LinkKey linkKey = linkKey(linkDescription.src(), linkDescription.dst());
            Provided<LinkKey> internalLinkKey = getProvided(linkKey, providerId);
            if (internalLinkKey == null) {
                return null;
            }
            linkDescriptions.compute(internalLinkKey, (k, v) -> createOrUpdateLinkInternal(v  , linkDescription));
            return refreshLinkCache(linkKey);
        } else {
            if (dstNodeId == null) {
                return null;
            }
            return Futures.getUnchecked(clusterCommunicator.sendAndReceive(new Provided<>(linkDescription, providerId),
                    LINK_INJECT_MESSAGE,
                    SERIALIZER::encode,
                    SERIALIZER::decode,
                    dstNodeId));
        }
    }

    private Provided<LinkKey> getProvided(LinkKey linkKey, ProviderId provId) {
        ProviderId bpid = getBaseProviderId(linkKey);
        if (provId == null) {
            // The LinkService didn't know who this LinkKey belongs to.
            // A fix is to either modify the getProvider() in LinkService classes
            // or expose the contents of linkDescriptions to the LinkService.
            return (bpid == null) ? null : new Provided<>(linkKey, bpid);
        } else {
            return new Provided<>(linkKey, provId);
        }
    }

    private LinkDescription createOrUpdateLinkInternal(LinkDescription current, LinkDescription updated) {
        if (current != null) {
            // we only allow transition from INDIRECT -> DIRECT
            return  new DefaultLinkDescription(
                        current.src(),
                        current.dst(),
                        current.type() == DIRECT ? DIRECT : updated.type(),
                        union(current.annotations(), updated.annotations()));
        }
        return updated;
    }

    private LinkEvent refreshLinkCache(LinkKey linkKey) {
        AtomicReference<LinkEvent.Type> eventType = new AtomicReference<>();
        Link link = links.compute(linkKey, (key, existingLink) -> {
            Link newLink = composeLink(linkKey);
            if (existingLink == null) {
                eventType.set(LINK_ADDED);
                return newLink;
            } else if (existingLink.state() != newLink.state() ||
                        (existingLink.type() == INDIRECT && newLink.type() == DIRECT) ||
                        !AnnotationsUtil.isEqual(existingLink.annotations(), newLink.annotations())) {
                    eventType.set(LINK_UPDATED);
                    return newLink;
            } else {
                return existingLink;
            }
        });
        return eventType.get() != null ? new LinkEvent(eventType.get(), link) : null;
    }

    private Set<ProviderId> getAllProviders(LinkKey linkKey) {
        return linkDescriptions.keySet()
                               .stream()
                               .filter(key -> key.key().equals(linkKey))
                               .map(key -> key.providerId())
                               .collect(Collectors.toSet());
    }

    private ProviderId getBaseProviderId(LinkKey linkKey) {
        Set<ProviderId> allProviders = getAllProviders(linkKey);
        if (allProviders.size() > 0) {
            return allProviders.stream()
                               .filter(p -> !p.isAncillary())
                               .findFirst()
                               .orElse(Iterables.getFirst(allProviders, null));
        }
        return null;
    }

    private Link composeLink(LinkKey linkKey) {

        ProviderId baseProviderId = checkNotNull(getBaseProviderId(linkKey));
        LinkDescription base = linkDescriptions.get(new Provided<>(linkKey, baseProviderId));

        ConnectPoint src = base.src();
        ConnectPoint dst = base.dst();
        Type type = base.type();
        AtomicReference<DefaultAnnotations> annotations = new AtomicReference<>(DefaultAnnotations.builder().build());
        annotations.set(merge(annotations.get(), base.annotations()));

        getAllProviders(linkKey).stream()
                                .map(p -> new Provided<>(linkKey, p))
                                .forEach(key -> {
                                    annotations.set(merge(annotations.get(),
                                                          linkDescriptions.get(key).annotations()));
        });

        boolean isDurable = Objects.equals(annotations.get().value(AnnotationKeys.DURABLE), "true");
        return new DefaultLink(baseProviderId, src, dst, type, ACTIVE, isDurable, annotations.get());
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
            return new LinkEvent(LINK_UPDATED, newLink);
        }
        return null;
    }

    @Override
    public LinkEvent removeOrDownLink(ConnectPoint src, ConnectPoint dst) {
        Link link = getLink(src, dst);
        if (link == null) {
            return null;
        }

        if (link.isDurable()) {
            // FIXME: this will not sync link state!!!
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

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        final LinkKey linkKey = LinkKey.linkKey(src, dst);
        ProviderId primaryProviderId = getBaseProviderId(linkKey);
        // Stop if there is no base provider.
        if (primaryProviderId == null) {
            return null;
        }
        LinkDescription removedLinkDescription =
                linkDescriptions.remove(new Provided<>(linkKey, primaryProviderId));
        if (removedLinkDescription != null) {
            return purgeLinkCache(linkKey);
        }
        return null;
    }

    private LinkEvent purgeLinkCache(LinkKey linkKey) {
        Link removedLink = links.remove(linkKey);
        if (removedLink != null) {
            getAllProviders(linkKey).forEach(p -> linkDescriptions.remove(new Provided<>(linkKey, p)));
            return new LinkEvent(LINK_REMOVED, removedLink);
        }
        return null;
    }

    private Set<Link> filter(Collection<Link> links, Predicate<Link> predicate) {
        return links.stream().filter(predicate).collect(Collectors.toSet());
    }

    private LinkEvent injectLink(Provided<LinkDescription> linkInjectRequest) {
        log.trace("Received request to inject link {}", linkInjectRequest);

        ProviderId providerId = linkInjectRequest.providerId();
        LinkDescription linkDescription = linkInjectRequest.key();

        final DeviceId deviceId = linkDescription.dst().deviceId();
        if (!deviceClockService.isTimestampAvailable(deviceId)) {
            // workaround for ONOS-1208
            log.warn("Not ready to accept update. Dropping {}", linkInjectRequest);
            return null;
        }
        return createOrUpdateLink(providerId, linkDescription);
    }

    private class InternalLinkTracker implements EventuallyConsistentMapListener<Provided<LinkKey>, LinkDescription> {
        @Override
        public void event(EventuallyConsistentMapEvent<Provided<LinkKey>, LinkDescription> event) {
            if (event.type() == PUT) {
                notifyDelegate(refreshLinkCache(event.key().key()));
            } else if (event.type() == REMOVE) {
                notifyDelegate(purgeLinkCache(event.key().key()));
            }
        }
    }
}