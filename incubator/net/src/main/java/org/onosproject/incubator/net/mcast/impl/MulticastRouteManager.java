/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.net.mcast.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastStore;
import org.onosproject.net.mcast.McastStoreDelegate;
import org.onosproject.net.mcast.MulticastRouteService;
import org.slf4j.Logger;

import java.util.Set;

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

    @Activate
    public void activate() {
        eventDispatcher.addSink(McastEvent.class, listenerRegistry);
        store.setDelegate(delegate);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(McastEvent.class);
        log.info("Stopped");
    }

    @Override
    public void add(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        store.storeRoute(route, McastStore.Type.ADD);
    }

    @Override
    public void remove(McastRoute route) {
        checkNotNull(route, "Route cannot be null");
        store.storeRoute(route, McastStore.Type.REMOVE);
    }

    @Override
    public Set<McastRoute> getRoutes() {
        return store.getRoutes();
    }

    @Override
    public void addSource(McastRoute route, ConnectPoint connectPoint) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(connectPoint, "Source cannot be null");
        store.storeSource(route, connectPoint);
    }

    @Override
    public void addSink(McastRoute route, ConnectPoint connectPoint) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(connectPoint, "Sink cannot be null");
        store.storeSink(route, connectPoint, McastStore.Type.ADD);

    }

    @Override
    public void removeSink(McastRoute route, ConnectPoint connectPoint) {

        checkNotNull(route, "Route cannot be null");
        checkNotNull(connectPoint, "Sink cannot be null");

        store.storeSink(route, connectPoint, McastStore.Type.REMOVE);
    }

    @Override
    public ConnectPoint fetchSource(McastRoute route) {
        return store.sourceFor(route);
    }

    @Override
    public Set<ConnectPoint> fetchSinks(McastRoute route) {
        return store.sinksFor(route);
    }

    private class InternalMcastStoreDelegate implements McastStoreDelegate {
        @Override
        public void notify(McastEvent event) {
            post(event);
        }
    }
}
