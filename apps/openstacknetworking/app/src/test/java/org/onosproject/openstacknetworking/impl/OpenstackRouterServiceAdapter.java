/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

import java.util.Set;

/**
 * Test adapter for OpenstackRouterService.
 */
public class OpenstackRouterServiceAdapter implements OpenstackRouterAdminService {
    @Override
    public Router router(String osRouterId) {
        return null;
    }

    @Override
    public Set<Router> routers() {
        return null;
    }

    @Override
    public RouterInterface routerInterface(String osRouterIfaceId) {
        return null;
    }

    @Override
    public Set<RouterInterface> routerInterfaces() {
        return null;
    }

    @Override
    public Set<RouterInterface> routerInterfaces(String osRouterId) {
        return null;
    }

    @Override
    public NetFloatingIP floatingIp(String floatingIpId) {
        return null;
    }

    @Override
    public Set<NetFloatingIP> floatingIps() {
        return null;
    }

    @Override
    public Set<NetFloatingIP> floatingIps(String routerId) {
        return null;
    }

    @Override
    public void addListener(OpenstackRouterListener listener) {

    }

    @Override
    public void removeListener(OpenstackRouterListener listener) {

    }

    @Override
    public void createRouter(Router osRouter) {

    }

    @Override
    public void updateRouter(Router osRouter) {

    }

    @Override
    public void removeRouter(String osRouterId) {

    }

    @Override
    public void addRouterInterface(RouterInterface osRouterIface) {

    }

    @Override
    public void updateRouterInterface(RouterInterface osRouterIface) {

    }

    @Override
    public void removeRouterInterface(String osRouterIfaceId) {

    }

    @Override
    public void createFloatingIp(NetFloatingIP floatingIP) {

    }

    @Override
    public void updateFloatingIp(NetFloatingIP floatingIP) {

    }

    @Override
    public void removeFloatingIp(String floatingIpId) {

    }

    @Override
    public void clear() {

    }
}
