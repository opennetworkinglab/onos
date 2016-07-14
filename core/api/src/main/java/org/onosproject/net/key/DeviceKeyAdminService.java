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

package org.onosproject.net.key;

import com.google.common.annotations.Beta;

/**
 * Service for managing device keys.
 */
@Beta
public interface DeviceKeyAdminService extends DeviceKeyService {

    /**
     * Adds a new device key to the store.
     *
     * @param deviceKey device key to be stored
     */
    void addKey(DeviceKey deviceKey);

    /**
     * Removes a device key from the store using the device
     * key identifier.
     *
     * @param id device key identifier used to identify the device key
     */
    void removeKey(DeviceKeyId id);
}

