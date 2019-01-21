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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of instance IPAM; not intended for direct use.
 */
public interface K8sIpamStore extends Store<K8sIpamEvent, K8sIpamStoreDelegate> {

    /**
     * Allocates a new IP address.
     *
     * @param networkId network identifier
     * @return newly allocated IP address
     */
    IpAddress allocateIp(String networkId);

    /**
     * Leases the existing IP address.
     *
     * @param networkId network identifier
     * @return leased IP address
     */
    IpAddress leaseIp(String networkId);

    /**
     * Initializes a new IP pool with the given network.
     *
     * @param networkId network identifier
     */
    void initializeIpPool(String networkId);

    /**
     * Purges an existing IP pool associated with the given network.
     *
     * @param networkId network identifier
     */
    void purgeIpPool(String networkId);

    /**
     * Returns the already allocated IP addresses.
     *
     * @param networkId network identifier
     * @return allocated IP addresses
     */
    Set<IpAddress> allocatedIps(String networkId);

    /**
     * Returns the available IP addresses.
     *
     * @param networkId network identifier
     * @return available IP addresses
     */
    Set<IpAddress> availableIps(String networkId);
}
