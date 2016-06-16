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

package org.onosproject.bmv2.api.service;

import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.net.DeviceId;

/**
 * A controller of BMv2 devices.
 */
@Beta
public interface Bmv2Controller {

    /**
     * Default port.
     */
    int DEFAULT_PORT = 40123;

    /**
     * Return an agent to operate on the given device.
     *
     * @param deviceId a device ID
     * @return a BMv2 agent
     * @throws Bmv2RuntimeException if the agent is not available
     */
    Bmv2DeviceAgent getAgent(DeviceId deviceId) throws Bmv2RuntimeException;

    /**
     * Returns true if the given device is reachable from this controller, false otherwise.
     *
     * @param deviceId a device ID
     * @return a boolean value
     */
    boolean isReacheable(DeviceId deviceId);

    /**
     * Register the given device listener.
     *
     * @param listener a device listener
     */
    void addDeviceListener(Bmv2DeviceListener listener);

    /**
     * Unregister the given device listener.
     *
     * @param listener a device listener
     */
    void removeDeviceListener(Bmv2DeviceListener listener);

    /**
     * Register the given packet listener.
     *
     * @param listener a packet listener
     */
    void addPacketListener(Bmv2PacketListener listener);

    /**
     * Unregister the given packet listener.
     *
     * @param listener a packet listener
     */
    void removePacketListener(Bmv2PacketListener listener);
}
