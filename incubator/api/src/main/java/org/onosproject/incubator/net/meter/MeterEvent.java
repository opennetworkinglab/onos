/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.meter;

import org.onosproject.event.AbstractEvent;

/**
 * Entity that represents Meter events.
 */
public class MeterEvent extends AbstractEvent<MeterEvent.Type, MeterOperation> {


    private final MeterFailReason reason;

    public enum Type {

        /**
         * Signals that a meter has been added.
         */
        METER_UPDATED,

        /**
         * Signals that a meter update failed.
         */
        METER_OP_FAILED,

        /**
         * A meter operation was requested.
         */
        METER_OP_REQ,

    }


    /**
     * Creates an event of a given type and for the specified meter and the
     * current time.
     *
     * @param type  meter event type
     * @param op event subject
     */
    public MeterEvent(Type type, MeterOperation op) {
        super(type, op);
        this.reason = null;
    }

    /**
     * Creates an event of a given type and for the specified meter and time.
     *
     * @param type  meter event type
     * @param op event subject
     * @param time  occurrence time
     */
    public MeterEvent(Type type, MeterOperation op, long time) {
        super(type, op, time);
        this.reason = null;
    }

    public MeterEvent(Type type, MeterOperation op, MeterFailReason reason) {
        super(type, op);
        this.reason = reason;
    }

    public MeterFailReason reason() {
        return reason;
    }

}
