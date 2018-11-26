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

package org.onosproject.faultmanagement.alarms.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.faultmanagement.alarms.cli.UpdateAlarm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI completer for Alarm Field values.
 */
@Service
public class AlarmFieldValueCompleter extends AbstractChoicesCompleter {
    @Override
    protected List<String> choices() {

        List<String> choices = new ArrayList<>();
        UpdateAlarm.AlarmField field = UpdateAlarm.AlarmField.valueOf(commandLine.getArguments()[2]);

        switch (field) {
            case ACKNOWLEDGED:
            case MANUALLY_CLEARABLE:
            case SERVICE_AFFECTING:
                choices.add("TRUE");
                choices.add("FALSE");
                return choices;
            case TIME_CLEARED:
            case TIME_UPDATED:
                choices.add(Instant.now().toString());
                return choices;
            default:
                return choices;
        }
    }
}
