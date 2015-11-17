/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.annotations.Beta;
//import org.onosproject.event.ListenerService;

import java.util.Set;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

/**
 * Service for interacting with the alarm handling of devices. Unless stated
 * otherwise method return active AND recently-cleared alarms.
 */
@Beta
public interface AlarmService {
// extends ListenerService<AlarmEvent, AlarmListener> {

    /**
     * Alarm should be updated in ONOS's internal representation; only
     * administration/book-keeping fields may be updated. Attempting to update
     * fields which are mapped directly from device is prohibited.
     *
     * @param replacement alarm with updated book-keeping fields
     * @return updated alarm (including any recent device derived changes)

     * @throws java.lang.IllegalStateException if attempt to update not allowed
     * fields.
     */
    Alarm update(Alarm replacement);

    /**
     * Returns the number of ACTIVE alarms on a device.
     *
     * @param deviceId the device
     * @return number of alarms
     */
    int getActiveAlarmCount(DeviceId deviceId);

    /**
     * Returns the alarm with the specified identifier.
     *
     * @param alarmId alarm identifier
     * @return alarm or null if one with the given identifier is not known
     */
    Alarm getAlarm(AlarmId alarmId);

    /**
     * Returns all of the alarms.
     *
     * @return the alarms
     */
    Set<Alarm> getAlarms();

    /**
     * Returns all of the ACTIVE alarms. Recently cleared alarms excluded.
     *
     * @return the alarms
     */
    Set<Alarm> getActiveAlarms();

    /**
     * Returns the alarms with the specified severity.
     *
     * @param severity the alarm severity
     * @return the active alarms with a particular severity
     */
    Set<Alarm> getAlarms(Alarm.SeverityLevel severity);

    /**
     * Returns the alarm for a given device, regardless of source within that
     * device.
     *
     * @param deviceId the device
     * @return the alarms
     */
    Set<Alarm> getAlarms(DeviceId deviceId);

    /**
     * Returns the alarm for a given device and source.
     *
     * @param deviceId the device
     * @param source the source within the device
     * @return the alarms
     */
    Set<Alarm> getAlarms(DeviceId deviceId, AlarmEntityId source);

    /**
     * Returns the alarm affecting a given link.
     *
     * @param src one end of the link
     * @param dst one end of the link
     * @return the alarms
     */
    Set<Alarm> getAlarmsForLink(ConnectPoint src, ConnectPoint dst);

    /**
     * Returns the alarm affecting a given flow.
     *
     * @param deviceId the device
     * @param flowId the flow
     * @return the alarms
     */
    Set<Alarm> getAlarmsForFlow(DeviceId deviceId, long flowId);

// Support retrieving alarms affecting other ONOS entity types may be added in future release
}
