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

import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubevirt router.
 */
public interface KubevirtRouterService
        extends ListenerService<KubevirtRouterEvent, KubevirtRouterListener> {

    /**
     * Returns the kubevirt router with the supplied router name.
     *
     * @param name router name
     * @return kubevirt router
     */
    KubevirtRouter router(String name);

    /**
     * Returns all kubevirt routers registered in the service.
     *
     * @return set of kubevirt routers
     */
    Set<KubevirtRouter> routers();

    /**
     * Returns the kubevirt floating IP with the supplied identifier.
     *
     * @param id floating IP identifier
     * @return kubevirt floating IP
     */
    KubevirtFloatingIp floatingIp(String id);

    /**
     * Returns the kubevirt floating IP associated with the supplied POD.
     *
     * @param podName name of POD
     * @return kubevirt floating IP
     */
    KubevirtFloatingIp floatingIpByPodName(String podName);

    /**
     * Returns the kubevirt floating IPs bound to the given router.
     *
     * @param routerName name of router
     * @return set of kubevirt floating IPs
     */
    Set<KubevirtFloatingIp> floatingIpsByRouter(String routerName);

    /**
     * Returns all kubevirt floating IPs.
     *
     * @return set of kubevirt floating IPs
     */
    Set<KubevirtFloatingIp> floatingIps();
}
