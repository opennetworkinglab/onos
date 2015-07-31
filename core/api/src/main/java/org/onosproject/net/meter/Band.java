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
package org.onosproject.net.meter;

/**
 * Represents a band used within a meter.
 */
public interface Band {

    /**
     * Specifies the type of band.
     */
    enum Type {
        /**
         * Simple rate limiter which drops packets
         * when the rate is exceeded.
         */
        DROP,

        /**
         * defines a simple DiffServ policer that remark
         * the drop precedence of the DSCP field in the
         * IP header of the packets that exceed the band
         * rate value.
         */
        REMARK
    }

    /**
     * The rate at which this meter applies.
     *
     * @return the long value of the rate
     */
    long rate();

    /**
     * The burst size at which the meter applies.
     *
     * @return the long value of the size
     */
    long burst();

    /**
     * Only meaningful in the case of a REMARK band type.
     * indicates by which amount the drop precedence of
     * the packet should be increase if the band is exceeded.
     *
     * @return a short value
     */
    short dropPrecedence();

    /**
     * Signals the type of band to create.
     *
     * @return a band type
     */
    Type type();


}
