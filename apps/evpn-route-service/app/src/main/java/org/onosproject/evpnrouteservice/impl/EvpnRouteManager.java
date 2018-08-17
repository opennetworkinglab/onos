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

package org.onosproject.evpnrouteservice.impl;

import org.onosproject.evpnrouteservice.EvpnInternalRouteEvent;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteAdminService;
import org.onosproject.evpnrouteservice.EvpnRouteEvent;
import org.onosproject.evpnrouteservice.EvpnRouteListener;
import org.onosproject.evpnrouteservice.EvpnRouteService;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteStore;
import org.onosproject.evpnrouteservice.EvpnRouteStoreDelegate;
import org.onosproject.evpnrouteservice.EvpnRouteTableId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of the EVPN route service.
 */
@Component(service = { EvpnRouteService.class, EvpnRouteAdminService.class })
public class EvpnRouteManager implements EvpnRouteService,
        EvpnRouteAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteStore evpnRouteStore;

    @GuardedBy(value = "this")
    private Map<EvpnRouteListener, EvpnListenerQueue> listeners = new
            HashMap<>();

    private ThreadFactory threadFactory;

    private EvpnRouteStoreDelegate evpnRouteStoreDelegate = new
            InternalEvpnRouteStoreDelegate();

    @Activate
    protected void activate() {
        threadFactory = groupedThreads("onos/route", "listener-%d", log);
        evpnRouteStore.setDelegate(evpnRouteStoreDelegate);

    }

    @Deactivate
    protected void deactivate() {
        evpnRouteStore.unsetDelegate(evpnRouteStoreDelegate);
        synchronized (this) {
            listeners.values().forEach(EvpnListenerQueue::stop);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * In a departure from other services in ONOS, calling addListener will
     * cause all current routes to be pushed to the listener before any new
     * events are sent. This allows a listener to easily get the exact set of
     * routes without worrying about missing any.
     *
     * @param listener listener to be added
     */
    @Override
    public void addListener(EvpnRouteListener listener) {
        synchronized (this) {
            EvpnListenerQueue l = createListenerQueue(listener);

            evpnRouteStore.getRouteTables().forEach(routeTableId
                                                            -> {
                Collection<EvpnRouteSet> routes
                        = evpnRouteStore.getRoutes(routeTableId);
                if (routes != null) {
                    routes.forEach(route -> {
                        Collection<EvpnRoute> evpnRoutes = route.routes();
                        for (EvpnRoute evpnRoute : evpnRoutes) {
                            l.post(new EvpnRouteEvent(
                                    EvpnRouteEvent.Type.ROUTE_ADDED,
                                    evpnRoute,
                                    route.routes()));
                        }
                    });
                }
            });
            listeners.put(listener, l);

            l.start();
            log.debug("Route synchronization complete");
        }
    }

    @Override
    public void removeListener(EvpnRouteListener listener) {
        synchronized (this) {
            EvpnListenerQueue l = listeners.remove(listener);
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

    private void post(EvpnRouteEvent event) {
        if (event != null) {
            log.debug("Sending event {}", event);
            synchronized (this) {
                listeners.values().forEach(l -> l.post(event));
            }
        }
    }


    @Override
    public Collection<EvpnRouteTableId> getRouteTables() {
        return evpnRouteStore.getRouteTables();
    }

    @Override
    public void update(Collection<EvpnRoute> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received update {}", route);
                evpnRouteStore.updateRoute(route);
            });
        }
    }

    @Override
    public void withdraw(Collection<EvpnRoute> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received withdraw {}", route);
                evpnRouteStore.removeRoute(route);
            });
        }
    }

    /**
     * Creates a new listener queue.
     *
     * @param listener route listener
     * @return listener queue
     */
    DefaultListenerQueue createListenerQueue(EvpnRouteListener listener) {
        return new DefaultListenerQueue(listener);
    }

    /**
     * Default route listener queue.
     */
    private class DefaultListenerQueue implements EvpnListenerQueue {

        private final ExecutorService executorService;
        private final BlockingQueue<EvpnRouteEvent> queue;
        private final EvpnRouteListener listener;

        /**
         * Creates a new listener queue.
         *
         * @param listener route listener to queue updates for
         */
        public DefaultListenerQueue(EvpnRouteListener listener) {
            this.listener = listener;
            queue = new LinkedBlockingQueue<>();
            executorService = newSingleThreadExecutor(threadFactory);
        }

        @Override
        public void post(EvpnRouteEvent event) {
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
    private class InternalEvpnRouteStoreDelegate implements
            EvpnRouteStoreDelegate {
        EvpnRouteSet routes;

        @Override
        public void notify(EvpnInternalRouteEvent event) {
            switch (event.type()) {
                case ROUTE_ADDED:
                    routes = event.subject();
                    if (routes != null) {
                        Collection<EvpnRoute> evpnRoutes = routes.routes();
                        for (EvpnRoute evpnRoute : evpnRoutes) {
                            post(new EvpnRouteEvent(
                                    EvpnRouteEvent.Type.ROUTE_ADDED,
                                    evpnRoute,
                                    routes.routes()));
                        }
                    }
                    break;
                case ROUTE_REMOVED:
                    routes = event.subject();
                    if (routes != null) {
                        Collection<EvpnRoute> evpnRoutes = routes.routes();
                        for (EvpnRoute evpnRoute : evpnRoutes) {
                            post(new EvpnRouteEvent(
                                    EvpnRouteEvent.Type.ROUTE_REMOVED,
                                    evpnRoute,
                                    routes.routes()));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
