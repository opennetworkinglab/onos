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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMeterStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.store.service.StorageException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.onosproject.net.meter.MeterFailReason.TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual meter store for a single instance.
 */
//TODO: support distributed meter store for virtual networks
@Component(immediate = true, service = VirtualNetworkMeterStore.class)
public class SimpleVirtualMeterStore
        extends AbstractVirtualStore<MeterEvent, MeterStoreDelegate>
        implements VirtualNetworkMeterStore {

        private Logger log = getLogger(getClass());

        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        protected ClusterService clusterService;

        private ConcurrentMap<NetworkId, ConcurrentMap<MeterKey, MeterData>> meterMap =
                Maps.newConcurrentMap();

        private NodeId local;

        private ConcurrentMap<NetworkId, ConcurrentMap<MeterFeaturesKey, MeterFeatures>>
                meterFeatureMap = Maps.newConcurrentMap();

        private ConcurrentMap<NetworkId,
                ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>>> futuresMap =
                Maps.newConcurrentMap();

        @Activate
        public void activate() {
            log.info("Started");
            local = clusterService.getLocalNode().id();
        }

        @Deactivate
        public void deactivate() {
            log.info("Stopped");
        }

        private ConcurrentMap<MeterKey, MeterData> getMetersByNetwork(NetworkId networkId) {
            meterMap.computeIfAbsent(networkId, m -> new ConcurrentHashMap<>());
            return meterMap.get(networkId);
        }

        private ConcurrentMap<MeterFeaturesKey, MeterFeatures>
        getMeterFeaturesByNetwork(NetworkId networkId) {
            meterFeatureMap.computeIfAbsent(networkId, f -> new ConcurrentHashMap<>());
            return meterFeatureMap.get(networkId);
        }

        private ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>>
        getFuturesByNetwork(NetworkId networkId) {
            futuresMap.computeIfAbsent(networkId, f -> new ConcurrentHashMap<>());
            return futuresMap.get(networkId);
        }

        @Override
        public CompletableFuture<MeterStoreResult> storeMeter(NetworkId networkId, Meter meter) {

            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

            ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>> futures =
                   getFuturesByNetwork(networkId);

            CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
            MeterKey key = MeterKey.key(meter.deviceId(), meter.id());
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
        public CompletableFuture<MeterStoreResult> deleteMeter(NetworkId networkId, Meter meter) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

            ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>> futures =
                    getFuturesByNetwork(networkId);

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
            } catch (StorageException e) {
                    future.completeExceptionally(e);
            }

            return future;
        }

        @Override
        public MeterStoreResult storeMeterFeatures(NetworkId networkId, MeterFeatures meterfeatures) {
            ConcurrentMap<MeterFeaturesKey, MeterFeatures> meterFeatures
                    = getMeterFeaturesByNetwork(networkId);

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
        public MeterStoreResult deleteMeterFeatures(NetworkId networkId, DeviceId deviceId) {
            ConcurrentMap<MeterFeaturesKey, MeterFeatures> meterFeatures
                    = getMeterFeaturesByNetwork(networkId);

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
        public CompletableFuture<MeterStoreResult> updateMeter(NetworkId networkId, Meter meter) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);
            ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>> futures =
                    getFuturesByNetwork(networkId);

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
        public void updateMeterState(NetworkId networkId, Meter meter) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

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
        public Meter getMeter(NetworkId networkId, MeterKey key) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

            MeterData data = meters.get(key);
            return data == null ? null : data.meter();
        }

        @Override
        public Collection<Meter> getAllMeters(NetworkId networkId) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

            return Collections2.transform(meters.values(), MeterData::meter);
        }

        @Override
        public void failedMeter(NetworkId networkId, MeterOperation op, MeterFailReason reason) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);

            MeterKey key = MeterKey.key(op.meter().deviceId(), op.meter().id());
            meters.computeIfPresent(key, (k, v) ->
                    new MeterData(v.meter(), reason, v.origin()));
        }

        @Override
        public void deleteMeterNow(NetworkId networkId, Meter m) {
            ConcurrentMap<MeterKey, MeterData> meters = getMetersByNetwork(networkId);
            ConcurrentMap<MeterKey, CompletableFuture<MeterStoreResult>> futures =
                    getFuturesByNetwork(networkId);

            MeterKey key = MeterKey.key(m.deviceId(), m.id());
            futures.remove(key);
            meters.remove(key);
        }

        @Override
        public long getMaxMeters(NetworkId networkId, MeterFeaturesKey key) {
            ConcurrentMap<MeterFeaturesKey, MeterFeatures> meterFeatures
                    = getMeterFeaturesByNetwork(networkId);

            MeterFeatures features = meterFeatures.get(key);
            return features == null ? 0L : features.maxMeter();
        }
}
