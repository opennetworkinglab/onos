/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.basics.BasicDeviceConfig;

import java.io.IOException;
import java.io.InputStream;

public final class TestUpfUtils {

    private static final String BASIC_CONFIG_KEY = "basic";

    private TestUpfUtils() {
        // hide constructor
    }

    public static BasicDeviceConfig getBasicConfig(DeviceId deviceId, String fileName)
            throws IOException {
        BasicDeviceConfig basicCfg = new BasicDeviceConfig();
        InputStream jsonStream = TestUpfUtils.class.getResourceAsStream(fileName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        basicCfg.init(deviceId, BASIC_CONFIG_KEY, jsonNode, mapper, config -> {
        });
        return basicCfg;
    }
}
