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

package org.onosproject.t3.impl;

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.MacAddress;

import java.util.Map;

/**
 * Utility class for the troubleshooting tool.
 */
final class TroubleshootUtils {

    private TroubleshootUtils() {
        //Banning construction
    }

    /**
     * Map defining if a specific driver is for a HW switch.
     */
    //Done with builder() instead of of() for clarity
    static Map<String, Boolean> hardwareOfdpaMap = ImmutableMap.<String, Boolean>builder()
            .put("ofdpa", true)
            .put("ofdpa3", true)
            .put("qmx-ofdpa3", true)
            .put("as7712-32x-premium", true)
            .put("as5912-54x-premium", true)
            .put("as5916-54x-premium", true)
            .put("accton-ofdpa3", true)
            .put("znyx-ofdpa", true)
            .build();

    /**
     * Checks if the Mac Address is inside a range between the min MAC and the mask.
     * @param macAddress the MAC address to check
     * @param minAddr the min MAC address
     * @param maskAddr the mask
     * @return true if in range, false otherwise.
     */
    static boolean compareMac(MacAddress macAddress, MacAddress minAddr, MacAddress maskAddr) {
        byte[] mac = macAddress.toBytes();
        byte[] min = minAddr.toBytes();
        byte[] mask = maskAddr.toBytes();
        boolean inRange = true;

        int i = 0;

        //if mask is 00 stop
        while (inRange && i < mask.length && (mask[i] & 0xFF) != 0) {
            int ibmac = mac[i] & 0xFF;
            int ibmin = min[i] & 0xFF;
            int ibmask = mask[i] & 0xFF;
            if (ibmask == 255) {
                inRange = ibmac == ibmin;
            } else if (ibmac < ibmin || ibmac >= ibmask) {
                inRange = false;
                break;
            }
            i++;
        }

        return inRange;
    }
}
