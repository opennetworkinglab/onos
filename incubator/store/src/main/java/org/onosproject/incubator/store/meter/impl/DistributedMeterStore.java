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
package org.onosproject.incubator.store.meter.impl;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.net.meter.MeterFailReason.TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed meter store implementation. Meters are stored consistently
 * across the cluster.
 */
@Component(immediate = true)
@Service
public class DistributedMeterStore extends AbstractStore<MeterEvent, MeterStoreDelegate>
                    implements MeterStore {

    private Logger log = getLogger(getClass());

    private static final String METERSTORE = "onos-meter-store";
    private static final String METERFEATURESSTORE = "onos-meter-features-store";
    private static final String AVAILABLEMETERIDSTORE = "onos-meters-available-store";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private ConsistentMap<MeterKey, MeterData> meters;
    private NodeId local;

    private ConsistentMap<MeterFeaturesKey, MeterFeatures> meterFeatures;

    private MapEventListener<MeterKey, MeterData> mapListener = new InternalMapEventListener();

    private Map<MeterKey, CompletableFuture<MeterStoreResult>> futures =
            Maps.newConcurrentMap();

    private ConsistentMap<DeviceId, BitSet> availableMeterIds;

    @Activate
    public void activate() {
        local = clusterService.getLocalNode().id();

        meters = storageService.<MeterKey, MeterData>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                     MeterKey.class,
                                                     MeterData.class,
                                                     DefaultMeter.class,
                                                     DefaultBand.class,
                                                     Band.Type.class,
                                                     MeterState.class,
                                                     Meter.Unit.class,
                                                     MeterFailReason.class)).build();

        meters.addListener(mapListener);

        meterFeatures = storageService.<MeterFeaturesKey, MeterFeatures>consistentMapBuilder()
                .withName(METERFEATURESSTORE)
                .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                        MeterFeaturesKey.class,
                        MeterFeatures.class,
                        DefaultMeterFeatures.class,
                        Band.Type.class,
                        Meter.Unit.class,
                        MeterFailReason.class)).build();

        availableMeterIds = storageService.<DeviceId, BitSet>consistentMapBuilder()
                .withName(AVAILABLEMETERIDSTORE)
                .withSerializer(Serializer.using(KryoNamespaces.API)).build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        meters.removeListener(mapListener);
        log.info("Stopped");
    }

    private void updateMeterIdAvailability(DeviceId deviceId, MeterId id,
                                           boolean available) {
        availableMeterIds.compute(deviceId, (k, v) -> {
            v = v == null ? new BitSet() : v;
            v.set(id.id().intValue(), available);
            return v;
        });
    }

    @Override
    public MeterId firstReusableMeterId(DeviceId deviceId) {
        Versioned<BitSet> bitSetVersioned = availableMeterIds.get(deviceId);
        if (bitSetVersioned == null) {
            return null;
        }
        BitSet value = bitSetVersioned.value();
        int nextSetBit = value.nextSetBit(1);
        if (nextSetBit < 0) {
            return null;
        }
        return MeterId.meterId(nextSetBit);
    }

    @Override
    public CompletableFuture<MeterStoreResult> storeMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        updateMeterIdAvailability(meter.deviceId(), meter.id(), false);
        futures.put(key, future);
        MeterData data = new MeterData(meter, null, local);

        try {
            meters.put(key, data);
        } catch (StorageException e) {
            future.completeExceptionally(e);
        }

        return future;

    }

    @Override
    public CompletableFuture<MeterStoreResult> deleteMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        futures.put(key, future);

        MeterData data = new MeterData(meter, null, local);

        // update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        try {
            if (meters.computeIfPresent(key, (k, v) -> data) == null) {
                future.complete(MeterStoreResult.success());
            }
            updateMeterIdAvailability(meter.deviceId(), meter.id(), true);
        } catch (StorageException e) {
            future.completeExceptionally(e);
        }


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
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void updateMeterState(Meter meter) {
        MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
        meters.computeIfPresent(key, (k, v) -> {
            DefaultMeter m = (DefaultMeter) v.meter();
            m.setState(meter.state());
            m.setProcessedPackets(meter.packetsSeen());
            m.setProcessedBytes(meter.bytesSeen());
            m.setLife(meter.life());
            // TODO: Prune if drops to zero.
            m.setReferenceCount(meter.referenceCount());
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
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        MeterKey key = MeterKey.key(op.meter().deviceId(), op.meter().id());
        meters.computeIfPresent(key, (k, v) ->
                new MeterData(v.meter(), reason, v.origin()));
    }

    @Override
    public void deleteMeterNow(Meter m) {
        MeterKey key = MeterKey.key(m.deviceId(), m.id());
        futures.remove(key);
        meters.remove(key);
        notifyDelegate(new MeterEvent(MeterEvent.Type.METER_REMOVED, m));
    }

    @Override
    public long getMaxMeters(MeterFeaturesKey key) {
        MeterFeatures features = Versioned.valueOrElse(meterFeatures.get(key), null);
        return features == null ? 0L : features.maxMeter();
    }

    private class InternalMapEventListener implements MapEventListener<MeterKey, MeterData> {
        @Override
        public void event(MapEvent<MeterKey, MeterData> event) {
            MeterKey key = event.key();
            MeterData data = event.value().value();
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
