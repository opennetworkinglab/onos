/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.api;

import com.google.common.annotations.Beta;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.Set;

/**
 * Service for controlling INT-capable pipelines.
 */
@Beta
public interface IntService {
    /**
     * Represents the role of INT-capable devices.
     */
    enum IntDeviceRole {
        /**
         * Intermediate device to add its own INT metadata to an INT packet by
         * following the INT instruction in the INT header.
         */
        TRANSIT,
        /**
         * A device that creates, inserts INT headers into the packet, and
         * extracts the INT headers.
         */
        SOURCE_SINK
    }

    /**
     * Starts the INT functionality in all INT-capable devices.
     * This will include populating tables to process INT packets.
     */
    void startInt();

    /**
     * Starts the INT functionality in specified set of INT transit devices.
     * <p>
     * Note: this is an experimental API, which can be either changed or removed.
     *
     * @param transitDevices set of devices to start INT functionalities
     */
    void startInt(Set<DeviceId> transitDevices);

    /**
     * Stops the INT functionalities in all INT-capable devices.
     */
    void stopInt();

    /**
     * Stops the INT functionalities in specified set of INT transit devices.
     * <p>
     * Note: this is an experimental API, which can be either changed or removed.
     *
     * @param transitDevices set of devices to stop INT functionalities
     */
    void stopInt(Set<DeviceId> transitDevices);

    /**
     * Configures all INT-capable devices with given configuration.
     *
     * @param cfg configuration to set up
     */
    void setConfig(IntDeviceConfig cfg);

    /**
     * Retrieves the INT configuration.
     *
     * @return configuration
     */
    IntDeviceConfig getConfig();

    /**
     * Installs an IntIntent to devices.
     *
     * @param intIntent an IntIntent
     * @return an IntIntent ID corresponding to given intIntent
     */
    IntIntentId installIntIntent(IntIntent intIntent);

    /**
     * Removes an IntIntent from devices.
     *
     * @param intentId ID of the intIntent to remove
     */
    void removeIntIntent(IntIntentId intentId);

    /**
     * Returns an IntIntent for given intent ID.
     *
     * @param intentId ID of the intIntent to retrieve
     * @return an IntIntent
     */
    IntIntent getIntIntent(IntIntentId intentId);

    /**
     * Returns all IntIntents installed in the network.
     *
     * @return an IntIntent
     */
    Map<IntIntentId, IntIntent> getIntIntents();

    //TODO: [ONOS-7616] Design IntEvent and related APIs
}
