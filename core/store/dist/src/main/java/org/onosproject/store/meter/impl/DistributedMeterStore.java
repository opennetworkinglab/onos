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
package org.onosproject.store.meter.impl;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.math.RandomUtils;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.MeterQuery;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesFlag;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.primitives.DefaultDistributedSet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounterMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.onosproject.store.meter.impl.DistributedMeterStore.ReuseStrategy.FIRST_FIT;
import static org.onosproject.net.meter.MeterFailReason.TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed meter store implementation. Meters are stored consistently
 * across the cluster.
 */
@Component(immediate = true, service = MeterStore.class)
public class DistributedMeterStore extends AbstractStore<MeterEvent, MeterStoreDelegate>
                    implements MeterStore {

    private Logger log = getLogger(getClass());

    private static final String METERSTORE = "onos-meter-store";
    private static final String METERFEATURESSTORE = "onos-meter-features-store";
    private static final String AVAILABLEMETERIDSTORE = "onos-meters-available-store";
    private static final String METERIDSTORE = "onos-meters-id-store";

    private static final KryoNamespace.Builder APP_KRYO_BUILDER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(MeterKey.class)
            .register(MeterData.class)
            .register(DefaultMeter.class)
            .register(DefaultBand.class)
            .register(Band.Type.class)
            .register(MeterState.class)
            .register(Meter.Unit.class)
            .register(MeterFailReason.class);

    private Serializer serializer = Serializer.using(Lists.newArrayList(APP_KRYO_BUILDER.build()));

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    private ConsistentMap<MeterKey, MeterData> meters;
    private NodeId local;

    private ConsistentMap<MeterFeaturesKey, MeterFeatures> meterFeatures;

    private MapEventListener<MeterKey, MeterData> mapListener = new InternalMapEventListener();

    private Map<MeterKey, CompletableFuture<MeterStoreResult>> futures =
            Maps.newConcurrentMap();

    // Available meter identifiers
    private DistributedSet<MeterKey> availableMeterIds;

    // Atomic counter map for generation of new identifiers;
    private AtomicCounterMap<DeviceId> meterIdGenerators;

    /**
     * Defines possible selection strategies to reuse meter ids.
     */
    enum ReuseStrategy {
        /**
         * Select randomly an available id.
         */
        RANDOM,
        /**
         * Select the first one.
         */
        FIRST_FIT
    }

    private ReuseStrategy reuseStrategy = FIRST_FIT;

    @Activate
    public void activate() {
        local = clusterService.getLocalNode().id();

        meters = storageService.<MeterKey, MeterData>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(serializer).build();

        meters.addListener(mapListener);

        meterFeatures = storageService.<MeterFeaturesKey, MeterFeatures>consistentMapBuilder()
                .withName(METERFEATURESSTORE)
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 MeterFeaturesKey.class,
                                                 MeterFeatures.class,
                                                 DefaultMeterFeatures.class,
                                                 Band.Type.class,
                                                 Meter.Unit.class,
                                                 MeterFailReason.class,
                                                 MeterFeaturesFlag.class)).build();

        // Init the set of the available ids
        availableMeterIds = new DefaultDistributedSet<>(storageService.<MeterKey>setBuilder()
                .withName(AVAILABLEMETERIDSTORE)
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 MeterKey.class)).build(),
                DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS);

        // Init atomic map counters
        meterIdGenerators = storageService.<DeviceId>atomicCounterMapBuilder()
                .withName(METERIDSTORE)
                .withSerializer(Serializer.using(KryoNamespaces.API)).build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        meters.removeListener(mapListener);
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<MeterStoreResult> storeMeter(Meter meter) {
        // Init steps
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        // Store the future related to the operation
        futures.put(key, future);
        // Store the meter data
        MeterData data = new MeterData(meter, null, local);
        try {
            meters.put(key, data);
        } catch (StorageException e) {
            futures.remove(key);
            future.completeExceptionally(e);
        }
        // Done, return the future
        return future;
    }

    @Override
    public CompletableFuture<MeterStoreResult> deleteMeter(Meter meter) {
        // Init steps
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        // Store the future related to the operation
        futures.put(key, future);
        // Create the meter data
        MeterData data = new MeterData(meter, null, local);
        // Update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        try {
            // If it does not exist in the system
            if (meters.computeIfPresent(key, (k, v) -> data) == null) {
                // Complete immediately
                future.complete(MeterStoreResult.success());
            }
        } catch (StorageException e) {
            futures.remove(key);
            future.completeExceptionally(e);
        }
        // Done, return the future
        return future;
    }

    @Override
    public MeterStoreResult storeMeterFeatures(MeterFeatures meterfeatures) {
        MeterStoreResult result = MeterStoreResult.success();
        MeterFeaturesKey key = MeterFeaturesKey.key(meterfeatures.deviceId());
        try {
            meterFeatures.putIfAbsent(key, meterfeatures);
        } catch (StorageException e) {
            result = MeterStoreResult.fail(TIMEOUT);
        }
        return result;
    }

    @Override
    public MeterStoreResult deleteMeterFeatures(DeviceId deviceId) {
        MeterStoreResult result = MeterStoreResult.success();
        MeterFeaturesKey key = MeterFeaturesKey.key(deviceId);
        try {
            meterFeatures.remove(key);
        } catch (StorageException e) {
            result = MeterStoreResult.fail(TIMEOUT);
        }
        return result;
    }

    @Override
    public CompletableFuture<MeterStoreResult> updateMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        futures.put(key, future);

        MeterData data = new MeterData(meter, null, local);
        try {
            if (meters.computeIfPresent(key, (k, v) -> data) == null) {
                future.complete(MeterStoreResult.fail(MeterFailReason.INVALID_METER));
            }
        } catch (StorageException e) {
            futures.remove(key);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void updateMeterState(Meter meter) {
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        meters.computeIfPresent(key, (k, v) -> {
            DefaultMeter m = (DefaultMeter) v.meter();
            MeterState meterState = m.state();
            if (meterState == MeterState.PENDING_ADD) {
                m.setState(meter.state());
            }
            m.setProcessedPackets(meter.packetsSeen());
            m.setProcessedBytes(meter.bytesSeen());
            m.setLife(meter.life());
            // TODO: Prune if drops to zero.
            m.setReferenceCount(meter.referenceCount());
            if (meter.referenceCount() == 0) {
                notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REFERENCE_COUNT_ZERO, m));
            }
            return new MeterData(m, null, v.origin());
        });
    }

    @Override
    public Meter getMeter(MeterKey key) {
        MeterData data = Versioned.valueOrElse(meters.get(key), null);
        return data == null ? null : data.meter();
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return Collections2.transform(meters.asJavaMap().values(),
                                      MeterData::meter);
    }

    @Override
    public Collection<Meter> getAllMeters(DeviceId deviceId) {
        return Collections2.transform(
                Collections2.filter(meters.asJavaMap().values(),
                        (MeterData m) -> m.meter().deviceId().equals(deviceId)),
                MeterData::meter);
    }

    @Override
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        MeterKey key = MeterKey.key(op.meter().deviceId(), op.meter().id());
        meters.computeIfPresent(key, (k, v) ->
                new MeterData(v.meter(), reason, v.origin()));
    }

    @Override
    public void deleteMeterNow(Meter m) {
        // Create the key
        MeterKey key = MeterKey.key(m.deviceId(), m.id());
        // Remove the future
        futures.remove(key);
        // Remove the meter
        if (Versioned.valueOrNull(meters.remove(key)) != null) {
            // Free the id
            freeMeterId(m.deviceId(), m.id());
            // Finally notify the delegate
            notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REMOVED, m));
        }
    }

    @Override
    public void purgeMeter(DeviceId deviceId) {

        List<Versioned<MeterData>> metersPendingRemove = meters.stream()
                .filter(e -> Objects.equals(e.getKey().deviceId(), deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        metersPendingRemove.forEach(versionedMeterKey
                -> deleteMeterNow(versionedMeterKey.value().meter()));

    }

    @Override
    public long getMaxMeters(MeterFeaturesKey key) {
        MeterFeatures features = Versioned.valueOrElse(meterFeatures.get(key), null);
        return features == null ? 0L : features.maxMeter();
    }

    // queryMaxMeters is implemented in FullMetersAvailable behaviour.
    private long queryMaxMeters(DeviceId device) {
        // Get driver handler for this device
        DriverHandler handler = driverService.createHandler(device);
        // If creation failed or the device does not have this behavior
        if (handler == null || !handler.hasBehaviour(MeterQuery.class)) {
            // We cannot know max meter
            return 0L;
        }
        // Get the behavior
        MeterQuery query = handler.behaviour(MeterQuery.class);
        // Return as max meter the result of the query
        return query.getMaxMeters();
    }

    private boolean updateMeterIdAvailability(DeviceId deviceId, MeterId id,
                                              boolean available) {
        // According to available, make available or unavailable a meter key
        return available ? availableMeterIds.add(MeterKey.key(deviceId, id)) :
                availableMeterIds.remove(MeterKey.key(deviceId, id));
    }

    private MeterId getNextAvailableId(Set<MeterId> availableIds) {
        // If there are no available ids
        if (availableIds.isEmpty()) {
            // Just end the cycle
            return null;
        }
        // If it is the first fit
        if (reuseStrategy == FIRST_FIT || availableIds.size() == 1) {
            return availableIds.iterator().next();
        }
        // If it is random, get the size
        int size = availableIds.size();
        // Return a random element
        return Iterables.get(availableIds, RandomUtils.nextInt(size));
    }

    // Implements reuse strategy
    private MeterId firstReusableMeterId(DeviceId deviceId) {
        // Filter key related to device id, and reduce to meter ids
        Set<MeterId> localAvailableMeterIds = availableMeterIds.stream()
                .filter(meterKey -> meterKey.deviceId().equals(deviceId))
                .map(MeterKey::meterId)
                .collect(Collectors.toSet());
        // Get next available id
        MeterId meterId = getNextAvailableId(localAvailableMeterIds);
        // Iterate until there are items
        while (meterId != null) {
            // If we are able to reserve the id
            if (updateMeterIdAvailability(deviceId, meterId, false)) {
                // Just end
                return meterId;
            }
            // Update the set
            localAvailableMeterIds.remove(meterId);
            // Try another time
            meterId = getNextAvailableId(localAvailableMeterIds);
        }
        // No reusable ids
        return null;
    }

    @Override
    public MeterId allocateMeterId(DeviceId deviceId) {
        // Init steps
        MeterId meterId;
        long id;
        // Try to reuse meter id
        meterId = firstReusableMeterId(deviceId);
        // We found a reusable id, return
        if (meterId != null) {
            return meterId;
        }
        // If there was no reusable MeterId we have to generate a new value
        // using maxMeters as upper limit.
        long maxMeters = getMaxMeters(MeterFeaturesKey.key(deviceId));
        // If the device does not give us MeterFeatures
        if (maxMeters == 0L) {
            // MeterFeatures couldn't be retrieved, fallback to queryMeters.
            maxMeters = queryMaxMeters(deviceId);
        }
        // If we don't know the max, cannot proceed
        if (maxMeters == 0L) {
            return null;
        }
        // Get a new value
        id = meterIdGenerators.incrementAndGet(deviceId);
        // Check with the max, and if the value is bigger, cannot proceed
        if (id >= maxMeters) {
            return null;
        }
        // Done, return the value
        return MeterId.meterId(id);
    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {
        // Avoid to free meter not allocated
        if (meterIdGenerators.get(deviceId) < meterId.id()) {
            return;
        }
        // Update the availability
        updateMeterIdAvailability(deviceId, meterId, true);
    }

    private class InternalMapEventListener implements MapEventListener<MeterKey, MeterData> {
        @Override
        public void event(MapEvent<MeterKey, MeterData> event) {
            MeterKey key = event.key();
            Versioned<MeterData> value = event.type() == MapEvent.Type.REMOVE ? event.oldValue() : event.newValue();
            MeterData data = value.value();
            NodeId master = mastershipService.getMasterFor(data.meter().deviceId());
            switch (event.type()) {
                case INSERT:
                case UPDATE:
                        switch (data.meter().state()) {
                            case PENDING_ADD:
                            case PENDING_REMOVE:
                                if (!data.reason().isPresent() && local.equals(master)) {
                                    notifyDelegate(
                                            new MeterEvent(data.meter().state() == MeterState.PENDING_ADD ?
                                                    MeterEvent.Type.METER_ADD_REQ : MeterEvent.Type.METER_REM_REQ,
                                                                  data.meter()));
                                } else if (data.reason().isPresent() && local.equals(data.origin())) {
                                    MeterStoreResult msr = MeterStoreResult.fail(data.reason().get());
                                    //TODO: No future -> no friend
                                    futures.get(key).complete(msr);
                                }
                                break;
                            case ADDED:
                                if (local.equals(data.origin()) &&
                                        (data.meter().state() == MeterState.PENDING_ADD
                                                || data.meter().state() == MeterState.ADDED)) {
                                    futures.computeIfPresent(key, (k, v) -> {
                                        v.complete(MeterStoreResult.success());
                                        notifyDelegate(
                                                new MeterEvent(MeterEvent.Type.METER_ADDED, data.meter()));
                                        return null;
                                    });
                                }
                                break;
                            case REMOVED:
                                if (local.equals(data.origin()) && data.meter().state() == MeterState.PENDING_REMOVE) {
                                    futures.remove(key).complete(MeterStoreResult.success());
                                }
                                break;
                            default:
                                log.warn("Unknown meter state type {}", data.meter().state());
                        }
                    break;
                case REMOVE:
                    //Only happens at origin so we do not need to care.
                    break;
                default:
                    log.warn("Unknown Map event type {}", event.type());
            }

        }
    }


}
