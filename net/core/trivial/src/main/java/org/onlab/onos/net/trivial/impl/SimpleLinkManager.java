package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.link.LinkAdminService;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the link SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleLinkManager
        extends AbstractProviderRegistry<LinkProvider, LinkProviderService>
        implements LinkService, LinkAdminService, LinkProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String LINK_DESC_NULL = "Link description cannot be null";
    private static final String CONNECT_POINT_NULL = "Connection point cannot be null";

    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<LinkEvent, LinkListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final SimpleLinkStore store = new SimpleLinkStore();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        eventDispatcher.addSink(LinkEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(LinkEvent.class);
        log.info("Stopped");
    }

    @Override
    protected LinkProviderService createProviderService(LinkProvider provider) {
        return new InternalLinkProviderService(provider);
    }

    @Override
    public int getLinkCount() {
        return store.getLinkCount();
    }

    @Override
    public Iterable<Link> getLinks() {
        return store.getLinks();
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return Sets.union(store.getDeviceEgressLinks(deviceId),
                          store.getDeviceIngressLinks(deviceId));
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceEgressLinks(deviceId);
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceIngressLinks(deviceId);
    }

    @Override
    public Set<Link> getLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return Sets.union(store.getEgressLinks(connectPoint),
                          store.getIngressLinks(connectPoint));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getEgressLinks(connectPoint);
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getIngressLinks(connectPoint);
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        checkNotNull(src, CONNECT_POINT_NULL);
        checkNotNull(dst, CONNECT_POINT_NULL);
        return store.getLink(src, dst);
    }

    @Override
    public void removeLinks(ConnectPoint connectPoint) {
        removeLinks(getLinks(connectPoint));
    }

    @Override
    public void removeLinks(DeviceId deviceId) {
        removeLinks(getDeviceLinks(deviceId));
    }

    @Override
    public void addListener(LinkListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LinkListener listener) {
        listenerRegistry.removeListener(listener);
    }

    // Personalized link provider service issued to the supplied provider.
    private class InternalLinkProviderService extends AbstractProviderService<LinkProvider>
            implements LinkProviderService {

        InternalLinkProviderService(LinkProvider provider) {
            super(provider);
        }

        @Override
        public void linkDetected(LinkDescription linkDescription) {
            checkNotNull(linkDescription, LINK_DESC_NULL);
            checkValidity();
            log.info("Link {} detected", linkDescription);
            LinkEvent event = store.createOrUpdateLink(provider().id(),
                                                       linkDescription);
            post(event);
        }

        @Override
        public void linkVanished(LinkDescription linkDescription) {
            checkNotNull(linkDescription, LINK_DESC_NULL);
            checkValidity();
            log.info("Link {} vanished", linkDescription);
            LinkEvent event = store.removeLink(linkDescription.src(),
                                               linkDescription.dst());
            post(event);
        }

        @Override
        public void linksVanished(ConnectPoint connectPoint) {
            checkNotNull(connectPoint, "Connect point cannot be null");
            checkValidity();
            log.info("Link for connection point {} vanished", connectPoint);
            removeLinks(getLinks(connectPoint));
        }

        @Override
        public void linksVanished(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();
            log.info("Link for device {} vanished", deviceId);
            removeLinks(getDeviceLinks(deviceId));
        }
    }

    // Removes all links in the specified set and emits appropriate events.
    private void removeLinks(Set<Link> links) {
        for (Link link : links) {
            LinkEvent event = store.removeLink(link.src(), link.dst());
            post(event);
        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(LinkEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

}
