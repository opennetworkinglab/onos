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


import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.mcast.api.McastEvent;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.mcast.api.McastStore;
import org.onosproject.mcast.api.McastStoreDelegate;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.mcast.api.McastRouteUpdate.mcastRouteUpdate;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * New distributed mcast route store implementation. Routes are stored consistently
 * across the cluster.
 */
@Component(immediate = true, service = McastStore.class)
public class DistributedMcastRoutesStore
        extends AbstractStore<McastEvent, McastStoreDelegate>
        implements McastStore {

    private static final String MCASTRIB = "onos-mcast-route-table";
    private Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private Map<McastRoute, McastRouteData> mcastRoutes;
    private ConsistentMap<McastRoute, McastRouteData> mcastRib;
    private MapEventListener<McastRoute, McastRouteData> mcastRouteListener =
            new McastRouteListener();

    @Activate
    public void activate() {
        mcastRib = storageService.<McastRoute, McastRouteData>consistentMapBuilder()
                .withName(MCASTRIB)
                .withSerializer(Serializer.using(KryoNamespace.newBuilder()
                        .register(KryoNamespaces.API)
                        .register(
                                McastRoute.class,
                                AtomicReference.class,
                                McastRouteData.class,
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

    @Override
    public void storeRoute(McastRoute route) {
        mcastRoutes.putIfAbsent(route, McastRouteData.empty());
    }

    @Override
    public void removeRoute(McastRoute route) {
        mcastRoutes.remove(route);
    }


    @Override
    public void storeSource(McastRoute route, HostId hostId, Set<ConnectPoint> connectPoints) {
        mcastRoutes.compute(route, (k, v) -> {
            v.addSources(hostId, connectPoints);
            return v;
        });
    }

    @Override
    public void storeSources(McastRoute route, Set<ConnectPoint> sources) {
        mcastRoutes.compute(route, (k, v) -> {
            v.addSources(HostId.NONE, sources);
            return v;
        });
    }


    @Override
    public void removeSources(McastRoute route) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSources();
            // Since we have cleared the sources, we should remove the route
            return null;
        });
    }

    @Override
    public void removeSource(McastRoute route, HostId source) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSource(source);
            // Since there are no sources, we should remove the route
            return v.sources().isEmpty() ? null : v;
        });
    }

    @Override
    public void removeSources(McastRoute route, Set<ConnectPoint> sources) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSources(HostId.NONE, sources);
            return v;
        });
    }

    @Override
    public void removeSources(McastRoute route, HostId hostId, Set<ConnectPoint> sources) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSources(hostId, sources);
            return v;
        });
    }

    @Override
    public void addSink(McastRoute route, HostId hostId, Set<ConnectPoint> sinks) {
        mcastRoutes.compute(route, (k, v) -> {
            v.addSinks(hostId, sinks);
            return v;
        });
    }

    @Override
    public void addSinks(McastRoute route, Set<ConnectPoint> sinks) {
        mcastRoutes.compute(route, (k, v) -> {
            v.addSinks(HostId.NONE, sinks);
            return v;
        });
    }


    @Override
    public void removeSinks(McastRoute route) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSinks();
            return v;
        });
    }

    @Override
    public void removeSink(McastRoute route, HostId hostId) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSinks(hostId);
            return v;
        });
    }

    @Override
    public void removeSinks(McastRoute route, HostId hostId, Set<ConnectPoint> sinks) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSinks(hostId, sinks);
            return v;
        });
    }

    @Override
    public void removeSinks(McastRoute route, Set<ConnectPoint> sinks) {
        mcastRoutes.compute(route, (k, v) -> {
            v.removeSinks(HostId.NONE, sinks);
            return v;
        });
    }

    @Override
    public Set<ConnectPoint> sourcesFor(McastRoute route) {
        McastRouteData data = mcastRoutes.getOrDefault(route, null);
        return data == null ? ImmutableSet.of() : ImmutableSet.copyOf(data.sources().values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    @Override
    public Set<ConnectPoint> sourcesFor(McastRoute route, HostId hostId) {
        McastRouteData data = mcastRoutes.getOrDefault(route, null);
        return data == null ? ImmutableSet.of() : ImmutableSet.copyOf(data.sources(hostId));
    }

    @Override
    public Set<ConnectPoint> sinksFor(McastRoute route) {
        McastRouteData data = mcastRoutes.getOrDefault(route, null);
        return data == null ? ImmutableSet.of() : ImmutableSet.copyOf(data.sinks().values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    @Override
    public Set<ConnectPoint> sinksFor(McastRoute route, HostId hostId) {
        McastRouteData data = mcastRoutes.getOrDefault(route, null);
        return data == null ? ImmutableSet.of() : ImmutableSet.copyOf(data.sinks(hostId));
    }

    @Override
    public Set<McastRoute> getRoutes() {
        return ImmutableSet.copyOf(mcastRoutes.keySet());
    }

    @Override
    public McastRouteData getRouteData(McastRoute route) {
        return mcastRoutes.get(route);
    }

    private class McastRouteListener implements MapEventListener<McastRoute, McastRouteData> {
        @Override
        public void event(MapEvent<McastRoute, McastRouteData> event) {
            final McastRoute route = event.key();
            final McastRouteData newData =
                    Optional.ofNullable(event.newValue()).map(Versioned::value).orElse(null);
            final McastRouteData oldData =
                    Optional.ofNullable(event.oldValue()).map(Versioned::value).orElse(null);

            switch (event.type()) {
                case INSERT:
                    checkNotNull(newData);
                    notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_ADDED, null,
                            mcastRouteUpdate(route, newData.sources(), newData.sinks())));
                    break;
                case UPDATE:
                    checkNotNull(newData);
                    checkNotNull(oldData);

                    if (newData.allSources().size() > oldData.allSources().size()) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SOURCES_ADDED,
                                mcastRouteUpdate(route, oldData.sources(), oldData.sinks()),
                                mcastRouteUpdate(route, newData.sources(), newData.sinks())));
                    } else if (newData.allSources().size() < oldData.allSources().size()) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SOURCES_REMOVED,
                                mcastRouteUpdate(route, oldData.sources(), oldData.sinks()),
                                mcastRouteUpdate(route, newData.sources(), newData.sinks())));
                    }
                    if (newData.allSinks().size() > oldData.allSinks().size()) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SINKS_ADDED,
                                mcastRouteUpdate(route, oldData.sources(), oldData.sinks()),
                                mcastRouteUpdate(route, newData.sources(), newData.sinks())));
                    } else if (newData.allSinks().size() < oldData.allSinks().size()) {
                        notifyDelegate(new McastEvent(McastEvent.Type.SINKS_REMOVED,
                                mcastRouteUpdate(route, oldData.sources(), oldData.sinks()),
                                mcastRouteUpdate(route, newData.sources(), newData.sinks())));
                    }
                    break;
                case REMOVE:
                    // Verify old data is not null
                    checkNotNull(oldData);
                    // Create a route removed event with just the route
                    // and the source connect point
                    notifyDelegate(new McastEvent(McastEvent.Type.ROUTE_REMOVED,
                            mcastRouteUpdate(route, oldData.sources(), oldData.sinks()),
                            null));
                    break;
                default:
                    log.warn("Unknown mcast operation type: {}", event.type());
            }
        }
    }
}
