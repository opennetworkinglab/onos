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

public interface KubevirtNetworkAdminService extends KubevirtNetworkService {

    /**
     * Creates a kubevirt network with the given information.
     *
     * @param network the new network
     */
    void createNetwork(KubevirtNetwork network);

    /**
     * Updates the kubevirt network with the given information.
     *
     * @param network the updated network
     */
    void updateNetwork(KubevirtNetwork network);

    /**
     * Removes the network.
     *
     * @param networkId network identifier
     */
    void removeNetwork(String networkId);

    void clear();
}
