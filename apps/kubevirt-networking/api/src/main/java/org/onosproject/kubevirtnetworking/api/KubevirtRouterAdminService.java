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

import org.onlab.packet.MacAddress;

/**
 * Service for administering the inventory of kubevirt router service.
 */
public interface KubevirtRouterAdminService extends KubevirtRouterService {

    /**
     * Creates a kubevirt router with the given information.
     *
     * @param router a new router
     */
    void createRouter(KubevirtRouter router);

    /**
     * Updates the kubevirt router with the given information.
     *
     * @param router the updated router
     */
    void updateRouter(KubevirtRouter router);

    /**
     * Removes the router.
     *
     * @param name router name
     */
    void removeRouter(String name);

    /**
     * Updates the MAC address of the peer router.
     *
     * @param name router name
     * @param mac peer router MAC address
     */
    void updatePeerRouterMac(String name, MacAddress mac);

    /**
     * Creates a kubevirt floating IP with the given information.
     *
     * @param floatingIp a new floating IP
     */
    void createFloatingIp(KubevirtFloatingIp floatingIp);

    /**
     * Updates the kubevirt floating IP with the given information.
     *
     * @param floatingIp the updated floating IP
     */
    void updateFloatingIp(KubevirtFloatingIp floatingIp);

    /**
     * Removes the floating IP with the given identifier.
     *
     * @param id floating IP identifier
     */
    void removeFloatingIp(String id);

    /**
     * Removes all routers.
     */
    void clear();
}
