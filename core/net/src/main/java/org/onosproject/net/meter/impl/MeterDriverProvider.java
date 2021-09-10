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

package org.onosproject.net.meter.impl;

import com.google.common.collect.Sets;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.meter.MeterProvider;

import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;

/**
 * Driver-based Meter provider.
 */
public class MeterDriverProvider extends AbstractProvider implements MeterProvider {
    // To be extracted for reuse as we deal with other.
    private static final String SCHEME = "default";
    private static final String PROVIDER_NAME = "org.onosproject.provider.meter";

    private static final Set<DeviceEvent.Type> POSITIVE_DEVICE_EVENT = Sets.immutableEnumSet(
            DEVICE_ADDED, DEVICE_AVAILABILITY_CHANGED);

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected DeviceService deviceService;
    protected MastershipService mastershipService;
    MeterProviderService meterProviderService;
    int pollFrequency;

    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private ScheduledExecutorService executor
            = newSingleThreadScheduledExecutor(groupedThreads("MeterDriverProvider", "%d", log));
    private ScheduledFuture<?> poller = null;

    public MeterDriverProvider() {
        super(new ProviderId(SCHEME, PROVIDER_NAME));
    }

    /**
     * Initializes the provider with the necessary device service, meter provider service,
     * mastership service and poll frequency.
     *
     * @param deviceService        device service
     * @param meterProviderService meter provider service
     * @param mastershipService    mastership service
     * @param pollFrequency        meter entry poll frequency
     */
    void init(DeviceService deviceService, MeterProviderService meterProviderService,
              MastershipService mastershipService, int pollFrequency) {
        this.deviceService = deviceService;
        this.meterProviderService = meterProviderService;
        this.mastershipService = mastershipService;
        this.pollFrequency = pollFrequency;

        deviceService.addListener(deviceListener);

        if (poller != null && !poller.isCancelled()) {
            poller.cancel(false);
        }

        poller = executor.scheduleAtFixedRate(this::pollMeters, pollFrequency,
                pollFrequency, TimeUnit.SECONDS);

    }

    void terminate() {
        deviceService.removeListener(deviceListener);
        deviceService = null;
        meterProviderService = null;
        mastershipService = null;
        poller.cancel(true);
        executor.shutdown();
    }

    private void pollMeters() {
        try {
            deviceService.getAvailableDevices().forEach(device -> {
                if (mastershipService.isLocalMaster(device.id()) && device.is(MeterProgrammable.class)) {
                    pollDeviceMeters(device);
                }
            });
        } catch (Exception e) {
            log.warn("Exception thrown while polling meters", e);
        }
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperations meterOps) {
        meterOps.operations().forEach(meterOperation -> performMeterOperation(deviceId, meterOperation));
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperation meterOp) {
        MeterProgrammable programmable = getMeterProgrammable(deviceId);
        if (programmable != null) {
            programmable.performMeterOperation(meterOp);
        }
    }

    private void pollDeviceMeters(Device device) {
        try {
            meterProviderService.pushMeterMetrics(device.id(), device.as(MeterProgrammable.class).getMeters()
                    .completeOnTimeout(Collections.emptySet(), pollFrequency, TimeUnit.SECONDS).get());
        } catch (Exception e) {
            log.warn("Unable to get the Meters from {}, error: {}", device, e.getMessage());
            log.debug("Exception: ", e);
        }
    }

    private void getMeterFeatures(Device device) {
        try {
            meterProviderService.pushMeterFeatures(device.id(), device.as(MeterProgrammable.class).getMeterFeatures()
                    .completeOnTimeout(Collections.emptySet(), pollFrequency, TimeUnit.SECONDS).get());
        } catch (Exception e) {
            log.warn("Unable to get the Meter Features from {}, error: {}", device.id(), e.getMessage());
            log.debug("Exception: ", e);
        }
    }

    private MeterProgrammable getMeterProgrammable(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && device.is(MeterProgrammable.class)) {
            return device.as(MeterProgrammable.class);
        } else {
            log.debug("Device {} is not meter programmable or does not exist", deviceId);
            return null;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            executor.execute(() -> handleEvent(event));
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().is(MeterProgrammable.class);
        }

        private void handleEvent(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {
                case DEVICE_ADDED:
                    getMeterFeatures(device);
                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    meterProviderService.deleteMeterFeatures(device.id());
                    break;
                default:
                    break;
            }

            boolean isRelevant = POSITIVE_DEVICE_EVENT.contains(event.type()) &&
                    mastershipService.isLocalMaster(device.id()) && deviceService.isAvailable(device.id());

            if (isRelevant) {
                pollDeviceMeters(device);
            }
        }
    }
}
