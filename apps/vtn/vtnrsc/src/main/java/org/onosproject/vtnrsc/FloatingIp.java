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
package org.onosproject.vtnrsc;

import org.onlab.packet.IpAddress;

/**
 * Representation of a floatingIp.
 */
public interface FloatingIp {

    /**
     * Coarse classification of the type of the FloatingIp.
     */
    public enum Status {
        /**
         * Signifies that a floating Ip is currently active.
         */
        ACTIVE,
        /**
         * Signifies that a floating Ip is currently inactive.
         */
        INACTIVE
    }

    /**
     * Returns the floatingIp identifier.
     *
     * @return identifier
     */
    FloatingIpId id();

    /**
     * Returns the tenant identifier.
     *
     * @return the tenant identifier
     */
    TenantId tenantId();

    /**
     * Returns the network identifier.
     *
     * @return the network identifier
     */
    TenantNetworkId networkId();

    /**
     * Returns the port identifier.
     *
     * @return the port identifier
     */
    VirtualPortId portId();

    /**
     * Returns the router identifier.
     *
     * @return the router identifier
     */
    RouterId routerId();

    /**
     * Returns the floating ip address.
     *
     * @return floatingIp
     */
    IpAddress floatingIp();

    /**
     * Returns the fixed ip address.
     *
     * @return fixedIp
     */
    IpAddress fixedIp();

    /**
     * Returns the status of floating ip.
     *
     * @return floatingIpStatus
     */
    Status status();
}
