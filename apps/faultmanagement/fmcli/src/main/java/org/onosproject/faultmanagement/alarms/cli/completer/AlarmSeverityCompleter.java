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

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.alarm.Alarm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CLI completer for Alarm Severity.
 */
public class AlarmSeverityCompleter extends AbstractChoicesCompleter {
    @Override
    public List<String> choices() {
        List<String> severityList = new ArrayList<>();

        Arrays.asList(Alarm.SeverityLevel.values()).forEach(s -> severityList.add(s.toString()));

        return severityList;
    }
}
