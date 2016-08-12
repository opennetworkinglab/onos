/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.faultmanagement.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmConsumer;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderRegistry;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Alarm provider capable of polling the environment using the device driver
 * {@link AlarmConsumer} behaviour.
 */
@Component(immediate = true)
public class PollingAlarmProvider extends AbstractProvider implements AlarmProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AlarmProviderRegistry providerRegistry;

    protected AlarmProviderService providerService;

    protected ScheduledExecutorService alarmsExecutor;

    private ScheduledFuture<?> scheduledTask;

    private ExecutorService eventHandlingExecutor;

    protected final MastershipListener mastershipListener = new InternalMastershipListener();

    protected final DeviceListener deviceListener = new InternalDeviceListener();

    private static final int CORE_POOL_SIZE = 10;

    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 60;
    @Property(name = "alarmPollFrequencySeconds", intValue = DEFAULT_POLL_FREQUENCY_SECONDS,
            label = "Frequency (in seconds) for polling alarm from devices")
    protected int alarmPollFrequencySeconds = DEFAULT_POLL_FREQUENCY_SECONDS;

    // TODO implement purging of old alarms.
    private static final int DEFAULT_CLEAR_FREQUENCY_SECONDS = 500;
    @Property(name = "clearedAlarmPurgeSeconds", intValue = DEFAULT_CLEAR_FREQUENCY_SECONDS,
            label = "Frequency (in seconds) for deleting cleared alarms")
    private int clearedAlarmPurgeFrequencySeconds = DEFAULT_CLEAR_FREQUENCY_SECONDS;

    public PollingAlarmProvider() {
        super(new ProviderId("default", "org.onosproject.core"));
    }

    @Activate
    public void activate(ComponentContext context) {
        alarmsExecutor = newScheduledThreadPool(CORE_POOL_SIZE,
                                                groupedThreads("onos/pollingalarmprovider",
                                                               "alarm-executor-%d", log));
        eventHandlingExecutor =
                Executors.newFixedThreadPool(CORE_POOL_SIZE,
                                             groupedThreads("onos/pollingalarmprovider",
                                                            "device-installer-%d", log));

        providerService = providerRegistry.register(this);

        deviceService.addListener(deviceListener);
        mastershipService.addListener(mastershipListener);

        if (context == null) {
            alarmPollFrequencySeconds = DEFAULT_POLL_FREQUENCY_SECONDS;
            log.info("No component configuration");
        } else {
            Dictionary<?, ?> properties = context.getProperties();
            alarmPollFrequencySeconds = getNewPollFrequency(properties, alarmPollFrequencySeconds);
        }
        scheduledTask = schedulePolling();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        mastershipService.removeListener(mastershipListener);
        deviceService.removeListener(deviceListener);
        alarmsExecutor.shutdown();
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No component configuration");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newPollFrequency = getNewPollFrequency(properties, alarmPollFrequencySeconds);
        if (newPollFrequency != alarmPollFrequencySeconds) {
            alarmPollFrequencySeconds = newPollFrequency;
            //stops the old scheduled task
            scheduledTask.cancel(true);
            //schedules new task at the new polling rate
            scheduledTask = schedulePolling();
        }
    }

    private ScheduledFuture schedulePolling() {
        return alarmsExecutor.scheduleAtFixedRate(this::consumeAlarms,
                                                  alarmPollFrequencySeconds / 4, alarmPollFrequencySeconds,
                                                  TimeUnit.SECONDS);
    }

    private int getNewPollFrequency(Dictionary<?, ?> properties, int pollFrequency) {
        int newPollFrequency;
        try {
            String s = get(properties, "pollFrequency");
            newPollFrequency = isNullOrEmpty(s) ? pollFrequency : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = DEFAULT_POLL_FREQUENCY_SECONDS;
        }
        return newPollFrequency;
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        if (mastershipService.isLocalMaster(deviceId)) {
            triggerProbe(deviceService.getDevice(deviceId));
        }
    }

    private void triggerProbe(Device device) {
        alarmsExecutor.submit(() -> consumeAlarms(device));
    }

    private void consumeAlarms() {
        deviceService.getAvailableDevices().forEach(device -> {
            if (mastershipService.isLocalMaster(device.id())) {
                consumeAlarms(device);
            }
        });
    }

    private void consumeAlarms(Device device) {
        if (device.is(AlarmConsumer.class)) {
            providerService.updateAlarmList(device.id(),
                                            device.as(AlarmConsumer.class).consumeAlarms());
        } else {
            log.debug("Device {} does not support alarm consumer behaviour", device.id());
        }
    }

    private class InternalMastershipListener implements MastershipListener {

        @Override
        public boolean isRelevant(MastershipEvent event) {
            return mastershipService.isLocalMaster(event.subject());
        }

        @Override
        public void event(MastershipEvent event) {
            triggerProbe(event.subject());
        }
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type().equals(DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED)
                    && deviceService.isAvailable(event.subject().id());
        }

        @Override
        public void event(DeviceEvent event) {
            log.debug("InternalDeviceListener has got event from device-service{} with ", event);
            eventHandlingExecutor.execute(() -> triggerProbe(event.subject().id()));
        }

    }
}
