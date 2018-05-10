/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openflow.config;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

import java.util.Optional;

/**
 * Configuration for OpenFlow devices.
 */
@Beta
public class OpenFlowDeviceConfig extends Config<DeviceId> {

    /**
     * netcfg ConfigKey.
     */
    public static final String CONFIG_KEY = "openflow";

    public static final String CLIENT_KEY_ALIAS = "keyAlias";

    @Override
    public boolean isValid() {
        return hasOnlyFields(CLIENT_KEY_ALIAS);
    }

    /**
     * Gets the certFile for the switch.
     *
     * @return cert file
     */
    public Optional<String> keyAlias() {
        String keyAlias = get(CLIENT_KEY_ALIAS, "");
        return (keyAlias.isEmpty() ? Optional.empty() : Optional.ofNullable(keyAlias));
    }

    /**
     * Sets the key alias for the Device.
     *
     * @param keyAlias key alias as string
     * @return instance for chaining
     */
    public OpenFlowDeviceConfig setKeyAlias(String keyAlias) {
        return (OpenFlowDeviceConfig) setOrClear(CLIENT_KEY_ALIAS, keyAlias);
    }
}
