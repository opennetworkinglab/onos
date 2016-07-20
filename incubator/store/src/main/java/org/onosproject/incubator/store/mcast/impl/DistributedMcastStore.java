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
package org.onosproject.incubator.store.mcast.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.mcast.McastStore;
import org.onosproject.net.mcast.McastStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed mcast store implementation. Routes are stored consistently
 * across the cluster.
 */
@Component(immediate = true)
@Service
public class DistributedMcastStore extends AbstractStore<McastEvent, McastStoreDelegate>
        implements McastStore {
    //FIXME the number of events that will potentially be generated here is
    // not sustainable, consider changing this to an eventually consistent
    // map and not emitting events but rather use a provider-like mechanism
    // to program the dataplane.

    private static final String MCASTRIB = "mcast-rib-table";
    private Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected ConsistentMap<McastRoute, MulticastData> mcastRib;
    protected Map<McastRoute, MulticastData> mcastRoutes;
    private final MapEventListener<McastRoute, MulticastData> mcastMapListener =
            new McastMapListener();

    // NOTE: MapEvent cannot provide correct old value of sink since MulticastData
    //       is a object reference. Use this localSink to track sink.
    private Map<McastRoute, Set<ConnectPoint>> localSink =
            new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        mcastRib = storageService.<McastRoute, MulticastData>consistentMapBuilder()
                .withName(MCASTRIB)
                .withSerializer(Serializer.using(KryoNamespace.newBuilder()
                                                         .register(KryoNamespaces.API)
                                                         .register(
                                                                 AtomicReference.class,
                                                                 MulticastData.class,
                                                                 McastRoute.class,
                                                                 McastRoute.Type.class
                                                         ).build()))
                //.withRelaxedReadConsistency()
                .build();
        mcastRib.addListener(mcastMapListener);
        mcastRoutes = mcastRib.asJavaMap();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void storeRoute(McastRoute route, Type operation) {
        switch (operation) {
            case ADD:
                mcastRoutes.putIfAbsent(route, MulticastData.empty());
                break;
            case REMOVE:
                mcastRoutes.remove(route);
                break;
            default:
                log.warn("Unknown mcast operation type: {}", operation);
        }
    }

    @Override
    public void storeSource(McastRoute route, ConnectPoint source) {
        MulticastData data = mcastRoutes.compute(route, (k, v) -> {
            if (v == null) {
                return new MulticastData(source);
            } else {
                v.setSource(source);
            }
            return v;
        });
    }

    @Override
    public void storeSink(McastRoute route, ConnectPoint sink, Type operation) {
        MulticastData data = mcastRoutes.compute(route, (k, v) -> {
            switch (operation) {
                case ADD:
                    if (v == null) {
                        v = MulticastData.empty();
                    }
                    v.appendSink(sink);
                    break;
                case REMOVE:
                    if (v != null) {
                        v.removeSink(sink);
                    }
                    break;
                default:
                    log.warn("Unknown mcast operation type: {}", operation);
            }
            return v;
        });
    }

    @Override
    public ConnectPoint sourceFor(McastRoute route) {
        return mcastRoutes.getOrDefault(route, MulticastData.empty()).source();
    }

    @Override
    public Set<ConnectPoint> sinksFor(McastRoute route) {
        return mcastRoutes.getOrDefault(route, MulticastData.empty()).sinks();
    }

    @Override
    public Set<McastRoute> getRoutes() {
        return mcastRoutes.keySet();
    }

    private class McastMapListener implements MapEventListener<McastRoute, MulticastData> {
        @Override
        public void event(MapEvent<McastRoute, MulticastData> event) {
            McastRoute route = event.key();
            MulticastData newValue, oldValue;

            switch (event.type()) {
                case INSERT:
                    checkState(event.newValue() != null, "Map insert event should have newValue");
                    newValue = event.newValue().value();
                    if (newValue.source() != null) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SOURCE_ADDED,
                                McastRouteInfo.mcastRouteInfo(route,
                                        newValue.sinks(), newValue.source())));
                    }
                    if (!newValue.sinks().isEmpty()) {
                        newValue.sinks().forEach(sink -> {
                            notifyDelegate(new McastEvent(McastEvent.Type.SINK_ADDED,
                                    McastRouteInfo.mcastRouteInfo(route,
                                            sink, newValue.source())));
                        });
                    }
                    if (newValue.source() == null && newValue.sinks().isEmpty()) {
                        notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_ADDED,
                                McastRouteInfo.mcastRouteInfo(route)));
                    }
                    localSink.put(route, newValue.sinks());
                    break;
                case REMOVE:
                    checkState(event.oldValue() != null, "Map remove event should have oldValue");
                    oldValue = event.oldValue().value();
                    notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_REMOVED,
                            McastRouteInfo.mcastRouteInfo(route)));
                    oldValue.sinks().forEach(sink -> {
                        notifyDelegate(new McastEvent(
                                McastEvent.Type.SINK_REMOVED,
                                McastRouteInfo.mcastRouteInfo(route, sink, oldValue.source())));
                    });
                    localSink.remove(route);
                    break;
                case UPDATE:
                    checkState(event.newValue() != null, "Map update event should have newValue");
                    checkState(event.oldValue() != null, "Map update event should have oldValue");
                    newValue = event.newValue().value();
                    oldValue = event.oldValue().value();
                    if (newValue.source() != null && oldValue.source() == null) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SOURCE_ADDED,
                                McastRouteInfo.mcastRouteInfo(route,
                                        newValue.sinks(), newValue.source())));
                    }
                    newValue.sinks().stream()
                            .filter(sink -> !localSink.get(route).contains(sink))
                            .forEach(addedSink -> {
                                notifyDelegate(new McastEvent(McastEvent.Type.SINK_ADDED,
                                        McastRouteInfo.mcastRouteInfo(route,
                                                addedSink, newValue.source())));
                            });
                    localSink.get(route).stream()
                            .filter(sink -> !newValue.sinks().contains(sink))
                            .forEach(removedSink -> {
                                notifyDelegate(new McastEvent(McastEvent.Type.SINK_REMOVED,
                                        McastRouteInfo.mcastRouteInfo(route,
                                                removedSink, newValue.source())));
                            });
                    localSink.put(route, newValue.sinks());
                    break;
                default:
                    break;
            }
        }
    }
}
