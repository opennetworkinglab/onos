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

import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of immutable Qos description.
 */
@Beta
public interface QosDescription extends Description, Annotated {

    /**
     * Denotes the type of the Qos.
     */
    enum Type {
        /**
         * Corresponds to Hierarchy token bucket classifier.
         */
        HTB,

        /**
         * Corresponds to Hierarchical Fair Service Curve classifier.
         */
        HFSC,

        /**
         * Corresponds to Stochastic Fairness Queueing classifier.
         */
        SFQ,

        /**
         * Corresponds to Controlled Delay classifier classifier.
         */
        CODEL,

        /**
         * Corresponds to Fair Queuing with Controlled Delay classifier.
         */
        FQ_CODEL,

        /**
         * No operation.
         */
        NOOP,

        /**
         * DPDK egress policer.
         */
        EGRESS_POLICER
    }

    /**
     * Returns qos identifier.
     *
     * @return qos identifier
     */
    QosId qosId();

    /**
     * Returns qos type.
     *
     * @return qos type
     */
    Type type();

    /**
     * Returns the max rate of qos, Valid only in specific qos type.
     *
     * @return Maximum rate shared by all queued traffic, in bit/s.
     */
    Optional<Bandwidth>  maxRate();

    /**
     * Returns Committed Information Rate of Qos, Valid only in specific qos type.
     * the CIR is measured in bytes of IP packets per second.
     *
     * @return cir
     */
    Optional<Long> cir();

    /**
     * Returns Committed Burst Size of Qos, Valid only in specific qos type.
     * the CBS is measured in bytes and represents a token bucket.
     *
     * @return cbs
     */
    Optional<Long> cbs();

    /**
     * Returns map of integer-Queue pairs, Valid only in specific qos type.
     *
     * @return queues
     */
    Optional<Map<Long, QueueDescription>> queues();

    /**
     * Builder of qos description entities.
     */
    interface Builder {
        /**
         * Returns qos description builder with a given name.
         *
         * @param qosId qos identifier
         * @return bridge description builder
         */
        Builder qosId(QosId qosId);

        /**
         * Returns qos description builder with a given type.
         *
         * @param type qos type
         * @return bridge description builder
         */
        Builder type(Type type);

        /**
         * Returns qos description builder with given maxRate.
         *
         * @param maxRate qos max rate
         * @return qos description builder
         */
        Builder maxRate(Bandwidth maxRate);

        /**
         * Returns qos description builder with a given cir.
         * @param cir in bytes of IP packets per second
         * @return qos description builder
         */
        Builder cir(Long cir);

        /**
         * Returns qos description builder with a given cbs.
         *
         * @param cbs in bytes and represents a token bucket
         * @return qos description builder
         */
        Builder cbs(Long cbs);

        /**
         * Returns qos description builder with a given queues.
         *
         * @param queues the map from queue numbers to Queue records
         * @return qos description builder
         */
        Builder queues(Map<Long, QueueDescription> queues);

        /**
         * Builds an immutable qos description.
         *
         * @return qos description
         */
        QosDescription build();
    }
}
