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
package org.onosproject.net.meter;

import org.onosproject.event.AbstractEvent;

/**
 * Entity that represents Meter events.
 */
public class MeterEvent extends AbstractEvent<MeterEvent.Type, Meter> {


    public enum Type {
        /**
         * A meter addition was requested.
         */
        METER_ADD_REQ,

        /**
         * A meter removal was requested.
         */
        METER_REM_REQ,

        /**
         * A meter was finally added to device.
         */
        METER_ADDED,

        /**
         * A meter was finally removed from device.
         */
        METER_REMOVED
    }


    /**
     * Creates an event of a given type and for the specified meter and the
     * current time.
     *
     * @param type  meter event type
     * @param meter event subject
     */
    public MeterEvent(Type type, Meter meter) {
        super(type, meter);
    }

    /**
     * Creates an event of a given type and for the specified meter and time.
     *
     * @param type  meter event type
     * @param meter event subject
     * @param time  occurrence time
     */
    public MeterEvent(Type type, Meter meter, long time) {
        super(type, meter, time);
    }


}
