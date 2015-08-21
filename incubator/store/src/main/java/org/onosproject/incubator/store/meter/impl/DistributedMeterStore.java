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
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterId;
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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private ConsistentMap<MeterId, MeterData> meters;
    private NodeId local;

    private MapEventListener mapListener = new InternalMapEventListener();

    private Map<MeterId, CompletableFuture<MeterStoreResult>> futures =
            Maps.newConcurrentMap();

    @Activate
    public void activate() {

        local = clusterService.getLocalNode().id();


        meters = storageService.<MeterId, MeterData>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                     MeterData.class,
                                                     DefaultMeter.class,
                                                     DefaultBand.class,
                                                     Band.Type.class,
                                                     MeterState.class,
                                                     Meter.Unit.class,
                                                     MeterFailReason.class,
                                                     MeterId.class)).build();

        meters.addListener(mapListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        meters.removeListener(mapListener);
        log.info("Stopped");
    }


    @Override
    public CompletableFuture<MeterStoreResult> storeMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        futures.put(meter.id(), future);
        MeterData data = new MeterData(meter, null, local);

        try {
            meters.put(meter.id(), data);
        } catch (StorageException e) {
            future.completeExceptionally(e);
        }

        return future;

    }

    @Override
    public CompletableFuture<MeterStoreResult> deleteMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        futures.put(meter.id(), future);

        MeterData data = new MeterData(meter, null, local);

        // update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        try {
            if (meters.computeIfPresent(meter.id(), (k, v) -> data) == null) {
                future.complete(MeterStoreResult.success());
            }
        } catch (StorageException e) {
            future.completeExceptionally(e);
        }


        return future;
    }

    @Override
    public CompletableFuture<MeterStoreResult> updateMeter(Meter meter) {
        CompletableFuture<MeterStoreResult> future = new CompletableFuture<>();
        futures.put(meter.id(), future);

        MeterData data = new MeterData(meter, null, local);
        try {
            if (meters.computeIfPresent(meter.id(), (k, v) -> data) == null) {
                future.complete(MeterStoreResult.fail(MeterFailReason.INVALID_METER));
            }
        } catch (StorageException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void updateMeterState(Meter meter) {
        meters.computeIfPresent(meter.id(), (id, v) -> {
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
    public Meter getMeter(MeterId meterId) {
        MeterData data = Versioned.valueOrElse(meters.get(meterId), null);
        return data == null ? null : data.meter();
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return Collections2.transform(meters.asJavaMap().values(),
                                      MeterData::meter);
    }

    @Override
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        meters.computeIfPresent(op.meter().id(), (k, v) ->
                new MeterData(v.meter(), reason, v.origin()));
    }

    @Override
    public void deleteMeterNow(Meter m) {
        futures.remove(m.id());
        meters.remove(m.id());
    }

    private class InternalMapEventListener implements MapEventListener<MeterId, MeterData> {
        @Override
        public void event(MapEvent<MeterId, MeterData> event) {
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
                                    futures.get(data.meter().id()).complete(msr);
                                }
                                break;
                            case ADDED:
                                if (local.equals(data.origin()) && data.meter().state() == MeterState.PENDING_ADD) {
                                    futures.remove(data.meter().id()).complete(MeterStoreResult.success());
                                }
                                break;
                            case REMOVED:
                                if (local.equals(data.origin()) && data.meter().state() == MeterState.PENDING_REMOVE) {
                                    futures.remove(data.meter().id()).complete(MeterStoreResult.success());
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
