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
package org.onosproject.store.mcast.impl;


import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastStore;
import org.onosproject.net.mcast.McastStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.mcast.McastRouteInfo.mcastRouteInfo;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed mcast store implementation. Routes are stored consistently
 * across the cluster.
 */
@Component(immediate = true, service = McastStore.class)
public class DistributedMcastStore
    extends AbstractStore<McastEvent, McastStoreDelegate>
    implements McastStore {

    private static final String MCASTRIB = "onos-mcast-rib-table";
    private Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private Map<McastRoute, MulticastData> mcastRoutes;
    private ConsistentMap<McastRoute, MulticastData> mcastRib;
    private MapEventListener<McastRoute, MulticastData> mcastRouteListener =
        new McastRouteListener();

    private ScheduledExecutorService executor;


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
        .build();

        mcastRoutes = mcastRib.asJavaMap();
        mcastRib.addListener(mcastRouteListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        mcastRib.removeListener(mcastRouteListener);
        log.info("Stopped");
    }


    private class McastRouteListener implements MapEventListener<McastRoute, MulticastData> {
        @Override
        public void event(MapEvent<McastRoute, MulticastData> event) {
            final McastRoute route = event.key();
            final MulticastData newData = Optional.ofNullable(event.newValue()).map(Versioned::value).orElse(null);
            final MulticastData oldData = Optional.ofNullable(event.oldValue()).map(Versioned::value).orElse(null);

            switch (event.type()) {
                case INSERT:
                    checkNotNull(newData);

                    if (newData.source() != null) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SOURCE_ADDED,
                                mcastRouteInfo(route,
                                        newData.sinks(),
                                        newData.source())));
                    } else if (!newData.sinks().isEmpty()) {
                        newData.sinks().forEach(sink ->
                            notifyDelegate(new McastEvent(McastEvent.Type.SINK_ADDED,
                                    mcastRouteInfo(route,
                                            sink,
                                            newData.source())))
                        );
                    } else {
                        notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_ADDED,
                                mcastRouteInfo(route)));
                    }
                    break;
                case UPDATE:
                    checkNotNull(newData);
                    checkNotNull(oldData);

                    // They are not equal
                    if (!Objects.equal(oldData.source(), newData.source())) {
                        // Both not null, it is an update event
                        if (oldData.source() != null && newData.source() != null) {
                            // Broadcast old and new data
                            notifyDelegate(new McastEvent(McastEvent.Type.SOURCE_UPDATED,
                                                          mcastRouteInfo(route,
                                                                         newData.sinks(),
                                                                         newData.source()),
                                                          mcastRouteInfo(route,
                                                                         oldData.sinks(),
                                                                         oldData.source())));
                        } else if (oldData.source() == null && newData.source() != null) {
                            // It is a source added event, broadcast new data
                            notifyDelegate(new McastEvent(McastEvent.Type.SOURCE_ADDED,
                                                          mcastRouteInfo(route,
                                                                         newData.sinks(),
                                                                         newData.source())));
                        } else {
                            // Scenario not managed for now
                            log.warn("Unhandled scenario {} - new {} - old {}", event.type());
                        }
                    } else {
                        Sets.difference(newData.sinks(), oldData.sinks()).forEach(sink ->
                            notifyDelegate(new McastEvent(McastEvent.Type.SINK_ADDED,
                                                          mcastRouteInfo(route,
                                                                         sink,
                                                                         newData.source())))
                        );

                        Sets.difference(oldData.sinks(), newData.sinks()).forEach(sink ->
                            notifyDelegate(new McastEvent(McastEvent.Type.SINK_REMOVED,
                                                          mcastRouteInfo(route,
                                                                         sink,
                                                                         newData.source())))
                        );
                    }
                    break;
                case REMOVE:
                    // Verify old data is not null
                    checkNotNull(oldData);
                    // Create a route removed event with just the route
                    // and the source connect point
                    notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_REMOVED,
                                                      mcastRouteInfo(route,
                                                                     oldData.sinks(),
                                                                     oldData.source()
                                                                     )));
                    break;
                default:
                    log.warn("Unknown mcast operation type: {}", event.type());
            }
        }
    }

    @Override
    public void storeRoute(McastRoute route, Type operation) {
        switch (operation) {
            case ADD:
                mcastRoutes.putIfAbsent(route, MulticastData.empty());
                break;
            case REMOVE:
                // before remove the route should check that source and sinks are removed?
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
}
