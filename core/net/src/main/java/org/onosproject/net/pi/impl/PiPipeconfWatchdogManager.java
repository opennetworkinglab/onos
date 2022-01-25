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

package org.onosproject.net.pi.impl;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import org.onlab.util.KryoNamespace;
import org.onlab.util.PredictableExecutor;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfEvent;
import org.onosproject.net.pi.service.PiPipeconfListener;
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.store.primitives.DefaultDistributedSet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.PWM_PROBE_INTERVAL;
import static org.onosproject.net.OsgiPropertyConstants.PWM_PROBE_INTERVAL_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of PiPipeconfWatchdogService that implements a periodic
 * pipeline probe task and listens for device events to update the status of the
 * pipeline.
 */
@Component(
        immediate = true,
        service = PiPipeconfWatchdogService.class,
        property = {
                PWM_PROBE_INTERVAL + ":Integer=" + PWM_PROBE_INTERVAL_DEFAULT
        }
)
public class PiPipeconfWatchdogManager
        extends AbstractListenerManager<PiPipeconfWatchdogEvent, PiPipeconfWatchdogListener>
        implements PiPipeconfWatchdogService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfMappingStore pipeconfMappingStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PiPipeconfService pipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService componentConfigService;

    /**
     * Configure interval in seconds for device pipeconf probing.
     */
    private int probeInterval = PWM_PROBE_INTERVAL_DEFAULT;

    // Setting to 0 will leverage available processors
    private static final int DEFAULT_THREADS = 0;
    protected PredictableExecutor watchdogWorkers = new PredictableExecutor(DEFAULT_THREADS,
            groupedThreads("onos/pipeconf-watchdog", "%d", log));

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final PiPipeconfListener pipeconfListener = new InternalPipeconfListener();

    private ScheduledExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads("onos/pipeconf-event", "%d", log));
    private ScheduledFuture<?> poller = null;

    private EventuallyConsistentMap<DeviceId, PipelineStatus> statusMap;
    private Map<DeviceId, PipelineStatus> localStatusMap;

    // Configured devices by this cluster. We use a set to keep track of all devices for which
    // we have pushed the forwarding pipeline config at least once. This guarantees that device
    // pipelines are wiped out/reset at least once when starting the cluster, minimizing the risk
    // of any stale state from previous runs affecting control operations. Another effect of this
    // approach is that the default entries mirror will get populated even though the pipeline results
    // to be the same across different ONOS installations.
    private static final String CONFIGURED_DEVICES = "onos-pipeconf-configured-set";
    private DistributedSet<DeviceId> configuredDevices;

    @Activate
    public void activate() {
        eventDispatcher.addSink(PiPipeconfWatchdogEvent.class, listenerRegistry);
        localStatusMap = Maps.newConcurrentMap();
        // Init distributed status map and configured devices set
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(PipelineStatus.class);
        statusMap = storageService.<DeviceId, PipelineStatus>eventuallyConsistentMapBuilder()
                .withName("onos-pipeconf-status-table")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        statusMap.addListener(new StatusMapListener());
        // Init the set of the configured devices
        configuredDevices = new DefaultDistributedSet<>(storageService.<DeviceId>setBuilder()
                .withName(CONFIGURED_DEVICES)
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build(),
                DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS);
        // Register component configurable properties.
        componentConfigService.registerProperties(getClass());
        // Start periodic watchdog task.
        startProbeTask();
        // Add listeners.
        deviceService.addListener(deviceListener);
        pipeconfService.addListener(pipeconfListener);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        final int oldProbeInterval = probeInterval;
        probeInterval = Tools.getIntegerProperty(
                properties, PWM_PROBE_INTERVAL, PWM_PROBE_INTERVAL_DEFAULT);
        log.info("Configured. {} is configured to {} seconds",
                 PWM_PROBE_INTERVAL_DEFAULT, probeInterval);

        if (oldProbeInterval != probeInterval) {
            rescheduleProbeTask();
        }
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PiPipeconfWatchdogEvent.class);
        pipeconfService.removeListener(pipeconfListener);
        deviceService.removeListener(deviceListener);
        stopProbeTask();
        eventExecutor.shutdown();
        watchdogWorkers.shutdown();
        statusMap = null;
        localStatusMap = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        final Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            filterAndTriggerTasks(singleton(device));
        }
    }

    @Override
    public PipelineStatus getStatus(DeviceId deviceId) {
        final PipelineStatus status = statusMap.get(deviceId);
        return status == null ? PipelineStatus.UNKNOWN : status;
    }

    private void triggerCheckAllDevices() {
        filterAndTriggerTasks(deviceService.getDevices());
    }

    private void filterAndTriggerTasks(Iterable<Device> devices) {
        devices.forEach(device -> watchdogWorkers.execute(() -> probeTask(device), device.id().hashCode()));
    }

    private void probeTask(Device device) {
        if (!isLocalMaster(device)) {
            return;
        }

        final PiPipeconfId pipeconfId = pipeconfMappingStore.getPipeconfId(device.id());
        if (pipeconfId == null || !device.is(PiPipelineProgrammable.class)) {
            return;
        }

        if (pipeconfService.getPipeconf(pipeconfId).isEmpty()) {
            log.warn("Pipeconf {} is not registered, skipping probe for {}",
                    pipeconfId, device.id());
            return;
        }

        final PiPipeconf pipeconf = pipeconfService.getPipeconf(pipeconfId).get();

        if (!device.is(DeviceHandshaker.class)) {
            log.error("Missing DeviceHandshaker behavior for {}", device.id());
            return;
        }

        final boolean success = doSetPipeconfIfRequired(device, pipeconf);
        // The probe task is not performed in atomic way and between the
        // initial mastership check and the actual probe the execution
        // can be blocked many times and the mastership can change. Recheck
        // the mastership after pipeline probe returns.
        if (isLocalMaster(device)) {
            // An harmless side effect of the check above is that when we return
            // from the set pipeline config we might be no longer the master and this
            // will delay in the worst case the mark online of the device for 15s
            // (next reconcile interval)
            if (success) {
                signalStatusReady(device.id());
                signalStatusConfigured(device.id());
            } else {
                // When a network partition occurs watchdog is stuck for LONG_TIMEOUT
                // before returning and will mark the device offline. However, in the
                // meanwhile the mastership has been passed to another instance which is
                // already connected and has already marked the device online.
                signalStatusUnknown(device.id());
            }
        } else {
            log.warn("No longer the master for {} aborting probe task", device.id());
        }
    }

    /**
     * Returns true if the given device is known to be configured with the given
     * pipeline, false otherwise. If necessary, this method enforces setting the
     * given pipeconf using drivers.
     *
     * @param device   device
     * @param pipeconf pipeconf
     * @return boolean
     */
    private boolean doSetPipeconfIfRequired(Device device, PiPipeconf pipeconf) {
        log.debug("Starting watchdog task for {} ({})", device.id(), pipeconf.id());
        final PiPipelineProgrammable pipelineProg = device.as(PiPipelineProgrammable.class);
        final DeviceHandshaker handshaker = device.as(DeviceHandshaker.class);
        if (!handshaker.hasConnection()) {
            log.warn("There is no connectivity with {}", device.id());
            return false;
        }
        if (Futures.getUnchecked(pipelineProg.isPipeconfSet(pipeconf)) &&
                configuredDevices.contains(device.id())) {
            log.debug("Pipeconf {} already configured on {}",
                      pipeconf.id(), device.id());
            return true;
        }
        return Futures.getUnchecked(pipelineProg.setPipeconf(pipeconf));
    }

    private void signalStatusUnknown(DeviceId deviceId) {
        statusMap.remove(deviceId);
    }

    private void signalStatusReady(DeviceId deviceId) {
        statusMap.put(deviceId, PipelineStatus.READY);
    }

    private void signalStatusUnconfigured(DeviceId deviceId) {
        configuredDevices.remove(deviceId);
    }

    private void signalStatusConfigured(DeviceId deviceId) {
        configuredDevices.add(deviceId);
    }

    private boolean isLocalMaster(Device device) {
        if (mastershipService.isLocalMaster(device.id())) {
            return true;
        }
        // The device might have no master (e.g. after it has been disconnected
        // from core), hence we use device mastership state.
        final MastershipInfo info = mastershipService.getMastershipFor(device.id());
        return !info.master().isPresent() &&
                device.is(DeviceHandshaker.class) &&
                device.as(DeviceHandshaker.class).getRole()
                        .equals(MastershipRole.MASTER);
    }

    private void startProbeTask() {
        synchronized (this) {
            log.info("Starting pipeline probe thread with {} seconds interval...", probeInterval);
            poller = eventExecutor.scheduleAtFixedRate(this::triggerCheckAllDevices, probeInterval,
                    probeInterval, TimeUnit.SECONDS);
        }
    }


    private void stopProbeTask() {
        synchronized (this) {
            log.info("Stopping pipeline probe thread...");
            poller.cancel(false);
            poller = null;
        }
    }


    private synchronized void rescheduleProbeTask() {
        synchronized (this) {
            stopProbeTask();
            startProbeTask();
        }
    }

    /**
     * Listener of device events used to update the pipeline status.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            eventExecutor.execute(() -> {
                final Device device = event.subject();
                switch (event.type()) {
                    case DEVICE_ADDED:
                    case DEVICE_UPDATED:
                    case DEVICE_AVAILABILITY_CHANGED:
                        // The GeneralDeviceProvider marks online/offline devices that
                        // have/have not ANY pipeline config set. Here we make sure the
                        // one configured in the pipeconf service is the expected one.
                        // Clearly, it would be better to let the GDP do this check and
                        // avoid sending twice the same message to the switch.
                        if (!deviceService.isAvailable(device.id())) {
                            signalStatusUnknown(device.id());
                        } else {
                            filterAndTriggerTasks(singleton(device));
                        }
                        break;
                    case DEVICE_REMOVED:
                    case DEVICE_SUSPENDED:
                        signalStatusUnknown(device.id());
                        signalStatusUnconfigured(device.id());
                        break;
                    case PORT_ADDED:
                    case PORT_UPDATED:
                    case PORT_REMOVED:
                    case PORT_STATS_UPDATED:
                    default:
                        break;
                }
            });
        }
    }

    private class InternalPipeconfListener implements PiPipeconfListener {
        @Override
        public void event(PiPipeconfEvent event) {
            eventExecutor.execute(() -> {
                if (Objects.equals(event.type(), PiPipeconfEvent.Type.REGISTERED)) {
                    pipeconfMappingStore.getDevices(event.subject())
                            .forEach(PiPipeconfWatchdogManager.this::triggerProbe);
                }
            });
        }
    }

    private class StatusMapListener
            implements EventuallyConsistentMapListener<DeviceId, PipelineStatus> {

        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, PipelineStatus> event) {
            final DeviceId deviceId = event.key();
            final PipelineStatus status = event.value();
            switch (event.type()) {
                case PUT:
                    postStatusEvent(deviceId, status);
                    break;
                case REMOVE:
                    postStatusEvent(deviceId, PipelineStatus.UNKNOWN);
                    break;
                default:
                    log.error("Unknown map event type {}", event.type());
            }
        }

        private void postStatusEvent(DeviceId deviceId, PipelineStatus newStatus) {
            PipelineStatus oldStatus = localStatusMap.put(deviceId, newStatus);
            oldStatus = oldStatus == null ? PipelineStatus.UNKNOWN : oldStatus;
            final PiPipeconfWatchdogEvent.Type eventType =
                    newStatus == PipelineStatus.READY
                            ? PiPipeconfWatchdogEvent.Type.PIPELINE_READY
                            : PiPipeconfWatchdogEvent.Type.PIPELINE_UNKNOWN;
            if (newStatus != oldStatus) {
                log.info("Pipeline status of {} is {}", deviceId, newStatus);
                post(new PiPipeconfWatchdogEvent(eventType, deviceId));
            }
        }
    }
}
