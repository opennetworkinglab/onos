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
package org.onosproject.incubator.net.meter.impl;

import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.TriConsumer;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterListener;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the meter service APIs.
 */
@Component(immediate = true, enabled = true)
@Service
public class MeterManager extends AbstractListenerProviderRegistry<MeterEvent, MeterListener,
        MeterProvider, MeterProviderService>
        implements MeterService, MeterProviderRegistry {

    private static final String METERCOUNTERIDENTIFIER = "meter-id-counter-%s";
    private final Logger log = getLogger(getClass());
    private final MeterStoreDelegate delegate = new InternalMeterStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterStore store;

    private Map<DeviceId, AtomicCounter> meterIdCounters
            = Maps.newConcurrentMap();

    private TriConsumer<MeterRequest, MeterStoreResult, Throwable> onComplete;

    @Activate
    public void activate() {

        store.setDelegate(delegate);

        onComplete = (request, result, error) ->
            {
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

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    protected MeterProviderService createProviderService(MeterProvider provider) {
        return new InternalMeterProviderService(provider);
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
        store.storeMeter(m).whenComplete((result, error) ->
                                                 onComplete.accept(request, result, error));
        return m;
    }

    @Override
    public void withdraw(MeterRequest request, MeterId meterId) {
        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withId(meterId)
                .withUnit(request.unit());

        if (request.isBurst()) {
            mBuilder.burst();
        }

        DefaultMeter m = (DefaultMeter) mBuilder.build();
        m.setState(MeterState.PENDING_REMOVE);
        store.deleteMeter(m).whenComplete((result, error) ->
                                                  onComplete.accept(request, result, error));
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterId id) {
        MeterKey key = MeterKey.key(deviceId, id);
        return store.getMeter(key);
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId) {
        return store.getAllMeters().stream().filter(m ->
                m.deviceId().equals(deviceId)).collect(Collectors.toList());
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return store.getAllMeters();
    }

    private MeterId allocateMeterId(DeviceId deviceId) {
        long id = meterIdCounters.compute(deviceId, (k, v) -> {
            if (v == null) {
                return allocateCounter(k);
            }
            return v;
        }).incrementAndGet();

        return MeterId.meterId((int) id);
    }

    private AtomicCounter allocateCounter(DeviceId deviceId) {
        return storageService.getAtomicCounter(String.format(METERCOUNTERIDENTIFIER, deviceId));
    }

    private class InternalMeterProviderService
            extends AbstractProviderService<MeterProvider>
            implements MeterProviderService {

        /**
         * Creates a provider service on behalf of the specified provider.
         *
         * @param provider provider to which this service is being issued
         */
        protected InternalMeterProviderService(MeterProvider provider) {
            super(provider);
        }

        @Override
        public void meterOperationFailed(MeterOperation operation,
                                         MeterFailReason reason) {
            store.failedMeter(operation, reason);
        }

        @Override
        public void pushMeterMetrics(DeviceId deviceId, Collection<Meter> meterEntries) {
            //FIXME: FOLLOWING CODE CANNOT BE TESTED UNTIL SOMETHING THAT
            //FIXME: IMPLEMENTS METERS EXISTS
            Map<Pair<DeviceId, MeterId>, Meter> storedMeterMap = store.getAllMeters().stream()
                    .collect(Collectors.toMap(m -> Pair.of(m.deviceId(), m.id()), Function.identity()));

            meterEntries.stream()
                    .filter(m -> storedMeterMap.remove(Pair.of(m.deviceId(), m.id())) != null)
                    .forEach(m -> store.updateMeterState(m));

            storedMeterMap.values().forEach(m -> {
                if (m.state() == MeterState.PENDING_ADD) {
                    provider().performMeterOperation(m.deviceId(),
                                                     new MeterOperation(m,
                                                                        MeterOperation.Type.MODIFY));
                } else if (m.state() == MeterState.PENDING_REMOVE) {
                    store.deleteMeterNow(m);
                }
            });
        }
    }

    private class InternalMeterStoreDelegate implements MeterStoreDelegate {

        @Override
        public void notify(MeterEvent event) {
            DeviceId deviceId = event.subject().deviceId();
            MeterProvider p = getProvider(event.subject().deviceId());
            switch (event.type()) {
                case METER_ADD_REQ:
                    p.performMeterOperation(deviceId, new MeterOperation(event.subject(),
                                                                         MeterOperation.Type.ADD));
                    break;
                case METER_REM_REQ:
                    p.performMeterOperation(deviceId, new MeterOperation(event.subject(),
                                                                         MeterOperation.Type.REMOVE));
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }

        }
    }

}
