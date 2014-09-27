package org.onlab.onos.store.link.impl;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkStore;
import org.onlab.onos.net.link.LinkStoreDelegate;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.impl.AbsentInvalidatingLoadingCache;
import org.onlab.onos.store.impl.AbstractHazelcastStore;
import org.onlab.onos.store.impl.OptionalCacheLoader;
import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableSet.Builder;
import com.hazelcast.core.IMap;

/**
 * Manages inventory of infrastructure links using Hazelcast-backed map.
 */
@Component(immediate = true)
@Service
public class DistributedLinkStore
    extends AbstractHazelcastStore<LinkEvent, LinkStoreDelegate>
    implements LinkStore {

    private final Logger log = getLogger(getClass());

    // Link inventory
    private IMap<byte[], byte[]> rawLinks;
    private LoadingCache<LinkKey, Optional<DefaultLink>> links;

    // TODO synchronize?
    // Egress and ingress link sets
    private final Multimap<DeviceId, Link> srcLinks = HashMultimap.create();
    private final Multimap<DeviceId, Link> dstLinks = HashMultimap.create();

    @Override
    @Activate
    public void activate() {
        super.activate();

        boolean includeValue = true;

        // TODO decide on Map name scheme to avoid collision
        rawLinks = theInstance.getMap("links");
        final OptionalCacheLoader<LinkKey, DefaultLink> linkLoader
                = new OptionalCacheLoader<>(storeService, rawLinks);
        links = new AbsentInvalidatingLoadingCache<>(newBuilder().build(linkLoader));
        // refresh/populate cache based on notification from other instance
        rawLinks.addEntryListener(new RemoteLinkEventHandler(links), includeValue);

        loadLinkCache();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        super.activate();
        log.info("Stopped");
    }

    private void loadLinkCache() {
        for (byte[] keyBytes : rawLinks.keySet()) {
            final LinkKey id = deserialize(keyBytes);
            links.refresh(id);
        }
    }

    @Override
    public int getLinkCount() {
        return links.asMap().size();
    }

    @Override
    public Iterable<Link> getLinks() {
        Builder<Link> builder = ImmutableSet.builder();
        for (Optional<DefaultLink> e : links.asMap().values()) {
            if (e.isPresent()) {
                builder.add(e.get());
            }
        }
        return builder.build();
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return ImmutableSet.copyOf(srcLinks.get(deviceId));
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        return ImmutableSet.copyOf(dstLinks.get(deviceId));
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        return links.getUnchecked(new LinkKey(src, dst)).orNull();
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egress = new HashSet<>();
        for (Link link : srcLinks.get(src.deviceId())) {
            if (link.src().equals(src)) {
                egress.add(link);
            }
        }
        return egress;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint dst) {
        Set<Link> ingress = new HashSet<>();
        for (Link link : dstLinks.get(dst.deviceId())) {
            if (link.dst().equals(dst)) {
                ingress.add(link);
            }
        }
        return ingress;
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {
        LinkKey key = new LinkKey(linkDescription.src(), linkDescription.dst());
        Optional<DefaultLink> link = links.getUnchecked(key);
        if (!link.isPresent()) {
            return createLink(providerId, key, linkDescription);
        }
        return updateLink(providerId, link.get(), key, linkDescription);
    }

    // Creates and stores the link and returns the appropriate event.
    private LinkEvent createLink(ProviderId providerId, LinkKey key,
                                 LinkDescription linkDescription) {
        DefaultLink link = new DefaultLink(providerId, key.src(), key.dst(),
                                           linkDescription.type());
        synchronized (this) {
            final byte[] keyBytes = serialize(key);
            rawLinks.put(keyBytes, serialize(link));
            links.asMap().putIfAbsent(key, Optional.of(link));

            addNewLink(link);
        }
        return new LinkEvent(LINK_ADDED, link);
    }

    // update Egress and ingress link sets
    private void addNewLink(DefaultLink link) {
        synchronized (this) {
            srcLinks.put(link.src().deviceId(), link);
            dstLinks.put(link.dst().deviceId(), link);
        }
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    private LinkEvent updateLink(ProviderId providerId, DefaultLink link,
                                 LinkKey key, LinkDescription linkDescription) {
        // FIXME confirm Link update condition is OK
        if (link.type() == INDIRECT && linkDescription.type() == DIRECT) {
            synchronized (this) {

                DefaultLink updated =
                        new DefaultLink(providerId, link.src(), link.dst(),
                                        linkDescription.type());
                final byte[] keyBytes = serialize(key);
                rawLinks.put(keyBytes, serialize(updated));
                links.asMap().replace(key, Optional.of(link), Optional.of(updated));

                replaceLink(link, updated);
                return new LinkEvent(LINK_UPDATED, updated);
            }
        }
        return null;
    }

    // update Egress and ingress link sets
    private void replaceLink(DefaultLink link, DefaultLink updated) {
        synchronized (this) {
            srcLinks.remove(link.src().deviceId(), link);
            dstLinks.remove(link.dst().deviceId(), link);

            srcLinks.put(link.src().deviceId(), updated);
            dstLinks.put(link.dst().deviceId(), updated);
        }
    }

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        synchronized (this) {
            LinkKey key = new LinkKey(src, dst);
            byte[] keyBytes = serialize(key);
            Link link = deserialize(rawLinks.remove(keyBytes));
            links.invalidate(key);
            if (link != null) {
                removeLink(link);
                return new LinkEvent(LINK_REMOVED, link);
            }
            return null;
        }
    }

    // update Egress and ingress link sets
    private void removeLink(Link link) {
        synchronized (this) {
            srcLinks.remove(link.src().deviceId(), link);
            dstLinks.remove(link.dst().deviceId(), link);
        }
    }

    private class RemoteLinkEventHandler extends RemoteEventHandler<LinkKey, DefaultLink> {
        public RemoteLinkEventHandler(LoadingCache<LinkKey, Optional<DefaultLink>> cache) {
            super(cache);
        }

        @Override
        protected void onAdd(LinkKey key, DefaultLink newVal) {
            addNewLink(newVal);
            notifyDelegate(new LinkEvent(LINK_ADDED, newVal));
        }

        @Override
        protected void onUpdate(LinkKey key, DefaultLink oldVal, DefaultLink newVal) {
            replaceLink(oldVal, newVal);
            notifyDelegate(new LinkEvent(LINK_UPDATED, newVal));
        }

        @Override
        protected void onRemove(LinkKey key, DefaultLink val) {
            removeLink(val);
            notifyDelegate(new LinkEvent(LINK_REMOVED, val));
        }
    }
}
