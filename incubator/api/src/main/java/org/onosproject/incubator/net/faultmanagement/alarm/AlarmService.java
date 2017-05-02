/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.net.faultmanagement.alarm;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for interacting with the alarm handling of devices. Unless stated otherwise, getter methods
 * return active AND recently-cleared alarms.
 */
public interface AlarmService extends ListenerService<AlarmEvent, AlarmListener> {

    /**
     * Update book-keeping (ie administrative) fields for the alarm matching the specified identifier.
     *
     * @param id             alarm identifier
     * @param isAcknowledged new acknowledged state
     * @param assignedUser   new assigned user, null clear
     * @return updated alarm (including any recent device-derived changes)
     * @deprecated 1.10.0 Kingfisher
     */
    @Deprecated
    Alarm updateBookkeepingFields(AlarmId id, boolean isAcknowledged, String assignedUser);

    /**
     * Update book-keeping (ie administrative) fields for the alarm matching the specified identifier.
     *
     * @param id             alarm identifier
     * @param clear          ture if the alarm has to be cleared
     * @param isAcknowledged new acknowledged state
     * @param assignedUser   new assigned user, null clear
     * @return updated alarm (including any recent device-derived changes)
     */
    Alarm updateBookkeepingFields(AlarmId id, boolean clear, boolean isAcknowledged, String assignedUser);

    /**
     * Remove an alarm from ONOS.
     *
     * @param id alarm
     */
    void remove(AlarmId id);

    /**
     * Returns summary of alarms on a given device.
     *
     * @param deviceId the device
     * @return map of severity (if applicable) vs alarm counts; empty map if either the device has no alarms or
     * identified device is not managed.
     */
    Map<Alarm.SeverityLevel, Long> getAlarmCounts(DeviceId deviceId);

    /**
     * Returns summary of alarms on all devices.
     *
     * @return map of severity (if applicable) vs alarm counts; empty map if no alarms.
     */
    Map<Alarm.SeverityLevel, Long> getAlarmCounts();

    /**
     * Returns the alarm with the specified identifier.
     *
     * @param alarmId alarm identifier
     * @return alarm matching id; null if no alarm matches the identifier.
     */
    Alarm getAlarm(AlarmId alarmId);

    /**
     * Returns all of the alarms.
     *
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getAlarms();

    /**
     * Returns all of the ACTIVE alarms. Recently cleared alarms excluded.
     *
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getActiveAlarms();

    /**
     * Returns the alarms with the specified severity.
     *
     * @param severity the alarm severity
     * @return set of alarms with a particular severity; empty set if no alarms
     */
    Set<Alarm> getAlarms(Alarm.SeverityLevel severity);

    /**
     * Returns the alarm matching a given device, regardless of source within that device.
     *
     * @param deviceId the device to use when searching alarms.
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getAlarms(DeviceId deviceId);

    /**
     * Returns all of the ACTIVE alarms for a specific device. Recently cleared alarms excluded.
     *
     * @param deviceId the device to use when searching alarms.
     * @return set of alarms; empty set if no alarms
     */
    default Set<Alarm> getActiveAlarms(DeviceId deviceId) {
        return getActiveAlarms().stream()
                .filter(a -> deviceId.equals(a.deviceId()))
                .collect(Collectors.toSet());
    }

    /**
     * Returns the alarm for a given device and source.
     *
     * @param deviceId the device
     * @param source   the source within the device
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getAlarms(DeviceId deviceId, AlarmEntityId source);

    /**
     * Returns the alarm affecting a given link.
     *
     * @param src one end of the link
     * @param dst one end of the link
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getAlarmsForLink(ConnectPoint src, ConnectPoint dst);

    /**
     * Returns the alarm affecting a given flow.
     *
     * @param deviceId the device
     * @param flowId   the flow
     * @return set of alarms; empty set if no alarms
     */
    Set<Alarm> getAlarmsForFlow(DeviceId deviceId, long flowId);

    // TODO Support retrieving alarms affecting other entity types may be added in future release
}
