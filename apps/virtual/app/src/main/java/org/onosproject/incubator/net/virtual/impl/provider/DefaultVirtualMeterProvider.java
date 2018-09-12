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

package org.onosproject.incubator.net.virtual.impl.provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterListener;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider to handle meters for virtual networks.
 */
@Component(service = VirtualMeterProvider.class)
public class DefaultVirtualMeterProvider extends AbstractVirtualProvider
        implements VirtualMeterProvider {

    private final Logger log = getLogger(getClass());

    static final long TIMEOUT = 30;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualProviderRegistryService providerRegistryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MeterService meterService;

    private MeterListener internalMeterListener;
    private Cache<Long, VirtualMeterOperation> pendingOperations;
    private IdGenerator idGenerator;

    @Activate
    public void activate() {
        providerRegistryService.registerProvider(this);
        internalMeterListener = new InternalMeterListener();

        idGenerator = getIdGenerator();

        pendingOperations = CacheBuilder.newBuilder()
                .expireAfterWrite(TIMEOUT, TimeUnit.SECONDS)
                .removalListener(
                        (RemovalNotification<Long, VirtualMeterOperation>
                                 notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        NetworkId networkId = notification.getValue().networkId();
                        MeterOperation op = notification.getValue().operation();

                        VirtualMeterProviderService providerService =
                                (VirtualMeterProviderService) providerRegistryService
                                        .getProviderService(networkId,
                                                            VirtualMeterProvider.class);

                        providerService.meterOperationFailed(op,
                                                             MeterFailReason.TIMEOUT);
                    }
                }).build();

        meterService.addListener(internalMeterListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        meterService.removeListener(internalMeterListener);
        providerRegistryService.unregisterProvider(this);
    }

    /**
     * Creates a provider with the identifier.
     */
    public DefaultVirtualMeterProvider() {
        super(new ProviderId("vnet-meter",
                             "org.onosproject.virtual.vnet-meter"));
    }

    @Override
    public void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                                      MeterOperations meterOps) {
        meterOps.operations().forEach(op -> performOperation(networkId, deviceId, op));
    }

    @Override
    public void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                                      MeterOperation meterOp) {
        performOperation(networkId, deviceId, meterOp);
    }

    private void performOperation(NetworkId networkId, DeviceId deviceId,
                                  MeterOperation op) {

        VirtualMeterOperation vOp = new VirtualMeterOperation(networkId, op);
        pendingOperations.put(idGenerator.getNewId(), vOp);

        switch (op.type()) {
            case ADD:
                //TODO: devirtualize + submit
                break;
            case REMOVE:
                //TODO: devirtualize + withdraw
                break;
            case MODIFY:
                //TODO: devitualize + withdraw and submit
                break;
            default:
                log.warn("Unknown Meter command {}; not sending anything",
                         op.type());
                VirtualMeterProviderService providerService =
                        (VirtualMeterProviderService) providerRegistryService
                                .getProviderService(networkId,
                                                    VirtualMeterProvider.class);
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN_COMMAND);
        }

    }

    /**
     * De-virtualizes a meter operation.
     * It takes a virtual meter operation, and translate it to a physical meter operation.
     *
     * @param networkId a virtual network identifier
     * @param deviceId a virtual network device identifier
     * @param meterOps a meter operation to be de-virtualized
     * @return de-virtualized meter operation
     */
    private VirtualMeterOperation devirtualize(NetworkId networkId,
                                      DeviceId deviceId,
                                      MeterOperation meterOps) {
        return null;
    }

    /**
     * Virtualizes meter.
     * This translates meter events for virtual networks before delivering them.
     *
     * @param meter
     * @return
     */
    private Meter virtualize(Meter meter) {
        return  null;
    }


    private class InternalMeterListener implements MeterListener {
        @Override
        public void event(MeterEvent event) {
            //TODO: virtualize + notify event to meter provider service
            //Is it enough to enable virtual network provider?
            switch (event.type()) {
                case METER_ADD_REQ:
                    break;
                case METER_REM_REQ:
                    break;
                case METER_ADDED:
                    break;
                case METER_REMOVED:
                    break;
                case METER_REFERENCE_COUNT_ZERO:
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }
        }
    }

    /**
     * A class to hold a network identifier and a meter operation.
     * This class is designed to be used only in virtual network meter provider.
     */
    private final class VirtualMeterOperation {
        private NetworkId networkId;
        private MeterOperation op;

        private VirtualMeterOperation(NetworkId networkId, MeterOperation op) {
            this.networkId = networkId;
            this.op = op;
        }

        private NetworkId networkId() {
            return networkId;
        }

        private MeterOperation operation() {
            return this.op;
        }
    }

    /**
     * A class to hold a network identifier and a meter.
     * This class is designed to be used in only virtual network meter provider.
     */
    private final class VirtualMeter {
        private NetworkId networkId;
        private Meter meter;

        private VirtualMeter(NetworkId networkId, Meter meter) {
            this.networkId = networkId;
            this.meter = meter;
        }

        private NetworkId networkId() {
            return this.networkId;
        }

        private Meter meter() {
            return this.meter;
        }
    }

    /**
     * Id generator for virtual meters to guarantee the uniqueness of its identifier
     * among multiple virtual network meters.
     *
     * @return an ID generator
     */
    private IdGenerator getIdGenerator() {
        return new IdGenerator() {
            private AtomicLong counter = new AtomicLong(0);

            @Override
            public long getNewId() {
                return counter.getAndIncrement();
            }
        };
    }
}
