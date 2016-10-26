/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onlab.util.Bandwidth;
import org.onosproject.net.Annotated;
import org.onosproject.net.Description;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Default implementation of immutable Queue description.
 */
@Beta
public interface QueueDescription extends Description, Annotated {

    /**
     * Denotes the type of the Queue.
     */
    enum Type {
        /**
         * Support min rate.
         */
        MIN,

        /**
         * Support max rate.
         */
        MAX,

        /**
         * Support priority.
         */
        PRIORITY,

        /**
         * Support burst.
         */
        BURST
    }

    /**
     * Returns queue identifier.
     *
     * @return queue identifier
     */
    QueueId queueId();

    /**
     * Returns dscp in range 0 to 63.
     *
     * @return dscp
     */
    Optional<Integer> dscp();

    /**
     * Returns type.
     *
     * @return type
     */
    EnumSet<Type> type();

    /**
     * Returns max rate, Valid only in specific type.
     *
     * @return Maximum allowed bandwidth, in bit/s.
     */
    Optional<Bandwidth> maxRate();

    /**
     * Returns min rate, Valid only in specific type.
     *
     * @return Minimum guaranteed bandwidth, in bit/s.
     */
    Optional<Bandwidth> minRate();

    /**
     * Returns burst, Valid only in specific type.
     *
     * @return Burst size, in bits
     */
    Optional<Long> burst();

    /**
     * Returns priority, Valid only in specific type.
     * small number have higher priority, in range 0 to 0xFFFFFFFF
     * @return priority
     */
    Optional<Long> priority();



    interface Builder {

        /**
         * Returns queue description builder with given name.
         *
         * @param queueId queue identifier
         * @return queue description builder
         */
        Builder queueId(QueueId queueId);

        /**
         * Returns queue description builder with given dscp.
         *
         * @param dscp dscp
         * @return queue description builder
         */
        Builder dscp(Integer dscp);

        /**
         * Returns queue description builder with given type.
         *
         * @param type type
         * @return queue description builder
         */
        Builder type(EnumSet<Type> type);

        /**
         * Returns queue description builder with max rate.
         * @param maxRate Maximum allowed bandwidth
         * @return queue description builder
         */
        Builder maxRate(Bandwidth maxRate);

        /**
         * Returns queue description builder with a given min rate.
         *
         * @param minRate Minimum guaranteed bandwidth
         * @return queue description builder
         */
        Builder minRate(Bandwidth minRate);

        /**
         * Returns queue description builder with a given burst.
         *
         * @param burst burst size
         * @return queue description builder
         */
        Builder burst(Long burst);

        /**
         * Returns queue description builder with a given priority.
         * small number have higher priority, in range 0 to 0xFFFFFFFF
         * @param priority priority
         * @return queue description builder
         */
        Builder priority(Long priority);

        /**
         * Builds an immutable bridge description.
         *
         * @return queue description
         */
        QueueDescription build();
    }
}
