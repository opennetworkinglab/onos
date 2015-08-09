/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

/**
 * Service for programming data plane flow rules in manner independent of
 * specific device table pipeline configuration.
 */
@Beta
public interface FlowObjectiveService {

    /**
     * Installs the filtering rules onto the specified device.
     *
     * @param deviceId            device identifier
     * @param filteringObjective the filtering objective
     */
    void filter(DeviceId deviceId, FilteringObjective filteringObjective);

    /**
     * Installs the forwarding rules onto the specified device.
     *
     * @param deviceId             device identifier
     * @param forwardingObjective the forwarding objective
     */
    void forward(DeviceId deviceId, ForwardingObjective forwardingObjective);

    /**
     * Installs the next hop elements into the specified device.
     *
     * @param deviceId       device identifier
     * @param nextObjective a next objective
     */
    void next(DeviceId deviceId, NextObjective nextObjective);

    /**
     * Obtains a globally unique next objective.
     *
     * @return an integer
     */
    int allocateNextId();

    /**
     * Installs the filtering rules onto the specified device.
     *
     * @param policy            policy expression
     */
    void initPolicy(String policy);
}
