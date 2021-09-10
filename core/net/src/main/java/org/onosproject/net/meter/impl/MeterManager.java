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

import com.google.common.collect.Sets;
import org.onlab.util.PredictableExecutor;
import org.onlab.util.PredictableExecutor.PickyRunnable;
import org.onlab.util.Tools;
import org.onlab.util.TriConsumer;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
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
import org.onosproject.net.meter.MeterCellId;
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
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.PredictableExecutor.newPredictableExecutor;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.MM_FALLBACK_METER_POLL_FREQUENCY;
import static org.onosproject.net.OsgiPropertyConstants.MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MM_NUM_THREADS;
import static org.onosproject.net.OsgiPropertyConstants.MM_NUM_THREADS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MM_PURGE_ON_DISCONNECTION;
import static org.onosproject.net.OsgiPropertyConstants.MM_PURGE_ON_DISCONNECTION_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MM_USER_DEFINED_INDEX;
import static org.onosproject.net.OsgiPropertyConstants.MM_USER_DEFINED_INDEX_DEFAULT;
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
                MM_USER_DEFINED_INDEX + ":Boolean=" + MM_USER_DEFINED_INDEX_DEFAULT,
        }
)
public class MeterManager
        extends AbstractListenerProviderRegistry<MeterEvent, MeterListener, MeterProvider, MeterProviderService>
        implements MeterService, MeterProviderRegistry {

    private PredictableExecutor meterInstallers;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    /** Number of worker threads. */
    // TODO Set 0 to use the available processors
    private int numThreads = MM_NUM_THREADS_DEFAULT;

    /** Frequency (in seconds) for polling meters via fallback provider. */
    private int fallbackMeterPollFrequency = MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT;

    /** Purge entries associated with a device when the device goes offline. */
    private boolean purgeOnDisconnection = MM_PURGE_ON_DISCONNECTION_DEFAULT;

    /** Enable user defined index mode. Users can provide their own meter index. */
    protected boolean userDefinedIndex = MM_USER_DEFINED_INDEX_DEFAULT;

    // Action triggered when the futures related to submit and withdrawal complete
    private TriConsumer<MeterRequest, MeterStoreResult, Throwable> onComplete;

    private final MeterDriverProvider defaultProvider = new MeterDriverProvider();

    // Node id used to verify who is charge of the meter ops
    // (usually one node can modify the internal state of the device)
    private NodeId local;

    @Activate
    public void activate(ComponentContext context) {
        store.setDelegate(delegate);
        cfgService.registerProperties(getClass());
        eventDispatcher.addSink(MeterEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        local = clusterService.getLocalNode().id();
        // Consumer logic is the following:
        // if there is an exceptional end (storage exception), on error is called
        // else if there is a reason for the failure, on error is called with the reason
        // else if the reason is empty, on success is called
        // In all the cases the meter context code is consumed
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
        meterInstallers.shutdown();
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

        flag = Tools.isPropertyEnabled(properties, MM_USER_DEFINED_INDEX);
        boolean enable = flag == null ? userDefinedIndex : flag;
        userDefinedIndex = store.userDefinedIndexMode(enable);
        log.info("UserDefinedIndex is {}", userDefinedIndex ? "enabled" : "disabled");

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
        if (meterInstallers != null) {
            meterInstallers.shutdown();
        }
        meterInstallers = newPredictableExecutor(numThreads,
                groupedThreads(GROUP_THREAD_NAME, WORKER_PATTERN, log));
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
        MeterCellId cellId;
        if (request.index().isPresent()) {
            checkArgument(userDefinedIndex, "Index cannot be provided when userDefinedIndex mode is disabled");
            // User provides index
            if (request.scope().isGlobal()) {
                cellId = MeterId.meterId(request.index().get());
            } else {
                cellId = PiMeterCellId.ofIndirect(
                    PiMeterId.of(request.scope().id()), request.index().get());
            }
        } else {
            checkArgument(!userDefinedIndex, "Index cannot be allocated when userDefinedIndex mode is enabled");
            // Allocate an id
            cellId = allocateMeterId(request.deviceId(), request.scope());
        }
        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(request.deviceId())
                .fromApp(request.appId())
                .withBands(request.bands())
                .withCellId(cellId)
                .withUnit(request.unit());
        if (request.isBurst()) {
            mBuilder.burst();
        }
        if (request.annotations() != null && !request.annotations().keys().isEmpty()) {
            mBuilder.withAnnotations(request.annotations());
        }
        DefaultMeter m = (DefaultMeter) mBuilder.build();
        // Meter installation logic (happy ending case)
        // PENDING -> stats -> ADDED -> future completes
        m.setState(MeterState.PENDING_ADD);
        store.addOrUpdateMeter(m).whenComplete((result, error) ->
                                                 onComplete.accept(request, result, error));
        return m;
    }

    @Override
    public void withdraw(MeterRequest request, MeterId meterId) {
        withdraw(request, (MeterCellId) meterId);
    }

    @Override
    public void withdraw(MeterRequest request, MeterCellId meterCellId) {
        checkNotNull(request, "request cannot be null.");
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
        // Meter removal logic (happy ending case)
        // PENDING -> stats -> removed from the map -> future completes
        m.setState(MeterState.PENDING_REMOVE);
        store.deleteMeter(m).whenComplete((result, error) ->
                                                  onComplete.accept(request, result, error));
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterId id) {
        return getMeter(deviceId, (MeterCellId) id);
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterCellId id) {
        MeterKey key = MeterKey.key(deviceId, id);
        return store.getMeter(key);
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId) {
        return store.getAllMeters(deviceId);
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId, MeterScope scope) {
        return store.getAllMeters(deviceId, scope);
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return store.getAllMeters();
    }

    @Override
    public MeterId allocateMeterId(DeviceId deviceId) {
        // We delegate directly to the store
        return (MeterId) store.allocateMeterId(deviceId, MeterScope.globalScope());
    }

    private MeterCellId allocateMeterId(DeviceId deviceId, MeterScope scope) {
        return store.allocateMeterId(deviceId, scope);
    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {
        // We delegate directly to the store
        store.freeMeterId(deviceId, meterId);
    }

    @Override
    public void purgeMeters(DeviceId deviceId) {
        // We delegate directly to the store
        store.purgeMeters(deviceId);
    }

    @Override
    public void purgeMeters(DeviceId deviceId, ApplicationId appId) {
        store.purgeMeters(deviceId, appId);
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

            Map<MeterCellId, Meter> meterEntriesMap = meterEntries.stream()
                    .collect(Collectors.toMap(Meter::meterCellId, Meter -> Meter));

            // Look for meters defined in onos and missing in the device (restore)
            allMeters.stream().forEach(m -> {
                if ((m.state().equals(MeterState.PENDING_ADD) ||
                        m.state().equals(MeterState.ADDED)) &&
                        !meterEntriesMap.containsKey(m.meterCellId())) {
                    // The meter is missing in the device. Reinstall!
                    log.debug("Adding meter missing in device {} {}", deviceId, m);
                    // offload the task to avoid the overloading of the sb threads
                    meterInstallers.execute(new MeterInstaller(deviceId, m, MeterOperation.Type.ADD));
                }
            });

            // Look for meters defined in the device and not in onos (remove)
            meterEntriesMap.entrySet().stream()
                    .filter(md -> !allMeters.stream().anyMatch(m -> m.meterCellId().equals(md.getKey())))
                    .forEach(mio -> {
                        Meter meter = mio.getValue();
                        // The meter is missing in onos. Uninstall!
                        log.debug("Remove meter in device not in onos {} {}", deviceId, mio.getKey());
                        // offload the task to avoid the overloading of the sb threads
                        meterInstallers.execute(new MeterInstaller(deviceId, meter, MeterOperation.Type.REMOVE));
                    });

            // Update the meter stats in the store (first time move the state from pending to added)
            Collection<Meter> addedMeters = Sets.newHashSet();
            meterEntries.stream()
                    .filter(m -> allMeters.stream()
                            .anyMatch(sm -> sm.deviceId().equals(deviceId) &&
                             sm.meterCellId().equals(m.meterCellId())))
                    .forEach(m -> {
                        Meter updatedMeter = store.updateMeterState(m);
                        if (updatedMeter != null && updatedMeter.state() == MeterState.ADDED) {
                            addedMeters.add(updatedMeter);
                        }
                    });
            Collection<Meter> newAllMeters = Sets.newHashSet(allMeters);
            newAllMeters.removeAll(addedMeters);

            // Remove definetely the remaining meters
            newAllMeters.forEach(m -> {
                if (m.state() == MeterState.PENDING_REMOVE) {
                    log.debug("Delete meter {} now in store", m.meterCellId());
                    store.purgeMeter(m);
                }
            });
        }

        @Override
        public void pushMeterFeatures(DeviceId deviceId, MeterFeatures meterfeatures) {
            store.storeMeterFeatures(meterfeatures);
        }

        @Override
        public void pushMeterFeatures(DeviceId deviceId, Collection<MeterFeatures> meterfeatures) {
            meterfeatures.forEach(mf -> store.storeMeterFeatures(mf));
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
                // REQ events will trigger a modification in the device.
                // Mastership check is performed inside the installer
                // to avoid the blocking of the RAFT threads
                case METER_ADD_REQ:
                    meterInstallers.execute(new MeterInstaller(deviceId, event.subject(),
                                                               MeterOperation.Type.ADD));
                    break;
                case METER_REM_REQ:
                    meterInstallers.execute(new MeterInstaller(deviceId, event.subject(),
                                                               MeterOperation.Type.REMOVE));
                    break;
                // Following events are forwarded to the apps subscribed for the meter events;
                // installers are not involved in this task. In this case, the overhead for this op
                // is almost null. Potentially we can introduce a store delegate thread.
                case METER_ADDED:
                case METER_REMOVED:
                case METER_REFERENCE_COUNT_ZERO:
                    log.debug("Post {} event {}", event.type(), event.subject());
                    post(event);
                    break;
                default:
                    log.warn("Unknown meter event {}", event.type());
            }

        }
    }

    /**
     * Task that passes the meter down to the provider.
     */
    private class MeterInstaller implements PickyRunnable {
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
            // Check mastership and eventually execute the op on the device
            log.debug("Meter {} request {}", op.name().toLowerCase(), meter);
            NodeId master = mastershipService.getMasterFor(meter.deviceId());
            if (!Objects.equals(local, master)) {
                log.trace("Not the master of device {}, skipping installation of the meter {}",
                        meter.deviceId(), meter.meterCellId());
                return;
            }
            MeterProvider p = getProvider(this.deviceId);
            if (p == null) {
                log.error("Unable to recover {}'s provider", deviceId);
                return;
            }
            p.performMeterOperation(deviceId, new MeterOperation(meter, op));
        }

        @Override
        public int hint() {
            return meter.meterCellId().hashCode();
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            meterInstallers.execute(() -> {
                switch (event.type()) {
                    case DEVICE_REMOVED:
                    case DEVICE_AVAILABILITY_CHANGED:
                        if (!deviceService.isAvailable(deviceId)) {
                            BasicDeviceConfig cfg = netCfgService.getConfig(deviceId, BasicDeviceConfig.class);
                            //if purgeOnDisconnection is set for the device or it's a global configuration
                            // lets remove the meters.
                            boolean purge = cfg != null && cfg.isPurgeOnDisconnectionConfigured() ?
                                    cfg.purgeOnDisconnection() : purgeOnDisconnection;
                            if (purge) {
                                log.info("PurgeOnDisconnection is requested for device {}, " +
                                        "removing meters", deviceId);
                                store.purgeMeters(deviceId);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }, deviceId.hashCode());

        }
    }

}
