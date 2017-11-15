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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;

/**
 * Generic abstraction for a token bucket which can mark and/or discard
 * traffic. Each token bucket in ONOS is made up of a set of attributes which
 * identifies the type.
 */
@Beta
public interface TokenBucket {

    /**
     * Upper bound for DSCP.
     */
    short MAX_DSCP = 255;
    /**
     * Lower bound for DSCP.
     */
    short MIN_DSCP = 0;

    /**
     * Token bucket type.
     */
    enum Type {
        /**
         * Committed rate.
         */
        COMMITTED,
        /**
         * Excess rate.
         */
        EXCESS,
        /**
         * Peak rate.
         */
        PEAK
    }

    /**
     * Action applied to the exceeding traffic.
     * Action in general depends on the token bucket type.
     */
    enum Action {
        /**
         * Drop action.
         */
        DROP,
        /**
         * Marking increases DSCP drop precedence.
         */
        DSCP_PRECEDENCE,
        /**
         * Marking sets DSCP class.
         */
        DSCP_CLASS,
        /**
         * Marking sets Drop Elegible Indicator.
         */
        DEI
    }

    /**
     * Rate of traffic subject to the SLAs
     * specified for this token bucket.
     *
     * @return the rate value
     */
    long rate();

    /**
     * Maximum burst size subject to the SLAs
     * specified for this token bucket.
     *
     * @return the burst size in bytes
     */
    long burstSize();

    /**
     * Action used by this token bucket
     * for the exceeding traffic.
     *
     * @return the type of action
     */
    Action action();

    /**
     * Dscp value, it meaning depends on the used marking.
     *
     * @return the dscp value for this token bucket
     */
    short dscp();

    /**
     * Token bucket type.
     *
     * @return the token bucket type
     */
    Type type();

    /**
     * Stats which reports how many packets have been
     * processed so far.
     *
     * Availability of this information depends on the
     * technology used for the implementation of the policer.
     *
     * @return the processed packets
     */
    long processedPackets();

    /**
     * Stats which reports how many bytes have been
     * processed so far.
     *
     * Availability of this information depends on the
     * technology used for the implementation of the policer.
     *
     * @return the processed bytes
     */
    long processedBytes();

    /**
     * Token bucket builder.
     */
    interface Builder {

        /**
         * Assigns the rate to this token bucket.
         *
         * @param rate a rate value
         * @return this
         */
        Builder withRate(long rate);

        /**
         * Assigns the burst size to this token bucket.
         * Default to 2 * 1500 bytes.
         *
         * @param burstSize a burst size
         * @return this
         */
        Builder withBurstSize(long burstSize);

        /**
         * Assigns the action to this token bucket.
         * <p>
         * Note: mandatory setter for this builder
         * </p>
         * @param action an action
         * @return this
         */
        Builder withAction(Action action);

        /**
         * Assigns the dscp value to this token bucket.
         *
         * @param dscp a dscp value
         * @return this
         */
        Builder withDscp(short dscp);

        /**
         * Assigns the type to this token bucket.
         * <p>
         * Note: mandatory setter for this builder
         * </p>
         * @param type the type
         * @return this
         */
        Builder withType(Type type);

        /**
         * Builds the token bucket based on the specified
         * parameters when possible.
         *
         * @return a token bucket
         */
        TokenBucket build();

    }
}
