/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.State;
import org.onosproject.net.LinkKey;
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
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;


/**
 * Provides basic implementation of the link SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class LinkManager
        extends AbstractListenerProviderRegistry<LinkEvent, LinkListener, LinkProvider, LinkProviderService>
        implements LinkService, LinkAdminService, LinkProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String LINK_DESC_NULL = "Link description cannot be null";
    private static final String CONNECT_POINT_NULL = "Connection point cannot be null";

    private final Logger log = getLogger(getClass());

    private final LinkStoreDelegate delegate = new InternalStoreDelegate();

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private final NetworkConfigListener networkConfigListener = new InternalNetworkConfigListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(LinkEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        networkConfigService.addListener(networkConfigListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LinkEvent.class);
        deviceService.removeListener(deviceListener);
        networkConfigService.removeListener(networkConfigListener);
        log.info("Stopped");
    }

    @Override
    public int getLinkCount() {
        checkPermission(LINK_READ);
        return store.getLinkCount();
    }

    @Override
    public Iterable<Link> getLinks() {
        checkPermission(LINK_READ);
        return store.getLinks();
    }

    @Override
    public Iterable<Link> getActiveLinks() {
        checkPermission(LINK_READ);
        return FluentIterable.from(getLinks())
                .filter(input -> input.state() == State.ACTIVE);
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        checkPermission(LINK_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return Sets.union(store.getDeviceEgressLinks(deviceId),
                          store.getDeviceIngressLinks(deviceId));
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        checkPermission(LINK_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceEgressLinks(deviceId);
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        checkPermission(LINK_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDeviceIngressLinks(deviceId);
    }

    @Override
    public Set<Link> getLinks(ConnectPoint connectPoint) {
        checkPermission(LINK_READ);
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return Sets.union(store.getEgressLinks(connectPoint),
                          store.getIngressLinks(connectPoint));
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        checkPermission(LINK_READ);
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getEgressLinks(connectPoint);
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
        checkPermission(LINK_READ);
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return store.getIngressLinks(connectPoint);
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        checkPermission(LINK_READ);
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
    public void removeLink(ConnectPoint src, ConnectPoint dst) {
        post(store.removeLink(src, dst));
    }

    private boolean isAllowed(BasicLinkConfig cfg) {
        return (cfg == null || cfg.isAllowed());
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
            linkDescription = validateLink(linkDescription);
            if (linkDescription != null) {
                LinkEvent event = store.createOrUpdateLink(provider().id(), linkDescription);
                if (event != null) {
                    log.info("Link {} detected", linkDescription);
                    post(event);
                }
            }
        }

        /**
         * Validates configuration against link configuration.
         *
         * @param linkDescription input
         * @return description combined with configuration or null if disallowed
         */
        private LinkDescription validateLink(LinkDescription linkDescription) {
            BasicLinkConfig cfg = networkConfigService.getConfig(linkKey(linkDescription.src(),
                                                                         linkDescription.dst()),
                                                                 BasicLinkConfig.class);
            if (!isAllowed(cfg)) {
                log.trace("Link {} is not allowed", linkDescription);
                return null;
            }

            // test if bidirectional reverse configuration exists
            BasicLinkConfig cfgRev = networkConfigService.getConfig(linkKey(linkDescription.dst(),
                                                                            linkDescription.src()),
                                                                    BasicLinkConfig.class);
            LinkDescription description = linkDescription;
            if (cfgRev != null && cfgRev.isBidirectional()) {
                if (!cfgRev.isAllowed()) {
                    log.trace("Link {} is not allowed (rev)", linkDescription);
                    return null;
                }
                description = BasicLinkOperator.combine(cfgRev, description);
            }

            description = BasicLinkOperator.combine(cfg, description);
            return description;
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
    private void removeLinks(Set<Link> links, boolean isSoftRemove) {
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

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements LinkStoreDelegate {
        @Override
        public void notify(LinkEvent event) {
            post(event);
        }
    }

    // listens for NetworkConfigEvents of type BasicLinkConfig and removes
    // links that the config does not allow
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(BasicLinkConfig.class)
                    && (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED
                        || event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }

        @Override
        public void event(NetworkConfigEvent event) {
            LinkKey lk = (LinkKey) event.subject();
            BasicLinkConfig cfg = (BasicLinkConfig) event.config().get();

            log.debug("Detected link network config event {}", event.type());

            if (!isAllowed(cfg)) {
                log.info("Kicking out links between {} and {}", lk.src(), lk.dst());
                removeLink(lk.src(), lk.dst());
                if (cfg.isBidirectional()) {
                    removeLink(lk.dst(), lk.src());
                }
                return;
            }

            doUpdate(lk.src(), lk.dst(), cfg);
            if (cfg.isBidirectional()) {
                doUpdate(lk.dst(), lk.src(), cfg);
            }
        }

        private void doUpdate(ConnectPoint src, ConnectPoint dst, BasicLinkConfig cfg) {
            Link link = getLink(src, dst);
            LinkDescription desc;

            if (link == null) {
                // TODO Revisit this behaviour.
                // config alone probably should not be adding a link,
                // netcfg provider should be the one.
                desc = BasicLinkOperator.descriptionOf(src, dst, cfg);
            } else {
                desc = BasicLinkOperator.combine(cfg,
                        BasicLinkOperator.descriptionOf(src, dst, link));
            }

            ProviderId pid = Optional.ofNullable(link)
                    .map(Link::providerId)
                    .orElse(ProviderId.NONE);
            store.createOrUpdateLink(pid, desc);
        }
    }
}
