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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.routeservice.InternalRouteEvent;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteListener;
import org.onosproject.routeservice.RouteService;
import org.onosproject.routeservice.RouteSet;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routeservice.RouteStoreDelegate;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of the unicast route service.
 */
@Service
@Component
public class RouteManager implements RouteService, RouteAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private RouteStoreDelegate delegate = new InternalRouteStoreDelegate();
    private InternalHostListener hostListener = new InternalHostListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ResolvedRouteStore resolvedRouteStore;

    private RouteMonitor routeMonitor;

    @GuardedBy(value = "this")
    private Map<RouteListener, ListenerQueue> listeners = new HashMap<>();

    private ThreadFactory threadFactory;

    protected Executor hostEventExecutor = newSingleThreadExecutor(
        groupedThreads("rm-event-host", "%d", log));

    @Activate
    protected void activate() {
        routeMonitor = new RouteMonitor(this, clusterService, storageService);
        threadFactory = groupedThreads("onos/route", "listener-%d", log);

        resolvedRouteStore = new DefaultResolvedRouteStore();

        routeStore.setDelegate(delegate);
        hostService.addListener(hostListener);

        routeStore.getRouteTables().stream()
                .flatMap(id -> routeStore.getRoutes(id).stream())
                .forEach(this::resolve);
    }

    @Deactivate
    protected void deactivate() {
        routeMonitor.shutdown();
        synchronized (this) {
            listeners.values().forEach(ListenerQueue::stop);
        }

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
        synchronized (this) {
            log.debug("Synchronizing current routes to new listener");
            ListenerQueue l = createListenerQueue(listener);
            resolvedRouteStore.getRouteTables().stream()
                    .map(resolvedRouteStore::getRoutes)
                    .flatMap(Collection::stream)
                    .map(route -> new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route,
                                                 resolvedRouteStore.getAllRoutes(route.prefix())))
                    .forEach(l::post);

            listeners.put(listener, l);

            l.start();
            log.debug("Route synchronization complete");
        }
    }

    @Override
    public void removeListener(RouteListener listener) {
        synchronized (this) {
            ListenerQueue l = listeners.remove(listener);
            if (l != null) {
                l.stop();
            }
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
            synchronized (this) {
                listeners.values().forEach(l -> l.post(event));
            }
        }
    }

    private Collection<Route> reformatRoutes(Collection<RouteSet> routeSets) {
        return routeSets.stream().flatMap(r -> r.routes().stream()).collect(Collectors.toList());
    }

    @Override
    public Collection<RouteTableId> getRouteTables() {
        return routeStore.getRouteTables();
    }

    @Override
    public Collection<RouteInfo> getRoutes(RouteTableId id) {
        return routeStore.getRoutes(id).stream()
                .map(routeSet -> new RouteInfo(routeSet.prefix(),
                        resolvedRouteStore.getRoute(routeSet.prefix()).orElse(null), resolveRouteSet(routeSet)))
                .collect(Collectors.toList());
    }

    private Set<ResolvedRoute> resolveRouteSet(RouteSet routeSet) {
        return routeSet.routes().stream()
                .map(this::tryResolve)
                .collect(Collectors.toSet());
    }

    private ResolvedRoute tryResolve(Route route) {
        ResolvedRoute resolvedRoute = resolve(route);
        if (resolvedRoute == null) {
            resolvedRoute = new ResolvedRoute(route, null, null, null);
        }
        return resolvedRoute;
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
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received update {}", route);
                routeStore.updateRoute(route);
            });
        }
    }

    @Override
    public void withdraw(Collection<Route> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received withdraw {}", route);
                routeStore.removeRoute(route);
            });
        }
    }

    @Override
    public Route longestPrefixMatch(IpAddress ip) {
        return longestPrefixLookup(ip)
                .map(ResolvedRoute::route)
                .orElse(null);
    }

    private ResolvedRoute resolve(Route route) {
        hostService.startMonitoringIp(route.nextHop());
        Set<Host> hosts = hostService.getHostsByIp(route.nextHop());

        return hosts.stream().findFirst()
                .map(host -> new ResolvedRoute(route, host.mac(), host.vlan(), host.location()))
                .orElse(null);
    }

    private ResolvedRoute decide(ResolvedRoute route1, ResolvedRoute route2) {
        return Comparator.comparing(ResolvedRoute::nextHop)
                       .compare(route1, route2) <= 0 ? route1 : route2;
    }

    private void store(ResolvedRoute route, Set<ResolvedRoute> alternatives) {
        post(resolvedRouteStore.updateRoute(route, alternatives));
    }

    private void remove(IpPrefix prefix) {
        post(resolvedRouteStore.removeRoute(prefix));
    }

    private void resolve(RouteSet routes) {
        if (routes.routes() == null) {
            // The routes were removed before we got to them, nothing to do
            return;
        }
        Set<ResolvedRoute> resolvedRoutes = routes.routes().stream()
                .map(this::resolve)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Optional<ResolvedRoute> bestRoute = resolvedRoutes.stream()
                    .reduce(this::decide);

        if (bestRoute.isPresent()) {
            store(bestRoute.get(), resolvedRoutes);
        } else {
            remove(routes.prefix());
        }
    }

    private void hostUpdated(Host host) {
        hostChanged(host);
    }

    private void hostRemoved(Host host) {
        hostChanged(host);
    }

    private void hostChanged(Host host) {
        synchronized (this) {
            host.ipAddresses().stream()
                    .flatMap(ip -> routeStore.getRoutesForNextHop(ip).stream())
                    .map(route -> routeStore.getRoutes(route.prefix()))
                    .forEach(this::resolve);
        }
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
                resolve(event.subject());
                break;
            case ROUTE_REMOVED:
                resolve(event.subject());
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
                log.trace("Scheduled host event {}", event);
                hostEventExecutor.execute(() -> hostUpdated(event.subject()));
                break;
            case HOST_REMOVED:
                log.trace("Scheduled host event {}", event);
                hostEventExecutor.execute(() -> hostRemoved(event.subject()));
                break;
            case HOST_MOVED:
                break;
            default:
                break;
            }
        }
    }

}
