/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.key;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Describes device key events.
 */
@Beta
public class DeviceKeyEvent extends AbstractEvent<DeviceKeyEvent.Type, DeviceKey> {

    /**
     * Type of device key events.
     */
    public enum Type {
        /**
         * Signals that a new device key has been added.
         */
        DEVICE_KEY_ADDED,

        /**
         * Signals that a device key has been updated or changed state.
         */
        DEVICE_KEY_UPDATED,

        /**
         * Signals that a device key has been removed.
         */
        DEVICE_KEY_REMOVED
    }

    /**
     * Creates an event of a given type, and for the specified device key.
     *
     * @param type      device key event type
     * @param deviceKey event device key subject
     */
    public DeviceKeyEvent(Type type, DeviceKey deviceKey) {
        super(type, deviceKey);
    }

    /**
     * Creates an event of a given type, for the specified device key, and
     * the current time.
     *
     * @param type      device key event type
     * @param deviceKey event device key subject
     * @param time      occurrence time
     */
    public DeviceKeyEvent(Type type, DeviceKey deviceKey, long time) {
        super(type, deviceKey, time);
    }
}
