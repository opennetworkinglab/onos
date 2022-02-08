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
import org.onosproject.core.ApplicationId;
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.store.meter.impl.DistributedMeterStore.ReuseStrategy.FIRST_FIT;
import static org.onosproject.net.meter.MeterFailReason.TIMEOUT;
import static org.onosproject.net.meter.MeterCellId.MeterCellType.INDEX;
import static org.onosproject.net.meter.MeterCellId.MeterCellType.PIPELINE_INDEPENDENT;
import static org.onosproject.net.meter.MeterStoreResult.Type.FAIL;
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
    protected final ConcurrentMap<MeterTableKey, DistributedSet<MeterKey>> availableMeterIds =
            new ConcurrentHashMap<>();
    private static final String METERIDSTORE = "onos-meters-id-store";
    private AtomicCounterMap<MeterTableKey> meterIdGenerators;

    private static final KryoNamespace.Builder APP_KRYO_BUILDER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(MeterKey.class)
            .register(MeterData.class)
            .register(DefaultMeter.class)
            .register(DefaultBand.class)
            .register(Band.Type.class)
            .register(MeterState.class)
            .register(Meter.Unit.class)
            .register(MeterFailReason.class)
            .register(MeterTableKey.class)
            .register(MeterFeatures.class)
            .register(DefaultMeterFeatures.class)
            .register(MeterFeaturesFlag.class)
            .register(MeterScope.class);
    private Serializer serializer = Serializer.using(Lists.newArrayList(APP_KRYO_BUILDER.build()));

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    // Local cache to handle async ops through futures.
    private Map<MeterKey, CompletableFuture<MeterStoreResult>> futures =
            Maps.newConcurrentMap();

    // Control the user defined index mode for the store.
    protected boolean userDefinedIndexMode = false;

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
        meters = storageService.<MeterKey, MeterData>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(serializer).build();
        meters.addListener(metersMapListener);
        metersMap = meters.asJavaMap();

        metersFeatures = storageService.<MeterTableKey, MeterFeatures>eventuallyConsistentMapBuilder()
                .withName(METERFEATURESSTORE)
                .withTimestampProvider((key, features) -> new WallClockTimestamp())
                .withSerializer(APP_KRYO_BUILDER).build();
        metersFeatures.addListener(featuresMapListener);

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

        // EC map does clean only the local state
        metersFeatures.destroy();
        // Distributed set does not override the default behavior
        availableMeterIds.forEach((key, set) -> set.destroy());

        log.info("Stopped");
    }

    @Override
    public CompletableFuture<MeterStoreResult> addOrUpdateMeter(Meter meter) {
        checkArgument(validIndex(meter), "Meter index is not valid");
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.meterCellId());
        MeterData data = new MeterData(meter, null);
        futures.put(key, future);
        try {
            meters.compute(key, (k, v) -> data);
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                    e.getMessage(), e);
            futures.remove(key);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<MeterStoreResult> deleteMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.meterCellId());
        futures.put(key, future);
        // Update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        try {
            Versioned<MeterData> versionedData = meters.computeIfPresent(key, (k, v) -> {
                DefaultMeter m = (DefaultMeter) v.meter();
                MeterState meterState = m.state();
                if (meterState == MeterState.PENDING_REMOVE) {
                    return v;
                }
                m.setState(meter.state());
                return new MeterData(m, v.reason().isPresent() ? v.reason().get() : null);
            });
            // If it does not exist in the system, completes immediately
            if (versionedData == null) {
                futures.remove(key);
                future.complete(MeterStoreResult.success());
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
    public MeterStoreResult storeMeterFeatures(Collection<MeterFeatures> meterfeatures) {
        // These store operations is treated as one single operation
        // If one of them is failed, Fail is returned
        // But the failed operation will not block the rest.
        MeterStoreResult result = MeterStoreResult.success();
        for (MeterFeatures mf : meterfeatures) {
            if (storeMeterFeatures(mf).type() == FAIL) {
                result = MeterStoreResult.fail(TIMEOUT);
            }
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
            keys.forEach(k -> metersFeatures.remove(k));
        } catch (StorageException e) {
            log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                        e.getMessage(), e);
            result = MeterStoreResult.fail(TIMEOUT);
        }
        return result;
    }

    @Override
    public MeterStoreResult deleteMeterFeatures(Collection<MeterFeatures> meterfeatures) {
        // Same logic of storeMeterFeatures
        MeterStoreResult result = MeterStoreResult.success();
        for (MeterFeatures mf : meterfeatures) {
            try {
                MeterTableKey key = MeterTableKey.key(mf.deviceId(), mf.scope());
                metersFeatures.remove(key);
            } catch (StorageException e) {
                log.error("{} thrown a storage exception: {}", e.getStackTrace()[0].getMethodName(),
                        e.getMessage(), e);
                result = MeterStoreResult.fail(TIMEOUT);
            }
        }

        return result;
    }

    @Override
    public Meter updateMeterState(Meter meter) {
        // Update meter if present (stats workflow)
        MeterKey key = MeterKey.key(meter.deviceId(), meter.meterCellId());
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
    public Collection<Meter> getAllMeters(DeviceId deviceId, MeterScope scope) {
        if (scope.equals(MeterScope.globalScope())) {
            return Collections2.transform(
                    Collections2.filter(ImmutableSet.copyOf(metersMap.values()),
                            (MeterData m) -> m.meter().meterCellId().type() == INDEX &&
                                    m.meter().deviceId().equals(deviceId)),
                    MeterData::meter);
        }
        return Collections2.transform(
                Collections2.filter(ImmutableSet.copyOf(metersMap.values()),
                        (MeterData m) -> m.meter().meterCellId().type() == PIPELINE_INDEPENDENT &&
                                m.meter().deviceId().equals(deviceId) &&
                                ((PiMeterCellId) m.meter().meterCellId()).meterId().id().equals(scope.id())),
                MeterData::meter);
    }

    @Override
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        // Meter ops failed (got notification from the sb)
        MeterKey key = MeterKey.key(op.meter().deviceId(), op.meter().meterCellId());
        meters.computeIfPresent(key, (k, v) -> new MeterData(v.meter(), reason));
    }

    @Override
    public void purgeMeter(Meter m) {
        // Once we receive the ack from the sb, create the key
        // remove definitely the meter and free the id
        MeterKey key = MeterKey.key(m.deviceId(), m.meterCellId());
        try {
            if (Versioned.valueOrNull(meters.remove(key)) != null) {
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
    public void purgeMeters(DeviceId deviceId) {
        List<Versioned<MeterData>> metersPendingRemove = meters.stream()
                .filter(e -> Objects.equals(e.getKey().deviceId(), deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        metersPendingRemove.forEach(versionedMeterKey
                -> purgeMeter(versionedMeterKey.value().meter()));
    }

    @Override
    public void purgeMeters(DeviceId deviceId, ApplicationId appId) {
        List<Versioned<MeterData>> metersPendingRemove = meters.stream()
                .filter(e -> Objects.equals(e.getKey().deviceId(), deviceId) &&
                        e.getValue().value().meter().appId().equals(appId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        metersPendingRemove.forEach(versionedMeterKey
                -> purgeMeter(versionedMeterKey.value().meter()));
    }

    @Override
    public boolean userDefinedIndexMode(boolean enable) {
        if (meters.isEmpty() && meterIdGenerators.isEmpty()) {
            userDefinedIndexMode = enable;
        } else {
            log.warn("Unable to {} user defined index mode as store did" +
                    "already some allocations", enable ? "activate" : "deactivate");
        }
        return userDefinedIndexMode;
    }

    protected long getMaxMeters(MeterTableKey key) {
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? 0L : features.maxMeter();
    }

    // Validate index using the meter features, useful mainly
    // when user defined index mode is enabled
    private boolean validIndex(Meter meter) {
        long index;
        MeterTableKey key;

        if (meter.meterCellId().type() == PIPELINE_INDEPENDENT) {
            PiMeterCellId piMeterCellId = (PiMeterCellId) meter.meterCellId();
            index = piMeterCellId.index();
            key = MeterTableKey.key(meter.deviceId(), MeterScope.of(piMeterCellId.meterId().id()));
        } else if (meter.meterCellId().type() == INDEX) {
            MeterId meterId = (MeterId) meter.meterCellId();
            index = meterId.id();
            key = MeterTableKey.key(meter.deviceId(), MeterScope.globalScope());
        } else {
            log.warn("Unable to validate index unsupported cell type {}", meter.meterCellId().type());
            return false;
        }

        MeterFeatures features = metersFeatures.get(key);
        long startIndex = features == null ? -1L : features.startIndex();
        long endIndex = features == null ? -1L : features.endIndex();
        return index >= startIndex && index <= endIndex;
    }

    private long getStartIndex(MeterTableKey key) {
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? -1L : features.startIndex();
    }

    private long getEndIndex(MeterTableKey key) {
        MeterFeatures features = metersFeatures.get(key);
        return features == null ? -1L : features.endIndex();
    }

    // queryMaxMeters is implemented in MeterQuery behaviour implementations.
    private long queryMaxMeters(DeviceId device) {
        DriverHandler handler = driverService.createHandler(device);
        if (handler == null || !handler.hasBehaviour(MeterQuery.class)) {
            return 0L;
        }

        // FIXME architecturally this is not right, we should fallback to this
        //  behavior in the providers. Once we do that we can remove this code.
        MeterQuery query = handler.behaviour(MeterQuery.class);
        // This results to be necessary because the available ids sets are created
        // in the meter features map listener if the device does not provide the meter
        // feature this is the only chance to create this set.
        String setName = AVAILABLEMETERIDSTORE + "-" + device + "global";
        MeterTableKey meterTableKey = MeterTableKey.key(device, MeterScope.globalScope());
        insertAvailableKeySet(meterTableKey, setName);

        return query.getMaxMeters();
    }

    private boolean updateMeterIdAvailability(MeterTableKey meterTableKey, MeterCellId id,
                                              boolean available) {
        DistributedSet<MeterKey> keySet = availableMeterIds.get(meterTableKey);
        if (keySet == null) {
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
        if (availableIds.isEmpty()) {
            return null;
        }

        if (reuseStrategy == FIRST_FIT || availableIds.size() == 1) {
            return availableIds.iterator().next();
        }

        // If it is random, get the size and return a random element
        int size = availableIds.size();
        return Iterables.get(availableIds, RandomUtils.nextInt(size));
    }

    // Implements reuse strategy of the meter cell ids
    private MeterCellId firstReusableMeterId(MeterTableKey meterTableKey) {
        DistributedSet<MeterKey> keySet = availableMeterIds.get(meterTableKey);
        if (keySet == null) {
            log.warn("Reusable Key set for device: {} scope: {} not found",
                meterTableKey.deviceId(), meterTableKey.scope());
            return null;
        }

        Set<MeterCellId> localAvailableMeterIds = keySet.stream()
                .filter(meterKey ->
                    meterKey.deviceId().equals(meterTableKey.deviceId()))
                .map(MeterKey::meterCellId)
                .collect(Collectors.toSet());
        MeterCellId meterId = getNextAvailableId(localAvailableMeterIds);
        while (meterId != null) {
            if (updateMeterIdAvailability(meterTableKey, meterId, false)) {
                return meterId;
            }
            localAvailableMeterIds.remove(meterId);
            meterId = getNextAvailableId(localAvailableMeterIds);
        }
        // there are no available ids that can be reused
        return null;
    }

    @Override
    public MeterCellId allocateMeterId(DeviceId deviceId, MeterScope meterScope) {
        if (userDefinedIndexMode) {
            log.warn("Unable to allocate meter id when user defined index mode is enabled");
            return null;
        }
        MeterTableKey meterTableKey = MeterTableKey.key(deviceId, meterScope);
        MeterCellId meterCellId;
        long id;
        // First, search for reusable key
        meterCellId = firstReusableMeterId(meterTableKey);
        if (meterCellId != null) {
            return meterCellId;
        }
        // If there was no reusable meter id we have to generate a new value
        // using start and end index as lower and upper bound respectively.
        long startIndex = getStartIndex(meterTableKey);
        long endIndex = getEndIndex(meterTableKey);
        // If the device does not give us MeterFeatures fallback to queryMeters
        if (startIndex == -1L || endIndex == -1L) {
            // Only meaningful for OpenFlow today
            long maxMeters = queryMaxMeters(deviceId);
            if (maxMeters == 0L) {
                return null;
            } else {
                // OpenFlow meter index starts from 1, ends with max
                startIndex = 1L;
                endIndex = maxMeters;
            }
        }

        do {
            id = meterIdGenerators.getAndIncrement(meterTableKey);
        } while (id < startIndex);
        if (id > endIndex) {
            return null;
        }

        // For backward compatibility if we are using global scope,
        // return a MeterId, otherwise we create a PiMeterCellId
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

    protected void freeMeterId(DeviceId deviceId, MeterCellId meterCellId) {
        MeterTableKey meterTableKey;
        if (meterCellId.type() == PIPELINE_INDEPENDENT) {
            meterTableKey = MeterTableKey.key(deviceId,
                    MeterScope.of(((PiMeterCellId) meterCellId).meterId().id()));
        } else if (meterCellId.type() == INDEX) {
            meterTableKey = MeterTableKey.key(deviceId, MeterScope.globalScope());
        } else {
            log.warn("Unable to free meter id unsupported cell type {}", meterCellId.type());
            return;
        }
        freeMeterId(meterTableKey, meterCellId);
    }

    protected void freeMeterId(MeterTableKey meterTableKey, MeterCellId meterCellId) {
        if (userDefinedIndexMode) {
            log.debug("Unable to free meter id when user defined index mode is enabled");
            return;
        }
        long index;
        if (meterCellId.type() == PIPELINE_INDEPENDENT) {
            PiMeterCellId piMeterCellId = (PiMeterCellId) meterCellId;
            index = piMeterCellId.index();
        } else if (meterCellId.type() == INDEX) {
            MeterId meterId = (MeterId) meterCellId;
            index = meterId.id();
        } else {
            log.warn("Unable to free meter id unsupported cell type {}", meterCellId.type());
            return;
        }
        // Avoid to free meter not allocated
        if (meterIdGenerators.get(meterTableKey) <= index) {
            return;
        }
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
                                // Update stats case - we report reference count zero only for INDEX based meters
                                } else if (data.meter().referenceCount() == 0 &&
                                        data.meter().meterCellId().type() == INDEX) {
                                    notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REFERENCE_COUNT_ZERO,
                                            data.meter()));
                                }
                                break;
                            default:
                                log.warn("Unknown meter state type {}", data.meter().state());
                        }
                    break;
                case REMOVE:
                    futures.computeIfPresent(key, (k, v) -> {
                        v.complete(MeterStoreResult.success());
                        return null;
                    });
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
                    String setName = AVAILABLEMETERIDSTORE + "-" +
                        meterFeatures.deviceId() + meterFeatures.scope().id();
                    insertAvailableKeySet(meterTableKey, setName);
                    break;
                case REMOVE:
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
