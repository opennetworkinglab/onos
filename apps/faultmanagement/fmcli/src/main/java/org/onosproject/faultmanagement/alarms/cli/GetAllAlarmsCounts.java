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

import java.util.Map;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;

/**
 * Lists alarm counts across all devices.
 */
@Command(scope = "onos", name = "alarm-counts",
        description = "Lists alarm counts across all devices.")
public class GetAllAlarmsCounts extends AbstractShellCommand {

    @Override
    protected void execute() {
        Map<Alarm.SeverityLevel, Long> alarmCounts
                = AbstractShellCommand.get(AlarmService.class).getAlarmCounts();
        printCounts(alarmCounts);
    }

    void printCounts(Map<Alarm.SeverityLevel, Long> alarmCounts) {
        alarmCounts.entrySet().forEach((countEntry) -> {
            print(String.format("%s, %d",
                    countEntry.getKey(), countEntry.getValue()));

        });
    }
}
