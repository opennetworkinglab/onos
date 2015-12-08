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

import static com.google.common.base.Strings.isNullOrEmpty;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.osgi.service.component.ComponentContext;

/**
 * Implementation of the Alarm service.
 */
@Component(immediate = true)
@Service
public class AlarmsManager implements AlarmService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    private final Logger log = getLogger(getClass());
    private ApplicationId appId;
    private IdGenerator idGenerator;


    @Property(name = "fmDevices", value = "127.0.0.1", label = "Instance-specific configurations")
    private String devConfigs;

    private final Map<AlarmId, Alarm> alarms = new ConcurrentHashMap<>();


    private final AtomicLong alarmIdGenerator = new AtomicLong(0);

    @Override
    public Alarm update(Alarm replacement) {

        final Alarm found = alarms.get(replacement.id());
        if (found == null) {
            throw new ItemNotFoundException("Alarm with id " + replacement.id() + " found");
        }
        final Alarm updated = new DefaultAlarm.Builder(found).
                withAcknowledged(replacement.acknowledged()).
                withAssignedUser(replacement.assignedUser()).build();
        alarms.put(replacement.id(), updated);
        return updated;
    }

    @Override
    public int getActiveAlarmCount(DeviceId deviceId) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }
    private static final String NOT_SUPPORTED_YET = "Not supported yet.";

    @Override
    public Alarm getAlarm(AlarmId alarmId) {
        return nullIsNotFound(
                alarms.get(alarmId),
                "Alarm is not found");
    }

    @Override
    public Set<Alarm> getAlarms() {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public Set<Alarm> getActiveAlarms() {
        // Enpty set if no values
        return alarms.isEmpty() ? new HashSet<>() : new HashSet<>(alarms.values());

    }

    private static DefaultAlarm generateFake(DeviceId deviceId, AlarmId alarmId) {

        return new DefaultAlarm.Builder(
                alarmId, deviceId, "NE is not reachable", Alarm.SeverityLevel.MAJOR, System.currentTimeMillis()).
                withTimeUpdated(System.currentTimeMillis()).
                withServiceAffecting(true)
                .withAcknowledged(true).
                withManuallyClearable(true)
                .withAssignedUser("user1").build();
    }

    @Override
    public Set<Alarm> getAlarms(Alarm.SeverityLevel severity) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public Set<Alarm> getAlarms(DeviceId deviceId, AlarmEntityId source) {
        //TODO
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
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

    private void discoverAlarmsForDevice(DeviceId deviceId) {
        final AlarmId alarmId = new AlarmId(alarmIdGenerator.incrementAndGet());

        // TODO In a new thread invoke SNMP Provider with DeviceId and device type and when done update our of alarms
        //
        alarms.put(alarmId, generateFake(deviceId, alarmId));

    }

    private class InternalAlarmListener implements AlarmListener {

        @Override
        public void event(AlarmEvent event) {
            // TODO
            throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        }
    }

    @Activate
    public void activate(ComponentContext context) {
        log.info("Activate ...");
        appId = coreService.registerApplication("org.onos.faultmanagement.alarms");
        idGenerator = coreService.getIdGenerator("alarm-ids");
        log.info("Started with appId={} idGenerator={}", appId, idGenerator);

        final boolean result = modified(context);
        log.info("modified result = {}", result);

    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        log.info("Deactivate ...");
        //     cfgService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    public boolean modified(ComponentContext context) {
        log.info("context={}", context);
        if (context == null) {
            log.info("No configuration file");
            return false;
        }
        final Dictionary<?, ?> properties = context.getProperties();
        final String ipaddresses = get(properties, "fmDevices");
        log.info("Settings: devConfigs={}", ipaddresses);
        if (!isNullOrEmpty(ipaddresses)) {
            discover(ipaddresses);

        }
        return true;
    }

    private void discover(String ipaddresses) {
        for (String deviceEntry : ipaddresses.split(",")) {
            final DeviceId deviceId = DeviceId.deviceId(deviceEntry);
            if (deviceId != null) {
                log.info("Device {} needs to have its alarms refreshed!", deviceId);
                discoverAlarmsForDevice(deviceId);
            }
        }
    }

}
