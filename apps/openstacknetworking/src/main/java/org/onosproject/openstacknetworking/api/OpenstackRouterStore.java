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
package org.onosproject.openstacknetworking.api;

import org.onosproject.store.Store;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

import java.util.Set;

/**
 * Manages inventory of OpenStack router states; not intended for direct use.
 */
public interface OpenstackRouterStore
        extends Store<OpenstackRouterEvent, OpenstackRouterStoreDelegate> {

    /**
     * Creates a new router.
     *
     * @param osRouter openstack router
     */
    void createRouter(Router osRouter);

    /**
     * Updates the router.
     *
     * @param osRouter updated openstack router
     */
    void updateRouter(Router osRouter);

    /**
     * Removes the router with the supplied ID.
     *
     * @param osRouterId openstack router id
     * @return removed router; null if failed
     */
    Router removeRouter(String osRouterId);

    /**
     * Returns the router with the supplied router ID.
     *
     * @param osRouterId openstack router id
     * @return openstack router; null if not found
     */
    Router router(String osRouterId);

    /**
     * Returns all routers.
     *
     * @return set of openstack router
     */
    Set<Router> routers();

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
     * @return removed router interface; null if failed
     */
    RouterInterface removeRouterInterface(String osRouterIfaceId);

    /**
     * Returns the router interface with the given ID.
     *
     * @param osRouterIfaceId openstack router interface port id
     * @return openstack router interface
     */
    RouterInterface routerInterface(String osRouterIfaceId);

    /**
     * Returns all router interfaces.
     *
     * @return set of openstack router interfaces
     */
    Set<RouterInterface> routerInterfaces();

    /**
     * Creates a new floating IP address.
     *
     * @param floatingIp openstack floating ip
     */
    void createFloatingIp(NetFloatingIP floatingIp);

    /**
     * Updates the floating IP address.
     *
     * @param floatingIp updated openstack floating ip
     */
    void updateFloatingIp(NetFloatingIP floatingIp);

    /**
     * Removes the floating IP with the supplied ID.
     *
     * @param floatingIpId floating ip id
     * @return removed floating ip; null if failed
     */
    NetFloatingIP removeFloatingIp(String floatingIpId);

    /**
     * Returns the floating IP with the supplied ID.
     *
     * @param floatingIpId floating ip id
     * @return openstack floating ip; null if not found
     */
    NetFloatingIP floatingIp(String floatingIpId);

    /**
     * Returns all floating IP addresses.
     *
     * @return openstack floating ip; empty set if no floating ip exists
     */
    Set<NetFloatingIP> floatingIps();

    /**
     * Clears the existing routers and floating IPs.
     */
    void clear();
}
