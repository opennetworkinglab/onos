/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.faultmanagement.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import static org.onlab.util.Tools.nullIsNotFound;

import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEvent;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmListener;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.apache.felix.scr.annotations.Service;
import static org.onlab.util.Tools.get;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.CoreService;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.ComponentContext;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.atomic.AtomicLong;
import static org.onlab.util.Tools.groupedThreads;
import org.onosproject.net.Device;

/**
 * Implementation of the Alarm service.
 */
@Component(immediate = true)
@Service
public class AlarmsManager implements AlarmService {

    // For subscribing to device-related events
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AlarmProvider alarmProvider;

    private final Logger log = getLogger(getClass());
    private ApplicationId appId;
    private IdGenerator idGenerator;

    private ScheduledExecutorService alarmPollExecutor;

    // dummy data
    private final AtomicLong alarmIdGenerator = new AtomicLong(0);

    private AlarmId generateAlarmId() {
        return AlarmId.alarmId(alarmIdGenerator.incrementAndGet());
    }

    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 120;
    @Property(name = "alarmPollFrequencySeconds", intValue = DEFAULT_POLL_FREQUENCY_SECONDS,
            label = "Frequency (in seconds) for polling alarm from devices")
    private int alarmPollFrequencySeconds = DEFAULT_POLL_FREQUENCY_SECONDS;

    // TODO implement purging of old alarms.
    private static final int DEFAULT_CLEAR_FREQUENCY_SECONDS = 500;
    @Property(name = "clearedAlarmPurgeSeconds", intValue = DEFAULT_CLEAR_FREQUENCY_SECONDS,
            label = "Frequency (in seconds) for deleting cleared alarms")
    private int clearedAlarmPurgeFrequencySeconds = DEFAULT_CLEAR_FREQUENCY_SECONDS;

    // TODO Later should must be persisted to disk or database
    private final Map<AlarmId, Alarm> alarms = new ConcurrentHashMap<>();

    @Override
    public Alarm updateBookkeepingFields(AlarmId id, boolean isAcknowledged, String assignedUser) {

        Alarm found = alarms.get(id);
        if (found == null) {
            throw new ItemNotFoundException("Alarm with id " + id + " found");
        }

        Alarm updated = new DefaultAlarm.Builder(found).
                withAcknowledged(isAcknowledged).
                withAssignedUser(assignedUser).build();
        alarms.put(id, updated);
        return updated;
    }

    public Alarm clear(AlarmId id) {

        Alarm found = alarms.get(id);
        if (found == null) {
            log.warn("id {} cant be cleared as it is already gone.", id);
            return null;
        }
        Alarm updated = new DefaultAlarm.Builder(found).clear().build();
        alarms.put(id, updated);
        return updated;
    }

    @Override
    public Map<Alarm.SeverityLevel, Long> getAlarmCounts(DeviceId deviceId) {

        return getAlarms(deviceId).stream().collect(
                Collectors.groupingBy(Alarm::severity, Collectors.counting()));

    }

    @Override
    public Map<Alarm.SeverityLevel, Long> getAlarmCounts() {

        return getAlarms().stream().collect(
                Collectors.groupingBy(Alarm::severity, Collectors.counting()));
    }


    private static final String NOT_SUPPORTED_YET = "Not supported yet.";

    @Override
    public Alarm getAlarm(AlarmId alarmId) {
        return nullIsNotFound(
                alarms.get(
                        checkNotNull(alarmId, "Alarm Id cannot be null")),
                "Alarm is not found");
    }

    @Override
    public Set<Alarm> getAlarms() {
        return new HashSet<>(alarms.values());
    }

