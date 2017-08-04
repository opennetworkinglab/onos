/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.key;

import com.google.common.annotations.Beta;
import org.onosproject.store.Store;

import java.util.Collection;

/**
 * Manages inventory of device keys; not intended for direct use.
 */
@Beta
public interface DeviceKeyStore extends Store<DeviceKeyEvent, DeviceKeyStoreDelegate> {
    /**
     * Creates or updates a device key.
     *
     * @param deviceKey device key
     */
    void createOrUpdateDeviceKey(DeviceKey deviceKey);

    /**
     * Deletes a device key by a specific device key identifier.
     *
     * @param deviceKeyId device key unique identifier
     */
    void deleteDeviceKey(DeviceKeyId deviceKeyId);

    /**
     * Returns all device keys.
     *
     * @return set of device keys
     */
    Collection<DeviceKey> getDeviceKeys();

    /**
     * Returns the device key matching a device key identifier.
     *
     * @param deviceKeyId device key unique identifier
     * @return device key
     */
    DeviceKey getDeviceKey(DeviceKeyId deviceKeyId);
}
