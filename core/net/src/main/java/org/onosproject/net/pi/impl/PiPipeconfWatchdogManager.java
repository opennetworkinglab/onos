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
import com.google.common.util.concurrent.Striped;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
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
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of PiPipeconfWatchdogService that implements a periodic
 * pipeline probe task and listens for device events to update the status of the
 * pipeline.
 */
@Component(immediate = true)
@Service
public class PiPipeconfWatchdogManager
        extends AbstractListenerManager<PiPipeconfWatchdogEvent, PiPipeconfWatchdogListener>
        implements PiPipeconfWatchdogService {

    private final Logger log = getLogger(getClass());

    private static final long SECONDS = 1000L;
    // Long enough to allow for network delay (e.g. to transfer large pipeline
    // binaries over slow network).
    private static final long PIPECONF_SET_TIMEOUT = 60; // Seconds.

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfMappingStore pipeconfMappingStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PiPipeconfService pipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService componentConfigService;

    private static final String PROBE_INTERVAL = "probeInterval";
    private static final int DEFAULT_PROBE_INTERVAL = 15;
    @Property(name = PROBE_INTERVAL, intValue = DEFAULT_PROBE_INTERVAL,
            label = "Configure interval in seconds for device pipeconf probing")
    private int probeInterval = DEFAULT_PROBE_INTERVAL;

    protected ExecutorService executor = Executors.newFixedThreadPool(
            30, groupedThreads("onos/pipeconf-watchdog", "%d", log));

    private final InternalDeviceListener deviceListener = new InternalDeviceListener();

    private Timer timer;
    private TimerTask task;

    private final Striped<Lock> locks = Striped.lock(30);

    private EventuallyConsistentMap<DeviceId, PipelineStatus> statusMap;
    private Map<DeviceId, PipelineStatus> localStatusMap;

    @Activate
    public void activate() {
        eventDispatcher.addSink(PiPipeconfWatchdogEvent.class, listenerRegistry);
        localStatusMap = Maps.newConcurrentMap();
        // Init distributed status map.
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(PipelineStatus.class);
        statusMap = storageService.<DeviceId, PipelineStatus>eventuallyConsistentMapBuilder()
                .withName("onos-pipeconf-status-table")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        statusMap.addListener(new StatusMapListener());
        // Register component configurable properties.
        componentConfigService.registerProperties(getClass());
        // Start periodic watchdog task.
        timer = new Timer();
        startProbeTask();
        // Add device listener.
        deviceService.addListener(deviceListener);
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
                properties, PROBE_INTERVAL, DEFAULT_PROBE_INTERVAL);
        log.info("Configured. {} is configured to {} seconds",
                 PROBE_INTERVAL, probeInterval);

        if (oldProbeInterval != probeInterval) {
            rescheduleProbeTask();
        }
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PiPipeconfWatchdogEvent.class);
        deviceService.removeListener(deviceListener);
        stopProbeTask();
        timer = null;
        statusMap = null;
        localStatusMap = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        final Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            filterAndTriggerTasks(Collections.singleton(device));
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
        devices.forEach(device -> {
            if (!isLocalMaster(device)) {
                return;
            }

            final PiPipeconfId pipeconfId = pipeconfMappingStore.getPipeconfId(device.id());
            if (pipeconfId == null
                    || !device.is(PiPipelineProgrammable.class)) {
                return;
            }

            if (!pipeconfService.getPipeconf(pipeconfId).isPresent()) {
                log.error("Pipeconf {} is not registered", pipeconfId);
                return;
            }

            final PiPipeconf pipeconf = pipeconfService.getPipeconf(pipeconfId).get();

            if (!device.is(DeviceHandshaker.class)) {
                log.error("Missing DeviceHandshaker behavior for {}", device.id());
                return;
            }

            // Trigger task with per-device lock.
            executor.execute(withLock(() -> {
                final boolean success = doSetPipeconfIfRequired(device, pipeconf);
                if (success) {
                    signalStatusReady(device.id());
                } else {
                    signalStatusUnknown(device.id());
                }
            }, device.id()));
        });
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
        if (!handshaker.isConnected()) {
            return false;
        }
        if (pipelineProg.isPipeconfSet(pipeconf)) {
            log.debug("Pipeconf {} already configured on {}",
                      pipeconf.id(), device.id());
            return true;
        }
        try {
            return pipelineProg.setPipeconf(pipeconf)
                    .get(PIPECONF_SET_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while setting pipeconf on {}",
                      device.id());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Exception while setting pipeconf on {}",
                      device.id(), e.getCause());
        } catch (TimeoutException e) {
            log.error("Operation TIMEOUT while setting pipeconf on {}",
                      device.id());
        }
        return false;
    }

    private Runnable withLock(Runnable task, Object object) {
        return () -> {
            final Lock lock = locks.get(object);
            lock.lock();
            try {
                task.run();
            } finally {
                lock.unlock();
            }
        };
    }

    private void signalStatusUnknown(DeviceId deviceId) {
        statusMap.remove(deviceId);
    }

    private void signalStatusReady(DeviceId deviceId) {
        statusMap.put(deviceId, PipelineStatus.READY);
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
        synchronized (timer) {
            log.info("Starting pipeline probe thread with {} seconds interval...", probeInterval);
            task = new InternalTimerTask();
            timer.scheduleAtFixedRate(task, probeInterval * SECONDS,
                                      probeInterval * SECONDS);
        }
    }


    private void stopProbeTask() {
        synchronized (timer) {
            log.info("Stopping pipeline probe thread...");
            task.cancel();
            task = null;
        }
    }


    private synchronized void rescheduleProbeTask() {
        synchronized (timer) {
            stopProbeTask();
            startProbeTask();
        }
    }

    private class InternalTimerTask extends TimerTask {
        @Override
        public void run() {
            triggerCheckAllDevices();
        }
    }

    /**
     * Listener of device events used to update the pipeline status.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            final Device device = event.subject();
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (!deviceService.isAvailable(device.id())) {
                        signalStatusUnknown(device.id());
                    }
                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    signalStatusUnknown(device.id());
                    break;
                case PORT_ADDED:
                case PORT_UPDATED:
                case PORT_REMOVED:
                case PORT_STATS_UPDATED:
                default:
                    break;
            }
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
