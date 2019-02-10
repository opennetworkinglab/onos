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

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of instance IPAM; not intended for direct use.
 */
public interface K8sIpamStore extends Store<K8sIpamEvent, K8sIpamStoreDelegate> {

    /**
     * Creates a new allocated IP address.
     *
     * @param ipam IPAM instance
     */
    void createAllocatedIp(K8sIpam ipam);

    /**
     * Updates the existing allocated IP address.
     *
     * @param ipam IPAM instance
     */
    void updateAllocatedIp(K8sIpam ipam);

    /**
     * Removes the existing allocated IP address.
     *
     * @param ipamId IPAM identifier
     * @return removed IPAM instance; null if failed
     */
    K8sIpam removeAllocatedIp(String ipamId);

    /**
     * Returns the IPAM with the given IPAM identifier.
     *
     * @param ipamId IPAM identifier
     * @return IPAM; null it not found
     */
    K8sIpam allocatedIp(String ipamId);

    /**
     * Returns all allocated IPAM instances.
     *
     * @return set of IPAM instances
     */
    Set<K8sIpam> allocatedIps();

    /**
     * Creates a new available IP address.
     *
     * @param ipam IPAM instance
     */
    void createAvailableIp(K8sIpam ipam);

    /**
     * Updates the existing available IP address.
     *
     * @param ipam IPAM instance
     */
    void updateAvailableIp(K8sIpam ipam);

    /**
     * Removes the existing available IP address.
     *
     * @param ipamId IPAM identifier
     * @return remved IPAM instance; null if failed
     */
    K8sIpam removeAvailableIp(String ipamId);

    /**
     * Returns the IPAM with the given IPAM identifier.
     *
     * @param ipamId IPAM identifier
     * @return IPAM; null it not found
     */
    K8sIpam availableIp(String ipamId);

    /**
     * Returns all available IPAM instances.
     *
     * @return set of IPAM instances
     */
    Set<K8sIpam> availableIps();

    /**
     * Clears all allocated and available IP addresses.
     */
    void clear();

    /**
     * Clears allocated and available IP addresses associated with the given network.
     *
     * @param networkId network identifier
     */
    void clear(String networkId);
}
