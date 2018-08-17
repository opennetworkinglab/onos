/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedExecutors;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
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
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.onosproject.net.DefaultAnnotations.union;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_UPDATED;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of links using a {@code EventuallyConsistentMap}.
 */
@Component(immediate = true, service = LinkStore.class)
public class ECLinkStore
        extends AbstractStore<LinkEvent, LinkStoreDelegate>
        implements LinkStore {

    /**
     * Modes for dealing with newly discovered links.
     */
    protected enum LinkDiscoveryMode {
        /**
         * Permissive mode - all newly discovered links are valid.
         */
        PERMISSIVE,

        /**
         * Strict mode - all newly discovered links must be defined in
         * the network config.
         */
        STRICT
    }

    private final Logger log = getLogger(getClass());

    private final Map<LinkKey, Link> links = Maps.newConcurrentMap();
    private final Map<LinkKey, Set<ProviderId>> linkProviders = Maps.newConcurrentMap();
    private EventuallyConsistentMap<Provided<LinkKey>, LinkDescription> linkDescriptions;


    private ApplicationId appId;

    private static final MessageSubject LINK_INJECT_MESSAGE = new MessageSubject("inject-link-request");

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private EventuallyConsistentMapListener<Provided<LinkKey>, LinkDescription> linkTracker =
            new InternalLinkTracker();

    // Listener for config changes
    private final InternalConfigListener cfgListener = new InternalConfigListener();

    protected LinkDiscoveryMode linkDiscoveryMode = LinkDiscoveryMode.STRICT;

    protected static final Serializer SERIALIZER = Serializer.using(
            KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(Provided.class)
                    .build("ECLink"));

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.core");
        netCfgService.registerConfigFactory(factory);
        netCfgService.addListener(cfgListener);

        cfgListener.reconfigure(netCfgService.getConfig(appId, CoreConfig.class));

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
        linkProviders.clear();
        links.clear();
        clusterCommunicator.removeSubscriber(LINK_INJECT_MESSAGE);
        netCfgService.removeListener(cfgListener);
        netCfgService.unregisterConfigFactory(factory);

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
            linkDescriptions.compute(internalLinkKey, (k, v) -> createOrUpdateLinkInternal(v, linkDescription));
            return refreshLinkCache(linkKey);
        } else {
            // Only forward for ConfigProvider or NullProvider
            // Forwarding was added as a workaround for ONOS-490
            if (!"cfg".equals(providerId.scheme()) && !"null".equals(providerId.scheme())) {
                return null;
            }
            // Temporary hack for NPE (ONOS-1171).
            // Proper fix is to implement forwarding to master on ConfigProvider
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
            Type type;
            if (current.type() == DIRECT && updated.type() == Type.INDIRECT) {
                // mask transition from DIRECT -> INDIRECT, likely to be triggered by BDDP
                type = Type.DIRECT;
            } else {
                type = updated.type();
            }
            return new DefaultLinkDescription(
                    current.src(),
                    current.dst(),
                    type,
                    current.isExpected(),
                    union(current.annotations(), updated.annotations()));
        }
        return updated;
    }

    private Set<ProviderId> createOrUpdateLinkProviders(Set<ProviderId> current, ProviderId providerId) {
        if (current == null) {
            current = Sets.newConcurrentHashSet();
        }
        current.add(providerId);
        return current;
    }

    private LinkEvent refreshLinkCache(LinkKey linkKey) {
        AtomicReference<LinkEvent.Type> eventType = new AtomicReference<>();
        Link link = links.compute(linkKey, (key, existingLink) -> {
            Link newLink = composeLink(linkKey);
            if (newLink == null) {
                return null;
            }
            if (existingLink == null) {
                eventType.set(LINK_ADDED);
                return newLink;
            } else if (existingLink.state() != newLink.state() ||
                    existingLink.isExpected() != newLink.isExpected() ||
                    (existingLink.type() !=  newLink.type()) ||
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
        return linkProviders.getOrDefault(linkKey, Sets.newConcurrentHashSet());
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

        ProviderId baseProviderId = getBaseProviderId(linkKey);
        if (baseProviderId == null) {
            // provider was not found, this means it was already removed by the
            // parent component.
            return null;
        }
        LinkDescription base = linkDescriptions.get(new Provided<>(linkKey, baseProviderId));
        // short circuit if link description no longer exists
        if (base == null) {
            return null;
        }
        ConnectPoint src = base.src();
        ConnectPoint dst = base.dst();
        Type type = base.type();
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.putAll(base.annotations());

        getAllProviders(linkKey).stream()
                .map(p -> new Provided<>(linkKey, p))
                .forEach(key -> {
                    LinkDescription linkDescription = linkDescriptions.get(key);
                    if (linkDescription != null) {
                        builder.putAll(linkDescription.annotations());
                    }
                });

        DefaultAnnotations annotations = builder.build();
        Link.State initialLinkState;

        boolean isExpected;
        if (linkDiscoveryMode == LinkDiscoveryMode.PERMISSIVE) {
            initialLinkState = ACTIVE;
            isExpected =
                    Objects.equals(annotations.value(AnnotationKeys.DURABLE), "true");
        } else {
            initialLinkState = base.isExpected() ? ACTIVE : INACTIVE;
            isExpected = base.isExpected();
        }


        return DefaultLink.builder()
                .providerId(baseProviderId)
                .src(src)
                .dst(dst)
                .type(type)
                .state(initialLinkState)
                .isExpected(isExpected)
                .annotations(annotations)
                .build();
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

        if (linkDiscoveryMode == LinkDiscoveryMode.PERMISSIVE && link.isExpected()) {
            // FIXME: this will not sync link state!!!
            return link.state() == INACTIVE ? null :
                    updateLink(linkKey(link.src(), link.dst()), link,
                               DefaultLink.builder()
                                       .providerId(link.providerId())
                                       .src(link.src())
                                       .dst(link.dst())
                                       .type(link.type())
                                       .state(INACTIVE)
                                       .isExpected(link.isExpected())
                                       .annotations(link.annotations())
                                       .build());
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
            linkProviders.remove(linkKey);
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
                linkProviders.compute(event.key().key(), (k, v) ->
                        createOrUpdateLinkProviders(v, event.key().providerId()));
                notifyDelegate(refreshLinkCache(event.key().key()));
            } else if (event.type() == REMOVE) {
                notifyDelegate(purgeLinkCache(event.key().key()));
                linkProviders.remove(event.key().key());
            }
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        void reconfigure(CoreConfig coreConfig) {
            if (coreConfig == null) {
                linkDiscoveryMode = LinkDiscoveryMode.PERMISSIVE;
            } else {
                linkDiscoveryMode = coreConfig.linkDiscoveryMode();
            }
            if (linkDiscoveryMode == LinkDiscoveryMode.STRICT) {
                // Remove any previous links to force them to go through the strict
                // discovery process
                if (linkDescriptions != null) {
                    linkDescriptions.clear();
                }
                if (links != null) {
                    links.clear();
                }
            }
            log.debug("config set link discovery mode to {}",
                      linkDiscoveryMode.name());
        }

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(CoreConfig.class)) {

                CoreConfig cfg = netCfgService.getConfig(appId, CoreConfig.class);
                reconfigure(cfg);
                log.info("Reconfigured");
            }
        }
    }

    // Configuration properties factory
    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, CoreConfig>(APP_SUBJECT_FACTORY,
                                                         CoreConfig.class,
                                                         "core") {
                @Override
                public CoreConfig createConfig() {
                    return new CoreConfig();
                }
            };
}
