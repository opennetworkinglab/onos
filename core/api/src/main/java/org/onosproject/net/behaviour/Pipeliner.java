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
package org.onosproject.net.behaviour;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;

/**
 * Behaviour for handling various pipelines.
 */
public interface Pipeliner extends HandlerBehaviour {

    /**
     * Initializes the driver with context required for its operation.
     *
     * @param deviceId the deviceId
     * @param context  processing context
     */
    void init(DeviceId deviceId, PipelinerContext context);

    /**
     * Installs the filtering rules onto the device.
     *
     * @param filterObjective a filtering objective
     */
    void filter(FilteringObjective filterObjective);

    /**
     * Installs the forwarding rules onto the device.
     *
     * @param forwardObjective a forwarding objective
     */
    void forward(ForwardingObjective forwardObjective);

    /**
     * Installs the next hop elements into the device.
     *
     * @param nextObjective a next objectives
     */
    void next(NextObjective nextObjective);
}
