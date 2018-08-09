/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.event.ListenerService;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

import java.util.Set;

/**
 * Handles router update requests from OpenStack.
 */
public interface OpenstackRouterService
        extends ListenerService<OpenstackRouterEvent, OpenstackRouterListener> {

    /**
     * Returns the router with the supplied router ID.
     *
     * @param osRouterId openstack router id
     * @return openstack router
     */
    Router router(String osRouterId);

    /**
     * Returns all routers.
     *
     * @return set of openstack routers
     */
    Set<Router> routers();

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
     * Returns all router interfaces with the router ID.
     *
     * @param osRouterId openstack router id
     * @return set of router interfaces
     */
    Set<RouterInterface> routerInterfaces(String osRouterId);

    /**
     * Returns the floating IP with the supplied floating IP ID.
     *
     * @param floatingIpId floating ip id
     * @return openstack floating ip
     */
    NetFloatingIP floatingIp(String floatingIpId);

    /**
     * Returns all floating IPs.
     *
     * @return set of openstack floating ips
     */
    Set<NetFloatingIP> floatingIps();

    /**
     * Returns all floating IPs associated with the router ID.
     *
     * @param routerId router id
     * @return set of openstack floating ips
     */
    Set<NetFloatingIP> floatingIps(String routerId);
}
