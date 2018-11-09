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
package org.onosproject.faultmanagement.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.alarm.Alarm;

/**
 * Hamcrest matcher for alarms.
 */
public final class AlarmJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Alarm alarm;

    private AlarmJsonMatcher(Alarm alarm) {
        this.alarm = alarm;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonAlarm, Description description) {
        String jsonAlarmId = jsonAlarm.get("id").asText();
        String alarmId = alarm.id().toString();
        if (!jsonAlarmId.equals(alarmId)) {
            description.appendText("alarm id was " + jsonAlarmId);
            return false;
        }

        String jsonDeviceId = jsonAlarm.get("deviceId").asText();
        String alarmDeviceId = alarm.deviceId().toString();
        if (!jsonDeviceId.equals(alarmDeviceId)) {
            description.appendText("DeviceId was " + jsonDeviceId);
            return false;
        }


        String jsonDescription = jsonAlarm.get("description").asText();
        String alarmDesc = alarm.description();
        if (!jsonDescription.equals(alarmDesc)) {
            description.appendText("description was " + jsonDescription);
            return false;
        }

        long jsonTimeRaised = jsonAlarm.get("timeRaised").asLong();
        long timeRaised = alarm.timeRaised();
        if (timeRaised != jsonTimeRaised) {
            description.appendText("timeRaised was " + jsonTimeRaised);
            return false;
        }


        long jsonTimeUpdated = jsonAlarm.get("timeUpdated").asLong();
        long timeUpdated = alarm.timeUpdated();
        if (timeUpdated != jsonTimeUpdated) {
            description.appendText("timeUpdated was " + jsonTimeUpdated);
            return false;
        }

        JsonNode jsonTimeClearedNode = jsonAlarm.get("timeCleared");

        if (alarm.timeCleared() != null) {
            Long jsonTimeCleared = jsonTimeClearedNode.longValue();
            Long timeCleared = alarm.timeCleared();

            if (!timeCleared.equals(jsonTimeCleared)) {
                description.appendText("Time Cleared was " + jsonTimeCleared);
                return false;
            }
        } else {
            //  No clear time not specified, JSON representation must be empty
            if (!jsonTimeClearedNode.isNull()) {
                description.appendText("Time Cleared should be null");
                return false;
            }
        }

        String jsonSeverity = jsonAlarm.get("severity").asText();
        String severity = alarm.severity().toString();
        if (!severity.equals(jsonSeverity)) {
            description.appendText("severity was " + jsonSeverity);
            return false;
        }

        JsonNode jsonAlarmNode = jsonAlarm.get("source");

        if (alarm.source() != null) {
            String jsonSource = jsonAlarmNode.textValue();
            String source = alarm.source().toString();

            if (!source.equals(jsonSource)) {
                description.appendText("source was " + jsonSource);
                return false;
            }
        } else {
            //  source not specified, JSON representation must be empty
            if (!jsonAlarmNode.isNull()) {
                description.appendText("source should be null");
                return false;
            }
        }

        // In progress
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(alarm.toString());
    }

    /**
     * Factory to allocate a alarm matcher.
     *
     * @param alarm alarm object we are looking for
     * @return matcher
     */
    public static AlarmJsonMatcher matchesAlarm(Alarm alarm) {
        return new AlarmJsonMatcher(alarm);
    }
}
