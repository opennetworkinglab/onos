/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.driver;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Driver configuration change event.
 */
public class DriverEvent extends AbstractEvent<DriverEvent.Type, Driver> {

    /**
     * Type of driver events.
     */
    public enum Type {
        /**
         * Signifies that the driver configuration has changed in an additive
         * manner. Either new behaviours were added, their implementations
         * changed, or there is a new driver entirely.
         */
        DRIVER_ENHANCED,

        /**
         * Signifies that the driver configuration has been reduced in some way.
         * Either behaviours or their implementations were withdrawn or the
         * driver was removed entirely.
         */
        DRIVER_REDUCED
    }

    /**
     * Creates an event of a given type and for the specified driver and the
     * current time.
     *
     * @param type   device event type
     * @param driver event driver subject
     */
    public DriverEvent(Type type, Driver driver) {
        super(type, driver);
    }

    /**
     * Creates an event of a given type and for the specified driver and time.
     *
     * @param type   device event type
     * @param driver event driver subject
     * @param time   occurrence time
     */
    public DriverEvent(Type type, Driver driver, long time) {
        super(type, driver, time);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .toString();
    }
}
