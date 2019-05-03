/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onlab.packet.IpAddress;

import java.util.Set;

/**
 * IP Address Management admin service for kubernetes network.
 */
public interface K8sIpamAdminService extends K8sIpamService {

    /**
     * Allocates a new IP address with the given network.
     *
     * @param networkId network identifier
     * @return allocated IP address
     */
    IpAddress allocateIp(String networkId);

    /**
     * Reserves an IP address with the given network.
     *
     * @param networkId network identifier
     * @param ipAddress IP address
     */
    void reserveIp(String networkId, IpAddress ipAddress);

    /**
     * Releases the IP address from the given network.
     *
     * @param networkId network identifier
     * @param ipAddress IP address
     * @return true if the given IP was successfully released, false otherwise
     */
    boolean releaseIp(String networkId, IpAddress ipAddress);

    /**
     * Initializes IP address pool.
     *
     * @param networkId network identifier
     * @param ipAddresses a set of IP addresses contained in this IP pool
     */
    void initializeIpPool(String networkId, Set<IpAddress> ipAddresses);

    /**
     * Purges the existing IP address pool of the given network identifier.
     *
     * @param networkId network identifier
     */
    void purgeIpPool(String networkId);
}
