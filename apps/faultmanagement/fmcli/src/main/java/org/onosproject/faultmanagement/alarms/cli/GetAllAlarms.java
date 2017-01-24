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
package org.onosproject.faultmanagement.alarms.cli;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Lists alarms across all devices.
 */
@Command(scope = "onos", name = "alarms",
        description = "Lists alarms")
public class GetAllAlarms extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--active", description = "Shows ACTIVE alarms only",
            required = false, multiValued = false)
    private boolean activeOnly = false;

    @Argument(index = 0, name = "deviceId", description = "Device identity",
            required = false, multiValued = false)
    String deviceId = null;

    private AlarmService alarmService = AbstractShellCommand.get(AlarmService.class);
    private Set<Alarm> alarms;

    @Override
    protected void execute() {
        if (deviceId != null) {
            if (activeOnly) {
                alarms = alarmService.getActiveAlarms(DeviceId.deviceId(deviceId));
            } else {
                alarms = alarmService.getAlarms(DeviceId.deviceId(deviceId));
            }
        } else if (activeOnly) {
            alarms = alarmService.getActiveAlarms();
        } else {
            alarms = alarmService.getAlarms();
        }
        printAlarms(alarms);
    }


    void printAlarms(Set<Alarm> alarms) {
        //FIXME this can be better formatted
        alarms.forEach((alarm) -> {
            print(ToStringBuilder.reflectionToString(alarm, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }
}
