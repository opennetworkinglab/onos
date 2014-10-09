package org.onlab.onos.store.link.impl;

import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.ClockService;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.device.impl.VersionedValue;
import org.slf4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableSet.Builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

//TODO: Add support for multiple provider and annotations
/**
 * Manages inventory of infrastructure links using a protocol that takes into consideration
 * the order in which events occur.
 */
// FIXME: This does not yet implement the full protocol.
// The full protocol requires the sender of LLDP message to include the
// version information of src device/port and the receiver to
// take that into account when figuring out if a more recent src
// device/port down event renders the link discovery obsolete.
@Component(immediate = true)
@Service
public class OnosDistributedLinkStore
    extends AbstractStore<LinkEvent, LinkStoreDelegate>
    implements LinkStore {

    private final Logger log = getLogger(getClass());

    // Link inventory
    private ConcurrentMap<LinkKey, VersionedValue<Link>> links;

    public static final String LINK_NOT_FOUND = "Link between %s and %s not found";

    // TODO synchronize?
    // Egress and ingress link sets
    private final Multimap<DeviceId, VersionedValue<Link>> srcLinks = HashMultimap.create();
    private final Multimap<DeviceId, VersionedValue<Link>> dstLinks = HashMultimap.create();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClockService clockService;

    @Activate
    public void activate() {

        links = new ConcurrentHashMap<>();

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
        Builder<Link> builder = ImmutableSet.builder();
        synchronized (this) {
            for (VersionedValue<Link> link : links.values()) {
                builder.add(link.entity());
            }
            return builder.build();
        }
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        Set<VersionedValue<Link>> egressLinks = ImmutableSet.copyOf(srcLinks.get(deviceId));
        Set<Link> rawEgressLinks = new HashSet<>();
        for (VersionedValue<Link> link : egressLinks) {
            rawEgressLinks.add(link.entity());
        }
        return rawEgressLinks;
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        Set<VersionedValue<Link>> ingressLinks = ImmutableSet.copyOf(dstLinks.get(deviceId));
        Set<Link> rawIngressLinks = new HashSet<>();
        for (VersionedValue<Link> link : ingressLinks) {
            rawIngressLinks.add(link.entity());
        }
        return rawIngressLinks;
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        VersionedValue<Link> link = links.get(new LinkKey(src, dst));
        checkArgument(link != null, "LINK_NOT_FOUND", src, dst);
        return link.entity();
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint src) {
        Set<Link> egressLinks = new HashSet<>();
        for (VersionedValue<Link> link : srcLinks.get(src.deviceId())) {
            if (link.entity().src().equals(src)) {
                egressLinks.add(link.entity());
            }
        }
        return egressLinks;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint dst) {
        Set<Link> ingressLinks = new HashSet<>();
        for (VersionedValue<Link> link : dstLinks.get(dst.deviceId())) {
            if (link.entity().dst().equals(dst)) {
                ingressLinks.add(link.entity());
            }
        }
        return ingressLinks;
    }

    @Override
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription) {

        final DeviceId destinationDeviceId = linkDescription.dst().deviceId();
        final Timestamp newTimestamp = clockService.getTimestamp(destinationDeviceId);

        LinkKey key = new LinkKey(linkDescription.src(), linkDescription.dst());
        VersionedValue<Link> link = links.get(key);
        if (link == null) {
            return createLink(providerId, key, linkDescription, newTimestamp);
        }

        checkState(newTimestamp.compareTo(link.timestamp()) > 0,
                "Existing Link has a timestamp in the future!");

        return updateLink(providerId, link, key, linkDescription, newTimestamp);
    }

    // Creates and stores the link and returns the appropriate event.
    private LinkEvent createLink(ProviderId providerId, LinkKey key,
            LinkDescription linkDescription, Timestamp timestamp) {
        VersionedValue<Link> link = new VersionedValue<Link>(new DefaultLink(providerId, key.src(), key.dst(),
                linkDescription.type()), true, timestamp);
        synchronized (this) {
            links.put(key, link);
            addNewLink(link, timestamp);
        }
        // FIXME: notify peers.
        return new LinkEvent(LINK_ADDED, link.entity());
    }

    // update Egress and ingress link sets
    private void addNewLink(VersionedValue<Link> link, Timestamp timestamp) {
        Link rawLink = link.entity();
        synchronized (this) {
            srcLinks.put(rawLink.src().deviceId(), link);
            dstLinks.put(rawLink.dst().deviceId(), link);
        }
    }

    // Updates, if necessary the specified link and returns the appropriate event.
    private LinkEvent updateLink(ProviderId providerId, VersionedValue<Link> existingLink,
                                 LinkKey key, LinkDescription linkDescription, Timestamp timestamp) {
        // FIXME confirm Link update condition is OK
        if (existingLink.entity().type() == INDIRECT && linkDescription.type() == DIRECT) {
            synchronized (this) {

                VersionedValue<Link> updatedLink = new VersionedValue<Link>(
                        new DefaultLink(providerId, existingLink.entity().src(), existingLink.entity().dst(),
                                        linkDescription.type()), true, timestamp);
                links.replace(key, existingLink, updatedLink);

                replaceLink(existingLink, updatedLink);
                // FIXME: notify peers.
                return new LinkEvent(LINK_UPDATED, updatedLink.entity());
            }
        }
        return null;
    }

    // update Egress and ingress link sets
    private void replaceLink(VersionedValue<Link> current, VersionedValue<Link> updated) {
        synchronized (this) {
            srcLinks.remove(current.entity().src().deviceId(), current);
            dstLinks.remove(current.entity().dst().deviceId(), current);

            srcLinks.put(current.entity().src().deviceId(), updated);
            dstLinks.put(current.entity().dst().deviceId(), updated);
        }
    }

    @Override
    public LinkEvent removeLink(ConnectPoint src, ConnectPoint dst) {
        synchronized (this) {
            LinkKey key = new LinkKey(src, dst);
            VersionedValue<Link> link = links.remove(key);
            if (link != null) {
                removeLink(link);
                // notify peers
                return new LinkEvent(LINK_REMOVED, link.entity());
            }
            return null;
        }
    }

    // update Egress and ingress link sets
    private void removeLink(VersionedValue<Link> link) {
        synchronized (this) {
            srcLinks.remove(link.entity().src().deviceId(), link);
            dstLinks.remove(link.entity().dst().deviceId(), link);
        }
    }
}
