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

package org.onosproject.l2lb.api;

import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.Map;

/**
 * L2 load balance service.
 */
public interface L2LbService extends ListenerService<L2LbEvent, L2LbListener> {
    /**
     * Gets all L2 load balancers from the store.
     *
     * @return L2 load balancer ID and L2 load balancer information mapping
     */
    Map<L2LbId, L2Lb> getL2Lbs();

    /**
     * Gets L2 load balancer that matches given device ID and key, or null if not found.
     *
     * @param deviceId Device ID
     * @param key L2 load balancer key
     * @return L2 load balancer information
     */
    L2Lb getL2Lb(DeviceId deviceId, int key);

    /**
     * Gets L2 load balancer next ids from the store.
     *
     * @return L2 load balancer id and next id mapping
     */
    Map<L2LbId, Integer> getL2LbNexts();

    /**
     * Gets L2 load balancer next id that matches given device Id and key, or null if not found.
     *
     * @param deviceId Device ID
     * @param key L2 load balancer key
     * @return next ID
     */
    int getL2LbNexts(DeviceId deviceId, int key);

    /**
     * Reserves a l2 load balancer. Only one application
     * at time can reserve a given l2 load balancer.
     *
     * @param l2LbId the l2 load balancer id
     * @param appId the application id
     * @return true if reservation was successful false otherwise
     */
    boolean reserve(L2LbId l2LbId, ApplicationId appId);

    /**
     * Releases a l2 load balancer. Once released
     * by the owner the l2 load balancer is eligible
     * for removal.
     *
     * @param l2LbId the l2 load balancer id
     * @param appId the application id
     * @return true if release was successful false otherwise
     */
    boolean release(L2LbId l2LbId, ApplicationId appId);

    /**
     * Gets reservation of a l2 load balancer. Only one application
     * at time can reserve a given l2 load balancer.
     *
     * @param l2LbId the l2 load balancer id
     * @return the id of the application using the l2 load balancer
     */
    ApplicationId getReservation(L2LbId l2LbId);

    /**
     * Gets l2 load balancer reservations. Only one application
     * at time can reserve a given l2 load balancer.
     *
     * @return reservations of the l2 load balancer resources
     */
    Map<L2LbId, ApplicationId> getReservations();

}
