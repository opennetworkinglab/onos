/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

/**
 * Representation of load balancer rule.
 */
public interface KubevirtLoadBalancerRule {

    /**
     * Returns the maximum port range.
     *
     * @return maximum port range
     */
    Integer portRangeMax();

    /**
     * Returns the minimum port range.
     *
     * @return minimum port range
     */
    Integer portRangeMin();

    /**
     * Returns the network protocol.
     *
     * @return network protocol
     */
    String protocol();

    /**
     * A default builder interface.
     */
    interface Builder {
        /**
         * Builds an immutable load balancer rule instance.
         *
         * @return kubevirt load balancer rule
         */
        KubevirtLoadBalancerRule build();

        /**
         * Returns kubevirt load balancer rule builder with supplied maximum port range.
         *
         * @param portRangeMax maximum port range
         * @return balancer rule rule builder
         */
        Builder portRangeMax(Integer portRangeMax);

        /**
         * Returns kubevirt load balancer rule builder with supplied minimum port range.
         *
         * @param portRangeMin minimum port range
         * @return balancer rule rule builder
         */
        Builder portRangeMin(Integer portRangeMin);

        /**
         * Returns kubevirt load balancer rule builder with supplied protocol.
         *
         * @param protocol network protocol
         * @return balancer rule rule builder
         */
        Builder protocol(String protocol);

    }
}
