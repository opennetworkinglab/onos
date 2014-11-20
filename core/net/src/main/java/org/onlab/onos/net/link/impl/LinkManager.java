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
package org.onlab.onos.net.link.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

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
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.link.LinkAdminService;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.link.LinkStore;
import org.onlab.onos.net.link.LinkStoreDelegate;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

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

    protected final AbstractListenerRegistry<LinkEvent, LinkListener>
            listenerRegistry = new AbstractListenerRegistry<>();

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
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LinkListener listener) {
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

            log.info("Links for connection point {} vanished", connectPoint);
            // FIXME: This will remove links registered by other providers
            removeLinks(getLinks(connectPoint), true);
        }

        @Override
        public void linksVanished(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();

            log.info("Links for device {} vanished", deviceId);
            removeLinks(getDeviceLinks(deviceId), true);
        }
    }

    // Removes all links in the specified set and emits appropriate events.
    private void  removeLinks(Set<Link> links, boolean isSoftRemove) {
        for (Link link : links) {
            if (!deviceService.getDevice(link.src().deviceId()).type().equals(
                    deviceService.getDevice(link.dst().deviceId()).type())) {
                //TODO this is aweful. need to be fixed so that we don't down
                // configured links. perhaps add a mechanism to figure out the
                // state of this link
                log.info("Ignoring removal of link as device types are " +
                                 "different {} {} ",
                         link.src() ,
                         link.dst());
                continue;
            }
            LinkEvent event = isSoftRemove ?
                    store.removeOrDownLink(link.src(), link.dst()) :
                    store.removeLink(link.src(), link.dst());
            post(event);
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
