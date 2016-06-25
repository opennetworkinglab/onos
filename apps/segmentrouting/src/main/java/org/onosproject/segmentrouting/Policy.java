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

package org.onosproject.segmentrouting;

/**
 * Interface for Segment Routing Policy.
 */
public interface Policy {
    /**
     * Enums for policy type.
     */
    enum Type {
        /**
         * Tunnel flow policy type.
         */
        TUNNEL_FLOW,

        /**
         * Load balancing policy type.
         */
        LOADBALANCE,

        /**
         * policy to avoid specific routers or links.
         */
        AVOID,

        /**
         * Access Control policy type.
         */
        DENY
    }

    /**
     * Returns the policy ID.
     *
     * @return policy ID
     */
    String id();

    /**
     * Returns the priority of the policy.
     *
     * @return priority
     */
    int priority();

    /**
     * Returns the policy type.
     *
     * @return policy type
     */
    Type type();

    /**
     * Returns the source IP address of the policy.
     *
     * @return source IP address
     */
    String srcIp();

    /**
     * Returns the destination IP address of the policy.
     *
     * @return destination IP address
     */
    String dstIp();

    /**
     * Returns the IP protocol of the policy.
     *
     * @return IP protocol
     */
    String ipProto();

    /**
     * Returns the source port of the policy.
     *
     * @return source port
     */
    short srcPort();

    /**
     * Returns the destination of the policy.
     *
     * @return destination port
     */
    short dstPort();

}
