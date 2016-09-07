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

import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;

/**
 * Lists active alarms across all devices.
 */
@Command(scope = "onos", name = "alarm-list-active",
        description = "Lists all the ACTIVE alarms across all devices.")
public class GetAllActiveAlarms extends AbstractShellCommand {


    @Override
    protected void execute() {
        printAlarms(AbstractShellCommand.get(AlarmService.class).getActiveAlarms());
    }

    void printAlarms(Set<Alarm> alarms) {
        alarms.forEach((alarm) -> {
            print(ToStringBuilder.reflectionToString(alarm, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }
}
