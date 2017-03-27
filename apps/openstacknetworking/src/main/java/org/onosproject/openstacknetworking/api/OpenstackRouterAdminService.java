/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

/**
 * Service for administering the inventory of OpenStack router and floating IP.
 */
public interface OpenstackRouterAdminService {

    /**
     * Creates a router with the given information.
     *
     * @param osRouter the new router
     */
    void createRouter(Router osRouter);

    /**
     * Updates the router with the given information.
     *
     * @param osRouter updated router
     */
    void updateRouter(Router osRouter);

    /**
     * Removes the router with the given router ID.
     *
     * @param osRouterId router id
     */
    void removeRouter(String osRouterId);

    // TODO fix the logic adding a router interface to a router
    /**
     * Adds a new interface to the router.
     *
     * @param osRouterIface openstack router interface
     */
    void addRouterInterface(RouterInterface osRouterIface);

    /**
     * Updates the router interface.
     *
     * @param osRouterIface openstack router interface
     */
    void updateRouterInterface(RouterInterface osRouterIface);

    /**
     * Removes the router interface.
     *
     * @param osRouterIfaceId openstack router interface id
     */
    void removeRouterInterface(String osRouterIfaceId);

    /**
     * Creates a floating IP with the given information.
     *
     * @param floatingIP the new floating ip
     */
    void createFloatingIp(NetFloatingIP floatingIP);

    /**
     * Updates the floating IP with the given information.
     *
     * @param floatingIP the updated floating ip
     */
    void updateFloatingIp(NetFloatingIP floatingIP);

    /**
     * Removes the floating IP with the given floating IP ID.
     *
     * @param floatingIpId floating ip id
     */
    void removeFloatingIp(String floatingIpId);

    /**
     * Clears the existing routers and floating IPs.
     */
    void clear();
}
