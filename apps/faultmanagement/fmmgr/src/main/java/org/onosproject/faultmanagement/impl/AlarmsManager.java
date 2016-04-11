/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderRegistry;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderService;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Alarm service.
 */
@Component(immediate = true)
@Service
public class AlarmsManager
        extends AbstractProviderRegistry<AlarmProvider, AlarmProviderService>
        implements AlarmService, AlarmProviderRegistry {

    private final Logger log = getLogger(getClass());

    private final AtomicLong alarmIdGenerator = new AtomicLong(0);

    // TODO Later should must be persisted to disk or database
    protected final Map<AlarmId, Alarm> alarms = new ConcurrentHashMap<>();

    private static final String NOT_SUPPORTED_YET = "Not supported yet.";

    @Activate
    public void activate(ComponentContext context) {
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        alarms.clear();
        log.info("Stopped");
    }

    @Modified
    public boolean modified(ComponentContext context) {
        log.info("Modified");
        return true;
    }

    private AlarmId generateAlarmId() {
        return AlarmId.alarmId(alarmIdGenerator.incrementAndGet());
    }

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

    @Override
    public Alarm getAlarm(AlarmId alarmId) {
        return nullIsNotFound(alarms.get(checkNotNull(alarmId, "Alarm Id cannot be null")),
                              "Alarm is not found");
    }

    @Override
    public Set<Alarm> getAlarms() {
        return ImmutableSet.copyOf(alarms.values());
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
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public Set<Alarm> getAlarmsForFlow(DeviceId deviceId, long flowId) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    protected AlarmProviderService createProviderService(AlarmProvider provider) {
        return new InternalAlarmProviderService(provider);
    }

    // Synchronised to prevent duplicate NE alarms being raised
    protected synchronized void updateAlarms(DeviceId deviceId, Set<Alarm> discoveredSet) {
        Set<Alarm> storedSet = getActiveAlarms(deviceId);
        log.trace("currentNeAlarms={}. discoveredAlarms={}", storedSet, discoveredSet);

        if (CollectionUtils.isEqualCollection(storedSet, discoveredSet)) {
            log.debug("Alarm lists are equivalent so no update for {}.", deviceId);
            return;
        }

        storedSet.stream().filter(
                (stored) -> (!discoveredSet.contains(stored))).forEach((stored) -> {
            log.debug("Alarm will be cleared as it is not on the element. Cleared alarm: {}.", stored);
            clear(stored.id());
        });

        discoveredSet.stream().filter(
                (discovered) -> (!storedSet.contains(discovered))).forEach((discovered) -> {
            log.info("New alarm raised as it is missing. New alarm: {}.", discovered);
            AlarmId id = generateAlarmId();
            alarms.put(id, new DefaultAlarm.Builder(discovered).withId(id).build());
        });
    }

    private class InternalAlarmProviderService
            extends AbstractProviderService<AlarmProvider>
            implements AlarmProviderService {

        InternalAlarmProviderService(AlarmProvider provider) {
            super(provider);


        }

        @Override
        public void updateAlarmList(DeviceId deviceId, Collection<Alarm> alarms) {
            updateAlarms(deviceId, ImmutableSet.copyOf(alarms));
        }
    }
}
