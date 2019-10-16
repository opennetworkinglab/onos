/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routeservice.impl;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.PredictableExecutor;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.routeservice.InternalRouteEvent;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteListener;
import org.onosproject.routeservice.RouteService;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routeservice.RouteStoreDelegate;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of the unicast route service.
 */
@Component(service = { RouteService.class, RouteAdminService.class })
public class RouteManager implements RouteService, RouteAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_BUCKETS = 0;

    private RouteStoreDelegate delegate = new InternalRouteStoreDelegate();
    private InternalHostListener hostListener = new InternalHostListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ResolvedRouteStore resolvedRouteStore;

    private RouteMonitor routeMonitor;

    protected RouteResolver routeResolver;

    private Map<RouteListener, ListenerQueue> listeners = new ConcurrentHashMap<>();

    private ThreadFactory threadFactory;

    protected PredictableExecutor hostEventExecutors;

    @Activate
    protected void activate() {
        routeMonitor = new RouteMonitor(this, clusterService, storageService);
        routeResolver = new RouteResolver(this, hostService);
        threadFactory = groupedThreads("onos/route", "listener-%d", log);
        hostEventExecutors = new PredictableExecutor(DEFAULT_BUCKETS, groupedThreads("onos/route-manager",
                                                                                     "event-host-%d", log));

        resolvedRouteStore = new DefaultResolvedRouteStore();

        routeStore.setDelegate(delegate);
        hostService.addListener(hostListener);

        routeStore.getRouteTables().stream()
                .flatMap(id -> routeStore.getRoutes(id).stream())
                .forEach(routeSet -> routeResolver.resolve(routeSet));
    }

    @Deactivate
    protected void deactivate() {
        routeMonitor.shutdown();
        routeResolver.shutdown();
        listeners.values().forEach(ListenerQueue::stop);

        routeStore.unsetDelegate(delegate);
        hostService.removeListener(hostListener);
    }

    /**
     * {@inheritDoc}
     *
     * In a departure from other services in ONOS, calling addListener will
     * cause all current routes to be pushed to the listener before any new
     * events are sent. This allows a listener to easily get the exact set of
     * routes without worrying about missing any.
     *
     * @param listener listener to be added
     */
    @Override
    public void addListener(RouteListener listener) {
        log.debug("Synchronizing current routes to new listener");
        ListenerQueue listenerQueue = listeners.compute(listener, (key, value) -> {
            // Create listener regardless the existence of a previous value
            ListenerQueue l = createListenerQueue(listener);
            resolvedRouteStore.getRouteTables().stream()
                    .map(resolvedRouteStore::getRoutes)
                    .flatMap(Collection::stream)
                    .map(route -> new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route,
                                                 resolvedRouteStore.getAllRoutes(route.prefix())))
                    .forEach(l::post);
            return l;
        });
        // Start draining the events
        listenerQueue.start();
        log.debug("Route synchronization complete");
    }

    @Override
    public void removeListener(RouteListener listener) {
        ListenerQueue l = listeners.remove(listener);
        if (l != null) {
            l.stop();
        }
    }

    /**
     * Posts an event to all listeners.
     *
     * @param event event
     */
    private void post(RouteEvent event) {
        if (event != null) {
            log.debug("Sending event {}", event);
            listeners.values().forEach(l -> l.post(event));
        }
    }

    @Override
    public Collection<RouteTableId> getRouteTables() {
        return routeStore.getRouteTables();
    }

    @Override
    public Collection<RouteInfo> getRoutes(RouteTableId id) {
        return routeStore.getRoutes(id).stream()
                .map(routeSet -> new RouteInfo(routeSet.prefix(),
                                               resolvedRouteStore.getRoute(routeSet.prefix()).orElse(null),
                                               routeResolver.resolveRouteSet(routeSet)))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ResolvedRoute> getResolvedRoutes(RouteTableId id) {
        return resolvedRouteStore.getRoutes(id);
    }

    @Override
    public Optional<ResolvedRoute> longestPrefixLookup(IpAddress ip) {
        return resolvedRouteStore.longestPrefixMatch(ip);
    }

    @Override
    public Collection<ResolvedRoute> getAllResolvedRoutes(IpPrefix prefix) {
        return ImmutableList.copyOf(resolvedRouteStore.getAllRoutes(prefix));
    }

    @Override
    public void update(Collection<Route> routes) {
        log.debug("Received update {}", routes);
        routeStore.updateRoutes(routes);
    }

    @Override
    public void withdraw(Collection<Route> routes) {
        log.debug("Received withdraw {}", routes);
        routeStore.removeRoutes(routes);
    }

    @Override
    public Route longestPrefixMatch(IpAddress ip) {
        return longestPrefixLookup(ip)
                .map(ResolvedRoute::route)
                .orElse(null);
    }

    void store(ResolvedRoute route, Set<ResolvedRoute> alternatives) {
        post(resolvedRouteStore.updateRoute(route, alternatives));
    }

    void remove(IpPrefix prefix) {
        post(resolvedRouteStore.removeRoute(prefix));
    }

    private void hostUpdated(Host host) {
        hostChanged(host);
    }

    private void hostRemoved(Host host) {
        hostChanged(host);
    }

    private void hostChanged(Host host) {
        routeStore.getRoutesForNextHops(host.ipAddresses())
                .forEach(routeSet -> routeResolver.resolve(routeSet));
    }

    /**
     * Creates a new listener queue.
     *
     * @param listener route listener
     * @return listener queue
     */
    ListenerQueue createListenerQueue(RouteListener listener) {
        return new DefaultListenerQueue(listener);
    }

    /**
     * Default route listener queue.
     */
    private class DefaultListenerQueue implements ListenerQueue {

        private final ExecutorService executorService;
        private final BlockingQueue<RouteEvent> queue;
        private final RouteListener listener;

        /**
         * Creates a new listener queue.
         *
         * @param listener route listener to queue updates for
         */
        public DefaultListenerQueue(RouteListener listener) {
            this.listener = listener;
            queue = new LinkedBlockingQueue<>();
            executorService = newSingleThreadExecutor(threadFactory);
        }

        @Override
        public void post(RouteEvent event) {
            queue.add(event);
        }

        @Override
        public void start() {
            executorService.execute(this::poll);
        }

        @Override
        public void stop() {
            executorService.shutdown();
        }

        private void poll() {
            while (true) {
                try {
                    listener.event(queue.take());
                } catch (InterruptedException e) {
                    log.info("Route listener event thread shutting down: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("Exception during route event handler", e);
                }
            }
        }
    }

    /**
     * Delegate to receive events from the route store.
     */
    private class InternalRouteStoreDelegate implements RouteStoreDelegate {
        @Override
        public void notify(InternalRouteEvent event) {
            switch (event.type()) {
            case ROUTE_ADDED:
                routeResolver.resolve(event.subject());
                break;
            case ROUTE_REMOVED:
                routeResolver.resolve(event.subject());
                break;
            default:
                break;
            }
        }
    }

    /**
     * Internal listener for host events.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
            case HOST_ADDED:
            case HOST_UPDATED:
            case HOST_MOVED:
                log.trace("Scheduled host event {}", event);
                hostEventExecutors.execute(() -> hostUpdated(event.subject()), event.subject().id().hashCode());
                break;
            case HOST_REMOVED:
                log.trace("Scheduled host event {}", event);
                hostEventExecutors.execute(() -> hostRemoved(event.subject()), event.subject().id().hashCode());
                break;
            default:
                break;
            }
        }
    }

}
