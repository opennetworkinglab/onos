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

import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;

import java.util.Map;

/**
 * Port load balance service.
 */
public interface PortLoadBalancerService extends ListenerService<PortLoadBalancerEvent, PortLoadBalancerListener> {
    /**
     * Gets all port load balancers from the store.
     *
     * @return port load balancer ID and port load balancer information mapping
     */
    Map<PortLoadBalancerId, PortLoadBalancer> getPortLoadBalancers();

    /**
     * Gets port load balancer that matches given device ID and key, or null if not found.
     *
     * @param portLoadBalancerId port load balancer id
     * @return port load balancer information
     */
    PortLoadBalancer getPortLoadBalancer(PortLoadBalancerId portLoadBalancerId);

    /**
     * Gets the mapping between port load balancer id and
     * the next objective id that represents the port load balancer.
     *
     * @return port load balancer id and next id mapping
     */
    Map<PortLoadBalancerId, Integer> getPortLoadBalancerNexts();

    /**
     * Gets port load balancer next id that matches given device Id and key, or null if not found.
     *
     * @param portLoadBalancerId port load balancer id
     * @return next ID
     */
    int getPortLoadBalancerNext(PortLoadBalancerId portLoadBalancerId);

    /**
     * Reserves a port load balancer. Only one application
     * at time can reserve a given port load balancer.
     *
     * @param portLoadBalancerId the port load balancer id
     * @param appId the application id
     * @return true if reservation was successful false otherwise
     */
    boolean reserve(PortLoadBalancerId portLoadBalancerId, ApplicationId appId);

    /**
     * Releases a port load balancer. Once released
     * by the owner the port load balancer is eligible
     * for removal.
     *
     * @param portLoadBalancerId the port load balancer id
     * @param appId the application id
     * @return true if release was successful false otherwise
     */
    boolean release(PortLoadBalancerId portLoadBalancerId, ApplicationId appId);

    /**
     * Gets reservation of a port load balancer. Only one application
     * at time can reserve a given port load balancer.
     *
     * @param portLoadBalancerId the port load balancer id
     * @return the id of the application using the port load balancer
     */
    ApplicationId getReservation(PortLoadBalancerId portLoadBalancerId);

    /**
     * Gets port load balancer reservations. Only one application
     * at time can reserve a given port load balancer.
     *
     * @return reservations of the port load balancer resources
     */
    Map<PortLoadBalancerId, ApplicationId> getReservations();

}
