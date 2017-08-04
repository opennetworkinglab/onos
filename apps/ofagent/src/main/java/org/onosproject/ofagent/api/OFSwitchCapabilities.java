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
package org.onosproject.ofagent.api;

import org.projectfloodlight.openflow.protocol.OFCapabilities;

import java.util.Set;

/**
 * Representation of capabilities of a virtual OpenFlow switch.
 */
public interface OFSwitchCapabilities {

    /**
     * Returns the capabilities of the switch.
     *
     * @return capabilities
     */
    Set<OFCapabilities> ofSwitchCapabilities();

    interface Builder {

        /**
         * Builds a OFSwitchCapabilities object.
         *
         * @return OFSwitchCapabilities
         */
        OFSwitchCapabilities build();

        /**
         * Enable OFPC_FLOW_STATS capability.
         *
         * @return Builder object
         */
        Builder flowStats();

        /**
         * Enable OFPC_TABLE_STATS capability.
         *
         * @return Builder object
         */
        Builder tableStats();

        /**
         * Enable OFPC_PORT_STATS capability.
         *
         * @return Builder object
         */
        Builder portStats();

        /**
         * Enable OFPC_GROUP_STATS capability.
         *
         * @return Builder object
         */
        Builder groupStats();

        /**
         * Enable OFPC_IP_REASM capability.
         *
         * @return Builder object
         */
        Builder ipReasm();

        /**
         * Enable OFPC_QUEUE_STATS capability.
         *
         * @return Builder object
         */
        Builder queueStats();

        /**
         * Enable OFPC_PORT_BLOCKED capability.
         *
         * @return Builder object
         */
        Builder portBlocked();
    }

}
