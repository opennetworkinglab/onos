/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.TriConsumer;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMeterStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterListener;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class VirtualNetworkMeterManager
        extends AbstractVirtualListenerManager<MeterEvent, MeterListener>
        implements MeterService {

    private static final String METERCOUNTERIDENTIFIER = "meter-id-counter-%s";
    private final Logger log = getLogger(getClass());

    protected StorageService coreStorageService;

    protected VirtualNetworkMeterStore store;
    private final MeterStoreDelegate storeDelegate = new InternalMeterStoreDelegate();

    private VirtualProviderRegistryService providerRegistryService;
    private InternalMeterProviderService innerProviderService;

    private Map<DeviceId, AtomicCounter> meterIdCounters
            = Maps.newConcurrentMap();

    private TriConsumer<MeterRequest, MeterStoreResult, Throwable> onComplete;

    /**
     * Creates a new VirtualNetworkMeterManager object.
     *
     * @param manager virtual network manager service
     * @param networkId a virtual network identifier
     */
    public VirtualNetworkMeterManager(VirtualNetworkService manager,
                                      NetworkId networkId) {
        super(manager, networkId, MeterEvent.class);

        coreStorageService = serviceDirectory.get(StorageService.class);
        providerRegistryService =
                serviceDirectory.get(VirtualProviderRegistryService.class);

        store = serviceDirectory.get(VirtualNetworkMeterStore.class);
        store.setDelegate(networkId, this.storeDelegate);

        innerProviderService = new InternalMeterProviderService();
        providerRegistryService.registerProviderService(networkId(), innerProviderService);


        onComplete = (request, result, error) -> {
            request.context().ifPresent(c -> {
                if (error != null) {
                    c.onError(request, MeterFailReason.UNKNOWN);
                } else {
                    if (result.reason().isPresent()) {
                        c.onError(request, result.reason().get());
                    } else {
                        c.onSuccess(request);
                    }
                }
            });

        };

        log.info("Started");
    }

    @Override
    public Meter submit(MeterRequest request) {

        MeterId id = allocateMeterId(request.deviceId());

        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withId(id)
                .withUnit(request.unit());

        if (request.isBurst()) {
            mBuilder.burst();
        }
        DefaultMeter m = (DefaultMeter) mBuilder.build();
        m.setState(MeterState.PENDING_ADD);
        store.storeMeter(networkId(), m).whenComplete((result, error) ->
                                                 onComplete.accept(request, result, error));
        return m;
    }

    @Override
    public void withdraw(MeterRequest request, MeterId meterId) {
        withdraw(request, (MeterCellId) meterId);
    }

    @Override
    public void withdraw(MeterRequest request, MeterCellId meterCellId) {
        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withCellId(meterCellId)
                .withUnit(request.unit());

        if (request.isBurst()) {
            mBuilder.burst();
        }

        DefaultMeter m = (DefaultMeter) mBuilder.build();
        m.setState(MeterState.PENDING_REMOVE);
        store.deleteMeter(networkId(), m).whenComplete((result, error) ->
                                                  onComplete.accept(request, result, error));
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterId id) {
        return getMeter(deviceId, (MeterCellId) id);
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterCellId id) {
        MeterKey key = MeterKey.key(deviceId, id);
        return store.getMeter(networkId(), key);
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId) {
        return store.getAllMeters(networkId()).stream()
                .filter(m -> m.deviceId().equals(deviceId)).collect(Collectors.toList());
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId, MeterScope scope) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return store.getAllMeters(networkId());
    }

    private long queryMeters(DeviceId device) {
        //FIXME: how to decide maximum number of meters per virtual device?
        return 1;
    }

    private AtomicCounter allocateCounter(DeviceId deviceId) {
        return coreStorageService
                .getAtomicCounter(String.format(METERCOUNTERIDENTIFIER, deviceId));
    }

    public MeterId allocateMeterId(DeviceId deviceId) {
        long maxMeters = store.getMaxMeters(networkId(), MeterFeaturesKey.key(deviceId));
        if (maxMeters == 0L) {
            // MeterFeatures couldn't be retrieved, trying with queryMeters
            maxMeters = queryMeters(deviceId);
        }

        if (maxMeters == 0L) {
            throw new IllegalStateException("Meters not supported by device " + deviceId);
        }

        final long mmeters = maxMeters;
        long id = meterIdCounters.compute(deviceId, (k, v) -> {
            if (v == null) {
                return allocateCounter(k);
            }
            if (v.get() >= mmeters) {
                throw new IllegalStateException("Maximum number of meters " +
                                                        meterIdCounters.get(deviceId).get() +
                                                        " reached for device " + deviceId +
                                                        " virtual network " + networkId());
            }
            return v;
        }).incrementAndGet();

        return MeterId.meterId(id);
    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {
        // Do nothing
    }

    @Override
    public void purgeMeters(DeviceId deviceId) {
        // Do nothing
    }

    private class InternalMeterProviderService
            extends AbstractVirtualProviderService<VirtualMeterProvider>
            implements VirtualMeterProviderService {

        /**
         * Creates a provider service on behalf of the specified provider.
         */
        protected InternalMeterProviderService() {
            Set<ProviderId> providerIds =
                    providerRegistryService.getProvidersByService(this);
            ProviderId providerId = providerIds.stream().findFirst().get();
            VirtualMeterProvider provider = (VirtualMeterProvider)
                    providerRegistryService.getProvider(providerId);
            setProvider(provider);
        }

        @Override
        public void meterOperationFailed(MeterOperation operation,
                                         MeterFailReason reason) {
            store.failedMeter(networkId(), operation, reason);
        }

        @Override
        public void pushMeterMetrics(DeviceId deviceId, Collection<Meter> meterEntries) {
            //FIXME: FOLLOWING CODE CANNOT BE TESTED UNTIL SOMETHING THAT
            //FIXME: IMPLEMENTS METERS EXISTS
            Map<Pair<DeviceId, MeterId>, Meter> storedMeterMap =
                    store.getAllMeters(networkId()).stream()
                    .collect(Collectors.toMap(m -> Pair.of(m.deviceId(), m.id()), Function.identity()));

            meterEntries.stream()
                    .filter(m -> storedMeterMap.remove(Pair.of(m.deviceId(), m.id())) != null)
                    .forEach(m -> store.updateMeterState(networkId(), m));

            storedMeterMap.values().forEach(m -> {
                if (m.state() == MeterState.PENDING_ADD) {
                    provider().performMeterOperation(networkId(), m.deviceId(),
                                                     new MeterOperation(m,
                                                                        MeterOperation.Type.MODIFY));
                } else if (m.state() == MeterState.PENDING_REMOVE) {
                    store.deleteMeterNow(networkId(), m);
                }
            });
        }

        @Override
        public void pushMeterFeatures(DeviceId deviceId, MeterFeatures meterfeatures) {
            store.storeMeterFeatures(networkId(), meterfeatures);
        }

        @Override
        public void deleteMeterFeatures(DeviceId deviceId) {
            store.deleteMeterFeatures(networkId(), deviceId);
        }
    }

    private class InternalMeterStoreDelegate implements MeterStoreDelegate {

        @Override
        public void notify(MeterEvent event) {
            DeviceId deviceId = event.subject().deviceId();
            VirtualMeterProvider p = innerProviderService.provider();

            switch (event.type()) {
                case METER_ADD_REQ:
                    p.performMeterOperation(networkId(), deviceId,
                                            new MeterOperation(event.subject(),
                                                               MeterOperation.Type.ADD));
                    break;
                case METER_REM_REQ:
                    p.performMeterOperation(networkId(), deviceId,
                                            new MeterOperation(event.subject(),
                                                               MeterOperation.Type.REMOVE));
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }
        }
    }
}
