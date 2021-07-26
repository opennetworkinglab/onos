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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.math.RandomUtils;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.MeterQuery;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesFlag;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.net.meter.MeterTableKey;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.primitives.DefaultDistributedSet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounterMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.onosproject.store.meter.impl.DistributedMeterStore.ReuseStrategy.FIRST_FIT;
import static org.onosproject.net.meter.MeterFailReason.TIMEOUT;
import static org.onosproject.net.meter.MeterCellId.MeterCellType.INDEX;
import static org.onosproject.net.meter.MeterCellId.MeterCellType.PIPELINE_INDEPENDENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed meter store implementation. Meters are stored consistently
 * across the cluster.
 */
@Component(immediate = true, service = MeterStore.class)
public class DistributedMeterStore extends AbstractStore<MeterEvent, MeterStoreDelegate>
                    implements MeterStore {

    private Logger log = getLogger(getClass());

    // Meters map related objects
    private static final String METERSTORE = "onos-meter-store";
    private ConsistentMap<MeterKey, MeterData> meters;
    private MapEventListener<MeterKey, MeterData> metersMapListener = new InternalMetersMapEventListener();
    private Map<MeterKey, MeterData> metersMap;

    // Meters features related objects
    private static final String METERFEATURESSTORE = "onos-meter-features-store";
    private EventuallyConsistentMap<MeterTableKey, MeterFeatures> metersFeatures;
    private EventuallyConsistentMapListener<MeterTableKey, MeterFeatures> featuresMapListener =
        new InternalFeaturesMapEventListener();

    // Meters id related objects
    private static final String AVAILABLEMETERIDSTORE = "onos-meters-available-store";
    // Available meter identifiers
    private ConcurrentMap<MeterTableKey, DistributedSet<MeterKey>> availableMeterIds;
    // Atomic counter map for generation of new identifiers;
    private static final String METERIDSTORE = "onos-meters-id-store";
    private AtomicCounterMap<MeterTableKey> meterIdGenerators;

    // Serializer related objects
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
    protected DriverService driverService;

    // Local cache to handle async ops through futures.
    private Map<MeterKey, CompletableFuture<MeterStoreResult>> futures =
            Maps.newConcurrentMap();

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
        // Init meters map and setup the map listener
        meters = storageService.<MeterKey, MeterData>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(serializer).build();
        meters.addListener(metersMapListener);
        metersMap = meters.asJavaMap();
        // Init meter features map
        metersFeatures = storageService.<MeterTableKey, MeterFeatures>eventuallyConsistentMapBuilder()
                .withName(METERFEATURESSTORE)
                .withTimestampProvider((key, features) -> new WallClockTimestamp())
                .withSerializer(KryoNamespace.newBuilder()
                                .register(KryoNamespaces.API)
                                .register(MeterTableKey.class)
                                .register(MeterFeatures.class)
                                .register(DefaultMeterFeatures.class)
                                .register(DefaultBand.class)
                                .register(Band.Type.class)
                                .register(Meter.Unit.class)
                                .register(MeterFailReason.class)
                                .register(MeterFeaturesFlag.class)).build();
        metersFeatures.addListener(featuresMapListener);
        // Init the map of the available ids set
        // Set will be created when a new Meter Features is pushed to the store
        availableMeterIds = new ConcurrentHashMap<>();
        // Init atomic map counters
        meterIdGenerators = storageService.<MeterTableKey>atomicCounterMapBuilder()
                .withName(METERIDSTORE)
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 MeterTableKey.class,
                                                 MeterScope.class)).build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        meters.removeListener(metersMapListener);
        metersFeatures.removeListener(featuresMapListener);
        meters.destroy();
        metersFeatures.destroy();
        availableMeterIds.forEach((key, set) -> {
            set.destroy();
        });
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
        MeterData data = new MeterData(meter, null);
        try {
            meters.put(key, data);
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
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
        MeterData data = new MeterData(meter, null);
        // Update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        try {
            // If it does not exist in the system
            if (meters.computeIfPresent(key, (k, v) -> data) == null) {
                // Complete immediately
                future.complete(MeterStoreResult.success());
            }
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
            futures.remove(key);
            future.completeExceptionally(e);
        }
        // Done, return the future
        return future;
    }

    @Override
    public MeterStoreResult storeMeterFeatures(MeterFeatures meterfeatures) {
        // Store meter features, this is done once for each features of every device
        MeterStoreResult result = MeterStoreResult.success();
        MeterTableKey key = MeterTableKey.key(meterfeatures.deviceId(), meterfeatures.scope());
        try {
            metersFeatures.put(key, meterfeatures);
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
            result = MeterStoreResult.fail(TIMEOUT);
        }
        return result;
    }

    @Override
    public MeterStoreResult deleteMeterFeatures(DeviceId deviceId) {
        MeterStoreResult result = MeterStoreResult.success();
        try {
            Set<MeterTableKey> keys = metersFeatures.keySet().stream()
                                        .filter(key -> key.deviceId().equals(deviceId))
                                        .collect(Collectors.toUnmodifiableSet());
            keys.forEach(k -> {
                metersFeatures.remove(k);
            });
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                        e.getMessage(), e);
            result = MeterStoreResult.fail(TIMEOUT);
        }

        return result;
    }

    @Override
    // TODO Should we remove it ? We are not using it
    public CompletableFuture<MeterStoreResult> updateMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        futures.put(key, future);

        MeterData data = new MeterData(meter, null);
        try {
            if (meters.computeIfPresent(key, (k, v) -> data) == null) {
                future.complete(MeterStoreResult.fail(MeterFailReason.INVALID_METER));
            }
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
            futures.remove(key);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public Meter updateMeterState(Meter meter) {
        // Update meter if present (stats workflow)
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        Versioned<MeterData> value = meters.computeIfPresent(key, (k, v) -> {
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
            return new MeterData(m, null);
        });
        return value != null ? value.value().meter() : null;
    }

    @Override
    public Meter getMeter(MeterKey key) {
        MeterData data = Versioned.valueOrElse(meters.get(key), null);
        return data == null ? null : data.meter();
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return Collections2.transform(ImmutableSet.copyOf(metersMap.values()),
                                      MeterData::meter);
    }

    @Override
    public Collection<Meter> getAllMeters(DeviceId deviceId) {
        return Collections2.transform(
                Collections2.filter(ImmutableSet.copyOf(metersMap.values()),
                        (MeterData m) -> m.meter().deviceId().equals(deviceId)),
                MeterData::meter);
    }

    @Override
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        // Meter ops failed (got notification from the sb)
        MeterKey key = MeterKey.key(op.meter().deviceId(), op.meter().id());
        meters.computeIfPresent(key, (k, v) -> new MeterData(v.meter(), reason));
    }

    @Override
    public void deleteMeterNow(Meter m) {
        // This method is renamed in onos-2.5
        purgeMeter(m);
    }

    @Override
    public void purgeMeter(Meter m) {
        // Once we receive the ack from the sb
        // create the key and remove definitely the meter
        MeterKey key = MeterKey.key(m.deviceId(), m.id());
        try {
            if (Versioned.valueOrNull(meters.remove(key)) != null) {
                // Free the id
                MeterScope scope;
                if (m.meterCellId().type() == PIPELINE_INDEPENDENT) {
                    PiMeterCellId piMeterCellId = (PiMeterCellId) m.meterCellId();
                    scope = MeterScope.of(piMeterCellId.meterId().id());
                } else {
                    scope = MeterScope.globalScope();
                }
                MeterTableKey meterTableKey = MeterTableKey.key(m.deviceId(), scope);
                freeMeterId(meterTableKey, m.meterCellId());
            }
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
        }
    }

    @Override
    public void purgeMeter(DeviceId deviceId) {
        // Purge api (typically used when the device is offline)
        List<Versioned<MeterData>> metersPendingRemove = meters.stream()
                .filter(e -> Objects.equals(e.getKey().deviceId(), deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        // Remove definitely the meter
        metersPendingRemove.forEach(versionedMeterKey
                -> purgeMeter(versionedMeterKey.value().meter()));
    }

    @Override
    public long getMaxMeters(MeterFeaturesKey key) {
        // Leverage the meter features to know the max id
        // Create a Meter Table key with FeaturesKey's device and global scope
        MeterTableKey meterTableKey = MeterTableKey.key(key.deviceId(), MeterScope.globalScope());
        return getMaxMeters(meterTableKey);
    }

    private long getMaxMeters(MeterTableKey key) {
        // Leverage the meter features to know the max id
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? 0L : features.maxMeter();
    }

    private long getStartIndex(MeterTableKey key) {
        // Leverage the meter features to know the start id
        // Since we are using index now
        // if there is no features related to the key
        // -1 is returned
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? -1L : features.startIndex();
    }

    private long getEndIndex(MeterTableKey key) {
        // Leverage the meter features to know the max id
        // Since we are using index now
        // if there is no features related to the key
        // -1 is returned
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? -1L : features.endIndex();
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
        // Insert a new available key set to the map
        String setName = AVAILABLEMETERIDSTORE + "-" + device + "global";
        MeterTableKey meterTableKey = MeterTableKey.key(device, MeterScope.globalScope());
        insertAvailableKeySet(meterTableKey, setName);
        // Return as max meter the result of the query
        return query.getMaxMeters();
    }

    private boolean updateMeterIdAvailability(MeterTableKey meterTableKey, MeterCellId id,
                                              boolean available) {
        // Retrieve the set first
        DistributedSet<MeterKey> keySet = availableMeterIds.get(meterTableKey);
        if (keySet == null) {
            // A reusable set should be inserted when a features is pushed
            log.warn("Reusable Key set for device: {} scope: {} not found",
                meterTableKey.deviceId(), meterTableKey.scope());
            return false;
        }
        // According to available, make available or unavailable a meter key
        DeviceId deviceId = meterTableKey.deviceId();
        return available ? keySet.add(MeterKey.key(deviceId, id)) :
                keySet.remove(MeterKey.key(deviceId, id));
    }

    private MeterCellId getNextAvailableId(Set<MeterCellId> availableIds) {
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
    private MeterCellId firstReusableMeterId(MeterTableKey meterTableKey) {
        // Create a Table key and use it to retrieve the reusable meterCellId set
        DistributedSet<MeterKey> keySet = availableMeterIds.get(meterTableKey);
        if (keySet == null) {
            // A reusable set should be inserted when a features is pushed
            log.warn("Reusable Key set for device: {} scope: {} not found",
                meterTableKey.deviceId(), meterTableKey.scope());
            return null;
        }
        // Filter key related to device id, and reduce to meter ids
        Set<MeterCellId> localAvailableMeterIds = keySet.stream()
                .filter(meterKey ->
                    meterKey.deviceId().equals(meterTableKey.deviceId()))
                .map(MeterKey::meterId)
                .collect(Collectors.toSet());
        // Get next available id
        MeterCellId meterId = getNextAvailableId(localAvailableMeterIds);
        // Iterate until there are items
        while (meterId != null) {
            // If we are able to reserve the id
            if (updateMeterIdAvailability(meterTableKey, meterId, false)) {
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
        // We use global scope for MeterId
        return (MeterId) allocateMeterId(deviceId, MeterScope.globalScope());
    }

    @Override
    public MeterCellId allocateMeterId(DeviceId deviceId, MeterScope meterScope) {
        MeterTableKey meterTableKey = MeterTableKey.key(deviceId, meterScope);
        MeterCellId meterCellId;
        long id;
        // First, search for reusable key
        meterCellId = firstReusableMeterId(meterTableKey);
        if (meterCellId != null) {
            // A reusable key is found
            return meterCellId;
        }
        // If there was no reusable meter id we have to generate a new value
        // using start and end index as lower and upper bound respectively.
        long startIndex = getStartIndex(meterTableKey);
        long endIndex = getEndIndex(meterTableKey);
        // If the device does not give us MeterFeatures
        if (startIndex == -1L || endIndex == -1L) {
            // MeterFeatures couldn't be retrieved, fallback to queryMeters.
            // Only meaningful to OpenFLow
            long maxMeters = queryMaxMeters(deviceId);
            // If we don't know the max, cannot proceed
            if (maxMeters == 0L) {
                return null;
            } else {
                // OpenFlow meter index starts from 1, ends with max-1
                startIndex = 1L;
                endIndex = maxMeters - 1;
            }
        }
        // Get a new value
        // If the value is smaller than the start index, get another one
        do {
            id = meterIdGenerators.incrementAndGet(meterTableKey);
        } while (id < startIndex);
        // Check with the end index, and if the value is bigger, cannot proceed
        if (id > endIndex) {
            return null;
        }
        // Done, return the value
        // If we are using global scope, return a MeterId
        // Else, return a PiMeterId
        if (meterScope.isGlobal()) {
            return MeterId.meterId(id);
        } else {
            return PiMeterCellId.ofIndirect(PiMeterId.of(meterScope.id()), id);
        }

    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {
        MeterTableKey meterTableKey = MeterTableKey.key(deviceId, MeterScope.globalScope());
        freeMeterId(meterTableKey, meterId);
    }

    private void freeMeterId(MeterTableKey meterTableKey, MeterCellId meterCellId) {
        long index;
        if (meterCellId.type() == PIPELINE_INDEPENDENT) {
            PiMeterCellId piMeterCellId = (PiMeterCellId) meterCellId;
            index = piMeterCellId.index();
        } else if (meterCellId.type() == INDEX) {
            MeterId meterId = (MeterId) meterCellId;
            index = meterId.id();
        } else {
            return;
        }
        // Avoid to free meter not allocated
        if (meterIdGenerators.get(meterTableKey) < index) {
            return;
        }
        // Update the availability
        updateMeterIdAvailability(meterTableKey, meterCellId, true);
    }

    // Enabling the events distribution across the cluster
    private class InternalMetersMapEventListener implements MapEventListener<MeterKey, MeterData> {
        @Override
        public void event(MapEvent<MeterKey, MeterData> event) {
            MeterKey key = event.key();
            Versioned<MeterData> value = event.type() == MapEvent.Type.REMOVE ? event.oldValue() : event.newValue();
            MeterData data = value.value();
            MeterData oldData = Versioned.valueOrNull(event.oldValue());
            switch (event.type()) {
                case INSERT:
                case UPDATE:
                        switch (data.meter().state()) {
                            case PENDING_ADD:
                            case PENDING_REMOVE:
                                // Two cases. If there is a reason, the meter operation failed.
                                // Otherwise, we are ready to install/remove through the delegate.
                                if (data.reason().isEmpty()) {
                                    notifyDelegate(new MeterEvent(data.meter().state() == MeterState.PENDING_ADD ?
                                        MeterEvent.Type.METER_ADD_REQ : MeterEvent.Type.METER_REM_REQ, data.meter()));
                                } else {
                                    futures.computeIfPresent(key, (k, v) -> {
                                        v.complete(MeterStoreResult.fail(data.reason().get()));
                                        return null;
                                    });
                                }
                                break;
                            case ADDED:
                                // Transition from pending to installed
                                if (data.meter().state() == MeterState.ADDED &&
                                        (oldData != null && oldData.meter().state() == MeterState.PENDING_ADD)) {
                                    futures.computeIfPresent(key, (k, v) -> {
                                        v.complete(MeterStoreResult.success());
                                        return null;
                                    });
                                    notifyDelegate(new MeterEvent(MeterEvent.Type.METER_ADDED, data.meter()));
                                // Update stats case
                                } else if (data.meter().referenceCount() == 0) {
                                    notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REFERENCE_COUNT_ZERO,
                                            data.meter()));
                                }
                                break;
                            default:
                                log.warn("Unknown meter state type {}", data.meter().state());
                        }
                    break;
                case REMOVE:
                    // Meter removal case
                    futures.computeIfPresent(key, (k, v) -> {
                        v.complete(MeterStoreResult.success());
                        return null;
                    });
                    // Finally notify the delegate
                    notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REMOVED, data.meter()));
                    break;
                default:
                    log.warn("Unknown Map event type {}", event.type());
            }
        }
    }

    private class InternalFeaturesMapEventListener implements
        EventuallyConsistentMapListener<MeterTableKey, MeterFeatures> {
        @Override
        public void event(EventuallyConsistentMapEvent<MeterTableKey, MeterFeatures> event) {
            MeterTableKey meterTableKey = event.key();
            MeterFeatures meterFeatures = event.value();
            switch (event.type()) {
                case PUT:
                    // Put a new available meter id set to the map
                    String setName = AVAILABLEMETERIDSTORE + "-" +
                        meterFeatures.deviceId() + meterFeatures.scope().id();
                    insertAvailableKeySet(meterTableKey, setName);
                    break;
                case REMOVE:
                    // Remove the set
                    DistributedSet<MeterKey> set = availableMeterIds.remove(meterTableKey);
                    if (set != null) {
                        set.destroy();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void insertAvailableKeySet(MeterTableKey meterTableKey, String setName) {
        DistributedSet<MeterKey> availableMeterIdSet =
            new DefaultDistributedSet<>(storageService.<MeterKey>setBuilder()
                .withName(setName)
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                MeterKey.class)).build(),
                DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS);
        availableMeterIds.put(meterTableKey, availableMeterIdSet);
    }
}
