/*
 * Copyright 2017-present Open Networking Foundation
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


package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages the mapping of Pipeconfs that are deployed to devices; not intended for direct use.
 */
@Beta
public interface PiPipeconfMappingStore extends Store<PiPipeconfDeviceMappingEvent, PiPipeconfMappingStoreDelegate> {

    /**
     * Retrieves the id of the pipeconf associated to a given device.
     *
     * @param deviceId device identifier
     * @return PiPipeconfId
     */
    PiPipeconfId getPipeconfId(DeviceId deviceId);

    /**
     * Retrieves the set of devices on which the given pipeconf is applied.
     *
     * @param pipeconfId pipeconf identifier
     * @return the set of devices that have that pipeconf applied.
     */
    Set<DeviceId> getDevices(PiPipeconfId pipeconfId);

    /**
     * Stores or updates a binding between a device and the pipeconf deployed on it.
     *
     * @param deviceId   deviceId
     * @param pipeconfId pipeconfId
     */

    void createOrUpdateBinding(DeviceId deviceId, PiPipeconfId pipeconfId);

    /**
     * Removes device to pipeconf binding.
     *
     * @param deviceId deviceId
     */

    void removeBinding(DeviceId deviceId);

}
