/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.dhcprelay.config;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;

import com.google.common.collect.Sets;

/**
 * Dhcp Host Auto Relearn Config.
 */
public class HostAutoRelearnConfig extends Config<ApplicationId> {
    public static final String KEY = "hostAutoRelearnEnabledDevices";

    @Override
    public boolean isValid() {
        if (array == null) {
            return false;
        }
        try {
            for (JsonNode node : array) {
               DeviceId.deviceId(node.asText());
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    /**
     * Returns Set of Devices on which Host Auto Relearn is enabled.
     *
     * @return Set of DeviceId where Host Auto Relearn is enabled.
     */

    public Set<DeviceId> hostAutoRelearnEnabledDevices() {
        Set<DeviceId> enabledDevices = Sets.newHashSet();

        array.forEach(node -> {
            DeviceId deviceId = DeviceId.deviceId(node.asText());
            enabledDevices.add(deviceId);
        });

        return enabledDevices;
    }
}
