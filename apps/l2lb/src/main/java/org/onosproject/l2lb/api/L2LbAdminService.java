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

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * LACP admin service.
 */
public interface L2LbAdminService {
    /**
     * Creates or updates a L2 load balancer.
     *
     * @param deviceId Device ID
     * @param key L2 load balancer key
     * @param ports physical ports in the L2 load balancer
     * @param mode L2 load balancer mode
     * @return L2 load balancer that is created or updated
     */
    L2Lb createOrUpdate(DeviceId deviceId, int key, Set<PortNumber> ports, L2LbMode mode);

    /**
     * Removes a L2 load balancer.
     *
     * @param deviceId Device ID
     * @param key L2 load balancer key
     * @return L2 load balancer that is removed or null if it was not possible
     */
    L2Lb remove(DeviceId deviceId, int key);

}
