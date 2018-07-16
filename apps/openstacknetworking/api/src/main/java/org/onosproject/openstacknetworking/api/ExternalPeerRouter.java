/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Representation of external peer router.
 */
public interface ExternalPeerRouter {

    /**
     * Returns external peer router IP address.
     *
     * @return ip address.
     */
    IpAddress ipAddress();

    /**
     * Returns external peer router MAC address.
     *
     * @return mac address
     */
    MacAddress macAddress();

    /**
     * Returns external peer router VLAN ID.
     *
     * @return vlan id
     */
    VlanId vlanId();

    /**
     * Builder of new external peer router.
     */
    interface Builder {

        /**
         * Builds an immutable external peer router instance.
         *
         * @return external peer router
         */
        ExternalPeerRouter build();

        /**
         * Returns router builder with supplied IP address.
         *
         * @param ipAddress IP address of external peer router
         * @return peer router builder
         */
        Builder ipAddress(IpAddress ipAddress);

        /**
         * Returns router builder with supplied MAC address.
         *
         * @param macAddress MAC address of external peer router
         * @return peer router builder
         */
        Builder macAddress(MacAddress macAddress);

        /**
         * Returns router builder with supplied VLAN ID.
         *
         * @param vlanId VLAN ID of external peer router
         * @return peer router builder
         */
        Builder vlanId(VlanId vlanId);
    }
}
