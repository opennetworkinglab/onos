/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.faultmanagement.api;

import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmEvent;
import org.onosproject.alarm.AlarmId;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import java.util.Collection;

/**
 * Manages inventory of alarms; not intended for direct use.
 */
public interface AlarmStore extends Store<AlarmEvent, AlarmStoreDelegate> {

    /**
     * Retrieves and alarm based on it's id.
     *
     * @param alarmId alarm identifier
     * @return alarm
     */
    Alarm getAlarm(AlarmId alarmId);

    /**
     * Retrieves all alarms present in the system.
     *
     * @return alarms
     */
    Collection<Alarm> getAlarms();

    /**
     * Retrieves alarms for a device.
     *
     * @param deviceId device identifier
     * @return alarms
     */
    Collection<Alarm> getAlarms(DeviceId deviceId);

    /**
     * Stores or updates an alarm.
     *
     * @param alarm alarm
     */

    void createOrUpdateAlarm(Alarm alarm);

    /**
     * Removes an alarm.
     *
     * @param alarmId alarm
     */
    void removeAlarm(AlarmId alarmId);
}
