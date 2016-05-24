/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.cordconfig.access;

import org.onosproject.net.DeviceId;

import java.util.Optional;
import java.util.Set;

/**
 * Provides access to the common CORD configuration.
 */
public interface CordConfigService {

    /**
     * Retrieves the set of all access devices in the system.
     *
     * @return set of access devices
     */
    Set<AccessDeviceData> getAccessDevices();

    /**
     * Retrieves the access device with the given device ID.
     *
     * @param deviceId device ID
     * @return access device
     */
    Optional<AccessDeviceData> getAccessDevice(DeviceId deviceId);

    /**
     * Retrieves the set of all access agents in the system.
     *
     * @return set of access agents
     */
    Set<AccessAgentData> getAccessAgents();

    /**
     * Retrieves the access agent for the given device ID.
     *
     * @param deviceId device ID
     * @return access agent
     */
    Optional<AccessAgentData> getAccessAgent(DeviceId deviceId);
}
