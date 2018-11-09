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
import org.onosproject.alarm.AlarmEntityId;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;

import java.time.Instant;

/**
 * Updates an existing alarm.
 */
@Command(scope = "onos", name = "alarm-update",
        description = "Updates an alarm")
@Service
public class UpdateAlarm extends AbstractShellCommand {

    @Argument(index = 0, name = "alarmId", description = "Unique alarm id",
            required = true, multiValued = false)
    String alarmId = null;

    @Argument(index = 1, name = "desc", description = "Alarm field",
            required = true, multiValued = false)
    String alarmField = null;

    @Argument(index = 2, name = "value", description = "The new value of the chosen Alarm field.",
            required = true, multiValued = false)
    String alarmFieldValue = null;

    private AlarmStore alarmStore = AbstractShellCommand.get(AlarmStore.class);

    @Override
    protected void doExecute() {
        Alarm existing = alarmStore.getAlarm(AlarmId.alarmId(alarmId));

        DefaultAlarm.Builder newAlarmBuilder = new DefaultAlarm.Builder(existing);
        UpdateAlarm.AlarmField field = UpdateAlarm.AlarmField.valueOf(alarmField);
        switch (field) {
            case SOURCE:
                AlarmEntityId sourceId = AlarmEntityId.alarmEntityId(alarmFieldValue);
                newAlarmBuilder.forSource(sourceId);
                break;
            case ASSIGNED_USER:
                newAlarmBuilder.withAssignedUser(alarmFieldValue);
                break;
            case ACKNOWLEDGED:
                newAlarmBuilder.withAcknowledged("TRUE".equalsIgnoreCase(alarmFieldValue));
                break;
            case MANUALLY_CLEARABLE:
                newAlarmBuilder.withManuallyClearable("TRUE".equalsIgnoreCase(alarmFieldValue));
                break;
            case SERVICE_AFFECTING:
                newAlarmBuilder.withServiceAffecting("TRUE".equalsIgnoreCase(alarmFieldValue));
                break;
            case TIME_CLEARED:
                newAlarmBuilder.clear();
                newAlarmBuilder.withTimeCleared(Instant.parse(alarmFieldValue).toEpochMilli());
                break;
            case TIME_UPDATED:
            default:
                newAlarmBuilder.withTimeUpdated(Instant.parse(alarmFieldValue).toEpochMilli());
                break;
        }
        alarmStore.createOrUpdateAlarm(newAlarmBuilder.build());
    }

    public enum AlarmField {
        SOURCE,
        ASSIGNED_USER,
        ACKNOWLEDGED,
        MANUALLY_CLEARABLE,
        SERVICE_AFFECTING,
        TIME_CLEARED,
        TIME_UPDATED
    }
}
