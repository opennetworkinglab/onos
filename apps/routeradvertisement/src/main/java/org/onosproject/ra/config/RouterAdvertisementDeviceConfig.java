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

package org.onosproject.ra.config;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.List;

/**
 * Device configuration for Router Advertisement.
 */
public class RouterAdvertisementDeviceConfig extends Config<DeviceId> {

    private static final String PREFIXES = "prefixes";

    @Override
    public boolean isValid() {
        return hasOnlyFields(PREFIXES) && prefixes() != null;
    }


    /**
     * Gets global router advertisement prefixes for device.
     *
     * @return global prefixes. Or null if not configured.
     */
    public List<InterfaceIpAddress> prefixes() {
        if (!object.has(PREFIXES)) {
            return null;
        }

        List<InterfaceIpAddress> ips = Lists.newArrayList();
        ArrayNode prefixes = (ArrayNode) object.path(PREFIXES);
        prefixes.forEach(i -> ips.add(InterfaceIpAddress.valueOf(i.asText())));
        return ips;
    }

}


