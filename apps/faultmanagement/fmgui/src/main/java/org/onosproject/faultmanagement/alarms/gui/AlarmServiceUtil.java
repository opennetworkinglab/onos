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
package org.onosproject.faultmanagement.alarms.gui;

import java.util.Map;
import java.util.Set;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.onosproject.net.DeviceId;

/**
 *
 * Utility for invoking on alarm service.
 */
public final class AlarmServiceUtil {

    static Alarm lookupAlarm(AlarmId alarmId) {
        return alarmService().getAlarm(alarmId);
    }

    static Set<Alarm> lookUpAlarms() {
        return alarmService().getAlarms();
    }

    static Set<Alarm> lookUpAlarms(DeviceId deviceId) {
        return alarmService().getAlarms(deviceId);
    }

    static Map<Alarm.SeverityLevel, Long> lookUpAlarmCounts(DeviceId deviceId) {
        return alarmService().getAlarmCounts(deviceId);
    }

    static Map<Alarm.SeverityLevel, Long> lookUpAlarmCounts() {
        return alarmService().getAlarmCounts();
    }

    private static AlarmService alarmService() {
        return AbstractShellCommand.get(AlarmService.class);
    }

    private AlarmServiceUtil() {
    }
}
