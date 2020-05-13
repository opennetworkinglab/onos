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
package org.onosproject.net.meter.impl;

import org.onlab.util.Tools;
import org.onlab.util.TriConsumer;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId.MeterCellType;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.MM_FALLBACK_METER_POLL_FREQUENCY;
import static org.onosproject.net.OsgiPropertyConstants.MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MM_NUM_THREADS;
import static org.onosproject.net.OsgiPropertyConstants.MM_NUM_THREADS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MM_PURGE_ON_DISCONNECTION;
import static org.onosproject.net.OsgiPropertyConstants.MM_PURGE_ON_DISCONNECTION_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the meter service APIs.
 */
@Component(
        immediate = true,
        service = {
                MeterService.class,
                MeterProviderRegistry.class
        },
        property = {
                MM_NUM_THREADS + ":Integer=" + MM_NUM_THREADS_DEFAULT,
                MM_FALLBACK_METER_POLL_FREQUENCY + ":Integer=" + MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT,
                MM_PURGE_ON_DISCONNECTION + ":Boolean=" + MM_PURGE_ON_DISCONNECTION_DEFAULT,
        }
)
public class MeterManager
        extends AbstractListenerProviderRegistry<MeterEvent, MeterListener, MeterProvider, MeterProviderService>
        implements MeterService, MeterProviderRegistry {

    private static final String WORKER_PATTERN = "installer-%d";
    private static final String GROUP_THREAD_NAME = "onos/meter";

    private final Logger log = getLogger(getClass());
    private final MeterStoreDelegate delegate = new InternalMeterStoreDelegate();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MeterStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    /** Number of worker threads. */
    private int numThreads = MM_NUM_THREADS_DEFAULT;

    /** Frequency (in seconds) for polling meters via fallback provider. */
    private int fallbackMeterPollFrequency = MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT;

    /** Purge entries associated with a device when the device goes offline. */
    private boolean purgeOnDisconnection = MM_PURGE_ON_DISCONNECTION_DEFAULT;

    private TriConsumer<MeterRequest, MeterStoreResult, Throwable> onComplete;

    private ExecutorService executorService;

    private final MeterDriverProvider defaultProvider = new MeterDriverProvider();

    @Activate
    public void activate(ComponentContext context) {
        store.setDelegate(delegate);
        cfgService.registerProperties(getClass());
        eventDispatcher.addSink(MeterEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);

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

        modified(context);
        executorService = newFixedThreadPool(numThreads,
                                             groupedThreads(GROUP_THREAD_NAME, WORKER_PATTERN, log));
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            readComponentConfiguration(context);
        }
        defaultProvider.init(deviceService, createProviderService(defaultProvider),
                             mastershipService, fallbackMeterPollFrequency);
    }

    @Deactivate
    public void deactivate() {
        defaultProvider.terminate();
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(MeterEvent.class);
        deviceService.removeListener(deviceListener);
        cfgService.unregisterProperties(getClass(), false);
        executorService.shutdown();
        log.info("Stopped");
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, MM_PURGE_ON_DISCONNECTION);
        if (flag == null) {
            log.info("PurgeOnDisconnection is not configured," +
                             "using current value of {}", purgeOnDisconnection);
        } else {
            purgeOnDisconnection = flag;
            log.info("Configured. PurgeOnDisconnection is {}",
                     purgeOnDisconnection ? "enabled" : "disabled");
        }

        String s = get(properties, MM_FALLBACK_METER_POLL_FREQUENCY);
        try {
            fallbackMeterPollFrequency = isNullOrEmpty(s) ?
                MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            fallbackMeterPollFrequency = MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT;
        }

        s = get(properties, MM_NUM_THREADS);
        try {
            numThreads = isNullOrEmpty(s) ? MM_NUM_THREADS_DEFAULT : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            numThreads = MM_NUM_THREADS_DEFAULT;
        }
    }

    @Override
    protected MeterProvider defaultProvider() {
        return defaultProvider;
    }

    @Override
    protected MeterProviderService createProviderService(MeterProvider provider) {
        return new InternalMeterProviderService(provider);
    }

    @Override
    public Meter submit(MeterRequest request) {
        checkNotNull(request, "request cannot be null.");
        MeterId id = allocateMeterId(request.deviceId());

        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withCellId(id)
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
        checkNotNull(request, "request cannot be null.");
        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withCellId(meterId)
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

    @Override
    public MeterId allocateMeterId(DeviceId deviceId) {
        // We delegate direclty to the store
        return store.allocateMeterId(deviceId);
    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {
        // We delegate direclty to the store
        store.freeMeterId(deviceId, meterId);
    }

    @Override
    public void purgeMeters(DeviceId deviceId) {
        // We delegate direclty to the store
        store.purgeMeter(deviceId);
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
            Collection<Meter> allMeters = store.getAllMeters(deviceId);

            Map<MeterId, Meter> meterEntriesMap = meterEntries.stream()
                    .collect(Collectors.toMap(Meter::id, Meter -> Meter));

            // Look for meters defined in onos and missing in the device (restore)
            allMeters.stream().forEach(m -> {
                if ((m.state().equals(MeterState.PENDING_ADD) ||
                        m.state().equals(MeterState.ADDED)) &&
                        !meterEntriesMap.containsKey(m.id())) {
                    // The meter is missing in the device. Reinstall!
                    log.debug("Adding meter missing in device {} {}", deviceId, m);
                    provider().performMeterOperation(deviceId,
                                                     new MeterOperation(m, MeterOperation.Type.ADD));
                }
            });

            // Look for meters defined in the device and not in onos (remove)
            meterEntriesMap.entrySet().stream()
                    .filter(md -> !allMeters.stream().anyMatch(m -> m.id().equals(md.getKey())))
                    .forEach(mio -> {
                        Meter meter = mio.getValue();
                        // FIXME: Removing a meter is meaningfull for OpenFlow, but not for P4Runtime.
                        // In P4Runtime meter cells cannot be removed. For the
                        // moment, we make the distinction between OpenFlow and
                        // P4Runtime by looking at the MeterCellType (always
                        // INDEX for OpenFlow).
                        if (meter.meterCellId().type() == MeterCellType.INDEX) {
                            // The meter is missing in onos. Uninstall!
                            log.debug("Remove meter in device not in onos {} {}", deviceId, mio.getKey());
                            provider().performMeterOperation(deviceId,
                                                             new MeterOperation(meter, MeterOperation.Type.REMOVE));
                        }
                    });

            meterEntries.stream()
                    .filter(m -> allMeters.stream()
                            .anyMatch(sm -> sm.deviceId().equals(deviceId) && sm.id().equals(m.id())))
                    .forEach(m -> store.updateMeterState(m));

            allMeters.forEach(m -> {
                if (m.state() == MeterState.PENDING_ADD) {
                    provider().performMeterOperation(m.deviceId(),
                                                     new MeterOperation(m,
                                                                        MeterOperation.Type.MODIFY));
                } else if (m.state() == MeterState.PENDING_REMOVE) {
                    store.deleteMeterNow(m);
                }
            });
        }

        @Override
        public void pushMeterFeatures(DeviceId deviceId, MeterFeatures meterfeatures) {
            store.storeMeterFeatures(meterfeatures);
        }

        @Override
        public void deleteMeterFeatures(DeviceId deviceId) {
            store.deleteMeterFeatures(deviceId);
        }
    }

    private class InternalMeterStoreDelegate implements MeterStoreDelegate {

        @Override
        public void notify(MeterEvent event) {
            DeviceId deviceId = event.subject().deviceId();
            switch (event.type()) {
                case METER_ADD_REQ:
                    executorService.execute(new MeterInstaller(deviceId, event.subject(),
                                                               MeterOperation.Type.ADD));
                    break;
                case METER_REM_REQ:
                    executorService.execute(new MeterInstaller(deviceId, event.subject(),
                                                               MeterOperation.Type.REMOVE));
                    break;
                case METER_ADDED:
                    log.info("Meter added {}", event.subject());
                    post(new MeterEvent(MeterEvent.Type.METER_ADDED, event.subject()));
                    break;
                case METER_REMOVED:
                    log.info("Meter removed {}", event.subject());
                    post(new MeterEvent(MeterEvent.Type.METER_REMOVED, event.subject()));
                    break;
                case METER_REFERENCE_COUNT_ZERO:
                    log.debug("Meter reference count zero {}", event.subject());
                    post(new MeterEvent(MeterEvent.Type.METER_REFERENCE_COUNT_ZERO, event.subject()));
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }

        }
    }
    /**
     * Task that passes the meter down to the provider.
     */
    private class MeterInstaller implements Runnable {
        private final DeviceId deviceId;
        private final Meter meter;
        private final MeterOperation.Type op;

        public MeterInstaller(DeviceId deviceId, Meter meter, MeterOperation.Type op) {
            this.deviceId = checkNotNull(deviceId);
            this.meter = checkNotNull(meter);
            this.op = checkNotNull(op);
        }

        @Override
        public void run() {
            MeterProvider p = getProvider(this.deviceId);
            if (p == null) {
                log.error("Unable to recover {}'s provider", deviceId);
                return;
            }
            p.performMeterOperation(deviceId, new MeterOperation(meter, op));
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    DeviceId deviceId = event.subject().id();
                    if (!deviceService.isAvailable(deviceId)) {
                        BasicDeviceConfig cfg = netCfgService.getConfig(deviceId, BasicDeviceConfig.class);
                        //if purgeOnDisconnection is set for the device or it's a global configuration
                        // lets remove the meters.
                        boolean purge = cfg != null && cfg.isPurgeOnDisconnectionConfigured() ?
                                cfg.purgeOnDisconnection() : purgeOnDisconnection;
                        if (purge) {
                            log.info("PurgeOnDisconnection is requested for device {}, " +
                                             "removing meters", deviceId);
                            store.purgeMeter(deviceId);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
