/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final String MCASTRIB = "mcast-rib-table";

    private Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;


    protected ApplicationId appId;
    protected ConsistentMap<McastRoute, MulticastData> mcastRoutes;

    @Activate
    public void activate() {

        eventDispatcher.addSink(McastEvent.class, listenerRegistry);

        appId = coreService.registerApplication("org.onosproject.mcastrib");

        mcastRoutes = storageService.<McastRoute, MulticastData>consistentMapBuilder()
                .withApplicationId(appId)
                .withName(MCASTRIB)
                .withSerializer(Serializer.using(KryoNamespace.newBuilder().register(
                        MulticastData.class,
                        McastRoute.class,
                        McastRoute.Type.class,
                        IpPrefix.class,
                        List.class,
                        ConnectPoint.class
                ).build())).build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void add(McastRoute route) {
        mcastRoutes.put(route, MulticastData.empty());
        post(new McastEvent(McastEvent.Type.ROUTE_ADDED, route, null, null));
    }

    @Override
    public void remove(McastRoute route) {
        mcastRoutes.remove(route);
        post(new McastEvent(McastEvent.Type.ROUTE_REMOVED, route, null, null));
    }

    @Override
    public void addSource(McastRoute route, ConnectPoint connectPoint) {
        Versioned<MulticastData> d = mcastRoutes.compute(route, (k, v) -> {
            if (v.isEmpty()) {
                return new MulticastData(connectPoint);
            } else {
                log.warn("Route {} is already in use.", route);
                return v;
            }
        });

        if (d != null) {
            post(new McastEvent(McastEvent.Type.SOURCE_ADDED,
                                route, null, connectPoint));
        }
    }

    @Override
    public void addSink(McastRoute route, ConnectPoint connectPoint) {
        AtomicReference<ConnectPoint> source = new AtomicReference<>();
        mcastRoutes.compute(route, (k, v) -> {
            if (!v.isEmpty()) {
                v.appendSink(connectPoint);
                source.set(v.source());
            } else {
                log.warn("Route {} does not exist");
            }
            return v;
        });

        if (source.get() != null) {
            post(new McastEvent(McastEvent.Type.SINK_ADDED, route,
                                connectPoint, source.get()));
        }
    }


    @Override
    public void removeSink(McastRoute route, ConnectPoint connectPoint) {
        AtomicReference<ConnectPoint> source = new AtomicReference<>();
        mcastRoutes.compute(route, (k, v) -> {
            if (v.removeSink(connectPoint)) {
                source.set(v.source());
            }
            return v;
        });

        if (source.get() != null) {
            post(new McastEvent(McastEvent.Type.SINK_REMOVED, route,
                                connectPoint, source.get()));
        }
    }

    @Override
    public ConnectPoint fetchSource(McastRoute route) {
        MulticastData d = mcastRoutes.asJavaMap().getOrDefault(route,
                                                               MulticastData.empty());
        return d.source();
    }

    @Override
    public List<ConnectPoint> fetchSinks(McastRoute route) {
        MulticastData d = mcastRoutes.asJavaMap().getOrDefault(route,
                                                               MulticastData.empty());
        return d.sinks();
    }
}
