/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.faultmanagement.alarms.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.faultmanagement.api.AlarmStore;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.time.Instant;

/**
 * Creates a default alarm on a device.
 */
@Service
@Command(scope = "onos", name = "alarm-create",
        description = "Creates an alarm")
public class CreateAlarm extends AbstractShellCommand {

    @Argument(index = 0, name = "deviceId", description = "Device identity",
            required = true, multiValued = false)
    String deviceIdStr = null;

    @Argument(index = 1, name = "severity", description = "Severity level",
            required = true, multiValued = false)
    String severityStr = null;

    @Argument(index = 2, name = "alarmId", description = "Unique alarm id",
            required = true, multiValued = false)
    String alarmId = null;

    @Argument(index = 3, name = "desc", description = "Alarm description",
            required = true, multiValued = false)
    String desc = null;

    private AlarmStore alarmStore = AbstractShellCommand.get(AlarmStore.class);

    private DeviceService deviceManager = AbstractShellCommand.get(DeviceService.class);

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        if (!deviceManager.isAvailable(deviceId)) {
            throw new IllegalArgumentException("Device " + deviceIdStr + " is not available");
        }

        Alarm.SeverityLevel severityLevel = Alarm.SeverityLevel.valueOf(severityStr);

        Alarm newAlarm = new DefaultAlarm.Builder(
                AlarmId.alarmId(deviceId, alarmId),
                deviceId,
                desc,
                severityLevel,
                Instant.now().toEpochMilli())
            .build();
        alarmStore.createOrUpdateAlarm(newAlarm);
    }
}
