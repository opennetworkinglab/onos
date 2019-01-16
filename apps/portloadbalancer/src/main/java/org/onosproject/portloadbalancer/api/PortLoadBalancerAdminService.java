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
package org.onosproject.portloadbalancer.api;

import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * Port load balancer admin service.
 */
public interface PortLoadBalancerAdminService {
    /**
     * Creates or updates a port load balancer.
     *
     * @param portLoadBalancerId port load balancer id
     * @param ports physical ports in the port load balancer
     * @param mode port load balancer mode
     * @return port load balancer that is created or updated
     */
    PortLoadBalancer createOrUpdate(PortLoadBalancerId portLoadBalancerId, Set<PortNumber> ports,
                                    PortLoadBalancerMode mode);

    /**
     * Removes a port load balancer.
     *
     * @param portLoadBalancerId port load balancer id
     * @return port load balancer that is removed or null if it was not possible
     */
    PortLoadBalancer remove(PortLoadBalancerId portLoadBalancerId);

}
