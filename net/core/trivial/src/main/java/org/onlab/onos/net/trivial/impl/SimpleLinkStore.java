package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.provider.ProviderId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages inventory of infrastructure links using trivial in-memory link
 * implementation.
 */
class SimpleLinkStore {

    // Link inventory
    private final Map<LinkKey, DefaultLink> links = new ConcurrentHashMap<>();

    // Egress and ingress link sets
    private final Multimap<DeviceId, Link> srcLinks = HashMultimap.create();
    private final Multimap<DeviceId, Link> dstLinks = HashMultimap.create();

    private static final Set<Link> EMPTY = ImmutableSet.copyOf(new Link[]{});

    /**
     * Returns the number of links in the store.
     *
     * @return number of links
     */
    int getLinkCount() {
        return links.size();
    }

    /**
     * Returns an iterable collection of all links in the inventory.
     *
     * @return collection of all links
     */
    Iterable<Link> getLinks() {
        return Collections.unmodifiableSet(new HashSet<Link>(links.values()));
    }

    /**
     * Returns all links egressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return ImmutableSet.copyOf(srcLinks.get(deviceId));
    }

    /**
     * Returns all links ingressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        return ImmutableSet.copyOf(dstLinks.get(deviceId));
    }

    /**
     * Returns all links egressing from the specified connection point.
     *
     * @param src source connection point
     * @return set of connection point links
     */
    Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egress = new HashSet<>();
        for (Link link : srcLinks.get(src.deviceId())) {
            if (link.src().equals(src)) {
                egress.add(link);
            }
        }
        return egress;
    }

    /**
     * Returns all links ingressing to the specified connection point.
     *
     * @param dst destination connection point
     * @return set of connection point links
     */
    Set<Link> getIngressLinks(ConnectPoint dst) {
        Set<Link> ingress = new HashSet<>();
        for (Link link : dstLinks.get(dst.deviceId())) {
            if (link.src().equals(dst)) {
                ingress.add(link);
            }
        }
        return ingress;
    }


    /**
     * Creates a new link, or updates an existing one, based on the given
     * information.
     *
     * @param providerId      provider identity
     * @param linkDescription link description
     * @return create or update link event, or null if no change resulted
     */
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {
        LinkKey key = new LinkKey(linkDescription.src(), linkDescription.dst());
        DefaultLink link = links.get(key);
        if (link == null) {
            return createLink(providerId, key, linkDescription);
        }
        return updateLink(link, linkDescription);
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
        return new LinkEvent(LinkEvent.Type.LINK_ADDED, link);
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    private LinkEvent updateLink(DefaultLink link, LinkDescription linkDescription) {
        return null;
    }

    /**
     * Removes the link based on the specified information.
     *
     * @param src link source
     * @param dst link destination
     * @return remove link event, or null if no change resulted
     */
    LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        synchronized (this) {
            Link link = links.remove(new LinkKey(src, dst));
            srcLinks.remove(link.src().deviceId(), link);
            dstLinks.remove(link.dst().deviceId(), link);
            return link == null ? null : new LinkEvent(LinkEvent.Type.LINK_REMOVED, link);
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
            if (obj instanceof LinkKey) {
                final LinkKey other = (LinkKey) obj;
                return Objects.equals(this.src, other.src) &&
                        Objects.equals(this.dst, other.dst);
            }
            return false;
        }
    }
}
