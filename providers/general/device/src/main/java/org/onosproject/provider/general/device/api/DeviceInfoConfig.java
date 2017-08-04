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

package org.onosproject.provider.general.device.api;

import com.google.common.annotations.Beta;

import java.util.Map;

/**
 * Protocol specific configuration for the general device provider.
 */
@Beta
public final class DeviceInfoConfig {

    private final Map<String, String> configValues;
    private final String deviceKeyId;


    public DeviceInfoConfig(Map<String, String> configValues, String deviceKeyId) {
        this.configValues = configValues;
        this.deviceKeyId = deviceKeyId;
    }

    /**
     * Gets the configValues contained in the json sent via net-cfg.
     *
     * @return configValues in key-value pairs.
     */
    public Map<String, String> configValues() {
        return configValues;
    }

    /**
     * Gets the device key id of the device.
     * This key should be pre-existing in ONOS.
     *
     * @return device key id
     */
    public String deviceKeyId() {
        return deviceKeyId;
    }

}