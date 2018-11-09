/*
 * Copyright 2015-present Open Networking Foundation
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

import static java.util.Comparator.comparingInt;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmService;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lists alarm counts across all devices.
 */
@Service
@Command(scope = "onos", name = "alarms-counts",
        description = "Lists the count of alarms for each severity")
public class GetAllAlarmsCounts extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--active", description = "Shows ACTIVE alarms only",
            required = false, multiValued = false)
    private boolean activeOnly = false;

    @Argument(index = 0, name = "deviceId", description = "Device identity",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceId = null;

    private AlarmService alarmService = AbstractShellCommand.get(AlarmService.class);
    private Map<Alarm.SeverityLevel, Long> alarmCounts;

    @Override
    protected void doExecute() {
        if (deviceId != null) {
            if (activeOnly) {
                alarmCounts = alarmService.getActiveAlarms(DeviceId.deviceId(deviceId))
                        .stream().filter(a -> !a.severity().equals(Alarm.SeverityLevel.CLEARED))
                        .collect(Collectors.groupingBy(Alarm::severity, Collectors.counting()));
            } else {
                alarmCounts = alarmService.
                        getAlarmCounts(DeviceId.deviceId(deviceId));
            }
        } else if (activeOnly) {
            alarmCounts = alarmService.getActiveAlarms()
                    .stream().filter(a -> !a.severity().equals(Alarm.SeverityLevel.CLEARED))
                    .collect(Collectors.groupingBy(Alarm::severity, Collectors.counting()));
        } else {
            alarmCounts = alarmService.getAlarmCounts();
        }
        printCounts(alarmCounts);
    }

    void printCounts(Map<Alarm.SeverityLevel, Long> alarmCounts) {
        alarmCounts.entrySet().stream()
            .sorted(comparingInt(e -> e.getKey().ordinal()))
            .forEach((countEntry) -> {
            print(String.format("%s, %d", countEntry.getKey(), countEntry.getValue()));
        });
    }
}
