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

import org.onlab.packet.IpPrefix;

/**
 * Representation of security group rule.
 */
public interface KubevirtSecurityGroupRule {

    /**
     * Returns the security group rule identifier.
     *
     * @return security group rule identifier
     */
    String id();

    /**
     * Returns the security group identifier.
     *
     * @return security group identifier
     */
    String securityGroupId();

    /**
     * Returns the traffic direction.
     *
     * @return traffic direction
     */
    String direction();

    /**
     * Returns the ethernet type.
     *
     * @return ethernet type
     */
    String etherType();

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
     * Returns the remote IP prefix.
     *
     * @return remote IP prefix
     */
    IpPrefix remoteIpPrefix();

    /**
     * Returns the remote group identifier.
     *
     * @return remote group identifier
     */
    String remoteGroupId();

    /**
     * Returns the security group rule with updated direction.
     *
     * @param direction direction
     * @return updated security group
     */
    KubevirtSecurityGroupRule updateDirection(String direction);

    /**
     * A default builder interface.
     */
    interface Builder {
        /**
         * Builds an immutable security group rule instance.
         *
         * @return kubevirt security group rule
         */
        KubevirtSecurityGroupRule build();

        /**
         * Returns kubevirt security group rule builder with supplied id.
         *
         * @param id security group rule id
         * @return security group rule builder
         */
        Builder id(String id);

        /**
         * Returns kubevirt security group rule builder with supplied security group id.
         *
         * @param securityGroupId security group  id
         * @return security group rule builder
         */
        Builder securityGroupId(String securityGroupId);

        /**
         * Returns kubevirt security group rule builder with supplied direction.
         *
         * @param direction traffic direction
         * @return security group rule builder
         */
        Builder direction(String direction);

        /**
         * Returns kubevirt security group rule builder with supplied etherType.
         *
         * @param etherType network etherType
         * @return security group rule builder
         */
        Builder etherType(String etherType);

        /**
         * Returns kubevirt security group rule builder with supplied maximum port range.
         *
         * @param portRangeMax maximum port range
         * @return security group rule builder
         */
        Builder portRangeMax(Integer portRangeMax);

        /**
         * Returns kubevirt security group rule builder with supplied minimum port range.
         *
         * @param portRangeMin minimum port range
         * @return security group rule builder
         */
        Builder portRangeMin(Integer portRangeMin);

        /**
         * Returns kubevirt security group rule builder with supplied protocol.
         *
         * @param protocol network protocol
         * @return security group rule builder
         */
        Builder protocol(String protocol);

        /**
         * Returns kubevirt security group rule builder with supplied remote IP prefix.
         *
         * @param remoteIpPrefix remote IP prefix
         * @return security group rule builder
         */
        Builder remoteIpPrefix(IpPrefix remoteIpPrefix);

        /**
         * Returns kubevirt security group rule builder with supplied remote group id.
         *
         * @param remoteGroupId remote group id
         * @return security group rule builder
         */
        Builder remoteGroupId(String remoteGroupId);
    }
}
