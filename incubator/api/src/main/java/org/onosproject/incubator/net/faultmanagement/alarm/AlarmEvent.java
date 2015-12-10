/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.Set;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

/**
 * Entity that represents Alarm events. Note: although the event will itself have a time, consumers may be more
 * interested in the times embedded in the alarms themselves.
 *
 */
public class AlarmEvent extends AbstractEvent<AlarmEvent.Type, Set<Alarm>> {

    private final DeviceId deviceRefreshed;

    /**
     * Creates an event due to one or more notification.
     *
     * @param alarms the set one or more of alarms.
     */
    public AlarmEvent(Set<Alarm> alarms) {
        super(Type.NOTIFICATION, alarms);
        deviceRefreshed = null;
    }

    /**
     * Creates an event due to alarm discovery for a device.
     *
     * @param alarms the set of alarms.
     * @param deviceRefreshed if of refreshed device, populated after a de-discovery
     */
    public AlarmEvent(Set<Alarm> alarms,
            DeviceId deviceRefreshed) {
        super(Type.DEVICE_DISCOVERY, alarms);
        this.deviceRefreshed = deviceRefreshed;

    }

    /**
     * Gets which device was refreshed.
     *
     * @return the refreshed device, or null if event related to a asynchronous notification(s)
     */
    public DeviceId getDeviceRefreshed() {
        return deviceRefreshed;
    }

    /**
     * Type of alarm event.
     */
    public enum Type {

        /**
         * Individual alarm(s) updated.
         */
        NOTIFICATION,
        /**
         * Alarm set updated for a given device.
         */
        DEVICE_DISCOVERY,
    }

}
