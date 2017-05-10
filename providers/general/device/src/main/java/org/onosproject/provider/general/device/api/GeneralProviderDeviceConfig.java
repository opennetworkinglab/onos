/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for General device provider.
 */
@Beta
public class GeneralProviderDeviceConfig extends Config<DeviceId> {

    private static final String DEVICEKEYID = "deviceKeyId";


    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Gets the information of all protocols associated to the device.
     *
     * @return map of protocol name and relative information
     */
    public Map<String, DeviceInfoConfig> protocolsInfo() {
        return getProtocolInfoMap();
    }

    private Map<String, DeviceInfoConfig> getProtocolInfoMap() {
        Map<String, DeviceInfoConfig> deviceMap = new HashMap<>();
        node.fieldNames().forEachRemaining(name -> {

            Map<String, String> configMap = new HashMap<>();
            JsonNode protocol = node.get(name);
            protocol.fieldNames().forEachRemaining(info -> configMap.put(info, protocol.get(info).asText()));

            String deviceKeyId = "";
            if (protocol.has(DEVICEKEYID)) {
                deviceKeyId = protocol.get(DEVICEKEYID).asText("");
            }

            deviceMap.put(name, new DeviceInfoConfig(configMap, deviceKeyId));
        });
        return deviceMap;
    }

}
