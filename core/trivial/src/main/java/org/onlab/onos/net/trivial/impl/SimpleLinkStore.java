package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkStore;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.link.LinkEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure links using trivial in-memory structures
 * implementation.
 */
@Component(immediate = true)
@Service
public class SimpleLinkStore implements LinkStore {

    private final Logger log = getLogger(getClass());

    // Link inventory
    private final Map<LinkKey, DefaultLink> links = new ConcurrentHashMap<>();

    // Egress and ingress link sets
    private final Multimap<DeviceId, Link> srcLinks = HashMultimap.create();
    private final Multimap<DeviceId, Link> dstLinks = HashMultimap.create();

    private static final Set<Link> EMPTY = ImmutableSet.of();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return Collections.unmodifiableSet(new HashSet<Link>(links.values()));
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
        return links.get(new LinkKey(src, dst));
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
        DefaultLink link = links.get(key);
        if (link == null) {
            return createLink(providerId, key, linkDescription);
        }
        return updateLink(providerId, link, key, linkDescription);
    }

    // Creates and stores the link and returns the appropriate event.
    private LinkEvent createLink(ProviderId providerId, LinkKey key,
                                 LinkDescription linkDescription) {
        DefaultLink link = new DefaultLink(providerId, key.src, key.dst,
                                           linkDescription.type());
        synchronized (this) {
            links.put(key, link);
            srcLinks.put(link.src().deviceId(), link);
            dstLinks.put(link.dst().deviceId(), link);
        }
        return new LinkEvent(LINK_ADDED, link);
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    private LinkEvent updateLink(ProviderId providerId, DefaultLink link,
                                 LinkKey key, LinkDescription linkDescription) {
        if (link.type() == INDIRECT && linkDescription.type() == DIRECT) {
            synchronized (this) {
                srcLinks.remove(link.src().deviceId(), link);
                dstLinks.remove(link.dst().deviceId(), link);

                DefaultLink updated =
                        new DefaultLink(providerId, link.src(), link.dst(),
                                        linkDescription.type());
                links.put(key, updated);
                srcLinks.put(link.src().deviceId(), updated);
                dstLinks.put(link.dst().deviceId(), updated);
                return new LinkEvent(LINK_UPDATED, updated);
            }
        }
        return null;
    }

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        synchronized (this) {
            Link link = links.remove(new LinkKey(src, dst));
            if (link != null) {
                srcLinks.remove(link.src().deviceId(), link);
                dstLinks.remove(link.dst().deviceId(), link);
                return new LinkEvent(LINK_REMOVED, link);
            }
            return null;
        }
    }

    // Auxiliary key to track links.
    private class LinkKey {
        final ConnectPoint src;
        final ConnectPoint dst;

        LinkKey(ConnectPoint src, ConnectPoint dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof LinkKey) {
                final LinkKey other = (LinkKey) obj;
                return Objects.equals(this.src, other.src) &&
                        Objects.equals(this.dst, other.dst);
            }
            return false;
        }
    }
}
