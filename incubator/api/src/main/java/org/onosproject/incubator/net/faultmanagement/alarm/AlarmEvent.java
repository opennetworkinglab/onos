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
package org.onosproject.incubator.net.faultmanagement.alarm;

import org.onosproject.event.AbstractEvent;

/**
 * Entity that represents Alarm events. Note: although the event will itself have a time,
 * consumers may be more interested in the times embedded in the alarms themselves.
 */
public class AlarmEvent extends AbstractEvent<AlarmEvent.Type, Alarm> {

    /**
     * Type of alarm event.
     */
    public enum Type {

        /**
         * Individual alarm updated.
         */
        CREATED,
        /**
         * Alarm set updated for a given device.
         */
        REMOVED,
    }

    /**
     * Creates an event due to one alarm.
     *
     * @param type alarm type
     * @param alarm the alarm related to the event.
     */
    public AlarmEvent(AlarmEvent.Type type, Alarm alarm) {
        super(type, alarm);
    }

}
