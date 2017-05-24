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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.DefaultAnnotations.union;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.link.LinkEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure links using trivial in-memory structures
 * implementation.
 */
@Component(immediate = true)
@Service
public class SimpleLinkStore
        extends AbstractStore<LinkEvent, LinkStoreDelegate>
        implements LinkStore {

    private final Logger log = getLogger(getClass());

    // Link inventory
    private final ConcurrentMap<LinkKey, Map<ProviderId, LinkDescription>>
            linkDescs = new ConcurrentHashMap<>();

    // Link instance cache
    private final ConcurrentMap<LinkKey, Link> links = new ConcurrentHashMap<>();

    // Egress and ingress link sets
    private final SetMultimap<DeviceId, LinkKey> srcLinks = createSynchronizedHashMultiMap();
    private final SetMultimap<DeviceId, LinkKey> dstLinks = createSynchronizedHashMultiMap();


    @Activate
    public void activate() {
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
        return links.get(linkKey(src, dst));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egress = new HashSet<>();
        synchronized (srcLinks) {
            for (LinkKey linkKey : srcLinks.get(src.deviceId())) {
                if (linkKey.src().equals(src)) {
                    egress.add(links.get(linkKey));
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
                    ingress.add(links.get(linkKey));
                }
            }
        }
        return ingress;
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {
        LinkKey key = linkKey(linkDescription.src(), linkDescription.dst());

        Map<ProviderId, LinkDescription> descs = getOrCreateLinkDescriptions(key);
        synchronized (descs) {
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

    @Override
    public LinkEvent removeOrDownLink(ConnectPoint src, ConnectPoint dst) {
        Link link = getLink(src, dst);
        if (link == null) {
            return null;
        }

        if (link.isExpected()) {
            return link.state() == INACTIVE ? null :
                    updateLink(linkKey(link.src(), link.dst()), link,
                               DefaultLink.builder()
                                       .providerId(link.providerId())
                                       .src(link.src())
                                       .dst(link.dst())
                                       .type(link.type())
                                       .state(INACTIVE)
                                       .isExpected(link.isExpected())
                                       .annotations(link.annotations()).build());
        }
        return removeLink(src, dst);
    }

    // Guarded by linkDescs value (=locking each Link)
    private LinkDescription createOrUpdateLinkDescription(
            Map<ProviderId, LinkDescription> descs,
            ProviderId providerId,
            LinkDescription linkDescription) {

        // merge existing attributes and merge
        LinkDescription oldDesc = descs.get(providerId);
        LinkDescription newDesc = linkDescription;
        if (oldDesc != null) {
            // we only allow transition from INDIRECT -> DIRECT
            final Type newType;
            if (oldDesc.type() == DIRECT) {
                newType = DIRECT;
            } else {
                newType = linkDescription.type();
            }
            SparseAnnotations merged = union(oldDesc.annotations(),
                                             linkDescription.annotations());
            newDesc = new DefaultLinkDescription(linkDescription.src(),
                                                 linkDescription.dst(),
                                                 newType, merged);
        }
        return descs.put(providerId, newDesc);
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
        if (oldLink.state() != newLink.state() ||
                (oldLink.type() == INDIRECT && newLink.type() == DIRECT) ||
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
        final LinkKey key = linkKey(src, dst);
        Map<ProviderId, LinkDescription> descs = getOrCreateLinkDescriptions(key);
        synchronized (descs) {
            Link link = links.remove(key);
            descs.clear();
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
    // Guarded by linkDescs value (=locking each Link)
    private ProviderId getBaseProviderId(Map<ProviderId, LinkDescription> providerDescs) {

        ProviderId fallBackPrimary = null;
        for (Entry<ProviderId, LinkDescription> e : providerDescs.entrySet()) {
            if (!e.getKey().isAncillary()) {
                return e.getKey();
            } else if (fallBackPrimary == null) {
                // pick randomly as a fallback in case there is no primary
                fallBackPrimary = e.getKey();
            }
        }
        return fallBackPrimary;
    }

    // Guarded by linkDescs value (=locking each Link)
    private Link composeLink(Map<ProviderId, LinkDescription> descs) {
        ProviderId primary = getBaseProviderId(descs);
        LinkDescription base = descs.get(verifyNotNull(primary));

        ConnectPoint src = base.src();
        ConnectPoint dst = base.dst();
        Type type = base.type();
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        annotations = merge(annotations, base.annotations());

        for (Entry<ProviderId, LinkDescription> e : descs.entrySet()) {
            if (primary.equals(e.getKey())) {
                continue;
            }

            // TODO: should keep track of Description timestamp
            // and only merge conflicting keys when timestamp is newer
            // Currently assuming there will never be a key conflict between
            // providers

            // annotation merging. not so efficient, should revisit later
            annotations = merge(annotations, e.getValue().annotations());
        }

        boolean isDurable = Objects.equals(annotations.value(AnnotationKeys.DURABLE), "true");
        return DefaultLink.builder()
                .providerId(primary)
                .src(src)
                .dst(dst)
                .type(type)
                .state(ACTIVE)
                .isExpected(isDurable)
                .annotations(annotations)
                .build();
    }

    private Map<ProviderId, LinkDescription> getOrCreateLinkDescriptions(LinkKey key) {
        Map<ProviderId, LinkDescription> r;
        r = linkDescs.get(key);
        if (r != null) {
            return r;
        }
        r = new HashMap<>();
        final Map<ProviderId, LinkDescription> concurrentlyAdded;
        concurrentlyAdded = linkDescs.putIfAbsent(key, r);
        if (concurrentlyAdded == null) {
            return r;
        } else {
            return concurrentlyAdded;
        }
    }

    private final Function<LinkKey, Link> lookupLink = new LookupLink();

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
}
