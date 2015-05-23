/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.link.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.Permission;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.State;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;


/**
 * Provides basic implementation of the link SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class LinkManager
        extends AbstractProviderRegistry<LinkProvider, LinkProviderService>
        implements LinkService, LinkAdminService, LinkProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String LINK_DESC_NULL = "Link description cannot be null";
    private static final String CONNECT_POINT_NULL = "Connection point cannot be null";

    private final Logger log = getLogger(getClass());

    protected final ListenerRegistry<LinkEvent, LinkListener>
            listenerRegistry = new ListenerRegistry<>();

    private final LinkStoreDelegate delegate = new InternalStoreDelegate();

    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(LinkEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LinkEvent.class);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    @Override
    public int getLinkCount() {
        checkPermission(Permission.LINK_READ);

        return store.getLinkCount();
    }

    @Override
    public Iterable<Link> getLinks() {
        checkPermission(Permission.LINK_READ);

        return store.getLinks();
    }

    @Override
    public Iterable<Link> getActiveLinks() {
        checkPermission(Permission.LINK_READ);

        return FluentIterable.from(getLinks())
                .filter(new Predicate<Link>() {

                    @Override
                    public boolean apply(Link input) {
                        return input.state() == State.ACTIVE;
                    }
                });
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return Sets.union(store.getDeviceEgressLinks(deviceId),
                          store.getDeviceIngressLinks(deviceId));
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceEgressLinks(deviceId);
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceIngressLinks(deviceId);
    }

    @Override
    public Set<Link> getLinks(ConnectPoint connectPoint) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return Sets.union(store.getEgressLinks(connectPoint),
                          store.getIngressLinks(connectPoint));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getEgressLinks(connectPoint);
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getIngressLinks(connectPoint);
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        checkPermission(Permission.LINK_READ);

        checkNotNull(src, CONNECT_POINT_NULL);
        checkNotNull(dst, CONNECT_POINT_NULL);
        return store.getLink(src, dst);
    }

    @Override
    public void removeLinks(ConnectPoint connectPoint) {
        if (deviceService.getRole(connectPoint.deviceId()) != MastershipRole.MASTER) {
            return;
        }
        removeLinks(getLinks(connectPoint), false);
    }

    @Override
    public void removeLinks(DeviceId deviceId) {
        if (deviceService.getRole(deviceId) != MastershipRole.MASTER) {
            return;
        }
        removeLinks(getDeviceLinks(deviceId), false);
    }

    @Override
    public void addListener(LinkListener listener) {
        checkPermission(Permission.LINK_EVENT);

        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LinkListener listener) {
        checkPermission(Permission.LINK_EVENT);

        listenerRegistry.removeListener(listener);
    }

    // Auxiliary interceptor for device remove events to prune links that
    // are associated with the removed device or its port.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == DeviceEvent.Type.DEVICE_REMOVED) {
                removeLinks(event.subject().id());
            } else if (event.type() == DeviceEvent.Type.PORT_REMOVED) {
                removeLinks(new ConnectPoint(event.subject().id(),
                                             event.port().number()));
            }
        }
    }

    @Override
    protected LinkProviderService createProviderService(LinkProvider provider) {
        return new InternalLinkProviderService(provider);
    }

    // Personalized link provider service issued to the supplied provider.
    private class InternalLinkProviderService
            extends AbstractProviderService<LinkProvider>
            implements LinkProviderService {

        InternalLinkProviderService(LinkProvider provider) {
            super(provider);
        }

        @Override
        public void linkDetected(LinkDescription linkDescription) {
            checkNotNull(linkDescription, LINK_DESC_NULL);
            checkValidity();

            LinkEvent event = store.createOrUpdateLink(provider().id(),
                                                       linkDescription);
            if (event != null) {
                log.info("Link {} detected", linkDescription);
                post(event);
            }
        }

        @Override
        public void linkVanished(LinkDescription linkDescription) {
            checkNotNull(linkDescription, LINK_DESC_NULL);
            checkValidity();

            ConnectPoint src = linkDescription.src();
            ConnectPoint dst = linkDescription.dst();

            LinkEvent event = store.removeOrDownLink(src, dst);
            if (event != null) {
                log.info("Link {} vanished", linkDescription);
                post(event);
            }
        }

        @Override
        public void linksVanished(ConnectPoint connectPoint) {
            checkNotNull(connectPoint, "Connect point cannot be null");
            checkValidity();

            log.debug("Links for connection point {} vanished", connectPoint);
            // FIXME: This will remove links registered by other providers
            removeLinks(getLinks(connectPoint), true);
        }

        @Override
        public void linksVanished(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();

            log.debug("Links for device {} vanished", deviceId);
            removeLinks(getDeviceLinks(deviceId), true);
        }
    }

    // Removes all links in the specified set and emits appropriate events.
    private void  removeLinks(Set<Link> links, boolean isSoftRemove) {
        for (Link link : links) {
            LinkEvent event = isSoftRemove ?
                    store.removeOrDownLink(link.src(), link.dst()) :
                    store.removeLink(link.src(), link.dst());
            if (event != null) {
                log.info("Link {} removed/vanished", event.subject());
                post(event);
            }
        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(LinkEvent event) {
        if (event != null) {
            eventDispatcher.post(event);
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements LinkStoreDelegate {
        @Override
        public void notify(LinkEvent event) {
            post(event);
        }
    }
}
