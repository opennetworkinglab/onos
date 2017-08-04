/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.driver.extensions;

/**
 * OFDPA MPLS Type experimenter match fields.
 */
public enum Ofdpa3MplsType {
    /**
     * None.
     */
    NONE((short) 0),
    /**
     * Virtual Private Wire Service.
     */
    VPWS((short) 1),
    /**
     * Virtual Private LAN Service.
     */
    VPLS((short) 2),
    /**
     * MPLS-TP OAM (Operation, Administration and Maintenance).
     */
    OAM((short) 4),
    /**
     * L3 unicast.
     */
    L3_UNICAST((short) 8),
    /**
     * L3 multicast.
     */
    L3_MULTICAST((short) 16),
    /**
     * L3 PHP (Penultimate Hop Popping).
     */
    L3_PHP((short) 32);

    private short value;

    Ofdpa3MplsType(short value) {
        this.value = value;
    }

    /**
     * Gets the value as an short.
     *
     * @return the value as an short
     */
    public short getValue() {
        return this.value;
    }
}