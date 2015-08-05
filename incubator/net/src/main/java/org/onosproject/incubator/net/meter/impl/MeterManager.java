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
import org.onosproject.incubator.net.meter.DefaultMeter;
import org.onosproject.incubator.net.meter.Meter;
import org.onosproject.incubator.net.meter.MeterEvent;
import org.onosproject.incubator.net.meter.MeterFailReason;
import org.onosproject.incubator.net.meter.MeterId;
import org.onosproject.incubator.net.meter.MeterListener;
import org.onosproject.incubator.net.meter.MeterOperation;
import org.onosproject.incubator.net.meter.MeterProvider;
import org.onosproject.incubator.net.meter.MeterProviderRegistry;
import org.onosproject.incubator.net.meter.MeterProviderService;
import org.onosproject.incubator.net.meter.MeterService;
import org.onosproject.incubator.net.meter.MeterState;
import org.onosproject.incubator.net.meter.MeterStore;
import org.onosproject.incubator.net.meter.MeterStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
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

    @Activate
    public void activate() {
        meterIdCounter = storageService.atomicCounterBuilder()
                .withName(meterIdentifier)
                .build();
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
    public void addMeter(Meter meter) {
        DefaultMeter m = (DefaultMeter) meter;
        m.setState(MeterState.PENDING_ADD);
        store.storeMeter(m);
    }

    @Override
    public void updateMeter(Meter meter) {
        DefaultMeter m = (DefaultMeter) meter;
        m.setState(MeterState.PENDING_ADD);
        store.updateMeter(m);
    }

    @Override
    public void removeMeter(Meter meter) {
        DefaultMeter m = (DefaultMeter) meter;
        m.setState(MeterState.PENDING_REMOVE);
        store.deleteMeter(m);
    }

    @Override
    public void removeMeter(MeterId id) {
        DefaultMeter meter = (DefaultMeter) store.getMeter(id);
        checkNotNull(meter, "No such meter {}", id);
        removeMeter(meter);
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
            DeviceId deviceId = event.subject().meter().deviceId();
            MeterProvider p = getProvider(event.subject().meter().deviceId());
            switch (event.type()) {
                case METER_UPDATED:
                    break;
                case METER_OP_FAILED:
                    event.subject().meter().context().ifPresent(c ->
                        c.onError(event.subject(), event.reason()));
                    break;
                case METER_OP_REQ:
                    p.performMeterOperation(deviceId, event.subject());
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }

        }
    }

}