    @Override
    public Set<Alarm> getActiveAlarms() {
        return alarms.values().stream().filter(
                a -> !a.severity().equals(Alarm.SeverityLevel.CLEARED)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Alarm> getAlarms(Alarm.SeverityLevel severity) {
        return alarms.values().stream().filter(
                a -> a.severity().equals(severity)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId) {
        return alarms.values().stream().filter(
                a -> deviceId.equals(a.deviceId())).
                collect(Collectors.toSet());
    }

    private Set<Alarm> getActiveAlarms(DeviceId deviceId) {
        return getActiveAlarms().stream().filter(
                a -> deviceId.equals(a.deviceId())).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId, AlarmEntityId source) {
        return getAlarms(deviceId).stream().filter(
                a -> source.equals(a.source())
        ).collect(Collectors.toSet());
    }

    @Override
    public Set<Alarm> getAlarmsForLink(ConnectPoint src, ConnectPoint dst) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public Set<Alarm> getAlarmsForFlow(DeviceId deviceId, long flowId) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    private final AlarmListener alarmListener = new InternalAlarmListener();

    private class InternalAlarmListener implements AlarmListener {

        @Override
        public void event(AlarmEvent event) {
            log.debug("AlarmsManager. InternalAlarmListener received {}", event);
            try {

                switch (event.type()) {
                    case DEVICE_DISCOVERY:
                        DeviceId deviceId = checkNotNull(event.getDeviceRefreshed(), "Listener cannot be null");
                        log.info("New alarm set for {} received!", deviceId);
                        updateAlarms(event.subject(), deviceId);
                        break;

                    case NOTIFICATION:
                        throw new IllegalArgumentException(
                                "Alarm Notifications (Traps) not expected or implemented yet. Received =" + event);
                    default:
                        break;
                }
            } catch (Exception e) {
                log.warn("Failed to process {}", event, e);
            }
        }
    }

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.faultmanagement.alarms");
        idGenerator = coreService.getIdGenerator("alarm-ids");
        log.info("Started with appId={}", appId);

        alarmProvider.addAlarmListener(alarmListener);

        probeActiveDevices();

        boolean result = modified(context);
        log.info("modified result = {}", result);

        alarmPollExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/fm", "alarms-poll-%d"));
        alarmPollExecutor.scheduleAtFixedRate(new PollAlarmsTask(),
                alarmPollFrequencySeconds, alarmPollFrequencySeconds, SECONDS);

    }

    /**
     * Auxiliary task to keep alarms up to date. IN future release alarm-notifications will be used as an optimization
     * so we dont have to wait until polling to detect changes. Furthermore with simple polling flapping alarms may be
     * missed.
     */
    private final class PollAlarmsTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }
            try {
                probeActiveDevices();
            } catch (RuntimeException e) {
                log.error("Exception thrown during alarm synchronization process", e);
            }
        }
    }

    private void probeActiveDevices() {
        Iterable<Device> devices = deviceService.getAvailableDevices();
        log.info("Refresh alarms for all available devices={} ...", devices);
        for (Device d : devices) {
            log.info("Lets tell alarm provider to refresh alarms for {} ...", d.id());
            alarmProvider.triggerProbe(d.id());
        }
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        log.info("Deactivate ...");
        alarmProvider.removeAlarmListener(alarmListener);

        if (alarmPollExecutor != null) {
            alarmPollExecutor.shutdownNow();
        }
        alarms.clear();
        log.info("Stopped");
    }

    @Modified
    public boolean modified(ComponentContext context) {
        log.info("context={}", context);
        if (context == null) {
            log.info("No configuration file");
            return false;
        }
        Dictionary<?, ?> properties = context.getProperties();
        String clearedAlarmPurgeSeconds = get(properties, "clearedAlarmPurgeSeconds");

        log.info("Settings: clearedAlarmPurgeSeconds={}", clearedAlarmPurgeSeconds);

        return true;
    }

    // Synchronised to prevent duplicate NE alarms being raised
    synchronized void updateAlarms(Set<Alarm> discoveredSet, DeviceId deviceId) {
        Set<Alarm> storedSet = getActiveAlarms(deviceId);
        log.trace("currentNeAlarms={}. discoveredAlarms={}", storedSet, discoveredSet);

        if (CollectionUtils.isEqualCollection(storedSet, discoveredSet)) {
            log.debug("Alarm lists are equivalent so no update for {}.", deviceId);
            return;
        }

        storedSet.stream().filter(
                (stored) -> (!discoveredSet.contains(stored))).forEach((stored) -> {
                    log.info("Alarm will be cleared as it is not on the element. Cleared alarm: {}.", stored);
                    clear(stored.id());
                });

        discoveredSet.stream().filter(
                (discovered) -> (!storedSet.contains(discovered))).forEach((discovered) -> {
            log.info("Alarm will be raised as it is missing. New alarm: {}.", discovered);
            AlarmId id = generateAlarmId();
            alarms.put(id, new DefaultAlarm.Builder(discovered).withId(id).build());
        });
    }

}
