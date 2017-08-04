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
package org.onosproject.net.config.inject;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.BaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;

//{
//    "devices" : {
//      "inject:10.49.54.31" : {
//        "basic" : {
//          "driver" : "polatis-config",
//          "hwVersion" : "1.0",
//          "swVersion" : "2.0",
//          "serial" : "R-3000"
//        },
//        "inject" : {
//          "ports" : "4"
//        }
//      }
//    }
//  }

/**
 * Config to inject a Device.
 */
@Beta
public class DeviceInjectionConfig
    extends BaseConfig<DeviceId> {

    /**
     * Configuration key for {@link DeviceInjectionConfig}.
     */
    public static final String CONFIG_KEY = "inject";

    /**
     * JSON key for ports. {@value #PORTS}.
     * Expects a string representation of decimal integer.
     */
    private static final String PORTS = "ports";


    @Override
    public boolean isValid() {
        return true;
    }

    public String ports() {
        return get(PORTS, "0");
    }

    /**
     * Create a {@link DeviceInjectionConfig}.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public DeviceInjectionConfig() {
        super();
    }

    /**
     * Create a {@link DeviceInjectionConfig} for specified Device.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param did DeviceId
     */
    public DeviceInjectionConfig(DeviceId did) {
        ObjectMapper mapper = new ObjectMapper();
        init(did, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }

    @Override
    public String toString() {
        return "DeviceInjectionConfig";
    }
}
