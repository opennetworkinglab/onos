/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.tenantnetwork;

import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;

/**
 * Service for interacting with the inventory of tenantNetwork.
 */
public interface TenantNetworkService {

    /**
     * Returns if the tenantNetwork is existed.
     *
     * @param networkId tenantNetwork identifier
     * @return true or false if one with the given identifier exists.
     */
    boolean exists(TenantNetworkId networkId);

    /**
     * Returns the number of tenantNetwork known to the system.
     *
     * @return number of tenantNetwork.
     */
    int getNetworkCount();

    /**
     * Returns an iterable collection of the currently known tenantNetwork.
     *
     * @return collection of tenantNetwork.
     */
    Iterable<TenantNetwork> getNetworks();

    /**
     * Returns the tenantNetwork with the identifier.
     *
     * @param networkId TenantNetwork identifier
     * @return TenantNetwork or null if one with the given identifier is not
     *         known.
     */
    TenantNetwork getNetwork(TenantNetworkId networkId);

    /**
     * Creates tenantNetworks by networks.
     *
     * @param networks the collection of tenantNetworks
     * @return true if all given identifiers created successfully.
     */
    boolean createNetworks(Iterable<TenantNetwork> networks);

    /**
     * Updates tenantNetworks by tenantNetworks.
     *
     * @param networks the collection of tenantNetworks
     * @return true if all given identifiers updated successfully.
     */
    boolean updateNetworks(Iterable<TenantNetwork> networks);

    /**
     * Deletes tenantNetwork by tenantNetworkIds.
     *
     * @param networksIds the collection of tenantNetworkIds
     * @return true if the specified tenantNetworks deleted successfully.
     */
    boolean removeNetworks(Iterable<TenantNetworkId> networksIds);
}
