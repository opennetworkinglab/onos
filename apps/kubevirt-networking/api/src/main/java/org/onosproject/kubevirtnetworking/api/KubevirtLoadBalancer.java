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

import org.onlab.packet.IpAddress;

import java.util.Set;

/**
 * Representation of load balancer.
 */
public interface KubevirtLoadBalancer {

    /**
     * Returns the load balancer name.
     *
     * @return load balancer name
     */
    String name();

    /**
     * Returns the load balancer description.
     *
     * @return load balancer description
     */
    String description();

    /**
     * Returns the network identifier.
     *
     * @return network identifier
     */
    String networkId();

    /**
     * Returns the load balancer virtual IP address.
     *
     * @return load balancer virtual IP address
     */
    IpAddress vip();

    /**
     * Returns the IP address of members.
     *
     * @return IP addresses
     */
    Set<IpAddress> members();

    /**
     * Returns the load balancer rules.
     *
     * @return load balancer rules
     */
    Set<KubevirtLoadBalancerRule> rules();

    /**
     * A default builder interface.
     */
    interface Builder {
        /**
         * Builds an immutable load balancer instance.
         *
         * @return kubevirt load balancer
         */
        KubevirtLoadBalancer build();

        /**
         * Returns kubevirt load balancer builder with supplied load balancer name.
         *
         * @param name load balancer name
         * @return load balancer builder
         */
        Builder name(String name);

        /**
         * Returns kubevirt load balancer builder with supplied load balancer description.
         *
         * @param description load balancer description
         * @return load balancer builder
         */
        Builder description(String description);

        /**
         * Returns kubevirt load balancer builder with supplied load balancer network identifier.
         *
         * @param networkId load balancer network identifier
         * @return load balancer builder
         */
        Builder networkId(String networkId);

        /**
         * Returns kubevirt load balancer builder with supplied load balancer vip.
         *
         * @param vip load balancer vip
         * @return load balancer builder
         */
        Builder vip(IpAddress vip);

        /**
         * Returns kubevirt load balancer builder with supplied load balancer members.
         *
         * @param members load balancer members
         * @return load balancer builder
         */
        Builder members(Set<IpAddress> members);

        /**
         * Returns kubevirt load balancer builder with supplied load balancer rules.
         *
         * @param rules load balancer rules
         * @return load balancer builder
         */
        Builder rules(Set<KubevirtLoadBalancerRule> rules);
    }
}
