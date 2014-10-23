package org.onlab.onos.store.trivial.impl;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.AnnotationsUtil;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.Link.Type;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkStore;
import org.onlab.onos.net.link.LinkStoreDelegate;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
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
import static org.onlab.onos.net.LinkKey.linkKey;
import static org.onlab.onos.net.link.LinkEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Verify.verifyNotNull;

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
            newDesc = new DefaultLinkDescription(
                        linkDescription.src(),
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

    private static <K, V> SetMultimap<K, V> createSynchronizedHashMultiMap() {
        return synchronizedSetMultimap(HashMultimap.<K, V>create());
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

        return new DefaultLink(primary , src, dst, type, annotations);
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
