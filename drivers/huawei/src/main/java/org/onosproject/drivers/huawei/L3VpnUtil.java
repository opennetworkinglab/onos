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

package org.onosproject.drivers.huawei;

import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.DeviceKeys;
import org.onosproject.yang.model.KeyInfo;

/**
 * Representation of utility for huawei driver's L3VPN.
 */
public final class L3VpnUtil {

    // No instantiation.
    private L3VpnUtil() {
    }

    /**
     * Returns the device id from the instance key.
     *
     * @param key instance key
     * @return device id
     */
    static String getDevIdFromIns(KeyInfo key) {
        return ((DeviceKeys) key).deviceid();
    }
}
