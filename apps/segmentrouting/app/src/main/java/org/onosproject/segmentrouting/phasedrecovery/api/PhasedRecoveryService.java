/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.segmentrouting.phasedrecovery.api;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Map;
import java.util.Set;

/**
 * Service that provides functionality related to phased recovery.
 */
public interface PhasedRecoveryService {

    // TODO Make timeout values configurable via Component Config Service
    /**
     * Timeout for PAIR phase in seconds.
     */
    int PAIR_TIMEOUT = 30;

    /**
     * Timeout for INFRA phase in seconds.
     */
    int INFRA_TIMEOUT = 30;

    /**
     * Timeout for EDGE phase in seconds.
     */
    int EDGE_TIMEOUT = 30;

    //
    // Phased recovery APIs.
    //


    /**
     * Returns true if phased recovery is enabled.
     *
     * @return true if phased recovery is enabled.
     */
    boolean isEnabled();

    /**
     * Initializes a device. Only the master of the device is allowed to do this.
     *
     * @param deviceId device ID
     * @return true if the device is initialized successfully and the caller should proceed,
     *         false if the device initialization has failed and the caller should abort.
     */
    boolean init(DeviceId deviceId);

    /**
     * Resets a device. Only the master of the device is allowed to do this.
     *
     * @param deviceId device ID
     * @return true if the device is reset successfully.
     *         false if the device has not been previously initialized.
     */
    boolean reset(DeviceId deviceId);

    /**
     * Gets recovery phase of every devices.
     *
     * @return a map between device ID and recovery phase
     */
    Map<DeviceId, Phase> getPhases();

    /**
     * Gets recovery phase of given device.
     *
     * @param deviceId device ID
     * @return current phase or null if the device wasn't seen before
     */
    Phase getPhase(DeviceId deviceId);

    /**
     * Sets given device with given recovery phase. Only the master of the device is allowed to do this.
     *
     * @param deviceId device ID
     * @param newPhase recovery phase
     * @return new phase if transition succeeded, otherwise return current phase.
     */
    Phase setPhase(DeviceId deviceId, Phase newPhase);

    //
    // Port manipulation APIs.
    //

    /**
     * Enables every ports on the given device.
     *
     * @param deviceId device id
     * @param enabled true to enable, false to disable
     * @return ports that have been enabled
     */
    Set<PortNumber> changeAllPorts(DeviceId deviceId, boolean enabled);

    /**
     * Enables pair port on the given device.
     *
     * @param deviceId device id
     * @param enabled true to enable, false to disable
     * @return ports that have been enabled
     */
    Set<PortNumber> changePairPort(DeviceId deviceId, boolean enabled);

    /**
     * Enables infrastructure ports on the given device.
     *
     * @param deviceId device id
     * @param enabled true to enable, false to disable
     * @return ports that have been enabled
     */
    Set<PortNumber> changeInfraPorts(DeviceId deviceId, boolean enabled);

    /**
     * Enables edge ports on the given device.
     *
     * @param deviceId device id
     * @param enabled true to enable, false to disable
     * @return ports that have been enabled
     */
    Set<PortNumber> changeEdgePorts(DeviceId deviceId, boolean enabled);
}
