/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onlab.util.ItemNotFoundException;
import org.onosproject.faultmanagement.api.AlarmStore;
import org.onosproject.faultmanagement.api.AlarmStoreDelegate;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmEntityId;
import org.onosproject.alarm.AlarmEvent;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.AlarmListener;
import org.onosproject.alarm.AlarmProvider;
import org.onosproject.alarm.AlarmProviderRegistry;
import org.onosproject.alarm.AlarmProviderService;
import org.onosproject.alarm.AlarmService;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Alarm service.
 */
@Component(immediate = true, service = { AlarmService.class, AlarmProviderRegistry.class })
public class AlarmManager
        extends AbstractListenerProviderRegistry<AlarmEvent, AlarmListener, AlarmProvider, AlarmProviderService>
        implements AlarmService, AlarmProviderRegistry {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AlarmStore store;

    protected AlarmStoreDelegate delegate = this::post;

    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    //TODO improve implementation of AlarmId
    private final AtomicLong alarmIdGenerator = new AtomicLong(0);

    private static final String NOT_SUPPORTED_YET = "Not supported yet.";

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(AlarmEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(AlarmEvent.class);
        log.info("Stopped");
    }

    @Override
    public Alarm updateBookkeepingFields(AlarmId id, boolean clear, boolean isAcknowledged,
                                         String assignedUser) {
        checkNotNull(id, "Alarm id is null");
        Alarm found = store.getAlarm(id);
        if (found == null) {
            throw new ItemNotFoundException("Alarm with id " + id + " found");
        }
        long now = System.currentTimeMillis();
        DefaultAlarm.Builder alarmBuilder = new DefaultAlarm.Builder(found).withTimeUpdated(now);
        if (found.cleared() != clear) {
            alarmBuilder.clear().withTimeCleared(now);
        }
        if (found.acknowledged() != isAcknowledged) {
            alarmBuilder.withAcknowledged(isAcknowledged);
        }
        if (assignedUser != null && !found.assignedUser().equals(assignedUser)) {
            alarmBuilder.withAssignedUser(assignedUser);
        }
        DefaultAlarm updated = alarmBuilder.build();
        store.createOrUpdateAlarm(updated);
        return updated;
    }

    //TODO move to AlarmAdminService
    @Override
    public void remove(AlarmId id) {
        checkNotNull(id, "Alarm id is null");
        store.removeAlarm(id);
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
        return nullIsNotFound(store.getAlarm(checkNotNull(alarmId, "Alarm Id cannot be null")),
                              "Alarm is not found");
    }

    @Override
    public Set<Alarm> getAlarms() {
        return ImmutableSet.copyOf(store.getAlarms());
    }

    @Override
    public Set<Alarm> getActiveAlarms() {
        return ImmutableSet.copyOf(store.getAlarms().stream().filter(
                a -> !a.severity().equals(Alarm.SeverityLevel.CLEARED)).
                collect(Collectors.toSet()));
    }

    @Override
    public Set<Alarm> getAlarms(Alarm.SeverityLevel severity) {
        return ImmutableSet.copyOf(store.getAlarms().stream().filter(
                a -> a.severity().equals(severity)).
                collect(Collectors.toSet()));
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId) {
        return ImmutableSet.copyOf(store.getAlarms(deviceId));
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId, AlarmEntityId source) {
        return ImmutableSet.copyOf(getAlarms(deviceId).stream().filter(
                a -> source.equals(a.source())).collect(Collectors.toSet()));
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

    private class InternalAlarmProviderService extends AbstractProviderService<AlarmProvider>
            implements AlarmProviderService {

        InternalAlarmProviderService(AlarmProvider provider) {
            super(provider);
        }

        @Override
        public void updateAlarmList(DeviceId deviceId, Collection<Alarm> alarms) {
            alarms.forEach(alarm -> store.createOrUpdateAlarm(alarm));
        }
    }

    /**
     * Internal listener for device events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type().equals(DeviceEvent.Type.DEVICE_REMOVED);
        }

        @Override
        public void event(DeviceEvent event) {
            if (mastershipService.isLocalMaster(event.subject().id())) {
                log.info("Device {} removed from ONOS, removing all related alarms", event.subject().id());
                //TODO this can be improved when core supports multiple keys map and gets implemented in AlarmStore
                store.getAlarms(event.subject().id()).forEach(alarm -> store.removeAlarm(alarm.id()));
            } else {
                log.info("This Node is not Master for device {}", event.subject().id());
            }
        }

    }
}
