/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.mcast.impl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mcast.api.McastEvent;
import org.onosproject.mcast.api.McastListener;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.mcast.api.McastStore;
import org.onosproject.mcast.api.McastStoreDelegate;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An implementation of a multicast route table.
 */
@Component(immediate = true)
@Service
public class MulticastRouteManager
        extends AbstractListenerManager<McastEvent, McastListener>
        implements MulticastRouteService {
    //TODO: add MulticastRouteAdminService

    private Logger log = getLogger(getClass());

    private final McastStoreDelegate delegate = new InternalMcastStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected McastStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private HostListener hostListener = new InternalHostListener();

    @Activate
    public void activate() {
        hostService.addListener(hostListener);
        eventDispatcher.addSink(McastEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(McastEvent.class);
        log.info("Stopped");
    }

    @Override
    public void add(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        store.storeRoute(route);
    }

    @Override
    public void remove(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        if (checkRoute(route)) {
            store.removeRoute(route);
        }
    }

    @Override
    public Set<McastRoute> getRoutes() {
        return store.getRoutes();
    }

    @Override
    public Set<McastRoute> getRoute(IpAddress groupIp, IpAddress sourceIp) {
        // Let's transform it into an optional
        final Optional<IpAddress> source = Optional.ofNullable(sourceIp);
        return store.getRoutes().stream()
                .filter(route -> route.group().equals(groupIp) &&
                                Objects.equal(route.source(), source))
                .collect(Collectors.toSet());
    }

    @Override
    public void addSources(McastRoute route, Set<ConnectPoint> connectPoints) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(connectPoints, "Source cannot be null");
        if (checkRoute(route)) {
            store.storeSources(route, connectPoints);
        }
    }

    @Override
    public void removeSources(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        if (checkRoute(route)) {
            store.removeSources(route);
        }
    }

    @Override
    public void removeSources(McastRoute route, Set<ConnectPoint> sources) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(sources, "Source cannot be null");
        if (checkRoute(route)) {
            store.removeSources(route, sources);
        }
    }

    @Override
    public void addSink(McastRoute route, HostId hostId) {
        if (checkRoute(route)) {
            Set<ConnectPoint> sinks = new HashSet<>();
            Host host = hostService.getHost(hostId);
            if (host != null) {
                host.locations().forEach(hostLocation -> sinks.add(
                        ConnectPoint.deviceConnectPoint(hostLocation.deviceId() + "/" + hostLocation.port())));
            }
            store.addSink(route, hostId, sinks);
        }

    }

    @Override
    public void addSinks(McastRoute route, HostId hostId, Set<ConnectPoint> sinks) {
        if (checkRoute(route)) {
            store.addSink(route, hostId, sinks);
        }

    }

    @Override
    public void addSink(McastRoute route, Set<ConnectPoint> sinks) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(sinks, "Sinks cannot be null");
        if (checkRoute(route)) {
            store.addSinks(route, sinks);
        }
    }

    @Override
    public void removeSinks(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        if (checkRoute(route)) {
            store.removeSinks(route);
        }
    }

    @Override
    public void removeSink(McastRoute route, HostId hostId) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(hostId, "Host cannot be null");
        if (checkRoute(route)) {
            store.removeSink(route, hostId);
        }
    }

    @Override
    public void removeSinks(McastRoute route, Set<ConnectPoint> connectPoints) {
        checkNotNull(route, "Route cannot be null");
        if (checkRoute(route)) {
            store.removeSinks(route, connectPoints);
        }
    }

    @Override
    public McastRouteData routeData(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        return checkRoute(route) ? store.getRouteData(route) : null;
    }

    @Override
    public Set<ConnectPoint> sources(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        return checkRoute(route) ? store.sourcesFor(route) : ImmutableSet.of();
    }

    @Override
    public Set<ConnectPoint> sinks(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        return checkRoute(route) ? store.sinksFor(route) : ImmutableSet.of();
    }

    @Override
    public Set<ConnectPoint> sinks(McastRoute route, HostId hostId) {
        checkNotNull(route, "Route cannot be null");
        return checkRoute(route) ? store.sinksFor(route, hostId) : ImmutableSet.of();
    }

    @Override
    public Set<ConnectPoint> nonHostSinks(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        return checkRoute(route) ? store.sinksFor(route, HostId.NONE) : ImmutableSet.of();
    }

    private class InternalMcastStoreDelegate implements McastStoreDelegate {
        @Override
        public void notify(McastEvent event) {
            log.debug("Notify event: {}", event);
            post(event);
        }
    }

    private boolean checkRoute(McastRoute route) {
        if (store.getRoutes().contains(route)) {
            return true;
        } else {
            log.warn("Route {} is not present in the store, please add it", route);
        }
        return false;
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            HostId hostId = event.subject().id();
            log.debug("Host event: {}", event);
            switch (event.type()) {
                case HOST_ADDED:
                    //the host is added, if it already comes with some locations let's use them
                    eventAddSinks(hostId, event.subject().locations());
                    break;
                case HOST_MOVED:
                    //both subjects must be null or the system is in an incoherent state
                    if ((event.prevSubject() != null && event.subject() != null)) {
                        //we compute the difference between old locations and new ones and remove the previous
                        Set<HostLocation> removedSinks = Sets.difference(event.prevSubject().locations(),
                                event.subject().locations()).immutableCopy();
                        if (!removedSinks.isEmpty()) {
                            eventRemoveSinks(hostId, removedSinks);
                        }
                        Set<HostLocation> addedSinks = Sets.difference(event.subject().locations(),
                                event.prevSubject().locations()).immutableCopy();
                        //if the host now has some new locations we add them to the sinks set
                        if (!addedSinks.isEmpty()) {
                            eventAddSinks(hostId, addedSinks);
                        }
                    }
                    break;
                case HOST_REMOVED:
                    // Removing all the sinks for that specific host
                    // even if the locations are 0 we keep
                    // the host information in the route in case it shows up again
                    eventRemoveSinks(event.subject().id(), event.subject().locations());
                    break;
                case HOST_UPDATED:
                default:
                    log.debug("Host event {} not handled", event.type());
            }
        }
    }

    //Adds sinks for a given host event
    private void eventRemoveSinks(HostId hostId, Set<HostLocation> removedSinks) {
        Set<ConnectPoint> sinks = new HashSet<>();
        // Build sink using host location
        sinks.addAll(removedSinks);
        // Filter by host id and then remove from each route the provided sinks
        store.getRoutes().stream().filter(mcastRoute -> store.getRouteData(mcastRoute)
                .sinks().get(hostId) != null)
                .forEach(route -> store.removeSinks(route, hostId, sinks));
    }

    //Removes the sinks for a given host event
    private void eventAddSinks(HostId hostId, Set<HostLocation> addedSinks) {
        Set<ConnectPoint> sinks = new HashSet<>();
        // Build sink using host location
        sinks.addAll(addedSinks);
        // Filter by host id and then add to each route the provided sinks
        store.getRoutes().stream().filter(mcastRoute -> store.getRouteData(mcastRoute)
                .sinks().get(hostId) != null)
                .forEach(route -> store.addSink(route, hostId, sinks));
    }
}
