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
package org.onosproject.incubator.net.meter.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.TriConsumer;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterListener;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provides implementation of the meter service APIs.
 */
@Component(immediate = true)
@Service
public class MeterManager extends AbstractListenerProviderRegistry<MeterEvent, MeterListener,
        MeterProvider, MeterProviderService>
        implements MeterService, MeterProviderRegistry {

    private final String meterIdentifier = "meter-id-counter";
    private final Logger log = getLogger(getClass());
    private final MeterStoreDelegate delegate = new InternalMeterStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    MeterStore store;

    private AtomicCounter meterIdCounter;

    private TriConsumer<MeterOperation, MeterStoreResult, Throwable> onComplete;

    @Activate
    public void activate() {
        meterIdCounter = storageService.atomicCounterBuilder()
                .withName(meterIdentifier)
                .build();

        onComplete = (op, result, error) ->
            {
                op.context().ifPresent(c -> {
                    if (error != null) {
                        c.onError(op.meter(), MeterFailReason.UNKNOWN);
                    } else {
                        if (result.reason().isPresent()) {
                            c.onError(op.meter(), result.reason().get());
                        } else {
                            c.onSuccess(op.meter());
                        }
                    }
                });

            };
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    protected MeterProviderService createProviderService(MeterProvider provider) {
        return new InternalMeterProviderService(provider);
    }

    @Override
    public void addMeter(MeterOperation op) {
        DefaultMeter m = (DefaultMeter) op.meter();
        m.setState(MeterState.PENDING_ADD);
        store.storeMeter(m).whenComplete((result, error) ->
                                                 onComplete.accept(op, result, error));
    }

    @Override
    public void updateMeter(MeterOperation op) {
        DefaultMeter m = (DefaultMeter) op.meter();
        m.setState(MeterState.PENDING_ADD);
        store.updateMeter(m).whenComplete((result, error) ->
                                                  onComplete.accept(op, result, error));
    }

    @Override
    public void removeMeter(MeterOperation op) {
        DefaultMeter m = (DefaultMeter) op.meter();
        m.setState(MeterState.PENDING_REMOVE);
        store.deleteMeter(m).whenComplete((result, error) ->
                                                  onComplete.accept(op, result, error));
    }

    @Override
    public Meter getMeter(MeterId id) {
        return store.getMeter(id);
    }

    @Override
    public MeterId allocateMeterId() {
        // FIXME: This will break one day.
        return MeterId.meterId((int) meterIdCounter.getAndIncrement());
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
            meterEntries.forEach(m -> store.updateMeterState(m));
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
                                                                         MeterOperation.Type.ADD,
                                                                         null));
                    break;
                case METER_REM_REQ:
                    p.performMeterOperation(deviceId, new MeterOperation(event.subject(),
                                                                         MeterOperation.Type.REMOVE,
                                                                         null));
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }

        }
    }

}
