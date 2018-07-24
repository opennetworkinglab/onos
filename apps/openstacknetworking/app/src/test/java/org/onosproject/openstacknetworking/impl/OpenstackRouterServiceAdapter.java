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

import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

import java.util.Set;

/**
 * Test adapter for OpenstackRouterService.
 */
public class OpenstackRouterServiceAdapter implements OpenstackRouterService {
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
}
