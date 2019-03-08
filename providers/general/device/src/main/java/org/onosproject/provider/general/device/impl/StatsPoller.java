/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.provider.general.device.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Striped;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.provider.general.device.impl.GeneralDeviceProvider.myScheme;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component devoted to polling stats from devices managed by the
 * GeneralDeviceProvider.
 */
public class StatsPoller {

    private static final int CORE_POOL_SIZE = 5;

    private final Logger log = getLogger(getClass());

    private final DeviceService deviceService;
    private final MastershipService mastershipService;
    private final DeviceProviderService providerService;

    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();
    private final Striped<Lock> deviceLocks = Striped.lock(30);

    private ScheduledExecutorService statsExecutor;
    private ConcurrentMap<DeviceId, ScheduledFuture<?>> statsPollingTasks;
    private ConcurrentMap<DeviceId, Integer> pollFrequencies;
    private int statsPollInterval;

    StatsPoller(DeviceService deviceService, MastershipService mastershipService,
                DeviceProviderService providerService) {
        this.deviceService = deviceService;
        this.mastershipService = mastershipService;
        this.providerService = providerService;
    }


    void activate(int statsPollInterval) {
        checkArgument(statsPollInterval > 0, "statsPollInterval must be greater than 0");
        statsExecutor = newScheduledThreadPool(CORE_POOL_SIZE, groupedThreads(
                "onos/gdp-stats", "%d", log));
        statsPollingTasks = Maps.newConcurrentMap();
        pollFrequencies = Maps.newConcurrentMap();
        reschedule(statsPollInterval);
        deviceService.addListener(deviceListener);
        mastershipService.addListener(mastershipListener);
        log.info("Started");
    }

    void reschedule(int statsPollInterval) {
        checkArgument(statsPollInterval > 0, "statsPollInterval must be greater than 0");
        this.statsPollInterval = statsPollInterval;
        // Consider all devices in the store, plus those of existing tasks
        // (which for some reason might disappear from the store and so we want
        // to cancel).
        Streams.concat(
                StreamSupport.stream(deviceService.getDevices().spliterator(), false)
                        .map(Device::id),
                statsPollingTasks.keySet().stream())
                .distinct()
                .forEach(this::updatePollingTask);
    }

    void deactivate() {
        deviceService.removeListener(deviceListener);
        mastershipService.removeListener(mastershipListener);

        statsPollingTasks.values().forEach(t -> t.cancel(false));
        statsPollingTasks.clear();
        pollFrequencies.clear();
        statsPollingTasks = null;
        pollFrequencies = null;

        statsExecutor.shutdownNow();
        try {
            statsExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("statsExecutor not terminated properly");
        }
        statsExecutor = null;

        log.info("Stopped");
    }


    private void updatePollingTask(DeviceId deviceId) {
        deviceLocks.get(deviceId).lock();
        try {
            final ScheduledFuture<?> existingTask = statsPollingTasks.get(deviceId);
            final boolean shouldHaveTask = myScheme(deviceId)
                    && deviceService.getDevice(deviceId) != null
                    && deviceService.isAvailable(deviceId)
                    && mastershipService.isLocalMaster(deviceId)
                    && deviceService.getDevice(deviceId).is(PortStatisticsDiscovery.class);
            final boolean pollIntervalChanged = !Objects.equals(
                    pollFrequencies.get(deviceId), statsPollInterval);

            if (existingTask != null && (!shouldHaveTask || pollIntervalChanged)) {
                existingTask.cancel(false);
                statsPollingTasks.remove(deviceId);
                pollFrequencies.remove(deviceId);
                log.info("Cancelled polling task for {}", deviceId);
            }

            if (shouldHaveTask) {
                if (statsPollingTasks.containsKey(deviceId)) {
                    // There's already a task, with the same interval.
                    return;
                }
                final int delay = new SecureRandom().nextInt(statsPollInterval);
                statsPollingTasks.put(deviceId, statsExecutor.scheduleAtFixedRate(
                        exceptionSafe(() -> updatePortStatistics(deviceId)),
                        delay, statsPollInterval, TimeUnit.SECONDS));
                pollFrequencies.put(deviceId, statsPollInterval);
                log.info("Started polling task for {} with interval {} seconds",
                         deviceId, statsPollInterval);
            }
        } finally {
            deviceLocks.get(deviceId).unlock();
        }
    }

    private void updatePortStatistics(DeviceId deviceId) {
        final Device device = deviceService.getDevice(deviceId);
        if (!device.is(PortStatisticsDiscovery.class)) {
            log.error("Missing PortStatisticsDiscovery behaviour for {}", deviceId);
        }
        final Collection<PortStatistics> statistics = device.as(
                PortStatisticsDiscovery.class).discoverPortStatistics();
        if (!statistics.isEmpty()) {
            providerService.updatePortStatistics(deviceId, statistics);
        }
    }

    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled exception in stats poller", e);
            }
        };
    }

    private class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            updatePollingTask(event.subject());
        }

        @Override
        public boolean isRelevant(MastershipEvent event) {
            return event.type() == MastershipEvent.Type.MASTER_CHANGED;
        }
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            updatePollingTask(event.subject().id());
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    return true;
                default:
                    return false;
            }
        }
    }
}
