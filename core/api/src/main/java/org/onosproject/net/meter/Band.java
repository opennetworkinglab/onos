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

/**
 * Represents a band used within a meter.
 */
public interface Band {

    short MIN_PRECEDENCE = 0;
    short MAX_PRECEDENCE = 255;
    String ERR_MSG = "Precedence out of range";

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
         * the drop precedence of the DSCP field in the
         * IP header of the packets that exceed the band
         * rate value.
         */
        REMARK,

        /**
         * defines an experimental meter band.
         */
        EXPERIMENTAL
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
    Long burst();

    /**
     * Only meaningful in the case of a REMARK band type.
     * indicates by which amount the drop precedence of
     * the packet should be increase if the band is exceeded.
     *
     * @return a short value
     */
    Short dropPrecedence();

    /**
     * Signals the type of band to create.
     *
     * @return a band type
     */
    Type type();

    /**
     * Returns the packets seen by this band.
     *
     * @return a long value
     */
    long packets();

    /**
     * Return the bytes seen by this band.
     *
     * @return a byte counter
     */
    long bytes();

    interface Builder {

        /**
         * Assigns a rate to this band. The units for this rate
         * are defined in the encapsulating meter.
         *
         * @param rate a long value
         * @return this
         */
        Builder withRate(long rate);

        /**
         * Assigns a burst size to this band. Only meaningful if
         * the encapsulating meter is of burst type.
         *
         * @param burstSize a long value.
         * @return this
         */
        Builder burstSize(long burstSize);

        /**
         * Assigns the drop precedence for this band. Only meaningful if
         * the band is of REMARK type.
         *
         * @param prec a short value
         * @return this
         */
        Builder dropPrecedence(short prec);

        /**
         * Assigns the {@link Type} of this band.
         *
         * @param type a band type
         * @return this
         */
        Builder ofType(Type type);

        /**
         * Builds the band.
         *
         * @return a band
         */
        Band build();

    }


}
