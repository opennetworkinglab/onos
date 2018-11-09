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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.faultmanagement.api.AlarmStore;
import org.onosproject.alarm.AlarmId;

/**
 * Remove an alarm from the Alarm Store.
 */
@Command(scope = "onos", name = "alarm-remove",
        description = "Removes an alarm")
public class RemoveAlarm extends AbstractShellCommand {
    @Argument(index = 0, name = "alarmId", description = "Unique alarm id",
            required = true, multiValued = false)
    String alarmId = null;

    private AlarmStore alarmStore = AbstractShellCommand.get(AlarmStore.class);

    @Override
    protected void doExecute() {
        alarmStore.removeAlarm(AlarmId.alarmId(alarmId));
    }
}
